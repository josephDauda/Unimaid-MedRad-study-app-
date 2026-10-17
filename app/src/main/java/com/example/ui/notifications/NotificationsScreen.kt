package com.example.ui.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.data.repository.NotificationItemUi
import com.example.data.repository.NotificationRepository
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  user: UserEntity,
  notificationRepository: NotificationRepository,
  onNavigateBack: () -> Unit,
  onNavigateToCbt: (() -> Unit)? = null
) {
  BackHandler { onNavigateBack() }

  val scope = rememberCoroutineScope()
  val notifications by notificationRepository.getNotificationsForUserFlow(user).collectAsState(initial = emptyList())
  val unreadCount by notificationRepository.getUnreadCountFlow(user).collectAsState(initial = 0)

  var isRefreshing by remember { mutableStateOf(false) }
  var refreshMessage by remember { mutableStateOf<String?>(null) }
  var selectedFilter by remember { mutableStateOf("All") }
  val filterOptions = listOf("All", "Unread", "Exam Alert", "Lecture Update", "General Announcement", "Urgent Notice")

  var selectedNotificationForDetail by remember { mutableStateOf<NotificationItemUi?>(null) }

  val filteredNotifications = remember(notifications, selectedFilter) {
    when (selectedFilter) {
      "All" -> notifications
      "Unread" -> notifications.filter { !it.isRead }
      else -> notifications.filter { it.category.equals(selectedFilter, ignoreCase = true) || it.priority.equals(selectedFilter, ignoreCase = true) }
    }
  }

  val userIdKey = user.email.ifBlank { user.id.toString() }

  Scaffold(
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("notifications_back_button")
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = MaterialTheme.colorScheme.onBackground
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Column {
                Text(
                  text = "Notifications & Alerts",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                  text = if (unreadCount > 0) "$unreadCount unread notices for ${user.level}" else "All caught up",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (unreadCount > 0) {
                IconButton(
                  onClick = {
                    scope.launch {
                      notificationRepository.markAllAsRead(userIdKey, notifications)
                    }
                  },
                  modifier = Modifier.testTag("notifications_mark_all_read_button")
                ) {
                  Icon(
                    Icons.Default.DoneAll,
                    contentDescription = "Mark All Read",
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }

              IconButton(
                onClick = {
                  scope.launch {
                    isRefreshing = true
                    refreshMessage = "Checking Supabase for latest notifications..."
                    val res = notificationRepository.refreshNotificationsFromCloud(user)
                    isRefreshing = false
                    refreshMessage = if (res.isSuccess) {
                      "Up to date • ${res.getOrDefault(0)} synced from cloud"
                    } else {
                      "Offline mode • Cached notifications displayed"
                    }
                  }
                },
                modifier = Modifier.testTag("notifications_refresh_button")
              ) {
                if (isRefreshing) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                  Icon(
                    Icons.Default.Sync,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }

          // Category / Filter Row
          LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(filterOptions) { opt ->
              val isSelected = selectedFilter == opt
              val count = when (opt) {
                "All" -> notifications.size
                "Unread" -> unreadCount
                else -> notifications.count { it.category.equals(opt, ignoreCase = true) || it.priority.equals(opt, ignoreCase = true) }
              }
              FilterChip(
                selected = isSelected,
                onClick = { selectedFilter = opt },
                label = {
                  Text(
                    text = if (count > 0) "$opt ($count)" else opt,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        scope.launch {
          isRefreshing = true
          notificationRepository.refreshNotificationsFromCloud(user)
          isRefreshing = false
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
      ) {
        refreshMessage?.let { msg ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        if (filteredNotifications.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(72.dp)
                  .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Notifications,
                  contentDescription = null,
                  modifier = Modifier.size(36.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = if (selectedFilter == "Unread") "No Unread Notifications" else "No Notifications Yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = if (selectedFilter == "Unread") {
                  "You have read all announcements targeted to your profile."
                } else {
                  "Departmental broadcasts, mock exam schedules, and lecture notices will appear here as soon as administrators publish them."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
              )
              Spacer(modifier = Modifier.height(18.dp))
              OutlinedButton(
                onClick = {
                  scope.launch {
                    isRefreshing = true
                    notificationRepository.refreshNotificationsFromCloud(user)
                    isRefreshing = false
                  }
                },
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Check for Cloud Updates")
              }
            }
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(filteredNotifications, key = { it.id }) { item ->
              NotificationCard(
                item = item,
                onClick = {
                  selectedNotificationForDetail = item
                  if (!item.isRead) {
                    scope.launch {
                      notificationRepository.markAsRead(item.id, userIdKey)
                    }
                  }
                },
                onDismiss = {
                  scope.launch {
                    notificationRepository.dismissNotification(item.id, userIdKey)
                  }
                }
              )
            }
          }
        }
      }
    }
  }

  // Full Reading Experience Modal Dialog
  selectedNotificationForDetail?.let { detailItem ->
    NotificationDetailDialog(
      item = detailItem,
      onDismissDialog = { selectedNotificationForDetail = null },
      onRemoveFromInbox = {
        scope.launch {
          notificationRepository.dismissNotification(detailItem.id, userIdKey)
          selectedNotificationForDetail = null
        }
      },
      onAction = {
        selectedNotificationForDetail = null
        if (detailItem.category.contains("Exam", ignoreCase = true) || detailItem.priority.contains("Exam", ignoreCase = true)) {
          onNavigateToCbt?.invoke()
        }
      }
    )
  }
}

@Composable
fun NotificationCard(
  item: NotificationItemUi,
  onClick: () -> Unit,
  onDismiss: () -> Unit
) {
  val isExamAlert = item.priority.contains("Exam", ignoreCase = true) || item.category.contains("Exam", ignoreCase = true)
  val isUrgent = item.priority.contains("Urgent", ignoreCase = true)

  val cardBg = if (!item.isRead) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
  } else {
    MaterialTheme.colorScheme.surface
  }

  val borderColor = if (!item.isRead) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("notification_card_${item.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = BorderStroke(if (!item.isRead) 1.5.dp else 1.dp, borderColor)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Priority badge
          val (prioColor, prioBg) = when {
            isExamAlert -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
            isUrgent -> WarningAmber to WarningAmber.copy(alpha = 0.15f)
            else -> MedicalTeal to MedicalTeal.copy(alpha = 0.15f)
          }

          Surface(
            color = prioBg,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = item.priority.uppercase(),
              color = prioColor,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          // Category tag
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = item.category,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          // Target audience
          if (item.target.isNotBlank() && item.target != "All Students") {
            Surface(
              color = ProfessionalBlue.copy(alpha = 0.12f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = item.target,
                color = ProfessionalBlue,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        // Unread dot indicator
        if (!item.isRead) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .background(MaterialTheme.colorScheme.primary, CircleShape)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = item.message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.createdAt))
        Text(
          text = "$dateFormatted • ${item.author}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            Icons.Default.Close,
            contentDescription = "Dismiss from inbox",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
fun NotificationDetailDialog(
  item: NotificationItemUi,
  onDismissDialog: () -> Unit,
  onRemoveFromInbox: () -> Unit,
  onAction: () -> Unit
) {
  val isExamAlert = item.priority.contains("Exam", ignoreCase = true) || item.category.contains("Exam", ignoreCase = true)
  val isUrgent = item.priority.contains("Urgent", ignoreCase = true)

  AlertDialog(
    onDismissRequest = onDismissDialog,
    shape = RoundedCornerShape(18.dp),
    title = {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val (prioColor, prioBg) = when {
              isExamAlert -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
              isUrgent -> WarningAmber to WarningAmber.copy(alpha = 0.15f)
              else -> MedicalTeal to MedicalTeal.copy(alpha = 0.15f)
            }
            Surface(
              color = prioBg,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = item.priority.uppercase(),
                color = prioColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = item.category,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          IconButton(onClick = onDismissDialog, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = item.title,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        val dateFormatted = SimpleDateFormat("EEEE, dd MMMM yyyy • hh:mm a", Locale.getDefault()).format(Date(item.createdAt))
        Text(
          text = dateFormatted,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Audience: ${item.target} • Issued by ${item.author}",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
          color = ProfessionalBlue
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Text(
          text = item.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          lineHeight = 22.sp
        )
      }
    },
    confirmButton = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = onRemoveFromInbox,
          colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
        ) {
          Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Dismiss", style = MaterialTheme.typography.labelMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (isExamAlert) {
            Button(
              onClick = onAction,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open CBT Mock")
            }
          } else {
            Button(
              onClick = onDismissDialog,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Close")
            }
          }
        }
      }
    }
  )
}
