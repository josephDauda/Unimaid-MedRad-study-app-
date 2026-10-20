package com.example.data.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseProfile(
  val id: String,
  @Json(name = "full_name") val fullName: String? = null,
  val email: String? = null,
  @Json(name = "matric_number") val matricNumber: String? = null,
  val level: String? = null,
  val department: String? = null,
  val faculty: String? = null,
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "app_role") val appRole: String? = "STUDENT",
  @Json(name = "is_active") val isActive: Boolean = true,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCourse(
  val id: Any, // Can be Long, Int, or UUID String from Supabase
  @Json(name = "department_id") val departmentId: Any? = null,
  val code: String? = null,
  val title: String? = null,
  val level: String? = null,
  val semester: String? = null,
  val description: String? = null,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseStudyMaterial(
  val id: Any, // Can be Long, Int, or UUID String from Supabase
  @Json(name = "course_id") val courseId: Any? = null,
  @Json(name = "topic_id") val topicId: Any? = null,
  val title: String? = null,
  val description: String? = null,
  @Json(name = "content_type") val contentType: String? = null,
  @Json(name = "content_body") val contentBody: String? = null,
  @Json(name = "external_url") val externalUrl: String? = null,
  @Json(name = "file_url") val fileUrl: String? = null,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "is_premium") val isPremium: Boolean? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCourseLibrary(
  val id: Any, // Can be Long, Int, or UUID String from Supabase
  @Json(name = "course_id") val courseId: Any? = null,
  @Json(name = "course_code") val courseCode: String? = null,
  val name: String? = null,
  val title: String? = null,
  val description: String? = null,
  val url: String? = null,
  @Json(name = "external_url") val externalUrl: String? = null,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAnnouncement(
  val id: String,
  val title: String? = null,
  val message: String? = null,
  val author: String? = null,
  val priority: String = "Normal",
  val target: String? = null,
  val category: String? = null,
  @Json(name = "sender_id") val senderId: String? = null,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseNotificationItem(
  val id: String,
  val title: String? = null,
  val message: String? = null,
  val priority: String = "Normal",
  val category: String? = null,
  val target: String? = null,
  @Json(name = "recipient_id") val recipientId: String? = null,
  @Json(name = "sender_id") val senderId: String? = null,
  @Json(name = "is_read") val isRead: Boolean = false,
  @Json(name = "read_at") val readAt: String? = null,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseNotificationRead(
  val id: String? = null,
  @Json(name = "notification_id") val notificationId: String,
  @Json(name = "user_id") val userId: String,
  @Json(name = "is_read") val isRead: Boolean = true,
  @Json(name = "is_dismissed") val isDismissed: Boolean = false,
  @Json(name = "read_at") val readAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseExam(
  val id: Any, // Can be Long, Int, or UUID String from Supabase
  @Json(name = "course_id") val courseId: Any? = null,
  val title: String? = null,
  val description: String? = null,
  @Json(name = "duration_minutes") val durationMinutes: Int? = null,
  @Json(name = "total_marks") val totalMarks: Int? = null,
  @Json(name = "pass_mark") val passMark: Double? = 50.0,
  @Json(name = "pass_percentage") val passPercentage: Int? = null,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseQuestion(
  val id: Any, // Can be Long, Int, or UUID String from Supabase
  @Json(name = "exam_id") val examId: Any? = null,
  @Json(name = "course_id") val courseId: Any? = null,
  @Json(name = "topic_id") val topicId: Any? = null,
  @Json(name = "question_text") val questionText: String? = null,
  @Json(name = "option_a") val optionA: String? = null,
  @Json(name = "option_b") val optionB: String? = null,
  @Json(name = "option_c") val optionC: String? = null,
  @Json(name = "option_d") val optionD: String? = null,
  @Json(name = "correct_answer") val correctAnswer: String? = null,
  val explanation: String? = "",
  val difficulty: String? = null,
  @Json(name = "question_type") val questionType: String? = "MULTIPLE_CHOICE",
  @Json(name = "assessment_title") val assessmentTitle: String? = null,
  val marks: Double? = 1.0,
  @Json(name = "time_limit_seconds") val timeLimitSeconds: Int? = null,
  @Json(name = "image_url") val imageUrl: String? = null,
  @Json(name = "is_active") val isActive: Boolean = true,
  @Json(name = "is_premium") val isPremium: Boolean? = null,
  @Json(name = "is_published") val isPublished: Boolean? = null,
  @Json(name = "is_demo") val isDemo: Boolean? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseExamAttempt(
  val id: String? = null,
  @Json(name = "user_id") val userId: String? = null,
  @Json(name = "student_id") val studentId: String? = null,
  @Json(name = "exam_id") val examId: Any? = null,
  @Json(name = "course_id") val courseId: Any? = null,
  val score: Int? = 0,
  @Json(name = "total_questions") val totalQuestions: Int? = null,
  @Json(name = "time_spent_seconds") val timeSpentSeconds: Int? = null,
  @Json(name = "duration_seconds") val durationSeconds: Int? = null,
  @Json(name = "is_passed") val isPassed: Boolean? = null,
  val passed: Boolean? = null,
  val percentage: Double? = null,
  val status: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseStudentFeedback(
  val id: String? = null,
  @Json(name = "user_id") val userId: String? = null,
  @Json(name = "student_id") val studentId: String? = null,
  @Json(name = "student_name") val studentName: String? = null,
  @Json(name = "student_email") val studentEmail: String? = null,
  val subject: String? = null,
  val category: String? = null,
  val message: String = "",
  val status: String? = "PENDING",
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAppConfigKeyValue(
  val key: String? = null,
  val value: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAppConfig(
  val id: String = "global",
  @Json(name = "card_style") val cardStyle: String? = "NAVY_GOLD",
  @Json(name = "about_description") val aboutDescription: String? = null,
  @Json(name = "version_notes") val versionNotes: String? = null,
  @Json(name = "department_contact") val departmentContact: String? = null,
  val credits: String? = null,
  @Json(name = "has_update_notice") val hasUpdateNotice: Boolean = false,
  @Json(name = "update_notes") val updateNotes: String? = null,
  @Json(name = "sprint_title") val sprintTitle: String? = "Radiography Challenge",
  @Json(name = "sprint_subtitle") val sprintSubtitle: String? = "15 practice questions ready for your level",
  @Json(name = "sprint_badge") val sprintBadge: String? = "DAILY CBT SPRINT",
  @Json(name = "sprint_course_id") val sprintCourseId: Any? = 1L,
  @Json(name = "sprint_course_code") val sprintCourseCode: String? = "RAD 101",
  @Json(name = "sprint_question_count") val sprintQuestionCount: Int? = 15,
  @Json(name = "sprint_active_students") val sprintActiveStudents: String? = "38 students active",
  @Json(name = "sprint_theme") val sprintTheme: String? = "NAVY_GOLD",
  @Json(name = "practice_card_course_id") val practiceCardCourseId: Any? = 1L,
  @Json(name = "practice_card_course_code") val practiceCardCourseCode: String? = "RAD 101",
  @Json(name = "practice_card_course_title") val practiceCardCourseTitle: String? = "Radiation Physics & Safety",
  @Json(name = "practice_card_question_count") val practiceCardQuestionCount: Int? = 15,
  @Json(name = "practice_card_is_active") val practiceCardIsActive: Boolean? = true,
  @Json(name = "mock_card_course_id") val mockCardCourseId: Any? = 1L,
  @Json(name = "mock_card_course_code") val mockCardCourseCode: String? = "RAD 101",
  @Json(name = "mock_card_course_title") val mockCardCourseTitle: String? = "Radiation Physics & Safety",
  @Json(name = "mock_card_exam_id") val mockCardExamId: Any? = 0L,
  @Json(name = "mock_card_exam_title") val mockCardExamTitle: String? = "RAD 101 Mock Exam",
  @Json(name = "mock_card_question_count") val mockCardQuestionCount: Int? = 15,
  @Json(name = "mock_card_duration_minutes") val mockCardDurationMinutes: Int? = 30,
  @Json(name = "mock_card_is_active") val mockCardIsActive: Boolean? = true,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
  @Json(name = "access_token") val accessToken: String? = null,
  @Json(name = "token_type") val tokenType: String? = null,
  @Json(name = "expires_in") val expiresIn: Long? = null,
  @Json(name = "refresh_token") val refreshToken: String? = null,
  val user: SupabaseAuthUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUser(
  val id: String,
  val email: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)
