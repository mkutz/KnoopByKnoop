package com.knoopbyknoop.android

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knoopbyknoop.core.Language

class MainActivity : AppCompatActivity() {

  // While the app is visible, the navigation samples live, see `knot-navigation`.
  override fun onStart() {
    super.onStart()
    Navigating.visible.value = true
  }

  override fun onStop() {
    Navigating.visible.value = false
    super.onStop()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val viewModel: AppViewModel = viewModel()
      KnoopTheme(viewModel.state.appearance) {
        // The map extends behind the system bars, their icons dark or light like the appearance,
        // see `map-planning` "Clear of the system bars". Set once the UI shows the appearance: set
        // while the configuration changes, the system did not always take it over.
        val dark = isAppInDarkTheme()
        LaunchedEffect(dark) { showSystemBars(dark) }
        val state = viewModel.state
        // The core's texts follow the app's language, also when it changes while the app is open,
        // see `app-languages` "Language of the texts".
        val language = Language.of(LocalConfiguration.current.locales[0].language)
        LaunchedEffect(language) { viewModel.useLanguage(language) }
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(state.message) {
          state.message?.let {
            val opensSettings = state.messageOpensSettings
            val result =
              snackbarHostState.showSnackbar(
                it,
                actionLabel = if (opensSettings) getString(R.string.settings) else null,
              )
            if (opensSettings && result == SnackbarResult.ActionPerformed) {
              startActivity(
                Intent(
                  Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                  Uri.fromParts("package", packageName, null),
                )
              )
            }
            viewModel.dismissMessage()
          }
        }
        PlannerScreen(state, viewModel, snackbarHostState)
      }
    }
  }

  /** Transparent bars, with light icons when [dark]. */
  private fun showSystemBars(dark: Boolean) {
    val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
  }
}
