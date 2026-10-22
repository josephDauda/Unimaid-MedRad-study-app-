package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.ExamAttemptEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.MedicalRadiographyEmblem
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.getCardTheme
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  user: UserEntity,
  dashboardViewModel: DashboardViewModel,
  onStartPractice: (courseId: Long) -> Unit,
  onStartExam: (ExamEntity) -> Unit,
  onNavigateToMaterials: () -> Unit,
  onNavigateToSubscription: () -> Unit,
  onNavigateToResults: (attemptId: Long) -> Unit,
  onNavigateToCourses: () -> Unit,
  onNavigateToNotifications: () -> Unit = {},
  onStartPracticeAdvanced: (courseId: Long, questionCount: Int, customTitle: String?, customCode: String?, questionIds: List<Long>) -> Unit = { id, _, _, _, _ -> onStartPractice(id) }
) {
  LaunchedEffect(user.id) {
    dashboardViewModel.loadDashboard(user)
  }

  val dailyAllowance by dashboardViewModel.dailyAllowance.collectAsState()
  val recentAttempts by dashboardViewModel.recentAttempts.collectAsState()
  val activeExams by dashboardViewModel.activeExams.collectAsState()
  val announcements by dashboardViewModel.announcements.collectAsState()
  val isSyncing by dashboardViewModel.isSyncing.collectAsState()
  val dailySprint by dashboardViewModel.dailySprintConfig.collectAsState()
  val practiceCard by dashboardViewModel.practiceCardConfig.collectAsState()
  val mockCard by dashboardViewModel.mockCardConfig.collectAsState()
  val globalCardStyle by dashboardViewModel.globalCardStyle.collectAsState()
  val sprintTheme = getCardTheme(dailySprint.theme.ifBlank { globalCardStyle })

  val greeting = rememberGreeting()
  val firstName = user.fullName.split(" ").firstOrNull() ?: user.fullName

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    PullToRefreshBox(
      isRefreshing = isSyncing,
      onRefresh = { dashboardViewModel.syncWithCloud(user) {} },
      modifier = Modifier.fillMaxSize()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 14.dp)
      ) {
      // 1. Top Bar (Avatar + Hello Greeting + Date Pill + Sync, inspired by reference image images (4).jpeg)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Circular Avatar
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            val initial = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            Text(
              text = initial,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "Hello, $firstName 👋",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.padding(top = 2.dp)
            ) {
              Text(
                text = "Today, ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())} • ${user.level}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        // Live Cloud Sync Action Button
        Surface(
          onClick = { dashboardViewModel.syncWithCloud(user) {} },
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (isSyncing) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProfessionalBlue)
            } else {
              Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 2. Daily Challenge Hero Banner (matching Phone 1 in images (4).jpeg)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onStartPracticeAdvanced(
              dailySprint.courseId,
              dailySprint.questionCount,
              dailySprint.title,
              dailySprint.courseCode,
              dailySprint.questionIds
            )
          },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = sprintTheme.primaryColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, sprintTheme.accentColor.copy(alpha = 0.4f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = sprintTheme.gradientColors
              )
            )
            .padding(20.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = sprintTheme.accentColor.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, sprintTheme.accentColor.copy(alpha = 0.5f))
              ) {
                Text(
                  text = dailySprint.badge,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = sprintTheme.accentColor,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = dailySprint.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.Bold,
                  lineHeight = 28.sp
                ),
                color = Color.White
              )

              Text(
                text = dailySprint.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
              )

              Spacer(modifier = Modifier.height(14.dp))

              // Student Avatar Row + Action Button
              Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                  repeat(3) { i ->
                    Box(
                      modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                          when (i) {
                            0 -> MedicalTeal
                            1 -> WarningAmber
                            else -> ProfessionalBlue
                          }
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = ('A' + i).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 10.sp
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                  text = dailySprint.activeStudentsText,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFC7D2FE)
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Visual 3D Caduceus Accent
            MedicalRadiographyEmblem(
              size = 64.dp,
              showBorder = true,
              borderColor = WarningAmber
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 3. Weekday Calendar / Study Streak Strip (matching reference images (4).jpeg)
      WeekdayCalendarStrip()

      Spacer(modifier = Modifier.height(16.dp))

      // 4. "Your Plan" / Core Academic Modules Section (Asymmetric cards matching reference image)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Your Plan & Courses",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "View All",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clickable { onNavigateToCourses() }
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Taller Left Card: Active Department Practice Course or "Coming Soon..."
        if (practiceCard.isConfigured && practiceCard.courseId > 0L) {
          Surface(
            modifier = Modifier
              .weight(1f)
              .height(190.dp)
              .clickable {
                onStartPracticeAdvanced(
                  practiceCard.courseId,
                  practiceCard.questionCount,
                  practiceCard.courseTitle,
                  practiceCard.courseCode,
                  practiceCard.questionIds
                )
              },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFEF3C7), // Warm soft amber
            border = BorderStroke(1.dp, Color(0xFFFDE68A))
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                ) {
                  Text(
                    text = practiceCard.courseCode.ifBlank { "ANA 201" },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = practiceCard.courseTitle.ifBlank { "Clinical Practice Questions" },
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF78350F),
                  maxLines = 2
                )
                Text(
                  text = "${practiceCard.questionCount} Questions",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF92400E)
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Practice",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFFB45309)
                )
                Surface(
                  shape = CircleShape,
                  color = Color(0xFFB45309),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Default.PlayArrow,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        } else {
          // Professional Coming Soon... state
          Surface(
            modifier = Modifier
              .weight(1f)
              .height(190.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                  Text(
                    text = "PRACTICE CARD",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Coming soon..",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "New clinical questions and case scenarios will appear here once published.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 3
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Pending",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Default.Lock,
                      contentDescription = "Coming Soon",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }
        }

        // Right Stacked Column (Mock Exam + Study Notes)
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Card: Mock Exams (Soft Teal or configured Course or Coming soon)
          if (mockCard.isConfigured && mockCard.courseId > 0L) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clickable {
                  val exam = activeExams.find { it.id == mockCard.examId }
                    ?: activeExams.find { it.courseId == mockCard.courseId }
                    ?: ExamEntity(
                      id = mockCard.examId.takeIf { it > 0 } ?: System.currentTimeMillis(),
                      courseId = mockCard.courseId,
                      title = mockCard.examTitle.ifBlank { "${mockCard.courseCode} Mock CBT" },
                      description = "Official departmental timed CBT examination simulation.",
                      durationMinutes = mockCard.durationMinutes,
                      totalQuestions = mockCard.questionCount,
                      isPublished = true
                    )
                  onStartExam(exam)
                },
              shape = RoundedCornerShape(18.dp),
              color = Color(0xFFCCFBF1), // Soft teal
              border = BorderStroke(1.dp, Color(0xFF99F6E4))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = mockCard.examTitle.ifBlank { "Mock CBT" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F766E),
                    maxLines = 1
                  )
                  Text(
                    text = "${mockCard.courseCode} • ${mockCard.durationMinutes}m Timed",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF115E59)
                  )
                }
                Surface(
                  shape = CircleShape,
                  color = Color(0xFF0F766E),
                  modifier = Modifier.size(28.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                  }
                }
              }
            }
          } else {
            // Professional Coming Soon... state for Mock CBT
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Mock CBT",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Coming soon..",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                  modifier = Modifier.size(28.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Default.Lock,
                      contentDescription = "Coming Soon",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
              }
            }
          }

          // Card: Study Notes (Soft Sky Blue)
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .height(90.dp)
              .clickable(onClick = onNavigateToMaterials),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFE0E7FF), // Soft lavender
            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Lecture Notes",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF3730A3)
                )
                Text(
                  text = "Curated Packs",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF4338CA)
                )
              }
              Surface(
                shape = CircleShape,
                color = Color(0xFF3730A3),
                modifier = Modifier.size(28.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 5. Daily Allowance Counter
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Daily Practice Allowance",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (user.isPremium || user.role == "ADMIN") "Unlimited questions active (Premium Account)" else "$dailyAllowance / 15 questions remaining today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Icon(
              imageVector = if (user.isPremium) Icons.Default.Stars else Icons.Default.Timer,
              contentDescription = null,
              tint = if (user.isPremium) WarningAmber else ProfessionalBlue
            )
          }

          if (!user.isPremium && user.role != "ADMIN") {
            Spacer(modifier = Modifier.height(10.dp))
            val progress = (dailyAllowance.toFloat() / 15f).coerceIn(0f, 1f)
            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = if (dailyAllowance > 4) ProfessionalBlue else WarningAmber,
              trackColor = Color(0xFFE2E8F0)
            )
          }
        }
      }

      // 6. Live Announcements (if any)
      if (announcements.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        val latestAnn = announcements.first()
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToNotifications() },
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = latestAnn.title,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                  color = WarningAmber.copy(alpha = 0.2f),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = latestAnn.priority,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = WarningAmber,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = latestAnn.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // 7. Recent Result Review
      if (recentAttempts.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Latest Assessment",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Full Review",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
              val first = recentAttempts.firstOrNull()
              if (first != null) onNavigateToResults(first.id)
            }
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        val firstAttempt = recentAttempts.first()
        val percent = if (firstAttempt.totalQuestions > 0) (firstAttempt.score * 100) / firstAttempt.totalQuestions else 0
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToResults(firstAttempt.id) },
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = firstAttempt.examTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = firstAttempt.courseTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (percent >= 50) SuccessGreen.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
            ) {
              Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (percent >= 50) SuccessGreen else Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
}

/**
 * Modern Weekday Calendar Strip inspired by reference image images (4).jpeg
 */
@Composable
fun WeekdayCalendarStrip() {
  val calendar = Calendar.getInstance()
  val todayDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
  val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...

  // Days: Mon to Sun
  val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      dayNames.forEachIndexed { index, name ->
        // Assuming Wednesday or today is active
        val dayNumber = (todayDayOfMonth - (todayDayOfWeek - 2) + index).coerceAtLeast(1)
        val isToday = (index == (todayDayOfWeek - 2 + 7) % 7)

        if (isToday) {
          // Active pill
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(42.dp)
          ) {
            Column(
              modifier = Modifier.padding(vertical = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
        } else {
          Column(
            modifier = Modifier
              .width(38.dp)
              .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = name,
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted,
              fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "$dayNumber",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}

private fun rememberGreeting(): String {
  val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  return when {
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    else -> "Good evening"
  }
}
