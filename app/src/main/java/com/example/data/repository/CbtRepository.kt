package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DailyUsageEntity
import com.example.data.local.entity.ExamAttemptEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseExamAttempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CbtRepository(
  private val database: AppDatabase,
  private val context: Context? = null
) {

  private val questionDao = database.questionDao()
  private val examDao = database.examDao()
  private val dailyUsageDao = database.dailyUsageDao()
  private val scope = CoroutineScope(Dispatchers.IO)
  private val tag = "CbtRepository"

  private val supabaseClient: SupabaseClient? = context?.let { SupabaseClient(it) }

  private fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
  }

  suspend fun getRemainingAllowance(user: UserEntity): Int {
    return withContext(Dispatchers.IO) {
      if (user.isPremium || user.role == "ADMIN") {
        return@withContext 9999 // Unlimited for premium/admin
      }
      val dateStr = getTodayDateString()
      val usage = dailyUsageDao.getUsage(user.id, dateStr)
      val answered = usage?.questionsAnsweredToday ?: 0
      val remaining = 15 - answered
      if (remaining < 0) 0 else remaining
    }
  }

  suspend fun canAnswerQuestion(user: UserEntity): Boolean {
    if (user.isPremium || user.role == "ADMIN") return true
    return getRemainingAllowance(user) > 0
  }

  suspend fun recordQuestionAnswered(user: UserEntity): Int {
    return withContext(Dispatchers.IO) {
      if (user.isPremium || user.role == "ADMIN") {
        return@withContext 9999
      }
      val dateStr = getTodayDateString()
      val usage = dailyUsageDao.getUsage(user.id, dateStr)
      val currentCount = usage?.questionsAnsweredToday ?: 0
      val newCount = currentCount + 1
      dailyUsageDao.saveUsage(
        DailyUsageEntity(
          id = usage?.id ?: 0,
          userId = user.id,
          dateString = dateStr,
          questionsAnsweredToday = newCount
        )
      )
      val remaining = 15 - newCount
      if (remaining < 0) 0 else remaining
    }
  }

  suspend fun getQuestionsByIds(ids: List<Long>): List<QuestionEntity> {
    return withContext(Dispatchers.IO) {
      if (ids.isEmpty()) emptyList() else questionDao.getQuestionsByIds(ids)
    }
  }

  suspend fun getPracticeQuestions(courseId: Long, limit: Int = 15): List<QuestionEntity> {
    return withContext(Dispatchers.IO) {
      val questions = questionDao.getPublishedQuestionsForCourseList(courseId)
      questions.shuffled().take(limit)
    }
  }

  suspend fun getExamQuestions(
    courseId: Long,
    totalQuestions: Int,
    examRemoteId: String? = null,
    allowDepartmentalFallback: Boolean = false
  ): List<QuestionEntity> {
    return withContext(Dispatchers.IO) {
      val specificExamQuestions = if (!examRemoteId.isNullOrBlank()) {
        questionDao.getQuestionsByRemoteExamId(examRemoteId).filter { it.isPublished }
      } else emptyList()

      val courseQuestions = questionDao.getPublishedQuestionsForCourseList(courseId)
      val basePool = (specificExamQuestions + courseQuestions).distinctBy { it.id }.toMutableList()

      if (basePool.size < totalQuestions && (allowDepartmentalFallback || courseId == 9999L)) {
        val extra = questionDao.getAllQuestionsList().filter { it.isPublished }
        for (q in extra) {
          if (!basePool.any { it.id == q.id }) {
            basePool.add(q)
            if (basePool.size >= totalQuestions) break
          }
        }
      }

      basePool.shuffled().take(totalQuestions)
    }
  }

  suspend fun getExamById(examId: Long): ExamEntity? {
    return withContext(Dispatchers.IO) {
      examDao.getExamById(examId)
    }
  }

  suspend fun submitExamAttempt(attempt: ExamAttemptEntity): Long {
    return withContext(Dispatchers.IO) {
      val attemptId = examDao.insertAttempt(attempt)

      // Sync attempt to Supabase with authentic UUIDs and backend functions
      supabaseClient?.let { client ->
        scope.launch {
          try {
            val localUser = database.userDao().getUserById(attempt.userId)
            val userEmail = localUser?.email?.trim()?.lowercase()

            val examEntity = attempt.examId?.let { examDao.getExamById(it) }
            var remoteExamId = examEntity?.remoteId
            var remoteCourseId = examEntity?.remoteCourseId

            // If remoteCourseId is null, resolve from local courses by title/code
            if (remoteCourseId.isNullOrBlank()) {
              val allCourses = database.academicDao().getCoursesList()
              val matched = allCourses.find {
                it.title.equals(attempt.courseTitle, ignoreCase = true) ||
                attempt.examTitle.contains(it.code, ignoreCase = true) ||
                attempt.courseTitle.contains(it.code, ignoreCase = true)
              }
              remoteCourseId = matched?.remoteId
            }

            // 1. Resolve student UUID from Supabase profiles (or create/upsert profile if missing)
            var finalUid: String? = null
            if (!userEmail.isNullOrBlank()) {
              val profileRes = client.restGet("profiles?email=eq.$userEmail&select=id&limit=1")
              val profJson = profileRes.getOrNull() ?: "[]"
              try {
                val arr = org.json.JSONArray(profJson)
                if (arr.length() > 0) finalUid = arr.getJSONObject(0).optString("id")
              } catch (_: Exception) {}

              if (finalUid.isNullOrBlank()) {
                val newProfileUuid = java.util.UUID.randomUUID().toString()
                val profilePayload = org.json.JSONObject().apply {
                  put("id", newProfileUuid)
                  put("email", userEmail)
                  put("full_name", localUser.fullName)
                  put("matric_number", localUser.phoneNumber)
                  put("department", localUser.department)
                  put("faculty", localUser.faculty)
                  put("level", localUser.level)
                  put("app_role", if (localUser.role == "ADMIN") "admin" else "student")
                  put("is_active", true)
                }
                val upRes = client.restPost("profiles", profilePayload.toString(), upsert = true)
                if (upRes.isSuccess) finalUid = newProfileUuid
              }
            }

            if (finalUid.isNullOrBlank()) {
              finalUid = if (!userEmail.isNullOrBlank()) {
                java.util.UUID.nameUUIDFromBytes("supabase:$userEmail".toByteArray()).toString()
              } else {
                com.example.data.supabase.FlexibleIdHelper.toUuidString(attempt.userId, "student")
              }
              if (localUser != null) {
                val profilePayload = org.json.JSONObject().apply {
                  put("id", finalUid)
                  if (!userEmail.isNullOrBlank()) put("email", userEmail)
                  put("full_name", localUser.fullName)
                  put("matric_number", localUser.phoneNumber)
                  put("department", localUser.department)
                  put("faculty", localUser.faculty)
                  put("level", localUser.level)
                  put("app_role", if (localUser.role == "ADMIN") "admin" else "student")
                  put("is_active", true)
                }
                client.restPost("profiles", profilePayload.toString(), upsert = true)
              }
            }

            // 2. Resolve or create exam in Supabase exams table to satisfy foreign key constraint
            var resolvedExamId = remoteExamId
            if (resolvedExamId.isNullOrBlank() || resolvedExamId.length != 36) {
              val queryEndpoint = if (!remoteCourseId.isNullOrBlank()) "exams?course_id=eq.$remoteCourseId&limit=1" else "exams?limit=1"
              val examRes = client.restGet(queryEndpoint)
              val examJson = examRes.getOrNull() ?: "[]"
              try {
                val arr = org.json.JSONArray(examJson)
                if (arr.length() > 0) {
                  resolvedExamId = arr.getJSONObject(0).optString("id")
                }
              } catch (_: Exception) {}

              if (resolvedExamId.isNullOrBlank()) {
                val newExamUuid = java.util.UUID.randomUUID().toString()
                val examPayload = org.json.JSONObject().apply {
                  put("id", newExamUuid)
                  if (!remoteCourseId.isNullOrBlank()) put("course_id", remoteCourseId)
                  put("title", attempt.examTitle.ifBlank { "Departmental CBT Examination" })
                  put("duration_minutes", maxOf(10, attempt.timeSpentSeconds / 60))
                  put("total_questions", attempt.totalQuestions)
                  put("is_published", true)
                }
                val res = client.restPost("exams", examPayload.toString(), upsert = true)
                if (res.isSuccess) resolvedExamId = newExamUuid
              }
            }

            // 3. Post exam attempt directly to Supabase exam_attempts with full candidate metadata
            var remoteAttemptId: String? = null
            if (!resolvedExamId.isNullOrBlank() && !finalUid.isNullOrBlank()) {
              val generatedAttemptId = java.util.UUID.randomUUID().toString()
              val pct = (attempt.score.toDouble() * 100.0) / maxOf(attempt.totalQuestions, 1)
              val isPassed = pct >= 50.0
              val isoDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
              }.format(java.util.Date())

              val tablePayload = org.json.JSONObject().apply {
                put("id", generatedAttemptId)
                put("user_id", finalUid)
                put("student_id", finalUid)
                put("exam_id", resolvedExamId)
                if (!remoteCourseId.isNullOrBlank()) {
                  put("course_id", remoteCourseId)
                }
                put("score", attempt.score)
                put("total_questions", attempt.totalQuestions)
                put("duration_seconds", attempt.timeSpentSeconds)
                put("time_spent_seconds", attempt.timeSpentSeconds)
                put("status", "submitted")
                put("submitted_at", isoDate)
                put("passed", isPassed)
                put("is_passed", isPassed)
                put("percentage", pct)
                if (localUser != null) {
                  put("student_name", localUser.fullName)
                  put("student_email", localUser.email)
                  put("student_phone", localUser.phoneNumber)
                }
              }
              var directRes = client.restPost("exam_attempts", tablePayload.toString(), upsert = false)
              if (directRes.isFailure) {
                val err = directRes.exceptionOrNull()?.message ?: ""
                if (err.contains("PGRST204") || err.contains("Could not find the") || err.contains("column") || err.contains("student_name")) {
                  tablePayload.remove("student_name")
                  tablePayload.remove("student_email")
                  tablePayload.remove("student_phone")
                  directRes = client.restPost("exam_attempts", tablePayload.toString(), upsert = false)
                }
              }
              if (directRes.isSuccess) {
                remoteAttemptId = generatedAttemptId
                Log.i(tag, "Mock exam attempt synced to Supabase successfully: $generatedAttemptId")
              } else {
                Log.w(tag, "exam_attempts insert status: ${directRes.exceptionOrNull()?.message}")
              }
            }

            if (!remoteAttemptId.isNullOrBlank()) {
              val updatedAttempt = attempt.copy(
                id = attemptId,
                remoteId = remoteAttemptId,
                remoteExamId = resolvedExamId,
                remoteCourseId = remoteCourseId
              )
              examDao.insertAttempt(updatedAttempt)
            }
          } catch (e: Exception) {
            Log.w(tag, "Supabase attempt sync notice: ${e.message}")
          }
        }
      }

      attemptId
    }
  }

  suspend fun getAttemptById(attemptId: Long): ExamAttemptEntity? {
    return withContext(Dispatchers.IO) {
      examDao.getAttemptById(attemptId)
    }
  }

  fun getUserAttempts(userId: Long): Flow<List<ExamAttemptEntity>> {
    return examDao.getAttemptsForUser(userId)
  }

  suspend fun getUserAttemptsList(userId: Long, limit: Int = 10): List<ExamAttemptEntity> {
    return withContext(Dispatchers.IO) {
      examDao.getRecentAttempts(userId, limit)
    }
  }
}
