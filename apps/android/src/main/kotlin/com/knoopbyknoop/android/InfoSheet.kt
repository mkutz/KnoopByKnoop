package com.knoopbyknoop.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.NetworkState
import com.knoopbyknoop.core.NetworkStatus

/**
 * Data dates, download state and attribution of the knot networks, see `network-sync` "Info sheet".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(state: AppState, viewModel: AppViewModel, onDismiss: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
      // The appearance of the whole app, see `map-planning` "Light and dark appearance".
      Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleLarge)
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
      ) {
        Appearance.entries.forEachIndexed { index, appearance ->
          SegmentedButton(
            selected = state.appearance == appearance,
            onClick = { viewModel.chooseAppearance(appearance) },
            shape = SegmentedButtonDefaults.itemShape(index, Appearance.entries.size),
          ) {
            Text(stringResource(appearance.label))
          }
        }
      }
      Text(stringResource(R.string.knot_network_data), style = MaterialTheme.typography.titleLarge)
      state.catalog.networks.forEach { status ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              stringResource(status.network.label),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(describe(status, state.online), style = MaterialTheme.typography.bodyMedium)
          }
          if (status.lastAttemptFailed)
            TextButton(onClick = viewModel::retrySync) { Text(stringResource(R.string.retry)) }
        }
      }
      if (state.needsAppUpdate) {
        Text(
          stringResource(R.string.needs_app_update),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(top = 12.dp),
        )
      }
      Text(
        stringResource(R.string.odbl_attribution),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 24.dp),
      )
      Text(
        stringResource(R.string.photon_attribution),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
      )
    }
  }
}

@Composable
private fun describe(status: NetworkStatus, online: Boolean): String {
  val data =
    status.dataDate?.let { stringResource(R.string.data_of, it) }
      ?: stringResource(R.string.not_installed)
  val state =
    when {
      status.lastAttemptFailed -> R.string.last_download_failed
      status.state == NetworkState.DOWNLOADING -> R.string.downloading
      !status.hasGeometry && !online -> R.string.courses_need_internet
      !status.hasGeometry -> R.string.courses_loading
      status.state == NetworkState.UPDATABLE -> R.string.update_pending
      else -> null
    }
  return listOfNotNull(data, state?.let { stringResource(it) }).joinToString(", ")
}

/** Shown while the courses of the selected network are missing, see `network-sync`. */
@Composable
fun CoursesNote(online: Boolean, modifier: Modifier = Modifier) {
  LoadingNote(
    stringResource(if (online) R.string.loading_courses else R.string.courses_need_internet_once),
    modifier,
  )
}

/** A short note over the map about something that is still loading. */
@Composable
fun LoadingNote(text: String, modifier: Modifier = Modifier) {
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    modifier =
      modifier
        .background(mapOverlayColor.copy(alpha = 0.9f), MaterialTheme.shapes.small)
        .padding(horizontal = 12.dp, vertical = 6.dp),
  )
}
