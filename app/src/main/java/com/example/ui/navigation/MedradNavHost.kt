package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MedradAppContainer
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.about.AboutAppScreen
import com.example.ui.about.TermsAndPrivacyScreen
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegisterScreen
import com.example.ui.cbt.CbtScreen
import com.example.ui.courses.CoursesScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.materials.StudyMaterialsScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.results.ResultsScreen
import com.example.ui.subscription.SubscriptionScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CbtViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.ResultsViewModel
import com.example.ui.viewmodel.SubscriptionViewModel
import com.example.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

enum class ScreenRoute {
  WELCOME,
  LOGIN,
  REGISTER,
  DASHBOARD,
  COURSES,
  MATERIALS,
  PROGRESS,
  SUBSCRIPTION,
  ADMIN,
  CBT_SESSION,
  RESULTS,
  ABOUT,
  TERMS
}

@Composable
fun MedradNavHost(
  container: MedradAppContainer
) {
  val authViewModel = remember { AuthViewModel(container.authRepository) }
  val dashboardViewModel = remember {
    DashboardViewModel(
      container.cbtRepository,
      container.academicRepository,
      container.subscriptionRepository,
      container.cloudSyncManager
    )
  }
  val cbtViewModel = remember { CbtViewModel(container.cbtRepository) }
  val resultsViewModel = remember { ResultsViewModel(container.cbtRepository, container.academicRepository) }
  val subscriptionViewModel = remember { SubscriptionViewModel(container.subscriptionRepository, container.authRepository) }
  val adminViewModel = remember { AdminViewModel(container.adminRepository, container.academicRepository, container.cloudSyncManager) }

  val currentUser by authViewModel.currentUser.collectAsState()

  var currentRoute by remember { mutableStateOf(ScreenRoute.WELCOME) }
  var currentAttemptId by remember { mutableLongStateOf(0L) }
  val backStack = remember { mutableStateListOf<ScreenRoute>() }
  val focusManager = LocalFocusManager.current

  fun navigateTo(route: ScreenRoute) {
    if (route == currentRoute) return
    focusManager.clearFocus(force = true)
    if (route == ScreenRoute.DASHBOARD) {
      backStack.clear()
      currentRoute = ScreenRoute.DASHBOARD
    } else {
      if (backStack.isEmpty()) {
        backStack.add(currentRoute)
      } else {
        backStack.removeAll { it == route }
        backStack.add(currentRoute)
      }
      currentRoute = route
    }
  }

  fun popBackStack() {
    focusManager.clearFocus(force = true)
    if (backStack.isNotEmpty()) {
      currentRoute = backStack.removeAt(backStack.size - 1)
    } else if (currentUser != null && currentRoute != ScreenRoute.DASHBOARD) {
      currentRoute = ScreenRoute.DASHBOARD
    } else if (currentUser == null && currentRoute != ScreenRoute.WELCOME) {
      currentRoute = ScreenRoute.WELCOME
    }
  }

  // Intercept Android System Back when not at root/home destination
  BackHandler(
    enabled = (currentUser != null && currentRoute != ScreenRoute.DASHBOARD) ||
              (currentUser == null && currentRoute != ScreenRoute.WELCOME)
  ) {
    popBackStack()
  }

  // Check auth session safely in side-effect
  LaunchedEffect(currentUser, currentRoute) {
    if (currentUser != null && (currentRoute == ScreenRoute.WELCOME || currentRoute == ScreenRoute.LOGIN || currentRoute == ScreenRoute.REGISTER)) {
      backStack.clear()
      currentRoute = ScreenRoute.DASHBOARD
    } else if (currentUser == null && currentRoute != ScreenRoute.LOGIN && currentRoute != ScreenRoute.REGISTER && currentRoute != ScreenRoute.ABOUT && currentRoute != ScreenRoute.TERMS && currentRoute != ScreenRoute.WELCOME) {
      backStack.clear()
      currentRoute = ScreenRoute.WELCOME
    }
  }

  AnimatedContent(
    targetState = currentRoute,
    transitionSpec = {
      (fadeIn(animationSpec = tween(280)) + slideInHorizontally(animationSpec = tween(280)) { fullWidth ->
        if (targetState.ordinal >= initialState.ordinal) fullWidth / 4 else -fullWidth / 4
      }) togetherWith (
        fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { fullWidth ->
          if (targetState.ordinal >= initialState.ordinal) -fullWidth / 4 else fullWidth / 4
        }
      )
    },
    label = "GlobalRouteTransition"
  ) { activeRoute ->
    when (activeRoute) {
      ScreenRoute.WELCOME -> {
        WelcomeScreen(
          onNavigateToRegister = { navigateTo(ScreenRoute.REGISTER) },
          onNavigateToLogin = { navigateTo(ScreenRoute.LOGIN) },
          onNavigateToAbout = { navigateTo(ScreenRoute.ABOUT) }
        )
      }

      ScreenRoute.LOGIN -> {
        LoginScreen(
          authViewModel = authViewModel,
          onNavigateToRegister = { navigateTo(ScreenRoute.REGISTER) },
          onLoginSuccess = {
            backStack.clear()
            currentRoute = ScreenRoute.DASHBOARD
          },
          onNavigateBack = { popBackStack() }
        )
      }

      ScreenRoute.REGISTER -> {
        RegisterScreen(
          authViewModel = authViewModel,
          onNavigateToLogin = { navigateTo(ScreenRoute.LOGIN) },
          onRegisterSuccess = {
            backStack.clear()
            currentRoute = ScreenRoute.DASHBOARD
          },
          onNavigateBack = { popBackStack() }
        )
      }

      ScreenRoute.ABOUT -> {
        AboutAppScreen(
          cloudSyncManager = container.cloudSyncManager,
          onNavigateBack = { popBackStack() },
          onNavigateToTerms = { navigateTo(ScreenRoute.TERMS) }
        )
      }

      ScreenRoute.TERMS -> {
        TermsAndPrivacyScreen(
          onNavigateBack = { popBackStack() }
        )
      }

      ScreenRoute.CBT_SESSION -> {
        val user = currentUser
        if (user != null) {
          CbtScreen(
            user = user,
            cbtViewModel = cbtViewModel,
            onExamSubmitted = { attemptId ->
              currentAttemptId = attemptId
              navigateTo(ScreenRoute.RESULTS)
            },
            onNavigateBack = { popBackStack() },
            onNavigateToSubscription = { navigateTo(ScreenRoute.SUBSCRIPTION) }
          )
        }
      }

      ScreenRoute.RESULTS -> {
        ResultsScreen(
          attemptId = currentAttemptId,
          resultsViewModel = resultsViewModel,
          onNavigateToDashboard = { navigateTo(ScreenRoute.DASHBOARD) },
          onPracticeAgain = {
            val user = currentUser
            if (user != null) {
              cbtViewModel.startPractice(1L, user)
              navigateTo(ScreenRoute.CBT_SESSION)
            }
          }
        )
      }

      ScreenRoute.ADMIN -> {
        val user = currentUser
        if (user != null && user.role == "ADMIN") {
          AdminDashboardScreen(
            user = user,
            adminViewModel = adminViewModel,
            onNavigateBack = { popBackStack() }
          )
        } else {
          navigateTo(ScreenRoute.DASHBOARD)
        }
      }

      else -> {
        // Main authenticated app frame with modern bottom pill bar
        val user = currentUser
        if (user != null) {
          MainAuthenticatedScaffold(
            user = user,
            userProfilePreferences = container.userProfilePreferences,
            currentRoute = activeRoute,
            onNavigate = { route -> navigateTo(route) },
            onLogout = {
              authViewModel.logout()
              backStack.clear()
              currentRoute = ScreenRoute.WELCOME
            },
            content = {
              when (activeRoute) {
                ScreenRoute.DASHBOARD -> DashboardScreen(
                  user = user,
                  dashboardViewModel = dashboardViewModel,
                  onStartPractice = { courseId ->
                    cbtViewModel.startPractice(courseId, user)
                    navigateTo(ScreenRoute.CBT_SESSION)
                  },
                  onStartExam = { exam ->
                    cbtViewModel.startMockExam(exam, user)
                    navigateTo(ScreenRoute.CBT_SESSION)
                  },
                  onNavigateToMaterials = { navigateTo(ScreenRoute.MATERIALS) },
                  onNavigateToSubscription = { navigateTo(ScreenRoute.SUBSCRIPTION) },
                  onNavigateToResults = { attemptId ->
                    currentAttemptId = attemptId
                    navigateTo(ScreenRoute.RESULTS)
                  },
                  onNavigateToCourses = { navigateTo(ScreenRoute.COURSES) }
                )
                ScreenRoute.COURSES -> CoursesScreen(
                  user = user,
                  academicRepository = container.academicRepository,
                  cloudSyncManager = container.cloudSyncManager,
                  onNavigateBack = { popBackStack() },
                  onStartCoursePractice = { courseId ->
                    cbtViewModel.startPractice(courseId, user)
                    navigateTo(ScreenRoute.CBT_SESSION)
                  }
                )
                ScreenRoute.MATERIALS -> StudyMaterialsScreen(
                  user = user,
                  academicRepository = container.academicRepository,
                  cloudSyncManager = container.cloudSyncManager,
                  onNavigateBack = { popBackStack() },
                  onNavigateToSubscription = { navigateTo(ScreenRoute.SUBSCRIPTION) }
                )
                ScreenRoute.PROGRESS -> ProgressScreen(
                  user = user,
                  cbtRepository = container.cbtRepository,
                  authViewModel = authViewModel,
                  userProfilePreferences = container.userProfilePreferences,
                  cloudSyncManager = container.cloudSyncManager,
                  onNavigateBack = { popBackStack() },
                  onPracticeCourse = { courseId ->
                    cbtViewModel.startPractice(courseId, user)
                    navigateTo(ScreenRoute.CBT_SESSION)
                  },
                  onNavigateToResults = { attemptId ->
                    currentAttemptId = attemptId
                    navigateTo(ScreenRoute.RESULTS)
                  },
                  onNavigateToAbout = { navigateTo(ScreenRoute.ABOUT) }
                )
                ScreenRoute.SUBSCRIPTION -> SubscriptionScreen(
                  user = user,
                  subscriptionViewModel = subscriptionViewModel,
                  onNavigateBack = { popBackStack() }
                )
                else -> {}
              }
            }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAuthenticatedScaffold(
  user: UserEntity,
  userProfilePreferences: com.example.data.repository.UserProfilePreferences,
  currentRoute: ScreenRoute,
  onNavigate: (ScreenRoute) -> Unit,
  onLogout: () -> Unit,
  content: @Composable () -> Unit
) {
  val profilePhotoUri by userProfilePreferences.profilePhotoUri.collectAsState()

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isTablet = maxWidth >= 600.dp

    if (isTablet) {
      Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          header = {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(vertical = 16.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = WarningAmber.copy(alpha = 0.2f),
                modifier = Modifier.size(46.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Image(
                    painter = painterResource(id = R.drawable.medrad_official_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                  )
                }
              }
            }
          }
        ) {
          NavigationRailItem(
            selected = currentRoute == ScreenRoute.DASHBOARD,
            onClick = { onNavigate(ScreenRoute.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Home", fontSize = 10.sp) },
            colors = railItemColors()
          )
          NavigationRailItem(
            selected = currentRoute == ScreenRoute.COURSES,
            onClick = { onNavigate(ScreenRoute.COURSES) },
            icon = { Icon(Icons.Default.School, contentDescription = "Courses") },
            label = { Text("Courses", fontSize = 10.sp) },
            colors = railItemColors()
          )
          NavigationRailItem(
            selected = currentRoute == ScreenRoute.MATERIALS,
            onClick = { onNavigate(ScreenRoute.MATERIALS) },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Materials") },
            label = { Text("Notes", fontSize = 10.sp) },
            colors = railItemColors()
          )
          NavigationRailItem(
            selected = currentRoute == ScreenRoute.PROGRESS,
            onClick = { onNavigate(ScreenRoute.PROGRESS) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 10.sp) },
            colors = railItemColors()
          )
          NavigationRailItem(
            selected = currentRoute == ScreenRoute.SUBSCRIPTION,
            onClick = { onNavigate(ScreenRoute.SUBSCRIPTION) },
            icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium") },
            label = { Text("Premium", fontSize = 10.sp) },
            colors = railItemColors()
          )
          if (user.role == "ADMIN") {
            NavigationRailItem(
              selected = currentRoute == ScreenRoute.ADMIN,
              onClick = { onNavigate(ScreenRoute.ADMIN) },
              icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
              label = { Text("Admin", fontSize = 10.sp) },
              colors = railItemColors()
            )
          }

          Spacer(modifier = Modifier.weight(1f))

          IconButton(
            onClick = { onNavigate(ScreenRoute.ABOUT) }
          ) {
            Icon(Icons.Outlined.Info, contentDescription = "About App", tint = MaterialTheme.colorScheme.onSurface)
          }

          IconButton(
            onClick = onLogout,
            modifier = Modifier.padding(bottom = 16.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF4444))
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          content()
        }
      }
    } else {
      // Mobile Layout: Integrated Clean Header + Content + Modern Floating Dock (Adapts to Light & Dark)
      Scaffold(
        topBar = {
          Surface(
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              // Left: User Avatar + Title & Subtitle (Clickable to jump to Profile)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .weight(1f)
                  .clickable { onNavigate(ScreenRoute.PROGRESS) }
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  border = BorderStroke(1.5.dp, WarningAmber),
                  modifier = Modifier.size(38.dp)
                ) {
                  if (profilePhotoUri != null) {
                    AsyncImage(
                      model = profilePhotoUri,
                      contentDescription = "Profile",
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
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = if (initials.isNotEmpty()) initials else "MR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                  Text(
                    text = "MedRad Study App",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                  )
                  Text(
                    text = "${user.fullName} • ${user.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              // Right: Action Icons
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (user.role == "ADMIN") {
                  IconButton(
                    onClick = { onNavigate(ScreenRoute.ADMIN) },
                    modifier = Modifier.testTag("admin_portal_icon_button")
                  ) {
                    Icon(
                      Icons.Default.AdminPanelSettings,
                      contentDescription = "Admin Portal",
                      tint = WarningAmber
                    )
                  }
                }

                IconButton(
                  onClick = { onNavigate(ScreenRoute.ABOUT) },
                  modifier = Modifier.testTag("topbar_about_button")
                ) {
                  Icon(
                    Icons.Outlined.Info,
                    contentDescription = "About App",
                    tint = MaterialTheme.colorScheme.onSurface
                  )
                }

                IconButton(onClick = onLogout) {
                  Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color(0xFFEF4444)
                  )
                }
              }
            }
          }
        },
        bottomBar = {
          // Floating Adaptive Dock Bar (No dark/brown heavy block; adapts cleanly to light and dark)
          Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            shadowElevation = 8.dp,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .navigationBarsPadding()
              .testTag("modern_floating_nav_bar")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceAround,
              verticalAlignment = Alignment.CenterVertically
            ) {
              NavPillItem(
                selected = currentRoute == ScreenRoute.DASHBOARD,
                icon = Icons.Default.Dashboard,
                label = "Home",
                onClick = { onNavigate(ScreenRoute.DASHBOARD) }
              )
              NavPillItem(
                selected = currentRoute == ScreenRoute.COURSES,
                icon = Icons.Default.School,
                label = "Courses",
                onClick = { onNavigate(ScreenRoute.COURSES) }
              )
              NavPillItem(
                selected = currentRoute == ScreenRoute.MATERIALS,
                icon = Icons.Default.MenuBook,
                label = "Notes",
                onClick = { onNavigate(ScreenRoute.MATERIALS) }
              )
              NavPillItem(
                selected = currentRoute == ScreenRoute.PROGRESS,
                icon = Icons.Default.Person,
                label = "Profile",
                onClick = { onNavigate(ScreenRoute.PROGRESS) }
              )
              NavPillItem(
                selected = currentRoute == ScreenRoute.SUBSCRIPTION,
                icon = Icons.Default.WorkspacePremium,
                label = "Premium",
                onClick = { onNavigate(ScreenRoute.SUBSCRIPTION) }
              )
            }
          }
        }
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          content()
        }
      }
    }
  }
}

@Composable
private fun NavPillItem(
  selected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
    modifier = Modifier.height(44.dp)
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = if (selected) 12.dp else 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
      if (selected) {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
    }
  }
}

@Composable
fun railItemColors() = NavigationRailItemDefaults.colors(
  selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
  selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
  indicatorColor = MaterialTheme.colorScheme.primaryContainer
)
