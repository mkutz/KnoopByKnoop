package com.knoopbyknoop.android

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.knoopbyknoop.core.FerryDetour
import com.knoopbyknoop.core.HeaderRow
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.KnotPlate
import com.knoopbyknoop.core.KnotPreview
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.MapFeatures
import com.knoopbyknoop.core.NavigationState
import com.knoopbyknoop.core.PathDataStatus
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteSummary
import com.knoopbyknoop.core.StartChoice
import com.knoopbyknoop.core.TapOptions
import com.knoopbyknoop.core.WaypointRole
import com.knoopbyknoop.core.hasStraightLines
import com.knoopbyknoop.core.nameIn
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
  // How high the question about a closed ferry is, so the map shows the detour above it.
  var detourCardHeight by remember { mutableIntStateOf(0) }
  val mapArea = remember {
    movableContentOf { currentState: AppState, modifier: Modifier ->
      MapArea(
        currentState,
        viewModel,
        detourBottomPadding = detourCardHeight,
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
      Column(modifier = Modifier.align(Alignment.BottomCenter)) {
        state.tapNote?.let { TapNoteCard(it, viewModel) }
        SnackbarHost(snackbarHostState)
      }
    }
  } else {
    // While navigating, the sheet is at least as high as the number to follow and the whole strip,
    // with the coming knots above, and starting or ending navigation brings it down to its smallest
    // height, see `knot-navigation` "Panel height while navigating".
    val scaffoldState = rememberBottomSheetScaffoldState()
    var navigationHeight by remember { mutableIntStateOf(0) }
    // Whether a junction picture showed when it was measured: it makes the navigation part taller
    // only while it shows, see `knot-navigation` "Picture in the app".
    var navigationHeightPicture by remember { mutableStateOf(false) }
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
    // A layer above everything, so the note gets its taps, see "Changing what a tapped knot
    // became".
    Box(modifier = Modifier.fillMaxSize()) {
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
              onNavigationHeight = { height, picture ->
                navigationHeight =
                  if (picture == navigationHeightPicture) maxOf(navigationHeight, height)
                  else height
                navigationHeightPicture = picture
              },
              onSummaryHeight = { summaryHeight = it },
              // The sheet comes down, so the knot is not hidden by it, see `knot-navigation`
              // "Showing
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
      var noteHeight by remember { mutableIntStateOf(0) }
      state.tapNote?.let { note ->
        // Right above the panel, wherever it is.
        TapNoteCard(
          note,
          viewModel,
          Modifier.offset {
              val sheetTop = runCatching {
                scaffoldState.bottomSheetState.requireOffset()
              }
                .getOrDefault(0f)
              IntOffset(0, sheetTop.roundToInt() - noteHeight)
            }
            .onSizeChanged { noteHeight = it.height },
        )
      }
    }
  }

  state.startChoice?.let { choice -> StartChoiceDialog(choice, viewModel) }
  state.ferryDetour?.let { detour ->
    FerryDetourQuestion(detour, viewModel, onHeight = { detourCardHeight = it })
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
  detourBottomPadding: Int,
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
          navigating.position?.let { navigating.navigation?.mapFocus(it, navigating.guidedWay) }
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
      detour = state.ferryDetour,
      detourBottomPadding = detourBottomPadding,
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
  onNavigationHeight: (height: Int, picture: Boolean) -> Unit = { _, _ -> },
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
      Column(
        modifier =
          Modifier.onSizeChanged {
            onNavigationHeight(it.height, navigation.guidedWay?.junctionAhead() != null)
          }
      ) {
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
        state.baseMap?.let { BaseMapNote(it) }
        // See `map-planning` "No path data".
        if (session.hasStraightLines) {
          Text(
            stringResource(R.string.straight_ways),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
          )
        }
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
 * What a tapped knot became, in one line, with the buttons start, stop and end, each previewing
 * that role with badges, the current one selected and inactive, and Undo, see `map-planning`
 * "Changing what a tapped knot became". Goes away after 6 seconds, but not while TalkBack is on.
 */
@Composable
private fun TapNoteCard(note: TapNote, viewModel: AppViewModel, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val options = note.options
  LaunchedEffect(note.id) {
    val accessibility =
      context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
        as android.view.accessibility.AccessibilityManager
    if (!accessibility.isTouchExplorationEnabled) {
      kotlinx.coroutines.delay(TAP_NOTE_MILLIS)
      viewModel.dismissTapNote(note.id)
    }
  }
  // The tapped knot pops in where it went.
  val pop = remember(note.id) { Animatable(0.3f) }
  LaunchedEffect(note.id) {
    pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
  }
  // Light like the panel and the map buttons, see "Changing what a tapped knot became".
  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shadowElevation = 6.dp,
    modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 8.dp),
    ) {
      Text(
        stringResource(
          when (options.role) {
            WaypointRole.START -> R.string.tap_note_added_start
            WaypointRole.END -> R.string.tap_note_added_end
            WaypointRole.STOP -> R.string.tap_note_added_stop
          },
          options.knot.ref,
        ),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        for (choice in options.choices) {
          val label =
            stringResource(
              when (choice.role) {
                WaypointRole.START -> R.string.tap_choice_as_start
                WaypointRole.END -> R.string.tap_choice_as_end
                WaypointRole.STOP -> R.string.tap_choice_as_stop
              }
            )
          val selected = MaterialTheme.colorScheme.primary
          OutlinedButton(
            onClick = { viewModel.chooseTap(choice) },
            enabled = !choice.current,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors =
              ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
              ),
            border =
              BorderStroke(
                if (choice.current) 2.dp else 1.dp,
                if (choice.current) selected else MaterialTheme.colorScheme.outlineVariant,
              ),
            modifier =
              Modifier.weight(if (choice.role == WaypointRole.STOP) 1.5f else 1f)
                .heightIn(min = 40.dp)
                .semantics {
                  contentDescription = label
                  this.selected = choice.current
                },
          ) {
            KnotPreviewRow(choice.preview, highlightScale = if (choice.current) pop.value else 1f)
          }
        }
        IconButton(onClick = viewModel::undoTap) {
          Icon(
            painterResource(R.drawable.ic_undo),
            contentDescription = stringResource(R.string.tap_note_undo),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

/**
 * The knots of [preview] as badges, the highlighted one larger by [highlightScale] and ringed, with
 * "…" where the route goes on.
 */
@Composable
private fun KnotPreviewRow(
  preview: KnotPreview,
  modifier: Modifier = Modifier,
  highlightScale: Float = 1f,
) {
  Row(
    // Room for the corner symbols, which reach beyond the badges.
    horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier,
  ) {
    if (preview.moreBefore)
      Text(
        "…",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.clearAndSetSemantics {},
      )
    preview.knots.forEachIndexed { index, (knot, role) ->
      val highlighted = index == preview.highlighted
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          // The knot the preview is about stands out by its size; the ones around it are dimmed.
          if (highlighted)
            Modifier.clearAndSetSemantics {}
              .graphicsLayer {
                scaleX = highlightScale
                scaleY = highlightScale
              }
          else Modifier.clearAndSetSemantics {}.graphicsLayer { alpha = 0.55f },
      ) {
        KnotBadge(
          knot.ref,
          role,
          scale = if (highlighted) 0.85f else 0.66f,
          plate = KnotPlate.of(knot.signRegion),
          place = knot.routePoint?.placeKind,
        )
      }
    }
    if (preview.moreAfter)
      Text(
        "…",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.clearAndSetSemantics {},
      )
  }
}

private const val TAP_NOTE_MILLIS = 6_000L

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
  // The picture of a doubtful junction close ahead, below the row, so the row keeps its height,
  // see `knot-navigation` "Picture in the app".
  val guided = (row as? HeaderRow.ToRoute)?.guided
  val picture = guided?.picture
  if (picture != null) {
    val sentence = guided.pictureSentence.orEmpty()
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier.padding(top = 8.dp).clearAndSetSemantics { contentDescription = sentence },
    ) {
      JunctionPictureView(picture, Modifier.size(104.dp))
      Text(sentence, style = MaterialTheme.typography.titleMedium)
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

/** Whether the route's base map is stored, see `network-sync` "Base map ready for the ride". */
@Composable
private fun BaseMapNote(status: PathDataStatus) {
  Text(
    stringResource(
      when (status) {
        PathDataStatus.LOADING -> R.string.base_map_loading
        PathDataStatus.READY -> R.string.base_map_ready
        PathDataStatus.UNAVAILABLE -> R.string.base_map_unavailable
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
        // A knot by the village, town or city it lies in, see "Waypoint list with badges".
        val locality = knot.place?.nameIn(viewModel.textLanguage)
        val placeName =
          knot.routePoint?.placeName
            ?: placeKind?.let { stringResource(placeKindLabel(it)) }
            ?: locality
        val name =
          when {
            knot.isRoutePoint && placeName != null -> "$roleName, $placeName"
            knot.isRoutePoint -> roleName
            locality != null -> "${knot.ref}, $locality"
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
    "campsite" -> R.string.place_campsite
    "lodging" -> R.string.place_lodging
    else -> R.string.place_drinking_water
  }

/**
 * The start, stop and end buttons for an offered place, each previewing that role with badges, the
 * suggestion ringed, see `map-planning` "Choose a route point on the map".
 */
@Composable
private fun PlaceChoiceButtons(options: TapOptions, viewModel: AppViewModel) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
  ) {
    for (choice in options.choices) {
      val name =
        stringResource(
          when (choice.role) {
            WaypointRole.START -> R.string.start_here
            WaypointRole.END -> R.string.end_here
            WaypointRole.STOP -> R.string.add_stop_here
          }
        )
      val suggested = choice.role == options.suggested
      val label = if (suggested) stringResource(R.string.place_choice_suggested, name) else name
      OutlinedButton(
        onClick = { viewModel.chooseOffer(choice) },
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        border =
          BorderStroke(
            if (suggested) 2.dp else 1.dp,
            if (suggested) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
          ),
        colors =
          ButtonDefaults.outlinedButtonColors(
            containerColor =
              if (suggested) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
          ),
        modifier =
          Modifier.heightIn(min = 40.dp).semantics {
            contentDescription = label
          },
      ) {
        KnotPreviewRow(choice.preview)
      }
    }
  }
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
        offer.details?.let { PlaceDetailsRows(it, viewModel.textLanguage) }
        if (!offer.nameOnly) HorizontalDivider()
      }
      if (offer.nameOnly) return@DropdownMenu
      // Without a route, the place is added as a tapped knot would be; once there is one, the same
      // buttons as after a tapped knot, see "Choose a route point on the map".
      if (!offer.offer.canAddStop) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.add_to_route)) },
          onClick = viewModel::addOfferToRoute,
        )
        return@DropdownMenu
      }
      val options = offer.options
      if (options == null) {
        // The size of the buttons while they are worked out, so the card does not jump.
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp).width(300.dp).height(48.dp),
        ) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
      } else {
        PlaceChoiceButtons(options, viewModel)
      }
    }
  }
}

/**
 * Navigate's question about ferries closed by the time the rider gets there, see `knot-navigation`
 * "Riding around a closed ferry": a card at the bottom without dimming, so the way around stays in
 * view on the map above it, see "Detour on the map".
 */
@Composable
private fun FerryDetourQuestion(
  detour: FerryDetour,
  viewModel: AppViewModel,
  onHeight: (Int) -> Unit,
) {
  Dialog(
    onDismissRequest = { viewModel.dismissFerryDetour() },
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
    Box(
      modifier =
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
      contentAlignment = Alignment.BottomCenter,
    ) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 560.dp).onSizeChanged { onHeight(it.height) },
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Text(
            if (detour.count > 1)
              stringResource(R.string.ferry_detour_title_numbered, detour.number, detour.count)
            else stringResource(R.string.ferry_detour_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
          )
          Spacer(Modifier.height(16.dp))
          for (warning in detour.warnings) {
            Text(warning, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
          }
          if (detour.around == null)
            Text(
              stringResource(R.string.ferry_detour_none),
              style = MaterialTheme.typography.bodyMedium,
            )
          Spacer(Modifier.height(16.dp))
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
          ) {
            if (detour.around != null) {
              TextButton(onClick = { viewModel.rideAroundFerries() }) {
                Text(
                  stringResource(
                    R.string.ferry_detour_around,
                    detour.extraDistance.orEmpty(),
                    detour.extraTime.orEmpty(),
                  )
                )
              }
              TextButton(onClick = { viewModel.keepFerries() }) {
                Text(stringResource(R.string.ferry_detour_keep))
              }
            } else
              TextButton(onClick = { viewModel.keepFerries() }) {
                Text(stringResource(R.string.ferry_detour_navigate))
              }
          }
        }
      }
    }
  }
}

/**
 * Navigate's question away from the start but close to the route, see `knot-navigation` "Start on
 * the route nearby".
 */
@Composable
private fun StartChoiceDialog(choice: StartChoice, viewModel: AppViewModel) {
  val language = viewModel.textLanguage
  val startDistance = MapFeatures.formatDistance(choice.startMeters, language)
  val nearbyDistance = MapFeatures.formatDistance(choice.nearbyMeters, language)
  AlertDialog(
    onDismissRequest = { viewModel.dismissStartChoice() },
    title = { Text(stringResource(R.string.start_choice_title)) },
    text = { Text(stringResource(R.string.start_choice_text)) },
    confirmButton = {
      TextButton(onClick = { viewModel.startNearby() }) {
        Text(stringResource(R.string.start_nearby, nearbyDistance))
      }
    },
    dismissButton = {
      TextButton(onClick = { viewModel.rideToStart() }) {
        Text(
          if (choice.start.isRoutePoint) stringResource(R.string.ride_to_start_point, startDistance)
          else stringResource(R.string.ride_to_start, choice.start.ref, startDistance)
        )
      }
    },
  )
}
