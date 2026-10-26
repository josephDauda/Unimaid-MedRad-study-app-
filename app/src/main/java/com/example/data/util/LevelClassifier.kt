package com.example.data.util

import com.example.data.local.entity.UserEntity

object LevelClassifier {

  /**
   * Normalizes any academic level representation to a standardized format:
   * e.g. "100", "100L", "Level 100", "1" -> "100 Level"
   *      "200", "200L", "Level 200", "2" -> "200 Level"
   *      "300", "300L", "Level 300", "3" -> "300 Level"
   *      "400", "400L", "Level 400", "4" -> "400 Level"
   *      "500", "500L", "Level 500", "5" -> "500 Level"
   */
  fun normalizeLevel(raw: String?): String {
    if (raw.isNullOrBlank()) return "200 Level"
    val clean = raw.trim()
    val match = Regex("""([1-9])\d{2}""").find(clean)
      ?: Regex("""\b([1-9])\b""").find(clean)
    val digit = match?.groupValues?.getOrNull(1) ?: return if (clean.contains("1")) "100 Level" else "200 Level"
    return "${digit}00 Level"
  }

  /**
   * Determines academic level from a Course Code.
   * e.g. "BIO 101" -> "100 Level"
   *      "MTH 101" -> "100 Level"
   *      "ANA 201" -> "200 Level"
   *      "PHS 201" -> "200 Level"
   *      "RAD 301" -> "300 Level"
   *      "RAD 401" -> "400 Level"
   *      "RAD 501" -> "500 Level"
   *      "RAD 601" -> "600 Level"
   *      "RAD 701" -> "700 Level"
   */
  fun getLevelFromCourseCode(courseCode: String?): String {
    if (courseCode.isNullOrBlank()) return "200 Level"
    val clean = courseCode.trim().uppercase()
    val match = Regex("""([1-9])\d{2}""").find(clean)
    if (match != null) {
      val firstDigit = match.groupValues[1]
      return "${firstDigit}00 Level"
    }
    for (i in 1..7) {
      if (clean.contains("${i}00") || clean.contains("LEVEL $i") || clean.contains("${i}L")) {
        return "${i}00 Level"
      }
    }
    return "200 Level"
  }

  /**
   * Checks if an item (course, question, material, exam) matches the user's level.
   * Primary admins match all levels.
   * Secondary admins match only their assigned level.
   * Students match only their registered level.
   */
  fun matchesUserLevel(
    user: UserEntity?,
    courseCode: String?,
    explicitLevel: String? = null
  ): Boolean {
    if (user == null) return true
    if (user.role == "ADMIN") {
      val isPrimary = user.adminRole.equals("PRIMARY", ignoreCase = true) ||
          user.email.equals("admin@medrad.edu.ng", ignoreCase = true) ||
          user.adminRole.isBlank()
      if (isPrimary) return true

      // Secondary Admin
      val assigned = user.assignedCourses.ifBlank { user.level }
      if (assigned.isBlank() || assigned.equals("ALL", ignoreCase = true)) return true
      val assignedLevel = normalizeLevel(assigned)
      val targetLevel = explicitLevel?.let { normalizeLevel(it) } ?: getLevelFromCourseCode(courseCode)
      return assignedLevel == targetLevel
    }

    // Student: strictly match their registered level
    val studentLevel = normalizeLevel(user.level)
    val targetLevel = explicitLevel?.let { normalizeLevel(it) } ?: getLevelFromCourseCode(courseCode)
    return studentLevel == targetLevel
  }

  /**
   * Checks if an item level matches a target level string (e.g. "100 Level" vs "200 Level")
   */
  fun matchesLevel(targetLevel: String?, itemCodeOrLevel: String?): Boolean {
    if (targetLevel.isNullOrBlank() || targetLevel.equals("All", ignoreCase = true) || targetLevel.equals("All Levels", ignoreCase = true)) return true
    val normTarget = normalizeLevel(targetLevel)
    val normItem = if (itemCodeOrLevel != null && itemCodeOrLevel.contains("Level", ignoreCase = true)) {
      normalizeLevel(itemCodeOrLevel)
    } else {
      getLevelFromCourseCode(itemCodeOrLevel)
    }
    return normTarget == normItem
  }

  /**
   * Extracts level integer (e.g. 100, 200, 300)
   */
  fun getLevelInt(levelStr: String?): Int {
    val norm = normalizeLevel(levelStr)
    return norm.substring(0, 3).toIntOrNull() ?: 200
  }
}
