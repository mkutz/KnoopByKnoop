package com.knoopbyknoop.core

/**
 * Planning a route on the map, see `map-planning`: tapping knots sets start, end and stops, and the
 * waypoints can be moved and removed. Immutable: every action returns a new session.
 *
 * @property route the planned route, or `null` while there are fewer than two waypoints
 */
class PlanningSession
private constructor(
  val graph: NetworkGraph,
  val waypoints: List<Knot>,
  val route: Route?,
  private val router: Router,
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

  /**
   * The first tapped knot becomes the start, the second the end, every further one a stop in the
   * leg where it adds the least distance. Tapping a waypoint again changes nothing, so an
   * accidental double tap does not create a round trip and the start does not turn into a stop.
   */
  fun tap(knot: Knot): PlanningSession =
    when {
      knot in waypoints -> this
      waypoints.isEmpty() -> copy(listOf(knot), route = null)
      route == null -> withRoute(Route.plan(waypoints + knot, router))
      else -> withRoute(route.insertStop(knot))
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
    else withRoute(route.removeWaypoint(position))
  }

  fun clear(): PlanningSession = copy(listOf(), route = null)

  /** An empty session on another network, e.g. hiking instead of cycling. */
  fun switchTo(otherGraph: NetworkGraph): PlanningSession = start(otherGraph)

  private fun withRoute(newRoute: Route) = copy(newRoute.waypoints, newRoute)

  private fun copy(newWaypoints: List<Knot>, route: Route?) =
    PlanningSession(graph, newWaypoints, route, router)

  companion object {

    /** A session on [graph], optionally continuing with [waypoints], e.g. a restored route. */
    fun start(graph: NetworkGraph, waypoints: List<Knot> = listOf()): PlanningSession {
      val router = ShortestPathRouter(graph)
      val route = if (waypoints.size >= 2) Route.plan(waypoints, router) else null
      return PlanningSession(graph, waypoints, route, router)
    }
  }
}
