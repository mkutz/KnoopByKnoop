package com.knoopbyknoop.android

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteSummary
import kotlin.math.roundToInt

private val WAYPOINT_ROW_HEIGHT = 56.dp

/**
 * The map with the network, the network switch, the waypoints and the info sheet, see
 * `map-planning` and `network-sync`. In portrait, the waypoints are a sheet over the map; in
 * landscape, they are shown next to it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(state: AppState, viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
  var showInfo by remember { mutableStateOf(false) }
  if (showInfo) InfoSheet(state, viewModel, onDismiss = { showInfo = false })
  val session = state.session
  val layers = state.layers
  if (session == null || layers == null || state.loading) {
    // Only without bundled graphs, e.g. a debug build that was built without network access.
    Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (state.status.dataDate == null && !state.online) {
        Text("The knot networks need an internet connection once.", textAlign = TextAlign.Center)
      } else {
        CircularProgressIndicator()
        Text(
          "Loading the ${state.network.name.lowercase()} knot network…",
          modifier = Modifier.padding(top = 16.dp),
        )
      }
      TextButton(onClick = { showInfo = true }) { Text("Info") }
    }
    return
  }
  var pendingNetwork by remember { mutableStateOf<IndexedNetworkType?>(null) }
  val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  // The same map in both layouts: moving it instead of creating a new one keeps its position when
  // the device is rotated, see `map-planning` "Landscape layout".
  val mapArea = remember {
    movableContentOf { currentState: AppState, modifier: Modifier ->
      MapArea(
        currentState,
        viewModel,
        onSwitch = { pendingNetwork = it },
        onInfo = { showInfo = true },
        modifier = modifier,
      )
    }
  }

  if (landscape) {
    Box(modifier = Modifier.fillMaxSize()) {
      Row(modifier = Modifier.fillMaxSize()) {
        mapArea(state, Modifier.weight(1f).fillMaxHeight())
        VerticalDivider()
        Surface(modifier = Modifier.width(360.dp).fillMaxHeight()) {
          Column(modifier = Modifier.padding(top = 16.dp)) { WaypointSheet(state, viewModel) }
        }
      }
      SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
  } else {
    BottomSheetScaffold(
      sheetPeekHeight = 150.dp,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      sheetContent = { WaypointSheet(state, viewModel) },
    ) { padding ->
      mapArea(state, Modifier.fillMaxSize().padding(padding))
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

/** The map with the network switch, the info button, the courses note and the attribution. */
@Composable
private fun MapArea(
  state: AppState,
  viewModel: AppViewModel,
  onSwitch: (IndexedNetworkType) -> Unit,
  onInfo: () -> Unit,
  modifier: Modifier,
) {
  val session = state.session ?: return
  val layers = state.layers ?: return
  Box(modifier = modifier) {
    NetworkMap(
      graph = session.graph,
      layers = layers,
      // When a network opens, the map shows the whole route if there is one.
      focus = (session.route?.summary as? RouteSummary.Complete)?.knots ?: session.waypoints,
      onTap = viewModel::tap,
      online = state.online,
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
            if (session.waypoints.isEmpty()) viewModel.switchNetwork(network) else onSwitch(network)
          },
          label = { Text(network.name.lowercase().replaceFirstChar { it.uppercase() }) },
          modifier = Modifier.padding(end = 8.dp),
        )
      }
      TextButton(onClick = onInfo) { Text("Info") }
    }
    if (!state.status.hasGeometry) {
      CoursesNote(
        online = state.online,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
      )
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

@OptIn(ExperimentalLayoutApi::class)
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
            // The route's knots like on the map; the chosen waypoints stand out.
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
              itemVerticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 4.dp),
            ) {
              description.knots.forEach { (knot, role) -> KnotBadge(knot.ref, role) }
            }
          }
          is RouteDescription.Unreachable ->
            Text(
              "No route for ${description.legs.joinToString()}",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.error,
            )
        }
      }
      Column(horizontalAlignment = Alignment.End) {
        if (session.waypoints.isNotEmpty()) TextButton(onClick = viewModel::clear) { Text("Clear") }
        if (session.canReturnToStart) {
          TextButton(onClick = viewModel::returnToStart) { Text("Back to start") }
        }
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    WaypointList(session.waypoints, session.canRemoveWaypoints, viewModel)
    Spacer(modifier = Modifier.height(16.dp))
  }
}

/**
 * Waypoints in order: long-press and drag to move one, swipe to remove one. While dragging, the
 * list already shows the new order, so the other waypoints make room for the dragged one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaypointList(waypoints: List<Knot>, canRemove: Boolean, viewModel: AppViewModel) {
  val rowHeightPixels = with(LocalDensity.current) { WAYPOINT_ROW_HEIGHT.toPx() }
  // Indices into [waypoints] in the order shown; differs from 0, 1, 2, … only while dragging.
  var order by remember(waypoints) { mutableStateOf(waypoints.indices.toList()) }
  var dragged by remember(waypoints) { mutableStateOf<Int?>(null) }
  var dragOffset by remember(waypoints) { mutableFloatStateOf(0f) }

  LazyColumn(modifier = Modifier.height(WAYPOINT_ROW_HEIGHT * minOf(waypoints.size, 5))) {
    items(order, key = { it }) { index ->
      val knot = waypoints[index]
      val isDragged = dragged == index
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
        enableDismissFromStartToEnd = canRemove && dragged == null,
        enableDismissFromEndToStart = canRemove && dragged == null,
        modifier =
          (if (isDragged) Modifier.zIndex(1f) else Modifier.animateItem()).graphicsLayer {
            translationY = if (isDragged) dragOffset else 0f
            shadowElevation = if (isDragged) 8f else 0f
          },
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .height(WAYPOINT_ROW_HEIGHT)
              .background(MaterialTheme.colorScheme.surface)
              .pointerInput(index, waypoints) {
                detectDragGesturesAfterLongPress(
                  onDragStart = {
                    dragged = index
                    dragOffset = 0f
                  },
                  onDrag = { change, amount ->
                    change.consume()
                    dragOffset += amount.y
                    // Move the dragged waypoint in the shown order as soon as it covers a neighbor.
                    val current = order.indexOf(index)
                    val target =
                      (current + (dragOffset / rowHeightPixels).roundToInt()).coerceIn(
                        0,
                        order.lastIndex,
                      )
                    if (target != current) {
                      order = order.toMutableList().apply { add(target, removeAt(current)) }
                      dragOffset -= (target - current) * rowHeightPixels
                    }
                  },
                  onDragEnd = {
                    val target = order.indexOf(index)
                    dragged = null
                    dragOffset = 0f
                    if (target != index) viewModel.moveWaypoint(index, target)
                  },
                  onDragCancel = {
                    order = waypoints.indices.toList()
                    dragged = null
                    dragOffset = 0f
                  },
                )
              },
        ) {
          // Read here, so the role follows the shown order while dragging.
          val role =
            when (order.indexOf(index)) {
              0 -> KnotRole.START
              order.lastIndex -> KnotRole.END
              else -> KnotRole.STOP
            }
          Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            KnotBadge(knot.ref, role)
          }
          Text(
            role.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
          )
        }
      }
    }
  }
}
