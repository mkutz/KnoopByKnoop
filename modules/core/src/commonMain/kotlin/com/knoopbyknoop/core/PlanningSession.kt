package com.knoopbyknoop.core

/**
 * What a waypoint is in the route, by its position, see `map-planning` "Edit the waypoint list".
 */
enum class WaypointRole {
  START,
  STOP,
  END,
}

/**
 * Planning a route on the map, see `map-planning`: tapping knots sets start, end and stops, chosen
 * places become route points, and the waypoints can be moved and removed. Immutable: every action
 * returns a new session.
 *
 * @property graph the installed network, without route points, for the map and the knot search
 * @property route the planned route, or `null` while there are fewer than two waypoints
 * @property routeGraph [graph] with the route points of [waypoints], for routing and navigating
 * @property endFirst whether the only waypoint is an end chosen before any start, see "End before
 *   the start"
 */
class PlanningSession
private constructor(
  val graph: NetworkGraph,
  val waypoints: List<Knot>,
  val route: Route?,
  val routeGraph: NetworkGraph,
  private val router: Router,
  val endFirst: Boolean = false,
  /**
   * The IDs of the waypoints added only for one navigation, see `knot-navigation` "The plan after
   * navigation": a start nearby and the knots on the way around a closed ferry.
   */
  val temporary: Set<Long> = setOf(),
  /**
   * The ferries the route of one navigation leaves out, as knot ID pairs, see `knot-navigation`
   * "Riding around a closed ferry"; never stored.
   */
  private val avoiding: Set<Pair<Long, Long>> = setOf(),
) {

  val network: NetworkType = requireNotNull(graph.network) { "Cannot plan on an empty network" }

  /**
   * Any waypoint can be removed; removing one of the last two leaves the other as the start, see
   * `map-planning` "Edit the waypoint list".
   */
  val canRemoveWaypoints: Boolean
    get() = waypoints.isNotEmpty()

  /** Whether [returnToStart] can turn the route into a round trip. */
  val canReturnToStart: Boolean
    get() = waypoints.size >= 2 && waypoints.last() != waypoints.first()

  /** The route points among the waypoints, each once. */
  val routePoints: List<Knot>
    get() = waypoints.filter { it.isRoutePoint }.distinct()

  /**
   * Whether the waypoint at [position] was added only to ride around a closed ferry: an ordinary
   * knot of the route, not a stop, see `knot-navigation` "Temporary knots are not stops".
   */
  fun isTemporaryStop(position: Int): Boolean =
    position in 1 until waypoints.lastIndex && waypoints[position].id in temporary

  /** What the waypoint at [position] is, see `map-planning` "What a chosen route point does". */
  fun roleOf(position: Int): WaypointRole =
    when {
      endFirst -> WaypointRole.END
      position == 0 -> WaypointRole.START
      position == waypoints.lastIndex -> WaypointRole.END
      else -> WaypointRole.STOP
    }

  /**
   * The first tapped knot becomes the start, the second the end, every further one goes where it
   * adds the least distance: a stop in a leg or the new end, see `map-planning` "Plan by tapping
   * knots". Tapping a waypoint again changes nothing, so an accidental double tap does not create a
   * round trip and the start does not turn into a stop.
   */
  fun tap(knot: Knot): PlanningSession =
    when {
      knot in waypoints -> this
      endFirst -> planned(listOf(knot) + waypoints)
      waypoints.isEmpty() -> copy(listOf(knot), route = null)
      // A route point needs a graph that knows it.
      route == null && knot.isRoutePoint -> planned(waypoints + knot)
      route == null -> withRoute(Route.plan(waypoints + knot, router))
      knot.isRoutePoint -> placed(knot)
      else -> withRoute(route.insertWhereItFits(knot))
    }

  /**
   * [point], a route point not yet in the route, placed where it adds the least distance, as a
   * tapped knot would be, see `map-planning` "Choose a route point on the map"; without a route as
   * [tap] does.
   */
  fun placed(point: Knot): PlanningSession {
    val route = route ?: return tap(point)
    if (!point.isRoutePoint) return withRoute(route.insertWhereItFits(point))
    val newGraph = graph.withRoutePoints(routePoints + point)
    val newRouter = ShortestPathRouter(newGraph, avoiding)
    return withRoute(route.using(newRouter).insertWhereItFits(point), newGraph, newRouter)
  }

  /**
   * Makes [point] the new start; an earlier start becomes a stop, see `map-planning` "What a chosen
   * route point does".
   */
  fun startAt(point: Knot): PlanningSession = planned(listOf(point) + waypoints)

  /**
   * The route for one navigation starting at [nearby], a knot of the route or a route point on its
   * course before the first stop, instead of at the plan's start: the waypoints before the first
   * stop go, the stops and the end stay. A new [nearby] is temporary, see `knot-navigation` "Start
   * on the route nearby".
   */
  fun startOnRouteAt(nearby: Knot): PlanningSession {
    require(waypoints.size >= 2) { "Starting nearby needs a route" }
    val rest = waypoints.drop(1)
    return if (rest.first() == nearby) planned(rest)
    else planned(listOf(nearby) + rest, temporary + nearby.id)
  }

  /**
   * The route for one navigation through [newWaypoints]: this plan's waypoints in their order with
   * further knots between them, which become temporary, and also without the ferries in [more], as
   * knot ID pairs, see `knot-navigation` "Riding around a closed ferry". Never stored.
   */
  fun through(
    newWaypoints: List<Knot>,
    more: Set<Pair<Long, Long>> = setOf(),
  ): PlanningSession {
    val ids = waypoints.mapTo(HashSet()) { it.id }
    val newTemporary = temporary + newWaypoints.map { it.id }.filter { it !in ids }
    return planned(newWaypoints, newTemporary, avoiding + more)
  }

  /**
   * Makes [point] the new end; an earlier end becomes a stop, also the start of a round trip.
   * Without any waypoint, the point waits as the end until a start is chosen.
   */
  fun endAt(point: Knot): PlanningSession =
    if (waypoints.isEmpty()) copy(listOf(point), route = null, endFirst = true)
    else planned(waypoints + point)

  /**
   * Adds [knot] as a stop in the leg where it adds the least distance, also a knot of the network;
   * needs a route, see `map-planning` "Changing what a tapped knot became".
   */
  fun stopAt(knot: Knot): PlanningSession {
    if (knot.isRoutePoint) return addStopAt(knot)
    val route = requireNotNull(route) { "A stop needs a route" }
    return withRoute(route.insertStop(knot))
  }

  /** Adds [point] as a stop in the leg where it adds the least distance; needs a route. */
  fun addStopAt(point: Knot): PlanningSession {
    val route = requireNotNull(route) { "A stop needs a route" }
    val newGraph = graph.withRoutePoints(routePoints + point)
    val newRouter = ShortestPathRouter(newGraph, avoiding)
    return withRoute(route.using(newRouter).insertStop(point), newGraph, newRouter)
  }

  /**
   * Replaces the route point with the same ID as [point], e.g. when a way along paths replaces its
   * straight line, see `map-planning` "A route point away from the network".
   */
  fun withRoutePoint(point: Knot): PlanningSession {
    if (waypoints.none { it.id == point.id }) return this
    val replaced = waypoints.map { if (it.id == point.id) point else it }
    return if (route == null) copy(replaced, route = null, endFirst = endFirst)
    else planned(replaced)
  }

  /** Turns the route into a round trip by adding the start as new end. */
  fun returnToStart(): PlanningSession {
    require(canReturnToStart) { "The route needs a start and an end other than the start" }
    return withRoute(Route.plan(waypoints + waypoints.first(), router))
  }

  fun moveWaypoint(from: Int, to: Int): PlanningSession =
    if (route == null) this else withRoute(route.moveWaypoint(from, to))

  fun removeWaypoint(position: Int): PlanningSession {
    require(position in waypoints.indices) { "No waypoint at $position" }
    val remaining = waypoints.filterIndexed { index, _ -> index != position }
    return if (route == null || remaining.size < 2) copy(remaining, route = null)
    else planned(remaining)
  }

  fun clear(): PlanningSession =
    PlanningSession(graph, listOf(), null, graph, ShortestPathRouter(graph))

  /**
   * The same waypoints planned also without the ferries in [more], as knot ID pairs, only to find
   * the way around them, see `knot-navigation` "Riding around a closed ferry".
   */
  internal fun routeAvoiding(more: Set<Pair<Long, Long>>): Route? =
    if (waypoints.size >= 2) Route.plan(waypoints, ShortestPathRouter(routeGraph, avoiding + more))
    else null

  /** An empty session on another network, e.g. hiking instead of cycling. */
  fun switchTo(otherGraph: NetworkGraph): PlanningSession = start(otherGraph)

  /**
   * A session with [newWaypoints], routed on a graph with their route points. Legs between
   * waypoints that stay the same are reused.
   */
  private fun planned(
    newWaypoints: List<Knot>,
    newTemporary: Set<Long> = temporary,
    newAvoiding: Set<Pair<Long, Long>> = avoiding,
  ): PlanningSession {
    val newPoints = newWaypoints.filter { it.isRoutePoint }.distinct()
    val sameGraph = newPoints.toSet() == routePoints.toSet()
    val newGraph = if (sameGraph) routeGraph else graph.withRoutePoints(newPoints)
    val newRouter =
      if (sameGraph && newAvoiding == avoiding) router
      else ShortestPathRouter(newGraph, newAvoiding)
    if (newWaypoints.size < 2)
      return PlanningSession(
        graph,
        newWaypoints,
        null,
        newGraph,
        newRouter,
        temporary = newTemporary,
        avoiding = newAvoiding,
      )
    val newRoute =
      if (newAvoiding == avoiding)
        route?.using(newRouter)?.withWaypoints(newWaypoints) ?: Route.plan(newWaypoints, newRouter)
      else Route.plan(newWaypoints, newRouter)
    return PlanningSession(
      graph,
      newRoute.waypoints,
      newRoute,
      newGraph,
      newRouter,
      temporary = newTemporary,
      avoiding = newAvoiding,
    )
  }

  private fun withRoute(
    newRoute: Route,
    newGraph: NetworkGraph = routeGraph,
    newRouter: Router = router,
  ) =
    PlanningSession(
      graph,
      newRoute.waypoints,
      newRoute,
      newGraph,
      newRouter,
      temporary = temporary,
      avoiding = avoiding,
    )

  private fun copy(newWaypoints: List<Knot>, route: Route?, endFirst: Boolean = false) =
    if (newWaypoints.filter { it.isRoutePoint }.toSet() == routePoints.toSet())
      PlanningSession(graph, newWaypoints, route, routeGraph, router, endFirst, temporary, avoiding)
    else {
      val newGraph = graph.withRoutePoints(newWaypoints)
      PlanningSession(
        graph,
        newWaypoints,
        route,
        newGraph,
        ShortestPathRouter(newGraph, avoiding),
        endFirst,
        temporary,
        avoiding,
      )
    }

  companion object {

    /** A session on [graph], optionally continuing with [waypoints], e.g. a restored route. */
    fun start(graph: NetworkGraph, waypoints: List<Knot> = listOf()): PlanningSession {
      val installed = graph.installed
      val routeGraph = installed.withRoutePoints(waypoints)
      val router = ShortestPathRouter(routeGraph)
      val route = if (waypoints.size >= 2) Route.plan(waypoints, router) else null
      return PlanningSession(installed, waypoints, route, routeGraph, router)
    }
  }
}
