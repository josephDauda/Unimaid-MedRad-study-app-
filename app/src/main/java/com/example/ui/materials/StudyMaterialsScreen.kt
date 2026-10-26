package com.example.ui.materials

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.StudyMaterialEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.AcademicRepository
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialsScreen(
  user: UserEntity,
  academicRepository: AcademicRepository,
  cloudSyncManager: com.example.data.sync.CloudSyncManager? = null,
  onNavigateBack: () -> Unit,
  onNavigateToSubscription: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val isSyncing by (cloudSyncManager?.isSyncing ?: remember { MutableStateFlow(false) }).collectAsState()
  val allMaterials by academicRepository.getAllMaterials().collectAsState(initial = emptyList())
  val allCourses by academicRepository.getAllCourses().collectAsState(initial = emptyList())
  val courseMap = remember(allCourses) { allCourses.associateBy { it.id } }
  val courses = remember(allCourses, user.level, user.role, user.adminRole) {
    if (user.role == "ADMIN") allCourses
    else {
      val lf = allCourses.filter { com.example.data.util.LevelClassifier.matchesUserLevel(user, it.code, it.level) }
      if (lf.isNotEmpty()) lf else allCourses
    }
  }

  var searchQuery by remember { mutableStateOf("") }
  var selectedCourseId by remember { mutableStateOf<Long?>(null) } // null means "All Courses"
  var selectedCategory by remember { mutableStateOf("All") }
  var viewingMaterial by remember { mutableStateOf<StudyMaterialEntity?>(null) }
  var showLockedDialog by remember { mutableStateOf(false) }

  val categories = listOf("All", "Lecture Note", "Handout", "Revision Summary", "Past Question")

  val filteredMaterials = allMaterials.filter { mat ->
    val parentCourse = courseMap[mat.courseId]
    val matchesLevel = user.role == "ADMIN" || com.example.data.util.LevelClassifier.matchesUserLevel(user, parentCourse?.code, parentCourse?.level)
    val matchesCourse = (selectedCourseId == null || mat.courseId == selectedCourseId)
    val matchesCategory = (selectedCategory == "All" || mat.contentType.equals(selectedCategory, ignoreCase = true))
    val matchesSearch = mat.title.contains(searchQuery, ignoreCase = true) ||
        mat.description.contains(searchQuery, ignoreCase = true) ||
        mat.contentBody.contains(searchQuery, ignoreCase = true)
    matchesLevel && matchesCourse && matchesCategory && matchesSearch
  }

  BackHandler {
    if (viewingMaterial != null) {
      viewingMaterial = null
    } else if (showLockedDialog) {
      showLockedDialog = false
    } else if (searchQuery.isNotBlank()) {
      searchQuery = ""
    } else if (selectedCourseId != null) {
      selectedCourseId = null
    } else {
      onNavigateBack()
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0.dp),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Study Materials & Handouts",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
        },
        navigationIcon = {
          IconButton(onClick = {
            if (viewingMaterial != null) {
              viewingMaterial = null
            } else if (showLockedDialog) {
              showLockedDialog = false
            } else if (searchQuery.isNotBlank()) {
              searchQuery = ""
            } else if (selectedCourseId != null) {
              selectedCourseId = null
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
    PullToRefreshBox(
      isRefreshing = isSyncing,
      onRefresh = {
        coroutineScope.launch {
          cloudSyncManager?.pullLatestFromCloud()
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
          .padding(horizontal = 16.dp)
      ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search lecture notes, handouts, past questions...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("materials_search_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Course Filter Chips (All Courses, RAD 101, PHY 101, BIO 101)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        item {
          FilterChip(
            selected = selectedCourseId == null,
            onClick = { selectedCourseId = null },
            label = { Text("All Courses") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
        items(courses) { course ->
          FilterChip(
            selected = selectedCourseId == course.id,
            onClick = { selectedCourseId = course.id },
            label = { Text(course.code) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Content Category Chips (Lecture Note, Handout, etc.)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(categories) { category ->
          FilterChip(
            selected = selectedCategory == category,
            onClick = { selectedCategory = category },
            label = { Text(category) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // List of Materials
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          val courseLabel = courses.find { it.id == selectedCourseId }?.code ?: "All Courses"
          Text(
            text = "${filteredMaterials.size} Materials Available • $courseLabel",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (filteredMaterials.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "No study materials match your filter criteria.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          items(filteredMaterials) { material ->
            val course = courses.find { it.id == material.courseId }
            MaterialCard(
              material = material,
              courseCode = course?.code ?: "RAD",
              isUserPremium = user.isPremium || user.role == "ADMIN",
              onClick = {
                if (material.isPremium && !user.isPremium && user.role != "ADMIN") {
                  showLockedDialog = true
                } else {
                  viewingMaterial = material
                }
              },
              onOpenExternal = {
                material.externalUrl?.let { url ->
                  try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    Toast.makeText(context, "Unable to open link: ${e.message}", Toast.LENGTH_SHORT).show()
                  }
                }
              }
            )
          }
        }

        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
    }
  }

  // Material Reader Bottom Sheet
  if (viewingMaterial != null) {
    ModalBottomSheet(
      onDismissRequest = { viewingMaterial = null },
      sheetState = rememberModalBottomSheetState()
    ) {
      val mat = viewingMaterial!!
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (mat.isPremium) WarningAmber else SuccessGreen,
            contentColor = Color.White
          ) {
            Text(
              text = if (mat.isPremium) "PREMIUM ARCHIVE" else "DEPARTMENTAL STUDY NOTE",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }

          Text(
            text = mat.contentType,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ProfessionalBlue
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = mat.title,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = mat.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Document Body Container
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
        ) {
          Text(
            text = mat.contentBody,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        if (!mat.externalUrl.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = {
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mat.externalUrl))
                context.startActivity(intent)
              } catch (e: Exception) {
                Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondary,
              contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Attached Resource / Drive")
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = { viewingMaterial = null },
          modifier = Modifier.fillMaxWidth().height(48.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Done Reading")
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Premium locked prompt
  if (showLockedDialog) {
    AlertDialog(
      onDismissRequest = { showLockedDialog = false },
      icon = {
        Icon(Icons.Default.Lock, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(36.dp))
      },
      title = {
        Text("Premium Study Material", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      },
      text = {
        Text(
          "This lecture document and past question archive is exclusive to MEDRAD Study Squad Premium members. Upgrade to unlock full access across all devices.",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showLockedDialog = false
            onNavigateToSubscription()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("View Premium Plans")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLockedDialog = false }) {
          Text("Later")
        }
      }
    )
  }
}

@Composable
fun MaterialCard(
  material: StudyMaterialEntity,
  courseCode: String,
  isUserPremium: Boolean,
  onClick: () -> Unit,
  onOpenExternal: () -> Unit
) {
  val isLocked = material.isPremium && !isUserPremium

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("material_item_${material.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(if (material.isPremium) Color(0xFFFEF3C7) else Color(0xFFE0EBF7)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Description,
          contentDescription = null,
          tint = if (material.isPremium) WarningAmber else ProfessionalBlue,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = courseCode,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }

          Text(
            text = material.contentType,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = ProfessionalBlue
          )

          if (material.isPremium) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFFEF3C7)
            ) {
              Text(
                text = "PREMIUM",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFF92400E),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = material.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1
        )

        Text(
          text = material.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2
        )

        if (!material.externalUrl.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onOpenExternal() }
          ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = ProfessionalBlue, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Attached Cloud Link / Drive",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
              color = ProfessionalBlue
            )
          }
        }
      }
    }
  }
}
