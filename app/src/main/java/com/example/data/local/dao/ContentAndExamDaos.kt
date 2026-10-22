package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.ExamAttemptEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.StudyMaterialEntity
import com.example.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
  @Query("SELECT * FROM questions ORDER BY id DESC")
  fun getAllQuestions(): Flow<List<QuestionEntity>>

  @Query("SELECT * FROM questions ORDER BY id ASC")
  suspend fun getAllQuestionsList(): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE (courseId = :courseId OR remoteCourseId = (SELECT remoteId FROM courses WHERE id = :courseId LIMIT 1)) ORDER BY id ASC")
  fun getQuestionsForCourse(courseId: Long): Flow<List<QuestionEntity>>

  @Query("SELECT * FROM questions WHERE (courseId = :courseId OR remoteCourseId = (SELECT remoteId FROM courses WHERE id = :courseId LIMIT 1)) AND isPublished = 1 ORDER BY RANDOM() LIMIT :limit")
  suspend fun getRandomQuestionsForCourse(courseId: Long, limit: Int): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE (courseId = :courseId OR remoteCourseId = (SELECT remoteId FROM courses WHERE id = :courseId LIMIT 1)) AND isPublished = 1 ORDER BY id ASC")
  suspend fun getPublishedQuestionsForCourseList(courseId: Long): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
  suspend fun getQuestionById(id: Long): QuestionEntity?

  @Query("SELECT * FROM questions WHERE remoteId = :remoteId LIMIT 1")
  suspend fun getQuestionByRemoteId(remoteId: String): QuestionEntity?

  @Query("SELECT * FROM questions WHERE remoteExamId = :remoteExamId ORDER BY id ASC")
  suspend fun getQuestionsByRemoteExamId(remoteExamId: String): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE id IN (:ids) AND isPublished = 1 ORDER BY id ASC")
  suspend fun getQuestionsByIds(ids: List<Long>): List<QuestionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuestion(question: QuestionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllQuestions(questions: List<QuestionEntity>)

  @Update
  suspend fun updateQuestion(question: QuestionEntity)

  @Delete
  suspend fun deleteQuestion(question: QuestionEntity)

  @Query("DELETE FROM questions")
  suspend fun clearAllQuestions()

  @Query("SELECT COUNT(*) FROM questions")
  suspend fun getTotalQuestionsCount(): Int

  @Query("SELECT COUNT(*) FROM questions WHERE courseId = :courseId")
  suspend fun getQuestionsCountByCourse(courseId: Long): Int
}

@Dao
interface ExamDao {
  @Query("SELECT * FROM exams ORDER BY id DESC")
  fun getAllExams(): Flow<List<ExamEntity>>

  @Query("SELECT * FROM exams WHERE isPublished = 1 ORDER BY id DESC")
  fun getPublishedExams(): Flow<List<ExamEntity>>

  @Query("SELECT * FROM exams WHERE (courseId = :courseId OR remoteCourseId = (SELECT remoteId FROM courses WHERE id = :courseId LIMIT 1)) AND isPublished = 1 ORDER BY id DESC")
  fun getExamsForCourse(courseId: Long): Flow<List<ExamEntity>>

  @Query("SELECT * FROM exams WHERE (courseId = :courseId OR remoteCourseId = (SELECT remoteId FROM courses WHERE id = :courseId LIMIT 1)) ORDER BY id ASC")
  suspend fun getExamsForCourseList(courseId: Long): List<ExamEntity>

  @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
  suspend fun getExamById(id: Long): ExamEntity?

  @Query("SELECT * FROM exams WHERE remoteId = :remoteId LIMIT 1")
  suspend fun getExamByRemoteId(remoteId: String): ExamEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExam(exam: ExamEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllExams(exams: List<ExamEntity>)

  @Update
  suspend fun updateExam(exam: ExamEntity)

  @Delete
  suspend fun deleteExam(exam: ExamEntity)

  @Query("SELECT COUNT(*) FROM exams")
  suspend fun getTotalExamsCount(): Int

  @Query("SELECT * FROM exams ORDER BY id DESC")
  suspend fun getAllExamsList(): List<ExamEntity>

  // Exam Attempts
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttempt(attempt: ExamAttemptEntity): Long

  @Query("SELECT * FROM exam_attempts WHERE userId = :userId ORDER BY timestamp DESC")
  fun getAttemptsForUser(userId: Long): Flow<List<ExamAttemptEntity>>

  @Query("SELECT * FROM exam_attempts WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
  suspend fun getRecentAttempts(userId: Long, limit: Int): List<ExamAttemptEntity>

  @Query("SELECT * FROM exam_attempts ORDER BY timestamp DESC")
  fun getAllAttempts(): Flow<List<ExamAttemptEntity>>

  @Query("SELECT * FROM exam_attempts ORDER BY timestamp DESC")
  suspend fun getAllAttemptsList(): List<ExamAttemptEntity>

  @Query("SELECT * FROM exam_attempts WHERE id = :attemptId LIMIT 1")
  suspend fun getAttemptById(attemptId: Long): ExamAttemptEntity?

  @Query("SELECT * FROM exam_attempts WHERE remoteId = :remoteId LIMIT 1")
  suspend fun getAttemptByRemoteId(remoteId: String): ExamAttemptEntity?

  @Query("SELECT COUNT(*) FROM exam_attempts")
  suspend fun getTotalAttemptsCount(): Int

  @Query("SELECT COUNT(*) FROM exam_attempts WHERE userId = :userId")
  suspend fun getUserAttemptsCount(userId: Long): Int
}

@Dao
interface StudyMaterialDao {
  @Query("SELECT * FROM study_materials ORDER BY id DESC")
  fun getAllMaterials(): Flow<List<StudyMaterialEntity>>

  @Query("SELECT * FROM study_materials WHERE (courseId = :courseId OR remoteCourseId = (SELECT remoteId FROM courses WHERE id = :courseId LIMIT 1)) ORDER BY id DESC")
  fun getMaterialsForCourse(courseId: Long): Flow<List<StudyMaterialEntity>>

  @Query("SELECT * FROM study_materials WHERE id = :id LIMIT 1")
  suspend fun getMaterialById(id: Long): StudyMaterialEntity?

  @Query("SELECT * FROM study_materials WHERE remoteId = :remoteId LIMIT 1")
  suspend fun getMaterialByRemoteId(remoteId: String): StudyMaterialEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMaterial(material: StudyMaterialEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllMaterials(materials: List<StudyMaterialEntity>)

  @Update
  suspend fun updateMaterial(material: StudyMaterialEntity)

  @Delete
  suspend fun deleteMaterial(material: StudyMaterialEntity)

  @Query("SELECT COUNT(*) FROM study_materials")
  suspend fun getTotalMaterialsCount(): Int

  @Query("SELECT * FROM study_materials ORDER BY id DESC")
  suspend fun getAllMaterialsList(): List<StudyMaterialEntity>
}

@Dao
interface SubscriptionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubscription(sub: SubscriptionEntity): Long

  @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY createdAt DESC")
  fun getSubscriptionsForUser(userId: Long): Flow<List<SubscriptionEntity>>

  @Query("SELECT * FROM subscriptions WHERE userId = :userId AND subscriptionStatus = 'ACTIVE' AND expiryDate > :currentTime ORDER BY expiryDate DESC LIMIT 1")
  suspend fun getActiveSubscription(userId: Long, currentTime: Long): SubscriptionEntity?

  @Query("SELECT * FROM subscriptions ORDER BY createdAt DESC")
  fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

  @Query("SELECT SUM(amount) FROM subscriptions WHERE paymentStatus = 'COMPLETED'")
  suspend fun getTotalRevenue(): Double?

  @Query("SELECT COUNT(*) FROM subscriptions WHERE subscriptionStatus = 'ACTIVE'")
  suspend fun getActiveSubscriptionsCount(): Int
}

@Dao
interface CourseLibraryDao {
  @Query("SELECT * FROM course_libraries ORDER BY id DESC")
  fun getAllLibraries(): Flow<List<CourseLibraryEntity>>

  @Query("SELECT * FROM course_libraries WHERE (courseId = :courseId OR remoteCourseId = (SELECT remoteId FROM courses WHERE id = :courseId LIMIT 1)) ORDER BY id DESC")
  fun getLibrariesForCourse(courseId: Long): Flow<List<CourseLibraryEntity>>

  @Query("SELECT * FROM course_libraries WHERE (courseId = :courseId OR remoteCourseId = (SELECT remoteId FROM courses WHERE id = :courseId LIMIT 1)) ORDER BY id DESC")
  suspend fun getLibrariesForCourseList(courseId: Long): List<CourseLibraryEntity>

  @Query("SELECT * FROM course_libraries WHERE id = :id LIMIT 1")
  suspend fun getLibraryById(id: Long): CourseLibraryEntity?

  @Query("SELECT * FROM course_libraries WHERE remoteId = :remoteId LIMIT 1")
  suspend fun getLibraryByRemoteId(remoteId: String): CourseLibraryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLibrary(library: CourseLibraryEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllLibraries(libraries: List<CourseLibraryEntity>)

  @Update
  suspend fun updateLibrary(library: CourseLibraryEntity)

  @Delete
  suspend fun deleteLibrary(library: CourseLibraryEntity)

  @Query("SELECT * FROM course_libraries ORDER BY id DESC")
  suspend fun getAllLibrariesList(): List<CourseLibraryEntity>

  @Query("SELECT COUNT(*) FROM course_libraries")
  suspend fun getTotalLibrariesCount(): Int
}

