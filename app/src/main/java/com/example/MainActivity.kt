package com.example

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.MedradAppContainer
import com.example.ui.navigation.MedradNavHost
import com.example.ui.theme.MedradTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private var activeActionMode: ActionMode? = null
  private lateinit var appContainer: MedradAppContainer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    appContainer = MedradAppContainer.getInstance(applicationContext)

    handleAuthIntent(intent)

    setContent {
      MedradTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          MedradNavHost(container = appContainer)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleAuthIntent(intent)
  }

  private fun handleAuthIntent(intent: Intent?) {
    val data = intent?.data ?: return
    if (data.scheme == "medrad" && data.host == "auth") {
      lifecycleScope.launch {
        appContainer.authRepository.handleOAuthCallback(data)
      }
    }
  }

  override fun onActionModeStarted(mode: ActionMode?) {
    super.onActionModeStarted(mode)
    activeActionMode = mode
  }

  override fun onActionModeFinished(mode: ActionMode?) {
    super.onActionModeFinished(mode)
    if (activeActionMode == mode) {
      activeActionMode = null
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (!hasFocus) {
      try {
        activeActionMode?.finish()
        activeActionMode = null
      } catch (_: Exception) {}
    }
  }

  override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
      appContainer.cloudSyncManager.pullLatestFromCloud()
    }
  }

  override fun onPause() {
    try {
      activeActionMode?.finish()
      activeActionMode = null
    } catch (_: Exception) {}
    super.onPause()
  }
}
