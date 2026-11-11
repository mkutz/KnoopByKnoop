package com.knoopbyknoop.core

/** What a tapped knot becomes once a route exists, see `map-planning` "What new knots become". */
enum class NewKnots {
  /** A stop in the leg where it adds the least distance. */
  STOP,

  /** The new end, with the old end a stop. */
  END,
}

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
   * The ferries the plan avoids, as knot ID pairs, see `knot-navigation` "Riding around a closed
   * ferry"; kept while the plan is edited.
   */
  val avoidedFerries: Set<Pair<Long, Long>> = setOf(),
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

  /** What the waypoint at [position] is, see `map-planning` "What a chosen route point does". */
  fun roleOf(position: Int): WaypointRole =
    when {
      endFirst -> WaypointRole.END
      position == 0 -> WaypointRole.START
      position == waypoints.lastIndex -> WaypointRole.END
      else -> WaypointRole.STOP
    }

  /**
   * The first tapped knot becomes the start, the second the end, every further one what [newKnots]
   * says. Tapping a waypoint again changes nothing, so an accidental double tap does not create a
   * round trip and the start does not turn into a stop.
   */
  fun tap(knot: Knot, newKnots: NewKnots): PlanningSession =
    when {
      knot in waypoints -> this
      endFirst -> planned(listOf(knot) + waypoints)
      waypoints.isEmpty() -> copy(listOf(knot), route = null)
      route == null -> withRoute(Route.plan(waypoints + knot, router))
      newKnots == NewKnots.END -> withRoute(route.withNewEnd(knot))
      else -> withRoute(route.insertStop(knot))
    }

  /**
   * Makes [point] the new start; an earlier start becomes a stop, see `map-planning` "What a chosen
   * route point does".
   */
  fun startAt(point: Knot): PlanningSession = planned(listOf(point) + waypoints)

  /**
   * Starts the plan at [nearby], a knot of the route or a route point on its course before the
   * first stop, instead of at its start: the waypoints before the first stop go, the stops and the
   * end stay, see `knot-navigation` "Start on the route nearby".
   */
  fun startOnRouteAt(nearby: Knot): PlanningSession {
    require(waypoints.size >= 2) { "Starting nearby needs a route" }
    val rest = waypoints.drop(1)
    return planned(if (rest.first() == nearby) rest else listOf(nearby) + rest)
  }

  /**
   * Makes [point] the new end; an earlier end becomes a stop, also the start of a round trip.
   * Without any waypoint, the point waits as the end until a start is chosen.
   */
  fun endAt(point: Knot): PlanningSession =
    if (waypoints.isEmpty()) copy(listOf(point), route = null, endFirst = true)
    else planned(waypoints + point)

  /** Adds [point] as a stop in the leg where it adds the least distance; needs a route. */
  fun addStopAt(point: Knot): PlanningSession {
    val route = requireNotNull(route) { "A stop needs a route" }
    val newGraph = graph.withRoutePoints(routePoints + point)
    val newRouter = ShortestPathRouter(newGraph, avoidedFerries)
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
   * The same waypoints planned again without [ferries], and without those avoided before, see
   * `knot-navigation` "Riding around a closed ferry".
   */
  fun avoidingFerries(ferries: Set<Pair<Long, Long>>): PlanningSession {
    val avoided = avoidedFerries + ferries
    val newRouter = ShortestPathRouter(routeGraph, avoided)
    val newRoute = if (waypoints.size >= 2) Route.plan(waypoints, newRouter) else null
    return PlanningSession(graph, waypoints, newRoute, routeGraph, newRouter, endFirst, avoided)
  }

  /** An empty session on another network, e.g. hiking instead of cycling. */
  fun switchTo(otherGraph: NetworkGraph): PlanningSession = start(otherGraph)

  /**
   * A session with [newWaypoints], routed on a graph with their route points. Legs between
   * waypoints that stay the same are reused.
   */
  private fun planned(newWaypoints: List<Knot>): PlanningSession {
    val newPoints = newWaypoints.filter { it.isRoutePoint }.distinct()
    val sameGraph = newPoints.toSet() == routePoints.toSet()
    val newGraph = if (sameGraph) routeGraph else graph.withRoutePoints(newPoints)
    val newRouter = if (sameGraph) router else ShortestPathRouter(newGraph, avoidedFerries)
    if (newWaypoints.size < 2)
      return PlanningSession(
        graph,
        newWaypoints,
        null,
        newGraph,
        newRouter,
        avoidedFerries = avoidedFerries,
      )
    val newRoute =
      route?.using(newRouter)?.withWaypoints(newWaypoints) ?: Route.plan(newWaypoints, newRouter)
    return withRoute(newRoute, newGraph, newRouter)
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
      avoidedFerries = avoidedFerries,
    )

  private fun copy(newWaypoints: List<Knot>, route: Route?, endFirst: Boolean = false) =
    if (newWaypoints.filter { it.isRoutePoint }.toSet() == routePoints.toSet())
      PlanningSession(graph, newWaypoints, route, routeGraph, router, endFirst, avoidedFerries)
    else {
      val newGraph = graph.withRoutePoints(newWaypoints)
      PlanningSession(
        graph,
        newWaypoints,
        route,
        newGraph,
        ShortestPathRouter(newGraph, avoidedFerries),
        endFirst,
        avoidedFerries,
      )
    }

  companion object {

    /** A session on [graph], optionally continuing with [waypoints], e.g. a restored route. */
    fun start(
      graph: NetworkGraph,
      waypoints: List<Knot> = listOf(),
      avoidedFerries: Set<Pair<Long, Long>> = setOf(),
    ): PlanningSession {
      val installed = graph.installed
      val routeGraph = installed.withRoutePoints(waypoints)
      val router = ShortestPathRouter(routeGraph, avoidedFerries)
      val route = if (waypoints.size >= 2) Route.plan(waypoints, router) else null
      return PlanningSession(
        installed,
        waypoints,
        route,
        routeGraph,
        router,
        avoidedFerries = avoidedFerries,
      )
    }
  }
}
