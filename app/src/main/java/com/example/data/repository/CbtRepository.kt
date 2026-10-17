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
      if (questions.isEmpty()) {
        val fallback = questionDao.getAllQuestionsList().filter { it.isPublished }
        fallback.shuffled().take(limit)
      } else {
        questions.shuffled().take(limit)
      }
    }
  }

  suspend fun getExamQuestions(courseId: Long, totalQuestions: Int): List<QuestionEntity> {
    return withContext(Dispatchers.IO) {
      val questions = questionDao.getPublishedQuestionsForCourseList(courseId)
      if (questions.isEmpty()) {
        val fallback = questionDao.getAllQuestionsList().filter { it.isPublished }
        fallback.shuffled().take(totalQuestions)
      } else {
        questions.shuffled().take(totalQuestions)
      }
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
            val isValidUuid = supabaseUid != null && supabaseUid.length == 36 && supabaseUid.count { it == '-' } == 4
            if (!isValidUuid || remoteExamId.isNullOrBlank()) {
              Log.i(tag, "Attempt $attemptId saved locally in Room. Cloud sync requires valid Supabase user session & exam UUID.")
              return@launch
            }

            var remoteAttemptId: String? = null

            // 1. Attempt official RPC flow: start_exam_attempt -> save_exam_answer -> submit_exam_attempt
            val startPayload = org.json.JSONObject().apply {
              put("p_exam_id", remoteExamId)
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
              val isPassed = (attempt.score * 100) / maxOf(attempt.totalQuestions, 1) >= 50
              val tablePayload = org.json.JSONObject().apply {
                put("id", generatedAttemptId)
                put("user_id", supabaseUid)
                put("student_id", supabaseUid)
                put("exam_id", remoteExamId)
                if (!remoteCourseId.isNullOrBlank()) {
                  put("course_id", remoteCourseId)
                }
                put("score", attempt.score)
                put("total_questions", attempt.totalQuestions)
                put("duration_seconds", attempt.timeSpentSeconds)
                put("status", "COMPLETED")
                put("passed", isPassed)
                put("percentage", attempt.score.toDouble())
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
                remoteExamId = remoteExamId,
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
