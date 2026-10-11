package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object SystemNotificationHelper {
  private const val CHANNEL_ID = "medrad_broadcast_alerts"
  private const val CHANNEL_NAME = "MedRad Departmental Alerts"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Instant notifications and announcements from the Department of Medical Radiography"
        enableLights(true)
        enableVibration(true)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun showNotification(
    context: Context,
    title: String,
    message: String,
    priority: String = "Normal"
  ) {
    createNotificationChannel(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("navigate_to", "dashboard")
      putExtra("alert_title", title)
      putExtra("alert_message", message)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      System.currentTimeMillis().toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val notificationPriority = when (priority.uppercase()) {
      "URGENT", "EXAM ALERT" -> NotificationCompat.PRIORITY_MAX
      else -> NotificationCompat.PRIORITY_HIGH
    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(notificationPriority)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setSound(soundUri)
      .setDefaults(NotificationCompat.DEFAULT_ALL)

    try {
      val notificationManager = NotificationManagerCompat.from(context)
      notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    } catch (e: SecurityException) {
      // Permission not granted on Android 13+ runtime
    } catch (e: Exception) {
      // Graceful fallback
    }
  }
}
