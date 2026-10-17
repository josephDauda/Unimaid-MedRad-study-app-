package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val courseId: Long,
  val topicId: Long,
  val questionText: String,
  val optionA: String,
  val optionB: String,
  val optionC: String,
  val optionD: String,
  val correctAnswer: String, // "A", "B", "C", "D"
  val explanation: String,
  val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
  val isPremium: Boolean = false,
  val isPublished: Boolean = true,
  val isDemo: Boolean = true, // Clearly labelled Demo Question as per guidelines
  val questionType: String = "MULTIPLE_CHOICE", // "MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_IN_BLANK", "IMAGE_DIAGRAM", "DRAWING_LABEL"
  val assessmentTitle: String = "General Practice",
  val imageUrl: String? = null,
  val timeLimitSeconds: Int = 60,
  val marks: Int = 1,
  val createdAt: Long = System.currentTimeMillis(),
  val remoteId: String? = null,
  val remoteExamId: String? = null,
  val remoteCourseId: String? = null
)

@Entity(tableName = "exams")
data class ExamEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val courseId: Long,
  val title: String,
  val description: String,
  val durationMinutes: Int = 30,
  val totalQuestions: Int = 15,
  val isPremium: Boolean = false,
  val isPublished: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val remoteId: String? = null,
  val remoteCourseId: String? = null
)

@Entity(tableName = "exam_attempts")
data class ExamAttemptEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val userId: Long,
  val examId: Long? = null,
  val courseTitle: String,
  val examTitle: String,
  val score: Int,
  val totalQuestions: Int,
  val correctAnswers: Int,
  val incorrectAnswers: Int,
  val unansweredQuestions: Int,
  val timeSpentSeconds: Int,
  val answersJson: String, // JSON mapping questionId to chosen option & correctness
  val timestamp: Long = System.currentTimeMillis(),
  val remoteId: String? = null,
  val remoteExamId: String? = null,
  val remoteCourseId: String? = null
)

@Entity(tableName = "study_materials")
data class StudyMaterialEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val courseId: Long,
  val topicId: Long,
  val title: String,
  val description: String,
  val contentType: String, // "Lecture Note", "Handout", "Past Question", "Revision Summary", "Course Outline"
  val contentBody: String,
  val externalUrl: String? = null,
  val isPremium: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val remoteId: String? = null,
  val remoteCourseId: String? = null
)

@Entity(tableName = "course_libraries")
data class CourseLibraryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val courseId: Long,
  val courseCode: String = "", // e.g. "RAD 101"
  val name: String, // e.g. "RAD 101 Lecture Library"
  val description: String, // e.g. "Lecture notes and study resources"
  val externalUrl: String, // Google Drive URL or external resource link
  val isPublished: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val remoteId: String? = null,
  val remoteCourseId: String? = null
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val userId: Long,
  val planName: String, // "15-Day Plan", "30-Day Plan"
  val amount: Double,
  val startDate: Long,
  val expiryDate: Long,
  val paymentReference: String,
  val paymentStatus: String, // "COMPLETED", "PENDING"
  val subscriptionStatus: String, // "ACTIVE", "EXPIRED"
  val createdAt: Long = System.currentTimeMillis()
)
