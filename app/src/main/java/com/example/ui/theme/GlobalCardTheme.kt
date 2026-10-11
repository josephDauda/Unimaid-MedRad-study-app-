package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class DepartmentCardTheme(
  val id: String,
  val name: String,
  val primaryColor: Color,
  val accentColor: Color,
  val gradientColors: List<Color>,
  val badgeBackground: Color,
  val badgeTextColor: Color
)

object GlobalCardThemes {
  val NAVY_GOLD = DepartmentCardTheme(
    id = "NAVY_GOLD",
    name = "Deep Navy & Gold (Official Faculty)",
    primaryColor = Color(0xFF0B2545),
    accentColor = Color(0xFFF59E0B),
    gradientColors = listOf(Color(0xFF0B2545), Color(0xFF133E68), Color(0xFF1D4ED8)),
    badgeBackground = Color(0xFFF59E0B).copy(alpha = 0.25f),
    badgeTextColor = Color(0xFFF59E0B)
  )

  val ROYAL_AZURE = DepartmentCardTheme(
    id = "ROYAL_AZURE",
    name = "Royal Azure Radiance",
    primaryColor = Color(0xFF1E3A8A),
    accentColor = Color(0xFF93C5FD),
    gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF3B82F6)),
    badgeBackground = Color(0xFF93C5FD).copy(alpha = 0.25f),
    badgeTextColor = Color(0xFF93C5FD)
  )

  val EMERALD_CLINICAL = DepartmentCardTheme(
    id = "EMERALD_CLINICAL",
    name = "Clinical Emerald Radiography",
    primaryColor = Color(0xFF064E3B),
    accentColor = Color(0xFF34D399),
    gradientColors = listOf(Color(0xFF064E3B), Color(0xFF0F766E), Color(0xFF0D9488)),
    badgeBackground = Color(0xFF34D399).copy(alpha = 0.25f),
    badgeTextColor = Color(0xFF34D399)
  )

  val ONYX_DARK = DepartmentCardTheme(
    id = "ONYX_DARK",
    name = "Onyx Diagnostic Midnight",
    primaryColor = Color(0xFF0F172A),
    accentColor = Color(0xFFFBBF24),
    gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)),
    badgeBackground = Color(0xFFFBBF24).copy(alpha = 0.25f),
    badgeTextColor = Color(0xFFFBBF24)
  )

  val TEAL_MINT = DepartmentCardTheme(
    id = "TEAL_MINT",
    name = "Teal Mint Hospital Core",
    primaryColor = Color(0xFF134E4A),
    accentColor = Color(0xFF5EEAD4),
    gradientColors = listOf(Color(0xFF134E4A), Color(0xFF0F766E), Color(0xFF14B8A6)),
    badgeBackground = Color(0xFF5EEAD4).copy(alpha = 0.25f),
    badgeTextColor = Color(0xFF5EEAD4)
  )

  val BURGUNDY = DepartmentCardTheme(
    id = "BURGUNDY",
    name = "Burgundy Royal College",
    primaryColor = Color(0xFF4A0404),
    accentColor = Color(0xFFFCA5A5),
    gradientColors = listOf(Color(0xFF4A0404), Color(0xFF7F1D1D), Color(0xFF991B1B)),
    badgeBackground = Color(0xFFFCA5A5).copy(alpha = 0.25f),
    badgeTextColor = Color(0xFFFCA5A5)
  )

  fun getThemeById(id: String?): DepartmentCardTheme {
    return when (id?.trim()?.uppercase()) {
      "ROYAL_AZURE" -> ROYAL_AZURE
      "EMERALD_CLINICAL" -> EMERALD_CLINICAL
      "ONYX_DARK" -> ONYX_DARK
      "TEAL_MINT" -> TEAL_MINT
      "BURGUNDY" -> BURGUNDY
      else -> NAVY_GOLD
    }
  }
}
