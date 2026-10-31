package com.knoopbyknoop.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
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
import com.knoopbyknoop.core.NetworkCatalog
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkStatus
import com.knoopbyknoop.core.PlaceHit
import com.knoopbyknoop.core.PlaceSearch
import com.knoopbyknoop.core.PlanningSession
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteKnot
import com.knoopbyknoop.core.RouteSnapshot
import com.knoopbyknoop.core.RouteSummary
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** The map layers of the planner as GeoJSON, see [MapFeatures]. */
data class MapLayers(
  val knots: String,
  val connections: String,
  val route: String,
  val routeKnots: String,
  val routeKnotList: List<RouteKnot>,
  val refs: Set<String>,
  /** Whether [connections] follow their course, or are straight lines without geometry. */
  val hasCourses: Boolean,
)

/**
 * Everything the planner shows, owned by [AppViewModel].
 *
 * @property network the selected network type
 * @property online whether the device has network access, for the note while courses are loading
 */
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
  /** Whether to ask for location access on start: the first start, with nothing else to show. */
  val askLocationOnStart: Boolean = false,
  /** The appearance chosen in the info sheet, see `map-planning` "Light and dark appearance". */
  val appearance: Appearance = Appearance.SYSTEM,
  /** The navigation, or `null` when not navigating, see `knot-navigation`. */
  val navigation: NavigationState? = null,
  /**
   * The coming knot last tapped, shown on the map, see `knot-navigation` "Showing a coming knot".
   */
  val shownKnot: ShownKnot? = null,
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

  /** The language of the core's texts, see `app-languages` "Language of the texts". */
  private var language = Language.of(application.resources.configuration.locales[0].language)

  private fun text(@StringRes id: Int, vararg arguments: Any): String =
    getApplication<Application>().getString(id, *arguments)

  private val sync = NetworkSync(application)
  private val connectivity = application.getSystemService(ConnectivityManager::class.java)
  private var loaded: LoadedNetwork? = null

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
        initialPlace = camera,
        askLocationOnStart = camera == null && storage.route()?.waypoints.isNullOrEmpty(),
      )
    viewModelScope.launch {
      withContext(Dispatchers.IO) { sync.installBundled() }
      refreshCatalog()
      restoreRoute()
      if (sync.isDue()) NetworkSyncWorker.enqueue(application)
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
    viewModelScope.launch { Navigating.state.collect { state = state.copy(navigation = it) } }
  }

  /** Starts navigating the planned route; location access must be granted. */
  fun startNavigation() {
    val session = state.session ?: return
    val knots = (session.route?.summary as? RouteSummary.Complete)?.knots ?: return
    val network = loaded ?: return
    NavigationService.start(
      getApplication(),
      NavigationRoute(
        knots,
        session.graph,
        network.geometry.takeIf { network.hasGeometry },
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

  /** Chooses and keeps the [appearance], see `map-planning` "Light and dark appearance". */
  fun chooseAppearance(appearance: Appearance) {
    settings.edit().putString(KEY_APPEARANCE, appearance.name).apply()
    state = state.copy(appearance = appearance)
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
    if (state.locating) return
    state = state.copy(locating = true)
    viewModelScope.launch {
      val location = withTimeoutOrNull(LOCATE_TIMEOUT_MILLIS) { currentLocation() }
      state =
        if (location == null)
          state.copy(locating = false, message = text(R.string.position_not_found))
        else
          state.copy(
            locating = false,
            myPosition =
              Fix(
                location.latitude,
                location.longitude,
                location.accuracy.toDouble(),
                location.time,
              ),
          )
    }
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
  private suspend fun currentLocation(): Location? = suspendCancellableCoroutine { continuation ->
    val cancellation = CancellationTokenSource()
    try {
      LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
        .addOnSuccessListener { continuation.resume(it) }
        .addOnFailureListener { continuation.resume(null) }
    } catch (exception: SecurityException) {
      continuation.resume(null)
    }
    continuation.invokeOnCancellation { cancellation.cancel() }
  }

  private var searchJob: Job? = null
  private var mapCenter: CameraPlace? = null

  /** The map's current place, to rank search hits near it. */
  fun mapMoved(place: CameraPlace) {
    mapCenter = place
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
          Locale.getDefault().language,
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
  fun tap(latitude: Double, longitude: Double, radiusMeters: Double) {
    val session = state.session ?: return
    val knot = loaded?.locator?.nearest(latitude, longitude, radiusMeters) ?: return
    update(session.tap(knot))
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
      val route = state.session?.route ?: return null
      val network = loaded?.takeIf { it.hasGeometry && !state.navigating } ?: return null
      return network.geometry.courseOf(route)?.takeIf { Gpx.canExport(route, it) }
    }

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
    storage.saveRoute(RouteSnapshot.of(session))
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
                .mapLayer(network, "knots", withGeometry = false) {
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
    state.session?.let { state = state.copy(layers = layersFor(it)) }
  }

  private fun layersFor(session: PlanningSession): MapLayers {
    val network = loaded!!
    return MapLayers(
      knots = network.knots,
      connections = network.connections,
      route = MapFeatures.route(session, network.geometry),
      routeKnots = MapFeatures.routeKnotsLayer(session),
      routeKnotList = MapFeatures.routeKnotMarkers(session),
      refs = network.refs,
      hasCourses = network.hasGeometry,
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
          loadNetwork(state.network, waypointsFrom = { state.session?.let(RouteSnapshot::of) })
        }
      }
  }

  private companion object {
    const val SEARCH_DELAY_MILLIS = 300L
  }
}

/** How long finding the position on request may take, see "Show my position". */
private const val LOCATE_TIMEOUT_MILLIS = 15_000L

private const val KEY_APPEARANCE = "appearance"
