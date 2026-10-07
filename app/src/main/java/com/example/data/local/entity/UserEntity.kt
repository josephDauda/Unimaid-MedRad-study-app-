package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "users",
  indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val fullName: String,
  val email: String,
  val phoneNumber: String,
  val passwordHash: String,
  val role: String, // "STUDENT" or "ADMIN"
  val adminRole: String = "PRIMARY", // "PRIMARY" or "SECONDARY"
  val permissions: String = "ALL", // Comma-separated: "materials,questions,announcements,libraries,exams" or "ALL"
  val isActive: Boolean = true,
  val assignedCourses: String = "", // e.g. "RAD 101" or empty for unrestricted
  val university: String,
  val faculty: String,
  val department: String,
  val level: String,
  val semester: String,
  val isPremium: Boolean = false,
  val premiumPlan: String? = null,
  val premiumExpiry: Long? = null,
  val createdAt: Long = System.currentTimeMillis()
)
