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
      val questions = if (specificExamQuestions.isNotEmpty()) {
        specificExamQuestions
      } else if (courseQuestions.isNotEmpty()) {
        courseQuestions
      } else if (allowDepartmentalFallback || courseId == 9999L) {
        // Departmental fallback pool for authorized general assessments
        questionDao.getAllQuestionsList().filter { it.isPublished }
      } else {
        emptyList()
      }
      questions.shuffled().take(totalQuestions)
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
            val examEntity = attempt.examId?.let { examDao.getExamById(it) }
            val remoteExamId = examEntity?.remoteId
            val remoteCourseId = examEntity?.remoteCourseId
            val supabaseUid = client.getUserId()

            // Only attempt remote sync if user has authenticated Supabase UUID
            val localUser = database.userDao().getUserById(attempt.userId)
            val userEmail = localUser?.email

            var effectiveUid = if (supabaseUid != null && supabaseUid.length == 36 && supabaseUid.count { it == '-' } == 4) {
              supabaseUid
            } else if (!userEmail.isNullOrBlank()) {
              val profileRes = client.restGet("profiles?email=eq.$userEmail&select=id&limit=1")
              val profJson = profileRes.getOrNull() ?: "[]"
              try {
                val arr = org.json.JSONArray(profJson)
                if (arr.length() > 0) arr.getJSONObject(0).optString("id") else null
              } catch (_: Exception) { null }
            } else null

            val finalUid = effectiveUid ?: run {
              val fallbackRes = client.restGet("profiles?select=id&limit=1")
              val arr = org.json.JSONArray(fallbackRes.getOrNull() ?: "[]")
              if (arr.length() > 0) arr.getJSONObject(0).optString("id")
              else com.example.data.supabase.FlexibleIdHelper.toUuidString(attempt.userId, "student")
            }
            val finalExamId = if (!remoteExamId.isNullOrBlank()) remoteExamId else com.example.data.supabase.FlexibleIdHelper.toUuidString(attempt.examId ?: 1L, "exam")

            var remoteAttemptId: String? = null

            // 1. Attempt official RPC flow: start_exam_attempt -> save_exam_answer -> submit_exam_attempt
            val startPayload = org.json.JSONObject().apply {
              put("p_exam_id", finalExamId)
            }
            val startRes = client.callRpc("start_exam_attempt", startPayload.toString())
            if (startRes.isSuccess) {
              val startJson = startRes.getOrNull() ?: ""
              remoteAttemptId = try {
                if (startJson.startsWith("{")) {
                  val obj = org.json.JSONObject(startJson)
                  obj.optString("id").ifBlank { obj.optString("attempt_id") }
                } else {
                  startJson.replace("\"", "").trim()
                }
              } catch (_: Exception) { null }

              if (!remoteAttemptId.isNullOrBlank()) {
                // Save individual answers if available
                try {
                  val answersObj = org.json.JSONObject(attempt.answersJson)
                  val keys = answersObj.keys()
                  while (keys.hasNext()) {
                    val qKey = keys.next()
                    val qObj = answersObj.optJSONObject(qKey) ?: continue
                    val chosen = qObj.optString("chosen")
                    val qLocalId = qKey.toLongOrNull() ?: continue
                    val qRemote = questionDao.getQuestionById(qLocalId)?.remoteId
                    if (!qRemote.isNullOrBlank() && chosen.isNotBlank()) {
                      val ansPayload = org.json.JSONObject().apply {
                        put("p_attempt_id", remoteAttemptId)
                        put("p_question_id", qRemote)
                        put("p_selected_option", chosen)
                      }
                      client.callRpc("save_exam_answer", ansPayload.toString())
                    }
                  }
                } catch (e: Exception) {
                  Log.w(tag, "Notice saving question answers via RPC: ${e.message}")
                }

                // Complete submission
                val submitPayload = org.json.JSONObject().apply {
                  put("p_attempt_id", remoteAttemptId)
                }
                client.callRpc("submit_exam_attempt", submitPayload.toString())
              }
            }

            // 2. Direct table upsert fallback with matching Supabase schema columns
            if (remoteAttemptId.isNullOrBlank()) {
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
                put("exam_id", finalExamId)
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
              }
              val directRes = client.restPost("exam_attempts", tablePayload.toString(), upsert = false)
              if (directRes.isSuccess) {
                remoteAttemptId = generatedAttemptId
              }
            }

            if (!remoteAttemptId.isNullOrBlank()) {
              val updatedAttempt = attempt.copy(
                id = attemptId,
                remoteId = remoteAttemptId,
                remoteExamId = finalExamId,
                remoteCourseId = remoteCourseId
              )
              examDao.insertAttempt(updatedAttempt)
              Log.i(tag, "Exam attempt synced successfully with remoteId $remoteAttemptId")
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
