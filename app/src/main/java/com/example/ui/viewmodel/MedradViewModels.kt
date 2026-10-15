package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.ExamAttemptEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.StudyMaterialEntity
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.AcademicRepository
import com.example.data.repository.AdminDashboardStats
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CbtRepository
import com.example.data.repository.SubscriptionPlan
import com.example.data.repository.SubscriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface UiState<out T> {
  data object Idle : UiState<Nothing>
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class AuthViewModel(
  private val authRepository: AuthRepository
) : ViewModel() {
  val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private val _authActionState = MutableStateFlow<UiState<UserEntity>>(UiState.Idle)
  val authActionState: StateFlow<UiState<UserEntity>> = _authActionState.asStateFlow()

  private val _resetPasswordState = MutableStateFlow<UiState<String>>(UiState.Idle)
  val resetPasswordState: StateFlow<UiState<String>> = _resetPasswordState.asStateFlow()

  init {
    viewModelScope.launch {
      authRepository.checkExistingSession()
    }
  }

  fun login(email: String, pass: String) {
    if (email.isBlank() || pass.isBlank()) {
      _authActionState.value = UiState.Error("Please enter both email and password.")
      return
    }
    viewModelScope.launch {
      _authActionState.value = UiState.Loading
      val result = authRepository.login(email, pass)
      result.fold(
        onSuccess = { user -> _authActionState.value = UiState.Success(user) },
        onFailure = { err -> _authActionState.value = UiState.Error(err.message ?: "Authentication failed") }
      )
    }
  }

  fun startGoogleOAuth(context: android.content.Context) {
    try {
      _authActionState.value = UiState.Loading
      val oauthUrl = authRepository.getGoogleOAuthUrl()
      val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(oauthUrl)).apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(browserIntent)
    } catch (e: Exception) {
      _authActionState.value = UiState.Error("Unable to open browser for Google authentication: ${e.message}")
    }
  }

  fun loginWithGoogle(email: String, fullName: String = "Student", idToken: String? = null) {
    if (email.isBlank() || !email.contains("@")) {
      _authActionState.value = UiState.Error("Please enter a valid Google/Gmail account.")
      return
    }
    viewModelScope.launch {
      _authActionState.value = UiState.Loading
      val result = authRepository.signInWithGoogle(email, fullName, idToken)
      result.fold(
        onSuccess = { user -> _authActionState.value = UiState.Success(user) },
        onFailure = { err -> _authActionState.value = UiState.Error(err.message ?: "Google authentication failed") }
      )
    }
  }

  fun register(
    fullName: String,
    email: String,
    phoneNumber: String = "",
    password: String,
    university: String = "University of Maiduguri",
    faculty: String = "College of Medical Sciences",
    department: String = "Medical Radiography",
    level: String = "100 Level",
    semester: String = "First Semester"
  ) {
    if (fullName.isBlank() || email.isBlank() || password.isBlank() || university.isBlank() || department.isBlank()) {
      _authActionState.value = UiState.Error("Please fill out all required academic registration fields.")
      return
    }
    viewModelScope.launch {
      _authActionState.value = UiState.Loading
      val result = authRepository.register(
        fullName = fullName,
        email = email,
        phoneNumber = phoneNumber,
        password = password,
        university = university,
        faculty = faculty,
        department = department,
        level = level,
        semester = semester
      )
      result.fold(
        onSuccess = { user -> _authActionState.value = UiState.Success(user) },
        onFailure = { err -> _authActionState.value = UiState.Error(err.message ?: "Registration failed") }
      )
    }
  }

  fun resetPassword(email: String, newPass: String = "MedRad123") {
    if (email.isBlank() || (newPass.isNotBlank() && newPass.length < 6)) {
      _resetPasswordState.value = UiState.Error("Enter valid email and new password (min 6 chars).")
      return
    }
    viewModelScope.launch {
      _resetPasswordState.value = UiState.Loading
      val res = authRepository.resetPassword(email, newPass)
      res.fold(
        onSuccess = { _resetPasswordState.value = UiState.Success("Password reset successfully. You can now login.") },
        onFailure = { _resetPasswordState.value = UiState.Error(it.message ?: "Password reset failed.") }
      )
    }
  }

  fun updateProfile(userId: Long, newFullName: String, newPhoneNumber: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = authRepository.updateProfile(userId, newFullName, newPhoneNumber)
      res.fold(
        onSuccess = { onResult(true, null) },
        onFailure = { onResult(false, it.message) }
      )
    }
  }

  fun changePassword(userId: Long, currentPass: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = authRepository.changePassword(userId, currentPass, newPass)
      res.fold(
        onSuccess = { onResult(true, null) },
        onFailure = { onResult(false, it.message) }
      )
    }
  }

  fun logout() {
    viewModelScope.launch {
      authRepository.logout()
      _authActionState.value = UiState.Idle
    }
  }

  fun clearState() {
    _authActionState.value = UiState.Idle
    _resetPasswordState.value = UiState.Idle
  }

  fun refreshUser() {
    viewModelScope.launch {
      authRepository.refreshCurrentUser()
    }
  }
}

class DashboardViewModel(
  private val cbtRepository: CbtRepository,
  private val academicRepository: AcademicRepository,
  private val subscriptionRepository: SubscriptionRepository,
  private val cloudSyncManager: com.example.data.sync.CloudSyncManager? = null
) : ViewModel() {
  private val _dailyAllowance = MutableStateFlow(15)
  val dailyAllowance: StateFlow<Int> = _dailyAllowance.asStateFlow()

  private val _recentAttempts = MutableStateFlow<List<ExamAttemptEntity>>(emptyList())
  val recentAttempts: StateFlow<List<ExamAttemptEntity>> = _recentAttempts.asStateFlow()

  val announcements = cloudSyncManager?.announcements ?: MutableStateFlow(emptyList())
  val isSyncing = cloudSyncManager?.isSyncing ?: MutableStateFlow(false)
  val syncStatusMessage = cloudSyncManager?.cloudStatusMessage ?: MutableStateFlow("Ready")

  val activeExams = academicRepository.getPublishedExams()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val courses = academicRepository.getAllCourses()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val dailySprintConfig = cloudSyncManager?.dailySprintConfig ?: MutableStateFlow(com.example.data.sync.DailyCbtSprintConfig())
  val practiceCardConfig = cloudSyncManager?.practiceCardConfig ?: MutableStateFlow(com.example.data.sync.PracticeCardConfig())
  val mockCardConfig = cloudSyncManager?.mockCardConfig ?: MutableStateFlow(com.example.data.sync.MockCardConfig())
  val globalCardStyle = cloudSyncManager?.globalCardStyle ?: MutableStateFlow("NAVY_GOLD")

  fun loadDashboard(user: UserEntity) {
    viewModelScope.launch {
      val remaining = cbtRepository.getRemainingAllowance(user)
      _dailyAllowance.value = remaining
      val attempts = cbtRepository.getUserAttemptsList(user.id, 5)
      _recentAttempts.value = attempts
    }
  }

  fun syncWithCloud(user: UserEntity, onComplete: (com.example.data.sync.SyncReport) -> Unit) {
    viewModelScope.launch {
      val report = cloudSyncManager?.pullLatestFromCloud()
        ?: com.example.data.sync.SyncReport(false, message = "Cloud sync unavailable")
      loadDashboard(user)
      onComplete(report)
    }
  }
}

data class CbtSessionState(
  val isExamMode: Boolean = false,
  val examId: Long? = null,
  val courseTitle: String = "RAD 101 — Introduction to Radiography",
  val examTitle: String = "Daily Practice",
  val questions: List<QuestionEntity> = emptyList(),
  val currentIndex: Int = 0,
  val userAnswers: Map<Int, String> = emptyMap(), // question index -> option "A", "B", "C", "D"
  val markedForReview: Set<Int> = emptySet(),
  val timeRemainingSeconds: Int = 0,
  val initialDurationSeconds: Int = 0,
  val dailyAllowanceRemaining: Int = 15,
  val isAllowanceExhausted: Boolean = false,
  val isSubmitted: Boolean = false,
  val submittedAttemptId: Long? = null,
  val isLoading: Boolean = false,
  val showSubmissionDialog: Boolean = false
)

class CbtViewModel(
  private val cbtRepository: CbtRepository,
  private val academicRepository: AcademicRepository? = null
) : ViewModel() {
  private val _sessionState = MutableStateFlow(CbtSessionState())
  val sessionState: StateFlow<CbtSessionState> = _sessionState.asStateFlow()

  private var timerJob: Job? = null

  fun startPractice(
    courseId: Long,
    user: UserEntity,
    questionCount: Int = 15,
    customTitle: String? = null,
    customCode: String? = null,
    specificQuestionIds: List<Long> = emptyList()
  ) {
    viewModelScope.launch {
      _sessionState.value = CbtSessionState(isLoading = true)
      val remaining = if (user.isPremium || user.role == "ADMIN") 9999 else cbtRepository.getRemainingAllowance(user)
      if (!user.isPremium && user.role != "ADMIN" && remaining <= 0) {
        _sessionState.value = CbtSessionState(
          isAllowanceExhausted = true,
          dailyAllowanceRemaining = 0,
          isLoading = false
        )
        return@launch
      }

      val course = academicRepository?.getCourseById(courseId)
      val code = if (!customCode.isNullOrBlank() && !customCode.contains(courseId.toString())) {
        customCode
      } else {
        course?.code ?: "RAD 101"
      }
      val title = if (!customTitle.isNullOrBlank() && !customTitle.contains(courseId.toString())) {
        customTitle
      } else {
        course?.title ?: "Medical Radiography"
      }

      val questions = if (specificQuestionIds.isNotEmpty()) {
        val specific = cbtRepository.getQuestionsByIds(specificQuestionIds)
        if (specific.isNotEmpty()) specific else cbtRepository.getPracticeQuestions(courseId, questionCount)
      } else {
        cbtRepository.getPracticeQuestions(courseId, questionCount)
      }

      val displayExamTitle = if (course != null) {
        "${course.code} — ${course.title}"
      } else if (!customTitle.isNullOrBlank() && !customCode.isNullOrBlank()) {
        "$customCode — $customTitle"
      } else {
        "$code — $title"
      }

      _sessionState.value = CbtSessionState(
        isExamMode = false,
        examTitle = displayExamTitle,
        courseTitle = "$code — $title",
        questions = questions,
        currentIndex = 0,
        dailyAllowanceRemaining = remaining,
        isLoading = false
      )
    }
  }

  fun startMockExam(exam: ExamEntity, user: UserEntity) {
    viewModelScope.launch {
      _sessionState.value = CbtSessionState(isLoading = true)
      val durationSecs = exam.durationMinutes * 60
      val questions = cbtRepository.getExamQuestions(exam.courseId, exam.totalQuestions)
      val course = academicRepository?.getCourseById(exam.courseId)
      val code = course?.code ?: "RAD 101"
      val title = course?.title ?: "Medical Radiography"

      val displayExamTitle = if (exam.title.isNotBlank()) exam.title else "$code — $title Mock Exam"

      _sessionState.value = CbtSessionState(
        isExamMode = true,
        examId = exam.id,
        examTitle = displayExamTitle,
        courseTitle = "$code — $title",
        questions = questions,
        currentIndex = 0,
        timeRemainingSeconds = durationSecs,
        initialDurationSeconds = durationSecs,
        dailyAllowanceRemaining = if (user.isPremium || user.role == "ADMIN") 9999 else cbtRepository.getRemainingAllowance(user),
        isLoading = false
      )

      startTimer(user)
    }
  }

  private fun startTimer(user: UserEntity) {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      while (_sessionState.value.timeRemainingSeconds > 0 && !_sessionState.value.isSubmitted) {
        delay(1000)
        val newTime = _sessionState.value.timeRemainingSeconds - 1
        _sessionState.value = _sessionState.value.copy(timeRemainingSeconds = newTime)
      }
      if (_sessionState.value.timeRemainingSeconds <= 0 && !_sessionState.value.isSubmitted) {
        // Auto submit when time reaches zero
        submitExam(user)
      }
    }
  }

  fun selectAnswer(option: String, user: UserEntity) {
    val state = _sessionState.value
    if (state.isSubmitted || state.questions.isEmpty()) return

    val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
    val alreadyAnsweredThisQ = state.userAnswers.containsKey(state.currentIndex)

    // Decrement allowance for free student practice if this question wasn't answered yet
    if (!state.isExamMode && !alreadyAnsweredThisQ && !user.isPremium && user.role != "ADMIN") {
      viewModelScope.launch {
        val remaining = cbtRepository.recordQuestionAnswered(user)
        val updatedAnswers = state.userAnswers + (state.currentIndex to option)
        _sessionState.value = state.copy(
          userAnswers = updatedAnswers,
          dailyAllowanceRemaining = remaining,
          isAllowanceExhausted = remaining <= 0
        )
      }
      return
    }

    val updatedAnswers = state.userAnswers + (state.currentIndex to option)
    _sessionState.value = state.copy(userAnswers = updatedAnswers)
  }

  fun toggleMarkForReview() {
    val state = _sessionState.value
    val cur = state.currentIndex
    val marked = state.markedForReview.toMutableSet()
    if (marked.contains(cur)) {
      marked.remove(cur)
    } else {
      marked.add(cur)
    }
    _sessionState.value = state.copy(markedForReview = marked)
  }

  fun navigateTo(index: Int) {
    val state = _sessionState.value
    if (index in state.questions.indices) {
      _sessionState.value = state.copy(currentIndex = index)
    }
  }

  fun nextQuestion() {
    val state = _sessionState.value
    if (state.currentIndex < state.questions.size - 1) {
      _sessionState.value = state.copy(currentIndex = state.currentIndex + 1)
    }
  }

  fun previousQuestion() {
    val state = _sessionState.value
    if (state.currentIndex > 0) {
      _sessionState.value = state.copy(currentIndex = state.currentIndex - 1)
    }
  }

  fun showSubmissionPrompt(show: Boolean) {
    _sessionState.value = _sessionState.value.copy(showSubmissionDialog = show)
  }

  fun submitExam(user: UserEntity) {
    timerJob?.cancel()
    val state = _sessionState.value
    if (state.isSubmitted || state.questions.isEmpty()) return

    viewModelScope.launch {
      var correctCount = 0
      var incorrectCount = 0
      var unansweredCount = 0
      val jsonAnswers = JSONObject()

      state.questions.forEachIndexed { index, question ->
        val chosen = state.userAnswers[index]
        val isCorrect = (chosen != null && chosen.equals(question.correctAnswer, ignoreCase = true))
        if (chosen == null) {
          unansweredCount++
        } else if (isCorrect) {
          correctCount++
        } else {
          incorrectCount++
        }

        val qObj = JSONObject()
        qObj.put("chosen", chosen ?: "")
        qObj.put("correct", question.correctAnswer)
        qObj.put("isCorrect", isCorrect)
        jsonAnswers.put(question.id.toString(), qObj)
      }

      val total = state.questions.size
      val scorePercentage = if (total > 0) (correctCount * 100) / total else 0
      val timeSpent = if (state.isExamMode) {
        state.initialDurationSeconds - state.timeRemainingSeconds
      } else {
        60
      }

      val attempt = ExamAttemptEntity(
        userId = user.id,
        examId = state.examId,
        courseTitle = state.courseTitle,
        examTitle = state.examTitle,
        score = scorePercentage,
        totalQuestions = total,
        correctAnswers = correctCount,
        incorrectAnswers = incorrectCount,
        unansweredQuestions = unansweredCount,
        timeSpentSeconds = if (timeSpent < 0) 0 else timeSpent,
        answersJson = jsonAnswers.toString()
      )

      val attemptId = cbtRepository.submitExamAttempt(attempt)

      _sessionState.value = state.copy(
        isSubmitted = true,
        submittedAttemptId = attemptId,
        showSubmissionDialog = false
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
  }
}

class ResultsViewModel(
  private val cbtRepository: CbtRepository,
  private val academicRepository: AcademicRepository
) : ViewModel() {
  private val _attempt = MutableStateFlow<ExamAttemptEntity?>(null)
  val attempt: StateFlow<ExamAttemptEntity?> = _attempt.asStateFlow()

  private val _questions = MutableStateFlow<List<QuestionEntity>>(emptyList())
  val questions: StateFlow<List<QuestionEntity>> = _questions.asStateFlow()

  fun loadAttempt(attemptId: Long) {
    viewModelScope.launch {
      val res = cbtRepository.getAttemptById(attemptId)
      _attempt.value = res
      if (res != null) {
        // Load questions for review
        val qList = cbtRepository.getPracticeQuestions(1L, 20)
        _questions.value = qList
      }
    }
  }
}

class SubscriptionViewModel(
  private val subscriptionRepository: SubscriptionRepository,
  private val authRepository: AuthRepository
) : ViewModel() {
  val availablePlans = subscriptionRepository.availablePlans

  private val _subscribeState = MutableStateFlow<UiState<SubscriptionEntity>>(UiState.Idle)
  val subscribeState: StateFlow<UiState<SubscriptionEntity>> = _subscribeState.asStateFlow()

  fun subscribe(
    user: UserEntity,
    plan: SubscriptionPlan,
    channel: String = "CARD",
    customReference: String? = null
  ) {
    viewModelScope.launch {
      _subscribeState.value = UiState.Loading
      val result = subscriptionRepository.processSubscription(user.id, plan, channel, customReference)
      result.fold(
        onSuccess = { sub ->
          _subscribeState.value = UiState.Success(sub)
          authRepository.refreshCurrentUser()
        },
        onFailure = { err ->
          _subscribeState.value = UiState.Error(err.message ?: "Subscription payment processing failed")
        }
      )
    }
  }

  fun resetState() {
    _subscribeState.value = UiState.Idle
  }
}

class AdminViewModel(
  private val adminRepository: AdminRepository,
  private val academicRepository: AcademicRepository,
  private val cloudSyncManager: com.example.data.sync.CloudSyncManager? = null
) : ViewModel() {
  private val _stats = MutableStateFlow(AdminDashboardStats())
  val stats: StateFlow<AdminDashboardStats> = _stats.asStateFlow()

  val isSyncing = cloudSyncManager?.isSyncing ?: MutableStateFlow(false)
  val lastSyncReport = cloudSyncManager?.lastSyncReport ?: MutableStateFlow(null)
  val cloudStatusMessage = cloudSyncManager?.cloudStatusMessage ?: MutableStateFlow("Ready")
  val announcements = cloudSyncManager?.announcements ?: MutableStateFlow(emptyList())

  val allQuestions = academicRepository.getAllQuestions()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allExams = academicRepository.getAllExams()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allMaterials = academicRepository.getAllMaterials()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allStudents = adminRepository.getAllStudents()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allSubscriptions = adminRepository.getAllSubscriptions()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allCourses = academicRepository.getAllCourses()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allAttempts = adminRepository.getAllExamAttempts()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allLibraries = academicRepository.getAllLibraries()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allAdmins = adminRepository.getAllAdmins()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val globalCardStyle = cloudSyncManager?.globalCardStyle ?: MutableStateFlow("NAVY_GOLD")
  val aboutAppContent = cloudSyncManager?.aboutAppContent ?: MutableStateFlow(com.example.data.sync.AboutAppContent())
  val studentFeedbackList = cloudSyncManager?.studentFeedbackList ?: MutableStateFlow(emptyList())
  val dailySprintConfig = cloudSyncManager?.dailySprintConfig ?: MutableStateFlow(com.example.data.sync.DailyCbtSprintConfig())
  val practiceCardConfig = cloudSyncManager?.practiceCardConfig ?: MutableStateFlow(com.example.data.sync.PracticeCardConfig())
  val mockCardConfig = cloudSyncManager?.mockCardConfig ?: MutableStateFlow(com.example.data.sync.MockCardConfig())

  fun updateDailySprint(config: com.example.data.sync.DailyCbtSprintConfig, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      cloudSyncManager?.updateDailySprintConfig(config)
      onDone()
    }
  }

  fun updatePracticeCard(config: com.example.data.sync.PracticeCardConfig, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      cloudSyncManager?.updatePracticeCardConfig(config)
      onDone()
    }
  }

  fun updateMockCard(config: com.example.data.sync.MockCardConfig, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      cloudSyncManager?.updateMockCardConfig(config)
      onDone()
    }
  }

  fun setGlobalCardStyle(style: String, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      cloudSyncManager?.setGlobalCardStyle(style)
      onDone()
    }
  }

  fun updateAboutAppManagement(content: com.example.data.sync.AboutAppContent, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      cloudSyncManager?.updateAboutAppManagement(content)
      onDone()
    }
  }

  fun sendPushNotification(title: String, message: String, target: String, priority: String, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      cloudSyncManager?.sendPushNotification(title, message, target, priority)
      onDone()
    }
  }

  fun exportStudentScores(courseTitle: String?, onResult: (String) -> Unit) {
    viewModelScope.launch {
      val report = cloudSyncManager?.exportStudentScores(courseTitle) ?: "No records found."
      onResult(report)
    }
  }

  fun loadStats() {
    viewModelScope.launch {
      _stats.value = adminRepository.getDashboardStats()
    }
  }

  fun createUniversity(name: String, shortName: String, onDone: () -> Unit) {
    viewModelScope.launch {
      adminRepository.addUniversity(name, shortName)
      loadStats()
      onDone()
    }
  }

  fun createFaculty(uniId: Long, name: String, onDone: () -> Unit) {
    viewModelScope.launch {
      adminRepository.addFaculty(uniId, name)
      loadStats()
      onDone()
    }
  }

  fun createDepartment(facId: Long, name: String, onDone: () -> Unit) {
    viewModelScope.launch {
      adminRepository.addDepartment(facId, name)
      loadStats()
      onDone()
    }
  }

  fun createCourse(deptId: Long, code: String, title: String, level: String, semester: String, desc: String, onDone: () -> Unit) {
    viewModelScope.launch {
      val id = adminRepository.addCourse(deptId, code, title, level, semester, desc)
      val newCourse = CourseEntity(
        id = id,
        departmentId = deptId,
        code = code,
        title = title,
        level = level,
        semester = semester,
        description = desc
      )
      cloudSyncManager?.broadcastCourse(newCourse)
      loadStats()
      onDone()
    }
  }

  fun deleteCourse(course: CourseEntity) {
    viewModelScope.launch {
      adminRepository.deleteCourse(course)
      cloudSyncManager?.deleteCourseCloud(course.id)
      loadStats()
    }
  }

  fun createTopic(courseId: Long, name: String, onDone: () -> Unit) {
    viewModelScope.launch {
      adminRepository.addTopic(courseId, name, 1)
      loadStats()
      onDone()
    }
  }

  fun createQuestion(
    courseId: Long,
    topicId: Long,
    text: String,
    optA: String,
    optB: String,
    optC: String,
    optD: String,
    correct: String,
    explanation: String,
    difficulty: String,
    isPremium: Boolean,
    questionType: String = "MULTIPLE_CHOICE",
    assessmentTitle: String = "General Practice",
    marks: Int = 1,
    timeLimitSeconds: Int = 60,
    publishTarget: String = "COURSE_BANK",
    onDone: () -> Unit
  ) {
    viewModelScope.launch {
      val q = QuestionEntity(
        courseId = courseId,
        topicId = topicId,
        questionText = text,
        optionA = optA,
        optionB = optB,
        optionC = optC,
        optionD = optD,
        correctAnswer = correct,
        explanation = explanation,
        difficulty = difficulty,
        isPremium = isPremium,
        isPublished = true,
        isDemo = false,
        questionType = questionType,
        assessmentTitle = assessmentTitle,
        marks = marks,
        timeLimitSeconds = timeLimitSeconds
      )
      val id = adminRepository.saveQuestion(q)
      val savedQuestion = q.copy(id = id)
      cloudSyncManager?.broadcastQuestion(savedQuestion)

      val course = academicRepository.getCourseById(courseId)
      val courseCode = course?.code ?: "RAD 101"
      val courseTitle = course?.title ?: "Medical Radiography"

      if (publishTarget == "DAILY_SPRINT" || publishTarget == "ALL_SECTIONS") {
        val curSprint = dailySprintConfig.value
        val updatedIds = (curSprint.questionIds + id).distinct()
        val updatedSprint = curSprint.copy(
          courseId = courseId,
          courseCode = courseCode,
          title = if (assessmentTitle.isNotBlank()) assessmentTitle else "$courseCode Daily CBT Sprint",
          subtitle = "${updatedIds.size} High-Yield Questions • $courseTitle",
          questionCount = updatedIds.size,
          questionIds = updatedIds
        )
        cloudSyncManager?.updateDailySprintConfig(updatedSprint)
      }

      if (publishTarget == "PRACTICE_CARD" || publishTarget == "ALL_SECTIONS") {
        val curPractice = practiceCardConfig.value
        val updatedIds = (curPractice.questionIds + id).distinct()
        val updatedPractice = curPractice.copy(
          isConfigured = true,
          courseId = courseId,
          courseCode = courseCode,
          courseTitle = if (assessmentTitle.isNotBlank()) assessmentTitle else courseTitle,
          questionCount = updatedIds.size,
          questionIds = updatedIds
        )
        cloudSyncManager?.updatePracticeCardConfig(updatedPractice)
      }

      if (publishTarget == "MOCK_EXAM" || publishTarget == "ALL_SECTIONS") {
        val curMock = mockCardConfig.value
        val updatedIds = (curMock.questionIds + id).distinct()
        val updatedMock = curMock.copy(
          isConfigured = true,
          courseId = courseId,
          courseCode = courseCode,
          courseTitle = courseTitle,
          examTitle = if (assessmentTitle.isNotBlank()) assessmentTitle else "$courseCode Mock CBT Exam",
          questionCount = updatedIds.size,
          durationMinutes = 30,
          questionIds = updatedIds
        )
        cloudSyncManager?.updateMockCardConfig(updatedMock)
      }

      loadStats()
      onDone()
    }
  }

  fun deleteQuestion(q: QuestionEntity) {
    viewModelScope.launch {
      adminRepository.deleteQuestion(q)
      cloudSyncManager?.deleteQuestionCloud(q.id)
      loadStats()
    }
  }

  fun clearAllQuestions() {
    viewModelScope.launch {
      adminRepository.clearAllQuestions()
      loadStats()
    }
  }

  fun createExam(
    courseId: Long,
    title: String,
    desc: String,
    durationMinutes: Int,
    questionCount: Int,
    isPremium: Boolean,
    onDone: () -> Unit
  ) {
    viewModelScope.launch {
      val exam = ExamEntity(
        courseId = courseId,
        title = title,
        description = desc,
        durationMinutes = durationMinutes,
        totalQuestions = questionCount,
        isPremium = isPremium,
        isPublished = true
      )
      val id = adminRepository.saveExam(exam)
      cloudSyncManager?.broadcastExam(exam.copy(id = id))
      loadStats()
      onDone()
    }
  }

  fun deleteExam(exam: ExamEntity) {
    viewModelScope.launch {
      adminRepository.deleteExam(exam)
      cloudSyncManager?.deleteExamCloud(exam.id)
      loadStats()
    }
  }

  fun createMaterial(
    courseId: Long,
    topicId: Long,
    title: String,
    desc: String,
    type: String,
    body: String,
    isPremium: Boolean,
    externalUrl: String? = null,
    onDone: () -> Unit
  ) {
    viewModelScope.launch {
      val mat = StudyMaterialEntity(
        courseId = courseId,
        topicId = topicId,
        title = title,
        description = desc,
        contentType = type,
        contentBody = body,
        externalUrl = externalUrl,
        isPremium = isPremium
      )
      val id = adminRepository.saveMaterial(mat)
      cloudSyncManager?.broadcastMaterial(mat.copy(id = id))
      loadStats()
      onDone()
    }
  }

  fun deleteMaterial(mat: StudyMaterialEntity) {
    viewModelScope.launch {
      adminRepository.deleteMaterial(mat)
      cloudSyncManager?.deleteMaterialCloud(mat.id)
      loadStats()
    }
  }

  // Google Drive & Course Libraries Management
  fun createLibrary(
    courseId: Long,
    courseCode: String,
    name: String,
    desc: String,
    url: String,
    onDone: () -> Unit
  ) {
    viewModelScope.launch {
      val lib = CourseLibraryEntity(
        courseId = courseId,
        courseCode = courseCode,
        name = name,
        description = desc,
        externalUrl = url,
        isPublished = true
      )
      val id = adminRepository.saveLibrary(lib)
      cloudSyncManager?.broadcastLibrary(lib.copy(id = id))
      loadStats()
      onDone()
    }
  }

  fun updateLibrary(lib: CourseLibraryEntity, onDone: () -> Unit) {
    viewModelScope.launch {
      adminRepository.updateLibrary(lib)
      cloudSyncManager?.broadcastLibrary(lib)
      loadStats()
      onDone()
    }
  }

  fun deleteLibrary(lib: CourseLibraryEntity) {
    viewModelScope.launch {
      adminRepository.deleteLibrary(lib)
      cloudSyncManager?.deleteLibraryCloud(lib.id)
      loadStats()
    }
  }

  // Primary & Secondary Admin Management
  fun addSecondaryAdmin(
    creator: UserEntity,
    fullName: String,
    email: String,
    phone: String,
    pass: String,
    permissions: String,
    assignedCourses: String,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = adminRepository.addSecondaryAdmin(creator, fullName, email, phone, pass, permissions, assignedCourses)
      res.fold(
        onSuccess = { onResult(true, null) },
        onFailure = { onResult(false, it.message) }
      )
    }
  }

  fun updateAdminPermissions(
    modifier: UserEntity,
    adminId: Long,
    permissions: String,
    assignedCourses: String,
    isActive: Boolean,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = adminRepository.updateAdminPermissions(modifier, adminId, permissions, assignedCourses, isActive)
      res.fold(
        onSuccess = { onResult(true, null) },
        onFailure = { onResult(false, it.message) }
      )
    }
  }

  fun removeAdmin(
    remover: UserEntity,
    adminId: Long,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = adminRepository.removeAdmin(remover, adminId)
      res.fold(
        onSuccess = { onResult(true, null) },
        onFailure = { onResult(false, it.message) }
      )
    }
  }

  // Live Cloud Synchronization & Push Methods
  fun pushAllToCloud(onComplete: (com.example.data.sync.SyncReport) -> Unit) {
    viewModelScope.launch {
      val report = cloudSyncManager?.pushAllContentToCloud()
        ?: com.example.data.sync.SyncReport(false, message = "Cloud sync manager unavailable")
      loadStats()
      onComplete(report)
    }
  }

  fun pullLatestFromCloud(onComplete: (com.example.data.sync.SyncReport) -> Unit) {
    viewModelScope.launch {
      val report = cloudSyncManager?.pullLatestFromCloud()
        ?: com.example.data.sync.SyncReport(false, message = "Cloud sync manager unavailable")
      loadStats()
      onComplete(report)
    }
  }

  fun testCloudConnection(onResult: (String) -> Unit) {
    viewModelScope.launch {
      val res = cloudSyncManager?.testSupabaseConnection() ?: "Cloud sync manager unavailable"
      onResult(res)
    }
  }

  fun broadcastAnnouncement(title: String, message: String, priority: String, onDone: () -> Unit) {
    viewModelScope.launch {
      cloudSyncManager?.broadcastAnnouncement(title, message, priority)
      onDone()
    }
  }

  fun exportQuestionsJson(onResult: (String) -> Unit) {
    viewModelScope.launch {
      val json = cloudSyncManager?.exportQuestionsToJson() ?: "{}"
      onResult(json)
    }
  }

  fun importQuestionsJson(json: String, onResult: (Result<Int>) -> Unit) {
    viewModelScope.launch {
      val result: Result<Int> = cloudSyncManager?.importQuestionsFromJson(json)
        ?: Result.failure(Exception("Cloud sync unavailable"))
      loadStats()
      onResult(result)
    }
  }

  fun getSupabaseProjectId(): String = cloudSyncManager?.getSupabaseProjectId() ?: "zvmiquntlxmiotlfpumc"
  fun getSupabaseUrl(): String = cloudSyncManager?.getSupabaseUrl() ?: "https://zvmiquntlxmiotlfpumc.supabase.co"
  fun getSupabaseKey(): String = cloudSyncManager?.getSupabaseKey() ?: ""
  fun updateSupabaseKey(key: String) { cloudSyncManager?.setSupabaseKey(key) }

  fun getFirebaseProjectId(): String = cloudSyncManager?.getFirebaseProjectId() ?: "zvmiquntlxmiotlfpumc"
  fun updateFirebaseProjectId(id: String) { cloudSyncManager?.setSupabaseKey(id) }
}
