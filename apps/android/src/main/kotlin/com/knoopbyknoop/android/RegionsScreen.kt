package com.knoopbyknoop.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.RegionState
import com.knoopbyknoop.core.RegionStatus
import java.util.Locale

/** The region list, see `region-management`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionsScreen(state: AppState, viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Regions") },
        actions = {
          if (state.session != null) {
            TextButton(onClick = { viewModel.openPlanner(state.session.region) }) { Text("Map") }
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { padding ->
    LazyColumn(modifier = Modifier.padding(padding)) {
      if (state.catalog.otherRegionsNeedNetworkAccess) {
        item {
          Text(
            "Other regions need network access.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      items(state.catalog.regions, key = { it.region }) { region ->
        RegionRow(region, state.progress[region.region], viewModel)
      }
    }
  }
}

@Composable
private fun RegionRow(region: RegionStatus, progress: Float?, viewModel: AppViewModel) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
    Text(region.name, style = MaterialTheme.typography.titleMedium)
    Text(
      "${formatMegabytes(region.sizeBytes)} · data from ${region.dataDate}" +
        when (region.state) {
          RegionState.INSTALLED -> " · installed"
          RegionState.UPDATABLE -> " · update available"
          else -> ""
        },
      style = MaterialTheme.typography.bodySmall,
    )
    if (region.state == RegionState.DOWNLOADING) {
      LinearProgressIndicator(
        progress = { progress ?: 0f },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      )
    } else {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
      ) {
        when (region.state) {
          RegionState.AVAILABLE ->
            Button(onClick = { viewModel.download(region) }) { Text("Download") }
          RegionState.UPDATABLE ->
            Button(onClick = { viewModel.download(region) }) { Text("Update") }
          else -> {}
        }
        if (region.state == RegionState.INSTALLED || region.state == RegionState.UPDATABLE) {
          OutlinedButton(onClick = { viewModel.openPlanner(region.region) }) { Text("Open") }
          TextButton(onClick = { viewModel.remove(region.region) }) { Text("Remove") }
        }
      }
    }
  }
}

private fun formatMegabytes(bytes: Long) =
  String.format(Locale.ENGLISH, "%.1f MB", bytes / 1_000_000.0)
