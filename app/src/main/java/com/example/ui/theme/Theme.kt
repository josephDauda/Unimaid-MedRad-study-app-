package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
  LIGHT,
  DARK,
  SYSTEM
}

class ThemeController(context: Context) {
  private val prefs = context.getSharedPreferences("medrad_theme_prefs", Context.MODE_PRIVATE)
  private val _themeMode = MutableStateFlow(
    when (prefs.getString("theme_mode", "SYSTEM")) {
      "LIGHT" -> AppThemeMode.LIGHT
      "DARK" -> AppThemeMode.DARK
      else -> AppThemeMode.SYSTEM
    }
  )
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

  fun setThemeMode(mode: AppThemeMode) {
    _themeMode.value = mode
    prefs.edit().putString("theme_mode", mode.name).apply()
  }
}

val LocalThemeController = compositionLocalOf<ThemeController> {
  error("ThemeController not provided")
}

private val LightColorScheme = lightColorScheme(
  primary = AcademicNavy,
  onPrimary = Color.White,
  primaryContainer = ProfessionalBlueLight,
  onPrimaryContainer = AcademicNavy,
  secondary = MedicalTeal,
  onSecondary = Color.White,
  secondaryContainer = MedicalTealLight,
  onSecondaryContainer = MedicalTealDark,
  tertiary = WarningAmber,
  onTertiary = Color.Black,
  tertiaryContainer = WarningAmberLight,
  onTertiaryContainer = WarningAmberDark,
  background = BackgroundCoolLight,
  onBackground = TextPrimaryDarkSlate,
  surface = SurfaceWhite,
  onSurface = TextPrimaryDarkSlate,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = TextSecondarySlate,
  outline = BorderSubtle,
  outlineVariant = BorderMedium,
  error = ErrorRed,
  onError = Color.White,
  errorContainer = ErrorRedLight,
  onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF93C5FD), // Soft blue for dark mode
  onPrimary = Color(0xFF03162C),
  primaryContainer = Color(0xFF0B2545),
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = Color(0xFF2DD4BF), // Soft teal
  onSecondary = Color(0xFF022C22),
  secondaryContainer = Color(0xFF134E4A),
  onSecondaryContainer = Color(0xFFCCFBF1),
  tertiary = Color(0xFFFBBF24), // Amber
  onTertiary = Color(0xFF451A03),
  tertiaryContainer = Color(0xFF78350F),
  onTertiaryContainer = Color(0xFFFEF3C7),
  background = Color(0xFF0A111C),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF111C2E),
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = Color(0xFF1B2A44),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF2B3D5A),
  outlineVariant = Color(0xFF1E2D44),
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
  errorContainer = Color(0xFF7F1D1D),
  onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun MedradTheme(
  themeController: ThemeController? = null,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val controller = themeController ?: androidx.compose.runtime.remember { ThemeController(context) }
  val currentMode by controller.themeMode.collectAsState()

  val isDark = when (currentMode) {
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

  CompositionLocalProvider(LocalThemeController provides controller) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
