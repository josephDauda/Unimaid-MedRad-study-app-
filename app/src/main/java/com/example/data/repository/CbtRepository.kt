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

      // Sync attempt to Supabase
      supabaseClient?.let { client ->
        scope.launch {
          try {
            val isPassed = (attempt.score * 100) / maxOf(attempt.totalQuestions, 1) >= 50
            val dto = SupabaseExamAttempt(
              userId = client.getUserId() ?: "user_${attempt.userId}",
              courseId = attempt.examId ?: 1L,
              score = attempt.score,
              totalQuestions = attempt.totalQuestions,
              timeSpentSeconds = attempt.timeSpentSeconds,
              isPassed = isPassed
            )
            val json = client.moshi.adapter(SupabaseExamAttempt::class.java).toJson(dto)
            client.restPost("exam_attempts", json, upsert = false)
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
