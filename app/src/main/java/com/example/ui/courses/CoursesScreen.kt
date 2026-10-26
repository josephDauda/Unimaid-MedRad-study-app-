package com.example.ui.courses

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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.StudyMaterialEntity
import com.example.data.local.entity.TopicEntity
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
fun CoursesScreen(
  user: UserEntity,
  academicRepository: AcademicRepository,
  cloudSyncManager: com.example.data.sync.CloudSyncManager? = null,
  onNavigateBack: () -> Unit,
  onStartCoursePractice: (courseId: Long, code: String, title: String) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val isSyncing by (cloudSyncManager?.isSyncing ?: remember { MutableStateFlow(false) }).collectAsState()
  val allCourses by academicRepository.getAllCourses().collectAsState(initial = emptyList())
  val courses = remember(allCourses, user.level, user.role, user.adminRole) {
    if (user.role == "ADMIN") {
      allCourses
    } else {
      val levelFiltered = allCourses.filter { com.example.data.util.LevelClassifier.matchesUserLevel(user, it.code, it.level) }
      if (levelFiltered.isNotEmpty()) levelFiltered else allCourses
    }
  }

  var selectedCourseId by remember { mutableStateOf<Long?>(null) }
  val activeCourse = courses.find { it.id == selectedCourseId } ?: courses.firstOrNull()

  // Collect data specific to active course
  val courseTopics by academicRepository.getTopicsByCourse(activeCourse?.id ?: 1L)
    .collectAsState(initial = emptyList())
  val courseMaterials by academicRepository.getMaterialsForCourse(activeCourse?.id ?: 1L)
    .collectAsState(initial = emptyList())
  val courseQuestions by academicRepository.getQuestionsForCourse(activeCourse?.id ?: 1L)
    .collectAsState(initial = emptyList())
  val courseExams by academicRepository.getExamsForCourse(activeCourse?.id ?: 1L)
    .collectAsState(initial = emptyList())
  val courseLibraries by academicRepository.getLibrariesForCourse(activeCourse?.id ?: 1L)
    .collectAsState(initial = emptyList())

  // Internal tab state: 0 -> Materials, 1 -> Handouts, 2 -> Question Bank & Quizzes, 3 -> Google Drive Libraries, 4 -> Syllabus Topics
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Materials", "Handouts", "CBT & Questions", "Drive Library", "Syllabus")

  var viewingMaterial by remember { mutableStateOf<StudyMaterialEntity?>(null) }
  var showLockedDialog by remember { mutableStateOf(false) }

  BackHandler {
    if (viewingMaterial != null) {
      viewingMaterial = null
    } else if (showLockedDialog) {
      showLockedDialog = false
    } else if (selectedTab != 0) {
      selectedTab = 0
    } else {
      onNavigateBack()
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0.dp),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Academic Study Zone",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = activeCourse?.let { "${it.code} — ${it.title}" } ?: "Departmental Courses",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = {
            if (viewingMaterial != null) {
              viewingMaterial = null
            } else if (showLockedDialog) {
              showLockedDialog = false
            } else if (selectedTab != 0) {
              selectedTab = 0
            } else {
              onNavigateBack()
            }
          }) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onBackground
            )
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
      ) {
      // 1. Department & University Header Banner
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.School,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = user.university,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${user.faculty} • ${user.department}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // 2. Course Selection Carousel (RAD 101, PHY 101, BIO 101, etc.)
      Text(
        text = "Select Course Hub",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
      )

      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(courses) { course ->
          val isSelected = (course.id == (activeCourse?.id ?: -1L))
          Surface(
            onClick = {
              selectedCourseId = course.id
            },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.height(48.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(if (isSelected) MaterialTheme.colorScheme.primary else ProfessionalBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = course.code.take(3),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else ProfessionalBlue
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = course.code,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = course.level,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 3. Course Specific Sub-Tabs (Materials, Handouts, CBT/Quizzes, Drive Library, Syllabus)
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = MaterialTheme.colorScheme.primary
          )
        }
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      // 4. Tab Content Body
      val currentCourse = activeCourse
      if (currentCourse == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("No courses found. Please check back later.")
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Course Header Card
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                  ) {
                    Text(
                      text = currentCourse.code,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                  Text(
                    text = "${currentCourse.level} • ${currentCourse.semester}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ProfessionalBlue
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                  text = currentCourse.title,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )

                if (currentCourse.description.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = currentCourse.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          // Section Content based on selected tab
          when (selectedTab) {
            0 -> {
              // Materials (Lecture Notes, Revision Summaries, Course Outlines)
              val materialsOnly = courseMaterials.filter { !it.contentType.equals("Handout", ignoreCase = true) }
              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${currentCourse.code} Study Materials & Notes (${materialsOnly.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                  )
                }
              }

              if (materialsOnly.isEmpty()) {
                item {
                  EmptySectionCard(
                    icon = Icons.Default.MenuBook,
                    message = "No lecture notes uploaded yet for ${currentCourse.code}."
                  )
                }
              } else {
                items(materialsOnly) { mat ->
                  CourseMaterialCard(
                    material = mat,
                    user = user,
                    onClick = {
                      if (mat.isPremium && !user.isPremium && user.role != "ADMIN") {
                        showLockedDialog = true
                      } else {
                        viewingMaterial = mat
                      }
                    },
                    onOpenExternal = {
                      mat.externalUrl?.let { url -> openExternalUrl(context, url) }
                    }
                  )
                }
              }
            }

            1 -> {
              // Handouts
              val handouts = courseMaterials.filter { it.contentType.equals("Handout", ignoreCase = true) }
              item {
                Text(
                  text = "${currentCourse.code} Official Handouts (${handouts.size})",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onBackground
                )
              }

              if (handouts.isEmpty()) {
                item {
                  EmptySectionCard(
                    icon = Icons.Default.Description,
                    message = "No departmental handouts released yet for ${currentCourse.code}."
                  )
                }
              } else {
                items(handouts) { handout ->
                  CourseMaterialCard(
                    material = handout,
                    user = user,
                    onClick = {
                      if (handout.isPremium && !user.isPremium && user.role != "ADMIN") {
                        showLockedDialog = true
                      } else {
                        viewingMaterial = handout
                      }
                    },
                    onOpenExternal = {
                      handout.externalUrl?.let { url -> openExternalUrl(context, url) }
                    }
                  )
                }
              }
            }

            2 -> {
              // Question Bank & CBT/Quizzes
              item {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(
                        text = "${currentCourse.code} CBT Question Bank",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "${courseQuestions.size} exam-grade questions curated for ${currentCourse.code} covering multiple choice, true/false, fill-in-the-blank, and diagnostic radiograph interpretation.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                      onClick = { onStartCoursePractice(currentCourse.id, currentCourse.code, currentCourse.title) },
                      modifier = Modifier.fillMaxWidth().height(46.dp),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                      ),
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Launch CBT Practice (${currentCourse.code})")
                    }
                  }
                }
              }

              item {
                Text(
                  text = "Published Continuous Assessments & Mock Exams (${courseExams.size})",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.padding(top = 8.dp)
                )
              }

              if (courseExams.isEmpty()) {
                item {
                  EmptySectionCard(
                    icon = Icons.Default.Quiz,
                    message = "No timed mock examinations scheduled for ${currentCourse.code}."
                  )
                }
              } else {
                items(courseExams) { exam ->
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                  ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = exam.title,
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                          color = if (exam.isPremium) WarningAmber.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.15f),
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text(
                            text = if (exam.isPremium) "PREMIUM" else "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (exam.isPremium) WarningAmber else SuccessGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = exam.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Spacer(modifier = Modifier.height(10.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "${exam.totalQuestions} Questions • ${exam.durationMinutes} Mins",
                          style = MaterialTheme.typography.labelSmall,
                          color = ProfessionalBlue
                        )
                        OutlinedButton(
                          onClick = { onStartCoursePractice(currentCourse.id, currentCourse.code, exam.title) },
                          shape = RoundedCornerShape(8.dp)
                        ) {
                          Text("Take Quiz")
                        }
                      }
                    }
                  }
                }
              }
            }

            3 -> {
              // Google Drive Libraries & External Resources
              item {
                Text(
                  text = "${currentCourse.code} Cloud Libraries & Drive Resources (${courseLibraries.size})",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onBackground
                )
              }

              if (courseLibraries.isEmpty()) {
                item {
                  EmptySectionCard(
                    icon = Icons.Outlined.Folder,
                    message = "No external Google Drive links or libraries added for ${currentCourse.code} yet."
                  )
                }
              } else {
                items(courseLibraries) { library ->
                  GoogleDriveLibraryCard(
                    library = library,
                    onOpen = {
                      openExternalUrl(context, library.externalUrl)
                    }
                  )
                }
              }
            }

            4 -> {
              // Curricular Topics
              item {
                Text(
                  text = "${currentCourse.code} Syllabus & Approved Curriculum (${courseTopics.size} Topics)",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onBackground
                )
              }

              if (courseTopics.isEmpty()) {
                item {
                  EmptySectionCard(
                    icon = Icons.Default.Topic,
                    message = "No curriculum topics registered for ${currentCourse.code}."
                  )
                }
              } else {
                items(courseTopics) { topic ->
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                  ) {
                    Row(
                      modifier = Modifier.padding(14.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                          modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ProfessionalBlue.copy(alpha = 0.15f)),
                          contentAlignment = Alignment.Center
                        ) {
                          Text(
                            text = "${topic.orderIndex}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ProfessionalBlue
                          )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                          text = topic.name,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                          color = MaterialTheme.colorScheme.onSurface
                        )
                      }
                      Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          item {
            Spacer(modifier = Modifier.height(28.dp))
          }
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

        // Document Body
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
              openExternalUrl(context, mat.externalUrl)
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

        Spacer(modifier = Modifier.height(16.dp))

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

  // Premium lock dialog
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
        Text("This material is part of the Departmental Premium Exam Archive. Upgrade your account to access.")
      },
      confirmButton = {
        Button(
          onClick = { showLockedDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("Dismiss")
        }
      }
    )
  }
}

/**
 * Google Drive Library Card Component
 */
@Composable
fun GoogleDriveLibraryCard(
  library: CourseLibraryEntity,
  onOpen: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("google_drive_library_card_${library.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      // 1. Top Badges Row: Course Tag + Verified Cloud Repository + Drive Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = library.courseCode.ifBlank { "ANA 201" },
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
          ) {
            Text(
              text = "Google Drive Archive",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        // Live Cloud Verified Status Pill
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = SuccessGreen.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(SuccessGreen)
            )
            Text(
              text = "LIVE CLOUD",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
              color = SuccessGreen
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Middle Body: Drive Icon Tile + Title & Description (Safe Multi-Line Text Wrapping)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        // High-polish Google Drive Thumbnail Container
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = ProfessionalBlue.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, ProfessionalBlue.copy(alpha = 0.25f)),
          modifier = Modifier.size(52.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.FolderShared,
              contentDescription = "Google Drive Folder",
              tint = ProfessionalBlue,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = library.name,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              lineHeight = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )

          if (library.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = library.description,
              style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = 19.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Official faculty shared notes, diagrams & reference manuals",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Bottom Action Bar: Open Library Button with High Touch-Target & Ripple
      Button(
        onClick = onOpen,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("open_library_button_${library.id}"),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(
          Icons.AutoMirrored.Filled.OpenInNew,
          contentDescription = "Open in Google Drive",
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Open Library",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

/**
 * Course Material Item Card
 */
@Composable
fun CourseMaterialCard(
  material: StudyMaterialEntity,
  user: UserEntity,
  onClick: () -> Unit,
  onOpenExternal: () -> Unit
) {
  val isLocked = material.isPremium && !user.isPremium && user.role != "ADMIN"

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (material.isPremium) WarningAmber.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f)
        ) {
          Text(
            text = if (material.isPremium) "PREMIUM" else "FREE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = if (material.isPremium) WarningAmber else SuccessGreen,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }

        Text(
          text = material.contentType,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
          color = ProfessionalBlue
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = material.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )
        if (isLocked) {
          Icon(Icons.Default.Lock, contentDescription = "Locked", tint = WarningAmber, modifier = Modifier.size(18.dp))
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = material.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2
      )

      if (!material.externalUrl.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenExternal() },
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = ProfessionalBlue, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "External Cloud Resource Attached",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ProfessionalBlue
          )
        }
      }
    }
  }
}

@Composable
fun EmptySectionCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  message: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

fun openExternalUrl(context: Context, url: String) {
  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
  } catch (e: Exception) {
    Toast.makeText(context, "Unable to open link: ${e.localizedMessage ?: "Invalid URL"}", Toast.LENGTH_SHORT).show()
  }
}
