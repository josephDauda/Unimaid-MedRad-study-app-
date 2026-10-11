package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class CardThemeOption(
  val id: String,
  val name: String,
  val description: String,
  val primaryColor: Color,
  val accentColor: Color,
  val gradientColors: List<Color>
)

val CARD_THEMES = listOf(
  CardThemeOption(
    id = "NAVY_GOLD",
    name = "Deep Navy & Gold (Official Faculty)",
    description = "Faculty of Medical Sciences signature theme. Deep navy with amber gold borders and caduceus crest.",
    primaryColor = Color(0xFF0B2545),
    accentColor = Color(0xFFF59E0B),
    gradientColors = listOf(Color(0xFF0B2545), Color(0xFF133E68))
  ),
  CardThemeOption(
    id = "ROYAL_AZURE",
    name = "Royal Azure Radiance",
    description = "Academic sapphire and crisp university cyan. Modern, energetic, high contrast.",
    primaryColor = Color(0xFF1D4ED8),
    accentColor = Color(0xFF93C5FD),
    gradientColors = listOf(Color(0xFF1D4ED8), Color(0xFF2563EB))
  ),
  CardThemeOption(
    id = "EMERALD_CLINICAL",
    name = "Clinical Emerald Radiography",
    description = "Hospital clinical department theme. Medical teal and jade with clear diagnostic aesthetic.",
    primaryColor = Color(0xFF0F766E),
    accentColor = Color(0xFF2DD4BF),
    gradientColors = listOf(Color(0xFF0F766E), Color(0xFF0D9488))
  ),
  CardThemeOption(
    id = "ONYX_DARK",
    name = "Onyx Diagnostic Midnight",
    description = "Darkroom / Radiographic viewing box theme. Charcoal slate with amber luminescence.",
    primaryColor = Color(0xFF0F172A),
    accentColor = Color(0xFFFBBF24),
    gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
  )
)

fun getCardTheme(id: String?): CardThemeOption {
  return CARD_THEMES.find { it.id.equals(id, ignoreCase = true) } ?: CARD_THEMES.first()
}
