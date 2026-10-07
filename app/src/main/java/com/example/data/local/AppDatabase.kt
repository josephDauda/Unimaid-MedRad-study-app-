package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AcademicDao
import com.example.data.local.dao.CourseLibraryDao
import com.example.data.local.dao.DailyUsageDao
import com.example.data.local.dao.ExamDao
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
    CourseLibraryEntity::class
  ],
  version = 2,
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

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "medrad_studysquad_database"
        )
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
