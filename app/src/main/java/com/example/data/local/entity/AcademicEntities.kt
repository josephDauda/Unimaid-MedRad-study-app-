package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "universities")
data class UniversityEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val shortName: String,
  val status: String = "Active"
)

@Entity(tableName = "faculties")
data class FacultyEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val universityId: Long,
  val name: String
)

@Entity(tableName = "departments")
data class DepartmentEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val facultyId: Long,
  val name: String
)

@Entity(tableName = "courses")
data class CourseEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val departmentId: Long,
  val code: String, // e.g. "RAD 101"
  val title: String, // e.g. "Introduction to Radiography"
  val level: String, // e.g. "100 Level"
  val semester: String, // e.g. "First Semester"
  val description: String = ""
)

@Entity(tableName = "topics")
data class TopicEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val courseId: Long,
  val name: String,
  val orderIndex: Int = 0
)
