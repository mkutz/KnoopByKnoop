package com.knoopbyknoop.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.knoopbyknoop.core.BaseMapArea
import com.knoopbyknoop.core.BaseMapSquares
import com.knoopbyknoop.core.Ferry
import com.knoopbyknoop.core.FerryDetour
import com.knoopbyknoop.core.Fix
import com.knoopbyknoop.core.GeoBounds
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.Gpx
import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.KnotLocator
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.MapFeatures
import com.knoopbyknoop.core.MapPlace
import com.knoopbyknoop.core.MapTap
import com.knoopbyknoop.core.NavigationRoute
import com.knoopbyknoop.core.NavigationState
import com.knoopbyknoop.core.NetworkCatalog
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkStatus
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.PathDataStatus
import com.knoopbyknoop.core.PathDownloader
import com.knoopbyknoop.core.PathSquares
import com.knoopbyknoop.core.PlaceDetails
import com.knoopbyknoop.core.PlaceDownloader
import com.knoopbyknoop.core.PlaceHit
import com.knoopbyknoop.core.PlaceSearch
import com.knoopbyknoop.core.PlaceSquares
import com.knoopbyknoop.core.PlanningSession
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteKnot
import com.knoopbyknoop.core.RouteSnapshot
import com.knoopbyknoop.core.RouteSummary
import com.knoopbyknoop.core.StartChoice
import com.knoopbyknoop.core.TapChoice
import com.knoopbyknoop.core.TapOptions
import com.knoopbyknoop.core.WayFinder
import com.knoopbyknoop.core.hasStraightLines
import com.knoopbyknoop.core.reattached
import com.knoopbyknoop.core.routePointFor
import com.knoopbyknoop.core.tapAt
import java.time.Instant
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** The map layers of the planner as GeoJSON, see [MapFeatures]. */
data class MapLayers(
  val knots: String,
  val connections: String,
  val route: String,
  val routeKnots: String,
  val routeKnotList: List<RouteKnot>,
  /** The ways of the route points, see `map-planning` "The line on the map". */
  val routePointWays: String,
  val refs: Set<String>,
  /** Whether [connections] follow their course, or are straight lines without geometry. */
  val hasCourses: Boolean,
  /** The installed geometry, to split lines at bridges; `null` without courses. */
  val geometry: NetworkGeometry? = null,
)

/**
 * Everything the planner shows, owned by [AppViewModel].
 *
 * @property network the selected network type
 * @property online whether the device has network access, for the note while courses are loading
 */
/**
 * A tapped place offered as a route point, see `map-planning` "Choose a route point on the map".
 * [x] and [y] are where it was tapped on the map, in pixels, to show the choices there.
 */
data class PlaceOffer(
  val offer: MapTap.Offer,
  val x: Float,
  val y: Float,
  /** Whether only the place's name is shown, without choices, see "Tapping a place on the map". */
  val nameOnly: Boolean = false,
  /**
   * The place's details once found, see `map-planning` "Contacts on the place card"; `null` before
   * or without them.
   */
  val details: PlaceDetails? = null,
)

/**
 * Where a tapped spot is being added, for a spinner there until it is, see "Choose a route point".
 */
data class AddingAt(val x: Float, val y: Float)

/** What to make of an offered place, see `map-planning` "What a chosen route point does". */
/** A [knot] to show on the map; [request] counts the taps, so the same knot can be shown again. */
data class ShownKnot(val knot: Knot, val request: Int)

data class AppState(
  val network: IndexedNetworkType = IndexedNetworkType.CYCLING,
  val catalog: NetworkCatalog = NetworkCatalog(null, listOf()),
  val needsAppUpdate: Boolean = false,
  val online: Boolean = true,
  val session: PlanningSession? = null,
  val layers: MapLayers? = null,
  val description: RouteDescription = RouteDescription.Empty,
  /** Where the map opens: where it was when the app was left last. */
  val initialPlace: CameraPlace? = null,
  val searchQuery: String = "",
  val searchResult: SearchResult = SearchResult.None,
  /** The chosen search hit, shown with a marker on the map. */
  val marker: PlaceHit? = null,
  val message: String? = null,
  /** Whether the [message] offers to open the app's settings, e.g. to allow location access. */
  val messageOpensSettings: Boolean = false,
  val loading: Boolean = false,
  /**
   * The rider's position as found on request, marked on the map without following them, see
   * `map-planning` "Show my position".
   */
  val myPosition: Fix? = null,
  /** Whether the position is being found. */
  val locating: Boolean = false,
  /** Whether the map shows no knots, see `map-planning` "Nearest knots". */
  val noKnotsInView: Boolean = false,
  /** Whether the nearest knots are being found. */
  val findingNearest: Boolean = false,
  /** Where the map is to move for the nearest knots, until it did. */
  val nearestKnots: NearestKnots? = null,
  /** Whether to ask for location access on start: the first start, with nothing else to show. */
  val askLocationOnStart: Boolean = false,
  /** What tapped knots become, see `map-planning` "What new knots become". */
  /** What the last tapped knot became, while the note about it shows. */
  val tapNote: TapNote? = null,
  /** Where a tapped spot or place is being added, while it is worked out. */
  val addingAt: AddingAt? = null,
  /** The appearance chosen in the info sheet, see `map-planning` "Light and dark appearance". */
  val appearance: Appearance = Appearance.SYSTEM,
  /**
   * Whether places useful on a ride are highlighted, see `map-planning` "Switch for places on the
   * map".
   */
  val places: Boolean = true,
  /** How far the path data for the planned route is ready, `null` if none is wanted. */
  val pathData: PathDataStatus? = null,
  /** The data date of the installed path data. */
  val pathDataDate: String? = null,
  /** The space the installed path data takes, in bytes, see `network-sync`. */
  val pathDataBytes: Long = 0,
  /**
   * The course of the route planned when the ways were deleted, until the route changes or
   * navigation starts, see `network-sync` "Deleting with a route planned".
   */
  val pathDataDeletedFor: List<GeoPoint>? = null,
  /** The data date of the installed place details. */
  val placeDetailsDate: String? = null,
  /** The space the installed place details take, in bytes, see `network-sync`. */
  val placeDetailsBytes: Long = 0,
  /**
   * The course of the route planned when the place details were deleted, until the route changes or
   * navigation starts, see `network-sync` "Deleting place details with a route planned".
   */
  val placeDetailsDeletedFor: List<GeoPoint>? = null,
  /** Whether the route's base map is stored, see `network-sync` "Base map ready for the ride". */
  val baseMap: PathDataStatus? = null,
  /** The space the stored base map takes, see "Base map in the info sheet". */
  val baseMapBytes: Long = 0,
  /** The course the user deleted the base map for: not stored again until the route changes. */
  val baseMapDeletedFor: List<GeoPoint>? = null,
  /** The navigation, or `null` when not navigating, see `knot-navigation`. */
  val navigation: NavigationState? = null,
  /**
   * The coming knot last tapped, shown on the map, see `knot-navigation` "Showing a coming knot".
   */
  val shownKnot: ShownKnot? = null,
  /**
   * A tapped place whose choices are shown, see `map-planning` "Choose a route point on the map".
   */
  val offer: PlaceOffer? = null,
  /**
   * Navigate's question whether to ride to the start or start on the route nearby, see
   * `knot-navigation` "Start on the route nearby".
   */
  val startChoice: StartChoice? = null,
  /**
   * Whether Navigate is finding the rider's position, see `knot-navigation` "Start on the route
   * nearby".
   */
  val findingStart: Boolean = false,
  /**
   * Navigate's question whether to ride around a ferry closed by the time the rider gets there, see
   * `knot-navigation` "Riding around a closed ferry".
   */
  val ferryDetour: FerryDetour? = null,
) {
  val status: NetworkStatus
    get() = catalog.status(network)

  val navigating: Boolean
    get() = navigation != null

  /**
   * Whether the route can be navigated: it is complete, see `knot-navigation` "Start navigation".
   */
  val canNavigate: Boolean
    get() = !navigating && description is RouteDescription.Complete
}

/** What the place search shows below its field, see `place-search`. */
sealed interface SearchResult {
  /** Nothing to show: no query, or a hit was chosen. */
  data object None : SearchResult

  data object Searching : SearchResult

  data class Hits(val hits: List<PlaceHit>) : SearchResult

  data object NoHits : SearchResult

  /** Photon did not answer or answered with an error. */
  data object Unavailable : SearchResult
}

/** The installed data of one network, prepared for planning and drawing. */
private class LoadedNetwork(
  val graphSha256: String,
  val hasGeometry: Boolean,
  val geometry: NetworkGeometry,
  val locator: KnotLocator,
  val knots: String,
  val connections: String,
  val refs: Set<String>,
)

/** Connects the planner with storage, the network sync and the core's planning logic. */
class AppViewModel(application: Application) : AndroidViewModel(application) {

  private val storage = NetworkStorage(application.filesDir)
  private val settings = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
  private val paths = PathData.store(application)
  private val places = PlaceDetailsData.store(application)
  private val baseMaps = BaseMapStore(application)

  /** The language of the core's texts, see `app-languages` "Language of the texts". */
  private var language = Language.of(AppLocale.of(application).language)

  /** A message in the app's language, also before Android 13, see [AppLocale]. */
  private fun text(@StringRes id: Int, vararg arguments: Any): String =
    AppLocale.context(getApplication()).getString(id, *arguments)

  private val sync = NetworkSync(application)
  private val connectivity = application.getSystemService(ConnectivityManager::class.java)
  private var loaded: LoadedNetwork? = null
    set(value) {
      field = value
      updateKnotsInView()
    }

  var state by mutableStateOf(AppState())
    private set

  init {
    val camera = storage.camera()
    state =
      state.copy(
        appearance =
          settings.getString(KEY_APPEARANCE, null)?.let { saved ->
            Appearance.entries.firstOrNull { it.name == saved }
          } ?: Appearance.SYSTEM,
        places = settings.getBoolean(KEY_PLACES, true),
        pathDataDate = paths.dataDate,
        initialPlace = camera,
        askLocationOnStart = camera == null && storage.route()?.waypoints.isNullOrEmpty(),
      )
    viewModelScope.launch {
      withContext(Dispatchers.IO) { sync.installBundled() }
      refreshCatalog()
      restoreRoute()
      // At every start, see `network-sync` "Keep the networks current".
      NetworkSyncWorker.enqueue(application)
      // Only after the route is restored, so a reload cannot replace it with an empty one.
      watchSync()
    }
    connectivity.registerDefaultNetworkCallback(
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          viewModelScope.launch { state = state.copy(online = true) }
        }

        override fun onLost(network: Network) {
          viewModelScope.launch { state = state.copy(online = false) }
        }
      }
    )
    state = state.copy(online = connectivity.activeNetwork != null)
    viewModelScope.launch {
      Navigating.state.collect {
        val ended = state.navigating && it == null
        state = state.copy(navigation = it)
        if (ended) restorePlan()
      }
    }
    viewModelScope.launch { loadPathData() }
    viewModelScope.launch { loadPlaceDetails() }
    viewModelScope.launch { loadBaseMap() }
  }

  /**
   * Keeps the place details around the planned route on the device, for cycling and hiking, see
   * `network-sync` "Place details around the route": like the path data, 2 seconds after the route
   * last changed.
   */
  private suspend fun loadPlaceDetails() {
    updatePlaceDetailsInfo()
    snapshotFlow {
      listOf(state.online, state.session, state.layers, state.placeDetailsDeletedFor)
    }
      .distinctUntilChanged()
      .collectLatest {
        val course =
          PlaceSquares.courseToLoad(plannedCourse(), state.placeDetailsDeletedFor)
            ?: return@collectLatest
        delay(PATH_DATA_DELAY_MILLIS)
        val job = currentCoroutineContext()
        withContext(Dispatchers.IO) {
          PlaceDownloader(places, HttpPathDataOrigin())
            .loadAround(course, System.currentTimeMillis(), isCancelled = { !job.isActive })
        }
        job.ensureActive()
        updatePlaceDetailsInfo()
        refreshOfferDetails()
      }
  }

  /**
   * Stores the base map around the planned route, for cycling and hiking, see `network-sync` "Base
   * map around the route": like the path data, 2 seconds after the route last changed, again with
   * network access coming back, and at navigation start.
   */
  private suspend fun loadBaseMap() {
    state = state.copy(baseMapBytes = baseMaps.installedBytes())
    snapshotFlow { listOf(state.online, state.session, state.layers, state.baseMapDeletedFor) }
      .distinctUntilChanged()
      .collectLatest {
        val area =
          BaseMapSquares.courseToLoad(plannedCourse(), state.baseMapDeletedFor)?.let {
            BaseMapArea.of(it)
          }
        if (area == null) {
          state = state.copy(baseMap = null)
          return@collectLatest
        }
        delay(PATH_DATA_DELAY_MILLIS)
        val status =
          baseMaps.store(area, state.online, onStatus = { state = state.copy(baseMap = it) })
        state = state.copy(baseMap = status, baseMapBytes = baseMaps.installedBytes())
      }
  }

  /**
   * Deletes the stored base map; the planned route's comes again once it changes or navigation
   * starts, see `network-sync` "Base map in the info sheet".
   */
  fun deleteBaseMap() {
    // Changing the deleted-for course restarts the flow in loadBaseMap, cancelling a download.
    state =
      state.copy(baseMap = null, baseMapBytes = 0, baseMapDeletedFor = plannedCourse() ?: listOf())
    viewModelScope.launch {
      baseMaps.clear()
      state = state.copy(baseMapBytes = baseMaps.installedBytes())
    }
  }

  private suspend fun updatePlaceDetailsInfo() {
    val bytes = withContext(Dispatchers.IO) { places.installedBytes() }
    state = state.copy(placeDetailsBytes = bytes, placeDetailsDate = places.dataDate)
  }

  /**
   * Shows the details of the offered place, from the installed squares; while planning, its square
   * is downloaded if it's missing, see `network-sync` "Place details for a tapped place".
   */
  private fun lookUpDetails(place: MapPlace) {
    viewModelScope.launch {
      refreshOfferDetails()
      val square =
        PlaceSquares.squareToLoad(place.position, state.navigating, places) ?: return@launch
      if (!state.online) return@launch
      withContext(Dispatchers.IO) {
        PlaceDownloader(places, HttpPathDataOrigin())
          .loadSquare(square, plannedCourse(), System.currentTimeMillis())
      }
      updatePlaceDetailsInfo()
      refreshOfferDetails()
    }
  }

  /** The offered place's details as installed now, see "Details arriving after the tap". */
  private suspend fun refreshOfferDetails() {
    val place = state.offer?.offer?.place ?: return
    val details = withContext(Dispatchers.IO) { places.detailsOf(place) }
    val offer = state.offer ?: return
    if (offer.offer.place == place && offer.details != details) {
      state = state.copy(offer = offer.copy(details = details))
    }
  }

  /**
   * Deletes the downloaded place details; they come again for a changed route, a place tapped while
   * planning, or when navigation starts, see `network-sync` "Place details in the info sheet".
   */
  fun deletePlaceDetails() {
    state =
      state.copy(
        placeDetailsDate = null,
        placeDetailsBytes = 0,
        placeDetailsDeletedFor = plannedCourse() ?: listOf(),
        offer = state.offer?.copy(details = null),
      )
    viewModelScope.launch(Dispatchers.IO) { places.clear() }
  }

  /** The language of the core's texts, e.g. for a place's opening status. */
  val textLanguage: Language
    get() = language

  /**
   * Keeps the squares around the planned route on the device, see `network-sync` "Path data around
   * the route": requests start 2 seconds after the route last changed, and start over with a new
   * route, after deleting the ways, or with network access coming back. A download running when
   * navigation starts goes on, as only the route decides what is requested.
   */
  private suspend fun loadPathData() {
    // Read before copying: a state read before suspending would undo what changed meanwhile.
    val installedBytes = withContext(Dispatchers.IO) { paths.installedBytes() }
    state = state.copy(pathDataBytes = installedBytes)
    snapshotFlow { listOf(state.online, state.session, state.layers, state.pathDataDeletedFor) }
      .distinctUntilChanged()
      .collectLatest {
        val course =
          PathSquares.courseToLoad(
            state.session?.network,
            plannedCourse(),
            state.pathDataDeletedFor,
          )
        if (course == null) {
          state = state.copy(pathData = null)
          return@collectLatest
        }
        delay(PATH_DATA_DELAY_MILLIS)
        val job = currentCoroutineContext()
        val status =
          withContext(Dispatchers.IO) {
            PathDownloader(paths, HttpPathDataOrigin())
              .load(
                course,
                System.currentTimeMillis(),
                isCancelled = { !job.isActive },
                onStatus = { status ->
                  viewModelScope.launch { state = state.copy(pathData = status) }
                },
              )
          }
        job.ensureActive()
        val bytes = withContext(Dispatchers.IO) { paths.installedBytes() }
        state = state.copy(pathData = status, pathDataDate = paths.dataDate, pathDataBytes = bytes)
        // The squares around a straight line may have arrived, see "While the path data loads".
        if (status == PathDataStatus.READY) findWays()
      }
  }

  /** The course of the complete planned route, `null` without one or without courses. */
  private fun plannedCourse(): List<GeoPoint>? {
    val network = loaded?.takeIf { it.hasGeometry } ?: return null
    val route = state.session?.route ?: return null
    return if (route.summary is RouteSummary.Complete) {
      network.geometry.withRoutePoints(state.session!!.routeGraph).courseOf(route)
    } else {
      null
    }
  }

  /**
   * Deletes the downloaded ways; the planned route's squares come again once it changes or
   * navigation starts, see `network-sync` "Path data in the info sheet".
   */
  fun deletePathData() {
    // Changing the deleted-for course restarts the flow in loadPathData, cancelling a download.
    state =
      state.copy(
        pathData = null,
        pathDataDate = null,
        pathDataBytes = 0,
        pathDataDeletedFor = plannedCourse() ?: listOf(),
      )
    viewModelScope.launch(Dispatchers.IO) { paths.clear() }
  }

  /**
   * Starts navigating the planned route; location access must be granted. Away from the start but
   * close to the route, asks first, see `knot-navigation` "Start on the route nearby": with one
   * position, given up after [START_POSITION_TIMEOUT_MILLIS], then navigation starts without
   * asking.
   */
  fun startNavigation() {
    val session = state.session ?: return
    val network = loaded ?: return
    if (session.route == null || state.findingStart) return
    // Shown on Navigate until the question or navigation takes over, see "Waiting for the
    // position"; a recent rough fix is enough to decide, see "Recent position".
    // The note and the other planning tools go at once, see "Navigate closes the note" and
    // "Planning tools close on Navigate": a spot being added is dropped by the note's id, a pending
    // map move by [planningMoves].
    tapNotes++
    planningMoves++
    searchJob?.cancel()
    state =
      state.copy(
        findingStart = true,
        tapNote = null,
        offer = null,
        addingAt = null,
        marker = null,
        searchQuery = "",
        searchResult = SearchResult.None,
        locating = false,
        findingNearest = false,
        nearestKnots = null,
      )
    viewModelScope.launch {
      val location =
        withTimeoutOrNull(START_POSITION_TIMEOUT_MILLIS) {
            currentLocation(
              CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(START_POSITION_MAX_AGE_MILLIS)
                .setDurationMillis(START_POSITION_TIMEOUT_MILLIS)
                .build()
            )
          }
          ?.takeIf { it.hasAccuracy() && it.accuracy <= START_POSITION_ACCURACY_METERS }
      state = state.copy(findingStart = false)
      val choice = location?.let {
        StartChoice.of(
          session,
          network.geometry.takeIf { network.hasGeometry },
          GeoPoint(it.latitude, it.longitude),
        )
      }
      Log.i("Navigation", "start choice: ${location != null}, ${choice?.nearbyMeters}")
      startPosition = location?.let { GeoPoint(it.latitude, it.longitude) }
      if (choice == null) checkFerries() else state = state.copy(startChoice = choice)
    }
  }

  /** Where the rider was when Navigate was pressed, for the ferries' arrival, or `null`. */
  private var startPosition: GeoPoint? = null

  /**
   * Asks about ferries closed by the time the rider gets there before navigating, see
   * `knot-navigation` "Riding around a closed ferry"; navigates at once when none is.
   */
  private fun checkFerries(kept: Set<Ferry> = setOf(), asked: Int = 0) {
    val session = state.session ?: return
    val network = loaded ?: return
    val position = startPosition
    keptFerries = kept
    ferriesAsked = asked
    val detour = position?.let {
      FerryDetour.of(
        session,
        network.geometry.takeIf { network.hasGeometry },
        it,
        System.currentTimeMillis(),
        { millis -> ferryMoment(Instant.ofEpochMilli(millis)) },
        language,
        kept,
        asked,
      )
    }
    Log.i("Navigation", "ferry detour: ${detour?.warnings?.size}, ${detour?.extraDistance}")
    if (detour == null) navigate() else state = state.copy(ferryDetour = detour)
  }

  /** The ferries kept and the number of ferry questions asked since Navigate was pressed. */
  private var keptFerries: Set<Ferry> = setOf()
  private var ferriesAsked = 0

  /**
   * Rides around the ferry asked about, for this navigation only, then asks about the next closed
   * ferry or navigates, see "Riding around it" and "Several closed ferries".
   */
  fun rideAroundFerries() {
    val around = state.ferryDetour?.around ?: return
    state = state.copy(ferryDetour = null)
    showForNavigation(around)
    checkFerries(keptFerries, ferriesAsked + 1)
  }

  /**
   * Keeps the ferry asked about, then asks about the next closed ferry or navigates, see "Keeping
   * the ferry".
   */
  fun keepFerries() {
    val ferry = state.ferryDetour?.ferry ?: return
    state = state.copy(ferryDetour = null)
    checkFerries(keptFerries + ferry, ferriesAsked + 1)
  }

  /**
   * The question dismissed: nothing starts, and a start nearby is undone, see "Question dismissed".
   */
  fun dismissFerryDetour() {
    state = state.copy(ferryDetour = null)
    restorePlan()
  }

  /** Rides to the plan's start, see `knot-navigation` "Riding to the start". */
  fun rideToStart() {
    state = state.copy(startChoice = null)
    checkFerries()
  }

  /** Starts on the route nearby for this navigation only, then navigates, see "Starting nearby". */
  fun startNearby() {
    val choice = state.startChoice ?: return
    val session = state.session ?: return
    state = state.copy(startChoice = null)
    showForNavigation(session.startOnRouteAt(choice.nearby))
    checkFerries()
  }

  /**
   * The plan as made while a route for one navigation is shown, see `knot-navigation` "The plan
   * after navigation"; `null` otherwise.
   */
  private var plannedSession: PlanningSession? = null

  /** Shows [session], a route for one navigation, without storing it. */
  private fun showForNavigation(session: PlanningSession) {
    if (loaded == null || state.navigating) return
    if (plannedSession == null) plannedSession = state.session
    show(session)
  }

  /**
   * Shows the plan as made again after a route for one navigation, see "The plan after navigation".
   */
  private fun restorePlan() {
    val planned = plannedSession ?: return
    plannedSession = null
    show(planned)
  }

  /** The question dismissed: nothing starts, see "Question dismissed". */
  fun dismissStartChoice() {
    state = state.copy(startChoice = null)
  }

  private fun navigate() {
    val session = state.session ?: return
    val knots = (session.route?.summary as? RouteSummary.Complete)?.knots ?: return
    val network = loaded ?: return
    // The route's squares are wanted again, see `network-sync` "Deleting with a route planned".
    state =
      state.copy(pathDataDeletedFor = null, placeDetailsDeletedFor = null, baseMapDeletedFor = null)
    Navigating.lateCourses.value = null
    NavigationService.start(
      getApplication(),
      NavigationRoute(
        knots,
        session.routeGraph,
        network.geometry.takeIf { network.hasGeometry }?.withRoutePoints(session.routeGraph),
        (state.description as? RouteDescription.Complete)?.stopPositions ?: setOf(),
      ),
    )
  }

  /**
   * Writes the core's texts in [newLanguage] from now on, e.g. after the app's language changed.
   */
  fun useLanguage(newLanguage: Language) {
    if (newLanguage == language) return
    language = newLanguage
    state =
      state.copy(
        description = state.session?.let { MapFeatures.describe(it, language) } ?: state.description
      )
  }

  fun stopNavigation() {
    NavigationService.stop(getApplication())
  }

  /**
   * Shows the coming [knot] on the map, see `knot-navigation` "Showing a coming knot on the map".
   */
  fun showOnMap(knot: Knot) {
    state = state.copy(shownKnot = ShownKnot(knot, (state.shownKnot?.request ?: 0) + 1))
  }

  /** The user did not grant location access, see `knot-navigation` "Location access denied". */
  fun locationDenied() {
    state = state.copy(message = text(R.string.needs_location_navigate))
  }

  /** The app is left: keeps the map's place and forgets the position found on request. */
  fun saveCamera(place: CameraPlace) {
    storage.saveCamera(place)
    state = state.copy(myPosition = null)
  }

  /**
   * One of the other roles in the note, see `map-planning` "Changing what a tapped knot became".
   */
  fun chooseTap(choice: TapChoice) {
    val note = state.tapNote ?: return
    // Stays with the choice selected and its time started again, so a mistap can be put right.
    state = state.copy(tapNote = TapNote(note.options.chosen(choice), note.before, ++tapNotes))
    update(choice.session)
  }

  /** "Undo" in the note: the route as it was before the tap. */
  fun undoTap() {
    val note = state.tapNote ?: return
    state = state.copy(tapNote = null)
    update(note.before)
  }

  /** The note's time is up, unless a newer note took its place. */
  fun dismissTapNote(id: Long) {
    if (state.tapNote?.id == id) state = state.copy(tapNote = null)
  }

  private var tapNotes = 0L

  /** Chooses and keeps the [appearance], see `map-planning` "Light and dark appearance". */
  fun chooseAppearance(appearance: Appearance) {
    settings.edit().putString(KEY_APPEARANCE, appearance.name).apply()
    state = state.copy(appearance = appearance)
  }

  /**
   * Turns the highlighted places on or off and keeps the choice, see `map-planning` "Switch for
   * places on the map".
   */
  fun choosePlaces(places: Boolean) {
    settings.edit().putBoolean(KEY_PLACES, places).apply()
    state = state.copy(places = places)
  }

  /** The start's question for location access was asked, see "Show my position". */
  fun askedLocationOnStart() {
    state = state.copy(askLocationOnStart = false)
  }

  /**
   * Finds the rider's position once, see `map-planning` "Show my position": a single fix, given up
   * after [LOCATE_TIMEOUT_MILLIS]; location access must be granted.
   */
  fun locate() {
    if (state.locating || state.findingNearest) return
    state = state.copy(locating = true)
    val move = ++planningMoves
    viewModelScope.launch {
      val location = withTimeoutOrNull(LOCATE_TIMEOUT_MILLIS) { currentLocation() }
      if (planningMoves != move) return@launch
      state =
        if (location == null)
          state.copy(locating = false, message = text(R.string.position_not_found))
        else
          state.copy(
            locating = false,
            myPosition = location.toFix(),
          )
    }
  }

  /**
   * Counts "Show my position" and "Nearest knots" on; Navigate counts it on too, so a map move
   * asked for before is not made, see "Planning tools close on Navigate".
   */
  private var planningMoves = 0L

  private fun Location.toFix() = Fix(latitude, longitude, accuracy.toDouble(), time)

  /**
   * Moves the map to the nearest knots, from the rider's position where location access is allowed,
   * otherwise from the middle of the map, without asking, see `map-planning` "Nearest knots".
   */
  fun showNearestKnots() {
    if (state.locating || state.findingNearest) return
    val application = getApplication<Application>()
    val allowed =
      listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any {
          ContextCompat.checkSelfPermission(application, it) == PackageManager.PERMISSION_GRANTED
        }
    if (!allowed) {
      moveToNearestKnots(rider = null)
      return
    }
    state = state.copy(findingNearest = true)
    val move = ++planningMoves
    viewModelScope.launch {
      val location = withTimeoutOrNull(LOCATE_TIMEOUT_MILLIS) { currentLocation() }
      if (planningMoves != move) return@launch
      state = state.copy(findingNearest = false)
      moveToNearestKnots(rider = location?.toFix())
    }
  }

  private fun moveToNearestKnots(rider: Fix?) {
    val from = rider?.let { GeoPoint(it.latitude, it.longitude) }
    val center = mapCenter ?: state.initialPlace
    val knot =
      loaded
        ?.locator
        ?.nearestPosition(
          from?.latitude ?: center?.latitude ?: return,
          from?.longitude ?: center?.longitude ?: return,
        ) ?: return
    state =
      state.copy(
        myPosition = rider ?: state.myPosition,
        nearestKnots = NearestKnots(knot, rider),
      )
  }

  fun nearestKnotsShown() {
    state = state.copy(nearestKnots = null)
  }

  /**
   * The user did not grant location access for their position; [permanently] if not asked again.
   */
  fun positionDenied(permanently: Boolean) {
    state =
      state.copy(
        message = text(R.string.needs_location_position),
        messageOpensSettings = permanently,
      )
  }

  @SuppressLint("MissingPermission") // Granted before locate() is called.
  private suspend fun currentLocation(
    request: CurrentLocationRequest =
      CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
  ): Location? = suspendCancellableCoroutine { continuation ->
    val cancellation = CancellationTokenSource()
    try {
      LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        .getCurrentLocation(request, cancellation.token)
        .addOnSuccessListener { continuation.resume(it) }
        .addOnFailureListener { continuation.resume(null) }
    } catch (exception: SecurityException) {
      continuation.resume(null)
    }
    continuation.invokeOnCancellation { cancellation.cancel() }
  }

  private var searchJob: Job? = null
  private var mapCenter: CameraPlace? = null
  private var mapArea: GeoBounds? = null

  /** The map's current place, to rank search hits near it. */
  fun mapMoved(place: CameraPlace, area: GeoBounds?) {
    mapCenter = place
    mapArea = area
    updateKnotsInView()
  }

  private fun updateKnotsInView() {
    val none = mapCenter != null && loaded?.locator?.noKnotsIn(mapArea) == true
    if (none != state.noKnotsInView) state = state.copy(noKnotsInView = none)
  }

  /**
   * Searches [query] once the user stopped typing for a moment; newer queries replace older ones.
   */
  fun search(query: String) {
    searchJob?.cancel()
    state = state.copy(searchQuery = query)
    val locator = loaded?.locator
    val area = state.session?.graph?.let(GeoBounds::of)
    val center = mapCenter ?: state.initialPlace
    if (query.trim().length < PlaceSearch.MIN_QUERY_LENGTH || locator == null || area == null) {
      state = state.copy(searchResult = SearchResult.None)
      return
    }
    searchJob = viewModelScope.launch {
      delay(SEARCH_DELAY_MILLIS)
      state = state.copy(searchResult = SearchResult.Searching)
      val requests =
        PlaceSearch.requests(
          query,
          center?.latitude ?: (area.minLatitude + area.maxLatitude) / 2,
          center?.longitude ?: (area.minLongitude + area.maxLongitude) / 2,
          area,
          // The app's language, as its texts, see `place-search` "Hits in the app's language".
          language.name.lowercase(),
        )
      val result =
        try {
          val (prominent, nearby) =
            coroutineScope {
              listOf(requests.prominent, requests.nearby)
                .map { url -> async { runCatching { PlaceSearchClient.fetch(url) }.getOrNull() } }
                .awaitAll()
            }
          val hits =
            withContext(Dispatchers.Default) { PlaceSearch.merge(prominent, nearby, locator) }
          if (hits.isEmpty()) SearchResult.NoHits else SearchResult.Hits(hits)
        } catch (exception: IllegalArgumentException) {
          // Neither request was answered.
          SearchResult.Unavailable
        }
      state = state.copy(searchResult = result)
    }
  }

  /** Moves the map to [hit] and marks it; the route stays as it is. */
  fun choose(hit: PlaceHit) {
    searchJob?.cancel()
    state = state.copy(searchQuery = hit.name, searchResult = SearchResult.None, marker = hit)
  }

  fun clearSearch() {
    searchJob?.cancel()
    state = state.copy(searchQuery = "", searchResult = SearchResult.None, marker = null)
  }

  fun dismissMessage() {
    state = state.copy(message = null, messageOpensSettings = false)
  }

  /** Retries a failed download right away instead of waiting for the next attempt. */
  fun retrySync() {
    NetworkSyncWorker.enqueue(getApplication(), restart = true)
  }

  fun switchNetwork(network: IndexedNetworkType) {
    if (state.navigating) return
    storage.saveRoute(null)
    // The map stays until the other network is loaded, so it keeps its place; without a loaded
    // network, taps are ignored meanwhile.
    loaded = null
    state = state.copy(network = network)
    viewModelScope.launch { loadNetwork(network, waypointsFrom = { null }) }
  }

  /** Handles a tap on the map; [radiusMeters] is the touch target at the current zoom level. */
  fun tap(
    latitude: Double,
    longitude: Double,
    radiusMeters: Double,
    x: Float,
    y: Float,
    place: MapPlace? = null,
  ) {
    // A tap beside the choices closes them, see `map-planning` "Offer and dismiss".
    if (state.offer != null) {
      state = state.copy(offer = null)
      return
    }
    val session = state.session ?: return
    val network = loaded ?: return
    // Any badge of a knot, see `map-planning` "Knots signposted at several places".
    val knot = network.locator.nearestAtAnyNode(latitude, longitude, radiusMeters)
    val position = GeoPoint(latitude, longitude)
    when (val tapped = session.tapAt(position, radiusMeters, knot, network.geometry, place)) {
      is MapTap.OnKnot -> {
        val after = session.tap(tapped.knot)
        update(after)
        state = state.copy(tapNote = null)
        // A note only for a knot added to a route, with the other roles' routes worked out off the
        // main thread, see "Changing what a tapped knot became".
        val id = ++tapNotes
        viewModelScope.launch {
          val options =
            withContext(Dispatchers.Default) { TapOptions.of(session, after, tapped.knot) }
          if (options != null && state.session === after && tapNotes == id) {
            state = state.copy(tapNote = TapNote(options, session, id))
          }
        }
      }
      is MapTap.OnRoutePoint -> {}
      // While navigating, a place shows its name only, see `map-planning` "Place while navigating".
      is MapTap.Offer -> {
        // A spot or a place offers "Add to route" first, see "Choose a route point on the map" and
        // "Tapping a place on the map".
        if (!state.navigating) state = state.copy(offer = PlaceOffer(tapped, x, y))
        else if (tapped.place != null) {
          state = state.copy(offer = PlaceOffer(tapped, x, y, nameOnly = true))
        }
        tapped.place?.let(::lookUpDetails)
      }
      is MapTap.PlaceOnly -> {
        state =
          state.copy(
            offer =
              PlaceOffer(
                MapTap.Offer(tapped.place.position, null, false, tapped.place),
                x,
                y,
                nameOnly = true,
              )
          )
        lookUpDetails(tapped.place)
      }
      // See `map-planning` "Too far from the network".
      is MapTap.TooFar ->
        if (!state.navigating) {
          val kilometers = (tapped.limitMeters / 1000).toInt()
          state =
            state.copy(
              message =
                when (session.network) {
                  NetworkType.CYCLING -> text(R.string.no_cycling_knots_within, kilometers)
                  NetworkType.HIKING -> text(R.string.no_hiking_knots_within, kilometers)
                }
            )
        }
    }
  }

  /** Keeps the offered place's choices at its flag while the map moves. */
  fun offerMoved(x: Float, y: Float) {
    val offer = state.offer ?: return
    if (offer.x != x || offer.y != y) state = state.copy(offer = offer.copy(x = x, y = y))
  }

  fun dismissOffer() {
    state = state.copy(offer = null)
  }

  /**
   * Adds a route point for [tapped] where it fits, as a tapped knot, off the main thread, then
   * shows the note, see `map-planning` "Choose a route point on the map". A later tap cancels it; a
   * route changed meanwhile, e.g. by the courses coming in, is worked out again.
   */
  private fun addPoint(session: PlanningSession, tapped: MapTap.Offer, x: Float, y: Float) {
    val geometry = loaded?.geometry ?: return
    val id = ++tapNotes
    state = state.copy(tapNote = null, addingAt = AddingAt(x, y))
    viewModelScope.launch {
      val added =
        withContext(Dispatchers.Default) {
          session.routePointFor(tapped, geometry)?.let { point -> point to session.placed(point) }
        }
      if (tapNotes != id) return@launch
      state = state.copy(addingAt = null)
      val (point, after) = added ?: return@launch
      val current = state.session
      if (current !== session) {
        if (current != null && !state.navigating) addPoint(current, tapped, x, y)
        return@launch
      }
      update(after)
      findWays()
      val options = withContext(Dispatchers.Default) { TapOptions.of(session, after, point) }
      if (options != null && tapNotes == id) {
        state = state.copy(tapNote = TapNote(options, session, id))
      }
    }
  }

  /** "Add to route" for a spot or a place, see "Choose a route point on the map". */
  fun addOfferToRoute() {
    val offer = state.offer ?: return
    state = state.copy(offer = null)
    val session = state.session ?: return
    addPoint(session, offer.offer, offer.x, offer.y)
  }

  /** Finds ways along paths in one place at a time, as the router is not thread safe. */
  private val wayFinding = Mutex()
  private val wayFinder by lazy { WayFinder(paths) }

  /**
   * Replaces the straight lines of the route points by ways along paths where the installed path
   * data has them, off the main thread, see `map-planning` "A route point away from the network".
   */
  private fun findWays() {
    val session = state.session ?: return
    if (!session.hasStraightLines || state.navigating) return
    val geometry = loaded?.takeIf { it.hasGeometry }?.geometry ?: return
    viewModelScope.launch {
      val found = wayFinding.withLock {
        withContext(Dispatchers.IO) {
          // Squares arrived since are read again, see "While the path data loads".
          val ways = wayFinder.routePointWays(geometry)
          session.routePoints.filter { it.routePoint?.straight == true }.mapNotNull(ways::wayFor)
        }
      }
      // Onto the session as it is now: the route may have changed meanwhile.
      val current = state.session ?: return@launch
      if (found.isEmpty() || state.navigating) return@launch
      val updated = found.fold(current) { next, point -> next.withRoutePoint(point) }
      if (updated !== current) update(updated)
    }
  }

  fun moveWaypoint(from: Int, to: Int) {
    state.session?.let { update(it.moveWaypoint(from, to)) }
  }

  fun removeWaypoint(position: Int) {
    state.session?.takeIf { it.canRemoveWaypoints }?.let { update(it.removeWaypoint(position)) }
  }

  /**
   * The course of the planned route along the network, or `null` while it cannot be exported, see
   * `map-planning` "Export the route as GPX": not while navigating, and not as straight lines.
   */
  private val exportCourse: List<GeoPoint>?
    get() {
      val session = state.session ?: return null
      val route = session.route ?: return null
      val network = loaded?.takeIf { it.hasGeometry && !state.navigating } ?: return null
      // Once per route and network: the waypoint sheet asks on every redraw, and building the
      // course took over 100 ms on the iPhone, which made its sheet stutter while it moved.
      exportCourseCache
        ?.takeIf { it.session === session && it.geometry === network.geometry }
        ?.let {
          return it.course
        }
      val course =
        network.geometry.withRoutePoints(session.routeGraph).courseOf(route)?.takeIf {
          Gpx.canExport(route, it)
        }
      exportCourseCache = ExportCourse(session, network.geometry, course)
      return course
    }

  /** The last [exportCourse], with the session and the network's geometry it was built for. */
  private var exportCourseCache: ExportCourse? = null

  private class ExportCourse(
    val session: PlanningSession,
    val geometry: NetworkGeometry,
    val course: List<GeoPoint>?,
  )

  /** Whether the planned route can be exported as GPX. */
  val canExportGpx: Boolean
    get() = exportCourse != null

  /** The GPX file of the planned route as its name and content, see [canExportGpx]. */
  fun gpx(): Pair<String, String>? {
    val route = state.session?.route ?: return null
    val course = exportCourse ?: return null
    return Gpx.fileName(route) to Gpx.write(route, course)
  }

  fun clear() {
    state.session?.let { update(it.clear()) }
  }

  fun returnToStart() {
    state.session?.takeIf { it.canReturnToStart }?.let { update(it.returnToStart()) }
  }

  private fun update(session: PlanningSession) {
    // While another network loads, the shown route belongs to the previous one. While navigating,
    // the route is fixed, see `knot-navigation` "Route is fixed while navigating".
    if (loaded == null || state.navigating) return
    // An edit makes the shown route the plan.
    plannedSession = null
    storage.saveRoute(RouteSnapshot.of(session))
    show(session)
  }

  private fun show(session: PlanningSession) {
    state =
      state.copy(
        session = session,
        layers = layersFor(session),
        description = MapFeatures.describe(session, language),
      )
  }

  private suspend fun restoreRoute() {
    val snapshot = storage.route()
    val network = snapshot?.network ?: IndexedNetworkType.CYCLING
    state = state.copy(network = network)
    loadNetwork(network, waypointsFrom = { snapshot })
  }

  /**
   * Loads a network off the main thread: reading it and preparing its map layers takes time.
   *
   * @param waypointsFrom the route to continue with, e.g. after a restart or a data update; read
   *   once the network is prepared
   */
  private suspend fun loadNetwork(
    network: IndexedNetworkType,
    waypointsFrom: () -> RouteSnapshot?,
  ) {
    state = state.copy(loading = state.session == null)
    // Step 1, the graph: knots, straight connections and the route, so planning can start.
    val prepared =
      withContext(Dispatchers.Default) {
        val graphEntry = storage.graphEntry(network) ?: return@withContext null
        val graphFile = storage.graph(network) ?: return@withContext null
        graphFile to
          LoadedNetwork(
            graphSha256 = graphEntry.sha256,
            hasGeometry = false,
            geometry = NetworkGeometry.straightLines(graphFile.graph),
            locator = KnotLocator(graphFile.graph.knots.values),
            knots =
              storage
                .mapLayer(network, "knot-badges", withGeometry = false) {
                  MapFeatures.knots(graphFile.graph)
                }
                .readText(),
            connections =
              storage
                .mapLayer(network, "lines", withGeometry = false) {
                  MapFeatures.connections(NetworkGeometry.straightLines(graphFile.graph))
                }
                .readText(),
            refs = graphFile.graph.knots.values.map { it.ref }.toSet(),
          )
      }
    // Read only now, so changes to the route while the network was prepared are kept.
    val snapshot = waypointsFrom()
    val result = prepared?.let { (graphFile, loadedNetwork) ->
      withContext(Dispatchers.Default) {
        val restored =
          if (snapshot != null && snapshot.network == network) snapshot.restore(graphFile.graph)
          else null
        Triple(
          loadedNetwork,
          restored?.session ?: PlanningSession.start(graphFile.graph),
          restored?.removedWaypoints,
        )
      }
    }
    if (result == null || state.network != network) {
      state = state.copy(loading = false)
      return
    }
    val (loadedNetwork, session, removedWaypoints) = result
    loaded = loadedNetwork
    // The restored route is the plan as made, see `knot-navigation` "The plan after navigation".
    plannedSession = null
    state =
      state.copy(
        loading = false,
        session = session,
        layers = layersFor(session),
        description = MapFeatures.describe(session, language),
        message =
          removedWaypoints
            ?.takeIf { it.isNotEmpty() }
            ?.let {
              text(R.string.waypoint_removed, it.joinToString())
            } ?: state.message,
      )
    addCourses(network, prepared.first, loadedNetwork)
  }

  /**
   * Step 2 of [loadNetwork], the geometry: the connections and the route are redrawn along their
   * course. The session stays, so planning goes on, and so does the map's place.
   */
  private suspend fun addCourses(
    network: IndexedNetworkType,
    graphFile: GraphFile,
    withoutCourses: LoadedNetwork,
  ) {
    val withCourses =
      withContext(Dispatchers.Default) {
        val geometry = storage.geometry(network, graphFile) ?: return@withContext null
        LoadedNetwork(
          graphSha256 = withoutCourses.graphSha256,
          hasGeometry = true,
          geometry = geometry,
          locator = withoutCourses.locator,
          knots = withoutCourses.knots,
          connections =
            storage
              .mapLayer(network, "courses", withGeometry = true) {
                MapFeatures.connections(geometry)
              }
              .readText(),
          refs = withoutCourses.refs,
        )
      } ?: return
    // Another network or newer data may have been loaded meanwhile.
    if (loaded !== withoutCourses) return
    loaded = withCourses
    state.session?.let { session ->
      // Route points whose connection went away join the courses anew, see "Connection gone".
      val reattached = session.reattached(withCourses.geometry)
      val planned = plannedSession
      if (planned != null) {
        // Only the plan as made is stored, see `knot-navigation` "The plan after navigation".
        val plannedReattached = planned.reattached(withCourses.geometry)
        plannedSession = plannedReattached
        if (plannedReattached !== planned) storage.saveRoute(RouteSnapshot.of(plannedReattached))
        if (!state.navigating) show(reattached) else state = state.copy(layers = layersFor(session))
      } else if (reattached !== session) update(reattached)
      else state = state.copy(layers = layersFor(session))
    }
    // A navigation started before the courses had loaded takes them now, see `knot-navigation`
    // "Courses loaded after the start".
    if (state.navigating)
      state.session?.let {
        Navigating.lateCourses.value = withCourses.geometry.withRoutePoints(it.routeGraph)
      }
    findWays()
  }

  private fun layersFor(session: PlanningSession): MapLayers {
    val network = loaded!!
    return MapLayers(
      knots = network.knots,
      connections = network.connections,
      route = MapFeatures.route(session, network.geometry),
      routeKnots = MapFeatures.routeKnotsLayer(session),
      routeKnotList = MapFeatures.routeKnotMarkers(session),
      routePointWays = MapFeatures.routePointWays(session),
      refs = network.refs,
      hasCourses = network.hasGeometry,
      geometry = network.geometry.takeIf { network.hasGeometry },
    )
  }

  private fun refreshCatalog() {
    state =
      state.copy(
        catalog = NetworkCatalog(null, storage.installed(), sync.downloading, sync.failed),
        needsAppUpdate = sync.needsAppUpdate,
      )
  }

  /**
   * Follows the background sync: after every installed file, the catalog is refreshed, and the
   * selected network is reloaded if its files changed, keeping the route, see `network-sync`.
   */
  private suspend fun watchSync() {
    WorkManager.getInstance(getApplication())
      .getWorkInfosForUniqueWorkFlow(NetworkSyncWorker.NAME)
      .collect {
        refreshCatalog()
        val graphEntry = storage.graphEntry(state.network) ?: return@collect
        val current = loaded
        val hasGeometry = state.status.hasGeometry
        if (
          current == null ||
            current.graphSha256 != graphEntry.sha256 ||
            current.hasGeometry != hasGeometry
        ) {
          loadNetwork(
            state.network,
            waypointsFrom = { (plannedSession ?: state.session)?.let(RouteSnapshot::of) },
          )
        }
      }
  }

  private companion object {
    const val SEARCH_DELAY_MILLIS = 300L
  }
}

/** How long finding the position on request may take, see "Show my position". */
private const val LOCATE_TIMEOUT_MILLIS = 15_000L

/** How long Navigate waits for a position to decide whether to ask, see "Position unknown". */
private const val START_POSITION_TIMEOUT_MILLIS = 10_000L

/** A fix up to this old decides at once, see `knot-navigation` "Recent position". */
private const val START_POSITION_MAX_AGE_MILLIS = 60_000L

/** Rougher fixes count as none for the start choice. */
private const val START_POSITION_ACCURACY_METERS = 100f

private const val KEY_APPEARANCE = "appearance"
private const val KEY_PLACES = "places"
private const val PATH_DATA_DELAY_MILLIS = 2000L

/**
 * The note after a tap: what the knot became and what else it can be, with the session [before] the
 * tap for Undo, see `map-planning` "Changing what a tapped knot became".
 */
data class TapNote(val options: TapOptions, val before: PlanningSession, val id: Long)
