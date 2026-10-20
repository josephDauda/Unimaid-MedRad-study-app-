package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.ui.components.MedicalRadiographyEmblem
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun GoogleEmblem(modifier: Modifier = Modifier, size: Dp = 22.dp) {
  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(Color.White),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size * 0.9f)) {
      val w = this.size.width
      val h = this.size.height
      val strokeW = w * 0.20f

      // Red: top arc
      drawArc(
        color = Color(0xFFEA4335),
        startAngle = 195f,
        sweepAngle = 110f,
        useCenter = false,
        style = Stroke(width = strokeW)
      )
      // Yellow: bottom-left arc
      drawArc(
        color = Color(0xFFFBBC05),
        startAngle = 115f,
        sweepAngle = 80f,
        useCenter = false,
        style = Stroke(width = strokeW)
      )
      // Green: bottom arc
      drawArc(
        color = Color(0xFF34A853),
        startAngle = 30f,
        sweepAngle = 85f,
        useCenter = false,
        style = Stroke(width = strokeW)
      )
      // Blue: right arc
      drawArc(
        color = Color(0xFF4285F4),
        startAngle = -45f,
        sweepAngle = 75f,
        useCenter = false,
        style = Stroke(width = strokeW)
      )
      // Blue crossbar
      drawLine(
        color = Color(0xFF4285F4),
        start = Offset(w * 0.45f, h * 0.5f),
        end = Offset(w * 0.98f, h * 0.5f),
        strokeWidth = strokeW
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  authViewModel: AuthViewModel,
  onNavigateToRegister: () -> Unit,
  onLoginSuccess: (UserEntity) -> Unit,
  onNavigateBack: (() -> Unit)? = null
) {
  val context = LocalContext.current
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var showResetDialog by remember { mutableStateOf(false) }
  var showEmailPasswordFields by remember { mutableStateOf(false) }
  var showGoogleDialog by remember { mutableStateOf(false) }

  // Inline Validation States
  var emailError by remember { mutableStateOf<String?>(null) }
  var passwordError by remember { mutableStateOf<String?>(null) }

  val focusManager = LocalFocusManager.current
  val authState by authViewModel.authActionState.collectAsState()

  LaunchedEffect(authState) {
    if (authState is UiState.Success) {
      focusManager.clearFocus(force = true)
      onLoginSuccess((authState as UiState.Success<UserEntity>).data)
      authViewModel.clearState()
    }
  }

  if (onNavigateBack != null) {
    BackHandler { onNavigateBack() }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (onNavigateBack != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Start
        ) {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        }
      } else {
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Department Logo Slot
      MedicalRadiographyEmblem(
        size = 64.dp,
        showBorder = true,
        borderColor = WarningAmber
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Department of Medical Radiography",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        ),
        color = WarningAmber
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Student Academic Portal",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )

      Text(
        text = "Sign in to access your course CBT tests, lecture materials, and exam analytics",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Elevated Login Form Card (Adaptive width breakpoint)
      Card(
        modifier = Modifier
          .widthIn(max = 540.dp)
          .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
        ) {
          Text(
            text = "Sign In",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(18.dp))

          Text(
            text = "Enter your academic email or credentials to access your CBT tests and study packs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Email Field with Real Inline Validation
          OutlinedTextField(
            value = email,
            onValueChange = {
              email = it.trim()
              emailError = when {
                email.isEmpty() -> "Email address is required"
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid academic email address"
                else -> null
              }
            },
            label = { Text("Institutional / Student Email") },
            placeholder = { Text("e.g. student@medrad.edu.ng", color = TextMuted) },
            leadingIcon = {
              Icon(Icons.Outlined.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            isError = emailError != null,
            supportingText = {
              if (emailError != null) {
                Text(emailError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
              }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("email_input_field"),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedFieldColors()
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Password Field with Real Inline Validation
          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              passwordError = when {
                password.isEmpty() -> "Password is required"
                password.length < 6 -> "Password must be at least 6 characters"
                !password.any { ch -> ch.isDigit() } -> "Password must contain at least one number"
                else -> null
              }
            },
            label = { Text("Password") },
            leadingIcon = {
              Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                  contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null,
            supportingText = {
              if (passwordError != null) {
                Text(passwordError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
              }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
              validateAndSubmitLogin(email, password, authViewModel, { emailError = it }, { passwordError = it })
            }),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("password_input_field"),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedFieldColors()
          )

          // Global Server / Auth State Error Message
          if (authState is UiState.Error) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = (authState as UiState.Error).message,
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(
              onClick = { showResetDialog = true },
              modifier = Modifier.testTag("forgot_password_button")
            ) {
              Text(
                "Forgot password?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Submit Button
          Button(
            onClick = {
              validateAndSubmitLogin(email, password, authViewModel, { emailError = it }, { passwordError = it })
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("login_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = authState !is UiState.Loading
          ) {
            if (authState is UiState.Loading) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
              Text(
                text = "Sign In with Credentials",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
            Text(
              text = "  OR  ",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
          }

          Spacer(modifier = Modifier.height(14.dp))

          OutlinedButton(
            onClick = { showGoogleDialog = true },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("google_signin_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              GoogleEmblem(size = 20.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Switch to Register
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "Don't have an account yet?",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
          onClick = onNavigateToRegister,
          modifier = Modifier.testTag("navigate_to_register_button")
        ) {
          Text(
            "Create account",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  // Google Sign-In Selection Dialog
  if (showGoogleDialog) {
    var googleEmailInput by remember { mutableStateOf("") }
    var googleNameInput by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showGoogleDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          GoogleEmblem(size = 24.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            "Continue with Google",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      },
      text = {
        Column {
          Text(
            text = "Authenticate securely with your Google / Gmail account through Supabase Auth:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = {
              showGoogleDialog = false
              authViewModel.startGoogleOAuth(context)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Open Google OAuth in Browser")
          }

          Spacer(modifier = Modifier.height(14.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
            Text("  OR DIRECT GMAIL  ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
          }
          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = googleEmailInput,
            onValueChange = { googleEmailInput = it; dialogError = null },
            label = { Text("Gmail Address") },
            placeholder = { Text("e.g. yourname@gmail.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = googleNameInput,
            onValueChange = { googleNameInput = it },
            label = { Text("Your Full Name (Optional)") },
            placeholder = { Text("e.g. Joseph Dauda") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
          if (dialogError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(dialogError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (googleEmailInput.isBlank() || !googleEmailInput.contains("@")) {
              dialogError = "Please enter a valid Gmail address"
            } else {
              showGoogleDialog = false
              authViewModel.loginWithGoogle(
                email = googleEmailInput.trim(),
                fullName = googleNameInput.trim().ifBlank { "Radiography Scholar" }
              )
            }
          }
        ) {
          Text("Sign In")
        }
      },
      dismissButton = {
        TextButton(onClick = { showGoogleDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Password Reset Dialog
  if (showResetDialog) {
    PasswordResetModal(
      initialEmail = email,
      onDismiss = { showResetDialog = false },
      onSubmit = { resetEmail ->
        authViewModel.resetPassword(resetEmail)
        showResetDialog = false
      }
    )
  }
}

private fun validateAndSubmitLogin(
  email: String,
  pass: String,
  vm: AuthViewModel,
  onEmailError: (String) -> Unit,
  onPassError: (String) -> Unit
) {
  var valid = true
  if (email.isBlank()) {
    onEmailError("Email address is required")
    valid = false
  } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
    onEmailError("Invalid email format")
    valid = false
  }

  if (pass.isBlank()) {
    onPassError("Password is required")
    valid = false
  } else if (pass.length < 6) {
    onPassError("Password must be at least 6 characters")
    valid = false
  }

  if (valid) {
    vm.login(email, pass)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
  authViewModel: AuthViewModel,
  onNavigateToLogin: () -> Unit,
  onRegisterSuccess: (UserEntity) -> Unit,
  onNavigateBack: (() -> Unit)? = null
) {
  val context = LocalContext.current
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  val levels = listOf("100 Level", "200 Level", "300 Level", "400 Level", "500 Level")
  var selectedLevel by remember { mutableStateOf(levels.first()) }
  var levelMenuExpanded by remember { mutableStateOf(false) }

  val department = "Medical Radiography" // Default department per requirement

  // Validation errors
  var nameError by remember { mutableStateOf<String?>(null) }
  var emailError by remember { mutableStateOf<String?>(null) }
  var phoneError by remember { mutableStateOf<String?>(null) }
  var passwordError by remember { mutableStateOf<String?>(null) }
  var confirmPasswordError by remember { mutableStateOf<String?>(null) }

  val focusManager = LocalFocusManager.current
  val authState by authViewModel.authActionState.collectAsState()

  LaunchedEffect(authState) {
    if (authState is UiState.Success) {
      focusManager.clearFocus(force = true)
      onRegisterSuccess((authState as UiState.Success<UserEntity>).data)
      authViewModel.clearState()
    }
  }

  if (onNavigateBack != null) {
    BackHandler { onNavigateBack() }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (onNavigateBack != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Start
        ) {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        }
      } else {
        Spacer(modifier = Modifier.height(16.dp))
      }

      MedicalRadiographyEmblem(
        size = 56.dp,
        showBorder = true,
        borderColor = WarningAmber
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Create Student Profile",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )

      Text(
        text = "University of Maiduguri • Department of Medical Radiography",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Registration Card (Adaptive width breakpoint)
      Card(
        modifier = Modifier
          .widthIn(max = 540.dp)
          .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
        ) {
          Text(
            text = "Create your academic student profile to access CBT tests and learning resources.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Full Name with gray hint placeholder per instructions: e.g. Joseph Dauda
          OutlinedTextField(
            value = fullName,
            onValueChange = {
              fullName = it
              nameError = if (fullName.trim().length < 3) "Enter your full academic name" else null
            },
            label = { Text("Full Name") },
            placeholder = { Text("e.g. Joseph Dauda", color = TextMuted) },
            leadingIcon = {
              Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            isError = nameError != null,
            supportingText = {
              if (nameError != null) Text(nameError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_fullname_field"),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedFieldColors()
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Email
          OutlinedTextField(
            value = email,
            onValueChange = {
              email = it.trim()
              emailError = when {
                email.isEmpty() -> "Email address is required"
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address"
                else -> null
              }
            },
            label = { Text("Email Address") },
            placeholder = { Text("e.g. student@unimaid.edu.ng", color = TextMuted) },
            leadingIcon = {
              Icon(Icons.Outlined.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            isError = emailError != null,
            supportingText = {
              if (emailError != null) Text(emailError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().testTag("register_email_field"),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedFieldColors()
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Department & Level
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Department (Read only / fixed)
            OutlinedTextField(
              value = department,
              onValueChange = {},
              readOnly = true,
              label = { Text("Department") },
              modifier = Modifier.weight(1.3f),
              shape = RoundedCornerShape(12.dp),
              colors = outlinedFieldColors()
            )

            // Academic Level Dropdown
            ExposedDropdownMenuBox(
              expanded = levelMenuExpanded,
              onExpandedChange = { levelMenuExpanded = it },
              modifier = Modifier.weight(1f)
            ) {
              OutlinedTextField(
                value = selectedLevel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Level") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelMenuExpanded) },
                modifier = Modifier.menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedFieldColors()
              )
              ExposedDropdownMenu(
                expanded = levelMenuExpanded,
                onDismissRequest = { levelMenuExpanded = false }
              ) {
                levels.forEach { lvl ->
                  DropdownMenuItem(
                    text = { Text(lvl) },
                    onClick = {
                      selectedLevel = lvl
                      levelMenuExpanded = false
                    }
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Phone Number
          OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
              phoneNumber = it.trim()
              phoneError = if (phoneNumber.length < 8) "Enter valid mobile number" else null
            },
            label = { Text("Phone Number") },
            placeholder = { Text("+234 800 000 0000", color = TextMuted) },
            leadingIcon = {
              Icon(Icons.Outlined.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            isError = phoneError != null,
            supportingText = {
              if (phoneError != null) Text(phoneError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().testTag("register_phone_field"),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedFieldColors()
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Password
          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              passwordError = when {
                password.length < 6 -> "Minimum 6 characters required"
                !password.any { ch -> ch.isDigit() } -> "Must include at least 1 number"
                else -> null
              }
              if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                confirmPasswordError = "Passwords do not match"
              } else {
                confirmPasswordError = null
              }
            },
            label = { Text("Password") },
            leadingIcon = {
              Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                  contentDescription = null
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null,
            supportingText = {
              if (passwordError != null) Text(passwordError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag("register_password_field"),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedFieldColors()
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Confirm Password
          OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
              confirmPassword = it
              confirmPasswordError = if (confirmPassword != password) "Passwords do not match" else null
            },
            label = { Text("Confirm Password") },
            leadingIcon = {
              Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmPasswordError != null,
            supportingText = {
              if (confirmPasswordError != null) Text(confirmPasswordError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_field"),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedFieldColors()
          )

          if (authState is UiState.Error) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = (authState as UiState.Error).message,
              color = ErrorRed,
              style = MaterialTheme.typography.bodySmall
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = {
              var valid = true
              if (fullName.trim().length < 3) {
                nameError = "Enter your full name"
                valid = false
              }
              if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError = "Valid email required"
                valid = false
              }
              if (phoneNumber.length < 8) {
                phoneError = "Valid phone number required"
                valid = false
              }
              if (password.length < 6 || !password.any { it.isDigit() }) {
                passwordError = "Password must have 6+ chars and 1+ number"
                valid = false
              }
              if (confirmPassword != password) {
                confirmPasswordError = "Passwords do not match"
                valid = false
              }

              if (valid) {
                authViewModel.register(
                  fullName = fullName.trim(),
                  email = email.trim(),
                  phoneNumber = phoneNumber.trim(),
                  password = password,
                  university = "University of Maiduguri",
                  faculty = "College of Medical Sciences",
                  department = department,
                  level = selectedLevel,
                  semester = "First Semester"
                )
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("register_submit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = authState !is UiState.Loading
          ) {
            if (authState is UiState.Loading) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
              Text(
                text = "Complete Registration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Already registered?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onNavigateToLogin) {
          Text("Sign In here", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = MaterialTheme.colorScheme.primary,
  unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
  focusedLabelColor = MaterialTheme.colorScheme.primary,
  unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
  focusedTextColor = MaterialTheme.colorScheme.onSurface,
  unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
  focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
  unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
  cursorColor = MaterialTheme.colorScheme.primary,
  focusedContainerColor = MaterialTheme.colorScheme.surface,
  unfocusedContainerColor = MaterialTheme.colorScheme.surface
)

@Composable
fun PasswordResetModal(
  initialEmail: String,
  onDismiss: () -> Unit,
  onSubmit: (String) -> Unit
) {
  val focusManager = LocalFocusManager.current
  var resetEmail by remember { mutableStateOf(initialEmail) }
  var error by remember { mutableStateOf<String?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      focusManager.clearFocus(force = true)
    }
  }

  AlertDialog(
    onDismissRequest = {
      focusManager.clearFocus(force = true)
      onDismiss()
    },
    title = {
      Text("Reset Portal Password", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    },
    text = {
      Column {
        Text(
          "Enter your registered institutional email. A secure recovery link will be dispatched.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = resetEmail,
          onValueChange = { resetEmail = it },
          label = { Text("Email") },
          placeholder = { Text("student@medrad.edu.ng", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          colors = outlinedFieldColors()
        )
        if (error != null) {
          Text(error!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (resetEmail.contains("@")) {
            focusManager.clearFocus(force = true)
            onSubmit(resetEmail)
          } else {
            error = "Enter a valid email address"
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Text("Send Reset Link")
      }
    },
    dismissButton = {
      TextButton(onClick = {
        focusManager.clearFocus(force = true)
        onDismiss()
      }) { Text("Cancel") }
    }
  )
}
