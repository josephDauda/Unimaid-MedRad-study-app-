package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProfilePreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("medrad_profile_preferences", Context.MODE_PRIVATE)

  private val _profilePhotoUri = MutableStateFlow<String?>(prefs.getString("profile_photo_uri", null))
  val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

  private val _customDisplayName = MutableStateFlow<String?>(prefs.getString("custom_display_name", null))
  val customDisplayName: StateFlow<String?> = _customDisplayName.asStateFlow()

  private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("pref_notifications_enabled", true))
  val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

  private val _quizRemindersEnabled = MutableStateFlow(prefs.getBoolean("pref_quiz_reminders_enabled", true))
  val quizRemindersEnabled: StateFlow<Boolean> = _quizRemindersEnabled.asStateFlow()

  private val _soundEnabled = MutableStateFlow(prefs.getBoolean("pref_sound_enabled", true))
  val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

  fun setProfilePhotoUri(uri: String?) {
    _profilePhotoUri.value = uri
    if (uri != null) {
      prefs.edit().putString("profile_photo_uri", uri).apply()
    } else {
      prefs.edit().remove("profile_photo_uri").apply()
    }
  }

  fun setCustomDisplayName(name: String?) {
    val clean = name?.trim()?.takeIf { it.isNotEmpty() }
    _customDisplayName.value = clean
    if (clean != null) {
      prefs.edit().putString("custom_display_name", clean).apply()
    } else {
      prefs.edit().remove("custom_display_name").apply()
    }
  }

  fun setNotificationsEnabled(enabled: Boolean) {
    _notificationsEnabled.value = enabled
    prefs.edit().putBoolean("pref_notifications_enabled", enabled).apply()
  }

  fun setQuizRemindersEnabled(enabled: Boolean) {
    _quizRemindersEnabled.value = enabled
    prefs.edit().putBoolean("pref_quiz_reminders_enabled", enabled).apply()
  }

  fun setSoundEnabled(enabled: Boolean) {
    _soundEnabled.value = enabled
    prefs.edit().putBoolean("pref_sound_enabled", enabled).apply()
  }
}
