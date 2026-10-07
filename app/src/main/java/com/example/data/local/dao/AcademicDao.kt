package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UniversityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicDao {
  // Universities
  @Query("SELECT * FROM universities ORDER BY name ASC")
  fun getAllUniversities(): Flow<List<UniversityEntity>>

  @Query("SELECT * FROM universities ORDER BY name ASC")
  suspend fun getUniversitiesList(): List<UniversityEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUniversity(uni: UniversityEntity): Long

  @Update
  suspend fun updateUniversity(uni: UniversityEntity)

  @Delete
  suspend fun deleteUniversity(uni: UniversityEntity)

  // Faculties
  @Query("SELECT * FROM faculties WHERE universityId = :universityId ORDER BY name ASC")
  fun getFacultiesByUniversity(universityId: Long): Flow<List<FacultyEntity>>

  @Query("SELECT * FROM faculties ORDER BY name ASC")
  fun getAllFaculties(): Flow<List<FacultyEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFaculty(faculty: FacultyEntity): Long

  @Update
  suspend fun updateFaculty(faculty: FacultyEntity)

  @Delete
  suspend fun deleteFaculty(faculty: FacultyEntity)

  // Departments
  @Query("SELECT * FROM departments WHERE facultyId = :facultyId ORDER BY name ASC")
  fun getDepartmentsByFaculty(facultyId: Long): Flow<List<DepartmentEntity>>

  @Query("SELECT * FROM departments ORDER BY name ASC")
  fun getAllDepartments(): Flow<List<DepartmentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDepartment(dept: DepartmentEntity): Long

  @Update
  suspend fun updateDepartment(dept: DepartmentEntity)

  @Delete
  suspend fun deleteDepartment(dept: DepartmentEntity)

  // Courses
  @Query("SELECT * FROM courses ORDER BY code ASC")
  fun getAllCourses(): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses ORDER BY code ASC")
  suspend fun getCoursesList(): List<CourseEntity>

  @Query("SELECT * FROM courses WHERE departmentId = :deptId ORDER BY code ASC")
  fun getCoursesByDepartment(deptId: Long): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
  suspend fun getCourseById(courseId: Long): CourseEntity?

  @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
  fun observeCourseById(courseId: Long): Flow<CourseEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCourse(course: CourseEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllCourses(courses: List<CourseEntity>)

  @Update
  suspend fun updateCourse(course: CourseEntity)

  @Delete
  suspend fun deleteCourse(course: CourseEntity)

  // Topics
  @Query("SELECT * FROM topics WHERE courseId = :courseId ORDER BY orderIndex ASC, name ASC")
  fun getTopicsByCourse(courseId: Long): Flow<List<TopicEntity>>

  @Query("SELECT * FROM topics ORDER BY name ASC")
  fun getAllTopics(): Flow<List<TopicEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTopic(topic: TopicEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllTopics(topics: List<TopicEntity>)

  @Update
  suspend fun updateTopic(topic: TopicEntity)

  @Delete
  suspend fun deleteTopic(topic: TopicEntity)
}
