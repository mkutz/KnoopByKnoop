package com.knoopbyknoop.web

import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.Gpx
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.MapFeatures
import com.knoopbyknoop.core.MapPlace
import com.knoopbyknoop.core.MapTap
import com.knoopbyknoop.core.PathDownloader
import com.knoopbyknoop.core.PathSquares
import com.knoopbyknoop.core.PathStore
import com.knoopbyknoop.core.PathStoreFiles
import com.knoopbyknoop.core.PlaceDetails
import com.knoopbyknoop.core.PlaceDownloader
import com.knoopbyknoop.core.PlaceSquares
import com.knoopbyknoop.core.PlaceStore
import com.knoopbyknoop.core.PlanningSession
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteFile
import com.knoopbyknoop.core.RouteHistory
import com.knoopbyknoop.core.RouteLink
import com.knoopbyknoop.core.RouteSnapshot
import com.knoopbyknoop.core.RouteSummary
import com.knoopbyknoop.core.WayFinder
import com.knoopbyknoop.core.hasStraightLines
import com.knoopbyknoop.core.reattached
import com.knoopbyknoop.core.routePointFor
import com.knoopbyknoop.core.tapAt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Where the planner gets its networks and squares from, see [PublishedData]. */
interface NetworkSource {

  /** [network] with straight connections, to plan on at once. */
  suspend fun graph(network: IndexedNetworkType): Loaded

  /** [loaded] with its courses. */
  suspend fun courses(loaded: Loaded): LoadedNetwork

  /** See [PublishedData.withSquares]; `null` when the squares can't be loaded. */
  suspend fun <T> withSquares(load: (com.knoopbyknoop.core.PathDataOrigin) -> T): T?

  /** A network with straight connections, and what [courses] needs to add them. */
  class Loaded(val network: LoadedNetwork, val graphSha256: String)
}

/** Text kept in the browser under a key, see `web-planner` "Route kept in the browser". */
interface Kept {
  fun read(key: String): String?

  fun write(key: String, value: String)
}

/** Files of a square store, in memory for as long as the page is open. */
private class MemoryFiles : PathStoreFiles {
  private val files = mutableMapOf<String, ByteArray>()

  override fun read(name: String) = files[name]

  override fun write(name: String, bytes: ByteArray) {
    files[name] = bytes
  }

  override fun delete(name: String) {
    files.remove(name)
  }
}

/** What a click on the map came to, for the page to show. */
sealed interface Clicked {
  /** The route changed, or nothing happened. */
  data object Done : Clicked

  /** A spot or place to offer as a route point, with its card for a place. */
  data class Offer(val offer: MapTap.Offer) : Clicked

  /** A place too far from the network: only its card. */
  data class PlaceOnly(val place: MapPlace) : Clicked
}

/**
 * The state of the web planner, see `web-planner`: the network, the route being planned with its
 * undo history, and the messages for the visitor. It plans with the core as the apps do, and keeps
 * the route in [kept]. The page draws it; the tests drive it without a browser.
 */
class Planner(
  private val source: NetworkSource,
  kept: Kept,
  val language: Language,
  private val texts: Map<String, String>,
  private val scope: CoroutineScope,
  private val nowMillis: () -> Long,
) {

  /** Called after every change, to redraw. */
  var onChange: () -> Unit = {}

  var network: IndexedNetworkType = IndexedNetworkType.CYCLING
    private set

  var loaded: LoadedNetwork? = null
    private set

  var session: PlanningSession? = null
    private set

  var history = RouteHistory()
    private set

  /** A message for the visitor, e.g. that a file holds no route; cleared by the next edit. */
  var message: String? = null
    private set

  /** The network data could not be loaded. */
  var dataProblem: String? = null
    private set

  val description: RouteDescription
    get() = session?.let { MapFeatures.describe(it, language) } ?: RouteDescription.Empty

  val canUndo: Boolean
    get() = history.canUndo

  /** Blocked storage only loses the route on reload, see "Browser storage unavailable". */
  private val kept =
    object : Kept {
      override fun read(key: String) = runCatching { kept.read(key) }.getOrNull()

      override fun write(key: String, value: String) {
        runCatching { kept.write(key, value) }
      }
    }

  private val paths = PathStore(MemoryFiles())
  private val places = PlaceStore(MemoryFiles())
  private val wayFinder = WayFinder(paths)
  private var loading: Job? = null
  private var squaresFor: List<GeoPoint>? = null

  /** Starts with the kept route's network and route, or an empty cycling route. */
  fun start() {
    val snapshot = keptSnapshot()
    history = kept.read(HISTORY_KEY)?.let(RouteHistory::parse) ?: RouteHistory()
    load(snapshot?.network ?: IndexedNetworkType.CYCLING, snapshot, pendingFile = null)
  }

  /** Switches to [other], starting a new route, see `web-planner` "Networks on the map". */
  fun switchNetwork(other: IndexedNetworkType) {
    if (other == network && loaded != null) return
    remember(RouteHistory())
    load(other, null, pendingFile = null)
  }

  private fun keptSnapshot(): RouteSnapshot? =
    kept.read(ROUTE_KEY)?.let { runCatching { RouteSnapshot.parse(it) }.getOrNull() }

  private fun load(
    other: IndexedNetworkType,
    snapshot: RouteSnapshot?,
    pendingFile: RouteFile.Parsed?,
  ) {
    network = other
    loaded = null
    session = null
    loading?.cancel()
    onChange()
    loading = scope.launch {
      val graph =
        try {
          source.graph(other)
        } catch (problem: DataProblem) {
          dataProblem = problem.message
          onChange()
          return@launch
        }
      dataProblem = null
      loaded = graph.network
      val restored = snapshot?.takeIf { it.network == other }?.restore(graph.network.graph)
      session = restored?.session ?: PlanningSession.start(graph.network.graph)
      restored
        ?.removedWaypoints
        ?.takeIf { it.isNotEmpty() }
        ?.let {
          message = text("waypointRemoved", it.joinToString())
        }
      save()
      onChange()
      pendingFile?.let { open(it, asEdit = false) }
      val withCourses =
        try {
          source.courses(graph)
        } catch (problem: DataProblem) {
          dataProblem = problem.message
          onChange()
          return@launch
        }
      loaded = withCourses
      session?.let { current ->
        val reattached = current.reattached(withCourses.geometry)
        if (reattached !== current) update(reattached) else onChange()
      }
      afterChange()
    }
  }

  /**
   * A click at [latitude], [longitude] with [radiusMeters] of tolerance, on [place] if the base map
   * has one there, see `web-planner` "Plan by clicking knots" and "Route points in the planner".
   */
  fun click(latitude: Double, longitude: Double, radiusMeters: Double, place: MapPlace?): Clicked {
    val current = session ?: return Clicked.Done
    val network = loaded ?: return Clicked.Done
    val knot = network.locator.nearestAtAnyNode(latitude, longitude, radiusMeters)
    return when (
      val tapped =
        current.tapAt(GeoPoint(latitude, longitude), radiusMeters, knot, network.geometry, place)
    ) {
      is MapTap.OnKnot -> {
        edit(current.tap(tapped.knot))
        Clicked.Done
      }
      is MapTap.OnRoutePoint -> Clicked.Done
      is MapTap.Offer -> {
        tapped.place?.let(::loadDetails)
        Clicked.Offer(tapped)
      }
      is MapTap.PlaceOnly -> {
        loadDetails(tapped.place)
        Clicked.PlaceOnly(tapped.place)
      }
      is MapTap.TooFar -> {
        message =
          text(
            if (current.network.name == "CYCLING") "noCyclingKnotsWithin"
            else "noHikingKnotsWithin",
            (tapped.limitMeters / 1000).toInt().toString(),
          )
        onChange()
        Clicked.Done
      }
    }
  }

  /** Adds the offered spot or place where it fits, see "Route points in the planner". */
  fun add(offer: MapTap.Offer) {
    val current = session ?: return
    val geometry = loaded?.geometry ?: return
    val point = current.routePointFor(offer, geometry) ?: return
    edit(current.placed(point))
  }

  fun remove(position: Int) {
    session?.takeIf { it.canRemoveWaypoints }?.let { edit(it.removeWaypoint(position)) }
  }

  fun move(from: Int, to: Int) {
    if (from != to) session?.let { edit(it.moveWaypoint(from, to)) }
  }

  fun returnToStart() {
    session?.takeIf { it.canReturnToStart }?.let { edit(it.returnToStart()) }
  }

  fun clear() {
    session?.takeIf { it.waypoints.isNotEmpty() }?.let { edit(it.clear()) }
  }

  /** See `web-planner` "Undo in the planner". */
  fun undo() {
    val current = session ?: return
    val network = loaded ?: return
    val (restored, earlier) = history.undo(current.graph) ?: return
    remember(earlier)
    message =
      restored.removedWaypoints
        .takeIf { it.isNotEmpty() }
        ?.let { text("waypointRemoved", it.joinToString()) }
    update(
      if (network.hasGeometry) restored.session.reattached(network.geometry) else restored.session
    )
  }

  /**
   * Opens the route file [text], or a route link, in place of the route, switching to its network
   * where it differs, see `web-planner` "Open a route in the planner".
   */
  fun open(text: String) {
    val fileText = RouteLink.textOf(text) ?: text
    val parsed = RouteFile.parse(fileText)
    val otherNetwork = IndexedNetworkType.of(parsed.network)
    if (otherNetwork == network && loaded != null) {
      open(parsed, asEdit = true)
      return
    }
    // The other network is only switched to if it holds the route, so a file without one leaves
    // the route as it is.
    scope.launch {
      val graph =
        try {
          source.graph(otherNetwork)
        } catch (problem: DataProblem) {
          dataProblem = problem.message
          onChange()
          return@launch
        }
      if (RouteFile.resolve(parsed, graph.network.graph) == null) {
        message = text("routeFileNone")
        onChange()
        return@launch
      }
      remember(RouteHistory())
      load(otherNetwork, null, parsed)
    }
  }

  private fun open(parsed: RouteFile.Parsed, asEdit: Boolean) {
    val network = loaded ?: return
    val current = session ?: return
    val resolved = RouteFile.resolve(parsed, current.graph)
    if (resolved == null) {
      message = text("routeFileNone")
      onChange()
      return
    }
    val restored = resolved.snapshot.restore(current.graph).session
    val opened = if (network.hasGeometry) restored.reattached(network.geometry) else restored
    if (asEdit) edit(opened) else update(opened)
    val route = opened.route
    message =
      when {
        resolved.leftOut.isNotEmpty() -> text("routeFileLeftOut", resolved.leftOut.joinToString())
        route != null && RouteFile.differsFrom(route, resolved.listed) -> text("routeFileDiffers")
        else -> message
      }
    openedRoutes++
    onChange()
  }

  /** Goes up with every route opened, so the page shows the whole route. */
  var openedRoutes = 0
    private set

  /** The route file's name and text, for a complete route, see "Downloads from the planner". */
  fun routeFile(): Pair<String, String>? {
    val current = session ?: return null
    val route = current.route?.takeIf { RouteFile.canShare(it) } ?: return null
    return RouteFile.fileName(route) to
      RouteFile.write(route, RouteFile.titleOf(route), current.labels)
  }

  /** The GPX file's name and text, once the route's course is known. */
  fun gpx(): Pair<String, String>? {
    val current = session ?: return null
    val route = current.route ?: return null
    val network = loaded?.takeIf { it.hasGeometry } ?: return null
    val course = network.geometry.withRoutePoints(current.routeGraph).courseOf(route) ?: return null
    if (!Gpx.canExport(route, course)) return null
    return Gpx.fileName(route) to Gpx.write(route, course)
  }

  /** The route's link, see "Route link from the planner". */
  fun link(): String? = routeFile()?.let { RouteLink.of(it.second) }

  /** The details of [place] as far as they are loaded, see "Places in the planner". */
  fun detailsOf(place: MapPlace): PlaceDetails? = places.detailsOf(place)

  /** Called when the details of a clicked place arrive. */
  var onDetails: (MapPlace) -> Unit = {}

  private fun loadDetails(place: MapPlace) {
    if (places.detailsOf(place) != null) return
    val square = PlaceSquares.squareToLoad(place.position, navigating = false, places) ?: return
    scope.launch {
      source.withSquares { origin ->
        PlaceDownloader(places, origin).loadSquare(square, plannedCourse(), nowMillis())
      }
      onDetails(place)
    }
  }

  private fun text(key: String, vararg arguments: String): String {
    var result = texts[key] ?: key
    arguments.forEachIndexed { index, argument -> result = result.replace("{$index}", argument) }
    return result
  }

  /** An edit by the visitor, which Undo can take back. */
  private fun edit(after: PlanningSession) {
    val before = session ?: return
    if (loaded == null) return
    message = null
    remember(history.record(before, after))
    update(after)
  }

  /** A change of the route, by the visitor or by the planner, kept in the browser. */
  private fun update(after: PlanningSession) {
    session = after
    save()
    onChange()
    afterChange()
  }

  private fun save() {
    session?.let { kept.write(ROUTE_KEY, RouteSnapshot.of(it).toJson()) }
  }

  private fun remember(newHistory: RouteHistory) {
    history = newHistory
    kept.write(HISTORY_KEY, newHistory.toJson())
  }

  private fun plannedCourse(): List<GeoPoint>? {
    val current = session ?: return null
    val route = current.route ?: return null
    val network = loaded?.takeIf { it.hasGeometry } ?: return null
    if (route.summary !is RouteSummary.Complete) return null
    return network.geometry.withRoutePoints(current.routeGraph).courseOf(route)
  }

  /**
   * Loads the path data and place details around a complete route, then finds the ways of route
   * points, as the apps do, see `network-sync` "Path data around the route".
   */
  private fun afterChange() {
    val current = session ?: return
    val course = plannedCourse()
    val pathCourse = PathSquares.courseToLoad(current.network, course, null)
    if (pathCourse == null || pathCourse == squaresFor) {
      findWays()
      return
    }
    squaresFor = pathCourse
    scope.launch {
      source.withSquares { origin -> PathDownloader(paths, origin).load(pathCourse, nowMillis()) }
      course?.let { placeCourse ->
        source.withSquares { origin ->
          PlaceDownloader(places, origin).loadAround(placeCourse, nowMillis())
        }
      }
      findWays()
    }
  }

  private fun findWays() {
    val current = session ?: return
    if (!current.hasStraightLines) return
    val geometry = loaded?.takeIf { it.hasGeometry }?.geometry ?: return
    val ways = wayFinder.routePointWays(geometry)
    val found =
      current.routePoints.filter { it.routePoint?.straight == true }.mapNotNull(ways::wayFor)
    if (found.isEmpty()) return
    val updated = found.fold(current) { next, point -> next.withRoutePoint(point) }
    if (updated !== current) update(updated)
  }

  companion object {
    const val ROUTE_KEY = "knoopbyknoop.route"
    const val HISTORY_KEY = "knoopbyknoop.history"
  }
}
