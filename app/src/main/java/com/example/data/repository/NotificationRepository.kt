package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.NotificationReadStatusEntity
import com.example.data.local.entity.UserEntity
import com.example.data.supabase.SupabaseAnnouncement
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseNotificationItem
import com.example.data.supabase.SupabaseNotificationRead
import com.example.data.supabase.SupabaseRealtimeManager
import com.example.util.SystemNotificationHelper
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NotificationItemUi(
  val id: String,
  val title: String,
  val message: String,
  val category: String,
  val priority: String,
  val target: String,
  val author: String,
  val createdAt: Long,
  val isRead: Boolean,
  val isDismissed: Boolean,
  val isSyncedWithCloud: Boolean
)

class NotificationRepository(
  private val context: Context,
  private val database: AppDatabase,
  private val supabaseClient: SupabaseClient,
  private var realtimeManager: SupabaseRealtimeManager? = null,
  private val userProfilePreferences: UserProfilePreferences
) {
  private val tag = "NotificationRepo"
  private val dao = database.notificationDao()

  fun setRealtimeManager(manager: SupabaseRealtimeManager?) {
    this.realtimeManager = manager
  }

  /**
   * Flow of filtered notifications intended for the specific student or admin,
   * merged with this student's individual read/dismissal statuses.
   */
  fun getNotificationsForUserFlow(user: UserEntity): Flow<List<NotificationItemUi>> {
    val userIdKey = user.email.ifBlank { user.id.toString() }
    val allFlow = dao.getAllNotificationsFlow()
    val readsFlow = dao.getReadStatusesForUserFlow(userIdKey)

    return allFlow.combine(readsFlow) { notifications, readStatuses ->
      val statusMap = readStatuses.associateBy { it.notificationId }

      notifications
        .filter { entity ->
          isAuthorizedForUser(entity, user)
        }
        .mapNotNull { entity ->
          val status = statusMap[entity.id]
          if (status?.isDismissed == true) {
            null // Omit dismissed items from student's personal inbox
          } else {
            NotificationItemUi(
              id = entity.id,
              title = entity.title,
              message = entity.message,
              category = entity.category,
              priority = entity.priority,
              target = entity.target,
              author = entity.author,
              createdAt = entity.createdAt,
              isRead = status?.isRead ?: false,
              isDismissed = false,
              isSyncedWithCloud = entity.isSyncedWithCloud
            )
          }
        }
    }
  }

  /**
   * Unread notification count specifically for this user.
   */
  fun getUnreadCountFlow(user: UserEntity): Flow<Int> {
    return getNotificationsForUserFlow(user).map { list ->
      list.count { !it.isRead }
    }
  }

  /**
   * Admin-facing flow showing all created notifications across all audiences.
   */
  fun getAllAdminNotificationsFlow(): Flow<List<NotificationEntity>> {
    return dao.getAllNotificationsFlow()
  }

  /**
   * Checks whether a notification's audience target matches the student's profile.
   */
  private fun isAuthorizedForUser(entity: NotificationEntity, user: UserEntity): Boolean {
    if (user.role == "ADMIN") return true // Administrators can view all communications

    // Direct targeted notification
    if (!entity.recipientId.isNullOrBlank()) {
      val userUuid = supabaseClient.getUserId() ?: ""
      return entity.recipientId == user.id.toString() ||
          entity.recipientId.equals(user.email, ignoreCase = true) ||
          (userUuid.isNotBlank() && entity.recipientId.equals(userUuid, ignoreCase = true))
    }

    val target = entity.target.trim()
    if (target.equals("All Students", ignoreCase = true) || target.isBlank()) {
      return true
    }

    // Level-based matching: "100 Level", "200 Level", "300 Level", etc.
    if (target.contains("Level", ignoreCase = true)) {
      return user.level.contains(target, ignoreCase = true) || target.contains(user.level, ignoreCase = true)
    }

    // Course-code matching: "RAD 101", "RAD 102", "RAD 201", etc.
    if (target.startsWith("RAD", ignoreCase = true)) {
      if (user.assignedCourses.contains(target, ignoreCase = true)) return true
      // Match by level: RAD 1xx -> 100 Level, RAD 2xx -> 200 Level, etc.
      val levelPrefix = target.filter { it.isDigit() }.take(1)
      if (levelPrefix.isNotBlank() && user.level.startsWith(levelPrefix)) {
        return true
      }
    }

    return false
  }

  /**
   * Pulls notifications and announcements from Supabase and caches them in Room.
   */
  suspend fun refreshNotificationsFromCloud(user: UserEntity): Result<Int> = withContext(Dispatchers.IO) {
    try {
      var pulledCount = 0
      val userIdKey = user.email.ifBlank { user.id.toString() }

      // 1. Fetch from announcements table
      val annRes = supabaseClient.restGet("announcements?select=*&order=created_at.desc&limit=50")
      annRes.onSuccess { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseAnnouncement::class.java)
          val adapter = supabaseClient.moshi.adapter<List<SupabaseAnnouncement>>(type)
          val list = adapter.fromJson(json) ?: emptyList()

          val entities = list.map { ann ->
            val createdTs = parseTimestamp(ann.createdAt)
            val updatedTs = parseTimestamp(ann.updatedAt)
            val annTitle = ann.title ?: "Notice"
            val annMsg = ann.message ?: ""
            NotificationEntity(
              id = ann.id,
              title = annTitle,
              message = annMsg,
              category = ann.category ?: extractCategoryFromTitleOrPriority(annTitle, ann.priority),
              priority = ann.priority,
              target = ann.target ?: extractTargetFromTitleOrMessage(annTitle, annMsg),
              author = ann.author ?: "Department of Medical Radiography",
              senderId = ann.senderId,
              recipientId = null,
              isPublished = ann.isPublished,
              createdAt = createdTs,
              updatedAt = updatedTs,
              isSyncedWithCloud = true
            )
          }

          if (entities.isNotEmpty()) {
            dao.insertNotifications(entities)
            pulledCount += entities.size
          }
        } catch (e: Exception) {
          Log.w(tag, "Failed parsing announcements JSON: ${e.message}")
        }
      }

      // 2. Fetch from notifications table (for direct or newer notification records)
      val notifRes = supabaseClient.restGet("notifications?select=*&order=created_at.desc&limit=50")
      notifRes.onSuccess { json ->
        try {
          val type = Types.newParameterizedType(List::class.java, SupabaseNotificationItem::class.java)
          val adapter = supabaseClient.moshi.adapter<List<SupabaseNotificationItem>>(type)
          val list = adapter.fromJson(json) ?: emptyList()

          val entities = list.map { item ->
            val createdTs = parseTimestamp(item.createdAt)
            val updatedTs = parseTimestamp(item.updatedAt)
            val itemTitle = item.title ?: "Notification"
            val itemMsg = item.message ?: ""
            NotificationEntity(
              id = item.id,
              title = itemTitle,
              message = itemMsg,
              category = item.category ?: "General Announcement",
              priority = item.priority,
              target = item.target ?: "All Students",
              author = "Department of Medical Radiography",
              senderId = item.senderId,
              recipientId = item.recipientId,
              isPublished = item.isPublished,
              createdAt = createdTs,
              updatedAt = updatedTs,
              isSyncedWithCloud = true
            )
          }

          if (entities.isNotEmpty()) {
            dao.insertNotifications(entities)
            pulledCount += entities.size
          }
        } catch (e: Exception) {
          Log.w(tag, "Failed parsing notifications JSON: ${e.message}")
        }
      }

      // 3. Fetch user's persistent read status from Supabase notification_reads if table exists
      try {
        val readsRes = supabaseClient.restGet("notification_reads?user_id=eq.$userIdKey&select=*")
        readsRes.onSuccess { json ->
          val type = Types.newParameterizedType(List::class.java, SupabaseNotificationRead::class.java)
          val adapter = supabaseClient.moshi.adapter<List<SupabaseNotificationRead>>(type)
          val reads = adapter.fromJson(json) ?: emptyList()
          reads.forEach { r ->
            dao.insertOrUpdateReadStatus(
              NotificationReadStatusEntity(
                notificationId = r.notificationId,
                userId = userIdKey,
                isRead = r.isRead,
                isDismissed = r.isDismissed,
                readAt = parseTimestamp(r.readAt)
              )
            )
          }
        }
      } catch (e: Exception) {
        Log.w(tag, "Remote notification_reads sync notice: ${e.message}")
      }

      Result.success(pulledCount)
    } catch (e: Exception) {
      Log.e(tag, "Failed refreshing notifications from cloud: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Marks a notification as read for a specific student and syncs to Supabase.
   */
  suspend fun markAsRead(notificationId: String, userId: String) = withContext(Dispatchers.IO) {
    try {
      val now = System.currentTimeMillis()
      dao.insertOrUpdateReadStatus(
        NotificationReadStatusEntity(
          notificationId = notificationId,
          userId = userId,
          isRead = true,
          isDismissed = false,
          readAt = now
        )
      )

      // Sync read record to Supabase notification_reads
      val payload = JSONObject().apply {
        put("notification_id", notificationId)
        put("user_id", userId)
        put("is_read", true)
        put("is_dismissed", false)
        put("read_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(now)))
      }
      supabaseClient.restPost("notification_reads", payload.toString(), upsert = true)
    } catch (e: Exception) {
      Log.w(tag, "Error syncing markAsRead: ${e.message}")
    }
  }

  /**
   * Marks all authorized notifications as read for this student.
   */
  suspend fun markAllAsRead(userId: String, items: List<NotificationItemUi>) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    items.forEach { item ->
      dao.insertOrUpdateReadStatus(
        NotificationReadStatusEntity(
          notificationId = item.id,
          userId = userId,
          isRead = true,
          isDismissed = false,
          readAt = now
        )
      )
    }
    // Attempt batch upsert to Supabase
    try {
      val array = JSONArray()
      val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(now))
      items.forEach { item ->
        array.put(JSONObject().apply {
          put("notification_id", item.id)
          put("user_id", userId)
          put("is_read", true)
          put("is_dismissed", false)
          put("read_at", dateStr)
        })
      }
      supabaseClient.restPost("notification_reads", array.toString(), upsert = true)
    } catch (e: Exception) {
      Log.w(tag, "Batch markAllAsRead remote notice: ${e.message}")
    }
  }

  /**
   * Dismisses a notification from this student's personal view without deleting it for other students.
   */
  suspend fun dismissNotification(notificationId: String, userId: String) = withContext(Dispatchers.IO) {
    try {
      dao.dismissNotification(notificationId, userId)
      val payload = JSONObject().apply {
        put("notification_id", notificationId)
        put("user_id", userId)
        put("is_read", true)
        put("is_dismissed", true)
      }
      supabaseClient.restPost("notification_reads", payload.toString(), upsert = true)
    } catch (e: Exception) {
      Log.w(tag, "Error syncing dismissNotification: ${e.message}")
    }
  }

  /**
   * Administrator: Broadcasts a new notification.
   * Persists to local Room DB and attempts to write to Supabase cloud.
   */
  suspend fun publishNotification(
    title: String,
    message: String,
    category: String,
    priority: String,
    target: String,
    author: String = "Department of Medical Radiography",
    senderId: String? = null
  ): Result<NotificationEntity> = withContext(Dispatchers.IO) {
    try {
      val trimmedTitle = title.trim()
      val trimmedMessage = message.trim()
      if (trimmedTitle.length < 3) {
        return@withContext Result.failure(IllegalArgumentException("Title must be at least 3 characters long."))
      }
      if (trimmedMessage.length < 5) {
        return@withContext Result.failure(IllegalArgumentException("Message must be at least 5 characters long."))
      }

      val uuid = UUID.randomUUID().toString()
      val now = System.currentTimeMillis()

      val localEntity = NotificationEntity(
        id = uuid,
        title = trimmedTitle,
        message = trimmedMessage,
        category = category,
        priority = priority,
        target = target,
        author = author,
        senderId = senderId,
        recipientId = null,
        isPublished = true,
        createdAt = now,
        updatedAt = now,
        isSyncedWithCloud = false
      )

      // 1. Save to Room immediately so no content is ever lost
      dao.insertNotification(localEntity)

      // 2. Push to Supabase announcements table using verified schema columns
      // Note: PostgreSQL constraint announcements_priority_check requires lowercase: ('low', 'normal', 'high', 'urgent')
      val supabasePriority = when (priority.trim().lowercase()) {
        "urgent" -> "urgent"
        "exam alert", "exam", "high", "critical" -> "high"
        "low" -> "low"
        else -> "normal"
      }

      val annPayload = JSONObject().apply {
        put("id", uuid)
        put("title", trimmedTitle)
        put("message", trimmedMessage)
        put("priority", supabasePriority)
        put("author", author)
        put("target", target)
        put("is_published", true)
        if (!senderId.isNullOrBlank() && senderId.length == 36 && senderId.count { it == '-' } == 4) {
          put("created_by", senderId)
        }
      }

      var remoteSuccess = false
      var failureReason: String? = null

      val primaryRes = supabaseClient.restPost("announcements", annPayload.toString(), upsert = true)
      if (primaryRes.isSuccess) {
        remoteSuccess = true
      } else {
        val err = primaryRes.exceptionOrNull()?.message ?: ""
        failureReason = err
        // Fallback with minimal columns if constraint or column mismatch occurred
        val minimalPayload = JSONObject().apply {
          put("id", uuid)
          put("title", trimmedTitle)
          put("message", trimmedMessage)
          put("priority", supabasePriority)
          put("is_published", true)
        }
        val fallbackRes = supabaseClient.restPost("announcements", minimalPayload.toString(), upsert = true)
        if (fallbackRes.isSuccess) {
          remoteSuccess = true
        } else {
          failureReason = fallbackRes.exceptionOrNull()?.message ?: err
        }
      }

      // Also attempt write to notifications table
      try {
        val notifPayload = JSONObject().apply {
          put("id", uuid)
          put("title", trimmedTitle)
          put("message", trimmedMessage)
          put("is_read", false)
        }
        supabaseClient.restPost("notifications", notifPayload.toString(), upsert = true)
      } catch (_: Exception) {}

      val updatedEntity = if (remoteSuccess) {
        val synced = localEntity.copy(isSyncedWithCloud = true)
        dao.updateNotification(synced)

        // Realtime broadcast to connected clients
        realtimeManager?.dispatchRealtimeEvent("announcements", "INSERT", annPayload)

        // Trigger local notification sound/banner if notifications enabled
        if (userProfilePreferences.notificationsEnabled.value) {
          SystemNotificationHelper.showNotification(context, trimmedTitle, trimmedMessage, priority)
        }
        synced
      } else {
        localEntity
      }

      if (remoteSuccess) {
        Result.success(updatedEntity)
      } else {
        val isRls = failureReason?.contains("42501") == true || failureReason?.contains("row-level security") == true
        val msg = if (isRls) {
          "Cloud write blocked by Supabase Row-Level Security (RLS). Notification is saved safely in Room database. Run the SQL migration script in your Supabase SQL Editor to enable public cloud publication."
        } else {
          "Notification saved locally in Room, but cloud upload failed: ${failureReason ?: "Unknown error"}."
        }
        Result.failure(Exception(msg))
      }
    } catch (e: Exception) {
      Log.e(tag, "Exception during publishNotification: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Administrator: Edits an existing notification.
   */
  suspend fun editNotification(
    id: String,
    title: String,
    message: String,
    category: String,
    priority: String,
    target: String
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val existing = dao.getNotificationById(id)
        ?: return@withContext Result.failure(Exception("Notification not found in database."))

      val updated = existing.copy(
        title = title.trim(),
        message = message.trim(),
        category = category,
        priority = priority,
        target = target,
        updatedAt = System.currentTimeMillis()
      )
      dao.updateNotification(updated)

      val supabasePriority = when (priority.trim().lowercase()) {
        "urgent" -> "urgent"
        "exam alert", "exam", "high", "critical" -> "high"
        "low" -> "low"
        else -> "normal"
      }

      val patchPayload = JSONObject().apply {
        put("title", updated.title)
        put("message", updated.message)
        put("priority", supabasePriority)
      }
      val res = supabaseClient.restPatch("announcements?id=eq.$id", patchPayload.toString())
      realtimeManager?.dispatchRealtimeEvent("announcements", "UPDATE", patchPayload.put("id", id))

      Result.success(res.isSuccess)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Administrator: Deletes a notification from both Supabase cloud and Room.
   */
  suspend fun deleteNotification(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      dao.deleteNotificationById(id)

      // Delete from cloud
      val resAnn = supabaseClient.restDelete("announcements?id=eq.$id")
      supabaseClient.restDelete("notifications?id=eq.$id")
      supabaseClient.restDelete("notification_reads?notification_id=eq.$id")

      // Realtime event to remove from student devices
      realtimeManager?.dispatchRealtimeEvent("announcements", "DELETE", JSONObject().put("id", id))

      Result.success(true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Handles incoming Realtime event from WebSocket for announcements/notifications.
   */
  suspend fun handleRealtimeNotificationEvent(table: String, eventType: String, record: JSONObject) = withContext(Dispatchers.IO) {
    try {
      val id = record.optString("id", "")
      if (id.isBlank()) return@withContext

      when (eventType.uppercase()) {
        "DELETE" -> {
          dao.deleteNotificationById(id)
        }
        "INSERT", "UPDATE" -> {
          val title = record.optString("title")
          val message = record.optString("message")
          if (title.isNotBlank()) {
            val priority = record.optString("priority", "Normal")
            val target = record.optString("target", "All Students")
            val category = record.optString("category", extractCategoryFromTitleOrPriority(title, priority))
            val author = record.optString("author", "Department of Medical Radiography")
            val now = System.currentTimeMillis()

            val entity = NotificationEntity(
              id = id,
              title = title,
              message = message,
              category = category,
              priority = priority,
              target = target,
              author = author,
              isPublished = true,
              createdAt = now,
              updatedAt = now,
              isSyncedWithCloud = true
            )
            dao.insertNotification(entity)

            // Trigger system notification if enabled
            if (eventType.uppercase() == "INSERT" && userProfilePreferences.notificationsEnabled.value) {
              SystemNotificationHelper.showNotification(context, title, message, priority)
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(tag, "Error handling realtime notification event: ${e.message}")
    }
  }

  private fun parseTimestamp(ts: String?): Long {
    if (ts.isNullOrBlank()) return System.currentTimeMillis()
    val formats = listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd HH:mm:ss"
    )
    for (fmt in formats) {
      try {
        val sdf = SimpleDateFormat(fmt, Locale.US)
        val date = sdf.parse(ts)
        if (date != null) return date.time
      } catch (_: Exception) {}
    }
    return System.currentTimeMillis()
  }

  private fun extractCategoryFromTitleOrPriority(title: String, priority: String): String {
    val lower = title.lowercase()
    return when {
      lower.contains("exam") || lower.contains("test") || lower.contains("cbt") || priority.contains("Exam", ignoreCase = true) -> "Exam Alert"
      lower.contains("lecture") || lower.contains("class") || lower.contains("timetable") -> "Lecture Update"
      lower.contains("practical") || lower.contains("clinical") || lower.contains("lab") -> "Assignment / Practical"
      priority.contains("Urgent", ignoreCase = true) || lower.contains("urgent") -> "Urgent Notice"
      else -> "General Announcement"
    }
  }

  private fun extractTargetFromTitleOrMessage(title: String, message: String): String {
    val text = "$title $message"
    val levels = listOf("100 Level", "200 Level", "300 Level", "400 Level", "500 Level")
    for (lvl in levels) {
      if (text.contains(lvl, ignoreCase = true)) return lvl
    }
    val courseCodes = listOf("ANA 201", "RAD 101", "RAD 102", "RAD 201", "RAD 202", "RAD 301", "RAD 401", "RAD 501")
    for (code in courseCodes) {
      if (text.contains(code, ignoreCase = true)) return code
    }
    return "All Students"
  }
}
