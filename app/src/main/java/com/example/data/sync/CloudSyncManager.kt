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
  val questionsSynced: Int = 0,
  val materialsSynced: Int = 0,
  val examsSynced: Int = 0,
  val coursesSynced: Int = 0,
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

  init {
    startRealtimeListeners()
    scope.launch {
      pullAppConfig()
      pullLatestFromCloud()
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
      "announcements" -> {
        val title = record.optString("title")
        if (title.isNotBlank()) {
          val item = CloudAnnouncement(
            id = record.optString("id", java.util.UUID.randomUUID().toString()),
            title = title,
            message = record.optString("message"),
            author = record.optString("author", "Department of Medical Radiography"),
            priority = record.optString("priority", "Normal"),
            target = record.optString("target", "All Students")
          )
          _announcements.value = listOf(item) + _announcements.value.filter { it.id != item.id }
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
          if (eventType == "DELETE") {
            val id = parseJsonId(record, "id", 0L)
            if (id > 0) {
              kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                database.academicDao().getCourseById(id)?.let { database.academicDao().deleteCourse(it) }
              }
            }
          } else if (record.has("code") && record.has("title")) {
            val id = parseJsonId(record, "id", System.currentTimeMillis())
            val entity = CourseEntity(
              id = id,
              departmentId = parseJsonId(record, "department_id", 1L),
              code = record.optString("code"),
              title = record.optString("title"),
              level = record.optString("level", "100 Level"),
              semester = record.optString("semester", "First Semester"),
              description = record.optString("description", "")
            )
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              database.academicDao().insertCourse(entity)
            }
          }
        }
        "exams" -> {
          if (eventType == "DELETE") {
            val id = parseJsonId(record, "id", 0L)
            if (id > 0) {
              kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                database.examDao().getExamById(id)?.let { database.examDao().deleteExam(it) }
              }
            }
          } else if (record.has("title")) {
            val id = parseJsonId(record, "id", System.currentTimeMillis())
            val entity = ExamEntity(
              id = id,
              courseId = parseJsonId(record, "course_id", 1L),
              title = record.optString("title"),
              description = record.optString("description", ""),
              durationMinutes = record.optInt("duration_minutes", 30),
              totalQuestions = record.optInt("total_questions", 15),
              isPremium = record.optBoolean("is_premium", false),
              isPublished = record.optBoolean("is_published", true)
            )
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              database.examDao().insertExam(entity)
            }
          }
        }
        "questions" -> {
          if (eventType == "DELETE") {
            val id = parseJsonId(record, "id", 0L)
            if (id > 0) {
              kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                database.questionDao().getQuestionById(id)?.let { database.questionDao().deleteQuestion(it) }
              }
            }
          } else if (record.has("question_text")) {
            val id = parseJsonId(record, "id", System.currentTimeMillis())
            val entity = QuestionEntity(
              id = id,
              courseId = parseJsonId(record, "course_id", 1L),
              topicId = parseJsonId(record, "topic_id", 1L),
              questionText = record.optString("question_text"),
              optionA = record.optString("option_a", ""),
              optionB = record.optString("option_b", ""),
              optionC = record.optString("option_c", ""),
              optionD = record.optString("option_d", ""),
              correctAnswer = record.optString("correct_answer", "A"),
              explanation = record.optString("explanation", ""),
              difficulty = record.optString("difficulty", "Medium"),
              isPremium = record.optBoolean("is_premium", false),
              isPublished = record.optBoolean("is_published", true),
              isDemo = record.optBoolean("is_demo", false),
              questionType = record.optString("question_type", "MULTIPLE_CHOICE"),
              assessmentTitle = record.optString("assessment_title", "Continuous Assessment"),
              marks = record.optInt("marks", 1),
              timeLimitSeconds = record.optInt("time_limit_seconds", 60),
              imageUrl = record.optString("image_url").takeIf { it.isNotBlank() }
            )
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              database.questionDao().insertQuestion(entity)
            }
          }
        }
        "study_materials" -> {
          val title = record.optString("title")
          val courseId = parseJsonId(record, "course_id", 1L)
          val id = parseJsonId(record, "id", System.currentTimeMillis())
          if (title.isNotBlank()) {
            val entity = StudyMaterialEntity(
              id = id,
              courseId = courseId,
              topicId = parseJsonId(record, "topic_id", 1L),
              title = title,
              description = record.optString("description", ""),
              contentType = record.optString("content_type", "Lecture Note"),
              contentBody = record.optString("content_body", ""),
              externalUrl = record.optString("external_url").takeIf { it.isNotBlank() },
              isPremium = record.optBoolean("is_premium", false)
            )
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              database.studyMaterialDao().insertMaterial(entity)
            }
          }
        }
        "course_libraries" -> {
          val name = record.optString("name")
          val url = record.optString("external_url")
          val id = parseJsonId(record, "id", System.currentTimeMillis())
          if (name.isNotBlank() && url.isNotBlank()) {
            val entity = CourseLibraryEntity(
              id = id,
              courseId = parseJsonId(record, "course_id", 1L),
              courseCode = record.optString("course_code", "RAD 101"),
              name = name,
              description = record.optString("description", ""),
              externalUrl = url,
              isPublished = record.optBoolean("is_published", true)
            )
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              database.courseLibraryDao().insertLibrary(entity)
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
        "study_materials" -> {
          val title = record.optString("title")
          val courseId = parseJsonId(record, "course_id", 1L)
          val id = parseJsonId(record, "id", System.currentTimeMillis())
          if (title.isNotBlank()) {
            val entity = StudyMaterialEntity(
              id = id,
              courseId = courseId,
              topicId = parseJsonId(record, "topic_id", 1L),
              title = title,
              description = record.optString("description", ""),
              contentType = record.optString("content_type", "Lecture Note"),
              contentBody = record.optString("content_body", ""),
              externalUrl = record.optString("external_url").takeIf { it.isNotBlank() },
              isPremium = record.optBoolean("is_premium", false)
            )
            database.studyMaterialDao().insertMaterial(entity)
          }
        }
        "course_libraries" -> {
          val name = record.optString("name")
          val url = record.optString("external_url")
          val id = parseJsonId(record, "id", System.currentTimeMillis())
          if (name.isNotBlank() && url.isNotBlank()) {
            val entity = CourseLibraryEntity(
              id = id,
              courseId = parseJsonId(record, "course_id", 1L),
              courseCode = record.optString("course_code", "RAD 101"),
              name = name,
              description = record.optString("description", ""),
              externalUrl = url,
              isPublished = record.optBoolean("is_published", true)
            )
            database.courseLibraryDao().insertLibrary(entity)
          }
        }
        "student_feedback" -> {
          pullStudentFeedback()
        }
        "courses" -> {
          if (eventType == "DELETE") {
            val id = parseJsonId(record, "id", 0L)
            if (id > 0) {
              database.academicDao().getCourseById(id)?.let { database.academicDao().deleteCourse(it) }
            }
          } else if (record.has("code") && record.has("title")) {
            val id = parseJsonId(record, "id", System.currentTimeMillis())
            val entity = CourseEntity(
              id = id,
              departmentId = parseJsonId(record, "department_id", 1L),
              code = record.optString("code"),
              title = record.optString("title"),
              level = record.optString("level", "100 Level"),
              semester = record.optString("semester", "First Semester"),
              description = record.optString("description", "")
            )
            database.academicDao().insertCourse(entity)
          } else {
            pullCourses()
          }
        }
        "exams" -> {
          if (eventType == "DELETE") {
            val id = parseJsonId(record, "id", 0L)
            if (id > 0) {
              database.examDao().getExamById(id)?.let { database.examDao().deleteExam(it) }
            }
          } else if (record.has("title")) {
            val id = parseJsonId(record, "id", System.currentTimeMillis())
            val entity = ExamEntity(
              id = id,
              courseId = parseJsonId(record, "course_id", 1L),
              title = record.optString("title"),
              description = record.optString("description", ""),
              durationMinutes = record.optInt("duration_minutes", 30),
              totalQuestions = record.optInt("total_questions", 15),
              isPremium = record.optBoolean("is_premium", false),
              isPublished = record.optBoolean("is_published", true)
            )
            database.examDao().insertExam(entity)
          } else {
            pullExams()
          }
        }
        "questions" -> {
          if (eventType == "DELETE") {
            val id = parseJsonId(record, "id", 0L)
            if (id > 0) {
              database.questionDao().getQuestionById(id)?.let { database.questionDao().deleteQuestion(it) }
            }
          } else if (record.has("question_text")) {
            val id = parseJsonId(record, "id", System.currentTimeMillis())
            val entity = QuestionEntity(
              id = id,
              courseId = parseJsonId(record, "course_id", 1L),
              topicId = parseJsonId(record, "topic_id", 1L),
              questionText = record.optString("question_text"),
              optionA = record.optString("option_a", ""),
              optionB = record.optString("option_b", ""),
              optionC = record.optString("option_c", ""),
              optionD = record.optString("option_d", ""),
              correctAnswer = record.optString("correct_answer", "A"),
              explanation = record.optString("explanation", ""),
              difficulty = record.optString("difficulty", "Medium"),
              isPremium = record.optBoolean("is_premium", false),
              isPublished = record.optBoolean("is_published", true),
              isDemo = record.optBoolean("is_demo", false)
            )
            database.questionDao().insertQuestion(entity)
          } else {
            val courseId = parseJsonId(record, "course_id", 0L).takeIf { it > 0 }
            pullQuestions(courseId)
          }
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

  suspend fun pullAnnouncements() = withContext(Dispatchers.IO) {
    val res = supabaseClient.restGet("announcements?select=*&order=created_at.desc&limit=20")
    res.onSuccess { json ->
      try {
        val type = Types.newParameterizedType(List::class.java, SupabaseAnnouncement::class.java)
        val adapter = supabaseClient.moshi.adapter<List<SupabaseAnnouncement>>(type)
        val list = adapter.fromJson(json) ?: emptyList()
        val mapped = list.map {
          CloudAnnouncement(
            id = it.id,
            title = it.title,
            message = it.message,
            author = it.author ?: "Department of Medical Radiography",
            priority = it.priority,
            target = it.target ?: "All Students",
            timestamp = System.currentTimeMillis()
          )
        }
        _announcements.value = mapped
      } catch (e: Exception) {
        Log.e(tag, "Failed to parse announcements: ${e.message}")
      }
    }
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
            message = it.message,
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
    val res = supabaseClient.restGet("courses?select=*&order=id.asc")
    res.fold(
      onSuccess = { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseCourse::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseCourse>>(type).fromJson(json) ?: emptyList()
          for (c in list) {
            database.academicDao().insertCourse(
              CourseEntity(
                id = FlexibleIdHelper.toPositiveLong(c.id, 0L),
                departmentId = FlexibleIdHelper.toPositiveLong(c.departmentId, 1L),
                code = c.code,
                title = c.title,
                level = c.level ?: "100 Level",
                semester = c.semester ?: "First Semester",
                description = c.description ?: ""
              )
            )
          }
          true
        } catch (e: Exception) {
          Log.e(tag, "Error parsing courses: ${e.message}")
          false
        }
      },
      onFailure = { false }
    )
  }

  suspend fun pullStudyMaterials(courseId: Long? = null): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    val endpoint = if (courseId != null) {
      val cUuid = FlexibleIdHelper.toUuidString(courseId, "course")
      "study_materials?course_id=eq.$cUuid&select=*"
    } else "study_materials?select=*&limit=100"
    val res = supabaseClient.restGet(endpoint)
    res.fold(
      onSuccess = { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseStudyMaterial::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseStudyMaterial>>(type).fromJson(json) ?: emptyList()
          for (m in list) {
            database.studyMaterialDao().insertMaterial(
              StudyMaterialEntity(
                id = FlexibleIdHelper.toPositiveLong(m.id, 0L),
                courseId = FlexibleIdHelper.toPositiveLong(m.courseId, 1L),
                topicId = FlexibleIdHelper.toPositiveLong(m.topicId, 1L),
                title = m.title,
                description = m.description ?: "",
                contentType = m.contentType ?: "Lecture Note",
                contentBody = m.contentBody ?: "",
                externalUrl = m.externalUrl ?: m.fileUrl,
                isPremium = m.isPremium ?: false
              )
            )
          }
          true
        } catch (e: Exception) {
          Log.e(tag, "Error parsing materials: ${e.message}")
          false
        }
      },
      onFailure = { false }
    )
  }

  suspend fun pullCourseLibraries(courseId: Long? = null): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    val endpoint = if (courseId != null) {
      val cUuid = FlexibleIdHelper.toUuidString(courseId, "course")
      "course_libraries?course_id=eq.$cUuid&select=*"
    } else "course_libraries?select=*&limit=50"
    val res = supabaseClient.restGet(endpoint)
    res.fold(
      onSuccess = { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseCourseLibrary::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseCourseLibrary>>(type).fromJson(json) ?: emptyList()
          for (l in list) {
            val libName = l.name ?: l.title ?: "Library Resource"
            val libUrl = l.url ?: l.externalUrl ?: ""
            if (libUrl.isNotBlank()) {
              database.courseLibraryDao().insertLibrary(
                CourseLibraryEntity(
                  id = FlexibleIdHelper.toPositiveLong(l.id, 0L),
                  courseId = FlexibleIdHelper.toPositiveLong(l.courseId, 1L),
                  courseCode = l.courseCode ?: "RAD 101",
                  name = libName,
                  description = l.description ?: "",
                  externalUrl = libUrl,
                  isPublished = l.isPublished
                )
              )
            }
          }
          true
        } catch (e: Exception) {
          Log.e(tag, "Error parsing libraries: ${e.message}")
          false
        }
      },
      onFailure = { false }
    )
  }

  suspend fun pullExams(): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    val res = supabaseClient.restGet("exams?select=*&order=id.asc")
    res.fold(
      onSuccess = { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseExam::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseExam>>(type).fromJson(json) ?: emptyList()
          for (e in list) {
            database.examDao().insertExam(
              ExamEntity(
                id = FlexibleIdHelper.toPositiveLong(e.id, 0L),
                courseId = FlexibleIdHelper.toPositiveLong(e.courseId, 1L),
                title = e.title,
                description = e.description ?: "",
                durationMinutes = e.durationMinutes ?: 30,
                totalQuestions = maxOf((e.totalMarks ?: 30) / 2, 10),
                isPremium = false,
                isPublished = e.isPublished
              )
            )
          }
          true
        } catch (e: Exception) {
          Log.e(tag, "Error parsing exams: ${e.message}")
          false
        }
      },
      onFailure = { false }
    )
  }

  suspend fun pullQuestions(courseId: Long? = null): Boolean = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) return@withContext false
    val endpoint = "questions?select=*&limit=100"
    val res = supabaseClient.restGet(endpoint)
    res.fold(
      onSuccess = { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseQuestion::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseQuestion>>(type).fromJson(json) ?: emptyList()
          for (q in list) {
            val qCourseId = FlexibleIdHelper.toPositiveLong(q.courseId ?: q.examId, courseId ?: 1L)
            database.questionDao().insertQuestion(
              QuestionEntity(
                id = FlexibleIdHelper.toPositiveLong(q.id, 0L),
                courseId = qCourseId,
                topicId = FlexibleIdHelper.toPositiveLong(q.topicId, 1L),
                questionText = q.questionText,
                optionA = q.optionA,
                optionB = q.optionB,
                optionC = q.optionC,
                optionD = q.optionD,
                correctAnswer = q.correctAnswer ?: "A",
                explanation = q.explanation ?: "Refer to course clinical study packs for verified answer rationale.",
                difficulty = q.difficulty ?: "Medium",
                isPremium = q.isPremium ?: false,
                isPublished = q.isPublished ?: q.isActive,
                isDemo = q.isDemo ?: false,
                questionType = q.questionType ?: "MULTIPLE_CHOICE",
                assessmentTitle = q.assessmentTitle ?: "Continuous Assessment",
                marks = q.marks ?: 1,
                timeLimitSeconds = q.timeLimitSeconds ?: 60,
                imageUrl = q.imageUrl
              )
            )
          }
          true
        } catch (e: Exception) {
          Log.e(tag, "Error parsing questions: ${e.message}")
          false
        }
      },
      onFailure = { false }
    )
  }

  suspend fun pullLatestFromCloud(): SyncReport = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) {
      return@withContext SyncReport(false, message = "Offline cache active. Connect to internet to sync Supabase cloud.")
    }
    _isSyncing.value = true
    try {
      pullAppConfig()
      pullAnnouncements()
      pullStudentFeedback()
      pullCourses()
      pullExams()
      pullStudyMaterials()
      pullCourseLibraries()

    // Pull App Config
    val cfgRes = supabaseClient.restGet("app_config?select=*")
    cfgRes.onSuccess { json ->
      try {
        val rootArray = org.json.JSONArray(json)
        for (i in 0 until rootArray.length()) {
          val item = rootArray.optJSONObject(i) ?: continue
          val key = item.optString("key")
          val value = item.optString("value")
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
                val newSprint = _dailySprintConfig.value.copy(title = value)
                _dailySprintConfig.value = newSprint
                prefs.edit().putString("sprint_title", value).apply()
              }
              "sprint_subtitle" -> {
                val newSprint = _dailySprintConfig.value.copy(subtitle = value)
                _dailySprintConfig.value = newSprint
                prefs.edit().putString("sprint_subtitle", value).apply()
              }
              "sprint_badge" -> {
                val newSprint = _dailySprintConfig.value.copy(badge = value)
                _dailySprintConfig.value = newSprint
                prefs.edit().putString("sprint_badge", value).apply()
              }
            }
          }
          val cardStyle = item.optString("card_style").ifBlank { item.optString("sprint_theme") }
          if (cardStyle.isNotBlank()) {
            _globalCardStyle.value = cardStyle
            prefs.edit().putString("global_card_style", cardStyle).apply()
          }
          if (item.has("sprint_title") || item.has("sprint_course_id") || item.has("sprint_badge")) {
            val newSprint = _dailySprintConfig.value.copy(
              badge = item.optString("sprint_badge").ifBlank { _dailySprintConfig.value.badge },
              title = item.optString("sprint_title").ifBlank { _dailySprintConfig.value.title },
              subtitle = item.optString("sprint_subtitle").ifBlank { _dailySprintConfig.value.subtitle },
              courseId = parseJsonId(item, "sprint_course_id", _dailySprintConfig.value.courseId),
              courseCode = item.optString("sprint_course_code").ifBlank { _dailySprintConfig.value.courseCode },
              questionCount = if (item.has("sprint_question_count")) item.optInt("sprint_question_count", 15) else _dailySprintConfig.value.questionCount,
              activeStudentsText = item.optString("sprint_active_students").ifBlank { _dailySprintConfig.value.activeStudentsText },
              theme = if (cardStyle.isNotBlank()) cardStyle else _dailySprintConfig.value.theme
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
              .apply()
          }
          if (item.has("practice_card_course_id") || item.has("practice_card_is_active")) {
            val newPractice = _practiceCardConfig.value.copy(
              isConfigured = item.optBoolean("practice_card_is_active", _practiceCardConfig.value.isConfigured),
              courseId = parseJsonId(item, "practice_card_course_id", _practiceCardConfig.value.courseId),
              courseCode = item.optString("practice_card_course_code").ifBlank { _practiceCardConfig.value.courseCode },
              courseTitle = item.optString("practice_card_course_title").ifBlank { _practiceCardConfig.value.courseTitle },
              questionCount = if (item.has("practice_card_question_count")) item.optInt("practice_card_question_count", 15) else _practiceCardConfig.value.questionCount
            )
            _practiceCardConfig.value = newPractice
            prefs.edit()
              .putBoolean("practice_card_is_active", newPractice.isConfigured)
              .putLong("practice_card_course_id", newPractice.courseId)
              .putString("practice_card_course_code", newPractice.courseCode)
              .putString("practice_card_course_title", newPractice.courseTitle)
              .putInt("practice_card_question_count", newPractice.questionCount)
              .apply()
          }
          if (item.has("mock_card_course_id") || item.has("mock_card_is_active")) {
            val newMock = _mockCardConfig.value.copy(
              isConfigured = item.optBoolean("mock_card_is_active", _mockCardConfig.value.isConfigured),
              courseId = parseJsonId(item, "mock_card_course_id", _mockCardConfig.value.courseId),
              courseCode = item.optString("mock_card_course_code").ifBlank { _mockCardConfig.value.courseCode },
              courseTitle = item.optString("mock_card_course_title").ifBlank { _mockCardConfig.value.courseTitle },
              examId = parseJsonId(item, "mock_card_exam_id", _mockCardConfig.value.examId),
              examTitle = item.optString("mock_card_exam_title").ifBlank { _mockCardConfig.value.examTitle },
              questionCount = if (item.has("mock_card_question_count")) item.optInt("mock_card_question_count", 15) else _mockCardConfig.value.questionCount,
              durationMinutes = if (item.has("mock_card_duration_minutes")) item.optInt("mock_card_duration_minutes", 30) else _mockCardConfig.value.durationMinutes
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
              .apply()
          }
        }
      } catch (e: Exception) {
        Log.e(tag, "Notice parsing app_config: ${e.message}")
      }
    }

      val cCount = database.academicDao().getCoursesList().size
      val mCount = database.studyMaterialDao().getAllMaterialsList().size
      val lCount = database.courseLibraryDao().getAllLibrariesList().size
      val eCount = database.examDao().getAllExamsList().size
      val qCount = database.questionDao().getAllQuestionsList().size

      _isSyncing.value = false
      val report = SyncReport(
        success = true,
        questionsSynced = qCount,
        materialsSynced = mCount,
        examsSynced = eCount,
        coursesSynced = cCount,
        message = "Synced with Supabase: $cCount courses, $mCount materials, $lCount libraries, $eCount exams & $qCount questions loaded."
      )
      _lastSyncReport.value = report
      _cloudStatusMessage.value = "Synced Live with Supabase at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}"
      report
    } catch (e: Exception) {
      _isSyncing.value = false
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
      // 1. Push Courses
      val courses = database.academicDao().getCoursesList()
      for (course in courses) {
        val coursePayload = JSONObject().apply {
          put("code", course.code)
          put("title", course.title)
          put("level", course.level)
          put("semester", course.semester)
          put("description", course.description)
        }
        val res = supabaseClient.restPost("courses", coursePayload.toString(), upsert = true)
        if (res.isSuccess) {
          cCount++
        } else {
          lastErrorMsg = res.exceptionOrNull()?.message
        }
      }

      // 2. Push Study Materials
      val materials = database.studyMaterialDao().getAllMaterialsList()
      for (mat in materials) {
        val matPayload = JSONObject().apply {
          put("title", mat.title)
          put("description", mat.description)
          if (!mat.externalUrl.isNullOrBlank()) {
            put("external_url", mat.externalUrl)
            put("file_url", mat.externalUrl)
          }
        }
        val res = supabaseClient.restPost("study_materials", matPayload.toString(), upsert = true)
        if (res.isSuccess) {
          mCount++
        } else {
          lastErrorMsg = res.exceptionOrNull()?.message
        }
      }

      // 3. Push Course Libraries
      val libraries = database.courseLibraryDao().getAllLibrariesList()
      for (lib in libraries) {
        val libPayload = JSONObject().apply {
          put("title", lib.name)
          put("description", lib.description)
          put("url", lib.externalUrl)
          put("is_published", lib.isPublished)
        }
        val res = supabaseClient.restPost("course_libraries", libPayload.toString(), upsert = true)
        if (res.isSuccess) {
          lCount++
        } else {
          lastErrorMsg = res.exceptionOrNull()?.message
        }
      }

      // 4. Push Questions
      val allQuestions = database.questionDao().getAllQuestionsList()
      for (q in allQuestions) {
        val qPayload = JSONObject().apply {
          put("question_text", q.questionText)
          put("option_a", q.optionA)
          put("option_b", q.optionB)
          put("option_c", q.optionC)
          put("option_d", q.optionD)
          put("explanation", q.explanation)
          put("question_type", q.questionType)
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

      // 5. Push Exams
      val allExams = database.examDao().getAllExamsList()
      for (ex in allExams) {
        val exPayload = JSONObject().apply {
          put("title", ex.title)
          put("description", ex.description)
          put("is_published", ex.isPublished)
          put("pass_mark", 50)
        }
        val res = supabaseClient.restPost("exams", exPayload.toString(), upsert = true)
        if (res.isSuccess) {
          eCount++
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
      val id = "ann_${System.currentTimeMillis()}"
      val announcement = SupabaseAnnouncement(
        id = id,
        title = title,
        message = message,
        author = author,
        priority = priority,
        target = target
      )
      val json = supabaseClient.moshi.adapter(SupabaseAnnouncement::class.java).toJson(announcement)
      val res = supabaseClient.restPost("announcements", json, upsert = true)

      // Also add to local state immediately
      val localItem = CloudAnnouncement(
        id = id,
        title = title,
        message = message,
        author = author,
        priority = priority,
        target = target,
        timestamp = System.currentTimeMillis()
      )
      _announcements.value = listOf(localItem) + _announcements.value

      // Realtime websocket pipeline broadcast across all active clients
      realtimeManager?.dispatchRealtimeEvent("announcements", "INSERT", JSONObject(json))

      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val res = supabaseClient.restDelete("announcements?id=eq.$announcementId")
      _announcements.value = _announcements.value.filter { it.id != announcementId }
      realtimeManager?.dispatchRealtimeEvent("announcements", "DELETE", JSONObject().put("id", announcementId))
      Result.success(res.isSuccess)
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
      val qDto = SupabaseQuestion(
        id = q.id,
        courseId = q.courseId,
        topicId = q.topicId,
        questionText = q.questionText,
        optionA = q.optionA,
        optionB = q.optionB,
        optionC = q.optionC,
        optionD = q.optionD,
        correctAnswer = q.correctAnswer,
        explanation = q.explanation,
        difficulty = q.difficulty,
        questionType = q.questionType,
        assessmentTitle = q.assessmentTitle,
        marks = q.marks,
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
      val mDto = SupabaseStudyMaterial(
        id = mat.id,
        courseId = mat.courseId,
        topicId = mat.topicId,
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
      val lDto = SupabaseCourseLibrary(
        id = lib.id,
        courseId = lib.courseId,
        courseCode = lib.courseCode,
        name = lib.name,
        description = lib.description,
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
      val cDto = SupabaseCourse(
        id = course.id,
        departmentId = course.departmentId,
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
      val res = supabaseClient.restDelete("courses?id=eq.$courseId")
      realtimeManager?.dispatchRealtimeEvent("courses", "DELETE", JSONObject().put("id", courseId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun broadcastExam(exam: ExamEntity): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val eDto = SupabaseExam(
        id = exam.id,
        courseId = exam.courseId,
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
      val res = supabaseClient.restDelete("exams?id=eq.$examId")
      realtimeManager?.dispatchRealtimeEvent("exams", "DELETE", JSONObject().put("id", examId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteQuestionCloud(questionId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val res = supabaseClient.restDelete("questions?id=eq.$questionId")
      realtimeManager?.dispatchRealtimeEvent("questions", "DELETE", JSONObject().put("id", questionId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteMaterialCloud(materialId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val res = supabaseClient.restDelete("study_materials?id=eq.$materialId")
      realtimeManager?.dispatchRealtimeEvent("study_materials", "DELETE", JSONObject().put("id", materialId))
      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteLibraryCloud(libraryId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val res = supabaseClient.restDelete("course_libraries?id=eq.$libraryId")
      realtimeManager?.dispatchRealtimeEvent("course_libraries", "DELETE", JSONObject().put("id", libraryId))
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
