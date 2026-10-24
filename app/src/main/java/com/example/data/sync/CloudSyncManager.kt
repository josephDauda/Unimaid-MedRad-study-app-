package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.ExamAttemptEntity
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
import com.example.data.local.entity.UserEntity
import com.example.data.supabase.SupabaseProfile
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
  val title: String = "Gross Anatomy Sprint",
  val subtitle: String = "Practice questions ready for your level",
  val courseId: Long = 1L,
  val courseCode: String = "ANA 201",
  val questionCount: Int = 0,
  val activeStudentsText: String = "Students active",
  val theme: String = "NAVY_GOLD",
  val questionIds: List<Long> = emptyList()
)

data class PracticeCardConfig(
  val isConfigured: Boolean = true,
  val courseId: Long = 1L,
  val courseCode: String = "ANA 201",
  val courseTitle: String = "GROSS ANATOMY OF UPPER AND LOWER LIMBS",
  val questionCount: Int = 0,
  val questionIds: List<Long> = emptyList()
)

data class MockCardConfig(
  val isConfigured: Boolean = true,
  val courseId: Long = 1L,
  val courseCode: String = "ANA 201",
  val courseTitle: String = "GROSS ANATOMY OF UPPER AND LOWER LIMBS",
  val examId: Long = 0L,
  val examTitle: String = "ANA 201 Mock Exam",
  val questionCount: Int = 0,
  val durationMinutes: Int = 30,
  val questionIds: List<Long> = emptyList(),
  val revealMode: String = "INSTANT", // "INSTANT" or "DEADLINE"
  val deadlineString: String = "",
  val deadlineTimestamp: Long = 0L,
  val sessionId: String = "Session 1"
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
      title = prefs.getString("sprint_title", "Gross Anatomy Sprint") ?: "Gross Anatomy Sprint",
      subtitle = prefs.getString("sprint_subtitle", "Practice questions ready for your level") ?: "Practice questions ready for your level",
      courseId = prefs.getLong("sprint_course_id", 1L),
      courseCode = prefs.getString("sprint_course_code", "ANA 201") ?: "ANA 201",
      questionCount = prefs.getInt("sprint_question_count", 0),
      activeStudentsText = prefs.getString("sprint_active_students", "Students active") ?: "Students active",
      theme = prefs.getString("sprint_theme", prefs.getString("global_card_style", "NAVY_GOLD") ?: "NAVY_GOLD") ?: "NAVY_GOLD",
      questionIds = prefs.getString("sprint_question_ids", "")?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    )
  )
  val dailySprintConfig: StateFlow<DailyCbtSprintConfig> = _dailySprintConfig.asStateFlow()

  private val _practiceCardConfig = MutableStateFlow(
    PracticeCardConfig(
      isConfigured = prefs.getBoolean("practice_card_is_active", true),
      courseId = prefs.getLong("practice_card_course_id", 1L),
      courseCode = prefs.getString("practice_card_course_code", "ANA 201") ?: "ANA 201",
      courseTitle = prefs.getString("practice_card_course_title", "GROSS ANATOMY OF UPPER AND LOWER LIMBS") ?: "GROSS ANATOMY OF UPPER AND LOWER LIMBS",
      questionCount = prefs.getInt("practice_card_question_count", 0),
      questionIds = prefs.getString("practice_card_question_ids", "")?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    )
  )
  val practiceCardConfig: StateFlow<PracticeCardConfig> = _practiceCardConfig.asStateFlow()

  private val _mockCardConfig = MutableStateFlow(
    MockCardConfig(
      isConfigured = prefs.getBoolean("mock_card_is_active", true),
      courseId = prefs.getLong("mock_card_course_id", 1L),
      courseCode = prefs.getString("mock_card_course_code", "ANA 201") ?: "ANA 201",
      courseTitle = prefs.getString("mock_card_course_title", "GROSS ANATOMY OF UPPER AND LOWER LIMBS") ?: "GROSS ANATOMY OF UPPER AND LOWER LIMBS",
      examId = prefs.getLong("mock_card_exam_id", 0L),
      examTitle = prefs.getString("mock_card_exam_title", "ANA 201 Mock Exam") ?: "ANA 201 Mock Exam",
      questionCount = prefs.getInt("mock_card_question_count", 0),
      durationMinutes = prefs.getInt("mock_card_duration_minutes", 30),
      questionIds = prefs.getString("mock_card_question_ids", "")?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList(),
      revealMode = prefs.getString("mock_card_reveal_mode", "INSTANT") ?: "INSTANT",
      deadlineString = prefs.getString("mock_card_deadline_string", "") ?: "",
      deadlineTimestamp = prefs.getLong("mock_card_deadline_timestamp", 0L),
      sessionId = prefs.getString("mock_card_session_id", "Session 1") ?: "Session 1"
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

  fun parseTimestamp(ts: String?): Long {
    if (ts.isNullOrBlank()) return System.currentTimeMillis()
    val formats = listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd HH:mm:ss"
    )
    for (fmt in formats) {
      try {
        val sdf = SimpleDateFormat(fmt, Locale.US)
        val date = sdf.parse(ts)
        if (date != null) return date.time
      } catch (_: Exception) {}
    }
    return System.currentTimeMillis()
  }

  fun handleRealtimeEvent(table: String, eventType: String, record: JSONObject) {
    if (table == "announcements" || table == "notifications") {
      val title = record.optString("title")
      val id = record.optString("id", java.util.UUID.randomUUID().toString())
      if (eventType == "DELETE") {
        _announcements.value = _announcements.value.filter { it.id != id }
      } else if (title.isNotBlank()) {
        val message = record.optString("message")
        val author = record.optString("author", "Department of Medical Radiography")
        val priority = record.optString("priority", "Normal")
        val target = record.optString("target", "All Students")
        val createdTs = parseTimestamp(record.optString("created_at"))
        val item = CloudAnnouncement(
          id = id,
          title = title,
          message = message,
          author = author,
          priority = priority,
          target = target,
          timestamp = createdTs
        )
        _announcements.value = listOf(item) + _announcements.value.filter { it.id != item.id }
      }
    }

    try {
      kotlinx.coroutines.runBlocking(Dispatchers.IO) {
        if (table == "announcements" || table == "notifications") {
          val title = record.optString("title")
          val id = record.optString("id", java.util.UUID.randomUUID().toString())
          if (eventType == "DELETE") {
            database.notificationDao().deleteNotificationById(id)
          } else if (title.isNotBlank()) {
            val message = record.optString("message")
            val author = record.optString("author", "Department of Medical Radiography")
            val priority = record.optString("priority", "Normal")
            val target = record.optString("target", "All Students")
            val createdTs = parseTimestamp(record.optString("created_at"))
            val updatedTs = parseTimestamp(record.optString("updated_at"))
            val entity = com.example.data.local.entity.NotificationEntity(
              id = id,
              title = title,
              message = message,
              category = record.optString("category", if (priority.contains("Exam", ignoreCase = true)) "Exam Alert" else "General Announcement"),
              priority = priority,
              target = target,
              author = author,
              isPublished = true,
              createdAt = createdTs,
              updatedAt = updatedTs,
              isSyncedWithCloud = true
            )
            database.notificationDao().insertNotification(entity)

            if (eventType == "INSERT" && prefs.getBoolean("pref_notifications_enabled", true)) {
              com.example.util.SystemNotificationHelper.showNotification(context, title, message, priority)
            }
          }
        }

        applyRealtimeRecordSync(table, eventType, record)
        handleRealtimeUpdate(table, eventType, record)
      }
    } catch (e: Exception) {
      Log.e(tag, "Error handling realtime event synchronously: ${e.message}")
    }
  }

  private suspend fun applyRealtimeRecordSync(table: String, eventType: String, record: JSONObject) = withContext(Dispatchers.IO) {
    try {
      when (table) {
        "courses" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            val localCourse = remoteUuid?.let { database.academicDao().getCourseByRemoteId(it) }
            val id = localCourse?.id ?: parseJsonId(record, "id", 0L)
            if (id > 0) {
              database.academicDao().getCourseById(id)?.let { database.academicDao().deleteCourse(it) }
            }
          } else if (record.has("code") && record.has("title")) {
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
        "exams" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            val localExam = remoteUuid?.let { database.examDao().getExamByRemoteId(it) }
            val id = localExam?.id ?: parseJsonId(record, "id", 0L)
            if (id > 0) {
              database.examDao().getExamById(id)?.let { database.examDao().deleteExam(it) }
            }
          } else if (record.has("title")) {
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
        "questions" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          val examRemoteId = record.optString("exam_id").takeIf { it.isNotBlank() }
          val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            val localQ = remoteUuid?.let { database.questionDao().getQuestionByRemoteId(it) }
            val id = localQ?.id ?: parseJsonId(record, "id", 0L)
            if (id > 0) {
              database.questionDao().getQuestionById(id)?.let { database.questionDao().deleteQuestion(it) }
            }
          } else if (record.has("question_text")) {
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
        "study_materials" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            val localMat = remoteUuid?.let { database.studyMaterialDao().getMaterialByRemoteId(it) }
            val id = localMat?.id ?: parseJsonId(record, "id", 0L)
            if (id > 0) {
              database.studyMaterialDao().getMaterialById(id)?.let { database.studyMaterialDao().deleteMaterial(it) }
            }
          } else {
            val title = record.optString("title")
            if (title.isNotBlank()) {
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
        "course_libraries" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            val localLib = remoteUuid?.let { database.courseLibraryDao().getLibraryByRemoteId(it) }
            val id = localLib?.id ?: parseJsonId(record, "id", 0L)
            if (id > 0) {
              database.courseLibraryDao().getLibraryById(id)?.let { database.courseLibraryDao().deleteLibrary(it) }
            }
          } else {
            val name = record.optString("name").ifBlank { record.optString("title") }
            val url = record.optString("external_url").ifBlank { record.optString("url") }
            if (name.isNotBlank() && url.isNotBlank()) {
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
        "exam_attempts" -> {
          val remoteUuid = record.optString("id").takeIf { it.isNotBlank() }
          if (eventType == "DELETE") {
            val localAttempt = remoteUuid?.let { database.examDao().getAttemptByRemoteId(it) }
            val id = localAttempt?.id ?: parseJsonId(record, "id", 0L)
            if (id > 0) {
              // Deleted remote attempt
            }
          } else if (remoteUuid != null) {
            val examRemoteId = record.optString("exam_id").takeIf { it.isNotBlank() }
            val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
            val localExam = examRemoteId?.let { database.examDao().getExamByRemoteId(it) }
            val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
            val localExamId = localExam?.id ?: parseJsonId(record, "exam_id", 0L)
            val localCourseId = localExam?.courseId ?: localCourse?.id ?: 1L
            val studentUuid = record.optString("student_id").ifBlank { record.optString("user_id") }
            val localUser = if (studentUuid.isNotBlank()) database.userDao().getUserByEmail(studentUuid) else null
            val localUserId = localUser?.id ?: FlexibleIdHelper.toPositiveLong(studentUuid, 1L)

            val existing = database.examDao().getAttemptByRemoteId(remoteUuid)
            val localId = existing?.id ?: parseJsonId(record, "id", System.currentTimeMillis())
            val score = record.optInt("score", 0)
            val totalQ = record.optInt("total_questions", localExam?.totalQuestions ?: 15)
            val durationSec = record.optInt("duration_seconds", record.optInt("time_spent_seconds", 0))

            database.examDao().insertAttempt(
              ExamAttemptEntity(
                id = localId,
                userId = localUserId,
                examId = localExamId,
                courseTitle = localCourse?.title ?: "Medical Radiography",
                examTitle = localExam?.title ?: "CBT Examination",
                score = score,
                totalQuestions = totalQ,
                correctAnswers = score,
                incorrectAnswers = maxOf(0, totalQ - score),
                unansweredQuestions = 0,
                timeSpentSeconds = durationSec,
                answersJson = existing?.answersJson ?: "{}",
                timestamp = parseTimestamp(record.optString("created_at")),
                remoteId = remoteUuid,
                remoteExamId = examRemoteId,
                remoteCourseId = courseRemoteId
              )
            )
          }
        }
      }
    } catch (e: Exception) {
      Log.e(tag, "Error in applyRealtimeRecordSync: ${e.message}")
    }
  }

  fun applyAppConfigKeyValue(key: String, value: String) {
    if (key.isBlank()) return
    when (key) {
      "global_card_style", "card_style" -> {
        _globalCardStyle.value = value
        _dailySprintConfig.value = _dailySprintConfig.value.copy(theme = value)
        prefs.edit().putString("global_card_style", value).putString("sprint_theme", value).apply()
      }
      "sprint_theme" -> {
        _globalCardStyle.value = value
        _dailySprintConfig.value = _dailySprintConfig.value.copy(theme = value)
        prefs.edit().putString("global_card_style", value).putString("sprint_theme", value).apply()
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
      "sprint_course_id" -> {
        val cid = value.toLongOrNull() ?: 1L
        _dailySprintConfig.value = _dailySprintConfig.value.copy(courseId = cid)
        prefs.edit().putLong("sprint_course_id", cid).apply()
      }
      "sprint_course_code" -> {
        _dailySprintConfig.value = _dailySprintConfig.value.copy(courseCode = value)
        prefs.edit().putString("sprint_course_code", value).apply()
      }
      "sprint_question_count" -> {
        val count = value.toIntOrNull() ?: 15
        _dailySprintConfig.value = _dailySprintConfig.value.copy(questionCount = count)
        prefs.edit().putInt("sprint_question_count", count).apply()
      }
      "sprint_active_students" -> {
        _dailySprintConfig.value = _dailySprintConfig.value.copy(activeStudentsText = value)
        prefs.edit().putString("sprint_active_students", value).apply()
      }
      "sprint_question_ids" -> {
        val ids = if (value.isNotBlank()) value.split(",").mapNotNull { it.trim().toLongOrNull() } else emptyList()
        _dailySprintConfig.value = _dailySprintConfig.value.copy(questionIds = ids)
        prefs.edit().putString("sprint_question_ids", value).apply()
      }
      "practice_card_is_active" -> {
        val active = value.toBoolean()
        _practiceCardConfig.value = _practiceCardConfig.value.copy(isConfigured = active)
        prefs.edit().putBoolean("practice_card_is_active", active).apply()
      }
      "practice_card_course_id" -> {
        val cid = value.toLongOrNull() ?: 1L
        _practiceCardConfig.value = _practiceCardConfig.value.copy(courseId = cid)
        prefs.edit().putLong("practice_card_course_id", cid).apply()
      }
      "practice_card_course_code" -> {
        _practiceCardConfig.value = _practiceCardConfig.value.copy(courseCode = value)
        prefs.edit().putString("practice_card_course_code", value).apply()
      }
      "practice_card_course_title" -> {
        _practiceCardConfig.value = _practiceCardConfig.value.copy(courseTitle = value)
        prefs.edit().putString("practice_card_course_title", value).apply()
      }
      "practice_card_question_count" -> {
        val count = value.toIntOrNull() ?: 15
        _practiceCardConfig.value = _practiceCardConfig.value.copy(questionCount = count)
        prefs.edit().putInt("practice_card_question_count", count).apply()
      }
      "practice_card_question_ids" -> {
        val ids = if (value.isNotBlank()) value.split(",").mapNotNull { it.trim().toLongOrNull() } else emptyList()
        _practiceCardConfig.value = _practiceCardConfig.value.copy(questionIds = ids)
        prefs.edit().putString("practice_card_question_ids", value).apply()
      }
      "mock_card_is_active" -> {
        val active = value.toBoolean()
        _mockCardConfig.value = _mockCardConfig.value.copy(isConfigured = active)
        prefs.edit().putBoolean("mock_card_is_active", active).apply()
      }
      "mock_card_course_id" -> {
        val cid = value.toLongOrNull() ?: 1L
        _mockCardConfig.value = _mockCardConfig.value.copy(courseId = cid)
        prefs.edit().putLong("mock_card_course_id", cid).apply()
      }
      "mock_card_course_code" -> {
        _mockCardConfig.value = _mockCardConfig.value.copy(courseCode = value)
        prefs.edit().putString("mock_card_course_code", value).apply()
      }
      "mock_card_course_title" -> {
        _mockCardConfig.value = _mockCardConfig.value.copy(courseTitle = value)
        prefs.edit().putString("mock_card_course_title", value).apply()
      }
      "mock_card_exam_id" -> {
        val eid = value.toLongOrNull() ?: 0L
        _mockCardConfig.value = _mockCardConfig.value.copy(examId = eid)
        prefs.edit().putLong("mock_card_exam_id", eid).apply()
      }
      "mock_card_exam_title" -> {
        _mockCardConfig.value = _mockCardConfig.value.copy(examTitle = value)
        prefs.edit().putString("mock_card_exam_title", value).apply()
      }
      "mock_card_question_count" -> {
        val count = value.toIntOrNull() ?: 15
        _mockCardConfig.value = _mockCardConfig.value.copy(questionCount = count)
        prefs.edit().putInt("mock_card_question_count", count).apply()
      }
      "mock_card_duration_minutes" -> {
        val dur = value.toIntOrNull() ?: 30
        _mockCardConfig.value = _mockCardConfig.value.copy(durationMinutes = dur)
        prefs.edit().putInt("mock_card_duration_minutes", dur).apply()
      }
      "mock_card_question_ids" -> {
        val ids = if (value.isNotBlank()) value.split(",").mapNotNull { it.trim().toLongOrNull() } else emptyList()
        _mockCardConfig.value = _mockCardConfig.value.copy(questionIds = ids)
        prefs.edit().putString("mock_card_question_ids", value).apply()
      }
      "mock_card_reveal_mode" -> {
        _mockCardConfig.value = _mockCardConfig.value.copy(revealMode = value)
        prefs.edit().putString("mock_card_reveal_mode", value).apply()
      }
      "mock_card_deadline_string" -> {
        _mockCardConfig.value = _mockCardConfig.value.copy(deadlineString = value)
        prefs.edit().putString("mock_card_deadline_string", value).apply()
      }
      "mock_card_deadline_timestamp" -> {
        val ts = value.toLongOrNull() ?: 0L
        _mockCardConfig.value = _mockCardConfig.value.copy(deadlineTimestamp = ts)
        prefs.edit().putLong("mock_card_deadline_timestamp", ts).apply()
      }
      "mock_card_session_id" -> {
        _mockCardConfig.value = _mockCardConfig.value.copy(sessionId = value)
        prefs.edit().putString("mock_card_session_id", value).apply()
      }
      "about_description" -> {
        _aboutAppContent.value = _aboutAppContent.value.copy(description = value)
        prefs.edit().putString("about_desc", value).apply()
      }
      "version_notes" -> {
        _aboutAppContent.value = _aboutAppContent.value.copy(versionNotes = value)
        prefs.edit().putString("about_version_notes", value).apply()
      }
      "department_contact" -> {
        _aboutAppContent.value = _aboutAppContent.value.copy(departmentContact = value)
        prefs.edit().putString("about_contact", value).apply()
      }
      "credits" -> {
        _aboutAppContent.value = _aboutAppContent.value.copy(credits = value)
        prefs.edit().putString("about_credits", value).apply()
      }
      "has_update_notice" -> {
        val hasNotice = value.toBoolean()
        _aboutAppContent.value = _aboutAppContent.value.copy(hasUpdateNotice = hasNotice)
        prefs.edit().putBoolean("has_update_notice", hasNotice).apply()
      }
      "update_notes" -> {
        _aboutAppContent.value = _aboutAppContent.value.copy(updateNotes = value)
        prefs.edit().putString("update_notes", value).apply()
      }
    }
  }

  private suspend fun handleRealtimeUpdate(table: String, eventType: String, record: JSONObject) = withContext(Dispatchers.IO) {
    when (table) {
      "announcements" -> {
        pullAnnouncements()
      }
      "exam_attempts" -> {
        pullExamAttempts()
      }
      "app_config" -> {
        if (record.has("key") && record.has("value")) {
          applyAppConfigKeyValue(record.optString("key"), record.optString("value"))
        } else {
          val keys = record.keys()
          while (keys.hasNext()) {
            val k = keys.next()
            applyAppConfigKeyValue(k, record.optString(k))
          }
        }
      }
      "student_feedback" -> {
        pullStudentFeedback()
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
              applyAppConfigKeyValue(key, value)
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
          val createdTs = parseTimestamp(it.createdAt)
          CloudAnnouncement(
            id = it.id,
            title = it.title ?: "Notice",
            message = it.message ?: "",
            author = it.author ?: "Department of Medical Radiography",
            priority = it.priority,
            target = it.target ?: "All Students",
            timestamp = createdTs
          )
        }
        _announcements.value = mapped

        val entities = list.map {
          val createdTs = parseTimestamp(it.createdAt)
          val updatedTs = parseTimestamp(it.updatedAt)
          com.example.data.local.entity.NotificationEntity(
            id = it.id,
            title = it.title ?: "Notice",
            message = it.message ?: "",
            category = it.category ?: if (it.priority.contains("Exam", ignoreCase = true)) "Exam Alert" else "General Announcement",
            priority = it.priority,
            target = it.target ?: "All Students",
            author = it.author ?: "Department of Medical Radiography",
            isPublished = it.isPublished,
            createdAt = createdTs,
            updatedAt = updatedTs,
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

  suspend fun pullProfiles(): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    try {
      val res = supabaseClient.restGet("profiles?select=*&order=created_at.desc")
      if (res.isFailure) {
        Log.e(tag, "pullProfiles failed: ${res.exceptionOrNull()?.message}")
        return@withContext false
      }
      val json = res.getOrNull() ?: "[]"
      val type = Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
      val list = supabaseClient.moshi.adapter<List<SupabaseProfile>>(type).fromJson(json) ?: emptyList()
      for (p in list) {
        val email = p.email?.trim()?.lowercase() ?: continue
        val resolvedRole = if (p.appRole?.equals("admin", ignoreCase = true) == true || email == "admin@medrad.edu.ng") "ADMIN" else "STUDENT"
        val existing = database.userDao().getUserByEmail(email)
        if (existing != null) {
          val updated = existing.copy(
            fullName = p.fullName?.ifBlank { existing.fullName } ?: existing.fullName,
            role = resolvedRole,
            department = p.department ?: existing.department,
            faculty = p.faculty ?: existing.faculty,
            level = p.level ?: existing.level,
            isActive = p.isActive,
            isPremium = if (resolvedRole == "ADMIN") true else existing.isPremium
          )
          database.userDao().updateUser(updated)
        } else {
          val newUser = UserEntity(
            fullName = p.fullName ?: "Student",
            email = email,
            phoneNumber = "",
            passwordHash = "",
            role = resolvedRole,
            adminRole = if (email == "admin@medrad.edu.ng") "PRIMARY" else "SECONDARY",
            permissions = if (resolvedRole == "ADMIN") "ALL" else "",
            isActive = p.isActive,
            university = "University of Maiduguri",
            faculty = p.faculty ?: "College of Medical Sciences",
            department = p.department ?: "Medical Radiography",
            level = p.level ?: "100 Level",
            semester = "Full Session",
            isPremium = resolvedRole == "ADMIN"
          )
          database.userDao().insertUser(newUser)
        }
      }
      true
    } catch (e: Exception) {
      Log.e(tag, "Error parsing profiles: ${e.message}")
      false
    }
  }

  suspend fun pullExamAttempts(): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    try {
      val res = supabaseClient.restGet("exam_attempts?select=*&order=created_at.desc&limit=100")
      if (res.isFailure) {
        Log.e(tag, "pullExamAttempts failed: ${res.exceptionOrNull()?.message}")
        return@withContext false
      }
      val json = res.getOrNull() ?: "[]"
      val array = JSONArray(json)
      for (i in 0 until array.length()) {
        val record = array.getJSONObject(i)
        val remoteUuid = record.optString("id").takeIf { it.isNotBlank() } ?: continue
        val examRemoteId = record.optString("exam_id").takeIf { it.isNotBlank() }
        val courseRemoteId = record.optString("course_id").takeIf { it.isNotBlank() }
        val localExam = examRemoteId?.let { database.examDao().getExamByRemoteId(it) }
        val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
        val localExamId = localExam?.id ?: parseJsonId(record, "exam_id", 0L)
        val studentUuid = record.optString("student_id").ifBlank { record.optString("user_id") }
        val localUser = if (studentUuid.isNotBlank()) database.userDao().getUserByEmail(studentUuid) else null
        val localUserId = localUser?.id ?: FlexibleIdHelper.toPositiveLong(studentUuid, 1L)

        val existing = database.examDao().getAttemptByRemoteId(remoteUuid)
        val localId = existing?.id ?: parseJsonId(record, "id", 0L).let { if (it > 0) it else 0L }
        val score = record.optInt("score", 0)
        val totalQ = record.optInt("total_questions", localExam?.totalQuestions ?: 15)
        val durationSec = record.optInt("duration_seconds", record.optInt("time_spent_seconds", 0))

        val resolvedCourse = localCourse ?: (localExam?.courseId?.let { database.academicDao().getCourseById(it) })
        val resolvedCourseTitle = resolvedCourse?.let { "${it.code} - ${it.title}" }
          ?: record.optString("course_title").ifBlank { "Medical Radiography" }

        val answersJsonStr = if (existing != null && existing.answersJson.isNotBlank() && existing.answersJson != "{}") {
          existing.answersJson
        } else {
          val metaObj = JSONObject().apply {
            put("session_id", record.optString("session_id", "Session 1"))
            if (localUser != null) {
              put("student_name", localUser.fullName)
              put("student_email", localUser.email)
              put("student_level", localUser.level)
            }
          }
          JSONObject().apply { put("_meta", metaObj) }.toString()
        }

        val attemptEntity = ExamAttemptEntity(
          id = if (localId > 0) localId else 0L,
          userId = localUserId,
          examId = if (localExamId > 0) localExamId else null,
          courseTitle = resolvedCourseTitle,
          examTitle = localExam?.title ?: record.optString("exam_title").ifBlank { "CBT Examination" },
          score = score,
          totalQuestions = totalQ,
          correctAnswers = score,
          incorrectAnswers = maxOf(0, totalQ - score),
          unansweredQuestions = 0,
          timeSpentSeconds = durationSec,
          answersJson = answersJsonStr,
          timestamp = parseTimestamp(record.optString("created_at")),
          remoteId = remoteUuid,
          remoteExamId = examRemoteId,
          remoteCourseId = courseRemoteId
        )
        database.examDao().insertAttempt(attemptEntity)
      }
      true
    } catch (e: Exception) {
      Log.e(tag, "Error parsing exam attempts: ${e.message}")
      false
    }
  }

  suspend fun pullCourses(): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    try {
      var offset = 0
      val pageSize = 100
      var hasMore = true
      var totalProcessed = 0
      val remoteCourseUuids = mutableSetOf<String>()
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
            remoteCourseUuids.add(remoteUuid)
            val existingByRemote = database.academicDao().getCourseByRemoteId(remoteUuid)
            val existingByCode = if (existingByRemote == null && !c.code.isNullOrBlank()) {
              database.academicDao().getCourseByCode(c.code)
            } else null
            val localId = existingByRemote?.id ?: existingByCode?.id ?: FlexibleIdHelper.toPositiveLong(c.id, 0L)
            database.academicDao().insertCourse(
              CourseEntity(
                id = localId,
                departmentId = FlexibleIdHelper.toPositiveLong(c.departmentId, 1L),
                code = c.code ?: "ANA 201",
                title = c.title ?: "Academic Course",
                level = c.level ?: "200 Level",
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
      val localCourses = database.academicDao().getCoursesList()
      for (lc in localCourses) {
        if (lc.remoteId != null && !remoteCourseUuids.contains(lc.remoteId)) {
          database.academicDao().deleteCourse(lc)
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
      val remoteExamUuids = mutableSetOf<String>()
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
            remoteExamUuids.add(remoteUuid)
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
      val localExams = database.examDao().getAllExamsList()
      for (le in localExams) {
        if (le.remoteId != null && !remoteExamUuids.contains(le.remoteId)) {
          database.examDao().deleteExam(le)
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
      val remoteQuestionUuids = mutableSetOf<String>()
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
            remoteQuestionUuids.add(remoteUuid)
            val examRemoteId = q.examId?.toString()
            val courseRemoteId = q.courseId?.toString()
            val localCourse = courseRemoteId?.let { database.academicDao().getCourseByRemoteId(it) }
            val localExam = examRemoteId?.let { database.examDao().getExamByRemoteId(it) }
            val localCourseId = localCourse?.id ?: localExam?.courseId ?: courseId ?: 1L
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
      val localQuestions = if (courseId != null) database.questionDao().getAllQuestionsList().filter { it.courseId == courseId } else database.questionDao().getAllQuestionsList()
      for (lq in localQuestions) {
        if (lq.remoteId != null && !remoteQuestionUuids.contains(lq.remoteId)) {
          database.questionDao().deleteQuestion(lq)
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
      val remoteMaterialUuids = mutableSetOf<String>()
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
            remoteMaterialUuids.add(remoteUuid)
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
      val localMaterials = if (courseId != null) database.studyMaterialDao().getAllMaterialsList().filter { it.courseId == courseId } else database.studyMaterialDao().getAllMaterialsList()
      for (lm in localMaterials) {
        if (lm.remoteId != null && !remoteMaterialUuids.contains(lm.remoteId)) {
          database.studyMaterialDao().deleteMaterial(lm)
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
      val remoteLibUuids = mutableSetOf<String>()
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
            remoteLibUuids.add(remoteUuid)
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
                  courseCode = l.courseCode ?: "ANA 201",
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
      val localLibs = if (courseId != null) database.courseLibraryDao().getAllLibrariesList().filter { it.courseId == courseId } else database.courseLibraryDao().getAllLibrariesList()
      for (ll in localLibs) {
        if (ll.remoteId != null && !remoteLibUuids.contains(ll.remoteId)) {
          database.courseLibraryDao().deleteLibrary(ll)
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

      // 9. User Profiles (Authoritative real users from Supabase)
      pullProfiles()

      // 10. Exam Attempts (Student examination history and scores)
      pullExamAttempts()

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
      val supabasePriority = when (priority.trim().lowercase()) {
        "urgent" -> "urgent"
        "exam alert", "exam", "high", "critical" -> "high"
        "low" -> "low"
        else -> "normal"
      }
      val announcement = SupabaseAnnouncement(
        id = id,
        title = title,
        message = message,
        author = author,
        priority = supabasePriority,
        target = target
      )
      val json = supabaseClient.moshi.adapter(SupabaseAnnouncement::class.java).toJson(announcement)
      var res = supabaseClient.restPost("announcements", json, upsert = true)

      // Fallback for missing author or target column in Supabase schema
      if (res.isFailure) {
        val err = res.exceptionOrNull()?.message ?: ""
        if (err.contains("PGRST204") || err.contains("Could not find the") || err.contains("does not exist") || err.contains("23514")) {
          val corePayload = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("message", "[Audience: $target]\n\n$message")
            put("priority", supabasePriority)
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
      val remoteUuid = if (!q.remoteId.isNullOrBlank() && q.remoteId.length == 36) q.remoteId else UUID.randomUUID().toString()
      val parentCourse = database.academicDao().getCourseById(q.courseId)
      var courseRemoteUuid = q.remoteCourseId ?: parentCourse?.remoteId
      if (courseRemoteUuid.isNullOrBlank() || courseRemoteUuid.length != 36) {
        val courseByCode = parentCourse?.let { database.academicDao().getCourseByCode(it.code) }
        courseRemoteUuid = courseByCode?.remoteId?.takeIf { it.length == 36 } ?: UUID.randomUUID().toString()
        if (parentCourse != null) {
          database.academicDao().insertCourse(parentCourse.copy(remoteId = courseRemoteUuid))
        }
      }

      // Supabase questions table strictly requires a non-null exam_id referencing an existing exam for this specific course
      var examRemoteUuid = q.remoteExamId
      if (examRemoteUuid.isNullOrBlank() || examRemoteUuid.length != 36) {
        val courseExams = database.examDao().getExamsForCourseList(q.courseId)
        val validExam = courseExams.firstOrNull { !it.remoteId.isNullOrBlank() && it.remoteId.length == 36 }
        if (validExam != null) {
          examRemoteUuid = validExam.remoteId
        } else {
          val newExamId = UUID.randomUUID().toString()
          val cCode = parentCourse?.code ?: "Course"
          val cTitle = parentCourse?.title ?: "Academic Course"
          val newExam = ExamEntity(
            courseId = q.courseId,
            title = "$cCode Continuous Assessment",
            description = "Continuous Assessment and Question Bank for $cTitle",
            durationMinutes = 30,
            totalQuestions = 15,
            isPremium = false,
            isPublished = true,
            remoteId = newExamId,
            remoteCourseId = courseRemoteUuid
          )
          val insertedId = database.examDao().insertExam(newExam)
          broadcastExam(newExam.copy(id = insertedId))
          examRemoteUuid = newExamId
        }
      }

      database.questionDao().insertQuestion(
        q.copy(
          remoteId = remoteUuid,
          remoteCourseId = courseRemoteUuid,
          remoteExamId = examRemoteUuid,
          isDemo = false
        )
      )

      val payload = JSONObject().apply {
        put("id", remoteUuid)
        put("course_id", courseRemoteUuid)
        put("exam_id", examRemoteUuid)
        put("topic_id", 1)
        put("question_text", q.questionText)
        put("option_a", q.optionA)
        put("option_b", q.optionB)
        put("option_c", q.optionC.takeIf { it.isNotBlank() })
        put("option_d", q.optionD.takeIf { it.isNotBlank() })
        put("correct_answer", q.correctAnswer)
        put("explanation", q.explanation)
        put("difficulty", q.difficulty)
        put("question_type", if (q.questionType.equals("MULTIPLE_CHOICE", ignoreCase = true)) "mcq" else q.questionType)
        put("assessment_title", q.assessmentTitle)
        put("marks", q.marks.toDouble())
        put("time_limit_seconds", q.timeLimitSeconds)
        put("image_url", q.imageUrl)
        put("is_active", true)
        put("is_premium", q.isPremium)
        put("is_published", q.isPublished)
        put("is_demo", false)
      }

      val res = supabaseClient.restPost("questions", payload.toString(), upsert = true)
      if (res.isFailure) {
        Log.e(tag, "broadcastQuestion to Supabase failed: ${res.exceptionOrNull()?.message}")
      }
      realtimeManager?.dispatchRealtimeEvent("questions", "INSERT", payload)
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Log.e(tag, "broadcastQuestion error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun broadcastMaterial(mat: StudyMaterialEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = if (!mat.remoteId.isNullOrBlank() && mat.remoteId.length == 36) mat.remoteId else UUID.randomUUID().toString()
      val parentCourse = database.academicDao().getCourseById(mat.courseId)
      var courseRemoteUuid = mat.remoteCourseId ?: parentCourse?.remoteId
      if (courseRemoteUuid.isNullOrBlank() || courseRemoteUuid.length != 36) {
        val courseByCode = parentCourse?.let { database.academicDao().getCourseByCode(it.code) }
        courseRemoteUuid = courseByCode?.remoteId?.takeIf { it.length == 36 } ?: UUID.randomUUID().toString()
        if (parentCourse != null) {
          database.academicDao().insertCourse(parentCourse.copy(remoteId = courseRemoteUuid))
        }
      }

      database.studyMaterialDao().insertMaterial(
        mat.copy(remoteId = remoteUuid, remoteCourseId = courseRemoteUuid)
      )

      val payload = JSONObject().apply {
        put("id", remoteUuid)
        put("course_id", courseRemoteUuid)
        put("topic_id", 1)
        put("title", mat.title)
        put("description", mat.description)
        put("content_type", mat.contentType)
        put("content_body", mat.contentBody)
        put("external_url", mat.externalUrl ?: "")
        put("file_url", mat.externalUrl ?: "")
        put("is_published", true)
        put("is_premium", mat.isPremium)
      }

      val res = supabaseClient.restPost("study_materials", payload.toString(), upsert = true)
      if (res.isFailure) {
        Log.e(tag, "broadcastMaterial to Supabase failed: ${res.exceptionOrNull()?.message}")
      }
      realtimeManager?.dispatchRealtimeEvent("study_materials", "INSERT", payload)
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Log.e(tag, "broadcastMaterial error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun broadcastLibrary(lib: CourseLibraryEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = if (!lib.remoteId.isNullOrBlank() && lib.remoteId.length == 36) lib.remoteId else UUID.randomUUID().toString()
      val parentCourse = database.academicDao().getCourseById(lib.courseId)
      var courseRemoteUuid = lib.remoteCourseId ?: parentCourse?.remoteId
      if (courseRemoteUuid.isNullOrBlank() || courseRemoteUuid.length != 36) {
        val courseByCode = parentCourse?.let { database.academicDao().getCourseByCode(it.code) }
        courseRemoteUuid = courseByCode?.remoteId?.takeIf { it.length == 36 } ?: UUID.randomUUID().toString()
        if (parentCourse != null) {
          database.academicDao().insertCourse(parentCourse.copy(remoteId = courseRemoteUuid))
        }
      }

      database.courseLibraryDao().insertLibrary(
        lib.copy(remoteId = remoteUuid, remoteCourseId = courseRemoteUuid)
      )

      val payload = JSONObject().apply {
        put("id", remoteUuid)
        put("course_id", courseRemoteUuid)
        put("course_code", lib.courseCode)
        put("name", lib.name)
        put("title", lib.name)
        put("description", lib.description)
        put("url", lib.externalUrl)
        put("external_url", lib.externalUrl)
        put("is_published", lib.isPublished)
      }

      val res = supabaseClient.restPost("course_libraries", payload.toString(), upsert = true)
      if (res.isFailure) {
        Log.e(tag, "broadcastLibrary to Supabase failed: ${res.exceptionOrNull()?.message}")
      }
      realtimeManager?.dispatchRealtimeEvent("course_libraries", "INSERT", payload)
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Log.e(tag, "broadcastLibrary error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun broadcastCourse(course: CourseEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = if (!course.remoteId.isNullOrBlank() && course.remoteId.length == 36) course.remoteId else UUID.randomUUID().toString()
      database.academicDao().insertCourse(course.copy(remoteId = remoteUuid))
      val payload = JSONObject().apply {
        put("id", remoteUuid)
        put("department_id", 1)
        put("code", course.code)
        put("title", course.title)
        put("level", course.level)
        put("semester", course.semester)
        put("description", course.description)
        put("is_published", true)
      }
      val res = supabaseClient.restPost("courses", payload.toString(), upsert = true)
      if (res.isFailure) {
        Log.e(tag, "broadcastCourse to Supabase failed: ${res.exceptionOrNull()?.message}")
      }
      realtimeManager?.dispatchRealtimeEvent("courses", "INSERT", payload)
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Log.e(tag, "broadcastCourse error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun deleteCourseCloud(course: CourseEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val targetId = course.remoteId
      if (!targetId.isNullOrBlank() && targetId.length == 36) {
        supabaseClient.restDelete("questions?course_id=eq.$targetId")
        supabaseClient.restDelete("study_materials?course_id=eq.$targetId")
        supabaseClient.restDelete("course_libraries?course_id=eq.$targetId")
        supabaseClient.restDelete("exams?course_id=eq.$targetId")
        val res = supabaseClient.restDelete("courses?id=eq.$targetId")
        realtimeManager?.dispatchRealtimeEvent("courses", "DELETE", JSONObject().put("id", targetId))
        Result.success(res.isSuccess)
      } else {
        Result.success(true)
      }
    } catch (e: Exception) {
      Log.e(tag, "deleteCourseCloud error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun deleteCourseCloud(courseId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    val course = database.academicDao().getCourseById(courseId)
    if (course != null) deleteCourseCloud(course) else Result.success(true)
  }

  suspend fun broadcastExam(exam: ExamEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val remoteUuid = if (!exam.remoteId.isNullOrBlank() && exam.remoteId.length == 36) exam.remoteId else UUID.randomUUID().toString()
      val parentCourse = database.academicDao().getCourseById(exam.courseId)
      var courseRemoteUuid = exam.remoteCourseId ?: parentCourse?.remoteId
      if (courseRemoteUuid.isNullOrBlank() || courseRemoteUuid.length != 36) {
        val courseByCode = parentCourse?.let { database.academicDao().getCourseByCode(it.code) }
        courseRemoteUuid = courseByCode?.remoteId?.takeIf { it.length == 36 } ?: UUID.randomUUID().toString()
        if (parentCourse != null) {
          database.academicDao().insertCourse(parentCourse.copy(remoteId = courseRemoteUuid))
        }
      }

      database.examDao().insertExam(exam.copy(remoteId = remoteUuid, remoteCourseId = courseRemoteUuid))

      val payload = JSONObject().apply {
        put("id", remoteUuid)
        put("course_id", courseRemoteUuid)
        put("title", exam.title)
        put("description", exam.description)
        put("duration_minutes", exam.durationMinutes)
        put("total_marks", exam.totalQuestions * 2)
        put("pass_percentage", 50)
        put("is_published", exam.isPublished)
      }

      val res = supabaseClient.restPost("exams", payload.toString(), upsert = true)
      if (res.isFailure) {
        Log.e(tag, "broadcastExam to Supabase failed: ${res.exceptionOrNull()?.message}")
      }
      realtimeManager?.dispatchRealtimeEvent("exams", "INSERT", payload)
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Log.e(tag, "broadcastExam error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun deleteExamCloud(exam: ExamEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val targetId = exam.remoteId
      if (!targetId.isNullOrBlank() && targetId.length == 36) {
        supabaseClient.restDelete("questions?exam_id=eq.$targetId")
        val res = supabaseClient.restDelete("exams?id=eq.$targetId")
        realtimeManager?.dispatchRealtimeEvent("exams", "DELETE", JSONObject().put("id", targetId))
        Result.success(res.isSuccess)
      } else {
        Result.success(true)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteExamCloud(examId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    val exam = database.examDao().getExamById(examId)
    if (exam != null) deleteExamCloud(exam) else Result.success(true)
  }

  suspend fun deleteQuestionCloud(q: QuestionEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val targetId = q.remoteId
      if (!targetId.isNullOrBlank() && targetId.length == 36) {
        val res = supabaseClient.restDelete("questions?id=eq.$targetId")
        realtimeManager?.dispatchRealtimeEvent("questions", "DELETE", JSONObject().put("id", targetId))
        Result.success(res.isSuccess)
      } else {
        Result.success(true)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteQuestionCloud(questionId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    val q = database.questionDao().getQuestionById(questionId)
    if (q != null) deleteQuestionCloud(q) else Result.success(true)
  }

  suspend fun clearAllQuestionsCloud(): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val res = supabaseClient.restDelete("questions?id=neq.00000000-0000-0000-0000-000000000000")
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteMaterialCloud(mat: StudyMaterialEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val targetId = mat.remoteId
      if (!targetId.isNullOrBlank() && targetId.length == 36) {
        val res = supabaseClient.restDelete("study_materials?id=eq.$targetId")
        realtimeManager?.dispatchRealtimeEvent("study_materials", "DELETE", JSONObject().put("id", targetId))
        Result.success(res.isSuccess)
      } else {
        Result.success(true)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteMaterialCloud(materialId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    val mat = database.studyMaterialDao().getMaterialById(materialId)
    if (mat != null) deleteMaterialCloud(mat) else Result.success(true)
  }

  suspend fun deleteLibraryCloud(lib: CourseLibraryEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val targetId = lib.remoteId
      if (!targetId.isNullOrBlank() && targetId.length == 36) {
        val res = supabaseClient.restDelete("course_libraries?id=eq.$targetId")
        realtimeManager?.dispatchRealtimeEvent("course_libraries", "DELETE", JSONObject().put("id", targetId))
        Result.success(res.isSuccess)
      } else {
        Result.success(true)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteLibraryCloud(libraryId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    val lib = database.courseLibraryDao().getLibraryById(libraryId)
    if (lib != null) deleteLibraryCloud(lib) else Result.success(true)
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
        .putString("mock_card_reveal_mode", config.revealMode)
        .putString("mock_card_deadline_string", config.deadlineString)
        .putLong("mock_card_deadline_timestamp", config.deadlineTimestamp)
        .putString("mock_card_session_id", config.sessionId)
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
        put(JSONObject().put("key", "mock_card_reveal_mode").put("value", config.revealMode))
        put(JSONObject().put("key", "mock_card_deadline_string").put("value", config.deadlineString))
        put(JSONObject().put("key", "mock_card_deadline_timestamp").put("value", config.deadlineTimestamp.toString()))
        put(JSONObject().put("key", "mock_card_session_id").put("value", config.sessionId))
      }
      val result = supabaseClient.restPost("app_config", array.toString(), upsert = true)
      realtimeManager?.dispatchRealtimeEvent("app_config", "UPDATE", JSONObject().put("mock_card_is_active", config.isConfigured))

      if (result.isSuccess) Result.success(true) else Result.failure(result.exceptionOrNull() ?: Exception("Failed to update mock card"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun broadcastProfile(user: UserEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val email = user.email.trim().lowercase()
      val existingRes = supabaseClient.restGet("profiles?email=eq.$email&limit=1")
      val json = existingRes.getOrNull() ?: "[]"
      val arr = JSONArray(json)
      val existingId = if (arr.length() > 0) arr.getJSONObject(0).optString("id") else null
      val profileId = existingId ?: FlexibleIdHelper.toUuidString(user.id, "user")

      val payload = JSONObject().apply {
        put("id", profileId)
        put("email", email)
        put("full_name", user.fullName.trim())
        put("matric_number", user.phoneNumber.trim())
        put("level", user.level)
        put("department", user.department)
        put("faculty", user.faculty)
        put("app_role", if (user.role == "ADMIN") "admin" else "student")
        put("is_active", user.isActive)
      }

      val res = supabaseClient.restPost("profiles", payload.toString(), upsert = true)
      realtimeManager?.dispatchRealtimeEvent("profiles", if (existingId != null) "UPDATE" else "INSERT", payload)
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Log.e(tag, "broadcastProfile error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun deleteProfileCloud(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val res = supabaseClient.restDelete("profiles?email=eq.${email.trim().lowercase()}")
      realtimeManager?.dispatchRealtimeEvent("profiles", "DELETE", JSONObject().put("email", email))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Log.e(tag, "deleteProfileCloud error: ${e.message}", e)
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
