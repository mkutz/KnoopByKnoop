package com.knoopbyknoop.android

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteSummary
import kotlin.math.roundToInt

private val WAYPOINT_ROW_HEIGHT = 48.dp

/**
 * The map with the network, the network switch, the waypoints and the info sheet, see
 * `map-planning` and `network-sync`. In portrait, the waypoints are a sheet over the map; in
 * landscape, they are shown next to it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(state: AppState, viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
  // The display stays on while navigating with the app open, see `knot-navigation` "Display on
  // while navigating in the app". Only while the window is visible, so leaving the app or the
  // power button work as usual.
  val view = LocalView.current
  DisposableEffect(state.navigating) {
    view.keepScreenOn = state.navigating
    onDispose { view.keepScreenOn = false }
  }
  var showInfo by remember { mutableStateOf(false) }
  if (showInfo) InfoSheet(state, viewModel, onDismiss = { showInfo = false })
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
        // The panel's background reaches behind the navigation bar; its content keeps its width
        // clear of it, so the header stays one row, see `map-planning` "Clear of the system bars".
        Surface(modifier = Modifier.fillMaxHeight()) {
          Column(
            modifier =
              Modifier.windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom
                  )
                )
                .width(360.dp)
                .padding(top = 16.dp)
          ) {
            WaypointSheet(state, viewModel)
          }
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
/** The status bar and the cutout, at the top and the sides. */
private val topInsets: WindowInsets
  @Composable
  get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

@Composable
private fun MapArea(
  state: AppState,
  viewModel: AppViewModel,
  onSwitch: (IndexedNetworkType) -> Unit,
  onInfo: () -> Unit,
  modifier: Modifier,
) {
  // The map is shown right away; the network follows when it is loaded, see `map-planning`.
  val session = state.session
  val layers = state.layers
  Box(modifier = modifier) {
    NetworkMap(
      graph = session?.graph,
      layers = layers,
      // Without a saved place, the map shows the whole route if there is one.
      focus =
        session?.let { (it.route?.summary as? RouteSummary.Complete)?.knots ?: it.waypoints }
          ?: listOf(),
      onTap = viewModel::tap,
      online = state.online,
      initialPlace = state.initialPlace,
      onLeave = viewModel::saveCamera,
      marker = state.marker,
      onMove = viewModel::mapMoved,
      modifier = Modifier.fillMaxSize(),
      position = state.navigation?.position,
      track = state.navigation?.track,
      direction = state.navigation?.direction,
      nextKnot = state.navigation?.navigation?.nextKnot,
      offRoute = state.navigation?.navigation?.offRoute == true,
    )
    // Below the status bar and clear of the cutout, where touches go to the system, see
    // `map-planning` "Clear of the system bars".
    Column(
      modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(topInsets).padding(12.dp)
    ) {
      PlaceSearchField(state, viewModel)
      Row(
        modifier =
          Modifier.padding(top = 8.dp)
            .background(mapOverlayColor.copy(alpha = 0.85f), MaterialTheme.shapes.medium)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Not while navigating: the route is fixed then, see `knot-navigation` "Switching the
        // network while navigating".
        if (!state.navigating)
          IndexedNetworkType.entries.forEach { network ->
            FilterChip(
              selected = state.network == network,
              onClick = {
                if (state.network == network) return@FilterChip
                if (session?.waypoints.isNullOrEmpty()) viewModel.switchNetwork(network)
                else onSwitch(network)
              },
              label = { Text(network.name.lowercase().replaceFirstChar { it.uppercase() }) },
              modifier = Modifier.padding(end = 8.dp),
            )
          }
        InfoButton(onClick = onInfo, modifier = Modifier.padding(vertical = 4.dp))
      }
    }
    if (layers == null) {
      // Only without bundled graphs, e.g. a debug build that was built without internet access,
      // or for the moment it takes to read the graph.
      LoadingNote(
        text =
          if (state.status.dataDate == null && !state.online)
            "The knot networks need an internet connection once"
          else "Loading the ${state.network.name.lowercase()} knot network…",
        modifier =
          Modifier.align(Alignment.TopCenter).windowInsetsPadding(topInsets).padding(top = 140.dp),
      )
    } else if (!state.status.hasGeometry) {
      CoursesNote(
        online = state.online,
        modifier =
          Modifier.align(Alignment.TopCenter).windowInsetsPadding(topInsets).padding(top = 140.dp),
      )
    }
    Text(
      "© OpenStreetMap contributors",
      style = MaterialTheme.typography.labelSmall,
      modifier =
        Modifier.align(Alignment.BottomEnd)
          // In landscape the map reaches the bottom edge; in portrait the sheet is below it.
          .windowInsetsPadding(
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE)
              WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            else WindowInsets(0)
          )
          .padding(4.dp)
          .background(mapOverlayColor.copy(alpha = 0.7f))
          .padding(horizontal = 4.dp),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaypointSheet(state: AppState, viewModel: AppViewModel) {
  val session = state.session ?: return
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    val navigation = state.navigation
    if (navigation != null) NavigationHeader(navigation, onStop = viewModel::stopNavigation)
    else
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
            is RouteDescription.Complete ->
              Column {
                Text(description.distance, style = MaterialTheme.typography.titleLarge)
                // The time at the assumed pace, below, so it never wraps next to the actions, see
                // `map-planning` "Show the route".
                Text(
                  description.duration,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
              }
            is RouteDescription.Unreachable ->
              Text(
                "No route for ${description.legs.joinToString()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
              )
          }
        }
        // Side by side, so the header stays one row and the badges fit into the smallest sheet.
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (session.canReturnToStart) {
            IconAction(
              "Back to start",
              R.drawable.ic_action_back_to_start,
              viewModel::returnToStart,
            )
          }
          if (session.waypoints.isNotEmpty())
            IconAction("Clear", R.drawable.ic_action_clear, viewModel::clear)
          if (state.canNavigate) NavigateButton(viewModel)
        }
      }
    val complete = state.description as? RouteDescription.Complete
    // While navigating, where the rider is on the route instead of the badges, see
    // `knot-navigation` "Progress strip".
    val progress =
      navigation?.position?.let { position -> navigation.navigation?.progress(position) }
    if (progress != null) ProgressPanel(progress) else complete?.let { RouteKnots(it) }
    // Below the fixed header, the ferries and the waypoints scroll together, see `map-planning`
    // "Long routes in the panel". No waypoints while navigating: the route is fixed then and the
    // knot sequence shows it, see "Waypoint list with badges".
    WaypointList(
      if (state.navigating) listOf() else session.waypoints,
      session.canRemoveWaypoints && !state.navigating,
      viewModel,
      editable = !state.navigating,
      modifier = Modifier.weight(1f, fill = false),
    ) {
      if (complete != null && complete.ferries.isNotEmpty()) {
        Box(modifier = Modifier.padding(bottom = 8.dp)) {
          RouteFerries(complete.ferries, state.navigation?.navigation)
        }
      }
    }
  }
}

/**
 * Starts navigation after asking for location access "while in use", see `knot-navigation` "Start
 * navigation". Notifications are asked for too; without them, navigation runs and speaks, without
 * the notification.
 */
@Composable
private fun NavigateButton(viewModel: AppViewModel) {
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      granted ->
      if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) viewModel.startNavigation()
      else viewModel.locationDenied()
    }
  ProminentAction(
    "Navigate",
    R.drawable.ic_action_navigate,
    onClick = {
      val permissions =
        listOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        ) +
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.POST_NOTIFICATIONS)
          else listOf()
      launcher.launch(permissions.toTypedArray())
    },
  )
}

/**
 * The number to follow at the next knot as the most prominent information, and Stop, see
 * `knot-navigation` "Number to follow".
 */
@Composable
private fun NavigationHeader(navigation: NavigationState, onStop: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      val display = navigation.navigation?.display
      val follow = display?.follow
      when {
        display == null ->
          Text("Waiting for your position…", style = MaterialTheme.typography.titleMedium)
        display.offRoute ->
          Text(
            display.offRouteText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
          )
        follow != null ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("At ${display.next} follow", style = MaterialTheme.typography.titleLarge)
            Text(
              follow,
              style = MaterialTheme.typography.displayMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(start = 12.dp),
            )
          }
        else -> Text(display.text, style = MaterialTheme.typography.titleLarge)
      }
    }
    ProminentAction("Stop", R.drawable.ic_action_stop, onStop, color = ActionColors.red)
  }
}

/**
 * The route's knots like on the map, with the chosen waypoints standing out, in one line that
 * scrolls sideways.
 */
@Composable
private fun RouteKnots(description: RouteDescription.Complete) {
  val ferryPositions = description.ferries.map { it.fromPosition }.toSet()
  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
  ) {
    description.knots.forEachIndexed { position, (knot, role) ->
      KnotBadge(knot.ref, role)
      if (position in ferryPositions) FerryBadge()
    }
  }
}

/**
 * Waypoints in order: long-press and drag to move one, swipe to remove one. While dragging, the
 * list already shows the new order, so the other waypoints make room for the dragged one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaypointList(
  waypoints: List<Knot>,
  canRemove: Boolean,
  viewModel: AppViewModel,
  editable: Boolean,
  modifier: Modifier = Modifier,
  summary: @Composable () -> Unit,
) {
  val rowHeightPixels = with(LocalDensity.current) { WAYPOINT_ROW_HEIGHT.toPx() }
  // Indices into [waypoints] in the order shown; differs from 0, 1, 2, … only while dragging.
  var order by remember(waypoints) { mutableStateOf(waypoints.indices.toList()) }
  var dragged by remember(waypoints) { mutableStateOf<Int?>(null) }
  var dragOffset by remember(waypoints) { mutableFloatStateOf(0f) }

  LazyColumn(modifier = modifier) {
    // Not animated, so it does not jump when the route changes; the drag offsets only count the
    // waypoint rows.
    item(key = "summary") { summary() }
    // Keyed by the waypoints too: after a removal, a row must not take over the swipe state of the
    // row that was at its index, which crashed `SwipeToDismissBox`.
    items(order, key = { "${waypoints.hashCode()}-$it" }) { index ->
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
              .pointerInput(index, waypoints, editable) {
                if (!editable) return@pointerInput
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
    item(key = "end") { Spacer(modifier = Modifier.height(16.dp)) }
  }
}
