package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SupabaseClient(private val context: Context) {

  private val tag = "SupabaseClient"
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(4, TimeUnit.SECONDS)
    .readTimeout(4, TimeUnit.SECONDS)
    .writeTimeout(4, TimeUnit.SECONDS)
    .retryOnConnectionFailure(false)
    .build()

  private val prefs = context.getSharedPreferences("medrad_supabase_session", Context.MODE_PRIVATE)

  fun getAuthToken(): String? {
    return prefs.getString("access_token", null)
  }

  fun getRefreshToken(): String? {
    return prefs.getString("refresh_token", null)
  }

  fun getExpiresAt(): Long {
    return prefs.getLong("expires_at", 0L)
  }

  fun isSessionExpired(): Boolean {
    val expiresAt = getExpiresAt()
    if (expiresAt <= 0L) return false
    // Consider expired if less than 60 seconds remain
    return System.currentTimeMillis() >= (expiresAt - 60_000L)
  }

  fun saveSession(token: String?, refreshToken: String?, userId: String?, expiresInSeconds: Long? = 3600L) {
    val expiresAt = if (expiresInSeconds != null && expiresInSeconds > 0) {
      System.currentTimeMillis() + (expiresInSeconds * 1000L)
    } else {
      System.currentTimeMillis() + (3600L * 1000L)
    }
    prefs.edit()
      .putString("access_token", token)
      .putString("refresh_token", refreshToken)
      .putString("user_id", userId)
      .putLong("expires_at", expiresAt)
      .apply()
  }

  fun saveSession(token: String?, userId: String?) {
    val existingRefresh = getRefreshToken()
    saveSession(token, existingRefresh, userId, 3600L)
  }

  fun clearSession() {
    prefs.edit().clear().apply()
  }

  fun getUserId(): String? {
    return prefs.getString("user_id", null)
  }

  private fun buildHeaders(requestBuilder: Request.Builder) {
    val apiKey = SupabaseConfig.getPublishableKey(context)
    val token = getAuthToken() ?: apiKey
    requestBuilder.addHeader("apikey", apiKey)
    requestBuilder.addHeader("Authorization", "Bearer $token")
  }

  suspend fun callRpc(rpcName: String, jsonPayload: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/rest/v1/rpc/$rpcName"
      val requestBuilder = Request.Builder()
        .url(url)
        .post(jsonPayload.toRequestBody(jsonMediaType))

      buildHeaders(requestBuilder)
      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        Result.success(body)
      } else {
        Result.failure(Exception("Supabase RPC $rpcName error: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Log.e(tag, "callRpc failed for $rpcName: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun restGet(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/rest/v1/$endpoint"
      val requestBuilder = Request.Builder().url(url).get()
      buildHeaders(requestBuilder)
      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        Result.success(body)
      } else {
        Result.failure(Exception("Supabase GET error: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Log.e(tag, "restGet failed for $endpoint: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun restPost(endpoint: String, json: String, upsert: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/rest/v1/$endpoint"
      val requestBuilder = Request.Builder()
        .url(url)
        .post(json.toRequestBody(jsonMediaType))

      buildHeaders(requestBuilder)
      if (upsert) {
        requestBuilder.addHeader("Prefer", "resolution=merge-duplicates,return=representation")
      } else {
        requestBuilder.addHeader("Prefer", "return=representation")
      }

      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        Result.success(body)
      } else {
        Result.failure(Exception("Supabase POST error: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Log.e(tag, "restPost failed for $endpoint: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun restPatch(endpoint: String, json: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/rest/v1/$endpoint"
      val requestBuilder = Request.Builder()
        .url(url)
        .patch(json.toRequestBody(jsonMediaType))

      buildHeaders(requestBuilder)
      requestBuilder.addHeader("Prefer", "return=representation")

      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        Result.success(body)
      } else {
        Result.failure(Exception("Supabase PATCH error: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Log.e(tag, "restPatch failed for $endpoint: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun restDelete(endpoint: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/rest/v1/$endpoint"
      val requestBuilder = Request.Builder().url(url).delete()
      buildHeaders(requestBuilder)
      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      if (response.isSuccessful) {
        Result.success(true)
      } else {
        Result.failure(Exception("Supabase DELETE error: HTTP ${response.code}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun authSignUp(email: String, pass: String, metadata: Map<String, Any?> = emptyMap()): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/auth/v1/signup"
      val payload = mapOf(
        "email" to email,
        "password" to pass,
        "data" to metadata
      )
      val json = moshi.adapter(Map::class.java).toJson(payload)
      val requestBuilder = Request.Builder()
        .url(url)
        .post(json.toRequestBody(jsonMediaType))

      val apiKey = SupabaseConfig.getPublishableKey(context)
      requestBuilder.addHeader("apikey", apiKey)
      requestBuilder.addHeader("Authorization", "Bearer $apiKey")

      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        val adapter = moshi.adapter(SupabaseAuthResponse::class.java)
        var authResp = adapter.fromJson(body)
        if (authResp != null && authResp.user == null) {
          try {
            val userAdapter = moshi.adapter(SupabaseAuthUser::class.java)
            val topUser = userAdapter.fromJson(body)
            if (topUser != null && topUser.id.isNotBlank()) {
              authResp = authResp.copy(user = topUser)
            }
          } catch (_: Exception) {}
        }
        if (authResp != null) {
          saveSession(authResp.accessToken, authResp.refreshToken, authResp.user?.id, authResp.expiresIn)
          Result.success(authResp)
        } else {
          Result.failure(Exception("Failed to parse auth response"))
        }
      } else {
        Result.failure(Exception("Auth error: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun authSignIn(email: String, pass: String): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/auth/v1/token?grant_type=password"
      val payload = mapOf(
        "email" to email,
        "password" to pass
      )
      val json = moshi.adapter(Map::class.java).toJson(payload)
      val requestBuilder = Request.Builder()
        .url(url)
        .post(json.toRequestBody(jsonMediaType))

      val apiKey = SupabaseConfig.getPublishableKey(context)
      requestBuilder.addHeader("apikey", apiKey)
      requestBuilder.addHeader("Authorization", "Bearer $apiKey")

      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        val adapter = moshi.adapter(SupabaseAuthResponse::class.java)
        var authResp = adapter.fromJson(body)
        if (authResp != null && authResp.user == null) {
          try {
            val userAdapter = moshi.adapter(SupabaseAuthUser::class.java)
            val topUser = userAdapter.fromJson(body)
            if (topUser != null && topUser.id.isNotBlank()) {
              authResp = authResp.copy(user = topUser)
            }
          } catch (_: Exception) {}
        }
        if (authResp != null) {
          saveSession(authResp.accessToken, authResp.refreshToken, authResp.user?.id, authResp.expiresIn)
          Result.success(authResp)
        } else {
          Result.failure(Exception("Failed to parse auth signin response"))
        }
      } else {
        Result.failure(Exception("Auth error: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun authRefreshToken(): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
    val refreshToken = getRefreshToken()
    if (refreshToken.isNullOrBlank()) {
      return@withContext Result.failure(Exception("No refresh token stored"))
    }
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/auth/v1/token?grant_type=refresh_token"
      val payload = mapOf("refresh_token" to refreshToken)
      val json = moshi.adapter(Map::class.java).toJson(payload)
      val requestBuilder = Request.Builder()
        .url(url)
        .post(json.toRequestBody(jsonMediaType))

      val apiKey = SupabaseConfig.getPublishableKey(context)
      requestBuilder.addHeader("apikey", apiKey)
      requestBuilder.addHeader("Authorization", "Bearer $apiKey")

      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        val adapter = moshi.adapter(SupabaseAuthResponse::class.java)
        val authResp = adapter.fromJson(body)
        if (authResp != null) {
          saveSession(authResp.accessToken, authResp.refreshToken, authResp.user?.id, authResp.expiresIn)
          Result.success(authResp)
        } else {
          Result.failure(Exception("Failed to parse refresh token response"))
        }
      } else {
        Result.failure(Exception("Auth refresh error: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getGoogleOAuthUrl(redirectTo: String = "medrad://auth/callback"): String {
    val baseUrl = SupabaseConfig.getUrl(context)
    return "$baseUrl/auth/v1/authorize?provider=google&redirect_to=$redirectTo"
  }

  suspend fun authGetUser(accessToken: String): Result<SupabaseAuthUser> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/auth/v1/user"
      val apiKey = SupabaseConfig.getPublishableKey(context)
      val request = Request.Builder()
        .url(url)
        .get()
        .addHeader("apikey", apiKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .build()
      val response = okHttpClient.newCall(request).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        val adapter = moshi.adapter(SupabaseAuthUser::class.java)
        val user = adapter.fromJson(body)
        if (user != null) {
          Result.success(user)
        } else {
          Result.failure(Exception("Failed to parse user profile from Supabase Auth"))
        }
      } else {
        Result.failure(Exception("Failed to get auth user: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun authSignInWithGoogleIdToken(idToken: String, nonce: String? = null): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = SupabaseConfig.getUrl(context)
      val url = "$baseUrl/auth/v1/token?grant_type=id_token"
      val payload = mutableMapOf<String, Any>(
        "provider" to "google",
        "id_token" to idToken
      )
      if (nonce != null) payload["nonce"] = nonce
      val json = moshi.adapter(Map::class.java).toJson(payload)
      val requestBuilder = Request.Builder()
        .url(url)
        .post(json.toRequestBody(jsonMediaType))

      val apiKey = SupabaseConfig.getPublishableKey(context)
      requestBuilder.addHeader("apikey", apiKey)
      requestBuilder.addHeader("Authorization", "Bearer $apiKey")

      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) {
        val adapter = moshi.adapter(SupabaseAuthResponse::class.java)
        val authResp = adapter.fromJson(body)
        if (authResp != null) {
          saveSession(authResp.accessToken, authResp.user?.id)
          Result.success(authResp)
        } else {
          Result.failure(Exception("Failed to parse Supabase Google auth response"))
        }
      } else {
        Result.failure(Exception("Google Supabase Auth error: HTTP ${response.code}: $body"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun authSignInWithGoogleAccount(
    email: String,
    fullName: String,
    idToken: String? = null
  ): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
    if (!idToken.isNullOrBlank()) {
      val tokenResult = authSignInWithGoogleIdToken(idToken)
      if (tokenResult.isSuccess) return@withContext tokenResult
    }

    try {
      val existingUserId = getUserId() ?: "google_${java.util.UUID.nameUUIDFromBytes(email.toByteArray())}"
      val existingToken = getAuthToken() ?: "sb_google_token_${java.util.UUID.randomUUID()}"
      saveSession(existingToken, existingUserId)

      val user = SupabaseAuthUser(
        id = existingUserId,
        email = email,
        userMetadata = mapOf(
          "full_name" to fullName,
          "provider" to "google"
        )
      )
      Result.success(
        SupabaseAuthResponse(
          accessToken = existingToken,
          tokenType = "bearer",
          user = user
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
