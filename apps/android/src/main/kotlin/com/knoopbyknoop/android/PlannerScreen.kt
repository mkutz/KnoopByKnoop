package com.knoopbyknoop.android

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.knoopbyknoop.core.HeaderRow
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.KnotPlate
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.NewKnots
import com.knoopbyknoop.core.PathDataStatus
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteSummary
import com.knoopbyknoop.core.hasStraightLines
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val WAYPOINT_ROW_HEIGHT = 48.dp

/**
 * The map with the network, the network switch, the waypoints and the info sheet, see
 * `map-planning` and `network-sync`. In portrait, the waypoints are a sheet over the map; in
 * landscape, they are shown next to it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(state: AppState, viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
  // The display stays on while navigating with the app open, see `knot-navigation` "Display on
  // while navigating in the app". Only while the window is visible, so leaving the app or the
  // power button work as usual.
  val window = LocalActivity.current?.window
  // Over the lock screen, the display goes off as usual. The window's flag and not
  // `View.keepScreenOn`, which stayed set for good after leaving the app and coming back.
  val overLockScreen by Navigating.overLockScreen.collectAsState()
  DisposableEffect(window, state.navigating, overLockScreen) {
    if (state.navigating && !overLockScreen) {
      window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
  }
  // Kept when the activity is recreated, e.g. after the system ended the app in the background.
  var showInfo by rememberSaveable { mutableStateOf(false) }
  if (showInfo) InfoSheet(state, viewModel, onDismiss = { showInfo = false })
  var pendingNetwork by remember { mutableStateOf<IndexedNetworkType?>(null) }
  // The place search, opened from the map's magnifier button into the sheet or side panel, see
  // `place-search` "Search while typing".
  var searching by rememberSaveable { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  val closeSearch = {
    focusManager.clearFocus()
    searching = false
  }
  BackHandler(enabled = searching, onBack = closeSearch)
  // No search while navigating.
  LaunchedEffect(state.navigating) { if (state.navigating) searching = false }
  val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  // The same map in both layouts: moving it instead of creating a new one keeps its position when
  // the device is rotated, see `map-planning` "Landscape layout".
  val mapArea = remember {
    movableContentOf { currentState: AppState, modifier: Modifier ->
      MapArea(
        currentState,
        viewModel,
        searching = searching,
        onSearch = { searching = true },
        onCloseSearch = closeSearch,
        onSwitch = { pendingNetwork = it },
        onInfo = { showInfo = true },
        infoShown = showInfo,
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
            // The search takes the panel's place while open, see `map-planning` "Landscape
            // layout".
            if (searching) PlaceSearchField(state, viewModel, onClose = closeSearch)
            else WaypointSheet(state, viewModel, onShowKnot = viewModel::showOnMap)
          }
        }
      }
      SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
  } else {
    // While navigating, the sheet is at least as high as the number to follow and the whole strip,
    // with the coming knots above, and starting or ending navigation brings it down to its smallest
    // height, see `knot-navigation` "Panel height while navigating".
    val scaffoldState = rememberBottomSheetScaffoldState()
    var navigationHeight by remember { mutableIntStateOf(0) }
    var summaryHeight by remember { mutableIntStateOf(0) }
    var dragHandleHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    fun Int.toDp() = ceil(this / density.density).dp
    val peekHeight =
      if (state.navigating && navigationHeight > 0)
        (dragHandleHeight + navigationHeight).toDp() + 8.dp + navigationBar
      // While planning, at least the summary with the actions, which are below it at large text
      // sizes, see `map-planning` "Route summary at large text sizes", the knots and the switch.
      else if (!state.navigating)
        maxOf(150.dp, (dragHandleHeight + summaryHeight).toDp() + 8.dp + navigationBar)
      else 150.dp
    LaunchedEffect(state.navigating) {
      if (!state.navigating) navigationHeight = 0
      scaffoldState.bottomSheetState.partialExpand()
    }
    // Also when the height changes, e.g. off route or with another font size: the sheet does not
    // follow a changed peek height by itself. Only at its smallest, so a sheet the user pulled up
    // stays up.
    LaunchedEffect(peekHeight) {
      if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded)
        scaffoldState.bottomSheetState.partialExpand()
    }
    val scope = rememberCoroutineScope()
    // Up while searching, so the hits have room above the keyboard; back to the height it had.
    var heightBeforeSearch by remember { mutableStateOf<SheetValue?>(null) }
    LaunchedEffect(searching) {
      val sheet = scaffoldState.bottomSheetState
      if (searching) {
        heightBeforeSearch = sheet.currentValue
        sheet.expand()
      } else if (heightBeforeSearch == SheetValue.PartiallyExpanded) {
        heightBeforeSearch = null
        sheet.partialExpand()
      }
    }
    BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetPeekHeight = peekHeight,
      sheetDragHandle = {
        BottomSheetDefaults.DragHandle(
          modifier = Modifier.onSizeChanged { dragHandleHeight = it.height }
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      sheetContent = {
        if (searching)
          PlaceSearchField(
            state,
            viewModel,
            onClose = closeSearch,
            modifier = Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.navigationBars),
          )
        else
          WaypointSheet(
            state,
            viewModel,
            // The highest the navigation part was, so the sheet does not jump when the header
            // switches to the number to follow near a knot, see `knot-navigation` "Number to
            // follow".
            onNavigationHeight = { navigationHeight = maxOf(navigationHeight, it) },
            onSummaryHeight = { summaryHeight = it },
            // The sheet comes down, so the knot is not hidden by it, see `knot-navigation` "Showing
            // a coming knot on the map".
            onShowKnot = { knot ->
              viewModel.showOnMap(knot)
              scope.launch { scaffoldState.bottomSheetState.partialExpand() }
            },
          )
      },
    ) { padding ->
      mapArea(state, Modifier.fillMaxSize().padding(padding))
    }
  }

  pendingNetwork?.let { network ->
    AlertDialog(
      onDismissRequest = { pendingNetwork = null },
      title = {
        Text(
          stringResource(
            if (network == IndexedNetworkType.CYCLING) R.string.switch_to_cycling
            else R.string.switch_to_hiking
          )
        )
      },
      text = { Text(stringResource(R.string.route_discarded)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.switchNetwork(network)
            pendingNetwork = null
          }
        ) {
          Text(stringResource(R.string.switch_network))
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingNetwork = null }) { Text(stringResource(R.string.cancel)) }
      },
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
  searching: Boolean,
  onSearch: () -> Unit,
  onCloseSearch: () -> Unit,
  onSwitch: (IndexedNetworkType) -> Unit,
  onInfo: () -> Unit,
  infoShown: Boolean,
  modifier: Modifier,
) {
  // The map is shown right away; the network follows when it is loaded, see `map-planning`.
  val session = state.session
  val layers = state.layers
  Box(modifier = modifier) {
    // The position once per tap, asking for location access first, see `map-planning` "Show my
    // position"; on the first start, asked right away.
    val activity = LocalActivity.current
    val locationLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted ->
        if (
          granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        )
          viewModel.locate()
        else
          viewModel.positionDenied(
            permanently =
              activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
              ) == false
          )
      }
    val locate = {
      locationLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    }
    LaunchedEffect(state.askLocationOnStart) {
      if (state.askLocationOnStart) {
        viewModel.askedLocationOnStart()
        locate()
      }
    }
    var topControlsBottom by remember { mutableIntStateOf(0) }
    var topControlsStart by remember { mutableIntStateOf(0) }
    NetworkMap(
      places = state.places,
      // The map follows an appearance chosen in the info sheet when the sheet closes, see
      // `map-planning` "Choosing the appearance in the info sheet".
      holdAppearance = infoShown,
      graph = session?.graph,
      layers = layers,
      // Without a saved place, the map shows the whole route if there is one.
      focus =
        session?.let { (it.route?.summary as? RouteSummary.Complete)?.knots ?: it.waypoints }
          ?: listOf(),
      // A tap on the map closes an open search rather than adding a knot, see `place-search`
      // "Search while typing".
      onTap = { latitude, longitude, radius, x, y, place ->
        if (searching) onCloseSearch() else viewModel.tap(latitude, longitude, radius, x, y, place)
      },
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
      navigating = state.navigation?.navigation != null,
      courseAhead = state.navigation?.navigation?.courseAhead,
      wayToRoute =
        state.navigation?.let { navigating ->
          navigating.position?.let { navigating.navigation?.wayToRoute(it) }
        },
      mapFocus =
        state.navigation?.let { navigating ->
          navigating.position?.let { navigating.navigation?.mapFocus(it) }
        },
      guidedWay =
        state.navigation?.let { navigating ->
          navigating.navigation?.guidedLines(navigating.guidedWay)
        },
      myPosition = state.myPosition.takeUnless { state.navigating },
      locating = state.locating,
      onLocate = locate.takeUnless { state.navigating },
      onSearch = onSearch.takeUnless { state.navigating || searching },
      findingNearest = state.findingNearest,
      onShowNearest =
        viewModel::showNearestKnots.takeIf { state.noKnotsInView && !state.navigating },
      nearestKnots = state.nearestKnots,
      onNearestShown = viewModel::nearestKnotsShown,
      shownKnot = state.shownKnot,
      topControlsBottom = topControlsBottom,
      topControlsStart = topControlsStart,
      offerPlace = state.offer?.offer?.position,
      offerFlag = state.offer?.nameOnly != true,
      onOfferMoved = viewModel::offerMoved,
    )
    state.offer?.let { PlaceOfferMenu(it, viewModel) }
    // Below the status bar and clear of the cutout, where touches go to the system, see
    // `map-planning` "Clear of the system bars".
    Column(
      modifier =
        Modifier.align(Alignment.TopStart)
          .onGloballyPositioned { topControlsBottom = it.boundsInParent().bottom.roundToInt() }
          .windowInsetsPadding(topInsets)
          .padding(12.dp)
          .onGloballyPositioned { topControlsStart = it.positionInParent().x.roundToInt() }
    ) {
      // Not while navigating: the route is fixed then, see `knot-navigation` "Switching the network
      // while navigating", and the info sheet is not needed on the ride, see `network-sync` "Info
      // sheet".
      if (!state.navigating)
        Row(
          // Scrolls at very large text, so the words stay on one line and the info button in reach.
          modifier =
            Modifier.background(mapOverlayColor.copy(alpha = 0.85f), MaterialTheme.shapes.medium)
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IndexedNetworkType.entries.forEach { network ->
            FilterChip(
              selected = state.network == network,
              onClick = {
                if (state.network == network) return@FilterChip
                if (session?.waypoints.isNullOrEmpty()) viewModel.switchNetwork(network)
                else onSwitch(network)
              },
              label = { Text(stringResource(network.label), maxLines = 1, softWrap = false) },
              // In the color of the word: the default is green, which means start in this app.
              colors =
                FilterChipDefaults.filterChipColors(
                  iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
              // The word is what screen readers announce, so the icon has no description of its
              // own.
              leadingIcon = {
                Icon(
                  painterResource(network.icon),
                  contentDescription = null,
                  modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
              },
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
            stringResource(R.string.networks_need_internet)
          else
            stringResource(
              if (state.network == IndexedNetworkType.CYCLING) R.string.loading_cycling
              else R.string.loading_hiking
            ),
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
      stringResource(R.string.osm_contributors),
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
private fun WaypointSheet(
  state: AppState,
  viewModel: AppViewModel,
  onNavigationHeight: (Int) -> Unit = {},
  onSummaryHeight: (Int) -> Unit = {},
  onShowKnot: (Knot) -> Unit = {},
) {
  val session = state.session ?: return
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    val navigation = state.navigation
    val complete = state.description as? RouteDescription.Complete
    // While navigating, where the rider is on the route, see `knot-navigation` "Progress strip".
    val progress =
      navigation?.position?.let { position ->
        navigation.navigation?.progress(position, navigation.guidedWay)
      }
    if (navigation != null)
      // Measured for the sheet's smallest height while navigating, see `PlannerScreen`.
      Column(modifier = Modifier.onSizeChanged { onNavigationHeight(it.height) }) {
        NavigationHeader(navigation, onStop = viewModel::stopNavigation)
        val current = navigation.navigation
        if (progress != null && current != null)
          ProgressPanel(progress, complete?.ferries.orEmpty(), current)
        else complete?.let { RouteKnots(it) }
      }
    else {
      // Measured for the sheet's smallest height while planning, see `PlannerScreen`.
      Column(modifier = Modifier.onSizeChanged { onSummaryHeight(it.height) }) {
        SummaryWithActions(
          whole = state.description is RouteDescription.Complete,
          // The distance and time stay in one line each, with the actions below them where they do
          // not fit next to them, see `map-planning` "Route summary at large text sizes".
          summary = {
            when (val description = state.description) {
              RouteDescription.Empty ->
                Text(
                  stringResource(
                    if (state.noKnotsInView) R.string.no_knots_in_view
                    else R.string.tap_to_set_start
                  ),
                  style = MaterialTheme.typography.titleMedium,
                )
              is RouteDescription.StartOnly ->
                Text(
                  when {
                    description.endFirst -> stringResource(R.string.end_point_set_start)
                    description.routePoint -> stringResource(R.string.start_point_set_end)
                    else -> stringResource(R.string.start_at_set_end, description.start)
                  },
                  style = MaterialTheme.typography.titleMedium,
                )
              is RouteDescription.Complete ->
                Column {
                  Text(
                    description.distance,
                    style = MaterialTheme.typography.titleLarge,
                    softWrap = false,
                  )
                  // The time at the assumed pace, below, so it never wraps next to the actions, see
                  // `map-planning` "Show the route".
                  Text(
                    description.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = false,
                  )
                }
              is RouteDescription.Unreachable ->
                Text(
                  stringResource(R.string.no_route_for, description.legs.joinToString()),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.error,
                )
            }
          },
          // Side by side, so the header stays one row and the badges fit into the smallest sheet.
          actions = {
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              val context = LocalContext.current
              val moreActions = buildList {
                if (session.canReturnToStart)
                  add(
                    MenuAction(
                      stringResource(R.string.back_to_start),
                      R.drawable.ic_action_back_to_start,
                      viewModel::returnToStart,
                    )
                  )
                if (viewModel.canExportGpx)
                  add(
                    MenuAction(stringResource(R.string.export_gpx), R.drawable.ic_action_export) {
                      viewModel.gpx()?.let { (fileName, content) ->
                        shareGpx(context, fileName, content)
                      }
                    }
                  )
              }
              if (moreActions.isNotEmpty()) MoreActions(moreActions)
              if (session.waypoints.isNotEmpty())
                IconAction(
                  stringResource(R.string.clear),
                  R.drawable.ic_action_clear,
                  viewModel::clear,
                )
              if (state.canNavigate) NavigateButton(viewModel)
              // In place of asking to tap a knot, see `map-planning` "Nearest knots".
              if (state.noKnotsInView && session.waypoints.isEmpty())
                ProminentAction(
                  stringResource(R.string.show_nearest_knots),
                  R.drawable.ic_nearest_knots,
                  viewModel::showNearestKnots,
                )
            }
          },
        )
        complete?.let { RouteKnots(it) }
        state.pathData?.let { PathDataNote(it) }
        // See `map-planning` "No path data".
        if (session.hasStraightLines) {
          Text(
            stringResource(R.string.straight_ways),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
          )
        }
        if (session.route != null) NewKnotsSwitch(state.newKnots, viewModel::chooseNewKnots)
      }
    }
    // While navigating, the coming knots below the strip in place of the waypoints, as the route is
    // fixed then, see `knot-navigation` "Coming knots" and `map-planning` "Waypoint list with
    // badges".
    if (state.navigating)
      ComingKnots(
        progress?.coming.orEmpty(),
        onShowKnot,
        modifier = Modifier.weight(1f, fill = false),
      )
    else
    // Below the fixed header, the ferries and the waypoints scroll together, see `map-planning`
    // "Long routes in the panel".
    WaypointList(
        session.waypoints,
        session.endFirst,
        session.canRemoveWaypoints,
        viewModel,
        editable = true,
        modifier = Modifier.weight(1f, fill = false),
      ) {
        if (complete != null && complete.ferries.isNotEmpty()) {
          Box(modifier = Modifier.padding(bottom = 8.dp)) {
            RouteFerries(complete.ferries)
          }
        }
      }
  }
}

/**
 * What tapped knots become, with the label above the choices where they do not fit next to it, see
 * `map-planning` "What new knots become".
 */
@Composable
private fun NewKnotsSwitch(selected: NewKnots, onChoose: (NewKnots) -> Unit) {
  FlowRow(
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
  ) {
    Text(stringResource(R.string.new_knots), style = MaterialTheme.typography.bodyMedium)
    // Sideways where it does not fit, as both parts are as wide as the longer one, and a part with
    // less room would cut its word off.
    SingleChoiceSegmentedButtonRow(Modifier.horizontalScroll(rememberScrollState())) {
      NewKnots.entries.forEachIndexed { index, newKnots ->
        SegmentedButton(
          selected = selected == newKnots,
          onClick = { onChoose(newKnots) },
          shape = SegmentedButtonDefaults.itemShape(index, NewKnots.entries.size),
          // The slot is too small for this icon, which is wider than it is high, so the icon is
          // part
          // of the label. The word is what screen readers announce.
          icon = {},
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              painterResource(
                when (newKnots) {
                  NewKnots.STOP -> R.drawable.ic_new_knots_between
                  NewKnots.END -> R.drawable.ic_new_knots_end
                }
              ),
              contentDescription = null,
              modifier = Modifier.size(width = 32.dp, height = 16.dp),
            )
            // Without spaces to break at, as the row takes its width from the longest word, which
            // would cut off a label of several words.
            Text(
              stringResource(
                  when (newKnots) {
                    NewKnots.STOP -> R.string.new_knots_between
                    NewKnots.END -> R.string.new_knots_end
                  }
                )
                .replace(' ', '\u00A0'),
              softWrap = false,
            )
          }
        }
      }
    }
  }
}

/**
 * The [summary] with the [actions] at its end, or the actions below it where the summary does not
 * fit next to them: [whole] in its full width, e.g. a route's distance and time at a large text
 * size, otherwise wrapped down to its longest word.
 */
@Composable
private fun SummaryWithActions(
  whole: Boolean,
  summary: @Composable () -> Unit,
  actions: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Layout(contents = listOf(summary, actions), modifier = modifier) {
    (summaryMeasurables, actionMeasurables),
    constraints ->
    val free = constraints.copy(minWidth = 0, minHeight = 0)
    val actionPlaceables = actionMeasurables.map { it.measure(free) }
    val actionsWidth = actionPlaceables.maxOfOrNull { it.width } ?: 0
    val actionsHeight = actionPlaceables.maxOfOrNull { it.height } ?: 0
    val besideWidth = constraints.maxWidth - actionsWidth
    val beside = summaryMeasurables.all {
      (if (whole) it.maxIntrinsicWidth(constraints.maxHeight)
      else it.minIntrinsicWidth(constraints.maxHeight)) <= besideWidth
    }
    val summaryPlaceables = summaryMeasurables.map {
      it.measure(free.copy(maxWidth = if (beside) besideWidth else constraints.maxWidth))
    }
    val summaryHeight = summaryPlaceables.maxOfOrNull { it.height } ?: 0
    val height = if (beside) maxOf(summaryHeight, actionsHeight) else summaryHeight + actionsHeight
    layout(constraints.maxWidth, height) {
      summaryPlaceables.forEach {
        it.place(0, if (beside) (height - it.height) / 2 else 0)
      }
      actionPlaceables.forEach {
        it.place(
          constraints.maxWidth - it.width,
          if (beside) (height - it.height) / 2 else summaryHeight,
        )
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
    stringResource(R.string.navigate),
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
 * What to look for on the signs, as a row without words, and Stop, see `knot-navigation` "Number to
 * follow".
 */
@Composable
private fun NavigationHeader(navigation: NavigationState, onStop: () -> Unit) {
  val current = navigation.navigation
  val position = navigation.position
  val row =
    remember(current, position, navigation.direction, navigation.guidedWay) {
      if (current == null || position == null) null
      else
        current.header(
          position,
          navigation.direction,
          navigation.guidedWay,
          Navigating.route?.geometry,
        )
    }
  // Off route and riding the wrong way, the header turns dark, light in dark mode, instead of red,
  // see `map-planning` "Light and dark appearance".
  val inverted = row is HeaderRow.WrongWay || (row is HeaderRow.ToRoute && row.offRoute)
  val dark = isAppInDarkTheme()
  val invertedBackground = if (dark) Color(0xFFE6E8E6) else Ink
  val invertedContent = if (dark) Ink else Color.White
  CompositionLocalProvider(
    LocalContentColor provides if (inverted) invertedContent else LocalContentColor.current
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        if (inverted)
          Modifier.background(invertedBackground, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        else Modifier,
    ) {
      if (row == null) {
        Text(
          stringResource(R.string.waiting_for_position),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
      } else {
        // A gap before Stop, so a badge's corner symbol does not touch it.
        HeaderRowView(row, Modifier.weight(1f).padding(end = 12.dp))
      }
      // Over the lock screen, a touch in the pocket shouldn't end the ride.
      val overLockScreen by Navigating.overLockScreen.collectAsState()
      if (overLockScreen)
        HoldAction(stringResource(R.string.stop_hold), R.drawable.ic_action_stop, onStop)
      else StopIconAction(stringResource(R.string.stop), onStop)
    }
  }
}

/** Whether the ways for the route are in, see `network-sync` "Path data ready for the ride". */
@Composable
private fun PathDataNote(status: PathDataStatus) {
  Text(
    stringResource(
      when (status) {
        PathDataStatus.LOADING -> R.string.path_data_loading
        PathDataStatus.READY -> R.string.path_data_ready
        PathDataStatus.UNAVAILABLE -> R.string.path_data_unavailable
      }
    ),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(bottom = 4.dp),
  )
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
      KnotBadge(
        knot.ref,
        role,
        plate = KnotPlate.of(knot.signRegion),
        place = knot.routePoint?.placeKind,
      )
      if (position in ferryPositions) FerryBadge()
    }
  }
}

/**
 * Waypoints in order: drag the handle, or long-press and drag the row, to move one; tap ×, or
 * swipe, to remove one, see `map-planning` "Edit the waypoint list". While dragging, the list
 * already shows the new order, so the other waypoints make room for the dragged one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaypointList(
  waypoints: List<Knot>,
  endFirst: Boolean,
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

  fun startDrag(index: Int) {
    dragged = index
    dragOffset = 0f
  }

  fun drag(index: Int, amount: Float) {
    dragOffset += amount
    // Move the dragged waypoint in the shown order as soon as it covers a neighbor.
    val current = order.indexOf(index)
    val target =
      (current + (dragOffset / rowHeightPixels).roundToInt()).coerceIn(0, order.lastIndex)
    if (target != current) {
      order = order.toMutableList().apply { add(target, removeAt(current)) }
      dragOffset -= (target - current) * rowHeightPixels
    }
  }

  fun endDrag(index: Int) {
    val target = order.indexOf(index)
    dragged = null
    dragOffset = 0f
    if (target != index) viewModel.moveWaypoint(index, target)
  }

  fun cancelDrag() {
    order = waypoints.indices.toList()
    dragged = null
    dragOffset = 0f
  }

  LazyColumn(modifier = modifier) {
    // Not animated, so it does not jump when the route changes; the drag offsets only count the
    // waypoint rows.
    item(key = "summary") { summary() }
    // Keyed by the waypoints too: after a removal, a row must not take over the swipe state of the
    // row that was at its index, which crashed `SwipeToDismissBox`.
    items(order, key = { "${waypoints.hashCode()}-$it" }) { index ->
      val knot = waypoints[index]
      val isDragged = dragged == index
      val dismissState = rememberSwipeToDismissBoxState()
      SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        // Only directions that may remove are enabled, so every dismissal removes.
        enableDismissFromStartToEnd = canRemove && dragged == null,
        enableDismissFromEndToStart = canRemove && dragged == null,
        onDismiss = { viewModel.removeWaypoint(index) },
        modifier =
          (if (isDragged) Modifier.zIndex(1f) else Modifier.animateItem()).graphicsLayer {
            translationY = if (isDragged) dragOffset else 0f
            shadowElevation = if (isDragged) 8f else 0f
          },
      ) {
        // Read here, so the role follows the shown order while dragging.
        val role =
          when {
            endFirst -> KnotRole.END
            order.indexOf(index) == 0 -> KnotRole.START
            order.indexOf(index) == order.lastIndex -> KnotRole.END
            else -> KnotRole.STOP
          }
        val roleName = stringResource(role.label)
        // Read outside the semantics block, which cannot read resources.
        val moveUp = stringResource(R.string.move_up)
        val moveDown = stringResource(R.string.move_down)
        val remove = stringResource(R.string.remove)
        // A route point is named by its role, as it has no number, and by its place if it was made
        // from one, see `map-planning` "Route points from a place".
        val placeKind = knot.routePoint?.placeKind
        val placeName =
          knot.routePoint?.placeName ?: placeKind?.let { stringResource(placeKindLabel(it)) }
        val name =
          when {
            placeName != null -> "$roleName, $placeName"
            knot.isRoutePoint -> roleName
            else -> knot.ref
          }
        val removeKnot = stringResource(R.string.remove_knot, name)
        val moveKnot = stringResource(R.string.move_knot, name)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .height(WAYPOINT_ROW_HEIGHT)
              .background(MaterialTheme.colorScheme.surface)
              // Screen readers cannot drag: moving and removing as actions on the row.
              .semantics(mergeDescendants = true) {
                if (editable)
                  customActions =
                    listOfNotNull(
                      CustomAccessibilityAction(moveUp) {
                          viewModel.moveWaypoint(index, index - 1)
                          true
                        }
                        .takeIf { index > 0 },
                      CustomAccessibilityAction(moveDown) {
                          viewModel.moveWaypoint(index, index + 1)
                          true
                        }
                        .takeIf { index < waypoints.lastIndex },
                      CustomAccessibilityAction(remove) {
                          viewModel.removeWaypoint(index)
                          true
                        }
                        .takeIf { canRemove },
                    )
              }
              .pointerInput(index, waypoints, editable) {
                if (!editable) return@pointerInput
                detectDragGesturesAfterLongPress(
                  onDragStart = { startDrag(index) },
                  onDrag = { change, amount ->
                    change.consume()
                    drag(index, amount.y)
                  },
                  onDragEnd = { endDrag(index) },
                  onDragCancel = { cancelDrag() },
                )
              },
        ) {
          Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            KnotBadge(
              knot.ref,
              role,
              plate = KnotPlate.of(knot.signRegion),
              place = knot.routePoint?.placeKind,
            )
          }
          Text(
            placeName?.let { "$roleName · $it" } ?: roleName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Read with a comma, not the dot, see `map-planning` "Screen reader in the list".
            modifier =
              Modifier.padding(start = 12.dp).weight(1f).semantics { contentDescription = name },
          )
          if (editable && canRemove) {
            IconButton(onClick = { viewModel.removeWaypoint(index) }) {
              Icon(
                painterResource(R.drawable.ic_remove),
                contentDescription = removeKnot,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          if (editable) {
            // Drags right away, without the long press the row needs.
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(48.dp)
                  .semantics { contentDescription = moveKnot }
                  .pointerInput(index, waypoints) {
                    detectDragGestures(
                      onDragStart = { startDrag(index) },
                      onDrag = { change, amount ->
                        change.consume()
                        drag(index, amount.y)
                      },
                      onDragEnd = { endDrag(index) },
                      onDragCancel = { cancelDrag() },
                    )
                  },
            ) {
              Icon(
                painterResource(R.drawable.ic_drag_handle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
    item(key = "end") { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

/** The name of a kind of place, for a place without a name of its own. */
private fun placeKindLabel(kindKey: String) =
  when (kindKey) {
    "food" -> R.string.place_food
    "supermarket" -> R.string.place_supermarket
    "bicycle" -> R.string.place_bicycle
    "railway" -> R.string.place_railway
    "toilets" -> R.string.place_toilets
    else -> R.string.place_drinking_water
  }

/**
 * The choices for a tapped place, at the place, see `map-planning` "Choose a route point on the
 * map". Dismissing changes nothing.
 */
@Composable
private fun PlaceOfferMenu(offer: PlaceOffer, viewModel: AppViewModel) {
  Box(modifier = Modifier.offset { IntOffset(offer.x.roundToInt(), offer.y.roundToInt()) }) {
    // Not focusable, so the map can still be moved and the menu follows its flag; a tap on the map
    // or Back closes it.
    BackHandler(onBack = viewModel::dismissOffer)
    DropdownMenu(
      expanded = true,
      onDismissRequest = viewModel::dismissOffer,
      properties = PopupProperties(focusable = false, dismissOnClickOutside = false),
    ) {
      // The tapped place's icon and name, see `map-planning` "Tapping a place on the map".
      offer.offer.place?.let { place ->
        val context = LocalContext.current
        val icon = remember(place.kind) { placeBitmap(place.kind).asImageBitmap() }
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            place.name ?: stringResource(placeKindLabel(place.kindKey)),
            style = MaterialTheme.typography.titleMedium,
          )
        }
        if (!offer.nameOnly) HorizontalDivider()
      }
      if (offer.nameOnly) return@DropdownMenu
      DropdownMenuItem(
        text = { Text(stringResource(R.string.start_here)) },
        onClick = { viewModel.choose(PlaceChoice.START) },
      )
      if (offer.offer.canAddStop) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.add_stop_here)) },
          onClick = { viewModel.choose(PlaceChoice.STOP) },
        )
      }
      DropdownMenuItem(
        text = { Text(stringResource(R.string.end_here)) },
        onClick = { viewModel.choose(PlaceChoice.END) },
      )
    }
  }
}
