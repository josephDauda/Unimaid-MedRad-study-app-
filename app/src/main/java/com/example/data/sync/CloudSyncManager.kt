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
  val questionCount: Int = 15,
  val activeStudentsText: String = "38 students active"
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
      questionCount = prefs.getInt("sprint_question_count", 15),
      activeStudentsText = prefs.getString("sprint_active_students", "38 students active") ?: "38 students active"
    )
  )
  val dailySprintConfig: StateFlow<DailyCbtSprintConfig> = _dailySprintConfig.asStateFlow()

  var realtimeManager: SupabaseRealtimeManager? = null
    private set

  init {
    startRealtimeListeners()
    scope.launch {
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
            val id = record.optLong("id", 0L)
            if (id > 0) {
              kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                database.academicDao().getCourseById(id)?.let { database.academicDao().deleteCourse(it) }
              }
            }
          } else if (record.has("code") && record.has("title")) {
            val id = record.optLong("id", System.currentTimeMillis())
            val entity = CourseEntity(
              id = id,
              departmentId = record.optLong("department_id", 1L),
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
            val id = record.optLong("id", 0L)
            if (id > 0) {
              kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                database.examDao().getExamById(id)?.let { database.examDao().deleteExam(it) }
              }
            }
          } else if (record.has("title")) {
            val id = record.optLong("id", System.currentTimeMillis())
            val entity = ExamEntity(
              id = id,
              courseId = record.optLong("course_id", 1L),
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
            val id = record.optLong("id", 0L)
            if (id > 0) {
              kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                database.questionDao().getQuestionById(id)?.let { database.questionDao().deleteQuestion(it) }
              }
            }
          } else if (record.has("question_text")) {
            val id = record.optLong("id", System.currentTimeMillis())
            val entity = QuestionEntity(
              id = id,
              courseId = record.optLong("course_id", 1L),
              topicId = record.optLong("topic_id", 1L),
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
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
              database.questionDao().insertQuestion(entity)
            }
          }
        }
        "study_materials" -> {
          val title = record.optString("title")
          val courseId = record.optLong("course_id", 1L)
          val id = record.optLong("id", System.currentTimeMillis())
          if (title.isNotBlank()) {
            val entity = StudyMaterialEntity(
              id = id,
              courseId = courseId,
              topicId = record.optLong("topic_id", 1L),
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
          val id = record.optLong("id", System.currentTimeMillis())
          if (name.isNotBlank() && url.isNotBlank()) {
            val entity = CourseLibraryEntity(
              id = id,
              courseId = record.optLong("course_id", 1L),
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

          // Handle Realtime Daily CBT Sprint changes
          val sprintTitle = record.optString("sprint_title")
          val sprintSubtitle = record.optString("sprint_subtitle")
          val sprintBadge = record.optString("sprint_badge")
          if (sprintTitle.isNotBlank() || sprintBadge.isNotBlank() || sprintSubtitle.isNotBlank()) {
            val newSprint = _dailySprintConfig.value.copy(
              badge = if (sprintBadge.isNotBlank()) sprintBadge else _dailySprintConfig.value.badge,
              title = if (sprintTitle.isNotBlank()) sprintTitle else _dailySprintConfig.value.title,
              subtitle = if (sprintSubtitle.isNotBlank()) sprintSubtitle else _dailySprintConfig.value.subtitle,
              courseId = if (record.has("sprint_course_id")) record.optLong("sprint_course_id", 1L) else _dailySprintConfig.value.courseId,
              questionCount = if (record.has("sprint_question_count")) record.optInt("sprint_question_count", 15) else _dailySprintConfig.value.questionCount,
              activeStudentsText = if (record.has("sprint_active_students")) record.optString("sprint_active_students", _dailySprintConfig.value.activeStudentsText) else _dailySprintConfig.value.activeStudentsText
            )
            _dailySprintConfig.value = newSprint
            prefs.edit()
              .putString("sprint_badge", newSprint.badge)
              .putString("sprint_title", newSprint.title)
              .putString("sprint_subtitle", newSprint.subtitle)
              .putLong("sprint_course_id", newSprint.courseId)
              .putInt("sprint_question_count", newSprint.questionCount)
              .putString("sprint_active_students", newSprint.activeStudentsText)
              .apply()
            Log.i(tag, "Realtime Daily CBT Sprint updated: ${newSprint.title}")
          }
        }
        "study_materials" -> {
          val title = record.optString("title")
          val courseId = record.optLong("course_id", 1L)
          val id = record.optLong("id", System.currentTimeMillis())
          if (title.isNotBlank()) {
            val entity = StudyMaterialEntity(
              id = id,
              courseId = courseId,
              topicId = record.optLong("topic_id", 1L),
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
          val id = record.optLong("id", System.currentTimeMillis())
          if (name.isNotBlank() && url.isNotBlank()) {
            val entity = CourseLibraryEntity(
              id = id,
              courseId = record.optLong("course_id", 1L),
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
            val id = record.optLong("id", 0L)
            if (id > 0) {
              database.academicDao().getCourseById(id)?.let { database.academicDao().deleteCourse(it) }
            }
          } else if (record.has("code") && record.has("title")) {
            val id = record.optLong("id", System.currentTimeMillis())
            val entity = CourseEntity(
              id = id,
              departmentId = record.optLong("department_id", 1L),
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
            val id = record.optLong("id", 0L)
            if (id > 0) {
              database.examDao().getExamById(id)?.let { database.examDao().deleteExam(it) }
            }
          } else if (record.has("title")) {
            val id = record.optLong("id", System.currentTimeMillis())
            val entity = ExamEntity(
              id = id,
              courseId = record.optLong("course_id", 1L),
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
            val id = record.optLong("id", 0L)
            if (id > 0) {
              database.questionDao().getQuestionById(id)?.let { database.questionDao().deleteQuestion(it) }
            }
          } else if (record.has("question_text")) {
            val id = record.optLong("id", System.currentTimeMillis())
            val entity = QuestionEntity(
              id = id,
              courseId = record.optLong("course_id", 1L),
              topicId = record.optLong("topic_id", 1L),
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
            val courseId = record.optLong("course_id", 0L).takeIf { it > 0 }
            pullQuestions(courseId)
          }
        }
      }
    }
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
            author = it.author,
            priority = it.priority,
            target = it.target,
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
            studentName = it.studentName,
            studentEmail = it.studentEmail,
            category = it.category,
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
                id = c.id,
                departmentId = c.departmentId ?: 1L,
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
    val endpoint = if (courseId != null) "study_materials?course_id=eq.$courseId&select=*" else "study_materials?select=*&limit=100"
    val res = supabaseClient.restGet(endpoint)
    res.fold(
      onSuccess = { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseStudyMaterial::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseStudyMaterial>>(type).fromJson(json) ?: emptyList()
          for (m in list) {
            database.studyMaterialDao().insertMaterial(
              StudyMaterialEntity(
                id = m.id,
                courseId = m.courseId,
                topicId = m.topicId ?: 1L,
                title = m.title,
                description = m.description ?: "",
                contentType = m.contentType,
                contentBody = m.contentBody,
                externalUrl = m.externalUrl,
                isPremium = m.isPremium
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
    val endpoint = if (courseId != null) "course_libraries?course_id=eq.$courseId&select=*" else "course_libraries?select=*&limit=50"
    val res = supabaseClient.restGet(endpoint)
    res.fold(
      onSuccess = { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseCourseLibrary::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseCourseLibrary>>(type).fromJson(json) ?: emptyList()
          for (l in list) {
            database.courseLibraryDao().insertLibrary(
              CourseLibraryEntity(
                id = l.id,
                courseId = l.courseId,
                courseCode = l.courseCode ?: "RAD 101",
                name = l.name,
                description = l.description ?: "",
                externalUrl = l.externalUrl,
                isPublished = l.isPublished
              )
            )
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
                id = e.id,
                courseId = e.courseId,
                title = e.title,
                description = e.description ?: "",
                durationMinutes = e.durationMinutes,
                totalQuestions = maxOf(e.totalMarks / 2, 10),
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
    val endpoint = if (courseId != null) "questions?course_id=eq.$courseId&select=*" else "questions?select=*&limit=100"
    val res = supabaseClient.restGet(endpoint)
    res.fold(
      onSuccess = { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseQuestion::class.java)
          val list = supabaseClient.moshi.adapter<List<SupabaseQuestion>>(type).fromJson(json) ?: emptyList()
          for (q in list) {
            database.questionDao().insertQuestion(
              QuestionEntity(
                id = q.id,
                courseId = q.courseId,
                topicId = q.topicId ?: 1L,
                questionText = q.questionText,
                optionA = q.optionA,
                optionB = q.optionB,
                optionC = q.optionC,
                optionD = q.optionD,
                correctAnswer = "A",
                explanation = "Refer to course clinical study packs for verified answer rationale.",
                difficulty = q.difficulty,
                isPremium = q.isPremium,
                isPublished = q.isPublished,
                isDemo = q.isDemo
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
      pullAnnouncements()
      pullStudentFeedback()
      pullCourses()
      pullExams()
      pullQuestions()

      // Pull Study Materials
    val matRes = supabaseClient.restGet("study_materials?select=*&limit=100")
    var mCount = 0
    matRes.onSuccess { json ->
      try {
        val type = Types.newParameterizedType(List::class.java, SupabaseStudyMaterial::class.java)
        val adapter = supabaseClient.moshi.adapter<List<SupabaseStudyMaterial>>(type)
        val list = adapter.fromJson(json) ?: emptyList()
        for (m in list) {
          database.studyMaterialDao().insertMaterial(
            StudyMaterialEntity(
              id = m.id,
              courseId = m.courseId,
              topicId = m.topicId ?: 1L,
              title = m.title,
              description = m.description ?: "",
              contentType = m.contentType,
              contentBody = m.contentBody,
              externalUrl = m.externalUrl,
              isPremium = m.isPremium
            )
          )
          mCount++
        }
      } catch (e: Exception) {
        Log.e(tag, "Error parsing materials: ${e.message}")
      }
    }

    // Pull Course Libraries
    val libRes = supabaseClient.restGet("course_libraries?select=*&limit=50")
    var lCount = 0
    libRes.onSuccess { json ->
      try {
        val type = Types.newParameterizedType(List::class.java, SupabaseCourseLibrary::class.java)
        val adapter = supabaseClient.moshi.adapter<List<SupabaseCourseLibrary>>(type)
        val list = adapter.fromJson(json) ?: emptyList()
        for (l in list) {
          database.courseLibraryDao().insertLibrary(
            CourseLibraryEntity(
              id = l.id,
              courseId = l.courseId,
              courseCode = l.courseCode ?: "RAD 101",
              name = l.name,
              description = l.description ?: "",
              externalUrl = l.externalUrl,
              isPublished = l.isPublished
            )
          )
          lCount++
        }
      } catch (e: Exception) {
        Log.e(tag, "Error parsing libraries: ${e.message}")
      }
    }

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
          val cardStyle = item.optString("card_style")
          if (cardStyle.isNotBlank()) {
            _globalCardStyle.value = cardStyle
            prefs.edit().putString("global_card_style", cardStyle).apply()
          }
        }
      } catch (e: Exception) {
        Log.e(tag, "Notice parsing app_config: ${e.message}")
      }
    }

      _isSyncing.value = false
      val report = SyncReport(
        success = true,
        materialsSynced = mCount,
        examsSynced = lCount,
        message = "Synced $mCount study materials and $lCount libraries from Supabase!"
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

    try {
      // 1. Push Courses
      val courses = database.academicDao().getCoursesList()
      for (course in courses) {
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
        if (res.isSuccess) cCount++
      }

      // 2. Push Study Materials
      val materials = database.studyMaterialDao().getAllMaterialsList()
      for (mat in materials) {
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
        if (res.isSuccess) mCount++
      }

      // 3. Push Course Libraries
      val libraries = database.courseLibraryDao().getAllLibrariesList()
      for (lib in libraries) {
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
        if (res.isSuccess) lCount++
      }

      // 4. Push Questions
      val allCourses = database.academicDao().getCoursesList()
      for (c in allCourses) {
        val questions = database.questionDao().getPublishedQuestionsForCourseList(c.id)
        for (q in questions) {
          val qDto = SupabaseQuestion(
            id = q.id,
            courseId = q.courseId,
            topicId = q.topicId,
            questionText = q.questionText,
            optionA = q.optionA,
            optionB = q.optionB,
            optionC = q.optionC,
            optionD = q.optionD,
            difficulty = q.difficulty,
            isPremium = q.isPremium,
            isPublished = q.isPublished,
            isDemo = q.isDemo
          )
          val json = supabaseClient.moshi.adapter(SupabaseQuestion::class.java).toJson(qDto)
          val res = supabaseClient.restPost("questions", json, upsert = true)
          if (res.isSuccess) qCount++
        }
      }

      // 5. Push Exams
      val allExams = database.examDao().getAllExamsList()
      var eCount = 0
      for (ex in allExams) {
        val eDto = SupabaseExam(
          id = ex.id,
          courseId = ex.courseId,
          title = ex.title,
          description = ex.description,
          durationMinutes = ex.durationMinutes,
          totalMarks = ex.totalQuestions * 2,
          passPercentage = 50,
          isPublished = ex.isPublished
        )
        val json = supabaseClient.moshi.adapter(SupabaseExam::class.java).toJson(eDto)
        val res = supabaseClient.restPost("exams", json, upsert = true)
        if (res.isSuccess) eCount++
      }

      _isSyncing.value = false
      val report = SyncReport(
        success = true,
        questionsSynced = qCount,
        materialsSynced = mCount,
        examsSynced = eCount,
        coursesSynced = cCount,
        message = "Pushed $cCount courses, $mCount materials, $lCount libraries, $eCount exams & $qCount questions LIVE to Supabase (${SupabaseConfig.PROJECT_ID})!"
      )
      _cloudStatusMessage.value = "Synced Live to Supabase at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}"
      _lastSyncReport.value = report
      report
    } catch (e: Exception) {
      _isSyncing.value = false
      val report = SyncReport(false, message = "Supabase push error: ${e.message}")
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
      prefs.edit().putString("global_card_style", style).apply()
      val payload = SupabaseAppConfig(
        id = "global",
        cardStyle = style
      )
      val json = supabaseClient.moshi.adapter(SupabaseAppConfig::class.java).toJson(payload)
      supabaseClient.restPost("app_config", json, upsert = true)
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

      val payload = SupabaseAppConfig(
        id = "global",
        aboutDescription = content.description,
        versionNotes = content.versionNotes,
        departmentContact = content.departmentContact,
        credits = content.credits,
        hasUpdateNotice = content.hasUpdateNotice,
        updateNotes = content.updateNotes
      )
      val json = supabaseClient.moshi.adapter(SupabaseAppConfig::class.java).toJson(payload)
      supabaseClient.restPost("app_config", json, upsert = true)

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
      val item = SupabaseStudentFeedback(
        studentName = studentName,
        studentEmail = studentEmail,
        category = category,
        message = message
      )
      val json = supabaseClient.moshi.adapter(SupabaseStudentFeedback::class.java).toJson(item)
      supabaseClient.restPost("student_feedback", json, upsert = false)

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

  suspend fun generateSampleCloudMaterials(courseId: Long): Result<Int> = withContext(Dispatchers.IO) {
    try {
      val sampleMaterials = listOf(
        StudyMaterialEntity(
          courseId = courseId,
          topicId = 1L,
          title = "Radiographic Physics & Radiation Protection Comprehensive Guide",
          description = "Official departmental clinical study pack covering inverse square law, filtration, and ALARA.",
          contentType = "Lecture Note",
          contentBody = "Section 1: Fundamental Principles of Radiation Protection\n- ALARA Principle (As Low As Reasonably Achievable)\n- Time, Distance, and Shielding fundamentals\n- Diagnostic reference levels for common examinations.",
          externalUrl = "https://drive.google.com/drive/folders/sample-medrad-drive-physics",
          isPremium = false
        ),
        StudyMaterialEntity(
          courseId = courseId,
          topicId = 2L,
          title = "Clinical Radiographic Positioning & Anatomy Manual",
          description = "Illustrated standard and special projections for extremities, chest, and spine examinations.",
          contentType = "Handout",
          contentBody = "Standard Projections Review:\n- Chest PA and Lateral positioning criteria\n- Upper extremity immobilization techniques\n- Quality assurance for diagnostic radiographs.",
          externalUrl = "https://drive.google.com/drive/folders/sample-medrad-positioning-atlas",
          isPremium = false
        )
      )
      for (m in sampleMaterials) {
        database.studyMaterialDao().insertMaterial(m)
      }
      pushAllContentToCloud()
      Result.success(sampleMaterials.size)
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
        difficulty = q.difficulty,
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
      prefs.edit()
        .putString("sprint_badge", config.badge)
        .putString("sprint_title", config.title)
        .putString("sprint_subtitle", config.subtitle)
        .putLong("sprint_course_id", config.courseId)
        .putInt("sprint_question_count", config.questionCount)
        .putString("sprint_active_students", config.activeStudentsText)
        .apply()

      val payload = mapOf(
        "id" to "global",
        "sprint_badge" to config.badge,
        "sprint_title" to config.title,
        "sprint_subtitle" to config.subtitle,
        "sprint_course_id" to config.courseId,
        "sprint_question_count" to config.questionCount,
        "sprint_active_students" to config.activeStudentsText,
        "updated_at" to System.currentTimeMillis().toString()
      )
      val json = supabaseClient.moshi.adapter(Map::class.java).toJson(payload)
      val result = supabaseClient.restPost("app_config", json, upsert = true)

      // Instantly dispatch to Realtime subscribers
      realtimeManager?.dispatchRealtimeEvent("app_config", "UPDATE", JSONObject(payload))

      if (result.isSuccess) Result.success(true) else Result.failure(result.exceptionOrNull() ?: Exception("Failed to update daily sprint"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
