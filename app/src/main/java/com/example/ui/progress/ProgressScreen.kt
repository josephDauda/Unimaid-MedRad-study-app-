package com.example.ui.progress

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import com.example.data.local.entity.ExamAttemptEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.CbtRepository
import com.example.data.repository.UserProfilePreferences
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalThemeController
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
  user: UserEntity,
  cbtRepository: CbtRepository,
  authViewModel: AuthViewModel,
  userProfilePreferences: UserProfilePreferences,
  cloudSyncManager: com.example.data.sync.CloudSyncManager? = null,
  onNavigateBack: () -> Unit,
  onPracticeCourse: (courseId: Long) -> Unit,
  onNavigateToResults: (attemptId: Long) -> Unit,
  onNavigateToAbout: () -> Unit = {}
) {
  val coroutineScope = rememberCoroutineScope()
  val isSyncing by (cloudSyncManager?.isSyncing ?: remember { MutableStateFlow(false) }).collectAsState()
  val attempts by cbtRepository.getUserAttempts(user.id).collectAsState(initial = emptyList())
  val profilePhotoUri by userProfilePreferences.profilePhotoUri.collectAsState()
  val notificationsEnabled by userProfilePreferences.notificationsEnabled.collectAsState()
  val quizRemindersEnabled by userProfilePreferences.quizRemindersEnabled.collectAsState()
  val soundEnabled by userProfilePreferences.soundEnabled.collectAsState()

  val themeController = LocalThemeController.current
  val currentThemeMode by themeController.themeMode.collectAsState()

  val totalExamsCompleted = attempts.size
  val totalQuestionsAttempted = attempts.sumOf { it.totalQuestions }
  val totalCorrect = attempts.sumOf { it.correctAnswers }
  val accuracy = if (totalQuestionsAttempted > 0) (totalCorrect * 100) / totalQuestionsAttempted else 0
  val averageScore = if (attempts.isNotEmpty()) attempts.sumOf { it.score } / attempts.size else 0

  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showChangePasswordDialog by remember { mutableStateOf(false) }
  var showPhotoOptionsDialog by remember { mutableStateOf(false) }

  BackHandler {
    if (showPhotoOptionsDialog) {
      showPhotoOptionsDialog = false
    } else if (showEditProfileDialog) {
      showEditProfileDialog = false
    } else if (showChangePasswordDialog) {
      showChangePasswordDialog = false
    } else {
      onNavigateBack()
    }
  }

  val context = LocalContext.current

  // Photo Picker launcher with internal caching for persistent access
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (_: Exception) {}

      val persistentUri = try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          val avatarFile = File(context.filesDir, "student_avatar_${user.id}.jpg")
          avatarFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
          }
          Uri.fromFile(avatarFile).toString()
        } ?: uri.toString()
      } catch (_: Exception) {
        uri.toString()
      }

      userProfilePreferences.setProfilePhotoUri(persistentUri)
    }
  }

  val isDarkTheme = when (currentThemeMode) {
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0.dp),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Profile & Settings",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          IconButton(onClick = onNavigateToAbout) {
            Icon(
              Icons.Outlined.Info,
              contentDescription = "About App",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isSyncing,
      onRefresh = {
        coroutineScope.launch {
          authViewModel.refreshUser()
          cloudSyncManager?.pullLatestFromCloud()
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Profile Header with Editable Photo & Monogram Fallback
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
          ) {
            // Main Circular Avatar
            Surface(
              modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .clickable { showPhotoOptionsDialog = true },
              shape = CircleShape,
              color = if (isDarkTheme) Color(0xFF1E2D44) else MaterialTheme.colorScheme.primary,
              border = BorderStroke(2.dp, WarningAmber)
            ) {
              if (profilePhotoUri != null) {
                AsyncImage(
                  model = profilePhotoUri,
                  contentDescription = "Profile Photo",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                val initials = user.fullName
                  .split(" ")
                  .filter { it.isNotBlank() }
                  .take(2)
                  .map { it.first().uppercaseChar() }
                  .joinToString("")

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                  Text(
                    text = if (initials.isNotBlank()) initials else "MR",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                  )
                }
              }
            }

            // Camera / Edit Overlay Badge
            Surface(
              onClick = { showPhotoOptionsDialog = true },
              shape = CircleShape,
              color = WarningAmber,
              border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(30.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.CameraAlt,
                  contentDescription = "Change Photo",
                  tint = Color.Black,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = user.fullName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "${user.department} • ${user.university}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = ProfessionalBlue.copy(alpha = 0.15f)
            ) {
              Text(
                text = "${user.level} • ${user.semester}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) Color(0xFF93C5FD) else ProfessionalBlue,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (user.isPremium || user.role == "ADMIN") SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)
            ) {
              Text(
                text = if (user.role == "ADMIN") "ADMINISTRATOR" else if (user.isPremium) "PREMIUM SCHOLAR" else "STUDENT TIER",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (user.isPremium || user.role == "ADMIN") (if (isDarkTheme) Color(0xFF34D399) else SuccessGreen) else WarningAmber,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Edit Profile Action Button
          OutlinedButton(
            onClick = { showEditProfileDialog = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("edit_profile_button")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit Profile Details", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
          }
        }
      }

      // 2. Three Metric Highlight Boxes Row (High contrast for light & dark)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Tests Taken
          Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFDCFCE7),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF065F46) else Color(0xFFBBF7D0))
          ) {
            Column(
              modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "$totalExamsCompleted",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF166534)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Tests Taken",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF15803D)
              )
            }
          }

          // Avg Grade
          Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = if (isDarkTheme) Color(0xFF0C4A6E).copy(alpha = 0.45f) else Color(0xFFE0F2FE),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF075985) else Color(0xFFBAE6FD))
          ) {
            Column(
              modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "$averageScore%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0369A1)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Avg Grade",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDarkTheme) Color(0xFF7DD3FC) else Color(0xFF0284C7)
              )
            }
          }

          // Accuracy
          Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = if (isDarkTheme) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF92400E) else Color(0xFFFDE68A))
          ) {
            Column(
              modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "$accuracy%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Accuracy",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFFD97706)
              )
            }
          }
        }
      }

      // 3. User Preferences Card: Theme / Appearance
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Appearance & Theme",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ThemePillOption(
                label = "Light",
                icon = Icons.Default.LightMode,
                isSelected = currentThemeMode == AppThemeMode.LIGHT,
                onClick = { themeController.setThemeMode(AppThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
              )
              ThemePillOption(
                label = "Dark",
                icon = Icons.Default.DarkMode,
                isSelected = currentThemeMode == AppThemeMode.DARK,
                onClick = { themeController.setThemeMode(AppThemeMode.DARK) },
                modifier = Modifier.weight(1f)
              )
              ThemePillOption(
                label = "System",
                icon = Icons.Default.SettingsBrightness,
                isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                onClick = { themeController.setThemeMode(AppThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      // 4. Notification & Sound Preferences
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Notifications & Audio",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            PreferenceToggleRow(
              title = "Academic Push Alerts",
              subtitle = "Receive real-time notices published by department Admin",
              checked = notificationsEnabled,
              onCheckedChange = { userProfilePreferences.setNotificationsEnabled(it) }
            )

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 10.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            PreferenceToggleRow(
              title = "Daily CBT Reminders",
              subtitle = "Get notified about unattempted daily practice drill",
              checked = quizRemindersEnabled,
              onCheckedChange = { userProfilePreferences.setQuizRemindersEnabled(it) }
            )

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 10.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            PreferenceToggleRow(
              title = "Subtle Notification Chime",
              subtitle = "Play professional alert sound for urgent departmental broadcasts",
              checked = soundEnabled,
              onCheckedChange = { userProfilePreferences.setSoundEnabled(it) }
            )
          }
        }
      }

      // 5. Security & Password Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Security & Access",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showChangePasswordDialog = true }
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Change Account Password",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Update your login credentials securely",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // 6. Topic Mastery Breakdown Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Radiographic Mastery Breakdown",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "RAD 101",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TopicMasteryBar(
              topicName = "Discovery of X-rays (Röntgen 1895)",
              percentage = 85,
              status = "Strong",
              color = SuccessGreen
            )

            Spacer(modifier = Modifier.height(10.dp))

            TopicMasteryBar(
              topicName = "Radiation Protection (ALARA Principles)",
              percentage = 78,
              status = "Good",
              color = ProfessionalBlue
            )

            Spacer(modifier = Modifier.height(10.dp))

            TopicMasteryBar(
              topicName = "X-ray Tube Construction & Heat Dissipation",
              percentage = 45,
              status = "Review",
              color = ErrorRed
            )

            Spacer(modifier = Modifier.height(10.dp))

            TopicMasteryBar(
              topicName = "Radiographic Positioning Landmarks",
              percentage = 70,
              status = "Moderate",
              color = WarningAmber
            )
          }
        }
      }

      // 7. Recent Examination Attempts Header & List
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Recent Exam Attempts",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${attempts.size} recorded",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (attempts.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "No test attempts yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Take a daily practice drill or mock exam to view your score analytics here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        items(attempts) { attempt ->
          AttemptHistoryCard(
            attempt = attempt,
            onReviewClick = { onNavigateToResults(attempt.id) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
    }
  }

  // DIALOG 1: Edit Profile (Allowed fields only: Full Name, Phone Number)
  if (showEditProfileDialog) {
    EditProfileModal(
      user = user,
      onDismiss = { showEditProfileDialog = false },
      onSave = { newName, newPhone ->
        authViewModel.updateProfile(user.id, newName, newPhone) { success, _ ->
          if (success) {
            userProfilePreferences.setCustomDisplayName(newName)
            authViewModel.refreshUser()
          }
        }
        showEditProfileDialog = false
      }
    )
  }

  // DIALOG 2: Change Password Modal
  if (showChangePasswordDialog) {
    ChangePasswordModal(
      onDismiss = { showChangePasswordDialog = false },
      onSubmit = { currentPass, newPass, onResult ->
        authViewModel.changePassword(user.id, currentPass, newPass) { success, errorMsg ->
          onResult(success, errorMsg)
          if (success) {
            showChangePasswordDialog = false
          }
        }
      }
    )
  }

  // DIALOG 3: Profile Photo Options Modal
  if (showPhotoOptionsDialog) {
    AlertDialog(
      onDismissRequest = { showPhotoOptionsDialog = false },
      title = {
        Text("Profile Photo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Select an action for your profile picture:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(8.dp))

          Surface(
            onClick = {
              showPhotoOptionsDialog = false
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(12.dp))
              Text("Choose Photo from Gallery", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
          }

          if (profilePhotoUri != null) {
            Surface(
              onClick = {
                try {
                  File(context.filesDir, "student_avatar_${user.id}.jpg").delete()
                } catch (_: Exception) {}
                userProfilePreferences.setProfilePhotoUri(null)
                showPhotoOptionsDialog = false
              },
              shape = RoundedCornerShape(12.dp),
              color = ErrorRed.copy(alpha = 0.1f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Remove Custom Photo", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = ErrorRed)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showPhotoOptionsDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun ThemePillOption(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    modifier = modifier.height(44.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun PreferenceToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = MaterialTheme.colorScheme.primary
      )
    )
  }
}

@Composable
fun EditProfileModal(
  user: UserEntity,
  onDismiss: () -> Unit,
  onSave: (newName: String, newPhone: String) -> Unit
) {
  var name by remember { mutableStateOf(user.fullName) }
  var phone by remember { mutableStateOf(user.phoneNumber) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Edit Profile Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "Update your personal student details below. Official institution records remain verified.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Full Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        // Read-only Academic Records (Protected per User Request)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Official Academic Records (Protected)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = WarningAmber
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "${user.department} • ${user.level}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              user.university,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (error != null) {
          Text(error!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.trim().length >= 3) {
            onSave(name.trim(), phone.trim())
          } else {
            error = "Please enter a valid full name (minimum 3 characters)"
          }
        },
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun ChangePasswordModal(
  onDismiss: () -> Unit,
  onSubmit: (currentPass: String, newPass: String, onResult: (Boolean, String?) -> Unit) -> Unit
) {
  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }

  var currentVisible by remember { mutableStateOf(false) }
  var newVisible by remember { mutableStateOf(false) }

  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isLoading by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Change Account Password", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "Enter your current password followed by your new password (min 6 characters).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = currentPassword,
          onValueChange = { currentPassword = it },
          label = { Text("Current Password") },
          singleLine = true,
          visualTransformation = if (currentVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { currentVisible = !currentVisible }) {
              Icon(if (currentVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = null)
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = newPassword,
          onValueChange = { newPassword = it },
          label = { Text("New Password") },
          singleLine = true,
          visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { newVisible = !newVisible }) {
              Icon(if (newVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = null)
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it },
          label = { Text("Confirm New Password") },
          singleLine = true,
          visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        if (errorMessage != null) {
          Text(errorMessage!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            currentPassword.isEmpty() -> errorMessage = "Current password is required"
            newPassword.length < 6 -> errorMessage = "New password must be at least 6 characters"
            newPassword != confirmPassword -> errorMessage = "New passwords do not match"
            else -> {
              isLoading = true
              errorMessage = null
              onSubmit(currentPassword, newPassword) { success, err ->
                isLoading = false
                if (!success) {
                  errorMessage = err ?: "Failed to update password"
                }
              }
            }
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(10.dp)
      ) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
          Text("Update Password")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun AttemptHistoryCard(
  attempt: ExamAttemptEntity,
  onReviewClick: () -> Unit
) {
  val percent = if (attempt.totalQuestions > 0) (attempt.score * 100) / attempt.totalQuestions else 0
  val isPassed = percent >= 50

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onReviewClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = attempt.examTitle,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = attempt.courseTitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(attempt.timestamp)),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isPassed) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
        ) {
          Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isPassed) SuccessGreen else ErrorRed,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${attempt.score}/${attempt.totalQuestions} Marks",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun TopicMasteryBar(
  topicName: String,
  percentage: Int,
  status: String,
  color: Color
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = topicName,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = "$percentage% ($status)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { percentage / 100f },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp)),
      color = color,
      trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
  }
}
