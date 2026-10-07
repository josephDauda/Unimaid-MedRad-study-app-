package com.example.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.UserEntity
import com.example.data.sync.AboutAppContent
import com.example.data.sync.CloudSyncManager
import com.example.ui.auth.outlinedFieldColors
import com.example.ui.components.MedicalRadiographyEmblem
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
  cloudSyncManager: CloudSyncManager,
  onNavigateBack: () -> Unit,
  onNavigateToTerms: () -> Unit
) {
  val aboutContent by cloudSyncManager.aboutAppContent.collectAsState()
  val isSyncing by cloudSyncManager.isSyncing.collectAsState()
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  BackHandler {
    onNavigateBack()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "About MedRad Study App",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
          navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
      )
    }
  ) { padding ->
    PullToRefreshBox(
      isRefreshing = isSyncing,
      onRefresh = {
        coroutineScope.launch {
          cloudSyncManager.pullLatestFromCloud()
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .background(MaterialTheme.colorScheme.background)
          .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Institutional Emblem & App Header
      MedicalRadiographyEmblem(
        size = 80.dp,
        showBorder = true,
        borderColor = WarningAmber
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "MedRad Study App",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "Department of Medical Radiography",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = WarningAmber
      )
      Text(
        text = "College of Medical Sciences • University of Maiduguri",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondarySlate
      )
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MedicalTeal.copy(alpha = 0.12f),
        modifier = Modifier.padding(top = 6.dp)
      ) {
        Text(
          text = "Official Academic Platform • v${aboutContent.currentVersion}",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MedicalTeal,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Card 1: Purpose & Mission
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Purpose & Mission",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Centralized Academic Learning Environment",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "A centralized academic learning platform designed to help Medical Radiography students access course materials, handouts, question banks, quizzes, tests, announcements, learning resources and academic progress in one place.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(12.dp))

          val keyFeatures = listOf(
            "Access course materials and curated lecture notes",
            "Take course-specific quizzes and timed mock exams",
            "Practice with past academic questions and explanations",
            "Receive important departmental announcements instantly",
            "Receive real-time content updates pushed from Admin",
            "Monitor academic performance and track test scores",
            "Access useful radiographic diagrams and learning resources",
            "Stay connected with departmental academic activities"
          )

          keyFeatures.forEach { feature ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MedicalTeal,
                modifier = Modifier
                  .size(18.dp)
                  .padding(top = 2.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = feature,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Card 2: Creator / Founder & Social Media
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = WarningAmber.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Outlined.Person, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(24.dp))
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Created by:",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "PrinceJoeX",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = "Founder / Creator",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "PrinceJoeX is the founder and creator of MedRad Study App, developing this platform to empower Medical Radiography students with modern digital tools for academic excellence, CBT readiness, and collaborative learning.",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Connect with the Creator:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Clickable Official Social Media Icons (Facebook & TikTok ONLY)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Facebook Button
            Surface(
              onClick = {
                openSocialMedia(
                  context = context,
                  webUrl = "https://www.facebook.com/prince.joe.david.2025",
                  appUriString = "fb://facewebmodal/f?href=https://www.facebook.com/prince.joe.david.2025"
                )
              },
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF1877F2).copy(alpha = 0.12f),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.35f)),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("about_facebook_button")
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_facebook),
                  contentDescription = "Facebook",
                  tint = Color.Unspecified,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Facebook",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF1877F2)
                )
              }
            }

            // TikTok Button
            Surface(
              onClick = {
                openSocialMedia(
                  context = context,
                  webUrl = "https://www.tiktok.com/@princejoe.x?_r=1&_t=ZS-9A8LHYo8ClY",
                  appUriString = "snssdk1233://user/profile/princejoe.x"
                )
              },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("about_tiktok_button")
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_tiktok),
                  contentDescription = "TikTok",
                  tint = Color.Unspecified,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "TikTok",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Card 3: Department Contact & Academic Advisory
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Departmental Contact & Support",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = aboutContent.departmentContact,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Academic Advisory & Content Review",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = aboutContent.credits,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Terms of Use & Privacy Button
      TextButton(
        onClick = onNavigateToTerms,
        modifier = Modifier.testTag("about_terms_button")
      ) {
        Icon(Icons.Outlined.Policy, contentDescription = null, tint = MedicalTeal, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          "View Academic Terms of Use & Data Policy",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MedicalTeal
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "© 2026 Department of Medical Radiography • UNIMAID",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
    }
  }
}

/**
 * Safely dispatches a social media link either to its native app or default web browser.
 */
private fun openSocialMedia(context: Context, webUrl: String, appUriString: String? = null) {
  if (appUriString != null) {
    try {
      val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appUriString))
      appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(appIntent)
      return
    } catch (_: Exception) {
      // Native app not installed; fall through to browser
    }
  }

  try {
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(webIntent)
  } catch (_: Exception) {
    // Graceful fallback
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndPrivacyScreen(onNavigateBack: () -> Unit) {
  BackHandler {
    onNavigateBack()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text("Terms of Use & Privacy Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
          navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .background(MaterialTheme.colorScheme.background)
        .padding(20.dp)
    ) {
      Text(
        text = "Departmental Data Policy & Academic Integrity",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "Medical Radiography Academic Practice Environment",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          PolicySection(
            title = "1. Student Profile Data",
            body = "This app stores student names, institutional emails, academic levels, and departmental affiliations strictly for personal test identification and academic progress analytics."
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSubtle)

          PolicySection(
            title = "2. Examination Records & Score Usage",
            body = "Practice quiz and mock examination attempts are recorded locally and synced securely to the departmental cloud to generate semester preparation insights. Scores remain private to the student and authorized departmental academic supervisors."
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSubtle)

          PolicySection(
            title = "3. Academic Content & Intellectual Property",
            body = "All lecture notes, past questions, and radiographic diagrams remain the academic intellectual property of the Department of Medical Radiography, College of Medical Sciences, University of Maiduguri."
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSubtle)

          PolicySection(
            title = "4. Offline Storage",
            body = "Study materials, questions, and test scores are cached in the secure local database (Room) on this device for offline study without requiring continuous internet access."
          )
        }
      }

      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
private fun PolicySection(title: String, body: String) {
  Column {
    Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = body, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFeedbackModal(
  user: UserEntity,
  cloudSyncManager: CloudSyncManager,
  onDismiss: () -> Unit
) {
  val categories = listOf("Academic Inquiry", "Course Question Feedback", "Study Notes Request", "Technical Issue", "Suggestion")
  var selectedCategory by remember { mutableStateOf(categories.first()) }
  var categoryMenuExpanded by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var isSubmitted by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.ContactSupport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isSubmitted) "Feedback Received" else "Student Help & Feedback",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    },
    text = {
      if (isSubmitted) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MedicalTeal, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Thank you! Your feedback has been sent directly to the Department of Medical Radiography administrator.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      } else {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Submit a question, report an issue with course content, or send an inquiry to the department.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Category Dropdown
          ExposedDropdownMenuBox(
            expanded = categoryMenuExpanded,
            onExpandedChange = { categoryMenuExpanded = it }
          ) {
            OutlinedTextField(
              value = selectedCategory,
              onValueChange = {},
              readOnly = true,
              label = { Text("Category") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
              modifier = Modifier.menuAnchor().fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = outlinedFieldColors()
            )
            ExposedDropdownMenu(
              expanded = categoryMenuExpanded,
              onDismissRequest = { categoryMenuExpanded = false }
            ) {
              categories.forEach { cat ->
                DropdownMenuItem(
                  text = { Text(cat) },
                  onClick = {
                    selectedCategory = cat
                    categoryMenuExpanded = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = message,
            onValueChange = {
              message = it
              if (message.isNotBlank()) errorMessage = null
            },
            label = { Text("Your Message / Feedback") },
            placeholder = { Text("Type your inquiry or issue here...", color = TextMuted) },
            modifier = Modifier.fillMaxWidth().height(130.dp),
            shape = RoundedCornerShape(10.dp),
            colors = outlinedFieldColors(),
            maxLines = 5
          )

          if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
          }
        }
      }
    },
    confirmButton = {
      if (isSubmitted) {
        Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
          Text("Close")
        }
      } else {
        Button(
          onClick = {
            if (message.isBlank()) {
              errorMessage = "Please enter your message"
            } else {
              isSubmitting = true
              CoroutineScope(Dispatchers.IO).launch {
                cloudSyncManager.submitStudentFeedback(
                  studentName = user.fullName,
                  studentEmail = user.email,
                  category = selectedCategory,
                  message = message.trim()
                )
                isSubmitting = false
                isSubmitted = true
              }
            }
          },
          enabled = !isSubmitting,
          shape = RoundedCornerShape(10.dp)
        ) {
          if (isSubmitting) {
            Text("Sending...")
          } else {
            Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Send to Admin")
          }
        }
      }
    },
    dismissButton = {
      if (!isSubmitted) {
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    }
  )
}
