package com.knoopbyknoop.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

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
      KnoopTheme {
        val viewModel: AppViewModel = viewModel()
        val state = viewModel.state
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(state.message) {
          state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
          }
        }
        PlannerScreen(state, viewModel, snackbarHostState)
      }
    }
  }
}
