package com.knoopbyknoop.android

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteSummary
import kotlin.math.roundToInt

private val WAYPOINT_ROW_HEIGHT = 56.dp

/** The map with the network, the network switch and the waypoint sheet, see `map-planning`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(state: AppState, viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
  val session = state.session
  val layers = state.layers
  if (session == null || layers == null || state.loading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }
  var pendingNetwork by remember { mutableStateOf<IndexedNetworkType?>(null) }

  BottomSheetScaffold(
    sheetPeekHeight = 112.dp,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    sheetContent = { WaypointSheet(state, viewModel) },
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      NetworkMap(
        graph = session.graph,
        layers = layers,
        // When a network opens, the map shows the whole route if there is one.
        focus = (session.route?.summary as? RouteSummary.Complete)?.knots ?: session.waypoints,
        onTap = viewModel::tap,
        modifier = Modifier.fillMaxSize(),
      )
      Row(
        modifier =
          Modifier.align(Alignment.TopStart)
            .padding(12.dp)
            .background(Color.White.copy(alpha = 0.85f), MaterialTheme.shapes.medium)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IndexedNetworkType.entries.forEach { network ->
          FilterChip(
            selected = IndexedNetworkType.of(session.network) == network,
            onClick = {
              if (IndexedNetworkType.of(session.network) == network) return@FilterChip
              if (session.waypoints.isEmpty()) viewModel.switchNetwork(network)
              else pendingNetwork = network
            },
            label = { Text(network.name.lowercase().replaceFirstChar { it.uppercase() }) },
            modifier = Modifier.padding(end = 8.dp),
          )
        }
        TextButton(onClick = viewModel::showRegions) { Text("Regions") }
      }
      Text(
        "© OpenStreetMap contributors",
        style = MaterialTheme.typography.labelSmall,
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(4.dp)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 4.dp),
      )
    }
  }

  pendingNetwork?.let { network ->
    AlertDialog(
      onDismissRequest = { pendingNetwork = null },
      title = { Text("Switch to ${network.name.lowercase()}?") },
      text = { Text("The current route will be discarded.") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.switchNetwork(network)
            pendingNetwork = null
          }
        ) {
          Text("Switch")
        }
      },
      dismissButton = { TextButton(onClick = { pendingNetwork = null }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun WaypointSheet(state: AppState, viewModel: AppViewModel) {
  val session = state.session ?: return
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        when (val description = state.description) {
          RouteDescription.Empty ->
            Text("Tap a knot to set the start", style = MaterialTheme.typography.titleMedium)
          is RouteDescription.StartOnly ->
            Text(
              "Start at ${description.start}, tap a knot to set the end",
              style = MaterialTheme.typography.titleMedium,
            )
          is RouteDescription.Complete -> {
            Text(description.distance, style = MaterialTheme.typography.titleLarge)
            Text(description.knots, style = MaterialTheme.typography.bodyMedium)
          }
          is RouteDescription.Unreachable ->
            Text(
              "No route for ${description.legs.joinToString()}",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.error,
            )
        }
      }
      if (session.waypoints.isNotEmpty()) TextButton(onClick = viewModel::clear) { Text("Clear") }
    }
    Spacer(modifier = Modifier.height(8.dp))
    WaypointList(session.waypoints, session.canRemoveWaypoints, viewModel)
    Spacer(modifier = Modifier.height(16.dp))
  }
}

/** Waypoints in order: long-press and drag to move one, swipe to remove one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaypointList(waypoints: List<Knot>, canRemove: Boolean, viewModel: AppViewModel) {
  val rowHeightPixels = with(LocalDensity.current) { WAYPOINT_ROW_HEIGHT.toPx() }
  var draggedIndex by remember { mutableStateOf<Int?>(null) }
  var dragOffset by remember { mutableFloatStateOf(0f) }

  LazyColumn(modifier = Modifier.height(WAYPOINT_ROW_HEIGHT * minOf(waypoints.size, 5))) {
    itemsIndexed(waypoints, key = { index, knot -> "$index-${knot.id}" }) { index, knot ->
      val dismissState =
        rememberSwipeToDismissBoxState(
          confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && canRemove) {
              viewModel.removeWaypoint(index)
              true
            } else {
              false
            }
          }
        )
      SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = canRemove,
        enableDismissFromEndToStart = canRemove,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .height(WAYPOINT_ROW_HEIGHT)
              .graphicsLayer { translationY = if (draggedIndex == index) dragOffset else 0f }
              .background(MaterialTheme.colorScheme.surface)
              .pointerInput(index, waypoints.size) {
                detectDragGesturesAfterLongPress(
                  onDragStart = {
                    draggedIndex = index
                    dragOffset = 0f
                  },
                  onDrag = { change, amount ->
                    change.consume()
                    dragOffset += amount.y
                  },
                  onDragEnd = {
                    val target =
                      (index + (dragOffset / rowHeightPixels).roundToInt()).coerceIn(
                        0,
                        waypoints.lastIndex,
                      )
                    draggedIndex = null
                    dragOffset = 0f
                    if (target != index) viewModel.moveWaypoint(index, target)
                  },
                  onDragCancel = {
                    draggedIndex = null
                    dragOffset = 0f
                  },
                )
              },
        ) {
          Text(
            when (index) {
              0 -> "Start"
              waypoints.lastIndex -> "End"
              else -> "Stop"
            },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(end = 16.dp),
          )
          Text(knot.ref, style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}
