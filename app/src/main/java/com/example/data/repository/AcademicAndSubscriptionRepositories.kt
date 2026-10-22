package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.StudyMaterialEntity
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UniversityEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class AcademicRepository(private val database: AppDatabase) {
  private val academicDao = database.academicDao()
  private val examDao = database.examDao()
  private val materialDao = database.studyMaterialDao()
  private val questionDao = database.questionDao()
  private val libraryDao = database.courseLibraryDao()

  fun getAllUniversities(): Flow<List<UniversityEntity>> = academicDao.getAllUniversities()
  fun getFacultiesByUniversity(uniId: Long): Flow<List<FacultyEntity>> = academicDao.getFacultiesByUniversity(uniId)
  fun getDepartmentsByFaculty(facId: Long): Flow<List<DepartmentEntity>> = academicDao.getDepartmentsByFaculty(facId)
  fun getAllCourses(): Flow<List<CourseEntity>> = academicDao.getAllCourses()
  fun getCoursesByDepartment(deptId: Long): Flow<List<CourseEntity>> = academicDao.getCoursesByDepartment(deptId)
  fun observeCourse(courseId: Long): Flow<CourseEntity?> = academicDao.observeCourseById(courseId)
  suspend fun getCourse(courseId: Long): CourseEntity? = academicDao.getCourseById(courseId)
  suspend fun getCourseById(courseId: Long): CourseEntity? = academicDao.getCourseById(courseId)
  suspend fun getCourseByCode(code: String): CourseEntity? = academicDao.getCourseByCode(code)

  fun getTopicsByCourse(courseId: Long): Flow<List<TopicEntity>> = academicDao.getTopicsByCourse(courseId)

  fun getExamsForCourse(courseId: Long): Flow<List<ExamEntity>> = examDao.getExamsForCourse(courseId)
  fun getAllExams(): Flow<List<ExamEntity>> = examDao.getAllExams()
  fun getPublishedExams(): Flow<List<ExamEntity>> = examDao.getPublishedExams()
  suspend fun getExamById(id: Long): ExamEntity? = examDao.getExamById(id)

  fun getMaterialsForCourse(courseId: Long): Flow<List<StudyMaterialEntity>> = materialDao.getMaterialsForCourse(courseId)
  fun getAllMaterials(): Flow<List<StudyMaterialEntity>> = materialDao.getAllMaterials()
  suspend fun getMaterialById(id: Long): StudyMaterialEntity? = materialDao.getMaterialById(id)

  fun getQuestionsForCourse(courseId: Long): Flow<List<QuestionEntity>> = questionDao.getQuestionsForCourse(courseId)
  fun getAllQuestions(): Flow<List<QuestionEntity>> = questionDao.getAllQuestions()

  fun getLibrariesForCourse(courseId: Long): Flow<List<CourseLibraryEntity>> = libraryDao.getLibrariesForCourse(courseId)
  fun getAllLibraries(): Flow<List<CourseLibraryEntity>> = libraryDao.getAllLibraries()
  suspend fun getLibraryById(id: Long): CourseLibraryEntity? = libraryDao.getLibraryById(id)
}

data class SubscriptionPlan(
  val id: String,
  val name: String,
  val durationDays: Int,
  val priceNaira: Int,
  val formattedPrice: String,
  val description: String,
  val features: List<String>
)

class SubscriptionRepository(private val database: AppDatabase) {
  private val subscriptionDao = database.subscriptionDao()
  private val userDao = database.userDao()

  val availablePlans = listOf(
    SubscriptionPlan(
      id = "plan_15_days",
      name = "15-Day Plan",
      durationDays = 15,
      priceNaira = 3500,
      formattedPrice = "₦3,500",
      description = "Ideal for rapid exam revisions and continuous CBT mock rehearsals.",
      features = listOf(
        "Unlimited CBT practice questions",
        "Full RAD 101 question bank & answers",
        "Timed mock examinations",
        "Detailed answer rationales & explanations",
        "Premium past questions & lecture summaries",
        "Comprehensive performance analytics"
      )
    ),
    SubscriptionPlan(
      id = "plan_30_days",
      name = "30-Day Plan",
      durationDays = 30,
      priceNaira = 7000,
      formattedPrice = "₦7,000",
      description = "Complete semester examination preparation for dedicated students.",
      features = listOf(
        "Everything in 15-Day Plan",
        "Full month unlimited practice & mocks",
        "Offline-ready study materials access",
        "Weak topic breakdown & revision drills",
        "Priority student academic support",
        "Continuous past question archives"
      )
    )
  )

  suspend fun processSubscription(
    userId: Long,
    plan: SubscriptionPlan,
    channel: String = "CARD",
    customReference: String? = null
  ): Result<SubscriptionEntity> {
    return withContext(Dispatchers.IO) {
      try {
        val now = System.currentTimeMillis()
        val durationMillis = plan.durationDays * 24L * 60 * 60 * 1000
        val expiry = now + durationMillis
        val paymentRef = customReference ?: "pstk_medrad_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6).lowercase()}"

        val sub = SubscriptionEntity(
          userId = userId,
          planName = plan.name,
          amount = plan.priceNaira.toDouble(),
          startDate = now,
          expiryDate = expiry,
          paymentReference = "$paymentRef [$channel]",
          paymentStatus = "COMPLETED",
          subscriptionStatus = "ACTIVE"
        )

        val id = subscriptionDao.insertSubscription(sub)
        val saved = sub.copy(id = id)

        // Update user premium state
        userDao.updatePremiumStatus(userId, true, plan.name, expiry)

        Result.success(saved)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  suspend fun getActiveSubscription(userId: Long): SubscriptionEntity? {
    return withContext(Dispatchers.IO) {
      subscriptionDao.getActiveSubscription(userId, System.currentTimeMillis())
    }
  }

  fun getSubscriptionsForUser(userId: Long): Flow<List<SubscriptionEntity>> {
    return subscriptionDao.getSubscriptionsForUser(userId)
  }
}
