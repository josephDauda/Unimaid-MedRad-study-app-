package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.NotificationReadStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
  @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
  fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

  @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
  suspend fun getAllNotifications(): List<NotificationEntity>

  @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
  suspend fun getNotificationById(id: String): NotificationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(notification: NotificationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotifications(notifications: List<NotificationEntity>)

  @Update
  suspend fun updateNotification(notification: NotificationEntity)

  @Query("DELETE FROM notifications WHERE id = :id")
  suspend fun deleteNotificationById(id: String)

  @Query("DELETE FROM notifications")
  suspend fun clearAllNotifications()

  // Read and dismissal status operations
  @Query("SELECT * FROM notification_read_statuses WHERE userId = :userId")
  fun getReadStatusesForUserFlow(userId: String): Flow<List<NotificationReadStatusEntity>>

  @Query("SELECT * FROM notification_read_statuses WHERE userId = :userId")
  suspend fun getReadStatusesForUser(userId: String): List<NotificationReadStatusEntity>

  @Query("SELECT * FROM notification_read_statuses WHERE notificationId = :notificationId AND userId = :userId LIMIT 1")
  suspend fun getReadStatus(notificationId: String, userId: String): NotificationReadStatusEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateReadStatus(status: NotificationReadStatusEntity)

  @Query("UPDATE notification_read_statuses SET isRead = 1, readAt = :readAt WHERE notificationId = :notificationId AND userId = :userId")
  suspend fun markAsRead(notificationId: String, userId: String, readAt: Long = System.currentTimeMillis())

  @Query("UPDATE notification_read_statuses SET isDismissed = 1 WHERE notificationId = :notificationId AND userId = :userId")
  suspend fun dismissNotification(notificationId: String, userId: String)

  @Query("DELETE FROM notification_read_statuses WHERE userId = :userId")
  suspend fun clearReadStatusesForUser(userId: String)
}
