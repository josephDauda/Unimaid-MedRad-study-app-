package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.StudyMaterialEntity
import com.example.data.supabase.FlexibleIdHelper
import com.example.data.supabase.SupabaseAnnouncement
import com.example.data.supabase.SupabaseAppConfig
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseCourse
import com.example.data.supabase.SupabaseCourseLibrary
import com.example.data.supabase.SupabaseExam
import com.example.data.supabase.SupabaseQuestion
import com.example.data.supabase.SupabaseRealtimeManager
import com.example.data.supabase.SupabaseStudentFeedback
import com.example.data.supabase.SupabaseStudyMaterial
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CloudAnnouncement(
  val id: String = "",
  val title: String = "",
  val message: String = "",
  val author: String = "Department of Medical Radiography",
  val timestamp: Long = System.currentTimeMillis(),
  val priority: String = "Normal",
  val target: String = "All Students"
)

data class AboutAppContent(
  val description: String = "MedRad Study Squad is the premier academic learning and CBT practice companion specifically engineered for undergraduate radiography students in Nigerian universities. Designed to bridge the gap between classroom didactic lectures and clinical hospital practice, it delivers authentic question banks, timed mock exam simulations, clinical anatomy, radiographic positioning atlases, and departmental updates.",
  val versionNotes: String = "Version 2.0: Connected to Supabase real-time cloud backend, course-tagged content, drawing & annotation exam questions, real-time push announcements, and global department card styling.",
  val departmentContact: String = "Department of Medical Radiography, College of Medical Sciences, University of Maiduguri, Borno State, Nigeria.\nEmail: radiography@unimaid.edu.ng",
  val credits: String = "Academic Advisory & Content Review: Faculty & Clinical Instructors, Department of Medical Radiography, UNIMAID.\nLead Developer: MedRad Study Squad Technical Team.",
  val currentVersion: String = "2.0.0",
  val latestVersion: String = "2.0.0",
  val hasUpdateNotice: Boolean = false,
  val updateNotes: String = "A new study pack update is available from the Department."
)

data class DailyCbtSprintConfig(
  val badge: String = "DAILY CBT SPRINT",
  val title: String = "Radiography Challenge",
  val subtitle: String = "15 practice questions ready for your level",
  val courseId: Long = 1L,
  val courseCode: String = "RAD 101",
  val questionCount: Int = 15,
  val activeStudentsText: String = "38 students active",
  val theme: String = "NAVY_GOLD",
  val questionIds: List<Long> = emptyList()
)

data class PracticeCardConfig(
  val isConfigured: Boolean = true,
  val courseId: Long = 1L,
  val courseCode: String = "RAD 101",
  val courseTitle: String = "Radiation Physics & Safety",
  val questionCount: Int = 15,
  val questionIds: List<Long> = emptyList()
)

data class MockCardConfig(
  val isConfigured: Boolean = true,
  val courseId: Long = 1L,
  val courseCode: String = "RAD 101",
  val courseTitle: String = "Radiation Physics & Safety",
  val examId: Long = 0L,
  val examTitle: String = "RAD 101 Mock Exam",
  val questionCount: Int = 15,
  val durationMinutes: Int = 30,
  val questionIds: List<Long> = emptyList()
)

data class StudentFeedbackItem(
  val id: String = "",
  val studentName: String = "",
  val studentEmail: String = "",
  val category: String = "Academic Inquiry",
  val message: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

data class SyncReport(
  val success: Boolean,
  val isPartial: Boolean = false,
  val questionsSynced: Int = 0,
  val materialsSynced: Int = 0,
  val examsSynced: Int = 0,
  val coursesSynced: Int = 0,
  val librariesSynced: Int = 0,
  val announcementsSynced: Int = 0,
  val failedCategories: List<String> = emptyList(),
  val message: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

class CloudSyncManager(
  private val context: Context,
  private val database: AppDatabase
) {
  private val prefs = context.getSharedPreferences("medrad_cloud_sync_prefs", Context.MODE_PRIVATE)
  private val scope = CoroutineScope(Dispatchers.IO)
  private val tag = "CloudSyncManager"

  val supabaseClient = SupabaseClient(context)

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val _lastSyncReport = MutableStateFlow<SyncReport?>(null)
  val lastSyncReport: StateFlow<SyncReport?> = _lastSyncReport.asStateFlow()

  private val _cloudStatusMessage = MutableStateFlow("Ready")
  val cloudStatusMessage: StateFlow<String> = _cloudStatusMessage.asStateFlow()

  private val _announcements = MutableStateFlow<List<CloudAnnouncement>>(emptyList())
  val announcements: StateFlow<List<CloudAnnouncement>> = _announcements.asStateFlow()

  private val _globalCardStyle = MutableStateFlow(prefs.getString("global_card_style", "NAVY_GOLD") ?: "NAVY_GOLD")
  val globalCardStyle: StateFlow<String> = _globalCardStyle.asStateFlow()

  private val _aboutAppContent = MutableStateFlow(
    AboutAppContent(
      description = prefs.getString("about_desc", null) ?: AboutAppContent().description,
      versionNotes = prefs.getString("about_version_notes", null) ?: AboutAppContent().versionNotes,
      departmentContact = prefs.getString("about_contact", null) ?: AboutAppContent().departmentContact,
      credits = prefs.getString("about_credits", null) ?: AboutAppContent().credits,
      hasUpdateNotice = prefs.getBoolean("has_update_notice", false),
      updateNotes = prefs.getString("update_notes", "") ?: ""
    )
  )
  val aboutAppContent: StateFlow<AboutAppContent> = _aboutAppContent.asStateFlow()

  private val _studentFeedbackList = MutableStateFlow<List<StudentFeedbackItem>>(emptyList())
  val studentFeedbackList: StateFlow<List<StudentFeedbackItem>> = _studentFeedbackList.asStateFlow()

  private val _dailySprintConfig = MutableStateFlow(
    DailyCbtSprintConfig(
      badge = prefs.getString("sprint_badge", "DAILY CBT SPRINT") ?: "DAILY CBT SPRINT",
      title = prefs.getString("sprint_title", "Radiography Challenge") ?: "Radiography Challenge",
      subtitle = prefs.getString("sprint_subtitle", "15 practice questions ready for your level") ?: "15 practice questions ready for your level",
      courseId = prefs.getLong("sprint_course_id", 1L),
      courseCode = prefs.getString("sprint_course_code", "RAD 101") ?: "RAD 101",
      questionCount = prefs.getInt("sprint_question_count", 15),
      activeStudentsText = prefs.getString("sprint_active_students", "38 students active") ?: "38 students active",
      theme = prefs.getString("sprint_theme", prefs.getString("global_card_style", "NAVY_GOLD") ?: "NAVY_GOLD") ?: "NAVY_GOLD",
      questionIds = prefs.getString("sprint_question_ids", "")?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    )
  )
  val dailySprintConfig: StateFlow<DailyCbtSprintConfig> = _dailySprintConfig.asStateFlow()

  private val _practiceCardConfig = MutableStateFlow(
    PracticeCardConfig(
      isConfigured = prefs.getBoolean("practice_card_is_active", true),
      courseId = prefs.getLong("practice_card_course_id", 1L),
      courseCode = prefs.getString("practice_card_course_code", "RAD 101") ?: "RAD 101",
      courseTitle = prefs.getString("practice_card_course_title", "Radiation Physics & Safety") ?: "Radiation Physics & Safety",
      questionCount = prefs.getInt("practice_card_question_count", 15),
      questionIds = prefs.getString("practice_card_question_ids", "")?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    )
  )
  val practiceCardConfig: StateFlow<PracticeCardConfig> = _practiceCardConfig.asStateFlow()

  private val _mockCardConfig = MutableStateFlow(
    MockCardConfig(
      isConfigured = prefs.getBoolean("mock_card_is_active", true),
      courseId = prefs.getLong("mock_card_course_id", 1L),
      courseCode = prefs.getString("mock_card_course_code", "RAD 101") ?: "RAD 101",
      courseTitle = prefs.getString("mock_card_course_title", "Radiation Physics & Safety") ?: "Radiation Physics & Safety",
      examId = prefs.getLong("mock_card_exam_id", 0L),
      examTitle = prefs.getString("mock_card_exam_title", "RAD 101 Mock Exam") ?: "RAD 101 Mock Exam",
      questionCount = prefs.getInt("mock_card_question_count", 15),
      durationMinutes = prefs.getInt("mock_card_duration_minutes", 30),
      questionIds = prefs.getString("mock_card_question_ids", "")?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    )
  )
  val mockCardConfig: StateFlow<MockCardConfig> = _mockCardConfig.asStateFlow()

  var realtimeManager: SupabaseRealtimeManager? = null
    private set

  private var networkCallback: ConnectivityManager.NetworkCallback? = null

  init {
    startRealtimeListeners()
    registerNetworkCallback()
    scope.launch {
      pullAppConfig()
      pullLatestFromCloud()
    }
  }

  private fun registerNetworkCallback() {
    try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
      val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
          Log.i(tag, "Network connection restored. Auto-syncing with Supabase...")
          scope.launch {
            startRealtimeListeners()
            pullAppConfig()
            pullLatestFromCloud()
          }
        }
      }
      networkCallback = callback
      cm.registerDefaultNetworkCallback(callback)
    } catch (e: Exception) {
      Log.w(tag, "Notice registering network callback: ${e.message}")
    }
  }

  fun getSupabaseProjectId(): String = SupabaseConfig.PROJECT_ID
  fun getSupabaseUrl(): String = SupabaseConfig.getUrl(context)
  fun setSupabaseUrl(url: String) = SupabaseConfig.setUrl(context, url)
  fun getSupabaseKey(): String = SupabaseConfig.getPublishableKey(context)
  fun setSupabaseKey(key: String) = SupabaseConfig.setPublishableKey(context, key)

  // Backward compatibility alias for existing admin screens
  fun getFirebaseProjectId(): String = SupabaseConfig.PROJECT_ID
  fun setFirebaseProjectId(id: String) {
    // Preserved for ViewModel compatibility
  }

  fun isNetworkAvailable(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  fun startRealtimeListeners() {
    realtimeManager?.stop()
    realtimeManager = SupabaseRealtimeManager(context, supabaseClient.okHttpClient) { table, eventType, record ->
      Log.i(tag, "Realtime event on $table: $eventType -> $record")
      handleRealtimeEvent(table, eventType, record)
    }
    realtimeManager?.start()
  }

  fun parseJsonId(record: JSONObject, key: String, defaultVal: Long = 0L): Long {
    if (!record.has(key)) return defaultVal
    val optLong = record.optLong(key, 0L)
    if (optLong > 0L) return optLong
    val rawStr = record.optString(key, "")
    if (rawStr.isNotBlank()) {
      val parsed = FlexibleIdHelper.toPositiveLong(rawStr, defaultVal)
      if (parsed > 0L) return parsed
    }
    return defaultVal
  }

  fun stopRealtimeListeners() {
    realtimeManager?.stop()
    realtimeManager = null
  }

  fun handleRealtimeEvent(table: String, eventType: String, record: JSONObject) {
    when (table) {
      "announcements", "notifications" -> {
        val title = record.optString("title")
        val id = record.optString("id", java.util.UUID.randomUUID().toString())
        if (eventType == "DELETE") {
          _announcements.value = _announcements.value.filter { it.id != id }
          kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            database.notificationDao().deleteNotificationById(id)
          }
        } else if (title.isNotBlank()) {
          val message = record.optString("message")
          val author = record.optString("author", "Department of Medical Radiography")
          val priority = record.optString("priority", "Normal")
          val target = record.optString("target", "All Students")
          val item = CloudAnnouncement(
            id = id,
            title = title,
            message = message,
            author = author,
            priority = priority,
            target = target
          )
          _announcements.value = listOf(item) + _announcements.value.filter { it.id != item.id }

          val entity = com.example.data.local.entity.NotificationEntity(
            id = id,
            title = title,
            message = message,
            category = record.optString("category", if (priority.contains("Exam", ignoreCase = true)) "Exam Alert" else "General Announcement"),
            priority = priority,
            target = target,
            author = author,
            isPublished = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSyncedWithCloud = true
          )
          kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            database.notificationDao().insertNotification(entity)
          }

          if (eventType == "INSERT" && prefs.getBoolean("pref_notifications_enabled", true)) {
            com.example.util.SystemNotificationHelper.showNotification(context, title, message, priority)
          }
        }
      }
      "app_config" -> {
        val cardStyle = record.optString("card_style")
        if (cardStyle.isNotBlank()) {
          _globalCardStyle.value = cardStyle
          prefs.edit().putString("global_card_style", cardStyle).apply()
        }
      }
    }
    // Execute synchronous DB entity updates immediately
    applyRealtimeRecordSync(table, eventType, record)
    scope.launch {
      handleRealtimeUpdate(table, eventType, record)
    }
  }

  private fun applyRealtimeRecordSync(table: String, eventType: String, record: JSONObject) {
    try {
      when (table) {
        "courses" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              val localCourse = remoteUuid?.let { database.academicDao().getCourseByRemoteId(it) }
              val id = localCourse?.id ?: parseJsonId(record, "id", 0L)
              if (id > 0) {
                database.academicDao().getCourseById(id)?.let { database.academicDao().deleteCourse(it) }
              }
            }
          } else if (record.has("code") && record.has("title")) {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              val existing = remoteUuid?.let { database.academicDao().getCourseByRemoteId(it) }
              val id = existing?.id ?: parseJsonId(record, "id", System.currentTimeMillis())
              val entity = CourseEntity(
                id = id,
                departmentId = parseJsonId(record, "department_id", 1L),
                code = record.optString("code"),
                title = record.optString("title"),
                level = record.optString("level", "100 Level"),
                semester = record.optString("semester", "First Semester"),
                description = record.optString("description", ""),
                remoteId = remoteUuid
              )
              database.academicDao().insertCourse(entity)
            }
          }
        }
        "exams" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              val localExam = remoteUuid?.let { database.examDao().getExamByRemoteId(it) }
              val id = localExam?.id ?: parseJsonId(record, "id", 0L)
              if (id > 0) {
                database.examDao().getExamById(id)?.let { database.examDao().deleteExam(it) }
              }
            }
          } else if (record.has("title")) {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
              val localCourseId = localCourse?.id ?: parseJsonId(record, "course_id", 1L)
              val existing = remoteUuid?.let { database.examDao().getExamByRemoteId(it) }
              val id = existing?.id ?: parseJsonId(record, "id", System.currentTimeMillis())
              val entity = ExamEntity(
                id = id,
                courseId = localCourseId,
                title = record.optString("title"),
                description = record.optString("description", ""),
                durationMinutes = record.optInt("duration_minutes", 30),
                totalQuestions = record.optInt("total_questions", 15),
                isPremium = record.optBoolean("is_premium", false),
                isPublished = record.optBoolean("is_published", true),
                remoteId = remoteUuid,
                remoteCourseId = courseRemoteId
              )
              database.examDao().insertExam(entity)
            }
          }
        }
        "questions" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          val examRemoteId = record.optString("exam_id").takeIf { it.isNotBlank() }
          val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              val localQ = remoteUuid?.let { database.questionDao().getQuestionByRemoteId(it) }
              val id = localQ?.id ?: parseJsonId(record, "id", 0L)
              if (id > 0) {
                database.questionDao().getQuestionById(id)?.let { database.questionDao().deleteQuestion(it) }
              }
            }
          } else if (record.has("question_text")) {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              val localExam = examRemoteId?.let { database.examDao().getExamByRemoteId(it) }
              val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
              val localCourseId = localExam?.courseId ?: localCourse?.id ?: parseJsonId(record, "course_id", 1L)
              val existing = remoteUuid?.let { database.questionDao().getQuestionByRemoteId(it) }
              val id = existing?.id ?: parseJsonId(record, "id", System.currentTimeMillis())
              val entity = QuestionEntity(
                id = id,
                courseId = localCourseId,
                topicId = parseJsonId(record, "topic_id", 1L),
                questionText = if (record.isNull("question_text")) "" else record.optString("question_text", ""),
                optionA = if (record.isNull("option_a")) "" else record.optString("option_a", ""),
                optionB = if (record.isNull("option_b")) "" else record.optString("option_b", ""),
                optionC = if (record.isNull("option_c")) "" else record.optString("option_c", ""),
                optionD = if (record.isNull("option_d")) "" else record.optString("option_d", ""),
                correctAnswer = record.optString("correct_answer", "A"),
                explanation = record.optString("explanation", "Refer to clinical radiology modules for verified rationale."),
                difficulty = record.optString("difficulty", "Medium"),
                isPremium = record.optBoolean("is_premium", false),
                isPublished = record.optBoolean("is_published", true),
                isDemo = record.optBoolean("is_demo", false),
                questionType = record.optString("question_type", "MULTIPLE_CHOICE"),
                assessmentTitle = record.optString("assessment_title", "Continuous Assessment"),
                marks = record.optInt("marks", 1),
                timeLimitSeconds = record.optInt("time_limit_seconds", 60),
                imageUrl = record.optString("image_url").takeIf { it.isNotBlank() },
                remoteId = remoteUuid,
                remoteExamId = examRemoteId,
                remoteCourseId = courseRemoteId
              )
              database.questionDao().insertQuestion(entity)
            }
          }
        }
        "study_materials" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              val localMat = remoteUuid?.let { database.studyMaterialDao().getMaterialByRemoteId(it) }
              val id = localMat?.id ?: parseJsonId(record, "id", 0L)
              if (id > 0) {
                database.studyMaterialDao().getMaterialById(id)?.let { database.studyMaterialDao().deleteMaterial(it) }
              }
            }
          } else {
            val title = record.optString("title")
            if (title.isNotBlank()) {
              kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
                val localCourseId = localCourse?.id ?: parseJsonId(record, "course_id", 1L)
                val existing = remoteUuid?.let { database.studyMaterialDao().getMaterialByRemoteId(it) }
                val id = existing?.id ?: parseJsonId(record, "id", System.currentTimeMillis())
                val entity = StudyMaterialEntity(
                  id = id,
                  courseId = localCourseId,
                  topicId = parseJsonId(record, "topic_id", 1L),
                  title = title,
                  description = record.optString("description", ""),
                  contentType = record.optString("content_type", "Lecture Note"),
                  contentBody = record.optString("content_body", ""),
                  externalUrl = (record.optString("external_url").takeIf { it.isNotBlank() } ?: record.optString("file_url")).takeIf { it.isNotBlank() },
                  isPremium = record.optBoolean("is_premium", false),
                  remoteId = remoteUuid,
                  remoteCourseId = courseRemoteId
                )
                database.studyMaterialDao().insertMaterial(entity)
              }
            }
          }
        }
        "course_libraries" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              val localLib = remoteUuid?.let { database.courseLibraryDao().getLibraryByRemoteId(it) }
              val id = localLib?.id ?: parseJsonId(record, "id", 0L)
              if (id > 0) {
                database.courseLibraryDao().getLibraryById(id)?.let { database.courseLibraryDao().deleteLibrary(it) }
              }
            }
          } else {
            val name = record.optString("name").ifBlank { record.optString("title") }
            val url = record.optString("external_url").ifBlank { record.optString("url") }
            if (name.isNotBlank() && url.isNotBlank()) {
              kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
                val localCourseId = localCourse?.id ?: parseJsonId(record, "course_id", 1L)
                val existing = remoteUuid?.let { database.courseLibraryDao().getLibraryByRemoteId(it) }
                val id = existing?.id ?: parseJsonId(record, "id", System.currentTimeMillis())
                val entity = CourseLibraryEntity(
                  id = id,
                  courseId = localCourseId,
                  courseCode = record.optString("course_code", "RAD 101"),
                  name = name,
                  description = record.optString("description", ""),
                  externalUrl = url,
                  isPublished = record.optBoolean("is_published", true),
                  remoteId = remoteUuid,
                  remoteCourseId = courseRemoteId
                )
                database.courseLibraryDao().insertLibrary(entity)
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(tag, "Error in applyRealtimeRecordSync: ${e.message}")
    }
  }

  private fun handleRealtimeUpdate(table: String, eventType: String, record: JSONObject) {
    scope.launch {
      when (table) {
        "announcements" -> {
          pullAnnouncements()
        }
        "app_config" -> {
          val cardStyle = record.optString("card_style")
          if (cardStyle.isNotBlank()) {
            _globalCardStyle.value = cardStyle
            prefs.edit().putString("global_card_style", cardStyle).apply()
          }
          val desc = record.optString("about_description")
          if (desc.isNotBlank()) {
            val updated = _aboutAppContent.value.copy(
              description = desc,
              versionNotes = record.optString("version_notes", _aboutAppContent.value.versionNotes),
              departmentContact = record.optString("department_contact", _aboutAppContent.value.departmentContact),
              credits = record.optString("credits", _aboutAppContent.value.credits),
              hasUpdateNotice = record.optBoolean("has_update_notice", false),
              updateNotes = record.optString("update_notes", "")
            )
            _aboutAppContent.value = updated
          }

          // Handle Realtime Daily CBT Sprint changes & Global Theme Sync
          val sprintTitle = record.optString("sprint_title")
          val sprintSubtitle = record.optString("sprint_subtitle")
          val sprintBadge = record.optString("sprint_badge")
          val sprintTheme = record.optString("sprint_theme")
          val effectiveTheme = if (sprintTheme.isNotBlank()) sprintTheme else cardStyle
          if (effectiveTheme.isNotBlank()) {
            _globalCardStyle.value = effectiveTheme
            _dailySprintConfig.value = _dailySprintConfig.value.copy(theme = effectiveTheme)
            prefs.edit()
              .putString("global_card_style", effectiveTheme)
              .putString("sprint_theme", effectiveTheme)
              .apply()
          }

          val sprintQIds = if (record.has("sprint_question_ids")) {
            record.optString("sprint_question_ids", "").split(",").mapNotNull { it.trim().toLongOrNull() }
          } else _dailySprintConfig.value.questionIds

          if (sprintTitle.isNotBlank() || sprintBadge.isNotBlank() || sprintSubtitle.isNotBlank() || record.has("sprint_course_id") || record.has("sprint_question_ids")) {
            val newSprint = _dailySprintConfig.value.copy(
              badge = if (sprintBadge.isNotBlank()) sprintBadge else _dailySprintConfig.value.badge,
              title = if (sprintTitle.isNotBlank()) sprintTitle else _dailySprintConfig.value.title,
              subtitle = if (sprintSubtitle.isNotBlank()) sprintSubtitle else _dailySprintConfig.value.subtitle,
              courseId = if (record.has("sprint_course_id")) parseJsonId(record, "sprint_course_id", 1L) else _dailySprintConfig.value.courseId,
              courseCode = if (record.has("sprint_course_code")) record.optString("sprint_course_code", "RAD 101") else _dailySprintConfig.value.courseCode,
              questionCount = if (record.has("sprint_question_count")) record.optInt("sprint_question_count", 15) else _dailySprintConfig.value.questionCount,
              activeStudentsText = if (record.has("sprint_active_students")) record.optString("sprint_active_students", _dailySprintConfig.value.activeStudentsText) else _dailySprintConfig.value.activeStudentsText,
              theme = if (effectiveTheme.isNotBlank()) effectiveTheme else _dailySprintConfig.value.theme,
              questionIds = sprintQIds
            )
            _dailySprintConfig.value = newSprint
            prefs.edit()
              .putString("sprint_badge", newSprint.badge)
              .putString("sprint_title", newSprint.title)
              .putString("sprint_subtitle", newSprint.subtitle)
              .putLong("sprint_course_id", newSprint.courseId)
              .putString("sprint_course_code", newSprint.courseCode)
              .putInt("sprint_question_count", newSprint.questionCount)
              .putString("sprint_active_students", newSprint.activeStudentsText)
              .putString("sprint_theme", newSprint.theme)
              .putString("sprint_question_ids", newSprint.questionIds.joinToString(","))
              .apply()
            Log.i(tag, "Realtime Daily CBT Sprint updated: ${newSprint.title} with theme ${newSprint.theme}")
          }

          if (record.has("practice_card_course_id") || record.has("practice_card_is_active") || record.has("practice_card_question_ids")) {
            val practiceQIds = if (record.has("practice_card_question_ids")) {
              record.optString("practice_card_question_ids", "").split(",").mapNotNull { it.trim().toLongOrNull() }
            } else _practiceCardConfig.value.questionIds

            val newPractice = _practiceCardConfig.value.copy(
              isConfigured = record.optBoolean("practice_card_is_active", _practiceCardConfig.value.isConfigured),
              courseId = parseJsonId(record, "practice_card_course_id", _practiceCardConfig.value.courseId),
              courseCode = record.optString("practice_card_course_code", _practiceCardConfig.value.courseCode),
              courseTitle = record.optString("practice_card_course_title", _practiceCardConfig.value.courseTitle),
              questionCount = record.optInt("practice_card_question_count", _practiceCardConfig.value.questionCount),
              questionIds = practiceQIds
            )
            _practiceCardConfig.value = newPractice
            prefs.edit()
              .putBoolean("practice_card_is_active", newPractice.isConfigured)
              .putLong("practice_card_course_id", newPractice.courseId)
              .putString("practice_card_course_code", newPractice.courseCode)
              .putString("practice_card_course_title", newPractice.courseTitle)
              .putInt("practice_card_question_count", newPractice.questionCount)
              .putString("practice_card_question_ids", newPractice.questionIds.joinToString(","))
              .apply()
          }

          if (record.has("mock_card_course_id") || record.has("mock_card_is_active") || record.has("mock_card_question_ids")) {
            val mockQIds = if (record.has("mock_card_question_ids")) {
              record.optString("mock_card_question_ids", "").split(",").mapNotNull { it.trim().toLongOrNull() }
            } else _mockCardConfig.value.questionIds

            val newMock = _mockCardConfig.value.copy(
              isConfigured = record.optBoolean("mock_card_is_active", _mockCardConfig.value.isConfigured),
              courseId = parseJsonId(record, "mock_card_course_id", _mockCardConfig.value.courseId),
              courseCode = record.optString("mock_card_course_code", _mockCardConfig.value.courseCode),
              courseTitle = record.optString("mock_card_course_title", _mockCardConfig.value.courseTitle),
              examId = parseJsonId(record, "mock_card_exam_id", _mockCardConfig.value.examId),
              examTitle = record.optString("mock_card_exam_title", _mockCardConfig.value.examTitle),
              questionCount = record.optInt("mock_card_question_count", _mockCardConfig.value.questionCount),
              durationMinutes = record.optInt("mock_card_duration_minutes", _mockCardConfig.value.durationMinutes),
              questionIds = mockQIds
            )
            _mockCardConfig.value = newMock
            prefs.edit()
              .putBoolean("mock_card_is_active", newMock.isConfigured)
              .putLong("mock_card_course_id", newMock.courseId)
              .putString("mock_card_course_code", newMock.courseCode)
              .putString("mock_card_course_title", newMock.courseTitle)
              .putLong("mock_card_exam_id", newMock.examId)
              .putString("mock_card_exam_title", newMock.examTitle)
              .putInt("mock_card_question_count", newMock.questionCount)
              .putInt("mock_card_duration_minutes", newMock.durationMinutes)
              .putString("mock_card_question_ids", newMock.questionIds.joinToString(","))
              .apply()
          }
        }
        "student_feedback" -> {
          pullStudentFeedback()
        }
      }
    }
  }

  suspend fun pullAppConfig(): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    val res = supabaseClient.restGet("app_config?select=*")
    res.fold(
      onSuccess = { json ->
        try {
          val array = JSONArray(json)
          for (i in 0 until array.length()) {
            val record = array.getJSONObject(i)
            val key = record.optString("key")
            val value = record.optString("value")
            if (key.isNotBlank()) {
              when (key) {
                "global_card_style", "card_style" -> {
                  _globalCardStyle.value = value
                  prefs.edit().putString("global_card_style", value).apply()
                }
                "about_description" -> {
                  _aboutAppContent.value = _aboutAppContent.value.copy(description = value)
                  prefs.edit().putString("about_desc", value).apply()
                }
                "sprint_title" -> {
                  _dailySprintConfig.value = _dailySprintConfig.value.copy(title = value)
                  prefs.edit().putString("sprint_title", value).apply()
                }
                "sprint_subtitle" -> {
                  _dailySprintConfig.value = _dailySprintConfig.value.copy(subtitle = value)
                  prefs.edit().putString("sprint_subtitle", value).apply()
                }
                "sprint_badge" -> {
                  _dailySprintConfig.value = _dailySprintConfig.value.copy(badge = value)
                  prefs.edit().putString("sprint_badge", value).apply()
                }
                "sprint_theme" -> {
                  _globalCardStyle.value = value
                  _dailySprintConfig.value = _dailySprintConfig.value.copy(theme = value)
                  prefs.edit().putString("global_card_style", value).putString("sprint_theme", value).apply()
                }
              }
            } else {
              handleRealtimeUpdate("app_config", "UPDATE", record)
            }
          }
          true
        } catch (e: Exception) {
          Log.e(tag, "Error parsing app_config: ${e.message}")
          false
        }
      },
      onFailure = { false }
    )
  }

  suspend fun pullAnnouncements(): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    val res = supabaseClient.restGet("announcements?select=*&order=created_at.desc&limit=50")
    if (res.isFailure) return@withContext false
    res.onSuccess { json ->
      try {
        val type = Types.newParameterizedType(List::class.java, SupabaseAnnouncement::class.java)
        val adapter = supabaseClient.moshi.adapter<List<SupabaseAnnouncement>>(type)
        val list = adapter.fromJson(json) ?: emptyList()
        val mapped = list.map {
          CloudAnnouncement(
            id = it.id,
            title = it.title ?: "Notice",
            message = it.message ?: "",
            author = it.author ?: "Department of Medical Radiography",
            priority = it.priority,
            target = it.target ?: "All Students",
            timestamp = System.currentTimeMillis()
          )
        }
        _announcements.value = mapped

        val entities = list.map {
          com.example.data.local.entity.NotificationEntity(
            id = it.id,
            title = it.title ?: "Notice",
            message = it.message ?: "",
            category = it.category ?: if (it.priority.contains("Exam", ignoreCase = true)) "Exam Alert" else "General Announcement",
            priority = it.priority,
            target = it.target ?: "All Students",
            author = it.author ?: "Department of Medical Radiography",
            isPublished = it.isPublished,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSyncedWithCloud = true
          )
        }
        if (entities.isNotEmpty()) {
          database.notificationDao().insertNotifications(entities)
        }
      } catch (e: Exception) {
        Log.e(tag, "Failed to parse announcements: ${e.message}")
      }
    }
    return@withContext true
  }

  suspend fun pullStudentFeedback() = withContext(Dispatchers.IO) {
    val res = supabaseClient.restGet("student_feedback?select=*&order=created_at.desc&limit=30")
    res.onSuccess { json ->
      try {
        val type = Types.newParameterizedType(List::class.java, SupabaseStudentFeedback::class.java)
        val adapter = supabaseClient.moshi.adapter<List<SupabaseStudentFeedback>>(type)
        val list = adapter.fromJson(json) ?: emptyList()
        val mapped = list.map {
          StudentFeedbackItem(
            id = it.id ?: UUID.randomUUID().toString(),
            studentName = it.studentName ?: "Student",
            studentEmail = it.studentEmail ?: "",
            category = it.subject ?: it.category ?: "Academic Inquiry",
            message = it.message ?: "",
            timestamp = System.currentTimeMillis()
          )
        }
        _studentFeedbackList.value = mapped
      } catch (e: Exception) {
        Log.e(tag, "Failed to parse feedback: ${e.message}")
      }
    }
  }

  suspend fun pullCourses(): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    try {
      var offset = 0
      val pageSize = 100
      var hasMore = true
      var totalProcessed = 0
      while (hasMore) {
        val res = supabaseClient.restGet("courses?select=*&order=id.asc&limit=$pageSize&offset=$offset")
        if (res.isFailure) {
          Log.e(tag, "pullCourses failed at offset $offset: ${res.exceptionOrNull()?.message}")
          return@withContext (totalProcessed > 0)
        }
        val json = res.getOrNull() ?: "[]"
        val type = Types.newParameterizedType(List::class.java, SupabaseCourse::class.java)
        val list = supabaseClient.moshi.adapter<List<SupabaseCourse>>(type).fromJson(json) ?: emptyList()
        if (list.isEmpty()) {
          hasMore = false
        } else {
          for (c in list) {
            val remoteUuid = c.id.toString()
            val existing = database.academicDao().getCourseByRemoteId(remoteUuid)
            val localId = existing?.id ?: FlexibleIdHelper.toPositiveLong(c.id, 0L)
            database.academicDao().insertCourse(
              CourseEntity(
                id = localId,
                departmentId = FlexibleIdHelper.toPositiveLong(c.departmentId, 1L),
                code = c.code ?: "RAD 101",
                title = c.title ?: "Academic Course",
                level = c.level ?: "100 Level",
                semester = c.semester ?: "First Semester",
                description = c.description ?: "",
                remoteId = remoteUuid
              )
            )
            totalProcessed++
          }
          if (list.size < pageSize) {
            hasMore = false
          } else {
            offset += pageSize
          }
        }
      }
      true
    } catch (e: Exception) {
      Log.e(tag, "Error parsing courses: ${e.message}")
      false
    }
  }

  suspend fun pullExams(): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    try {
      var offset = 0
      val pageSize = 100
      var hasMore = true
      var totalProcessed = 0
      while (hasMore) {
        val res = supabaseClient.restGet("exams?select=*&order=id.asc&limit=$pageSize&offset=$offset")
        if (res.isFailure) {
          Log.e(tag, "pullExams failed at offset $offset: ${res.exceptionOrNull()?.message}")
          return@withContext (totalProcessed > 0)
        }
        val json = res.getOrNull() ?: "[]"
        val type = Types.newParameterizedType(List::class.java, SupabaseExam::class.java)
        val list = supabaseClient.moshi.adapter<List<SupabaseExam>>(type).fromJson(json) ?: emptyList()
        if (list.isEmpty()) {
          hasMore = false
        } else {
          for (e in list) {
            val remoteUuid = e.id.toString()
            val courseRemoteId = e.courseId?.toString()
            val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
            val localCourseId = localCourse?.id ?: FlexibleIdHelper.toPositiveLong(e.courseId, 1L)
            val existing = database.examDao().getExamByRemoteId(remoteUuid)
            val localId = existing?.id ?: FlexibleIdHelper.toPositiveLong(e.id, 0L)
            database.examDao().insertExam(
              ExamEntity(
                id = localId,
                courseId = localCourseId,
                title = e.title ?: "Examination",
                description = e.description ?: "",
                durationMinutes = e.durationMinutes ?: 30,
                totalQuestions = maxOf((e.totalMarks ?: 30) / 2, 10),
                isPremium = false,
                isPublished = e.isPublished,
                remoteId = remoteUuid,
                remoteCourseId = courseRemoteId
              )
            )
            totalProcessed++
          }
          if (list.size < pageSize) {
            hasMore = false
          } else {
            offset += pageSize
          }
        }
      }
      true
    } catch (e: Exception) {
      Log.e(tag, "Error parsing exams: ${e.message}")
      false
    }
  }

  suspend fun pullQuestions(courseId: Long? = null): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    try {
      var offset = 0
      val pageSize = 100
      var hasMore = true
      var totalProcessed = 0
      while (hasMore) {
        val endpoint = if (courseId != null) {
          val courseEntity = database.academicDao().getCourseById(courseId)
          val cUuid = courseEntity?.remoteId ?: FlexibleIdHelper.toUuidString(courseId, "course")
          "questions?course_id=eq.$cUuid&select=*&order=id.asc&limit=$pageSize&offset=$offset"
        } else {
          "questions?select=*&order=id.asc&limit=$pageSize&offset=$offset"
        }
        val res = supabaseClient.restGet(endpoint)
        if (res.isFailure) {
          Log.e(tag, "pullQuestions failed at offset $offset: ${res.exceptionOrNull()?.message}")
          return@withContext (totalProcessed > 0)
        }
        val json = res.getOrNull() ?: "[]"
        val type = Types.newParameterizedType(List::class.java, SupabaseQuestion::class.java)
        val list = supabaseClient.moshi.adapter<List<SupabaseQuestion>>(type).fromJson(json) ?: emptyList()
        if (list.isEmpty()) {
          hasMore = false
        } else {
          for (q in list) {
            val remoteUuid = q.id.toString()
            val examRemoteId = q.examId?.toString()
            val courseRemoteId = q.courseId?.toString()
            val localExam = examRemoteId?.let { database.examDao().getExamByRemoteId(it) }
            val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
            val localCourseId = localExam?.courseId ?: localCourse?.id ?: courseId ?: 1L
            val existing = database.questionDao().getQuestionByRemoteId(remoteUuid)
            val localId = existing?.id ?: FlexibleIdHelper.toPositiveLong(q.id, 0L)

            database.questionDao().insertQuestion(
              QuestionEntity(
                id = localId,
                courseId = localCourseId,
                topicId = FlexibleIdHelper.toPositiveLong(q.topicId, 1L),
                questionText = q.questionText ?: "",
                optionA = q.optionA ?: "",
                optionB = q.optionB ?: "",
                optionC = q.optionC ?: "",
                optionD = q.optionD ?: "",
                correctAnswer = q.correctAnswer ?: "A",
                explanation = q.explanation ?: "Refer to clinical radiology modules for verified rationale.",
                difficulty = q.difficulty ?: "Medium",
                isPremium = q.isPremium ?: false,
                isPublished = q.isPublished ?: q.isActive,
                isDemo = q.isDemo ?: false,
                questionType = q.questionType ?: "MULTIPLE_CHOICE",
                assessmentTitle = q.assessmentTitle ?: "Continuous Assessment",
                marks = (q.marks ?: 1.0).toInt(),
                timeLimitSeconds = q.timeLimitSeconds ?: 60,
                imageUrl = q.imageUrl,
                remoteId = remoteUuid,
                remoteExamId = examRemoteId,
                remoteCourseId = courseRemoteId
              )
            )
            totalProcessed++
          }
          if (list.size < pageSize) {
            hasMore = false
          } else {
            offset += pageSize
          }
        }
      }
      true
    } catch (e: Exception) {
      Log.e(tag, "Error parsing questions: ${e.message}")
      false
    }
  }

  suspend fun pullStudyMaterials(courseId: Long? = null): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    try {
      var offset = 0
      val pageSize = 100
      var hasMore = true
      var totalProcessed = 0
      while (hasMore) {
        val endpoint = if (courseId != null) {
          val courseEntity = database.academicDao().getCourseById(courseId)
          val cUuid = courseEntity?.remoteId ?: FlexibleIdHelper.toUuidString(courseId, "course")
          "study_materials?course_id=eq.$cUuid&select=*&order=id.asc&limit=$pageSize&offset=$offset"
        } else {
          "study_materials?select=*&order=id.asc&limit=$pageSize&offset=$offset"
        }
        val res = supabaseClient.restGet(endpoint)
        if (res.isFailure) {
          Log.e(tag, "pullStudyMaterials failed at offset $offset: ${res.exceptionOrNull()?.message}")
          return@withContext (totalProcessed > 0)
        }
        val json = res.getOrNull() ?: "[]"
        val type = Types.newParameterizedType(List::class.java, SupabaseStudyMaterial::class.java)
        val list = supabaseClient.moshi.adapter<List<SupabaseStudyMaterial>>(type).fromJson(json) ?: emptyList()
        if (list.isEmpty()) {
          hasMore = false
        } else {
          for (m in list) {
            val remoteUuid = m.id.toString()
            val courseRemoteId = m.courseId?.toString()
            val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
            val localCourseId = localCourse?.id ?: FlexibleIdHelper.toPositiveLong(m.courseId, 1L)
            val existing = database.studyMaterialDao().getMaterialByRemoteId(remoteUuid)
            val localId = existing?.id ?: FlexibleIdHelper.toPositiveLong(m.id, 0L)
            database.studyMaterialDao().insertMaterial(
              StudyMaterialEntity(
                id = localId,
                courseId = localCourseId,
                topicId = FlexibleIdHelper.toPositiveLong(m.topicId, 1L),
                title = m.title ?: "Study Material",
                description = m.description ?: "",
                contentType = m.contentType ?: "Lecture Note",
                contentBody = m.contentBody ?: "",
                externalUrl = m.externalUrl ?: m.fileUrl,
                isPremium = m.isPremium ?: false,
                remoteId = remoteUuid,
                remoteCourseId = courseRemoteId
              )
            )
            totalProcessed++
          }
          if (list.size < pageSize) {
            hasMore = false
          } else {
            offset += pageSize
          }
        }
      }
      true
    } catch (e: Exception) {
      Log.e(tag, "Error parsing materials: ${e.message}")
      false
    }
  }

  suspend fun pullCourseLibraries(courseId: Long? = null): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    try {
      var offset = 0
      val pageSize = 100
      var hasMore = true
      var totalProcessed = 0
      while (hasMore) {
        val endpoint = if (courseId != null) {
          val courseEntity = database.academicDao().getCourseById(courseId)
          val cUuid = courseEntity?.remoteId ?: FlexibleIdHelper.toUuidString(courseId, "course")
          "course_libraries?course_id=eq.$cUuid&select=*&order=id.asc&limit=$pageSize&offset=$offset"
        } else {
          "course_libraries?select=*&order=id.asc&limit=$pageSize&offset=$offset"
        }
        val res = supabaseClient.restGet(endpoint)
        if (res.isFailure) {
          Log.e(tag, "pullCourseLibraries failed at offset $offset: ${res.exceptionOrNull()?.message}")
          return@withContext (totalProcessed > 0)
        }
        val json = res.getOrNull() ?: "[]"
        val type = Types.newParameterizedType(List::class.java, SupabaseCourseLibrary::class.java)
        val list = supabaseClient.moshi.adapter<List<SupabaseCourseLibrary>>(type).fromJson(json) ?: emptyList()
        if (list.isEmpty()) {
          hasMore = false
        } else {
          for (l in list) {
            val remoteUuid = l.id.toString()
            val courseRemoteId = l.courseId?.toString()
            val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
            val localCourseId = localCourse?.id ?: FlexibleIdHelper.toPositiveLong(l.courseId, 1L)
            val existing = database.courseLibraryDao().getLibraryByRemoteId(remoteUuid)
            val localId = existing?.id ?: FlexibleIdHelper.toPositiveLong(l.id, 0L)
            val libName = l.name ?: l.title ?: "Library Resource"
            val libUrl = l.url ?: l.externalUrl ?: ""
            if (libUrl.isNotBlank()) {
              database.courseLibraryDao().insertLibrary(
                CourseLibraryEntity(
                  id = localId,
                  courseId = localCourseId,
                  courseCode = l.courseCode ?: "RAD 101",
                  name = libName,
                  description = l.description ?: "",
                  externalUrl = libUrl,
                  isPublished = l.isPublished,
                  remoteId = remoteUuid,
                  remoteCourseId = courseRemoteId
                )
              )
              totalProcessed++
            }
          }
          if (list.size < pageSize) {
            hasMore = false
          } else {
            offset += pageSize
          }
        }
      }
      true
    } catch (e: Exception) {
      Log.e(tag, "Error parsing libraries: ${e.message}")
      false
    }
  }

  suspend fun pullLatestFromCloud(): SyncReport = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) {
      return@withContext SyncReport(false, message = "Offline cache active. Connect to internet to sync Supabase cloud.")
    }
    if (_isSyncing.value) {
      return@withContext _lastSyncReport.value ?: SyncReport(false, message = "Synchronization already in progress.")
    }

    _isSyncing.value = true
    _cloudStatusMessage.value = "Synchronizing with Supabase..."
    val failedCategories = mutableListOf<String>()

    try {
      // 1. Courses (Parent)
      val coursesOk = pullCourses()
      if (!coursesOk) failedCategories.add("Courses")

      // 2. Exams (Depends on Courses)
      val examsOk = pullExams()
      if (!examsOk) failedCategories.add("Exams")

      // 3. Questions (Depends on Exams and Courses) - Explicitly repaired!
      val questionsOk = pullQuestions()
      if (!questionsOk) failedCategories.add("Questions")

      // 4. Study Materials (Depends on Courses)
      val materialsOk = pullStudyMaterials()
      if (!materialsOk) failedCategories.add("Study Materials")

      // 5. Course Libraries (Depends on Courses)
      val librariesOk = pullCourseLibraries()
      if (!librariesOk) failedCategories.add("Course Libraries")

      // 6. Announcements (Broadcasts)
      val announcementsOk = pullAnnouncements()
      if (!announcementsOk) failedCategories.add("Announcements")

      // 7. App Configuration (Key-Value)
      val configOk = pullAppConfig()
      if (!configOk) failedCategories.add("App Config")

      // 8. Student Feedback
      pullStudentFeedback()

      val cCount = database.academicDao().getCoursesList().size
      val mCount = database.studyMaterialDao().getAllMaterialsList().size
      val lCount = database.courseLibraryDao().getAllLibrariesList().size
      val eCount = database.examDao().getAllExamsList().size
      val qCount = database.questionDao().getAllQuestionsList().size
      val aCount = database.notificationDao().getAllNotifications().size

      val allSuccess = failedCategories.isEmpty()
      val isPartial = failedCategories.isNotEmpty() && (coursesOk || examsOk || questionsOk || materialsOk || librariesOk || announcementsOk || configOk)

      val report = SyncReport(
        success = allSuccess,
        isPartial = isPartial,
        questionsSynced = qCount,
        materialsSynced = mCount,
        examsSynced = eCount,
        coursesSynced = cCount,
        librariesSynced = lCount,
        announcementsSynced = aCount,
        failedCategories = failedCategories,
        message = if (allSuccess) {
          "Authoritative Supabase sync complete: $cCount courses, $eCount exams, $qCount questions, $mCount materials, $lCount libraries loaded."
        } else if (isPartial) {
          "Partial sync with Supabase: Failed categories [${failedCategories.joinToString()}]. Retained local cache."
        } else {
          "Cloud sync failed. Displaying verified local cache."
        }
      )
      _lastSyncReport.value = report
      _cloudStatusMessage.value = if (allSuccess) {
        "Synced Live with Supabase at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}"
      } else if (isPartial) {
        "Partially synced at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}"
      } else {
        "Offline cache active"
      }
      report
    } catch (e: Exception) {
      Log.e(tag, "Full sync exception: ${e.message}")
      val report = SyncReport(false, message = "Supabase sync error: ${e.message}")
      _lastSyncReport.value = report
      report
    } finally {
      _isSyncing.value = false
    }
  }

  suspend fun pushAllContentToCloud(): SyncReport = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) {
      val report = SyncReport(false, message = "No internet connection. Saved locally in Room.")
      _lastSyncReport.value = report
      return@withContext report
    }

    _isSyncing.value = true
    _cloudStatusMessage.value = "Connecting to Supabase (${SupabaseConfig.PROJECT_ID})..."

    var qCount = 0
    var mCount = 0
    var lCount = 0
    var cCount = 0
    var eCount = 0
    var lastErrorMsg: String? = null

    try {
      // 1. Push Courses (Parent of all academic content)
      val courses = database.academicDao().getCoursesList()
      for (course in courses) {
        val remoteUuid = course.remoteId ?: UUID.randomUUID().toString()
        if (course.remoteId == null) {
          database.academicDao().insertCourse(course.copy(remoteId = remoteUuid))
        }
        val coursePayload = JSONObject().apply {
          put("id", remoteUuid)
          put("code", course.code)
          put("title", course.title)
          put("level", course.level)
          put("semester", course.semester)
          put("description", course.description)
          put("department_id", 1)
          put("is_published", true)
        }
        val res = supabaseClient.restPost("courses", coursePayload.toString(), upsert = true)
        if (res.isSuccess) {
          cCount++
        } else {
          lastErrorMsg = res.exceptionOrNull()?.message
        }
      }

      // 2. Push Exams (Depends on Courses)
      val allExams = database.examDao().getAllExamsList()
      for (ex in allExams) {
        val examRemoteUuid = ex.remoteId ?: UUID.randomUUID().toString()
        val parentCourse = database.academicDao().getCourseById(ex.courseId)
        val courseRemoteUuid = ex.remoteCourseId ?: parentCourse?.remoteId
        if (ex.remoteId == null || ex.remoteCourseId == null && courseRemoteUuid != null) {
          database.examDao().insertExam(ex.copy(remoteId = examRemoteUuid, remoteCourseId = courseRemoteUuid))
        }
        val exPayload = JSONObject().apply {
          put("id", examRemoteUuid)
          if (!courseRemoteUuid.isNullOrBlank()) {
            put("course_id", courseRemoteUuid)
          }
          put("title", ex.title)
          put("description", ex.description)
          put("duration_minutes", ex.durationMinutes)
          put("duration_seconds", ex.durationMinutes * 60)
          put("total_questions", ex.totalQuestions)
          put("total_marks", ex.totalQuestions * 2)
          put("pass_mark", 50.0)
          put("pass_percentage", 50)
          put("is_published", ex.isPublished)
        }
        val res = supabaseClient.restPost("exams", exPayload.toString(), upsert = true)
        if (res.isSuccess) {
          eCount++
        } else {
          lastErrorMsg = res.exceptionOrNull()?.message
        }
      }

      // 3. Push Study Materials (Depends on Courses)
      val materials = database.studyMaterialDao().getAllMaterialsList()
      for (mat in materials) {
        val matRemoteUuid = mat.remoteId ?: UUID.randomUUID().toString()
        val parentCourse = database.academicDao().getCourseById(mat.courseId)
        val courseRemoteUuid = mat.remoteCourseId ?: parentCourse?.remoteId
        if (mat.remoteId == null || mat.remoteCourseId == null && courseRemoteUuid != null) {
          database.studyMaterialDao().insertMaterial(mat.copy(remoteId = matRemoteUuid, remoteCourseId = courseRemoteUuid))
        }
        val matPayload = JSONObject().apply {
          put("id", matRemoteUuid)
          if (!courseRemoteUuid.isNullOrBlank()) {
            put("course_id", courseRemoteUuid)
          }
          put("topic_id", 1)
          put("title", mat.title)
          put("description", mat.description)
          put("content_type", mat.contentType)
          put("material_type", if (mat.contentType.contains("note", ignoreCase = true)) "lecture_note" else "handout")
          if (!mat.externalUrl.isNullOrBlank()) {
            put("external_url", mat.externalUrl)
            put("file_url", mat.externalUrl)
          }
          put("is_published", true)
        }
        val res = supabaseClient.restPost("study_materials", matPayload.toString(), upsert = true)
        if (res.isSuccess) {
          mCount++
        } else {
          lastErrorMsg = res.exceptionOrNull()?.message
        }
      }

      // 4. Push Course Libraries (Depends on Courses)
      val libraries = database.courseLibraryDao().getAllLibrariesList()
      for (lib in libraries) {
        val libRemoteUuid = lib.remoteId ?: UUID.randomUUID().toString()
        val parentCourse = database.academicDao().getCourseById(lib.courseId)
        val courseRemoteUuid = lib.remoteCourseId ?: parentCourse?.remoteId
        if (lib.remoteId == null || lib.remoteCourseId == null && courseRemoteUuid != null) {
          database.courseLibraryDao().insertLibrary(lib.copy(remoteId = libRemoteUuid, remoteCourseId = courseRemoteUuid))
        }
        val libPayload = JSONObject().apply {
          put("id", libRemoteUuid)
          if (!courseRemoteUuid.isNullOrBlank()) {
            put("course_id", courseRemoteUuid)
          }
          put("course_code", lib.courseCode.ifBlank { parentCourse?.code ?: "RAD 101" })
          put("title", lib.name)
          put("name", lib.name)
          put("description", lib.description)
          put("url", lib.externalUrl)
          put("external_url", lib.externalUrl)
          put("is_published", lib.isPublished)
        }
        val res = supabaseClient.restPost("course_libraries", libPayload.toString(), upsert = true)
        if (res.isSuccess) {
          lCount++
        } else {
          lastErrorMsg = res.exceptionOrNull()?.message
        }
      }

      // 5. Push Questions (Depends on Exams and Courses)
      val allQuestions = database.questionDao().getAllQuestionsList()
      for (q in allQuestions) {
        val qRemoteUuid = q.remoteId ?: UUID.randomUUID().toString()
        val parentCourse = database.academicDao().getCourseById(q.courseId)
        val courseRemoteUuid = q.remoteCourseId ?: parentCourse?.remoteId
        val examRemoteUuid = q.remoteExamId
        if (q.remoteId == null || (q.remoteCourseId == null && courseRemoteUuid != null)) {
          database.questionDao().insertQuestion(q.copy(remoteId = qRemoteUuid, remoteCourseId = courseRemoteUuid))
        }
        val qPayload = JSONObject().apply {
          put("id", qRemoteUuid)
          if (!courseRemoteUuid.isNullOrBlank()) {
            put("course_id", courseRemoteUuid)
          }
          if (!examRemoteUuid.isNullOrBlank()) {
            put("exam_id", examRemoteUuid)
          }
          put("topic_id", 1)
          put("question_text", q.questionText)
          put("option_a", q.optionA)
          put("option_b", q.optionB)
          if (q.optionC.isNotBlank()) put("option_c", q.optionC) else put("option_c", JSONObject.NULL)
          if (q.optionD.isNotBlank()) put("option_d", q.optionD) else put("option_d", JSONObject.NULL)
          put("correct_answer", q.correctAnswer)
          put("explanation", q.explanation)
          put("difficulty", q.difficulty)
          put("marks", q.marks.toDouble())
          put("time_limit_seconds", q.timeLimitSeconds)
          put("question_type", if (q.questionType.equals("MULTIPLE_CHOICE", ignoreCase = true)) "mcq" else q.questionType)
          put("is_active", true)
          put("is_published", q.isPublished)
          put("is_demo", q.isDemo)
          if (!q.imageUrl.isNullOrBlank()) {
            put("image_url", q.imageUrl)
          }
        }
        val res = supabaseClient.restPost("questions", qPayload.toString(), upsert = true)
        if (res.isSuccess) {
          qCount++
        } else {
          lastErrorMsg = res.exceptionOrNull()?.message
        }
      }

      _isSyncing.value = false
      val anySuccess = (cCount + mCount + lCount + qCount + eCount) > 0
      val report = if (anySuccess) {
        SyncReport(
          success = true,
          questionsSynced = qCount,
          materialsSynced = mCount,
          examsSynced = eCount,
          coursesSynced = cCount,
          message = "Pushed $cCount courses, $mCount materials, $lCount libraries, $eCount exams & $qCount questions LIVE to Supabase (${SupabaseConfig.PROJECT_ID})!"
        )
      } else {
        val isRls = lastErrorMsg?.contains("42501") == true || lastErrorMsg?.contains("row-level security") == true
        val msg = if (isRls) {
          "Cloud write blocked by Supabase Row-Level Security (RLS). All items remain safely stored in Room. Run the SQL migration script in Supabase SQL Editor to allow public writes."
        } else {
          "Cloud sync notice: ${lastErrorMsg ?: "Could not write to Supabase"}. All content remains safe in local Room storage."
        }
        SyncReport(
          success = false,
          message = msg
        )
      }
      _cloudStatusMessage.value = if (anySuccess) {
        "Synced Live to Supabase at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}"
      } else {
        "Local Room Cache Active (Cloud writes restricted by RLS policy)"
      }
      _lastSyncReport.value = report
      report
    } catch (e: Exception) {
      _isSyncing.value = false
      val report = SyncReport(false, message = "Supabase push notice: ${e.message}. Saved locally in Room.")
      _lastSyncReport.value = report
      report
    }
  }

  suspend fun broadcastAnnouncement(
    title: String,
    message: String,
    priority: String = "Normal",
    author: String = "Department of Medical Radiography",
    target: String = "All Students"
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val id = java.util.UUID.randomUUID().toString()
      val announcement = SupabaseAnnouncement(
        id = id,
        title = title,
        message = message,
        author = author,
        priority = priority,
        target = target
      )
      val json = supabaseClient.moshi.adapter(SupabaseAnnouncement::class.java).toJson(announcement)
      var res = supabaseClient.restPost("announcements", json, upsert = true)

      // Fallback for missing author or target column in Supabase schema
      if (res.isFailure) {
        val err = res.exceptionOrNull()?.message ?: ""
        if (err.contains("PGRST204") || err.contains("Could not find the") || err.contains("does not exist")) {
          val corePayload = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("message", "[Audience: $target]\n\n$message")
            put("priority", priority)
            put("is_published", true)
          }
          res = supabaseClient.restPost("announcements", corePayload.toString(), upsert = true)
        }
      }

      val isCloudSuccess = res.isSuccess

      // Also add to local state and Room DB immediately
      val localItem = CloudAnnouncement(
        id = id,
        title = title,
        message = message,
        author = author,
        priority = priority,
        target = target,
        timestamp = System.currentTimeMillis()
      )
      _announcements.value = listOf(localItem) + _announcements.value.filter { it.id != id }

      val entity = com.example.data.local.entity.NotificationEntity(
        id = id,
        title = title,
        message = message,
        category = if (priority.contains("Exam", ignoreCase = true)) "Exam Alert" else "General Announcement",
        priority = priority,
        target = target,
        author = author,
        isPublished = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isSyncedWithCloud = isCloudSuccess
      )
      database.notificationDao().insertNotification(entity)

      // Realtime websocket pipeline broadcast across all active clients
      if (isCloudSuccess) {
        realtimeManager?.dispatchRealtimeEvent("announcements", "INSERT", JSONObject(json))
        if (prefs.getBoolean("pref_notifications_enabled", true)) {
          com.example.util.SystemNotificationHelper.showNotification(context, title, message, priority)
        }
        Result.success(true)
      } else {
        val err = res.exceptionOrNull()?.message ?: "Unknown cloud write error"
        if (err.contains("42501") || err.contains("row-level security")) {
          Result.failure(Exception("Cloud write blocked by Supabase Row-Level Security (RLS). Saved in Room cache. Run migration SQL script in Supabase SQL Editor to enable public writes."))
        } else {
          Result.failure(Exception("Cloud broadcast notice: $err. Saved locally in Room."))
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      database.notificationDao().deleteNotificationById(announcementId)
      val res = supabaseClient.restDelete("announcements?id=eq.$announcementId")
      _announcements.value = _announcements.value.filter { it.id != announcementId }
      realtimeManager?.dispatchRealtimeEvent("announcements", "DELETE", JSONObject().put("id", announcementId))
      Result.success(true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun setGlobalCardStyle(style: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      _globalCardStyle.value = style
      _dailySprintConfig.value = _dailySprintConfig.value.copy(theme = style)
      prefs.edit()
        .putString("global_card_style", style)
        .putString("sprint_theme", style)
        .apply()
      val array = JSONArray().apply {
        put(JSONObject().put("key", "card_style").put("value", style))
        put(JSONObject().put("key", "sprint_theme").put("value", style))
      }
      supabaseClient.restPost("app_config", array.toString(), upsert = true)
      realtimeManager?.dispatchRealtimeEvent("app_config", "UPDATE", JSONObject().put("card_style", style))
      Result.success(true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateAboutAppContent(content: AboutAppContent): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      _aboutAppContent.value = content
      prefs.edit()
        .putString("about_desc", content.description)
        .putString("about_version_notes", content.versionNotes)
        .putString("about_contact", content.departmentContact)
        .putString("about_credits", content.credits)
        .putBoolean("has_update_notice", content.hasUpdateNotice)
        .putString("update_notes", content.updateNotes)
        .apply()

      val array = JSONArray().apply {
        put(JSONObject().put("key", "about_description").put("value", content.description))
        put(JSONObject().put("key", "version_notes").put("value", content.versionNotes))
        put(JSONObject().put("key", "department_contact").put("value", content.departmentContact))
        put(JSONObject().put("key", "credits").put("value", content.credits))
        put(JSONObject().put("key", "has_update_notice").put("value", content.hasUpdateNotice.toString()))
        put(JSONObject().put("key", "update_notes").put("value", content.updateNotes))
      }
      supabaseClient.restPost("app_config", array.toString(), upsert = true)

      val eventObj = JSONObject().apply {
        put("about_description", content.description)
        put("version_notes", content.versionNotes)
        put("department_contact", content.departmentContact)
        put("credits", content.credits)
        put("has_update_notice", content.hasUpdateNotice)
        put("update_notes", content.updateNotes)
      }
      realtimeManager?.dispatchRealtimeEvent("app_config", "UPDATE", eventObj)

      Result.success(true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun submitFeedback(
    studentName: String,
    studentEmail: String,
    category: String,
    message: String
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("subject", category)
        put("message", "From: $studentName ($studentEmail)\n\n$message")
      }
      supabaseClient.restPost("student_feedback", payload.toString(), upsert = false)

      val localItem = StudentFeedbackItem(
        id = UUID.randomUUID().toString(),
        studentName = studentName,
        studentEmail = studentEmail,
        category = category,
        message = message,
        timestamp = System.currentTimeMillis()
      )
      _studentFeedbackList.value = listOf(localItem) + _studentFeedbackList.value
      Result.success(true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun broadcastQuestion(q: QuestionEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = q.remoteId ?: UUID.randomUUID().toString()
      val parentCourse = database.academicDao().getCourseById(q.courseId)
      val courseRemoteUuid = q.remoteCourseId ?: parentCourse?.remoteId
      val examRemoteUuid = q.remoteExamId
      if (q.remoteId == null || (q.remoteCourseId == null && courseRemoteUuid != null)) {
        database.questionDao().insertQuestion(q.copy(remoteId = remoteUuid, remoteCourseId = courseRemoteUuid))
      }
      val qDto = SupabaseQuestion(
        id = remoteUuid,
        examId = examRemoteUuid,
        courseId = courseRemoteUuid,
        topicId = 1,
        questionText = q.questionText,
        optionA = q.optionA,
        optionB = q.optionB,
        optionC = q.optionC.takeIf { it.isNotBlank() },
        optionD = q.optionD.takeIf { it.isNotBlank() },
        correctAnswer = q.correctAnswer,
        explanation = q.explanation,
        difficulty = q.difficulty,
        questionType = if (q.questionType.equals("MULTIPLE_CHOICE", ignoreCase = true)) "mcq" else q.questionType,
        assessmentTitle = q.assessmentTitle,
        marks = q.marks.toDouble(),
        timeLimitSeconds = q.timeLimitSeconds,
        imageUrl = q.imageUrl,
        isPremium = q.isPremium,
        isPublished = q.isPublished,
        isDemo = q.isDemo
      )
      val json = supabaseClient.moshi.adapter(SupabaseQuestion::class.java).toJson(qDto)
      val res = supabaseClient.restPost("questions", json, upsert = true)
      realtimeManager?.dispatchRealtimeEvent("questions", "INSERT", JSONObject(json))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun broadcastMaterial(mat: StudyMaterialEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = mat.remoteId ?: UUID.randomUUID().toString()
      val parentCourse = database.academicDao().getCourseById(mat.courseId)
      val courseRemoteUuid = mat.remoteCourseId ?: parentCourse?.remoteId
      if (mat.remoteId == null || (mat.remoteCourseId == null && courseRemoteUuid != null)) {
        database.studyMaterialDao().insertMaterial(mat.copy(remoteId = remoteUuid, remoteCourseId = courseRemoteUuid))
      }
      val mDto = SupabaseStudyMaterial(
        id = remoteUuid,
        courseId = courseRemoteUuid ?: mat.courseId,
        topicId = 1,
        title = mat.title,
        description = mat.description,
        contentType = mat.contentType,
        contentBody = mat.contentBody,
        externalUrl = mat.externalUrl,
        isPremium = mat.isPremium
      )
      val json = supabaseClient.moshi.adapter(SupabaseStudyMaterial::class.java).toJson(mDto)
      val res = supabaseClient.restPost("study_materials", json, upsert = true)
      realtimeManager?.dispatchRealtimeEvent("study_materials", "INSERT", JSONObject(json))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun broadcastLibrary(lib: CourseLibraryEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = lib.remoteId ?: UUID.randomUUID().toString()
      val parentCourse = database.academicDao().getCourseById(lib.courseId)
      val courseRemoteUuid = lib.remoteCourseId ?: parentCourse?.remoteId
      if (lib.remoteId == null || (lib.remoteCourseId == null && courseRemoteUuid != null)) {
        database.courseLibraryDao().insertLibrary(lib.copy(remoteId = remoteUuid, remoteCourseId = courseRemoteUuid))
      }
      val lDto = SupabaseCourseLibrary(
        id = remoteUuid,
        courseId = courseRemoteUuid ?: lib.courseId,
        courseCode = lib.courseCode,
        name = lib.name,
        title = lib.name,
        description = lib.description,
        url = lib.externalUrl,
        externalUrl = lib.externalUrl,
        isPublished = lib.isPublished
      )
      val json = supabaseClient.moshi.adapter(SupabaseCourseLibrary::class.java).toJson(lDto)
      val res = supabaseClient.restPost("course_libraries", json, upsert = true)
      realtimeManager?.dispatchRealtimeEvent("course_libraries", "INSERT", JSONObject(json))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun broadcastCourse(course: CourseEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = course.remoteId ?: UUID.randomUUID().toString()
      if (course.remoteId == null) {
        database.academicDao().insertCourse(course.copy(remoteId = remoteUuid))
      }
      val cDto = SupabaseCourse(
        id = remoteUuid,
        departmentId = 1,
        code = course.code,
        title = course.title,
        level = course.level,
        semester = course.semester,
        description = course.description
      )
      val json = supabaseClient.moshi.adapter(SupabaseCourse::class.java).toJson(cDto)
      val res = supabaseClient.restPost("courses", json, upsert = true)
      realtimeManager?.dispatchRealtimeEvent("courses", "INSERT", JSONObject(json))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteCourseCloud(courseId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val course = database.academicDao().getCourseById(courseId)
      val targetId = course?.remoteId ?: courseId.toString()
      val res = supabaseClient.restDelete("courses?id=eq.$targetId")
      realtimeManager?.dispatchRealtimeEvent("courses", "DELETE", JSONObject().put("id", targetId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun broadcastExam(exam: ExamEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = exam.remoteId ?: UUID.randomUUID().toString()
      val parentCourse = database.academicDao().getCourseById(exam.courseId)
      val courseRemoteUuid = exam.remoteCourseId ?: parentCourse?.remoteId
      if (exam.remoteId == null || (exam.remoteCourseId == null && courseRemoteUuid != null)) {
        database.examDao().insertExam(exam.copy(remoteId = remoteUuid, remoteCourseId = courseRemoteUuid))
      }
      val eDto = SupabaseExam(
        id = remoteUuid,
        courseId = courseRemoteUuid ?: exam.courseId,
        title = exam.title,
        description = exam.description,
        durationMinutes = exam.durationMinutes,
        totalMarks = exam.totalQuestions * 2,
        passPercentage = 50,
        isPublished = exam.isPublished
      )
      val json = supabaseClient.moshi.adapter(SupabaseExam::class.java).toJson(eDto)
      val res = supabaseClient.restPost("exams", json, upsert = true)
      realtimeManager?.dispatchRealtimeEvent("exams", "INSERT", JSONObject(json))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteExamCloud(examId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val exam = database.examDao().getExamById(examId)
      val targetId = exam?.remoteId ?: examId.toString()
      val res = supabaseClient.restDelete("exams?id=eq.$targetId")
      realtimeManager?.dispatchRealtimeEvent("exams", "DELETE", JSONObject().put("id", targetId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteQuestionCloud(questionId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val q = database.questionDao().getQuestionById(questionId)
      val targetId = q?.remoteId ?: questionId.toString()
      val res = supabaseClient.restDelete("questions?id=eq.$targetId")
      realtimeManager?.dispatchRealtimeEvent("questions", "DELETE", JSONObject().put("id", targetId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteMaterialCloud(materialId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val mat = database.studyMaterialDao().getMaterialById(materialId)
      val targetId = mat?.remoteId ?: materialId.toString()
      val res = supabaseClient.restDelete("study_materials?id=eq.$targetId")
      realtimeManager?.dispatchRealtimeEvent("study_materials", "DELETE", JSONObject().put("id", targetId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteLibraryCloud(libraryId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val lib = database.courseLibraryDao().getLibraryById(libraryId)
      val targetId = lib?.remoteId ?: libraryId.toString()
      val res = supabaseClient.restDelete("course_libraries?id=eq.$targetId")
      realtimeManager?.dispatchRealtimeEvent("course_libraries", "DELETE", JSONObject().put("id", targetId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateAboutAppManagement(content: AboutAppContent): Result<Boolean> = updateAboutAppContent(content)

  suspend fun sendPushNotification(title: String, message: String, target: String, priority: String): Result<Boolean> =
    broadcastAnnouncement(title, message, priority = priority, target = target)

  suspend fun submitStudentFeedback(
    studentName: String,
    studentEmail: String,
    category: String,
    message: String
  ): Result<Boolean> = submitFeedback(studentName, studentEmail, category, message)

  suspend fun exportStudentScores(courseTitle: String?): String = withContext(Dispatchers.IO) {
    exportTranscriptForStudent("All Radiography Students", courseTitle ?: "Department Summary")
  }

  suspend fun exportQuestionsToJson(): String = withContext(Dispatchers.IO) {
    val questions = database.questionDao().getAllQuestionsList()
    val type = Types.newParameterizedType(List::class.java, QuestionEntity::class.java)
    val adapter = supabaseClient.moshi.adapter<List<QuestionEntity>>(type)
    adapter.toJson(questions)
  }

  suspend fun importQuestionsFromJson(json: String): Result<Int> = withContext(Dispatchers.IO) {
    try {
      val type = Types.newParameterizedType(List::class.java, QuestionEntity::class.java)
      val adapter = supabaseClient.moshi.adapter<List<QuestionEntity>>(type)
      val list = adapter.fromJson(json) ?: emptyList()
      var count = 0
      for (q in list) {
        database.questionDao().insertQuestion(q)
        count++
      }
      Result.success(count)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun exportTranscriptForStudent(studentName: String, matricNo: String): String = withContext(Dispatchers.IO) {
    val sb = StringBuilder()
    sb.append("UNIVERSITY OF MAIDUGURI\n")
    sb.append("COLLEGE OF MEDICAL SCIENCES - DEPARTMENT OF MEDICAL RADIOGRAPHY\n")
    sb.append("OFFICIAL CBT PRACTICE & CONTINUOUS ASSESSMENT TRANSCRIPT\n")
    sb.append("=========================================================================\n")
    sb.append("Student Name : $studentName\n")
    sb.append("Matric Number: $matricNo\n")
    sb.append("Generated On : ${SimpleDateFormat("EEEE, dd MMMM yyyy hh:mm a", Locale.getDefault()).format(Date())}\n")
    sb.append("Platform     : MedRad Study Squad (Supabase Cloud Connected)\n")
    sb.append("-------------------------------------------------------------------------\n\n")

    val attempts = database.examDao().getAllAttemptsList()
    if (attempts.isEmpty()) {
      sb.append("No official exam attempts recorded yet in local/cloud database.\n")
    } else {
      sb.append(String.format("%-6s | %-10s | %-12s | %-8s | %-6s | %-8s\n", "No.", "Score (%)", "Result", "Total Qs", "Time(m)", "Date"))
      sb.append("-------------------------------------------------------------------------\n")
      attempts.forEachIndexed { idx, att ->
        val pct = (att.score * 100) / maxOf(att.totalQuestions, 1)
        val passedStr = if (pct >= 50) "PASSED" else "NEEDS WORK"
        val timeMins = att.timeSpentSeconds / 60
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(att.timestamp))
        sb.append(String.format("%-6d | %-10d | %-12s | %-8d | %-6d | %-8s\n", idx + 1, pct, passedStr, att.totalQuestions, timeMins, dateStr))
      }
    }
    sb.append("\n=========================================================================\n")
    sb.append("End of Departmental Examination Transcript. Authorized by Examination Officer.\n")
    sb.toString()
  }

  suspend fun updateDailySprintConfig(config: DailyCbtSprintConfig): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      _dailySprintConfig.value = config
      _globalCardStyle.value = config.theme
      prefs.edit()
        .putString("sprint_badge", config.badge)
        .putString("sprint_title", config.title)
        .putString("sprint_subtitle", config.subtitle)
        .putLong("sprint_course_id", config.courseId)
        .putString("sprint_course_code", config.courseCode)
        .putInt("sprint_question_count", config.questionCount)
        .putString("sprint_active_students", config.activeStudentsText)
        .putString("sprint_theme", config.theme)
        .putString("global_card_style", config.theme)
        .putString("sprint_question_ids", config.questionIds.joinToString(","))
        .apply()

      val array = JSONArray().apply {
        put(JSONObject().put("key", "card_style").put("value", config.theme))
        put(JSONObject().put("key", "sprint_theme").put("value", config.theme))
        put(JSONObject().put("key", "sprint_badge").put("value", config.badge))
        put(JSONObject().put("key", "sprint_title").put("value", config.title))
        put(JSONObject().put("key", "sprint_subtitle").put("value", config.subtitle))
        put(JSONObject().put("key", "sprint_course_id").put("value", config.courseId.toString()))
        put(JSONObject().put("key", "sprint_course_code").put("value", config.courseCode))
        put(JSONObject().put("key", "sprint_question_count").put("value", config.questionCount.toString()))
        put(JSONObject().put("key", "sprint_active_students").put("value", config.activeStudentsText))
        put(JSONObject().put("key", "sprint_question_ids").put("value", config.questionIds.joinToString(",")))
      }
      val result = supabaseClient.restPost("app_config", array.toString(), upsert = true)

      // Instantly dispatch to Realtime subscribers
      realtimeManager?.dispatchRealtimeEvent("app_config", "UPDATE", JSONObject().put("sprint_theme", config.theme))

      if (result.isSuccess) Result.success(true) else Result.failure(result.exceptionOrNull() ?: Exception("Failed to update daily sprint"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updatePracticeCardConfig(config: PracticeCardConfig): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      _practiceCardConfig.value = config
      prefs.edit()
        .putBoolean("practice_card_is_active", config.isConfigured)
        .putLong("practice_card_course_id", config.courseId)
        .putString("practice_card_course_code", config.courseCode)
        .putString("practice_card_course_title", config.courseTitle)
        .putInt("practice_card_question_count", config.questionCount)
        .putString("practice_card_question_ids", config.questionIds.joinToString(","))
        .apply()

      val array = JSONArray().apply {
        put(JSONObject().put("key", "practice_card_is_active").put("value", config.isConfigured.toString()))
        put(JSONObject().put("key", "practice_card_course_id").put("value", config.courseId.toString()))
        put(JSONObject().put("key", "practice_card_course_code").put("value", config.courseCode))
        put(JSONObject().put("key", "practice_card_course_title").put("value", config.courseTitle))
        put(JSONObject().put("key", "practice_card_question_count").put("value", config.questionCount.toString()))
        put(JSONObject().put("key", "practice_card_question_ids").put("value", config.questionIds.joinToString(",")))
      }
      val result = supabaseClient.restPost("app_config", array.toString(), upsert = true)
      realtimeManager?.dispatchRealtimeEvent("app_config", "UPDATE", JSONObject().put("practice_card_is_active", config.isConfigured))

      if (result.isSuccess) Result.success(true) else Result.failure(result.exceptionOrNull() ?: Exception("Failed to update practice card"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateMockCardConfig(config: MockCardConfig): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      _mockCardConfig.value = config
      prefs.edit()
        .putBoolean("mock_card_is_active", config.isConfigured)
        .putLong("mock_card_course_id", config.courseId)
        .putString("mock_card_course_code", config.courseCode)
        .putString("mock_card_course_title", config.courseTitle)
        .putLong("mock_card_exam_id", config.examId)
        .putString("mock_card_exam_title", config.examTitle)
        .putInt("mock_card_question_count", config.questionCount)
        .putInt("mock_card_duration_minutes", config.durationMinutes)
        .putString("mock_card_question_ids", config.questionIds.joinToString(","))
        .apply()

      val array = JSONArray().apply {
        put(JSONObject().put("key", "mock_card_is_active").put("value", config.isConfigured.toString()))
        put(JSONObject().put("key", "mock_card_course_id").put("value", config.courseId.toString()))
        put(JSONObject().put("key", "mock_card_course_code").put("value", config.courseCode))
        put(JSONObject().put("key", "mock_card_course_title").put("value", config.courseTitle))
        put(JSONObject().put("key", "mock_card_exam_id").put("value", config.examId.toString()))
        put(JSONObject().put("key", "mock_card_exam_title").put("value", config.examTitle))
        put(JSONObject().put("key", "mock_card_question_count").put("value", config.questionCount.toString()))
        put(JSONObject().put("key", "mock_card_duration_minutes").put("value", config.durationMinutes.toString()))
        put(JSONObject().put("key", "mock_card_question_ids").put("value", config.questionIds.joinToString(",")))
      }
      val result = supabaseClient.restPost("app_config", array.toString(), upsert = true)
      realtimeManager?.dispatchRealtimeEvent("app_config", "UPDATE", JSONObject().put("mock_card_is_active", config.isConfigured))

      if (result.isSuccess) Result.success(true) else Result.failure(result.exceptionOrNull() ?: Exception("Failed to update mock card"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun testSupabaseConnection(): String = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) {
      return@withContext "Network offline. Device is not connected to the internet."
    }
    val getRes = supabaseClient.restGet("courses?select=id&limit=1")
    if (getRes.isFailure) {
      return@withContext "Cannot reach Supabase REST API (${SupabaseConfig.PROJECT_ID}). Check URL or connectivity."
    }
    val testPayload = JSONArray().apply {
      put(JSONObject().put("key", "test_connection").put("value", System.currentTimeMillis().toString()))
    }
    val postRes = supabaseClient.restPost("app_config", testPayload.toString(), upsert = true)
    return@withContext if (postRes.isSuccess) {
      "CONNECTED: Read & Write active on Supabase (${SupabaseConfig.PROJECT_ID}). Real-time cloud sync is fully operational!"
    } else {
      val err = postRes.exceptionOrNull()?.message ?: "RLS restricted"
      if (err.contains("42501") || err.contains("row-level security")) {
        "READ-ONLY ACCESS: Connected to Supabase (${SupabaseConfig.PROJECT_ID}), but writes are restricted by Row-Level Security (RLS). Run the SQL migration script in your Supabase dashboard to enable cloud updates."
      } else {
        "CONNECTED (Read Active): Remote database reachable. Write status: $err"
      }
    }
  }
}
