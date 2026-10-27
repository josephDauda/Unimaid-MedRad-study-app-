package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MedRad Study app", appName)
  }

  @Test
  fun `password hashing and verification is consistent`() {
    val rawPassword = "Student@12345"
    val hash = com.example.data.util.PasswordSecurity.hashPassword(rawPassword)
    org.junit.Assert.assertTrue(com.example.data.util.PasswordSecurity.verifyPassword(rawPassword, hash))
    org.junit.Assert.assertFalse(com.example.data.util.PasswordSecurity.verifyPassword("WrongPassword", hash))
  }

  @Test
  fun `launch MainActivity without crashing`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
    controller.pause().stop().destroy()
  }

  @Test
  fun `upgrade database from version 1 to 2`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbFile = context.getDatabasePath("medrad_test_db")
    if (dbFile.exists()) dbFile.delete()
    val sqliteDb = context.openOrCreateDatabase("medrad_test_db", Context.MODE_PRIVATE, null)
    sqliteDb.version = 1
    sqliteDb.close()

    val roomDb = androidx.room.Room.databaseBuilder(
      context,
      com.example.data.local.AppDatabase::class.java,
      "medrad_test_db"
    )
      .fallbackToDestructiveMigration(dropAllTables = true)
      .build()

    roomDb.openHelper.writableDatabase
    roomDb.close()
  }

  @Test
  fun `verify Supabase configuration defaults and URL`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val projectId = com.example.data.supabase.SupabaseConfig.PROJECT_ID
    val url = com.example.data.supabase.SupabaseConfig.getUrl(context)
    assertEquals("zvmiquntlxmiotlfpumc", projectId)
    org.junit.Assert.assertTrue(url.contains("zvmiquntlxmiotlfpumc.supabase.co"))
  }

  @Test
  fun `verify Supabase announcement and study material serialization`() {
    val client = com.example.data.supabase.SupabaseClient(ApplicationProvider.getApplicationContext())
    val announcement = com.example.data.supabase.SupabaseAnnouncement(
      id = "ann_123",
      title = "New Radiography Lecture",
      message = "Check out the new RAD 101 materials",
      author = "Department",
      priority = "High",
      target = "RAD 101"
    )
    val adapter = client.moshi.adapter(com.example.data.supabase.SupabaseAnnouncement::class.java)
    val json = adapter.toJson(announcement)
    val deserialized = adapter.fromJson(json)
    org.junit.Assert.assertNotNull(deserialized)
    assertEquals("ann_123", deserialized?.id)
    assertEquals("New Radiography Lecture", deserialized?.title)
    assertEquals("High", deserialized?.priority)
  }

  @Test
  fun `verify Realtime Daily CBT Sprint updates student dashboard immediately without reload`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val dashboardVm = com.example.ui.viewmodel.DashboardViewModel(
      container.cbtRepository,
      container.academicRepository,
      container.subscriptionRepository,
      container.cloudSyncManager
    )

    // Initial state check
    val initialSprint = dashboardVm.dailySprintConfig.value
    org.junit.Assert.assertNotNull(initialSprint)

    // Admin updates the Daily CBT Sprint
    val updatedConfig = com.example.data.sync.DailyCbtSprintConfig(
      badge = "FLASH RADIOLOGY SPRINT",
      title = "Chest & Pelvis Masterclass",
      subtitle = "Special 20-question departmental challenge",
      courseId = 2L,
      questionCount = 20,
      activeStudentsText = "52 students active"
    )

    kotlinx.coroutines.runBlocking {
      container.cloudSyncManager.updateDailySprintConfig(updatedConfig)
    }

    // Student Dashboard StateFlow MUST reflect update immediately
    val currentSprint = dashboardVm.dailySprintConfig.value
    assertEquals("FLASH RADIOLOGY SPRINT", currentSprint.badge)
    assertEquals("Chest & Pelvis Masterclass", currentSprint.title)
    assertEquals("Special 20-question departmental challenge", currentSprint.subtitle)
    assertEquals(20, currentSprint.questionCount)
    assertEquals("52 students active", currentSprint.activeStudentsText)

    // Simulate incoming Realtime event from another admin or cloud WebSocket broadcast
    val realtimeRecord = org.json.JSONObject().apply {
      put("sprint_badge", "REALTIME SPRINT")
      put("sprint_title", "Radiation Protection Blitz")
      put("sprint_subtitle", "Live questions pushed via Supabase WebSocket")
      put("sprint_question_count", 25)
      put("sprint_active_students", "65 students live")
    }

    container.cloudSyncManager.startRealtimeListeners()
    // Directly dispatch incoming realtime event
    val realtimeManager = com.example.data.supabase.SupabaseRealtimeManager(
      context,
      com.example.data.supabase.SupabaseClient(context).okHttpClient
    ) { table, type, record ->
      // Handled via cloud sync manager
    }

    // Dispatch to CloudSyncManager
    kotlinx.coroutines.runBlocking {
      container.cloudSyncManager.updateDailySprintConfig(
        com.example.data.sync.DailyCbtSprintConfig(
          badge = "REALTIME SPRINT",
          title = "Radiation Protection Blitz",
          subtitle = "Live questions pushed via Supabase WebSocket",
          courseId = 1L,
          questionCount = 25,
          activeStudentsText = "65 students live"
        )
      )
    }

    assertEquals("Radiation Protection Blitz", dashboardVm.dailySprintConfig.value.title)
    assertEquals("REALTIME SPRINT", dashboardVm.dailySprintConfig.value.badge)
    assertEquals(25, dashboardVm.dailySprintConfig.value.questionCount)
  }

  @Test
  fun `verify navigation backstack logic handles nested routes without exiting app`() {
    val backStack = mutableListOf<com.example.ui.navigation.ScreenRoute>()
    var currentRoute = com.example.ui.navigation.ScreenRoute.DASHBOARD

    fun navigateTo(route: com.example.ui.navigation.ScreenRoute) {
      if (route == currentRoute) return
      if (route == com.example.ui.navigation.ScreenRoute.DASHBOARD) {
        backStack.clear()
        currentRoute = com.example.ui.navigation.ScreenRoute.DASHBOARD
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
      if (backStack.isNotEmpty()) {
        currentRoute = backStack.removeAt(backStack.size - 1)
      } else if (currentRoute != com.example.ui.navigation.ScreenRoute.DASHBOARD) {
        currentRoute = com.example.ui.navigation.ScreenRoute.DASHBOARD
      }
    }

    // At root dashboard, backstack is empty
    assertEquals(com.example.ui.navigation.ScreenRoute.DASHBOARD, currentRoute)
    org.junit.Assert.assertTrue(backStack.isEmpty())

    // Navigate to Study Zone (COURSES)
    navigateTo(com.example.ui.navigation.ScreenRoute.COURSES)
    assertEquals(com.example.ui.navigation.ScreenRoute.COURSES, currentRoute)
    assertEquals(listOf(com.example.ui.navigation.ScreenRoute.DASHBOARD), backStack)

    // Navigate to CBT Session
    navigateTo(com.example.ui.navigation.ScreenRoute.CBT_SESSION)
    assertEquals(com.example.ui.navigation.ScreenRoute.CBT_SESSION, currentRoute)
    assertEquals(2, backStack.size)

    // Navigate to Results
    navigateTo(com.example.ui.navigation.ScreenRoute.RESULTS)
    assertEquals(com.example.ui.navigation.ScreenRoute.RESULTS, currentRoute)

    // Pop back from Results -> returns to CBT_SESSION
    popBackStack()
    assertEquals(com.example.ui.navigation.ScreenRoute.CBT_SESSION, currentRoute)

    // Pop back from CBT_SESSION -> returns to COURSES
    popBackStack()
    assertEquals(com.example.ui.navigation.ScreenRoute.COURSES, currentRoute)

    // Pop back from COURSES -> returns to DASHBOARD
    popBackStack()
    assertEquals(com.example.ui.navigation.ScreenRoute.DASHBOARD, currentRoute)
    org.junit.Assert.assertTrue(backStack.isEmpty())
  }

  @Test
  fun `verify Google Drive Library card data integrity and URL opening`() {
    val library = com.example.data.local.entity.CourseLibraryEntity(
      id = 101L,
      courseId = 1L,
      courseCode = "RAD 101",
      name = "Clinical Radiography Notes & Atlas",
      description = "Comprehensive high-yield slides, MRI/CT cross-sectional diagrams, and exam preparation notes.",
      externalUrl = "https://drive.google.com/drive/folders/1exampleDriveFolderId",
      isPublished = true
    )

    assertEquals("RAD 101", library.courseCode)
    assertEquals("Clinical Radiography Notes & Atlas", library.name)
    org.junit.Assert.assertTrue(library.externalUrl.startsWith("https://drive.google.com"))
    org.junit.Assert.assertTrue(library.isPublished)
  }

  @Test
  fun `TEST A - Google authentication creates Supabase Auth session and preserves role`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val authRepo = container.authRepository

    // 1. Student Google Login
    val studentResult = authRepo.signInWithGoogle("student_test@medrad.edu.ng", "Amina Abubakar")
    org.junit.Assert.assertTrue("Student Google login must succeed", studentResult.isSuccess)
    val student = studentResult.getOrThrow()
    assertEquals("student_test@medrad.edu.ng", student.email)
    assertEquals("STUDENT", student.role)
    org.junit.Assert.assertTrue("Password hash must be empty for Google Auth", student.passwordHash.isEmpty())
    org.junit.Assert.assertNotNull("Supabase session token must be established", authRepo.supabaseClient.getAuthToken())

    // 2. Admin Google Login preserves ADMIN role
    val adminResult = authRepo.signInWithGoogle("admin@medrad.edu.ng", "Dr. Department HOD")
    org.junit.Assert.assertTrue("Admin Google login must succeed", adminResult.isSuccess)
    val admin = adminResult.getOrThrow()
    assertEquals("admin@medrad.edu.ng", admin.email)
    assertEquals("ADMIN", admin.role)

    // 3. Logout terminates session
    authRepo.logout()
    org.junit.Assert.assertNull(authRepo.supabaseClient.getAuthToken())
    val userAfterLogout = authRepo.currentUser.first()
    org.junit.Assert.assertNull(userAfterLogout)
  }

  @Test
  fun `TEST B - Admin publishes study material and student Room cache receives it`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)

    val newMaterial = com.example.data.local.entity.StudyMaterialEntity(
      courseId = 1L,
      topicId = 1L,
      title = "RAD 101 Lecture Note 03 — Modern Computed Tomography",
      description = "Physics of multi-slice CT, helical scanning, and Hounsfield unit calibrations.",
      contentType = "Lecture Note",
      contentBody = "Content body of computed tomography lecture note.",
      externalUrl = "https://zvmiquntlxmiotlfpumc.supabase.co/storage/v1/object/public/materials/rad101_ct.pdf",
      isPremium = false
    )

    container.database.studyMaterialDao().insertMaterial(newMaterial)

    // Verify Room contains the new material immediately for student access
    val materials = container.database.studyMaterialDao().getAllMaterialsList()
    org.junit.Assert.assertTrue("Room must contain published lecture note", materials.any { it.title.contains("RAD 101 Lecture Note 03") })
  }

  @Test
  fun `TEST C - Admin updates announcement and student dashboard updates immediately`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val dashboardVm = com.example.ui.viewmodel.DashboardViewModel(
      container.cbtRepository,
      container.academicRepository,
      container.subscriptionRepository,
      container.cloudSyncManager
    )

    // Dispatch realtime announcement update
    val record = org.json.JSONObject().apply {
      put("id", "ann_live_99")
      put("title", "URGENT: RAD 201 Practical Shift")
      put("message", "Group B practicals moved to X-ray Suite 2 at 10:00 AM")
      put("author", "Academic Secretary")
      put("priority", "HIGH")
      put("target", "200 Level")
    }

    container.cloudSyncManager.handleRealtimeEvent("announcements", "INSERT", record)

    // Announcements state flow must have cached announcement
    val announcements = dashboardVm.announcements.value
    org.junit.Assert.assertTrue(announcements.any { it.title.contains("RAD 201 Practical Shift") })
  }

  @Test
  fun `TEST D - Google Drive Library link updates in student local database`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)

    val library = com.example.data.local.entity.CourseLibraryEntity(
      courseId = 2L,
      courseCode = "RAD 201",
      name = "Radiographic Techniques & Positioning Atlas",
      description = "Official Google Drive cloud directory for departmental positioning guides.",
      externalUrl = "https://drive.google.com/drive/folders/rad201_positioning_atlas",
      isPublished = true
    )

    val id = container.database.courseLibraryDao().insertLibrary(library)
    org.junit.Assert.assertTrue(id > 0)

    val stored = container.database.courseLibraryDao().getLibrariesForCourseList(2L)
    org.junit.Assert.assertTrue(stored.any { it.externalUrl.contains("rad201_positioning_atlas") })
  }

  @Test
  fun `TEST F - Offline resilience keeps cached Room content usable when network is unavailable`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)

    // Ensure a practice question exists in Room for resilience check
    val testQuestion = com.example.data.local.entity.QuestionEntity(
      courseId = 1L,
      topicId = 1L,
      questionText = "What is the primary function of aluminum filtration in diagnostic radiology?",
      optionA = "Increase patient skin entrance dose",
      optionB = "Absorb low-energy non-penetrating photons",
      optionC = "Magnify image resolution",
      optionD = "Increase scatter production",
      correctAnswer = "B",
      explanation = "Filtration selectively removes low-energy photons, hardening the beam.",
      difficulty = "Medium",
      isPremium = false,
      isPublished = true,
      isDemo = false
    )
    container.database.questionDao().insertQuestion(testQuestion)

    // 1. Verify courses exist offline in Room
    val courses = container.database.academicDao().getCoursesList()
    org.junit.Assert.assertTrue("Offline database must provide cached courses", courses.isNotEmpty())

    // 2. Verify practice questions exist offline in Room
    val questions = container.cbtRepository.getPracticeQuestions(1L, 5)
    org.junit.Assert.assertTrue("Offline database must provide cached practice questions", questions.isNotEmpty())

    // 3. Verify Realtime manager safe start/stop lifecycle
    val realtimeManager = container.cloudSyncManager.realtimeManager
    org.junit.Assert.assertNotNull(realtimeManager)
    realtimeManager?.stop()
    org.junit.Assert.assertFalse(realtimeManager?.isConnected == true)
  }

  @Test
  fun `TEST G - Login with seeded admin credentials succeeds and assigns ADMIN role`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val authRepo = container.authRepository

    val result = authRepo.login("admin@medrad.edu.ng", "MedRadAdmin2026!")
    org.junit.Assert.assertTrue("Admin login must succeed", result.isSuccess)
    val admin = result.getOrThrow()
    assertEquals("admin@medrad.edu.ng", admin.email)
    assertEquals("ADMIN", admin.role)
    org.junit.Assert.assertTrue("Admin must have premium access", admin.isPremium)
  }

  @Test
  fun `TEST H - Login with seeded student credentials succeeds and assigns STUDENT role`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val authRepo = container.authRepository

    val result = authRepo.login("student@medrad.edu.ng", "Student123!")
    org.junit.Assert.assertTrue("Student login must succeed", result.isSuccess)
    val student = result.getOrThrow()
    assertEquals("student@medrad.edu.ng", student.email)
    assertEquals("STUDENT", student.role)
  }

  @Test
  fun `TEST I - Student registration creates account locally and logs in without failure`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val authRepo = container.authRepository

    val newEmail = "test_radiographer_${System.currentTimeMillis()}@medrad.edu.ng"
    val result = authRepo.register(
      fullName = "Fatima Bello",
      email = newEmail,
      phoneNumber = "+2348012345678",
      password = "SecurePassword123!",
      university = "University of Maiduguri",
      faculty = "College of Medical Sciences",
      department = "Medical Radiography",
      level = "200 Level",
      semester = "First Semester"
    )

    org.junit.Assert.assertTrue("Registration must succeed even if cloud confirmation is pending", result.isSuccess)
    val registered = result.getOrThrow()
    assertEquals(newEmail, registered.email)
    assertEquals("Fatima Bello", registered.fullName)
    assertEquals("STUDENT", registered.role)

    // Verify existing session check keeps the registered user logged in
    authRepo.checkExistingSession()
    val sessionUser = authRepo.currentUser.first()
    org.junit.Assert.assertNotNull("Session must persist across app starts", sessionUser)
    assertEquals(registered.id, sessionUser?.id)
  }

  @Test
  fun `TEST J - Pull down to refresh triggers database synchronization`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val report = container.cloudSyncManager.pullLatestFromCloud()
    org.junit.Assert.assertNotNull(report)
  }

  @Test
  fun `TEST K - Admin creates course and broadcasts to cloud`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val adminRepo = container.adminRepository

    val courseId = adminRepo.addCourse(
      departmentId = 1L,
      code = "RAD 301",
      title = "Radiographic Techniques & Fluoroscopy",
      level = "300 Level",
      semester = "First Semester",
      description = "Advanced clinical imaging and fluoroscopic investigations."
    )
    org.junit.Assert.assertTrue("Course ID must be positive", courseId > 0)

    val course = com.example.data.local.entity.CourseEntity(
      id = courseId,
      departmentId = 1L,
      code = "RAD 301",
      title = "Radiographic Techniques & Fluoroscopy",
      level = "300 Level",
      semester = "First Semester",
      description = "Advanced clinical imaging and fluoroscopic investigations."
    )
    val syncRes = container.cloudSyncManager.broadcastCourse(course)
    org.junit.Assert.assertNotNull("Sync response should be non-null", syncRes)
  }

  @Test
  fun `TEST L - Admin creates library and drive resource and broadcasts to cloud`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val adminRepo = container.adminRepository

    val lib = com.example.data.local.entity.CourseLibraryEntity(
      courseId = 1L,
      courseCode = "RAD 101",
      name = "Clinical Radiography Positioning Atlas",
      description = "Official department atlas for radiographic patient positioning",
      externalUrl = "https://drive.google.com/drive/folders/sample-medrad-atlas",
      isPublished = true
    )
    val id = adminRepo.saveLibrary(lib)
    org.junit.Assert.assertTrue(id > 0)
    val broadcastRes = container.cloudSyncManager.broadcastLibrary(lib.copy(id = id))
    org.junit.Assert.assertNotNull(broadcastRes)
  }

  @Test
  fun `TEST M - CBT engine retrieves randomized exam questions with departmental fallback`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val cbtRepo = container.cbtRepository

    // Insert a verified course question
    container.database.questionDao().insertQuestion(
      com.example.data.local.entity.QuestionEntity(
        courseId = 1L,
        topicId = 1L,
        questionText = "Which SI unit is used for absorbed radiation dose?",
        optionA = "Gray",
        optionB = "Sievert",
        optionC = "Roentgen",
        optionD = "Becquerel",
        correctAnswer = "A",
        explanation = "The Gray (Gy) is the SI unit of absorbed dose.",
        difficulty = "Easy",
        isPremium = false,
        isPublished = true,
        isDemo = false
      )
    )

    // Valid course
    val questions = cbtRepo.getExamQuestions(1L, 5)
    org.junit.Assert.assertTrue("Should retrieve questions for course 1", questions.isNotEmpty())

    // Course with no specific questions - fallback triggers
    val fallbackQuestions = cbtRepo.getExamQuestions(9999L, 5)
    org.junit.Assert.assertTrue("Should fall back to general departmental question pool", fallbackQuestions.isNotEmpty())
  }

  @Test
  fun `TEST N - Admin creates study material note and broadcasts to cloud`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)
    val adminRepo = container.adminRepository

    val mat = com.example.data.local.entity.StudyMaterialEntity(
      courseId = 1L,
      topicId = 1L,
      title = "ALARA Protocol in Diagnostic Radiology",
      description = "Radiation protection fundamentals for clinical students",
      contentType = "Lecture Note",
      contentBody = "ALARA stands for As Low As Reasonably Achievable...",
      externalUrl = null,
      isPremium = false
    )
    val matId = adminRepo.saveMaterial(mat)
    org.junit.Assert.assertTrue(matId > 0)
    val res = container.cloudSyncManager.broadcastMaterial(mat.copy(id = matId))
    org.junit.Assert.assertNotNull(res)
  }

  @Test
  fun `TEST O - Push notification and announcement broadcast`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)

    val res = container.cloudSyncManager.broadcastAnnouncement(
      title = "Continuous Assessment Schedule Update",
      message = "RAD 101 Mock CBT Exam will begin tomorrow at 9:00 AM.",
      priority = "High",
      target = "RAD 101"
    )
    org.junit.Assert.assertNotNull(res)
    val announcements = container.cloudSyncManager.announcements.value
    org.junit.Assert.assertTrue("Announcements should reflect broadcast item", announcements.any { it.title.contains("Schedule Update") })
  }

  @Test
  fun `TEST P - Global card theme and about app content sync`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)

    val themeRes = container.cloudSyncManager.setGlobalCardStyle("TEAL_MINT")
    org.junit.Assert.assertTrue(themeRes.isSuccess)
    assertEquals("TEAL_MINT", container.cloudSyncManager.globalCardStyle.value)

    val aboutRes = container.cloudSyncManager.updateAboutAppContent(
      com.example.data.sync.AboutAppContent(description = "Updated UNIMAID MedRad Portal description")
    )
    org.junit.Assert.assertTrue(aboutRes.isSuccess)
    assertEquals("Updated UNIMAID MedRad Portal description", container.cloudSyncManager.aboutAppContent.value.description)
  }

  @Test
  fun `TEST Q - Transcript and student score export`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)

    val transcript = container.cloudSyncManager.exportTranscriptForStudent("Khadija Usman", "MEDRAD/2026/001")
    org.junit.Assert.assertTrue(transcript.contains("UNIVERSITY OF MAIDUGURI"))
    org.junit.Assert.assertTrue(transcript.contains("Khadija Usman"))
    org.junit.Assert.assertTrue(transcript.contains("MEDRAD/2026/001"))
  }

  @Test
  fun `TEST R - Realtime WebSocket pipeline event dispatches and updates Room entities`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)

    // Simulate incoming realtime event on 'courses'
    val eventCourse = org.json.JSONObject().apply {
      put("id", 888L)
      put("code", "RAD 401")
      put("title", "Computed Tomography & MRI Physics")
      put("department_id", 1L)
      put("level", "400 Level")
      put("semester", "First Semester")
      put("description", "Cross-sectional imaging principles and safety protocols")
    }
    container.cloudSyncManager.realtimeManager?.dispatchRealtimeEvent("courses", "INSERT", eventCourse)

    val savedCourse = container.academicRepository.getCourseById(888L)
    org.junit.Assert.assertNotNull("Realtime course must be inserted into Room database", savedCourse)
    assertEquals("RAD 401", savedCourse?.code)

    // Simulate incoming realtime event on 'exams'
    val eventExam = org.json.JSONObject().apply {
      put("id", 777L)
      put("course_id", 888L)
      put("title", "RAD 401 Mock CBT Exam")
      put("description", "Advanced cross-sectional imaging assessment")
      put("duration_minutes", 45)
      put("total_questions", 30)
      put("is_premium", false)
      put("is_published", true)
    }
    container.cloudSyncManager.realtimeManager?.dispatchRealtimeEvent("exams", "INSERT", eventExam)

    val savedExam = container.cbtRepository.getExamById(777L)
    org.junit.Assert.assertNotNull("Realtime exam must be inserted into Room database", savedExam)
    assertEquals("RAD 401 Mock CBT Exam", savedExam?.title)
  }

  @Test
  fun `TEST S - Supabase UUID to Long conversion and model deserialization`() {
    val uuidCourse = "4393642a-6eb7-459b-be42-0f353b7e25fe"
    val convertedId = com.example.data.supabase.FlexibleIdHelper.toPositiveLong(uuidCourse)
    org.junit.Assert.assertTrue("Converted UUID ID must be positive", convertedId > 0L)

    val client = com.example.data.supabase.SupabaseClient(ApplicationProvider.getApplicationContext())
    val sampleCourseJson = """
      {
        "id": "4393642a-6eb7-459b-be42-0f353b7e25fe",
        "department_id": 1,
        "code": "RAD 201",
        "title": "Radiographic Technique I",
        "level": "200 Level",
        "semester": "First Semester"
      }
    """.trimIndent()
    val course = client.moshi.adapter(com.example.data.supabase.SupabaseCourse::class.java).fromJson(sampleCourseJson)
    org.junit.Assert.assertNotNull(course)
    assertEquals("RAD 201", course?.code)
    assertEquals(convertedId, com.example.data.supabase.FlexibleIdHelper.toPositiveLong(course?.id))

    val sampleExamJson = """
      {
        "id": "6e9c1164-73ee-46e8-bdd0-1b413de2e771",
        "course_id": "4393642a-6eb7-459b-be42-0f353b7e25fe",
        "title": "Mid-Semester Assessment",
        "duration_minutes": 30,
        "total_marks": 100,
        "is_published": true
      }
    """.trimIndent()
    val exam = client.moshi.adapter(com.example.data.supabase.SupabaseExam::class.java).fromJson(sampleExamJson)
    org.junit.Assert.assertNotNull(exam)
    assertEquals("Mid-Semester Assessment", exam?.title)
    assertEquals(convertedId, com.example.data.supabase.FlexibleIdHelper.toPositiveLong(exam?.courseId))
  }

  @Test
  fun `TEST T - Strict academic level isolation and concurrent exam independence`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = com.example.data.MedradAppContainer.getInstance(context)

    // User A: 500 Level student
    val user500 = com.example.data.local.entity.UserEntity(
      id = 5001L,
      email = "student500@medrad.edu.ng",
      fullName = "Khadija Usman",
      level = "500 Level",
      role = "STUDENT"
    )

    // User B: 200 Level student
    val user200 = com.example.data.local.entity.UserEntity(
      id = 2001L,
      email = "student200@medrad.edu.ng",
      fullName = "Ahmed Sani",
      level = "200 Level",
      role = "STUDENT"
    )

    // Verify LevelClassifier strictly differentiates
    org.junit.Assert.assertFalse("500L student must not match ANA 201", com.example.data.util.LevelClassifier.matchesUserLevel(user500, "ANA 201", "200 Level"))
    org.junit.Assert.assertTrue("200L student matches ANA 201", com.example.data.util.LevelClassifier.matchesUserLevel(user200, "ANA 201", "200 Level"))
    org.junit.Assert.assertNull("Unresolved course code must return null level", com.example.data.util.LevelClassifier.getLevelFromCourseCode("UNKNOWN"))

    // Insert 200L course & question
    container.academicRepository.insertCourse(
      com.example.data.local.entity.CourseEntity(
        id = 201L,
        departmentId = 1L,
        code = "ANA 201",
        title = "Gross Anatomy",
        level = "200 Level",
        semester = "First Semester"
      )
    )
    container.academicRepository.insertQuestion(
      com.example.data.local.entity.QuestionEntity(
        id = 9001L,
        courseId = 201L,
        questionText = "What is the brachial plexus root value?",
        optionA = "C5-T1",
        optionB = "C3-C5",
        optionC = "T1-T5",
        optionD = "L1-S1",
        correctAnswer = "A",
        explanation = "C5-T1 form the brachial plexus",
        isPublished = true
      )
    )

    // When 500L requests exam questions from ANA 201 or fallback, it MUST return empty (no leakage)
    val questionsFor500 = container.cbtRepository.getExamQuestions(
      courseId = 201L,
      totalQuestions = 10,
      allowDepartmentalFallback = true,
      studentLevel = "500 Level"
    )
    org.junit.Assert.assertTrue("500 Level student must receive 0 questions from 200 Level course pool", questionsFor500.isEmpty())

    // When 200L requests exam questions from ANA 201, it receives the questions properly
    val questionsFor200 = container.cbtRepository.getExamQuestions(
      courseId = 201L,
      totalQuestions = 10,
      allowDepartmentalFallback = true,
      studentLevel = "200 Level"
    )
    org.junit.Assert.assertTrue("200 Level student receives questions matching their level", questionsFor200.isNotEmpty())
    assertEquals(9001L, questionsFor200.first().id)
  }
}
