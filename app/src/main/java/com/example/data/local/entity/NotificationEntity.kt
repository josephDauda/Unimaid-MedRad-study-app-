package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "notifications",
  indices = [
    Index(value = ["target"]),
    Index(value = ["createdAt"])
  ]
)
data class NotificationEntity(
  @PrimaryKey
  val id: String, // UUID string
  val title: String,
  val message: String,
  val category: String = "General", // "General Announcement", "Exam Alert", "Lecture Update", "Assignment / Practical", "Urgent Notice"
  val priority: String = "Normal", // "Normal", "Urgent", "Exam Alert"
  val target: String = "All Students", // "All Students", "100 Level", "200 Level", "300 Level", "400 Level", "500 Level", "RAD 101", etc.
  val author: String = "Department of Medical Radiography",
  val senderId: String? = null,
  val recipientId: String? = null, // null for broadcast, student UUID for direct targeted notification
  val isPublished: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val isSyncedWithCloud: Boolean = false
)

@Entity(
  tableName = "notification_read_statuses",
  primaryKeys = ["notificationId", "userId"],
  indices = [Index(value = ["userId"])]
)
data class NotificationReadStatusEntity(
  val notificationId: String,
  val userId: String, // Student user email or ID string
  val isRead: Boolean = true,
  val isDismissed: Boolean = false,
  val readAt: Long = System.currentTimeMillis()
)
