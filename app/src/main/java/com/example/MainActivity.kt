package com.example

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.model.ActiveWindow
import com.example.service.PanelAccessibilityService
import com.example.ui.OneHandApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.OneHandViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Transparent launcher. The panel opens as an overlay on top of whatever you were doing and this
 * screen closes at once. If the accessibility service is switched on but not running yet, we
 * restart it and wait a moment. Only if it never starts is the panel drawn here.
 */
class MainActivity : ComponentActivity() {
  private val viewModel: OneHandViewModel get() = (application as OneHandApplication).viewModel
  private var handedOver = false
  private var fallbackShown = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)
    if (PanelAccessibilityService.instance != null) {
      handOverToOverlay()
      return
    }
    viewModel.reviveAccessibilityIfDead()
    if (viewModel.isAccessibilitySwitchOn()) {
      lifecycleScope.launch {
        repeat(24) {
          delay(150)
          if (PanelAccessibilityService.instance != null) {
            handOverToOverlay()
            return@launch
          }
        }
        showFallback()
      }
    } else {
      showFallback()
    }
  }

  private fun showFallback() {
    if (fallbackShown || isFinishing || handedOver) return
    fallbackShown = true
    window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    enableEdgeToEdge()
    lifecycleScope.launch {
      viewModel.isPanelOpen.collect { open -> if (!open && !handedOver) finish() }
    }
    setContent {
      MyApplicationTheme(darkTheme = true) {
        OneHandApp(viewModel = viewModel, overlayMode = true, onDismiss = { finish() })
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
    if (PanelAccessibilityService.instance != null) handOverToOverlay()
  }

  override fun onStart() {
    super.onStart()
    if (!handedOver) viewModel.setActivityVisible(true)
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshPermissions()
    viewModel.refreshQuickStates()
  }

  override fun onStop() {
    super.onStop()
    // Do not reset the current window when an Android permission/settings activity is opened.
    // The same ViewModel and Compose overlay remain alive; onResume refreshes permission state.
    viewModel.setActivityVisible(false)
  }

  private fun handOverToOverlay() {
    handedOver = true
    viewModel.togglePanel(true)
    finish()
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
  }

  // Quick Settings tile opens the settings window, a normal launch opens the panel
  private fun handleIntent(i: Intent?) {
    if (i?.getBooleanExtra("open_settings", false) == true) {
      viewModel.navigateTo(ActiveWindow.PANEL_SETTINGS)
    } else {
      viewModel.navigateTo(ActiveWindow.MAIN_PANEL)
    }
  }
}
