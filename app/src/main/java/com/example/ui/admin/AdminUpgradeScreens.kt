package com.example.ui.admin

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.ExamAttemptEntity
import com.example.data.local.entity.UserEntity
import com.example.data.sync.AboutAppContent
import com.example.data.sync.StudentFeedbackItem
import com.example.ui.theme.BorderSubtle
import com.example.ui.auth.outlinedFieldColors
import com.example.ui.components.MedicalRadiographyEmblem
import com.example.ui.theme.CardThemeOption
import com.example.ui.theme.CARD_THEMES
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1. Global Student Card Styling View:
 * Admin sets the student profile card theme ONCE, and it applies automatically to all students.
 */
@Composable
fun AdminGlobalCardStyleView(
  adminViewModel: AdminViewModel
) {
  val currentStyle by adminViewModel.globalCardStyle.collectAsState()
  var selectedStyle by remember(currentStyle) { mutableStateOf(currentStyle) }
  var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

  val styles = CARD_THEMES

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 12.dp)
  ) {
    Text(
      text = "One-Time Global Card Styling",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Configure the official student profile card theme once. This updates the card design for all students across the platform in real time via Firestore.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Live Card Preview
    val activePreviewOption = styles.find { it.id == selectedStyle } ?: styles.first()
    Text(
      text = "LIVE STUDENT CARD PREVIEW:",
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
      color = WarningAmber
    )
    Spacer(modifier = Modifier.height(8.dp))

    StudentProfileCardPreview(option = activePreviewOption)

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Select Department Card Theme:",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    styles.forEach { option ->
      val isSelected = selectedStyle == option.id
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable { selectedStyle = option.id },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) option.accentColor else BorderSubtle)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(option.gradientColors))
              .border(1.5.dp, option.accentColor, CircleShape)
          )

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(option.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          if (isSelected) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected", tint = option.accentColor, modifier = Modifier.size(24.dp))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (saveSuccessMessage != null) {
      Surface(
        color = SuccessGreen.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
      ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Check, contentDescription = null, tint = SuccessGreen)
          Spacer(modifier = Modifier.width(8.dp))
          Text(saveSuccessMessage!!, color = SuccessGreen, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
        }
      }
    }

    Button(
      onClick = {
        adminViewModel.setGlobalCardStyle(selectedStyle) {
          saveSuccessMessage = "Card theme updated globally! All students now see '${activePreviewOption.name}'."
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
      Icon(Icons.Outlined.Style, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Apply Globally to All Students")
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}


@Composable
fun StudentProfileCardPreview(option: CardThemeOption) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = option.primaryColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    border = BorderStroke(1.5.dp, option.accentColor)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(Brush.linearGradient(option.gradientColors))
        .padding(18.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            MedicalRadiographyEmblem(size = 40.dp, showBorder = true, borderColor = option.accentColor)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("MEDICAL RADIOGRAPHY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = option.accentColor)
              Text("University of Maiduguri", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            }
          }

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = option.accentColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, option.accentColor)
          ) {
            Text(
              "STUDENT ID",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
              color = option.accentColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Joseph Dauda", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
        Text("Matric: UNIMAID/RAD/2026/042", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Level: 300 Level", style = MaterialTheme.typography.labelMedium, color = option.accentColor)
          Text("Faculty: Medical Sciences", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
        }
      }
    }
  }
}

/**
 * 2. Push Notifications Broadcaster:
 * Admin composes and sends announcements to students with audible sound and notification center UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPushNotificationsView(
  adminViewModel: AdminViewModel
) {
  var title by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }
  val targetOptions = listOf("All Students", "100 Level", "200 Level", "300 Level", "400 Level", "500 Level", "RAD 101", "RAD 102", "RAD 201")
  var selectedTarget by remember { mutableStateOf(targetOptions.first()) }
  var targetExpanded by remember { mutableStateOf(false) }

  val priorities = listOf("Normal", "Urgent", "Exam Alert")
  var selectedPriority by remember { mutableStateOf(priorities.first()) }

  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isSending by remember { mutableStateOf(false) }

  val recentAnnouncements by adminViewModel.announcements.collectAsState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 12.dp)
  ) {
    Text(
      text = "Broadcast Push Notifications",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Send departmental alerts, lecture changes, and exam reminders. Students receive an in-app banner alert and notification sound.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Notification Title") },
          placeholder = { Text("e.g. RAD 101 Mock Exam Scheduled for Friday", color = TextMuted) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = outlinedFieldColors()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Target audience dropdown
        ExposedDropdownMenuBox(
          expanded = targetExpanded,
          onExpandedChange = { targetExpanded = it }
        ) {
          OutlinedTextField(
            value = selectedTarget,
            onValueChange = {},
            readOnly = true,
            label = { Text("Target Audience") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = outlinedFieldColors()
          )
          ExposedDropdownMenu(
            expanded = targetExpanded,
            onDismissRequest = { targetExpanded = false }
          ) {
            targetOptions.forEach { opt ->
              DropdownMenuItem(
                text = { Text(opt) },
                onClick = {
                  selectedTarget = opt
                  targetExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Priority selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          priorities.forEach { prio ->
            val isSelected = selectedPriority == prio
            OutlinedButton(
              onClick = { selectedPriority = prio },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
              ),
              border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
              Text(prio, style = MaterialTheme.typography.labelSmall)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text("Notification Message") },
          placeholder = { Text("Enter detailed instruction, venue, or syllabus details...", color = TextMuted) },
          modifier = Modifier.fillMaxWidth().height(120.dp),
          shape = RoundedCornerShape(10.dp),
          colors = outlinedFieldColors(),
          maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (statusMessage != null) {
          Text(statusMessage!!, color = SuccessGreen, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 10.dp))
        }

        Button(
          onClick = {
            if (title.isNotBlank() && message.isNotBlank()) {
              isSending = true
              adminViewModel.sendPushNotification(title.trim(), message.trim(), selectedTarget, selectedPriority) {
                isSending = false
                statusMessage = "Notification broadcasted successfully to $selectedTarget!"
                title = ""
                message = ""
              }
            }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          enabled = !isSending && title.isNotBlank() && message.isNotBlank()
        ) {
          Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isSending) "Broadcasting..." else "Push Notification Now")
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Recent Department Announcements (${recentAnnouncements.size})",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    recentAnnouncements.forEach { ann ->
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(ann.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(ann.priority, style = MaterialTheme.typography.labelSmall, color = WarningAmber)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(ann.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
          Spacer(modifier = Modifier.height(4.dp))
          Text("Target: ${ann.target} • ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ann.timestamp))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

/**
 * 3. Score & Results Export View:
 * Admin views all students' test scores and exports/prints them (shareable transcript/grade sheet).
 */
@Composable
fun AdminScoreExportView(
  adminViewModel: AdminViewModel
) {
  val attempts by adminViewModel.allAttempts.collectAsState()
  val courses by adminViewModel.allCourses.collectAsState()
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current

  var selectedCourseFilter by remember { mutableStateOf("All Courses") }
  var courseFilterExpanded by remember { mutableStateOf(false) }

  var exportedText by remember { mutableStateOf<String?>(null) }

  val courseNames = listOf("All Courses") + courses.map { it.code }

  val filteredAttempts = if (selectedCourseFilter == "All Courses") {
    attempts
  } else {
    attempts.filter { it.courseTitle.contains(selectedCourseFilter, ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 12.dp)
  ) {
    Text(
      text = "Student Examination Scores & Grade Export",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "View student CBT performance across departmental examinations and export printable grade transcripts.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Filter and Action Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Course Filter Dropdown
      Box {
        OutlinedButton(
          onClick = { courseFilterExpanded = true },
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("Course: $selectedCourseFilter")
        }
        androidx.compose.material3.DropdownMenu(
          expanded = courseFilterExpanded,
          onDismissRequest = { courseFilterExpanded = false }
        ) {
          courseNames.forEach { name ->
            DropdownMenuItem(
              text = { Text(name) },
              onClick = {
                selectedCourseFilter = name
                courseFilterExpanded = false
              }
            )
          }
        }
      }

      // Export / Share Button
      Button(
        onClick = {
          adminViewModel.exportStudentScores(if (selectedCourseFilter == "All Courses") null else selectedCourseFilter) { report ->
            exportedText = report
            // Also trigger Android system share sheet
            val sendIntent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(Intent.EXTRA_TEXT, report)
              putExtra(Intent.EXTRA_SUBJECT, "MedRad Department CBT Examination Results")
              type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export Examination Results"))
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Export / Share")
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "Total Attempts: ${filteredAttempts.size}",
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
      color = ProfessionalBlue
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (filteredAttempts.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No exam attempts recorded under $selectedCourseFilter yet.", color = TextMuted)
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredAttempts) { att ->
          val percent = if (att.totalQuestions > 0) (att.score * 100) / att.totalQuestions else 0
          val passed = percent >= 50

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
                Text(att.examTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(att.courseTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                  text = "Student ID #${att.userId} • Date: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(att.timestamp))}",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMuted
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (passed) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (passed) SuccessGreen else ErrorRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
                Text(
                  text = "${att.score}/${att.totalQuestions} Marks",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * 4. About App Management View:
 * Admin updates description, version notes, department contact info, and push update notifications.
 */
@Composable
fun AdminAboutAppView(
  adminViewModel: AdminViewModel
) {
  val currentAbout by adminViewModel.aboutAppContent.collectAsState()

  var desc by remember(currentAbout) { mutableStateOf(currentAbout.description) }
  var versionNotes by remember(currentAbout) { mutableStateOf(currentAbout.versionNotes) }
  var contact by remember(currentAbout) { mutableStateOf(currentAbout.departmentContact) }
  var credits by remember(currentAbout) { mutableStateOf(currentAbout.credits) }
  var hasUpdateNotice by remember(currentAbout) { mutableStateOf(currentAbout.hasUpdateNotice) }
  var updateNotes by remember(currentAbout) { mutableStateOf(currentAbout.updateNotes) }

  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isSaving by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 12.dp)
  ) {
    Text(
      text = "About App Management",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Manage the 'About this app' description, faculty credits, contact info, and update notices. Changes push instantly to all students in real time.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        OutlinedTextField(
          value = desc,
          onValueChange = { desc = it },
          label = { Text("App Purpose / Overview") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = outlinedFieldColors(),
          minLines = 3
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = versionNotes,
          onValueChange = { versionNotes = it },
          label = { Text("Version Notes & Changelog") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = outlinedFieldColors(),
          minLines = 2
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = contact,
          onValueChange = { contact = it },
          label = { Text("Departmental Contact Info") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = outlinedFieldColors(),
          minLines = 2
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = credits,
          onValueChange = { credits = it },
          label = { Text("Faculty & Academic Credits") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = outlinedFieldColors(),
          minLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Update Notice Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Broadcast New App Version Notice", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text("Displays a non-blocking update banner on students' dashboards.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Switch(checked = hasUpdateNotice, onCheckedChange = { hasUpdateNotice = it })
        }

        if (hasUpdateNotice) {
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = updateNotes,
            onValueChange = { updateNotes = it },
            label = { Text("Update Banner Prompt Message") },
            placeholder = { Text("e.g. New 2026 Semester Mock Questions and Anatomy Diagrams have been released.", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = outlinedFieldColors()
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (statusMessage != null) {
          Text(statusMessage!!, color = SuccessGreen, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 10.dp))
        }

        Button(
          onClick = {
            isSaving = true
            val updated = AboutAppContent(
              description = desc.trim(),
              versionNotes = versionNotes.trim(),
              departmentContact = contact.trim(),
              credits = credits.trim(),
              hasUpdateNotice = hasUpdateNotice,
              updateNotes = updateNotes.trim()
            )
            adminViewModel.updateAboutAppManagement(updated) {
              isSaving = false
              statusMessage = "About App details pushed live to all student devices!"
            }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          enabled = !isSaving
        ) {
          Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isSaving) "Pushing to Cloud..." else "Push About Updates Instantly to Students")
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

/**
 * 5. Student Feedback Inbox View:
 * Admin reviews questions, feedback, and issue reports submitted by students.
 */
@Composable
fun AdminFeedbackInboxView(
  adminViewModel: AdminViewModel
) {
  val feedbackList by adminViewModel.studentFeedbackList.collectAsState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 12.dp)
  ) {
    Text(
      text = "Student Help & Feedback Inbox (${feedbackList.size})",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Inquiries, question feedback, and reports submitted by students from the help channel.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(14.dp))

    if (feedbackList.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Outlined.Feedback, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(10.dp))
          Text("No student feedback inquiries received yet.", color = TextMuted)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(feedbackList) { item ->
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
                Text(item.studentName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MedicalTeal.copy(alpha = 0.15f)
                ) {
                  Text(
                    item.category,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MedicalTeal,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Text(item.studentEmail, style = MaterialTheme.typography.labelSmall, color = TextMuted)

              Spacer(modifier = Modifier.height(8.dp))

              Text(item.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
