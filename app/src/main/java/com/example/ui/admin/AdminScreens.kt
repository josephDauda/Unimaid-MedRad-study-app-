package com.example.ui.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.StudyMaterialEntity
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.sync.DailyCbtSprintConfig
import com.example.data.sync.MockCardConfig
import com.example.data.sync.PracticeCardConfig
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CARD_THEMES
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AdminViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  user: UserEntity,
  adminViewModel: AdminViewModel,
  onNavigateBack: () -> Unit
) {
  // Security verification: Role-based authorization
  if (user.role != "ADMIN") {
    Scaffold { padding ->
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("Access Denied. Only university administrators can access this portal.")
      }
    }
    return
  }

  LaunchedEffect(Unit) {
    adminViewModel.loadStats()
  }

  val stats by adminViewModel.stats.collectAsState()
  val questions by adminViewModel.allQuestions.collectAsState()
  val exams by adminViewModel.allExams.collectAsState()
  val materials by adminViewModel.allMaterials.collectAsState()
  val students by adminViewModel.allStudents.collectAsState()
  val subscriptions by adminViewModel.allSubscriptions.collectAsState()
  val courses by adminViewModel.allCourses.collectAsState()
  val libraries by adminViewModel.allLibraries.collectAsState()
  val admins by adminViewModel.allAdmins.collectAsState()
  val isSyncing by adminViewModel.isSyncing.collectAsState()

  var selectedSection by remember { mutableStateOf("Overview") }
  val sections = listOf(
    "Overview",
    "Libraries & Drive",
    "Questions",
    "Study Materials",
    "Exam Builder",
    "Push Notifications",
    "Live Cloud Sync",
    "Admin Roles",
    "Academic Setup",
    "Students",
    "Score Export",
    "Global Card Theme",
    "About App Mgmt",
    "Feedback Inbox",
    "Subscriptions"
  )

  var showAddQuestionDialog by remember { mutableStateOf(false) }
  var showAddExamDialog by remember { mutableStateOf(false) }
  var showAddMaterialDialog by remember { mutableStateOf(false) }
  var showAddAcademicDialog by remember { mutableStateOf(false) }
  var showAddLibraryDialog by remember { mutableStateOf(false) }
  var editingLibrary by remember { mutableStateOf<CourseLibraryEntity?>(null) }
  var showAddAdminDialog by remember { mutableStateOf(false) }

  val hasOpenDialog = showAddQuestionDialog || showAddExamDialog || showAddMaterialDialog ||
      showAddAcademicDialog || showAddLibraryDialog || (editingLibrary != null) || showAddAdminDialog

  BackHandler {
    if (showAddQuestionDialog) showAddQuestionDialog = false
    else if (showAddExamDialog) showAddExamDialog = false
    else if (showAddMaterialDialog) showAddMaterialDialog = false
    else if (showAddAcademicDialog) showAddAcademicDialog = false
    else if (showAddLibraryDialog) showAddLibraryDialog = false
    else if (editingLibrary != null) editingLibrary = null
    else if (showAddAdminDialog) showAddAdminDialog = false
    else if (selectedSection != "Overview") selectedSection = "Overview"
    else onNavigateBack()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Administrator Management Console",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "MEDRAD Platform Governance",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = {
            if (hasOpenDialog) {
              showAddQuestionDialog = false
              showAddExamDialog = false
              showAddMaterialDialog = false
              showAddAcademicDialog = false
              showAddLibraryDialog = false
              editingLibrary = null
              showAddAdminDialog = false
            } else if (selectedSection != "Overview") {
              selectedSection = "Overview"
            } else {
              onNavigateBack()
            }
          }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
          navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Horizontal Tab Navigation
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(sections) { sec ->
          FilterChip(
            selected = selectedSection == sec,
            onClick = { selectedSection = sec },
            label = { Text(sec) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { adminViewModel.pullLatestFromCloud {} },
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
      ) {
        Box(
          modifier = Modifier.fillMaxSize()
        ) {
        when (selectedSection) {
          "Overview" -> AdminOverviewView(
            stats = stats,
            onQuickAddQuestion = { showAddQuestionDialog = true },
            onQuickAddExam = { showAddExamDialog = true },
            onQuickAddMaterial = { showAddMaterialDialog = true },
            onQuickAddAcademic = { showAddAcademicDialog = true }
          )
          "Libraries & Drive" -> AdminLibrariesView(
            libraries = libraries,
            courses = courses,
            onAddLibrary = { showAddLibraryDialog = true },
            onEditLibrary = { editingLibrary = it },
            onDeleteLibrary = { adminViewModel.deleteLibrary(it) }
          )
          "Live Cloud Sync" -> AdminLiveCloudSyncView(adminViewModel = adminViewModel)
          "Questions" -> AdminQuestionsView(
            questions = questions,
            courses = courses,
            onAddQuestion = { showAddQuestionDialog = true },
            onDeleteQuestion = { adminViewModel.deleteQuestion(it) },
            onClearAllQuestions = { adminViewModel.clearAllQuestions() }
          )
          "Exam Builder" -> AdminExamsView(
            exams = exams,
            onAddExam = { showAddExamDialog = true },
            onDeleteExam = { adminViewModel.deleteExam(it) }
          )
          "Study Materials" -> AdminMaterialsView(
            materials = materials,
            courses = courses,
            onAddMaterial = { showAddMaterialDialog = true },
            onDeleteMaterial = { adminViewModel.deleteMaterial(it) }
          )
          "Push Notifications" -> AdminPushNotificationsView(adminViewModel = adminViewModel)
          "Admin Roles" -> AdminRolesView(
            admins = admins,
            currentUser = user,
            onAddSecondaryAdmin = { showAddAdminDialog = true },
            onToggleActive = { admin, active ->
              adminViewModel.updateAdminPermissions(user, admin.id, admin.permissions, admin.assignedCourses, active) { _, _ -> }
            },
            onRemoveAdmin = { admin ->
              adminViewModel.removeAdmin(user, admin.id) { _, _ -> }
            }
          )
          "Academic Setup" -> AdminAcademicSetupView(
            courses = courses,
            onAddCourse = { showAddAcademicDialog = true },
            onDeleteCourse = { adminViewModel.deleteCourse(it) }
          )
          "Students" -> AdminStudentsView(
            students = students,
            onRefresh = { adminViewModel.syncProfilesFromCloud() }
          )
          "Score Export" -> AdminScoreExportView(adminViewModel = adminViewModel)
          "Global Card Theme" -> AdminGlobalCardStyleView(adminViewModel = adminViewModel)
          "About App Mgmt" -> AdminAboutAppView(adminViewModel = adminViewModel)
          "Feedback Inbox" -> AdminFeedbackInboxView(adminViewModel = adminViewModel)
          "Subscriptions" -> AdminSubscriptionsView(subscriptions = subscriptions)
        }
      }
      }
    }
  }

  // Modals for creating items
  if (showAddQuestionDialog) {
    AddQuestionDialog(
      courses = courses,
      onDismiss = { showAddQuestionDialog = false },
      onSave = { courseId, topicId, text, a, b, c, d, correct, explanation, diff, isPrem, qType, aTitle, marks, timeSec, target ->
        adminViewModel.createQuestion(
          courseId = courseId,
          topicId = topicId,
          text = text,
          optA = a,
          optB = b,
          optC = c,
          optD = d,
          correct = correct,
          explanation = explanation,
          difficulty = diff,
          isPremium = isPrem,
          questionType = qType,
          assessmentTitle = aTitle,
          marks = marks,
          timeLimitSeconds = timeSec,
          publishTarget = target
        ) {
          showAddQuestionDialog = false
        }
      }
    )
  }

  if (showAddExamDialog) {
    AddExamDialog(
      courses = courses,
      onDismiss = { showAddExamDialog = false },
      onSave = { courseId, title, desc, duration, count, isPrem ->
        adminViewModel.createExam(courseId, title, desc, duration, count, isPrem) {
          showAddExamDialog = false
        }
      }
    )
  }

  if (showAddMaterialDialog) {
    AddMaterialDialog(
      courses = courses,
      onDismiss = { showAddMaterialDialog = false },
      onSave = { courseId, topicId, title, desc, type, body, isPrem, extUrl ->
        adminViewModel.createMaterial(
          courseId = courseId,
          topicId = topicId,
          title = title,
          desc = desc,
          type = type,
          body = body,
          isPremium = isPrem,
          externalUrl = extUrl
        ) {
          showAddMaterialDialog = false
        }
      }
    )
  }

  if (showAddAcademicDialog) {
    AddAcademicHierarchyDialog(
      onDismiss = { showAddAcademicDialog = false },
      onSaveCourse = { code, title, dept, level, sem, desc ->
        adminViewModel.createCourse(1L, code, title, level, sem, desc) {
          showAddAcademicDialog = false
        }
      }
    )
  }

  if (showAddLibraryDialog) {
    AddLibraryDialog(
      courses = courses,
      onDismiss = { showAddLibraryDialog = false },
      onSave = { courseId, courseCode, name, desc, url ->
        adminViewModel.createLibrary(courseId, courseCode, name, desc, url) {
          showAddLibraryDialog = false
        }
      }
    )
  }

  if (editingLibrary != null) {
    EditLibraryDialog(
      library = editingLibrary!!,
      courses = courses,
      onDismiss = { editingLibrary = null },
      onSave = { updated ->
        adminViewModel.updateLibrary(updated) {
          editingLibrary = null
        }
      }
    )
  }

  if (showAddAdminDialog) {
    AddSecondaryAdminDialog(
      courses = courses,
      onDismiss = { showAddAdminDialog = false },
      onSave = { fullName, email, phone, pass, permissions, assignedCourses, onComplete ->
        adminViewModel.addSecondaryAdmin(
          creator = user,
          fullName = fullName,
          email = email,
          phone = phone,
          pass = pass,
          permissions = permissions,
          assignedCourses = assignedCourses
        ) { success, error ->
          onComplete(success, error)
          if (success) {
            showAddAdminDialog = false
          }
        }
      }
    )
  }
}

@Composable
fun AdminOverviewView(
  stats: com.example.data.repository.AdminDashboardStats,
  onQuickAddQuestion: () -> Unit,
  onQuickAddExam: () -> Unit,
  onQuickAddMaterial: () -> Unit,
  onQuickAddAcademic: () -> Unit
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "NG"))
  currencyFormat.maximumFractionDigits = 0

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Platform Statistics & Key Metrics",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    // 2x2 Stats Grid
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AdminStatCard(
          modifier = Modifier.weight(1f),
          title = "Total Students",
          value = "${stats.totalStudents}",
          subtitle = "${stats.activeStudents} active learners",
          icon = Icons.Default.People,
          color = MaterialTheme.colorScheme.primary
        )
        AdminStatCard(
          modifier = Modifier.weight(1f),
          title = "Premium Subs",
          value = "${stats.premiumSubscribers}",
          subtitle = "${stats.activeSubscriptions} active passes",
          icon = Icons.Default.WorkspacePremium,
          color = WarningAmber
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AdminStatCard(
          modifier = Modifier.weight(1f),
          title = "Exams Completed",
          value = "${stats.examsCompleted}",
          subtitle = "${stats.questionsAttempted} items answered",
          icon = Icons.Default.Assignment,
          color = ProfessionalBlue
        )
        AdminStatCard(
          modifier = Modifier.weight(1f),
          title = "Est. Revenue",
          value = "₦${stats.totalRevenueNaira.toInt()}",
          subtitle = "Paystack/Verified",
          icon = Icons.Default.AttachMoney,
          color = SuccessGreen
        )
      }
    }

    // Quick Admin Actions
    item {
      Text(
        text = "Quick Creation Tools",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onQuickAddQuestion,
          modifier = Modifier.weight(1f).height(46.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Question", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
        }

        Button(
          onClick = onQuickAddExam,
          modifier = Modifier.weight(1f).height(46.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ProfessionalBlue),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Build Exam", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onQuickAddMaterial,
          modifier = Modifier.weight(1f).height(46.dp),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Upload Note", style = MaterialTheme.typography.bodySmall)
        }

        OutlinedButton(
          onClick = onQuickAddAcademic,
          modifier = Modifier.weight(1f).height(46.dp),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Course", style = MaterialTheme.typography.bodySmall)
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun AdminStatCard(
  modifier: Modifier = Modifier,
  title: String,
  value: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = color)
      Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun AdminQuestionsView(
  questions: List<QuestionEntity>,
  courses: List<CourseEntity>,
  onAddQuestion: () -> Unit,
  onDeleteQuestion: (QuestionEntity) -> Unit,
  onClearAllQuestions: () -> Unit = {}
) {
  var showClearConfirm by remember { mutableStateOf(false) }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear All Questions?", fontWeight = FontWeight.Bold) },
      text = { Text("This will remove all questions from the question bank. You can add new verified questions anytime.") },
      confirmButton = {
        Button(
          onClick = {
            showClearConfirm = false
            onClearAllQuestions()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Question Bank (${questions.size} items)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (questions.isNotEmpty()) {
          OutlinedButton(
            onClick = { showClearConfirm = true },
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear All", color = ErrorRed)
          }
        }
        Button(
          onClick = onAddQuestion,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Create Question")
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(questions) { q ->
        val courseCode = courses.find { it.id == q.courseId }?.code ?: "Course #${q.courseId}"
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text(
                    text = courseCode,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = q.questionType,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                  text = "Ans: ${q.correctAnswer}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = SuccessGreen
                )
              }

              IconButton(onClick = { onDeleteQuestion(q) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(q.questionText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "A) ${q.optionA} • B) ${q.optionB} • C) ${q.optionC} • D) ${q.optionD}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (q.explanation.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Rationale: ${q.explanation}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = ProfessionalBlue,
                maxLines = 2
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun AdminExamsView(
  exams: List<ExamEntity>,
  onAddExam: () -> Unit,
  onDeleteExam: (ExamEntity) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Mock Examination Builder",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Button(
        onClick = onAddExam,
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Build Exam")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(exams) { exam ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(exam.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
              Text("${exam.totalQuestions} Questions • ${exam.durationMinutes} Minutes Duration", style = MaterialTheme.typography.bodySmall, color = ProfessionalBlue)
              Text(exam.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }

            IconButton(onClick = { onDeleteExam(exam) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun AdminMaterialsView(
  materials: List<StudyMaterialEntity>,
  courses: List<CourseEntity>,
  onAddMaterial: () -> Unit,
  onDeleteMaterial: (StudyMaterialEntity) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Study Materials & Notes",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Button(
        onClick = onAddMaterial,
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Upload Note")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(materials) { mat ->
        val courseCode = courses.find { it.id == mat.courseId }?.code ?: "Course #${mat.courseId}"
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text(
                    text = courseCode,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("${mat.contentType} • ${if (mat.isPremium) "Premium" else "Free"}", style = MaterialTheme.typography.bodySmall, color = ProfessionalBlue)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(mat.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
              Text(mat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
              if (!mat.externalUrl.isNullOrBlank()) {
                Text("Attached: ${mat.externalUrl}", style = MaterialTheme.typography.labelSmall, color = ProfessionalBlue, maxLines = 1)
              }
            }

            IconButton(onClick = { onDeleteMaterial(mat) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun AdminLibrariesView(
  libraries: List<CourseLibraryEntity>,
  courses: List<CourseEntity>,
  onAddLibrary: () -> Unit,
  onEditLibrary: (CourseLibraryEntity) -> Unit,
  onDeleteLibrary: (CourseLibraryEntity) -> Unit
) {
  val context = LocalContext.current
  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Course Libraries & Drive Resources",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${libraries.size} external Google Drive links & archives",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Button(
        onClick = onAddLibrary,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Library")
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(libraries) { lib ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Text(
                  text = lib.courseCode.ifBlank { "GENERAL" },
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onEditLibrary(lib) }, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ProfessionalBlue, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onDeleteLibrary(lib) }, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = lib.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
              text = lib.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = lib.externalUrl,
                style = MaterialTheme.typography.labelSmall,
                color = ProfessionalBlue,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
              )

              OutlinedButton(
                onClick = {
                  try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lib.externalUrl))
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                  }
                },
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open Link", fontSize = 11.sp)
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun AdminRolesView(
  admins: List<UserEntity>,
  currentUser: UserEntity,
  onAddSecondaryAdmin: () -> Unit,
  onToggleActive: (UserEntity, Boolean) -> Unit,
  onRemoveAdmin: (UserEntity) -> Unit
) {
  val isPrimary = currentUser.adminRole == "PRIMARY"

  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Administrative Governance",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Primary & Secondary Departmental Administrators",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (isPrimary) {
        Button(
          onClick = onAddSecondaryAdmin,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Admin")
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(admins) { admin ->
        val isTargetPrimary = admin.adminRole == "PRIMARY"
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (isTargetPrimary) WarningAmber.copy(alpha = 0.2f) else ProfessionalBlue.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = if (isTargetPrimary) "PRIMARY ADMIN" else "SECONDARY ADMIN",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 9.sp
                    ),
                    color = if (isTargetPrimary) Color(0xFFB45309) else ProfessionalBlue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (admin.isActive) SuccessGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = if (admin.isActive) "ACTIVE" else "DISABLED",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (admin.isActive) SuccessGreen else Color.Gray,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              if (isPrimary && !isTargetPrimary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Switch(
                    checked = admin.isActive,
                    onCheckedChange = { onToggleActive(admin, it) }
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  IconButton(onClick = { onRemoveAdmin(admin) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Admin", tint = ErrorRed, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = admin.fullName,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Text(
              text = "${admin.email} • ${admin.phoneNumber}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "Assigned Courses: ${admin.assignedCourses.ifBlank { "All Departmental Courses" }}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = ProfessionalBlue
            )

            Text(
              text = "Permissions: ${admin.permissions}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun AdminAcademicSetupView(
  courses: List<CourseEntity>,
  onAddCourse: () -> Unit,
  onDeleteCourse: (CourseEntity) -> Unit = {}
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Academic Hierarchy Configuration",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Button(
        onClick = onAddCourse,
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Course")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "University → Faculty → Department → Level → Semester → Course → Topic",
      style = MaterialTheme.typography.bodySmall,
      color = ProfessionalBlue
    )

    Spacer(modifier = Modifier.height(16.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Active Universities", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(6.dp))
        Text("1. University of Maiduguri (UNIMAID) - Active", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Active Faculties & Departments", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(6.dp))
        Text("• College of Medical Sciences -> Medical Radiography", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Configured Courses (${courses.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        courses.forEach { c ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("• ${c.code}: ${c.title}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
              Text("${c.level} • ${c.semester}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
              onClick = { onDeleteCourse(c) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete course", tint = ErrorRed, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

@Composable
fun AdminStudentsView(
  students: List<UserEntity>,
  onRefresh: (() -> Unit)? = null
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Enrolled Students (${students.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Real-time records synced from Supabase cloud",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      if (onRefresh != null) {
        IconButton(onClick = onRefresh) {
          Icon(Icons.Default.Sync, contentDescription = "Sync Profiles", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }
    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(students) { s ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(s.fullName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
              Text(s.email, style = MaterialTheme.typography.bodySmall, color = ProfessionalBlue)
              Text("${s.university} • ${s.department} (${s.level})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (s.isPremium) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = if (s.isPremium) "PREMIUM" else "FREE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (s.isPremium) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun AdminSubscriptionsView(subscriptions: List<SubscriptionEntity>) {
  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = "Active Subscriptions & Orders (${subscriptions.size})",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(subscriptions) { sub ->
        val dateFormatted = remember(sub.createdAt) {
          SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(sub.createdAt))
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(sub.planName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
              Text("₦${sub.amount.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = SuccessGreen)
            }

            Text("Ref: ${sub.paymentReference}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Date: $dateFormatted • Status: ${sub.subscriptionStatus}", style = MaterialTheme.typography.bodySmall, color = ProfessionalBlue)
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

// Dialogs for Admin Add actions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionDialog(
  courses: List<CourseEntity>,
  onDismiss: () -> Unit,
  onSave: (Long, Long, String, String, String, String, String, String, String, String, Boolean, String, String, Int, Int, String) -> Unit
) {
  var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: 1L) }
  var courseDropdownExpanded by remember { mutableStateOf(false) }

  var publishTarget by remember { mutableStateOf("COURSE_BANK") }
  var targetDropdownExpanded by remember { mutableStateOf(false) }

  var assessmentTitle by remember { mutableStateOf("Continuous Assessment 1") }
  var questionType by remember { mutableStateOf("MULTIPLE_CHOICE") }
  val questionTypes = listOf("MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_IN_BLANK", "IMAGE_DIAGRAM", "DRAWING_LABEL")
  var typeDropdownExpanded by remember { mutableStateOf(false) }

  var questionText by remember { mutableStateOf("") }
  var optionA by remember { mutableStateOf("") }
  var optionB by remember { mutableStateOf("") }
  var optionC by remember { mutableStateOf("") }
  var optionD by remember { mutableStateOf("") }
  var correctAnswer by remember { mutableStateOf("A") }
  var explanation by remember { mutableStateOf("") }
  var difficulty by remember { mutableStateOf("Medium") }
  var marks by remember { mutableStateOf("1") }
  var timeSeconds by remember { mutableStateOf("60") }
  var isPremium by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Course Question", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Course Selector
        Text("Target Course:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = courseDropdownExpanded,
          onExpandedChange = { courseDropdownExpanded = it }
        ) {
          val selectedCourse = courses.find { it.id == selectedCourseId }
          OutlinedTextField(
            value = selectedCourse?.let { "${it.code} — ${it.title}" } ?: "Select Course",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = courseDropdownExpanded,
            onDismissRequest = { courseDropdownExpanded = false }
          ) {
            courses.forEach { course ->
              DropdownMenuItem(
                text = { Text("${course.code} — ${course.title}") },
                onClick = {
                  selectedCourseId = course.id
                  courseDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Publish Target Section Selector
        Text("Publish Destination / Target Section:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = targetDropdownExpanded,
          onExpandedChange = { targetDropdownExpanded = it }
        ) {
          OutlinedTextField(
            value = when (publishTarget) {
              "DAILY_SPRINT" -> "A) Daily CBT Sprint Page (Hero Card)"
              "PRACTICE_CARD" -> "B) Middle Dashboard Practice Card Page"
              "MOCK_EXAM" -> "C) Mock Page (Mock Exam & Quizzes)"
              "ALL_SECTIONS" -> "Publish to All Sections (Bank, Sprint & Practice)"
              else -> "Course Question Bank (Standard CBT Bank)"
            },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(10.dp)
          )
          ExposedDropdownMenu(
            expanded = targetDropdownExpanded,
            onDismissRequest = { targetDropdownExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("Course Question Bank (Standard CBT Bank)") },
              onClick = {
                publishTarget = "COURSE_BANK"
                targetDropdownExpanded = false
              }
            )
            DropdownMenuItem(
              text = { Text("A) Daily CBT Sprint Page (Dashboard Hero Card)") },
              onClick = {
                publishTarget = "DAILY_SPRINT"
                targetDropdownExpanded = false
              }
            )
            DropdownMenuItem(
              text = { Text("B) Middle Dashboard Practice Card Page") },
              onClick = {
                publishTarget = "PRACTICE_CARD"
                targetDropdownExpanded = false
              }
            )
            DropdownMenuItem(
              text = { Text("C) Mock Page (Dashboard Mock Card & Quizzes)") },
              onClick = {
                publishTarget = "MOCK_EXAM"
                targetDropdownExpanded = false
              }
            )
            DropdownMenuItem(
              text = { Text("Publish to All Sections for this Course") },
              onClick = {
                publishTarget = "ALL_SECTIONS"
                targetDropdownExpanded = false
              }
            )
          }
        }
        Text(
          text = "Note: Questions are strictly saved to this course's bank without mixing across courses or notes.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = assessmentTitle,
          onValueChange = { assessmentTitle = it },
          label = { Text("Assessment / Quiz Title") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Question Type Selector
        Text("Question Type:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = typeDropdownExpanded,
          onExpandedChange = { typeDropdownExpanded = it }
        ) {
          OutlinedTextField(
            value = questionType,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = typeDropdownExpanded,
            onDismissRequest = { typeDropdownExpanded = false }
          ) {
            questionTypes.forEach { t ->
              DropdownMenuItem(
                text = { Text(t) },
                onClick = {
                  questionType = t
                  typeDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = questionText,
          onValueChange = { questionText = it },
          label = { Text("Question Text") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )

        if (questionType == "MULTIPLE_CHOICE" || questionType == "IMAGE_DIAGRAM" || questionType == "DRAWING_LABEL") {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = optionA, onValueChange = { optionA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = optionB, onValueChange = { optionB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = optionC, onValueChange = { optionC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = optionD, onValueChange = { optionD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth())
        } else if (questionType == "TRUE_FALSE") {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = optionA.ifBlank { "True" }, onValueChange = { optionA = it }, label = { Text("Option A (True)") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = optionB.ifBlank { "False" }, onValueChange = { optionB = it }, label = { Text("Option B (False)") }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = correctAnswer,
          onValueChange = { correctAnswer = it.uppercase() },
          label = { Text("Correct Answer (A, B, C, D or exact answer)") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = explanation,
          onValueChange = { explanation = it },
          label = { Text("Clinical Explanation & Rationale") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = marks,
            onValueChange = { marks = it },
            label = { Text("Marks") },
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = timeSeconds,
            onValueChange = { timeSeconds = it },
            label = { Text("Time (Sec)") },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Premium Question:", style = MaterialTheme.typography.bodySmall)
          Switch(checked = isPremium, onCheckedChange = { isPremium = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (questionText.isNotBlank()) {
            val optA = if (questionType == "TRUE_FALSE") (if (optionA.isNotBlank()) optionA else "True") else optionA
            val optB = if (questionType == "TRUE_FALSE") (if (optionB.isNotBlank()) optionB else "False") else optionB
            onSave(
              selectedCourseId,
              1L,
              questionText,
              optA,
              optB,
              optionC,
              optionD,
              correctAnswer.ifBlank { "A" },
              explanation,
              difficulty,
              isPremium,
              questionType,
              assessmentTitle,
              marks.toIntOrNull() ?: 1,
              timeSeconds.toIntOrNull() ?: 60,
              publishTarget
            )
          }
        },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Save Question")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExamDialog(
  courses: List<CourseEntity>,
  onDismiss: () -> Unit,
  onSave: (Long, String, String, Int, Int, Boolean) -> Unit
) {
  var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: 1L) }
  var courseDropdownExpanded by remember { mutableStateOf(false) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var duration by remember { mutableStateOf("30") }
  var count by remember { mutableStateOf("15") }
  var isPremium by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Build Examination", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("Target Course:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = courseDropdownExpanded,
          onExpandedChange = { courseDropdownExpanded = it }
        ) {
          val selectedCourse = courses.find { it.id == selectedCourseId }
          OutlinedTextField(
            value = selectedCourse?.let { "${it.code} — ${it.title}" } ?: "Select Course",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = courseDropdownExpanded,
            onDismissRequest = { courseDropdownExpanded = false }
          ) {
            courses.forEach { course ->
              DropdownMenuItem(
                text = { Text("${course.code} — ${course.title}") },
                onClick = {
                  selectedCourseId = course.id
                  courseDropdownExpanded = false
                  if (title.isBlank()) {
                    title = "${course.code} Mock Exam"
                  }
                }
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Exam Title (e.g. RAD 101 Mock 2)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (Minutes)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text("Number of Questions") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Premium Exam:", style = MaterialTheme.typography.bodySmall)
          Switch(checked = isPremium, onCheckedChange = { isPremium = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onSave(selectedCourseId, title, description, duration.toIntOrNull() ?: 30, count.toIntOrNull() ?: 15, isPremium)
          }
        },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Save Exam")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaterialDialog(
  courses: List<CourseEntity>,
  onDismiss: () -> Unit,
  onSave: (Long, Long, String, String, String, String, Boolean, String?) -> Unit
) {
  var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: 1L) }
  var courseDropdownExpanded by remember { mutableStateOf(false) }

  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var type by remember { mutableStateOf("Lecture Note") }
  val types = listOf("Lecture Note", "Handout", "Revision Summary", "Past Question", "Course Outline")
  var typeDropdownExpanded by remember { mutableStateOf(false) }

  var body by remember { mutableStateOf("") }
  var externalUrl by remember { mutableStateOf("") }
  var isPremium by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Upload Study Note / Handout", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("Course Assignment:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = courseDropdownExpanded,
          onExpandedChange = { courseDropdownExpanded = it }
        ) {
          val selectedCourse = courses.find { it.id == selectedCourseId }
          OutlinedTextField(
            value = selectedCourse?.let { "${it.code} — ${it.title}" } ?: "Select Course",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = courseDropdownExpanded,
            onDismissRequest = { courseDropdownExpanded = false }
          ) {
            courses.forEach { course ->
              DropdownMenuItem(
                text = { Text("${course.code} — ${course.title}") },
                onClick = {
                  selectedCourseId = course.id
                  courseDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        Text("Document Type:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = typeDropdownExpanded,
          onExpandedChange = { typeDropdownExpanded = it }
        ) {
          OutlinedTextField(
            value = type,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = typeDropdownExpanded,
            onDismissRequest = { typeDropdownExpanded = false }
          ) {
            types.forEach { t ->
              DropdownMenuItem(
                text = { Text(t) },
                onClick = {
                  type = t
                  typeDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          label = { Text("Document Content / Outline") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = externalUrl,
          onValueChange = { externalUrl = it },
          label = { Text("Google Drive / External Link (Optional)") },
          placeholder = { Text("https://drive.google.com/...") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Premium Material:", style = MaterialTheme.typography.bodySmall)
          Switch(checked = isPremium, onCheckedChange = { isPremium = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onSave(
              selectedCourseId,
              1L,
              title,
              description,
              type,
              body,
              isPremium,
              if (externalUrl.isNotBlank()) externalUrl.trim() else null
            )
          }
        },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Save Material")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLibraryDialog(
  courses: List<CourseEntity>,
  onDismiss: () -> Unit,
  onSave: (Long, String, String, String, String) -> Unit
) {
  var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: 1L) }
  var courseDropdownExpanded by remember { mutableStateOf(false) }

  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var url by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Course Library / Drive", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("Target Course:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = courseDropdownExpanded,
          onExpandedChange = { courseDropdownExpanded = it }
        ) {
          val selectedCourse = courses.find { it.id == selectedCourseId }
          OutlinedTextField(
            value = selectedCourse?.let { "${it.code} — ${it.title}" } ?: "Select Course",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = courseDropdownExpanded,
            onDismissRequest = { courseDropdownExpanded = false }
          ) {
            courses.forEach { course ->
              DropdownMenuItem(
                text = { Text("${course.code} — ${course.title}") },
                onClick = {
                  selectedCourseId = course.id
                  courseDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Resource Name (e.g. RAD 101 Lecture Library)") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description") },
          placeholder = { Text("Lecture notes, clinical positioning blueprints, and atlas") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          label = { Text("Google Drive / External URL") },
          placeholder = { Text("https://drive.google.com/...") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && url.isNotBlank()) {
            val selectedCourse = courses.find { it.id == selectedCourseId }
            val courseCode = selectedCourse?.code ?: "ANA 201"
            onSave(selectedCourseId, courseCode, name.trim(), description.trim(), url.trim())
          }
        },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Save Library")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLibraryDialog(
  library: CourseLibraryEntity,
  courses: List<CourseEntity>,
  onDismiss: () -> Unit,
  onSave: (CourseLibraryEntity) -> Unit
) {
  var selectedCourseId by remember { mutableStateOf(library.courseId) }
  var courseDropdownExpanded by remember { mutableStateOf(false) }

  var name by remember { mutableStateOf(library.name) }
  var description by remember { mutableStateOf(library.description) }
  var url by remember { mutableStateOf(library.externalUrl) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Course Library", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("Target Course:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = courseDropdownExpanded,
          onExpandedChange = { courseDropdownExpanded = it }
        ) {
          val selectedCourse = courses.find { it.id == selectedCourseId }
          OutlinedTextField(
            value = selectedCourse?.let { "${it.code} — ${it.title}" } ?: "Course #${selectedCourseId}",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = courseDropdownExpanded,
            onDismissRequest = { courseDropdownExpanded = false }
          ) {
            courses.forEach { course ->
              DropdownMenuItem(
                text = { Text("${course.code} — ${course.title}") },
                onClick = {
                  selectedCourseId = course.id
                  courseDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Resource Name") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          label = { Text("Google Drive / External URL") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && url.isNotBlank()) {
            val selectedCourse = courses.find { it.id == selectedCourseId }
            val courseCode = selectedCourse?.code ?: library.courseCode
            onSave(
              library.copy(
                courseId = selectedCourseId,
                courseCode = courseCode,
                name = name.trim(),
                description = description.trim(),
                externalUrl = url.trim()
              )
            )
          }
        },
        shape = RoundedCornerShape(8.dp)
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
fun AddSecondaryAdminDialog(
  courses: List<CourseEntity>,
  onDismiss: () -> Unit,
  onSave: (fullName: String, email: String, phone: String, pass: String, permissions: String, assignedCourses: String, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var assignedCourses by remember { mutableStateOf("RAD 101") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  var permMaterials by remember { mutableStateOf(true) }
  var permQuestions by remember { mutableStateOf(true) }
  var permAnnouncements by remember { mutableStateOf(true) }
  var permLibraries by remember { mutableStateOf(true) }
  var permExams by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Secondary Admin", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name (e.g. Dr. John Doe)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("University Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Temporary Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = assignedCourses, onValueChange = { assignedCourses = it }, label = { Text("Assigned Courses (e.g. RAD 101, PHY 101)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))

        Text("Assigned Permissions:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = permMaterials, onCheckedChange = { permMaterials = it })
          Spacer(modifier = Modifier.width(6.dp))
          Text("Manage Study Materials & Handouts", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = permQuestions, onCheckedChange = { permQuestions = it })
          Spacer(modifier = Modifier.width(6.dp))
          Text("Manage Question Bank & CBT Items", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = permLibraries, onCheckedChange = { permLibraries = it })
          Spacer(modifier = Modifier.width(6.dp))
          Text("Manage Google Drive Libraries", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = permAnnouncements, onCheckedChange = { permAnnouncements = it })
          Spacer(modifier = Modifier.width(6.dp))
          Text("Send Notifications & Announcements", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = permExams, onCheckedChange = { permExams = it })
          Spacer(modifier = Modifier.width(6.dp))
          Text("Build & Publish Mock Examinations", style = MaterialTheme.typography.bodySmall)
        }

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(errorMessage!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (fullName.isNotBlank() && email.isNotBlank() && password.length >= 6) {
            val perms = mutableListOf<String>()
            if (permMaterials) perms.add("materials")
            if (permQuestions) perms.add("questions")
            if (permLibraries) perms.add("libraries")
            if (permAnnouncements) perms.add("announcements")
            if (permExams) perms.add("exams")

            onSave(fullName.trim(), email.trim(), phone.trim(), password.trim(), perms.joinToString(","), assignedCourses.trim()) { success, err ->
              if (!success) {
                errorMessage = err ?: "Failed to create secondary admin."
              }
            }
          } else {
            errorMessage = "Please fill all fields and enter a password of at least 6 characters."
          }
        },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Create Admin")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun AddAcademicHierarchyDialog(
  onDismiss: () -> Unit,
  onSaveCourse: (String, String, String, String, String, String) -> Unit
) {
  var code by remember { mutableStateOf("") }
  var title by remember { mutableStateOf("") }
  var dept by remember { mutableStateOf("Medical Radiography") }
  var level by remember { mutableStateOf("100 Level") }
  var sem by remember { mutableStateOf("First Semester") }
  var desc by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Academic Course", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g. RAD 102)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Course Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = dept, onValueChange = { dept = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("Level") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = sem, onValueChange = { sem = it }, label = { Text("Semester") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (code.isNotBlank() && title.isNotBlank()) {
            onSaveCourse(code, title, dept, level, sem, desc)
          }
        },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Save Course")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionSelectionDialog(
  title: String,
  courseName: String,
  availableQuestions: List<QuestionEntity>,
  initialSelectedIds: List<Long>,
  onDismiss: () -> Unit,
  onConfirm: (List<Long>) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedIds by remember { mutableStateOf(initialSelectedIds.toSet()) }

  val filtered = remember(availableQuestions, searchQuery) {
    if (searchQuery.isBlank()) availableQuestions
    else availableQuestions.filter {
      it.questionText.contains(searchQuery, ignoreCase = true) ||
      it.assessmentTitle.contains(searchQuery, ignoreCase = true) ||
      it.questionType.contains(searchQuery, ignoreCase = true)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(
          "Course: $courseName • ${selectedIds.size} of ${availableQuestions.size} selected",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search questions...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = {
              selectedIds = filtered.map { it.id }.toSet()
            }
          ) {
            Text("Select Filtered (${filtered.size})", style = MaterialTheme.typography.labelSmall)
          }

          TextButton(
            onClick = { selectedIds = emptySet() }
          ) {
            Text("Clear All", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (filtered.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              "No questions found for this course.\nCreate new questions first in Questions tab.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filtered, key = { it.id }) { q ->
              val isChecked = selectedIds.contains(q.id)
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    selectedIds = if (isChecked) selectedIds - q.id else selectedIds + q.id
                  },
                shape = RoundedCornerShape(10.dp),
                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                  1.dp,
                  if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                      selectedIds = if (checked) selectedIds + q.id else selectedIds - q.id
                    },
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                      ) {
                        Text(
                          text = q.questionType,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSecondaryContainer,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                          fontSize = 10.sp
                        )
                      }
                      Text(
                        text = "${q.marks} mk • ${q.timeLimitSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = q.questionText,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 3,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedIds.toList()) },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Done (${selectedIds.size} Selected)")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveCloudSyncView(
  adminViewModel: AdminViewModel
) {
  val clipboardManager = LocalClipboardManager.current
  val isSyncing by adminViewModel.isSyncing.collectAsState()
  val statusMessage by adminViewModel.cloudStatusMessage.collectAsState()
  val lastReport by adminViewModel.lastSyncReport.collectAsState()
  val announcements by adminViewModel.announcements.collectAsState()
  val courses by adminViewModel.allCourses.collectAsState()
  val allQuestions by adminViewModel.allQuestions.collectAsState()
  val allExams by adminViewModel.allExams.collectAsState()

  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var announcementTitle by remember { mutableStateOf("") }
  var announcementBody by remember { mutableStateOf("") }
  var announcementPriority by remember { mutableStateOf("Exam Alert") }

  var exportedJson by remember { mutableStateOf<String?>(null) }
  var importJsonInput by remember { mutableStateOf("") }
  var showImportField by remember { mutableStateOf(false) }

  var customSupabaseKey by remember { mutableStateOf(adminViewModel.getSupabaseKey()) }
  var showProjectSettings by remember { mutableStateOf(false) }

  var testConnectionResult by remember { mutableStateOf<String?>(null) }
  var isTestingConnection by remember { mutableStateOf(false) }
  var showMigrationDetails by remember { mutableStateOf(false) }

  // Daily CBT Sprint States
  val sprintConfig by adminViewModel.dailySprintConfig.collectAsState()
  var sprintBadge by remember(sprintConfig.badge) { mutableStateOf(sprintConfig.badge) }
  var sprintTitle by remember(sprintConfig.title) { mutableStateOf(sprintConfig.title) }
  var sprintSubtitle by remember(sprintConfig.subtitle) { mutableStateOf(sprintConfig.subtitle) }
  var sprintCourseId by remember(sprintConfig.courseId) { mutableStateOf(sprintConfig.courseId) }
  var sprintQuestionCount by remember(sprintConfig.questionCount) { mutableStateOf(sprintConfig.questionCount.toString()) }
  var sprintActiveStudents by remember(sprintConfig.activeStudentsText) { mutableStateOf(sprintConfig.activeStudentsText) }
  var sprintTheme by remember(sprintConfig.theme) { mutableStateOf(sprintConfig.theme) }
  var sprintQuestionIds by remember(sprintConfig.questionIds) { mutableStateOf(sprintConfig.questionIds) }
  var sprintCourseDropdownExpanded by remember { mutableStateOf(false) }
  var showSprintQuestionPicker by remember { mutableStateOf(false) }

  // Center Practice Card States
  val practiceCardConfig by adminViewModel.practiceCardConfig.collectAsState()
  var practiceIsConfigured by remember(practiceCardConfig.isConfigured) { mutableStateOf(practiceCardConfig.isConfigured) }
  var practiceCourseId by remember(practiceCardConfig.courseId) { mutableStateOf(practiceCardConfig.courseId) }
  var practiceTitle by remember(practiceCardConfig.courseTitle) { mutableStateOf(practiceCardConfig.courseTitle) }
  var practiceQuestionCount by remember(practiceCardConfig.questionCount) { mutableStateOf(practiceCardConfig.questionCount.toString()) }
  var practiceQuestionIds by remember(practiceCardConfig.questionIds) { mutableStateOf(practiceCardConfig.questionIds) }
  var practiceCourseDropdownExpanded by remember { mutableStateOf(false) }
  var showPracticeQuestionPicker by remember { mutableStateOf(false) }

  // Dashboard Mock CBT Card States
  val mockCardConfig by adminViewModel.mockCardConfig.collectAsState()
  var mockIsConfigured by remember(mockCardConfig.isConfigured) { mutableStateOf(mockCardConfig.isConfigured) }
  var mockCourseId by remember(mockCardConfig.courseId) { mutableStateOf(mockCardConfig.courseId) }
  var mockExamId by remember(mockCardConfig.examId) { mutableStateOf(mockCardConfig.examId) }
  var mockTitle by remember(mockCardConfig.examTitle) { mutableStateOf(mockCardConfig.examTitle) }
  var mockQuestionCount by remember(mockCardConfig.questionCount) { mutableStateOf(mockCardConfig.questionCount.toString()) }
  var mockDurationMinutes by remember(mockCardConfig.durationMinutes) { mutableStateOf(mockCardConfig.durationMinutes.toString()) }
  var mockQuestionIds by remember(mockCardConfig.questionIds) { mutableStateOf(mockCardConfig.questionIds) }
  var mockCourseDropdownExpanded by remember { mutableStateOf(false) }
  var mockExamDropdownExpanded by remember { mutableStateOf(false) }
  var showMockQuestionPicker by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Live Sync Status Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .background(SuccessGreen.copy(alpha = 0.12f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = SuccessGreen)
              } else {
                Icon(Icons.Default.CloudDone, contentDescription = "Cloud Connected", tint = SuccessGreen)
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Live Cloud Sync Engine",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Supabase PostgreSQL • Real-Time Dual Sync",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            color = if (isSyncing) WarningAmber.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp)
          ) {
            Text(
              text = if (isSyncing) "Syncing..." else "ONLINE",
              color = if (isSyncing) WarningAmber else SuccessGreen,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Status: $statusMessage",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Every question, study note, and mock exam you save is stored locally and synced to the cloud. Students across Nigeria automatically download new materials whenever their device is connected.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp
        )

        feedbackMessage?.let { msg ->
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            color = ProfessionalBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = msg,
              color = ProfessionalBlue,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }

    // 2. Action Controls: Push & Pull
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = {
          feedbackMessage = "Pushing all local materials to cloud..."
          adminViewModel.pushAllToCloud { report ->
            feedbackMessage = report.message
          }
        },
        enabled = !isSyncing,
        modifier = Modifier.weight(1f).height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Push All Live", style = MaterialTheme.typography.labelLarge)
      }

      OutlinedButton(
        onClick = {
          feedbackMessage = "Checking cloud for latest questions..."
          adminViewModel.pullLatestFromCloud { report ->
            feedbackMessage = report.message
          }
        },
        enabled = !isSyncing,
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Pull from Cloud", style = MaterialTheme.typography.labelLarge)
      }
    }

    // 2a. Supabase Cloud Setup & SQL Migration Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("admin_supabase_migration_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudDone, contentDescription = null, tint = ProfessionalBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Supabase Cloud Setup & SQL Migration",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Connected: ${adminViewModel.getSupabaseProjectId()}.supabase.co",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "If cloud push encounters Row-Level Security (RLS) restrictions or missing table columns, execute the setup SQL script once in your Supabase SQL Editor. This enables public read/write access and real-time schema alignment.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(com.example.data.supabase.SupabaseMigrationScript.SQL))
              feedbackMessage = "SQL migration script copied to clipboard! Paste and run it in Supabase SQL Editor."
            },
            modifier = Modifier.weight(1f).height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProfessionalBlue),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy SQL Script", style = MaterialTheme.typography.labelMedium)
          }

          OutlinedButton(
            onClick = {
              isTestingConnection = true
              testConnectionResult = "Testing Supabase REST API & table permissions..."
              adminViewModel.testCloudConnection { result ->
                isTestingConnection = false
                testConnectionResult = result
              }
            },
            enabled = !isTestingConnection,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isTestingConnection) "Testing..." else "Test Connection", style = MaterialTheme.typography.labelMedium)
          }
        }

        testConnectionResult?.let { res ->
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            color = if (res.startsWith("CONNECTED: Read & Write active")) SuccessGreen.copy(alpha = 0.12f)
            else if (res.startsWith("READ-ONLY")) WarningAmber.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = res,
              color = if (res.startsWith("CONNECTED: Read & Write active")) SuccessGreen
              else if (res.startsWith("READ-ONLY")) WarningAmber
              else MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
          onClick = { showMigrationDetails = !showMigrationDetails },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = if (showMigrationDetails) "Hide Migration Steps ▲" else "How to Apply in Supabase (3 Simple Steps) ▼",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ProfessionalBlue
          )
        }

        if (showMigrationDetails) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
              .padding(12.dp)
          ) {
            Text(
              text = "1. Open your Supabase Dashboard: supabase.com/dashboard/project/${adminViewModel.getSupabaseProjectId()}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "2. Click on 'SQL Editor' in the left menu and click '+ New Query'.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "3. Paste the copied SQL script and click 'Run'. Once finished, return here and click 'Test Connection'!",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    // 2b. Daily CBT Sprint Realtime Configuration Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("admin_daily_sprint_config_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = WarningAmber)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Daily CBT Sprint Realtime Config",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Pushes course & questions instantly to student home dashboards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Target Course Selector
        Text("Target Course:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
          expanded = sprintCourseDropdownExpanded,
          onExpandedChange = { sprintCourseDropdownExpanded = it }
        ) {
          val selectedCourse = courses.find { it.id == sprintCourseId } ?: courses.firstOrNull()
          OutlinedTextField(
            value = selectedCourse?.let { "${it.code} — ${it.title}" } ?: "Select Course (Default ANA 201)",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sprintCourseDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(10.dp)
          )
          ExposedDropdownMenu(
            expanded = sprintCourseDropdownExpanded,
            onDismissRequest = { sprintCourseDropdownExpanded = false }
          ) {
            courses.forEach { course ->
              DropdownMenuItem(
                text = { Text("${course.code} — ${course.title}") },
                onClick = {
                  sprintCourseId = course.id
                  sprintCourseDropdownExpanded = false
                  sprintQuestionIds = emptyList()
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Question Selection Pipeline
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Specific Questions:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            Text(
              text = if (sprintQuestionIds.isEmpty()) "Using random $sprintQuestionCount questions from selected course"
                     else "${sprintQuestionIds.size} specific question(s) manually chosen",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          OutlinedButton(
            onClick = { showSprintQuestionPicker = true },
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (sprintQuestionIds.isEmpty()) "Choose Questions" else "Edit (${sprintQuestionIds.size})")
          }
        }

        if (sprintQuestionIds.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          TextButton(
            onClick = { sprintQuestionIds = emptyList() },
            modifier = Modifier.align(Alignment.End)
          ) {
            Text("Reset to Random Mode", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Theme Selector
        Text("Sprint Card Global Theme:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(CARD_THEMES) { themeOpt ->
            val isSelected = sprintTheme.equals(themeOpt.id, ignoreCase = true)
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { sprintTheme = themeOpt.id },
              shape = RoundedCornerShape(10.dp),
              color = themeOpt.primaryColor,
              border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, if (isSelected) themeOpt.accentColor else BorderSubtle)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(themeOpt.accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = themeOpt.name.split(" ").firstOrNull() ?: themeOpt.id,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White
                )
                if (isSelected) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(Icons.Default.Check, contentDescription = null, tint = themeOpt.accentColor, modifier = Modifier.size(14.dp))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = sprintBadge,
          onValueChange = { sprintBadge = it },
          label = { Text("Badge Label (e.g. DAILY CBT SPRINT)") },
          modifier = Modifier.fillMaxWidth().testTag("admin_sprint_badge_input"),
          singleLine = true,
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = sprintTitle,
          onValueChange = { sprintTitle = it },
          label = { Text("Sprint Title (e.g. Radiography Challenge)") },
          modifier = Modifier.fillMaxWidth().testTag("admin_sprint_title_input"),
          singleLine = true,
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = sprintSubtitle,
          onValueChange = { sprintSubtitle = it },
          label = { Text("Subtitle Description") },
          modifier = Modifier.fillMaxWidth().testTag("admin_sprint_subtitle_input"),
          minLines = 2,
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = sprintQuestionCount,
            onValueChange = { sprintQuestionCount = it },
            label = { Text("Question Count") },
            modifier = Modifier.weight(1f).testTag("admin_sprint_count_input"),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
          )

          OutlinedTextField(
            value = sprintActiveStudents,
            onValueChange = { sprintActiveStudents = it },
            label = { Text("Active Students Text") },
            modifier = Modifier.weight(1.5f).testTag("admin_sprint_students_input"),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            val count = if (sprintQuestionIds.isNotEmpty()) sprintQuestionIds.size else (sprintQuestionCount.toIntOrNull() ?: 15)
            val selectedCourse = courses.find { it.id == sprintCourseId }
            val updated = sprintConfig.copy(
              badge = sprintBadge.trim().ifBlank { "DAILY CBT SPRINT" },
              title = sprintTitle.trim().ifBlank { "Radiography Challenge" },
              subtitle = sprintSubtitle.trim().ifBlank { "15 practice questions ready for your level" },
              courseId = sprintCourseId,
              courseCode = selectedCourse?.code ?: "ANA 201",
              questionCount = count,
              activeStudentsText = sprintActiveStudents.trim().ifBlank { "38 students active" },
              theme = sprintTheme,
              questionIds = sprintQuestionIds
            )
            adminViewModel.updateDailySprint(updated) {
              feedbackMessage = "Daily CBT Sprint published LIVE across all users and theme applied globally!"
            }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_daily_sprint_button"),
          colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Publish Sprint Live Across App", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 2c. Dashboard Center Practice Card Pipeline Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Center Practice Card Pipeline",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (practiceIsConfigured) "Active on Dashboard — students tap to practice" else "Displayed as 'Coming soon..' in professional style",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = practiceIsConfigured,
            onCheckedChange = { practiceIsConfigured = it }
          )
        }

        if (practiceIsConfigured) {
          Spacer(modifier = Modifier.height(12.dp))

          // Target Course Selector
          Text("Target Course:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(4.dp))
          ExposedDropdownMenuBox(
            expanded = practiceCourseDropdownExpanded,
            onExpandedChange = { practiceCourseDropdownExpanded = it }
          ) {
            val selectedCourse = courses.find { it.id == practiceCourseId } ?: courses.firstOrNull()
            OutlinedTextField(
              value = selectedCourse?.let { "${it.code} — ${it.title}" } ?: "Select Course",
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = practiceCourseDropdownExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor(),
              shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
              expanded = practiceCourseDropdownExpanded,
              onDismissRequest = { practiceCourseDropdownExpanded = false }
            ) {
              courses.forEach { course ->
                DropdownMenuItem(
                  text = { Text("${course.code} — ${course.title}") },
                  onClick = {
                    practiceCourseId = course.id
                    if (practiceTitle.isBlank() || practiceTitle.startsWith("RAD")) {
                      practiceTitle = course.title
                    }
                    practiceCourseDropdownExpanded = false
                    practiceQuestionIds = emptyList()
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = practiceTitle,
            onValueChange = { practiceTitle = it },
            label = { Text("Practice Card Title (e.g. Clinical Practice Questions)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Practice Question Selection Pipeline
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Questions Mode:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
              Text(
                text = if (practiceQuestionIds.isEmpty()) "Random $practiceQuestionCount questions from selected course"
                       else "${practiceQuestionIds.size} specific question(s) manually chosen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            OutlinedButton(
              onClick = { showPracticeQuestionPicker = true },
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (practiceQuestionIds.isEmpty()) "Choose Questions" else "Edit (${practiceQuestionIds.size})")
            }
          }

          if (practiceQuestionIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
              onClick = { practiceQuestionIds = emptyList() },
              modifier = Modifier.align(Alignment.End)
            ) {
              Text("Reset to Random Mode", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            val selectedCourse = courses.find { it.id == practiceCourseId }
            val count = if (practiceQuestionIds.isNotEmpty()) practiceQuestionIds.size else (practiceQuestionCount.toIntOrNull() ?: 15)
            val updated = PracticeCardConfig(
              isConfigured = practiceIsConfigured,
              courseId = practiceCourseId,
              courseCode = selectedCourse?.code ?: "ANA 201",
              courseTitle = practiceTitle.trim().ifBlank { selectedCourse?.title ?: "GROSS ANATOMY OF UPPER AND LOWER LIMBS" },
              questionCount = count,
              questionIds = practiceQuestionIds
            )
            adminViewModel.updatePracticeCard(updated) {
              feedbackMessage = if (practiceIsConfigured) "Practice Card pushed LIVE for ${updated.courseCode}!" else "Practice Card set to 'Coming soon..' mode globally!"
            }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (practiceIsConfigured) "Push Practice Card Pipeline Live" else "Apply 'Coming soon..' to Practice Card")
        }
      }
    }

    // 2d. Dashboard Mock CBT Card Pipeline Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Mock CBT Card Pipeline",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (mockIsConfigured) "Active Mock Exam on Dashboard" else "Displayed as 'Coming soon..' in professional style",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = mockIsConfigured,
            onCheckedChange = { mockIsConfigured = it }
          )
        }

        if (mockIsConfigured) {
          Spacer(modifier = Modifier.height(12.dp))

          // Target Course Selector
          Text("Target Course:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(4.dp))
          ExposedDropdownMenuBox(
            expanded = mockCourseDropdownExpanded,
            onExpandedChange = { mockCourseDropdownExpanded = it }
          ) {
            val selectedCourse = courses.find { it.id == mockCourseId } ?: courses.firstOrNull()
            OutlinedTextField(
              value = selectedCourse?.let { "${it.code} — ${it.title}" } ?: "Select Course",
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mockCourseDropdownExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor(),
              shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
              expanded = mockCourseDropdownExpanded,
              onDismissRequest = { mockCourseDropdownExpanded = false }
            ) {
              courses.forEach { course ->
                DropdownMenuItem(
                  text = { Text("${course.code} — ${course.title}") },
                  onClick = {
                    mockCourseId = course.id
                    if (mockTitle.isBlank() || mockTitle.endsWith("Mock Exam")) {
                      mockTitle = "${course.code} Timed Mock Exam"
                    }
                    mockCourseDropdownExpanded = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Select Exam from Existing Exams
          if (allExams.isNotEmpty()) {
            Text("Link Existing Exam (Optional):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
              expanded = mockExamDropdownExpanded,
              onExpandedChange = { mockExamDropdownExpanded = it }
            ) {
              val currentExam = allExams.find { it.id == mockExamId }
              OutlinedTextField(
                value = currentExam?.let { "${it.title} (${it.durationMinutes}m)" } ?: "Custom Mock Settings",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mockExamDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(10.dp)
              )
              ExposedDropdownMenu(
                expanded = mockExamDropdownExpanded,
                onDismissRequest = { mockExamDropdownExpanded = false }
              ) {
                DropdownMenuItem(
                  text = { Text("Custom Mock Settings (Manual)") },
                  onClick = {
                    mockExamId = 0L
                    mockExamDropdownExpanded = false
                  }
                )
                allExams.forEach { ex ->
                  DropdownMenuItem(
                    text = { Text("${ex.title} • ${ex.durationMinutes} mins • ${ex.totalQuestions} Qs") },
                    onClick = {
                      mockExamId = ex.id
                      mockCourseId = ex.courseId
                      mockTitle = ex.title
                      mockDurationMinutes = ex.durationMinutes.toString()
                      mockQuestionCount = ex.totalQuestions.toString()
                      mockExamDropdownExpanded = false
                    }
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }

          OutlinedTextField(
            value = mockTitle,
            onValueChange = { mockTitle = it },
            label = { Text("Mock Exam Title (e.g. RAD 101 Mock Exam)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = mockDurationMinutes,
              onValueChange = { mockDurationMinutes = it },
              label = { Text("Duration (Mins)") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
              value = mockQuestionCount,
              onValueChange = { mockQuestionCount = it },
              label = { Text("Question Count") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              shape = RoundedCornerShape(10.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Specific Questions:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
              Text(
                text = if (mockQuestionIds.isEmpty()) "Standard exam question bank" else "${mockQuestionIds.size} specific question(s) selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            OutlinedButton(
              onClick = { showMockQuestionPicker = true },
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (mockQuestionIds.isEmpty()) "Choose Questions" else "Edit (${mockQuestionIds.size})")
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            val selectedCourse = courses.find { it.id == mockCourseId }
            val count = if (mockQuestionIds.isNotEmpty()) mockQuestionIds.size else (mockQuestionCount.toIntOrNull() ?: 15)
            val dur = mockDurationMinutes.toIntOrNull() ?: 30
            val updated = MockCardConfig(
              isConfigured = mockIsConfigured,
              courseId = mockCourseId,
              courseCode = selectedCourse?.code ?: "ANA 201",
              courseTitle = selectedCourse?.title ?: "GROSS ANATOMY OF UPPER AND LOWER LIMBS",
              examId = mockExamId,
              examTitle = mockTitle.trim().ifBlank { "${selectedCourse?.code ?: "ANA 201"} Mock CBT" },
              questionCount = count,
              durationMinutes = dur,
              questionIds = mockQuestionIds
            )
            adminViewModel.updateMockCard(updated) {
              feedbackMessage = if (mockIsConfigured) "Mock CBT Card pushed LIVE for ${updated.courseCode}!" else "Mock CBT Card set to 'Coming soon..' mode globally!"
            }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp),
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (mockIsConfigured) "Push Mock CBT Pipeline Live" else "Apply 'Coming soon..' to Mock Card")
        }
      }
    }

    // 3. Instant Live Announcement Broadcast Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Broadcast Live Announcement",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Text(
          text = "Appears instantly on all student home dashboards.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = announcementTitle,
          onValueChange = { announcementTitle = it },
          label = { Text("Title (e.g. RAD 101 Mock Exam Open)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = announcementBody,
          onValueChange = { announcementBody = it },
          label = { Text("Message Body") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )

        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = {
            if (announcementTitle.isNotBlank() && announcementBody.isNotBlank()) {
              adminViewModel.broadcastAnnouncement(announcementTitle, announcementBody, announcementPriority) {
                feedbackMessage = "Broadcast published live to all students!"
                announcementTitle = ""
                announcementBody = ""
              }
            }
          },
          modifier = Modifier.fillMaxWidth().height(46.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Send Broadcast Live")
        }
      }
    }

    // 4. Batch JSON Question Bank Sharing & Import
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Batch Question Bank JSON Tools",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Text(
          text = "Export questions to share, or paste formatted questions to bulk import.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              adminViewModel.exportQuestionsJson { json ->
                exportedJson = json
                clipboardManager.setText(AnnotatedString(json))
                feedbackMessage = "Question bank JSON copied to clipboard!"
              }
            },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export JSON")
          }

          OutlinedButton(
            onClick = { showImportField = !showImportField },
            modifier = Modifier.weight(1f)
          ) {
            Text(if (showImportField) "Hide Import" else "Bulk Import")
          }
        }

        if (showImportField) {
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = importJsonInput,
            onValueChange = { importJsonInput = it },
            label = { Text("Paste Questions JSON Here") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
          )
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = {
              if (importJsonInput.isNotBlank()) {
                adminViewModel.importQuestionsJson(importJsonInput) { result ->
                  result.onSuccess { count ->
                    feedbackMessage = "Successfully imported $count questions and synced live!"
                    importJsonInput = ""
                    showImportField = false
                  }.onFailure { err ->
                    feedbackMessage = "Import failed: ${err.localizedMessage ?: "Invalid JSON"}"
                  }
                }
              }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
          ) {
            Text("Validate & Push to Live Cloud")
          }
        }
      }
    }

    // 5. Cloud Project Configuration Info
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().clickable { showProjectSettings = !showProjectSettings },
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Supabase Cloud Centralized Integration",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (showProjectSettings) "Hide" else "Details",
            style = MaterialTheme.typography.labelMedium,
            color = ProfessionalBlue
          )
        }

        if (showProjectSettings) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Project ID: zvmiquntlxmiotlfpumc",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Endpoint: https://zvmiquntlxmiotlfpumc.supabase.co",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(SuccessGreen, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Status: Centralized Cloud Config Active",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = SuccessGreen
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = {
              adminViewModel.pullLatestFromCloud { report ->
                feedbackMessage = if (report.success) {
                  report.message.ifBlank { "Synchronized with Supabase Cloud successfully." }
                } else report.message
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sync Live Cloud Now")
          }
        }
      }
    }
  }
}
