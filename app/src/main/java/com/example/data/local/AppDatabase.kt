package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AcademicDao
import com.example.data.local.dao.CourseLibraryDao
import com.example.data.local.dao.DailyUsageDao
import com.example.data.local.dao.ExamDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.QuestionDao
import com.example.data.local.dao.StudyMaterialDao
import com.example.data.local.dao.SubscriptionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.DailyUsageEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.ExamAttemptEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.NotificationReadStatusEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.StudyMaterialEntity
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UniversityEntity
import com.example.data.local.entity.UserEntity

@Database(
  entities = [
    UserEntity::class,
    DailyUsageEntity::class,
    UniversityEntity::class,
    FacultyEntity::class,
    DepartmentEntity::class,
    CourseEntity::class,
    TopicEntity::class,
    QuestionEntity::class,
    ExamEntity::class,
    ExamAttemptEntity::class,
    StudyMaterialEntity::class,
    SubscriptionEntity::class,
    CourseLibraryEntity::class,
    NotificationEntity::class,
    NotificationReadStatusEntity::class
  ],
  version = 4,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun dailyUsageDao(): DailyUsageDao
  abstract fun academicDao(): AcademicDao
  abstract fun questionDao(): QuestionDao
  abstract fun examDao(): ExamDao
  abstract fun studyMaterialDao(): StudyMaterialDao
  abstract fun subscriptionDao(): SubscriptionDao
  abstract fun courseLibraryDao(): CourseLibraryDao
  abstract fun notificationDao(): NotificationDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
      override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        try {
          db.execSQL("ALTER TABLE courses ADD COLUMN remoteId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE exams ADD COLUMN remoteId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE exams ADD COLUMN remoteCourseId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE questions ADD COLUMN remoteId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE questions ADD COLUMN remoteExamId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE questions ADD COLUMN remoteCourseId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE study_materials ADD COLUMN remoteId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE study_materials ADD COLUMN remoteCourseId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE course_libraries ADD COLUMN remoteId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE course_libraries ADD COLUMN remoteCourseId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE exam_attempts ADD COLUMN remoteId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE exam_attempts ADD COLUMN remoteExamId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE exam_attempts ADD COLUMN remoteCourseId TEXT DEFAULT NULL")
        } catch (_: Exception) {}
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "medrad_studysquad_database"
        )
          .addMigrations(MIGRATION_3_4)
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
