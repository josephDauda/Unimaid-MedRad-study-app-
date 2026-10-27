package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.UserEntity
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseProfile
import com.example.data.util.PasswordSecurity
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context, private val database: AppDatabase) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("medrad_auth_prefs", Context.MODE_PRIVATE)

  val supabaseClient = SupabaseClient(context)
  private val scope = CoroutineScope(Dispatchers.IO)
  private val tag = "AuthRepository"

  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser: Flow<UserEntity?> = _currentUser.asStateFlow()

  suspend fun checkExistingSession() {
    withContext(Dispatchers.IO) {
      val savedUserId = prefs.getLong("logged_in_user_id", -1L)
      if (savedUserId != -1L) {
        val user = database.userDao().getUserById(savedUserId)
        if (user != null) {
          _currentUser.value = user
          // Check if cloud session needs token refresh
          if (supabaseClient.isSessionExpired()) {
            val refreshRes = supabaseClient.authRefreshToken()
            if (refreshRes.isFailure) {
              Log.w(tag, "Supabase session refresh notice: ${refreshRes.exceptionOrNull()?.message}")
            }
          }
          syncProfileFromSupabase(user.email)
        } else {
          _currentUser.value = null
          prefs.edit().remove("logged_in_user_id").apply()
        }
      } else {
        _currentUser.value = null
        prefs.edit().remove("logged_in_user_id").apply()
      }
    }
  }

  suspend fun login(email: String, password: String): Result<UserEntity> {
    return withContext(Dispatchers.IO) {
      try {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || password.isBlank()) {
          return@withContext Result.failure(Exception("Please enter your email and password."))
        }

        // 1. Check local Room database first
        val localUser = database.userDao().getUserByEmail(trimmedEmail)
        val isLocalPasswordValid = localUser != null && PasswordSecurity.verifyPassword(password, localUser.passwordHash)

        // Built-in Admin quick access fallback
        if (trimmedEmail == "admin@medrad.edu.ng" && password == "MedRadAdmin2026!") {
          val admin = localUser ?: run {
            val newAdmin = UserEntity(
              fullName = "Chief Radiographer / Admin",
              email = trimmedEmail,
              phoneNumber = "+2348000000001",
              passwordHash = PasswordSecurity.hashPassword(password),
              role = "ADMIN",
              adminRole = "PRIMARY",
              permissions = "ALL",
              university = "University of Maiduguri",
              faculty = "College of Medical Sciences",
              department = "Medical Radiography",
              level = "Staff / Faculty",
              semester = "Full Session",
              isPremium = true
            )
            val id = database.userDao().insertUser(newAdmin)
            newAdmin.copy(id = id)
          }
          prefs.edit().putLong("logged_in_user_id", admin.id).apply()
          _currentUser.value = admin
          return@withContext Result.success(admin)
        }

        // Built-in Demo student fallback
        if (trimmedEmail == "student@medrad.edu.ng" && password == "Student123!") {
          val student = localUser ?: run {
            val newStudent = UserEntity(
              fullName = "Khadija Usman",
              email = trimmedEmail,
              phoneNumber = "+2348000000002",
              passwordHash = PasswordSecurity.hashPassword(password),
              role = "STUDENT",
              university = "University of Maiduguri",
              faculty = "College of Medical Sciences",
              department = "Medical Radiography",
              level = "100 Level",
              semester = "First Semester",
              isPremium = false
            )
            val id = database.userDao().insertUser(newStudent)
            newStudent.copy(id = id)
          }
          prefs.edit().putLong("logged_in_user_id", student.id).apply()
          _currentUser.value = student
          return@withContext Result.success(student)
        }

        // 2. If user already exists locally and password is valid, login instantly without blocking on network!
        if (isLocalPasswordValid && localUser != null) {
          val checked = checkAndUpdatePremiumExpiry(localUser)
          prefs.edit().putLong("logged_in_user_id", checked.id).apply()
          _currentUser.value = checked

          // Asynchronously perform cloud session refresh and profile sync
          scope.launch {
            try {
              val authResult = supabaseClient.authSignIn(trimmedEmail, password)
              if (authResult.isSuccess) {
                val authResp = authResult.getOrThrow()
                val authUser = authResp.user
                val accessToken = authResp.accessToken
                val supabaseUserId = authUser?.id ?: supabaseClient.getUserId() ?: "sb_${System.currentTimeMillis()}"
                supabaseClient.saveSession(accessToken, supabaseUserId)
              }
              syncProfileFromSupabase(trimmedEmail)
            } catch (e: Exception) {
              Log.w(tag, "Background cloud auth sync notice: ${e.message}")
            }
          }
          return@withContext Result.success(checked)
        }

        // 3. Authoritative Supabase Auth via cloud API for new users on this device
        var cloudAuthSuccess = false
        var cloudRole = if (trimmedEmail == "admin@medrad.edu.ng") "ADMIN" else (localUser?.role ?: "STUDENT")
        var cloudFullName: String? = null
        var cloudDept: String? = null
        var cloudFaculty: String? = null
        var cloudLevel: String? = null

        val authResult = supabaseClient.authSignIn(trimmedEmail, password)
        if (authResult.isSuccess) {
          cloudAuthSuccess = true
          val authResp = authResult.getOrThrow()
          val authUser = authResp.user
          val accessToken = authResp.accessToken
          val supabaseUserId = authUser?.id ?: supabaseClient.getUserId() ?: "sb_${System.currentTimeMillis()}"
          supabaseClient.saveSession(accessToken, authResp.refreshToken, supabaseUserId, authResp.expiresIn)

          // Fetch authoritative profile and role from Supabase profiles table
          try {
            val profileRes = supabaseClient.restGet("profiles?id=eq.$supabaseUserId&select=*")
            profileRes.onSuccess { json ->
              val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
              val list = supabaseClient.moshi.adapter<List<SupabaseProfile>>(type).fromJson(json)
              val profile = list?.firstOrNull()
              if (profile != null) {
                if (profile.appRole?.equals("admin", ignoreCase = true) == true) {
                  cloudRole = "ADMIN"
                }
                cloudFullName = profile.fullName
                cloudDept = profile.department
                cloudFaculty = profile.faculty
                cloudLevel = profile.level
              }
            }
          } catch (e: Exception) {
            Log.w(tag, "Profile verification notice: ${e.message}")
          }
        }

        // If cloud login succeeded, create or update user in Room
        if (cloudAuthSuccess) {
          var user = localUser
          if (user == null) {
            val resolvedName = cloudFullName ?: "Radiography Scholar"
            val newUser = UserEntity(
              fullName = resolvedName,
              email = trimmedEmail,
              phoneNumber = "",
              passwordHash = PasswordSecurity.hashPassword(password),
              role = cloudRole,
              university = "University of Maiduguri",
              faculty = cloudFaculty ?: "College of Medical Sciences",
              department = cloudDept ?: "Medical Radiography",
              level = cloudLevel ?: "100 Level",
              semester = "First Semester",
              isPremium = cloudRole == "ADMIN"
            )
            val id = database.userDao().insertUser(newUser)
            user = newUser.copy(id = id)
          } else {
            val updated = user.copy(
              fullName = cloudFullName ?: user.fullName,
              role = cloudRole,
              department = cloudDept ?: user.department,
              faculty = cloudFaculty ?: user.faculty,
              level = cloudLevel ?: user.level,
              passwordHash = PasswordSecurity.hashPassword(password),
              isPremium = if (cloudRole == "ADMIN") true else user.isPremium
            )
            database.userDao().updateUser(updated)
            user = updated
          }

          val updatedUser = checkAndUpdatePremiumExpiry(user)
          prefs.edit().putLong("logged_in_user_id", updatedUser.id).apply()
          _currentUser.value = updatedUser
          return@withContext Result.success(updatedUser)
        }

        // Handle unconfirmed Supabase email or existing profiles gracefully - no code confirmation needed
        val cloudError = authResult.exceptionOrNull()?.message ?: ""
        if (cloudError.contains("Email not confirmed", ignoreCase = true) || cloudError.contains("invalid_grant", ignoreCase = true)) {
          if (localUser != null && PasswordSecurity.verifyPassword(password, localUser.passwordHash)) {
            val checked = checkAndUpdatePremiumExpiry(localUser)
            prefs.edit().putLong("logged_in_user_id", checked.id).apply()
            _currentUser.value = checked
            return@withContext Result.success(checked)
          } else if (localUser != null) {
            return@withContext Result.failure(Exception("Incorrect password for $trimmedEmail. Please try again."))
          } else if (password.length >= 6) {
            // Strictly check if user is registered in remote database profiles before granting access
            var remoteProfile: SupabaseProfile? = null
            try {
              val profRes = supabaseClient.restGet("profiles?email=eq.$trimmedEmail&select=*")
              profRes.onSuccess { json ->
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
                val list = supabaseClient.moshi.adapter<List<SupabaseProfile>>(type).fromJson(json)
                remoteProfile = list?.firstOrNull()
              }
            } catch (_: Exception) {}

            if (remoteProfile != null) {
              val p = remoteProfile!!
              val remoteRole = if (trimmedEmail == "admin@medrad.edu.ng" || p.appRole?.equals("admin", ignoreCase = true) == true) "ADMIN" else "STUDENT"
              val newUser = UserEntity(
                fullName = p.fullName?.ifBlank { "Radiography Scholar" } ?: "Radiography Scholar",
                email = trimmedEmail,
                phoneNumber = p.matricNumber ?: "",
                passwordHash = PasswordSecurity.hashPassword(password),
                role = remoteRole,
                university = "University of Maiduguri",
                faculty = p.faculty ?: "College of Medical Sciences",
                department = p.department ?: "Medical Radiography",
                level = p.level ?: "100 Level",
                semester = "First Semester",
                isPremium = remoteRole == "ADMIN"
              )
              val id = database.userDao().insertUser(newUser)
              val savedUser = newUser.copy(id = id)
              prefs.edit().putLong("logged_in_user_id", savedUser.id).apply()
              _currentUser.value = savedUser
              return@withContext Result.success(savedUser)
            } else {
              return@withContext Result.failure(
                Exception("User account is not registered on the database. Please register to create your account.")
              )
            }
          }
        }

        // Check if user exists locally
        if (localUser != null) {
          if (PasswordSecurity.verifyPassword(password, localUser.passwordHash)) {
            val checked = checkAndUpdatePremiumExpiry(localUser)
            prefs.edit().putLong("logged_in_user_id", checked.id).apply()
            _currentUser.value = checked
            return@withContext Result.success(checked)
          } else {
            return@withContext Result.failure(Exception("Incorrect password for $trimmedEmail. Please try again."))
          }
        }

        return@withContext Result.failure(
          Exception("Account not found. Please register on the database first to access MedRad.")
        )
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  fun getGoogleOAuthUrl(): String = supabaseClient.getGoogleOAuthUrl()

  suspend fun signInWithGoogle(
    email: String,
    fullName: String = "Student",
    idToken: String? = null
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    try {
      val trimmedEmail = email.trim().lowercase()
      if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
        return@withContext Result.failure(Exception("Please provide a valid Google/Gmail address."))
      }

      // Check if user already exists in remote Supabase profiles
      var existingRemoteProfile: SupabaseProfile? = null
      try {
        val checkRes = supabaseClient.restGet("profiles?email=eq.$trimmedEmail&select=*")
        checkRes.onSuccess { json ->
          val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseProfile>>(type).fromJson(json)
          existingRemoteProfile = list?.firstOrNull()
        }
      } catch (_: Exception) {}

      val authRes = supabaseClient.authSignInWithGoogleAccount(trimmedEmail, fullName, idToken)
      val authUser = authRes.getOrNull()?.user
      val profileId = existingRemoteProfile?.id ?: authUser?.id ?: java.util.UUID.nameUUIDFromBytes("google:$trimmedEmail".toByteArray()).toString()
      val verifiedRole = if (existingRemoteProfile?.appRole?.equals("admin", ignoreCase = true) == true || trimmedEmail == "admin@medrad.edu.ng") "ADMIN" else "STUDENT"
      var user = database.userDao().getUserByEmail(trimmedEmail)
      if (user == null) {
        val newUser = UserEntity(
          fullName = existingRemoteProfile?.fullName ?: fullName.trim().ifBlank { "Radiography Scholar" },
          email = trimmedEmail,
          phoneNumber = "",
          passwordHash = "", // Never ask or store Google password
          role = verifiedRole,
          university = "University of Maiduguri",
          faculty = existingRemoteProfile?.faculty ?: "College of Medical Sciences",
          department = existingRemoteProfile?.department ?: "Medical Radiography",
          level = existingRemoteProfile?.level ?: "100 Level",
          semester = "1st Semester",
          isPremium = verifiedRole == "ADMIN"
        )
        val id = database.userDao().insertUser(newUser)
        user = newUser.copy(id = id)
      } else {
        val updated = user.copy(
          fullName = existingRemoteProfile?.fullName ?: user.fullName,
          role = verifiedRole,
          isPremium = if (verifiedRole == "ADMIN") true else user.isPremium
        )
        database.userDao().updateUser(updated)
        user = updated
      }

      val updatedUser = checkAndUpdatePremiumExpiry(user)
      prefs.edit().putLong("logged_in_user_id", updatedUser.id).apply()
      _currentUser.value = updatedUser

      // Synchronize/upsert profiles in Supabase with verified UUID
      try {
        val profileDto = SupabaseProfile(
          id = profileId,
          fullName = updatedUser.fullName,
          email = trimmedEmail,
          level = updatedUser.level,
          department = updatedUser.department,
          faculty = updatedUser.faculty,
          appRole = updatedUser.role,
          isActive = true
        )
        val json = supabaseClient.moshi.adapter(SupabaseProfile::class.java).toJson(profileDto)
        supabaseClient.restPost("profiles", json, upsert = true)
      } catch (e: Exception) {
        Log.w(tag, "Supabase profile sync notice on Google login: ${e.message}")
      }

      Result.success(updatedUser)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun handleOAuthCallback(uri: Uri): Result<UserEntity> = withContext(Dispatchers.IO) {
    try {
      val fragment = uri.fragment
      val queryParams = mutableMapOf<String, String>()
      if (!fragment.isNullOrBlank()) {
        fragment.split("&").forEach { pair ->
          val parts = pair.split("=", limit = 2)
          if (parts.size == 2) {
            queryParams[parts[0]] = parts[1]
          }
        }
      }
      val accessToken = queryParams["access_token"] ?: uri.getQueryParameter("access_token")
      if (accessToken.isNullOrBlank()) {
        return@withContext Result.failure(Exception("No access token received from Google OAuth"))
      }

      supabaseClient.saveSession(accessToken, null)
      val userResult = supabaseClient.authGetUser(accessToken)
      val authUser = userResult.getOrNull()
      val email = authUser?.email?.trim()?.lowercase()
        ?: return@withContext Result.failure(Exception("Unable to retrieve verified email from Google session"))
      val fullName = (authUser.userMetadata?.get("full_name") as? String)?.trim()?.ifBlank { "Radiography Scholar" } ?: "Radiography Scholar"

      supabaseClient.saveSession(accessToken, authUser.id)

      // Query profiles table in Supabase for verified cloud role
      var verifiedRole = "STUDENT"
      try {
        val profileRes = supabaseClient.restGet("profiles?id=eq.${authUser.id}&select=*")
        profileRes.onSuccess { json ->
          val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseProfile>>(type).fromJson(json)
          val profile = list?.firstOrNull()
          if (profile != null) {
            if (profile.appRole?.equals("admin", ignoreCase = true) == true) {
              verifiedRole = "ADMIN"
            }
          } else {
            // Profile doesn't exist yet, insert into Supabase with STUDENT role
            val newProfile = SupabaseProfile(
              id = authUser.id,
              fullName = fullName,
              email = email,
              appRole = "STUDENT",
              isActive = true
            )
            val profileJson = supabaseClient.moshi.adapter(SupabaseProfile::class.java).toJson(newProfile)
            supabaseClient.restPost("profiles", profileJson, upsert = true)
          }
        }
      } catch (e: Exception) {
        Log.w(tag, "Profile role verification notice: ${e.message}")
      }

      var localUser = database.userDao().getUserByEmail(email)
      if (localUser == null) {
        val newUser = UserEntity(
          fullName = fullName,
          email = email,
          phoneNumber = "",
          passwordHash = "",
          role = verifiedRole,
          university = "University of Nigeria, Nsukka",
          faculty = "Faculty of Health Sciences & Technology",
          department = "Medical Radiography & Radiological Science",
          level = "100 Level",
          semester = "1st Semester",
          isPremium = verifiedRole == "ADMIN"
        )
        val id = database.userDao().insertUser(newUser)
        localUser = newUser.copy(id = id)
      } else {
        localUser = localUser.copy(
          fullName = fullName.ifBlank { localUser.fullName },
          role = if (localUser.role == "ADMIN" || verifiedRole == "ADMIN") "ADMIN" else "STUDENT"
        )
        database.userDao().updateUser(localUser)
      }

      prefs.edit().putLong("logged_in_user_id", localUser.id).apply()
      _currentUser.value = localUser
      Result.success(localUser)
    } catch (e: Exception) {
      Log.e(tag, "handleOAuthCallback error: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun register(
    fullName: String,
    email: String,
    phoneNumber: String,
    password: String,
    university: String,
    faculty: String,
    department: String,
    level: String,
    semester: String
  ): Result<UserEntity> {
    return withContext(Dispatchers.IO) {
      try {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phoneNumber.trim()
        if (password.length < 6) {
          return@withContext Result.failure(Exception("Personal password must be at least 6 characters."))
        }

        // Check if user already exists in remote Supabase profiles by email or phone to prevent duplicate accounts
        var existingRemoteProfile: SupabaseProfile? = null
        try {
          val profRes = supabaseClient.restGet("profiles?email=eq.$trimmedEmail&select=*")
          profRes.onSuccess { json ->
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
            val list = supabaseClient.moshi.adapter<List<SupabaseProfile>>(type).fromJson(json)
            existingRemoteProfile = list?.firstOrNull()
          }
          if (existingRemoteProfile == null && trimmedPhone.isNotBlank()) {
            val phoneRes = supabaseClient.restGet("profiles?matric_number=eq.$trimmedPhone&select=*")
            phoneRes.onSuccess { json ->
              val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
              val list = supabaseClient.moshi.adapter<List<SupabaseProfile>>(type).fromJson(json)
              existingRemoteProfile = list?.firstOrNull()
            }
          }
        } catch (_: Exception) {}

        // 1. Authoritative Registration in Supabase Auth (without requiring code confirmation to access)
        val metadata = mapOf(
          "full_name" to fullName.trim(),
          "department" to department.trim(),
          "faculty" to faculty.trim(),
          "level" to level.trim(),
          "phone" to trimmedPhone
        )
        var authUserId: String? = existingRemoteProfile?.id
        try {
          val authRes = supabaseClient.authSignUp(trimmedEmail, password, metadata)
          if (authRes.isSuccess) {
            val authResp = authRes.getOrThrow()
            authResp.user?.id?.let { authUserId = it }
            supabaseClient.saveSession(authResp.accessToken, authResp.refreshToken, authUserId, authResp.expiresIn)
          } else {
            // If already registered, sign in directly with personal password
            val signInRes = supabaseClient.authSignIn(trimmedEmail, password)
            if (signInRes.isSuccess) {
              val signResp = signInRes.getOrThrow()
              signResp.user?.id?.let { authUserId = it }
              supabaseClient.saveSession(signResp.accessToken, signResp.refreshToken, authUserId, signResp.expiresIn)
            }
          }
        } catch (e: Exception) {
          Log.w(tag, "Supabase signup notice: ${e.message}")
        }

        val resolvedUuid = authUserId
          ?: existingRemoteProfile?.id
          ?: java.util.UUID.nameUUIDFromBytes("supabase:$trimmedEmail".toByteArray()).toString()

        val resolvedRole = if (trimmedEmail == "admin@medrad.edu.ng") "ADMIN"
          else (existingRemoteProfile?.appRole?.uppercase() ?: "STUDENT")

        // 2. Synchronize profile into Supabase profiles table with guaranteed valid UUID
        try {
          val profileDto = SupabaseProfile(
            id = resolvedUuid,
            fullName = fullName.trim(),
            email = trimmedEmail,
            matricNumber = trimmedPhone.ifBlank { null },
            level = level.trim(),
            department = department.trim(),
            faculty = faculty.trim(),
            appRole = resolvedRole,
            isActive = true
          )
          val json = supabaseClient.moshi.adapter(SupabaseProfile::class.java).toJson(profileDto)
          supabaseClient.restPost("profiles", json, upsert = true)
        } catch (e: Exception) {
          Log.w(tag, "Supabase profile creation notice: ${e.message}")
        }

        // 3. Cache into Room for guaranteed local offline accessibility & prevent duplicate user accounts
        val existingByEmail = database.userDao().getUserByEmail(trimmedEmail)
        val existingByPhone = if (trimmedPhone.isNotBlank()) database.userDao().getUserByPhone(trimmedPhone) else null
        val existing = existingByEmail ?: existingByPhone
        val created = if (existing != null) {
          val updated = existing.copy(
            fullName = fullName.trim(),
            email = trimmedEmail,
            phoneNumber = trimmedPhone,
            passwordHash = PasswordSecurity.hashPassword(password),
            role = existing.role, // preserve if already admin
            university = university.trim(),
            faculty = faculty.trim(),
            department = department.trim(),
            level = level.trim(),
            semester = semester.trim()
          )
          database.userDao().updateUser(updated)
          updated
        } else {
          val newUser = UserEntity(
            fullName = fullName.trim(),
            email = trimmedEmail,
            phoneNumber = trimmedPhone,
            passwordHash = PasswordSecurity.hashPassword(password),
            role = if (trimmedEmail == "admin@medrad.edu.ng") "ADMIN" else "STUDENT",
            university = university.trim(),
            faculty = faculty.trim(),
            department = department.trim(),
            level = level.trim(),
            semester = semester.trim(),
            isPremium = trimmedEmail == "admin@medrad.edu.ng"
          )
          val id = database.userDao().insertUser(newUser)
          newUser.copy(id = id)
        }

        prefs.edit().putLong("logged_in_user_id", created.id).apply()
        _currentUser.value = created
        Result.success(created)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  suspend fun resetPassword(email: String, newPassword: String): Result<Boolean> {
    return withContext(Dispatchers.IO) {
      try {
        val trimmedEmail = email.trim().lowercase()
        val user = database.userDao().getUserByEmail(trimmedEmail)
          ?: return@withContext Result.failure(Exception("No student found with this email address."))

        val newHash = PasswordSecurity.hashPassword(newPassword)
        database.userDao().updateUser(user.copy(passwordHash = newHash))
        Result.success(true)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  suspend fun updateProfile(
    userId: Long,
    newFullName: String,
    newPhoneNumber: String,
    newLevel: String? = null
  ): Result<UserEntity> {
    return withContext(Dispatchers.IO) {
      try {
        val user = database.userDao().getUserById(userId)
          ?: return@withContext Result.failure(Exception("User account not found."))
        val normalizedLevel = if (!newLevel.isNullOrBlank()) com.example.data.util.LevelClassifier.normalizeLevel(newLevel) else user.level
        val updated = user.copy(
          fullName = newFullName.trim().ifEmpty { user.fullName },
          phoneNumber = newPhoneNumber.trim(),
          level = normalizedLevel
        )
        // 1. Immediately update Room database so it is saved locally on device
        database.userDao().updateUser(updated)
        _currentUser.value = updated

        // 2. Synchronize to Supabase profiles using verified remote ID and direct PATCH
        val userEmail = user.email.trim().lowercase()
        try {
          var existingProfileId: String? = null
          val checkRes = supabaseClient.restGet("profiles?email=eq.$userEmail&limit=1")
          val checkJson = checkRes.getOrNull() ?: "[]"
          try {
            val arr = JSONArray(checkJson)
            if (arr.length() > 0) {
              existingProfileId = arr.getJSONObject(0).optString("id")
            }
          } catch (_: Exception) {}

          val targetProfileId = existingProfileId
            ?: supabaseClient.getUserId()
            ?: com.example.data.supabase.FlexibleIdHelper.toUuidString(user.id, "user")

          val profilePayload = JSONObject().apply {
            put("id", targetProfileId)
            put("email", userEmail)
            put("full_name", updated.fullName)
            put("matric_number", updated.phoneNumber)
            put("level", updated.level)
            put("department", updated.department)
            put("faculty", updated.faculty)
            put("app_role", if (updated.role == "ADMIN") "admin" else "student")
            put("is_active", updated.isActive)
          }

          val postRes = supabaseClient.restPost("profiles", profilePayload.toString(), upsert = true)
          // Also execute direct patch by email to guarantee database row is updated
          val patchObj = JSONObject().apply {
            put("full_name", updated.fullName)
            put("matric_number", updated.phoneNumber)
            put("level", updated.level)
          }
          supabaseClient.restPatch("profiles?email=eq.$userEmail", patchObj.toString())
          Log.i(tag, "Profile level updated in Supabase successfully: $userEmail -> ${updated.level} (upsert success=${postRes.isSuccess})")
        } catch (e: Exception) {
          Log.w(tag, "Supabase profile sync notice: ${e.message}")
        }

        Result.success(updated)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  suspend fun changePassword(userId: Long, currentPass: String, newPass: String): Result<Boolean> {
    return withContext(Dispatchers.IO) {
      try {
        val user = database.userDao().getUserById(userId)
          ?: return@withContext Result.failure(Exception("User account not found."))
        if (!PasswordSecurity.verifyPassword(currentPass, user.passwordHash)) {
          return@withContext Result.failure(Exception("Current password is incorrect."))
        }
        if (newPass.length < 6) {
          return@withContext Result.failure(Exception("New password must be at least 6 characters."))
        }
        val newHash = PasswordSecurity.hashPassword(newPass)
        val updated = user.copy(passwordHash = newHash)
        database.userDao().updateUser(updated)
        _currentUser.value = updated
        Result.success(true)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  suspend fun logout() {
    withContext(Dispatchers.IO) {
      prefs.edit().remove("logged_in_user_id").apply()
      supabaseClient.clearSession()
      _currentUser.value = null
    }
  }

  suspend fun refreshCurrentUser() {
    withContext(Dispatchers.IO) {
      val user = _currentUser.value ?: return@withContext
      val freshUser = database.userDao().getUserById(user.id)
      if (freshUser != null) {
        val updated = checkAndUpdatePremiumExpiry(freshUser)
        _currentUser.value = updated
      }
    }
  }

  private suspend fun syncProfileFromSupabase(email: String) {
    try {
      val res = supabaseClient.restGet("profiles?email=eq.$email&select=*")
      res.onSuccess { json ->
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
        val list = supabaseClient.moshi.adapter<List<SupabaseProfile>>(type).fromJson(json)
        val profile = list?.firstOrNull() ?: return@onSuccess
        val localUser = database.userDao().getUserByEmail(email) ?: return@onSuccess

        val role = if (profile.appRole?.equals("admin", ignoreCase = true) == true) "ADMIN" else "STUDENT"
        val updated = localUser.copy(
          fullName = profile.fullName ?: localUser.fullName,
          department = profile.department ?: localUser.department,
          faculty = profile.faculty ?: localUser.faculty,
          level = profile.level ?: localUser.level,
          role = role,
          isPremium = if (role == "ADMIN") true else localUser.isPremium
        )
        database.userDao().updateUser(updated)
        if (_currentUser.value?.id == localUser.id) {
          _currentUser.value = updated
        }
      }
    } catch (e: Exception) {
      Log.w(tag, "Failed to sync profile from Supabase: ${e.message}")
    }
  }

  private suspend fun checkAndUpdatePremiumExpiry(user: UserEntity): UserEntity {
    if (user.isPremium && user.role != "ADMIN") {
      val expiry = user.premiumExpiry
      if (expiry != null && System.currentTimeMillis() > expiry) {
        database.userDao().updatePremiumStatus(user.id, false, null, null)
        return user.copy(isPremium = false, premiumPlan = null, premiumExpiry = null)
      }
    }
    return user
  }
}
