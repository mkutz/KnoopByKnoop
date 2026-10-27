package com.knoopbyknoop.android

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.KnotLocator
import com.knoopbyknoop.core.MapFeatures
import com.knoopbyknoop.core.NetworkCatalog
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkStatus
import com.knoopbyknoop.core.PlanningSession
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteKnot
import com.knoopbyknoop.core.RouteSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The map layers of the planner as GeoJSON, see [MapFeatures]. */
data class MapLayers(
  val knots: String,
  val connections: String,
  val route: String,
  val routeKnots: String,
  val routeKnotList: List<RouteKnot>,
  val refs: Set<String>,
)

/**
 * Everything the planner shows, owned by [AppViewModel].
 *
 * @property network the selected network type
 * @property online whether the device has network access, for the note while courses are loading
 */
data class AppState(
  val network: IndexedNetworkType = IndexedNetworkType.CYCLING,
  val catalog: NetworkCatalog = NetworkCatalog(null, listOf()),
  val needsAppUpdate: Boolean = false,
  val online: Boolean = true,
  val session: PlanningSession? = null,
  val layers: MapLayers? = null,
  val description: RouteDescription = RouteDescription.Empty,
  val message: String? = null,
  val loading: Boolean = false,
) {
  val status: NetworkStatus
    get() = catalog.status(network)
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
  private val sync = NetworkSync(application)
  private val connectivity = application.getSystemService(ConnectivityManager::class.java)
  private var loaded: LoadedNetwork? = null

  var state by mutableStateOf(AppState())
    private set

  init {
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
  }

  fun dismissMessage() {
    state = state.copy(message = null)
  }

  /** Retries a failed download right away instead of waiting for the next attempt. */
  fun retrySync() {
    NetworkSyncWorker.enqueue(getApplication(), restart = true)
  }

  fun switchNetwork(network: IndexedNetworkType) {
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

  fun clear() {
    state.session?.let { update(it.clear()) }
  }

  fun returnToStart() {
    state.session?.takeIf { it.canReturnToStart }?.let { update(it.returnToStart()) }
  }

  private fun update(session: PlanningSession) {
    // While another network loads, the shown route belongs to the previous one.
    if (loaded == null) return
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
    val prepared =
      withContext(Dispatchers.Default) {
        val graphEntry = storage.graphEntry(network) ?: return@withContext null
        val graphFile = storage.graph(network) ?: return@withContext null
        val installedGeometry = storage.geometry(network, graphFile)
        val geometry = installedGeometry ?: NetworkGeometry.straightLines(graphFile.graph)
        graphFile to
          LoadedNetwork(
            graphSha256 = graphEntry.sha256,
            hasGeometry = installedGeometry != null,
            geometry = geometry,
            locator = KnotLocator(graphFile.graph.knots.values),
            knots = storage.mapLayer(network, "knots") { MapFeatures.knots(graphFile.graph) },
            connections =
              storage.mapLayer(network, "connections") { MapFeatures.connections(geometry) },
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
}
