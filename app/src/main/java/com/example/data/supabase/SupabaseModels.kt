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
  val id: Long,
  @Json(name = "department_id") val departmentId: Long? = 1L,
  val code: String,
  val title: String,
  val level: String? = null,
  val semester: String? = null,
  val description: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseStudyMaterial(
  val id: Long,
  @Json(name = "course_id") val courseId: Long,
  @Json(name = "topic_id") val topicId: Long? = null,
  val title: String,
  val description: String? = null,
  @Json(name = "content_type") val contentType: String = "Lecture Note",
  @Json(name = "content_body") val contentBody: String = "",
  @Json(name = "external_url") val externalUrl: String? = null,
  @Json(name = "is_premium") val isPremium: Boolean = false,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCourseLibrary(
  val id: Long,
  @Json(name = "course_id") val courseId: Long,
  @Json(name = "course_code") val courseCode: String? = null,
  val name: String,
  val description: String? = null,
  @Json(name = "external_url") val externalUrl: String,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAnnouncement(
  val id: String,
  val title: String,
  val message: String,
  val author: String = "Department of Medical Radiography",
  val priority: String = "Normal",
  val target: String = "All Students",
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseExam(
  val id: Long,
  @Json(name = "course_id") val courseId: Long,
  val title: String,
  val description: String? = null,
  @Json(name = "duration_minutes") val durationMinutes: Int = 30,
  @Json(name = "total_marks") val totalMarks: Int = 100,
  @Json(name = "pass_percentage") val passPercentage: Int = 50,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseQuestion(
  val id: Long,
  @Json(name = "course_id") val courseId: Long,
  @Json(name = "topic_id") val topicId: Long? = null,
  @Json(name = "question_text") val questionText: String,
  @Json(name = "option_a") val optionA: String,
  @Json(name = "option_b") val optionB: String,
  @Json(name = "option_c") val optionC: String,
  @Json(name = "option_d") val optionD: String,
  val difficulty: String = "MEDIUM",
  @Json(name = "is_premium") val isPremium: Boolean = false,
  @Json(name = "is_published") val isPublished: Boolean = true,
  @Json(name = "is_demo") val isDemo: Boolean = false,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseExamAttempt(
  val id: String? = null,
  @Json(name = "user_id") val userId: String,
  @Json(name = "course_id") val courseId: Long,
  val score: Int,
  @Json(name = "total_questions") val totalQuestions: Int,
  @Json(name = "time_spent_seconds") val timeSpentSeconds: Int,
  @Json(name = "is_passed") val isPassed: Boolean,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseStudentFeedback(
  val id: String? = null,
  @Json(name = "user_id") val userId: String? = null,
  @Json(name = "student_name") val studentName: String = "Student",
  @Json(name = "student_email") val studentEmail: String = "",
  val category: String = "Academic Inquiry",
  val message: String = "",
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAppConfigKeyValue(
  val key: String? = null,
  val value: String? = null
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
  @Json(name = "sprint_course_id") val sprintCourseId: Long? = 1L,
  @Json(name = "sprint_question_count") val sprintQuestionCount: Int? = 15,
  @Json(name = "sprint_active_students") val sprintActiveStudents: String? = "38 students active",
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
