package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "daily_usage",
  indices = [Index(value = ["userId", "dateString"], unique = true)]
)
data class DailyUsageEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val userId: Long,
  val dateString: String, // e.g. "2026-09-26"
  val questionsAnsweredToday: Int = 0
)
