package com.knoopbyknoop.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.InstallCheck
import com.knoopbyknoop.core.KnotLocator
import com.knoopbyknoop.core.MapFeatures
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.PlanningSession
import com.knoopbyknoop.core.RegionCatalog
import com.knoopbyknoop.core.RegionInstaller
import com.knoopbyknoop.core.RegionState
import com.knoopbyknoop.core.RegionStatus
import com.knoopbyknoop.core.RejectionReason
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteKnot
import com.knoopbyknoop.core.RouteSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
  REGIONS,
  PLANNER,
}

/** The map layers of the planner as GeoJSON, see [MapFeatures]. */
data class MapLayers(
  val knots: String,
  val connections: String,
  val route: String,
  val routeKnots: String,
  val routeKnotList: List<RouteKnot>,
  val refs: Set<String>,
)

/** Everything the screens show, owned by [AppViewModel]. */
data class AppState(
  val screen: Screen = Screen.REGIONS,
  val catalog: RegionCatalog = RegionCatalog(null, listOf()),
  val progress: Map<String, Float> = mapOf(),
  val session: PlanningSession? = null,
  val layers: MapLayers? = null,
  val description: RouteDescription = RouteDescription.Empty,
  val message: String? = null,
  val loading: Boolean = false,
)

/** The installed data of one region and network, prepared for planning and drawing. */
private class LoadedNetwork(
  val geometry: NetworkGeometry,
  val locator: KnotLocator,
  val knots: String,
  val connections: String,
  val refs: Set<String>,
)

/** Connects the screens with storage, downloads and the core's planning logic. */
class AppViewModel(application: Application) : AndroidViewModel(application) {

  private val storage = RegionStorage(application.filesDir)
  private val downloads = RegionDownloads(application)
  private var index: NetworkIndex? = null
  private var loaded: LoadedNetwork? = null

  var state by mutableStateOf(AppState())
    private set

  init {
    refreshCatalog()
    viewModelScope.launch { restoreRoute() }
    viewModelScope.launch {
      index = NetworkIndexClient.fetch()
      refreshCatalog()
    }
    viewModelScope.launch { watchDownloads() }
  }

  fun showRegions() {
    state = state.copy(screen = Screen.REGIONS)
    // Without the index the app was offline; network access may be back by now.
    if (index == null) {
      viewModelScope.launch {
        index = NetworkIndexClient.fetch()
        refreshCatalog()
      }
    }
  }

  fun dismissMessage() {
    state = state.copy(message = null)
  }

  fun download(region: RegionStatus) {
    downloads.start(region)
    refreshCatalog()
  }

  fun remove(region: String) {
    downloads.cancel(region)
    storage.remove(region)
    if (state.session?.region == region) {
      loaded = null
      state = state.copy(session = null, layers = null, description = RouteDescription.Empty)
    }
    refreshCatalog()
  }

  fun openPlanner(region: String) {
    state = state.copy(screen = Screen.PLANNER)
    if (state.session?.region != region) {
      viewModelScope.launch {
        startSession(region, IndexedNetworkType.CYCLING, waypointsFrom = null)
      }
    }
  }

  fun switchNetwork(network: IndexedNetworkType) {
    val region = state.session?.region ?: return
    storage.saveRoute(null)
    viewModelScope.launch { startSession(region, network, waypointsFrom = null) }
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

  fun clear() {
    state.session?.let { update(it.clear()) }
  }

  fun returnToStart() {
    state.session?.takeIf { it.canReturnToStart }?.let { update(it.returnToStart()) }
  }

  private fun update(session: PlanningSession) {
    storage.saveRoute(RouteSnapshot.of(session))
    state =
      state.copy(
        session = session,
        layers = layersFor(session),
        description = MapFeatures.describe(session),
      )
  }

  private suspend fun restoreRoute() {
    val snapshot = storage.route()
    val region = snapshot?.region ?: storage.installed().firstOrNull()?.region ?: return
    state = state.copy(screen = Screen.PLANNER)
    startSession(region, snapshot?.network ?: IndexedNetworkType.CYCLING, waypointsFrom = snapshot)
    if (state.session == null) state = state.copy(screen = Screen.REGIONS)
  }

  /** Loads a network off the main thread: reading it and preparing its map layers takes time. */
  private suspend fun startSession(
    region: String,
    network: IndexedNetworkType,
    waypointsFrom: RouteSnapshot?,
  ) {
    state = state.copy(loading = true)
    val result =
      withContext(Dispatchers.Default) {
        val graphFile = storage.graph(region, network) ?: return@withContext null
        val geometry =
          storage.geometry(region, network, graphFile)
            ?: NetworkGeometry.straightLines(graphFile.graph)
        val loadedNetwork =
          LoadedNetwork(
            geometry = geometry,
            locator = KnotLocator(graphFile.graph.knots.values),
            knots =
              storage.mapLayer(region, network, "knots") { MapFeatures.knots(graphFile.graph) },
            connections =
              storage.mapLayer(region, network, "connections") {
                MapFeatures.connections(geometry)
              },
            refs = graphFile.graph.knots.values.map { it.ref }.toSet(),
          )
        val restored =
          if (
            waypointsFrom != null &&
              waypointsFrom.network == IndexedNetworkType.of(graphFile.network)
          ) {
            waypointsFrom.restore(graphFile.graph)
          } else {
            null
          }
        Triple(
          loadedNetwork,
          restored?.session ?: PlanningSession.start(region, graphFile.graph),
          restored?.removedWaypoints,
        )
      }
    if (result == null) {
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
        description = MapFeatures.describe(session),
        message =
          removedWaypoints
            ?.takeIf { it.isNotEmpty() }
            ?.let {
              "Waypoint ${it.joinToString()} no longer exists and was removed from the route."
            } ?: state.message,
      )
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
    )
  }

  private fun refreshCatalog() {
    val catalog = RegionCatalog(index, storage.installed(), downloads.regions())
    state = state.copy(catalog = catalog)
    if (state.session == null && catalog.regions.none { it.state != RegionState.AVAILABLE }) {
      state = state.copy(screen = Screen.REGIONS)
    }
  }

  /** Polls running downloads while the app is open and installs finished ones. */
  private suspend fun watchDownloads() {
    while (true) {
      val progress = mutableMapOf<String, Float>()
      for (region in downloads.regions()) {
        when (val download = downloads.progress(region)) {
          is RegionDownloads.Progress.Running -> progress[region] = download.fraction
          is RegionDownloads.Progress.Finished -> install(region, download.files)
          RegionDownloads.Progress.Failed ->
            fail(
              region,
              "The download of ${RegionCatalog.nameOf(region)} failed. Please try again.",
            )
          null -> {}
        }
      }
      if (progress != state.progress) state = state.copy(progress = progress)
      delay(1_000)
    }
  }

  private suspend fun install(region: String, files: Map<NetworkIndexEntry, java.io.File>) {
    // Unreadable files count as a damaged download, so the user can retry.
    val result =
      withContext(Dispatchers.IO) {
        runCatching { checkFiles(files.mapValues { it.value.readBytes() }) }
          .getOrElse { InstallCheck.Rejected(RejectionReason.CORRUPTED) }
      }
    downloads.cancel(region)
    when (result) {
      is InstallCheck.Rejected ->
        fail(
          region,
          when (result.reason) {
            RejectionReason.UNSUPPORTED_VERSION ->
              "${RegionCatalog.nameOf(region)} needs a newer version of the app. Please update it."
            else -> "The download of ${RegionCatalog.nameOf(region)} was damaged. Please try again."
          },
        )
      is InstallCheck.Valid -> {
        withContext(Dispatchers.IO) { storage.install(result.content) }
        refreshCatalog()
        if (state.session == null || state.session?.region == region) openPlanner(region)
      }
    }
  }

  /**
   * Checks all files of a region. A damaged graph rejects the whole region; a geometry that cannot
   * be installed is left out, so the network is drawn with straight lines until an update.
   */
  private fun checkFiles(
    files: Map<NetworkIndexEntry, ByteArray>
  ): InstallCheck<Map<NetworkIndexEntry, ByteArray>> {
    val verified = mutableMapOf<NetworkIndexEntry, ByteArray>()
    for ((graphEntry, graphBytes) in files.filterKeys { it.kind == IndexedFileKind.GRAPH }) {
      val graphFile =
        when (val check = RegionInstaller.checkGraph(graphEntry, graphBytes)) {
          is InstallCheck.Rejected -> return check
          is InstallCheck.Valid -> check.content
        }
      verified[graphEntry] = graphBytes
      val geometry =
        files.entries.firstOrNull {
          it.key.kind == IndexedFileKind.GEOMETRY && it.key.network == graphEntry.network
        }
      if (
        geometry != null &&
          RegionInstaller.checkGeometry(
            geometry.key,
            geometry.value,
            graphEntry,
            graphFile.graph,
          ) is InstallCheck.Valid
      ) {
        verified[geometry.key] = geometry.value
      }
    }
    return InstallCheck.Valid(verified)
  }

  private fun fail(region: String, message: String) {
    downloads.cancel(region)
    state = state.copy(message = message)
    refreshCatalog()
  }
}
