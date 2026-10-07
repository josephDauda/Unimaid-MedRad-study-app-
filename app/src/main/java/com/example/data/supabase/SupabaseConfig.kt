package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

object SupabaseConfig {
  const val PROJECT_ID = "zvmiquntlxmiotlfpumc"
  const val DEFAULT_SUPABASE_URL = "https://zvmiquntlxmiotlfpumc.supabase.co"
  // Default public anon publishable key for project zvmiquntlxmiotlfpumc
  const val DEFAULT_PUBLISHABLE_KEY = "sb_publishable_-aKCMGYwl5T91Dkkzi4R6w_kJ2uqxrC"

  private const val PREFS_NAME = "medrad_supabase_prefs"
  private const val KEY_URL = "supabase_url"
  private const val KEY_KEY = "supabase_publishable_key"

  fun getUrl(context: Context): String {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_URL, null)
    if (!saved.isNullOrBlank() && !saved.contains("placeholder")) return saved

    return try {
      val field = BuildConfig::class.java.getField("SUPABASE_URL")
      val buildVal = field.get(null) as? String
      if (!buildVal.isNullOrBlank() && !buildVal.contains("placeholder")) buildVal else DEFAULT_SUPABASE_URL
    } catch (_: Exception) {
      DEFAULT_SUPABASE_URL
    }
  }

  fun setUrl(context: Context, url: String) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_URL, url.trim().trimEnd('/')).apply()
  }

  fun getPublishableKey(context: Context): String {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_KEY, null)
    if (!saved.isNullOrBlank() && !saved.contains("placeholder") && !saved.endsWith(".placeholder") && (saved.startsWith("sb_publishable_") || saved.startsWith("eyJ"))) {
      return saved
    }

    return try {
      val field = BuildConfig::class.java.getField("SUPABASE_PUBLISHABLE_KEY")
      val buildVal = field.get(null) as? String
      if (!buildVal.isNullOrBlank() && !buildVal.contains("placeholder") && !buildVal.endsWith(".placeholder")) {
        buildVal
      } else {
        DEFAULT_PUBLISHABLE_KEY
      }
    } catch (_: Exception) {
      DEFAULT_PUBLISHABLE_KEY
    }
  }

  fun setPublishableKey(context: Context, key: String) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_KEY, key.trim()).apply()
  }
}
