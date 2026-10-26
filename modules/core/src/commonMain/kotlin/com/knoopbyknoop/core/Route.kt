package com.knoopbyknoop.core

/**
 * The part of a [Route] between two consecutive waypoints.
 *
 * @property path the shortest path from [from] to [to], or `null` if [to] is unreachable
 */
data class Leg(val from: Knot, val to: Knot, val path: Path?) {

  val isReachable: Boolean
    get() = path != null
}

/** What a [Route] adds up to. */
sealed interface RouteSummary {

  /** All legs are reachable. [knots] lists every knot to ride along in order. */
  data class Complete(val knots: List<Knot>, val lengthMeters: Int) : RouteSummary

  /** At least one leg cannot be ridden, so there is no route to summarize. */
  data class Incomplete(val unreachableLegs: List<Leg>) : RouteSummary
}

/**
 * An immutable route over a node network, defined by its waypoints: the first is the start, the
 * last is the end and all others are stops. The knots in between are computed.
 *
 * Every edit returns a new route. Legs that the edit did not touch are reused instead of being
 * routed again.
 */
class Route
private constructor(val waypoints: List<Knot>, val legs: List<Leg>, private val router: Router) {

  val summary: RouteSummary
    get() {
      val unreachableLegs = legs.filterNot { it.isReachable }
      if (unreachableLegs.isNotEmpty()) return RouteSummary.Incomplete(unreachableLegs)
      val paths = legs.map { it.path!! }
      return RouteSummary.Complete(
        knots = paths.first().knots + paths.drop(1).flatMap { it.knots.drop(1) },
        lengthMeters = paths.sumOf { it.lengthMeters },
      )
    }

  /** Inserts [stop] at [position], which must lie between the start and the end. */
  fun insertStop(stop: Knot, position: Int): Route {
    require(position in 1..waypoints.lastIndex) {
      "A stop must be inserted between start and end (1..${waypoints.lastIndex}), was $position"
    }
    return withWaypoints(waypoints.toMutableList().apply { add(position, stop) })
  }

  /**
   * Inserts [stop] into the leg where it adds the least distance. If [stop] cannot be reached from
   * any reachable leg, it is inserted right before the end, so the route reports it as unreachable.
   */
  fun insertStop(stop: Knot): Route {
    val bestPosition =
      legs
        .mapIndexedNotNull { index, leg ->
          val legLength = leg.path?.lengthMeters ?: return@mapIndexedNotNull null
          val toStop = router.shortestPath(leg.from, stop) ?: return@mapIndexedNotNull null
          val fromStop = router.shortestPath(stop, leg.to) ?: return@mapIndexedNotNull null
          index + 1 to toStop.lengthMeters + fromStop.lengthMeters - legLength
        }
        .minByOrNull { (_, addedLength) -> addedLength }
        ?.first ?: waypoints.lastIndex
    return insertStop(stop, bestPosition)
  }

  /** Moves the waypoint at [from] to [to]. Start and end can be moved as well. */
  fun moveWaypoint(from: Int, to: Int): Route {
    require(from in waypoints.indices && to in waypoints.indices) {
      "Positions must be within ${waypoints.indices}, were $from and $to"
    }
    return withWaypoints(waypoints.toMutableList().apply { add(to, removeAt(from)) })
  }

  /** Removes the waypoint at [position]. A route keeps at least two waypoints. */
  fun removeWaypoint(position: Int): Route {
    require(waypoints.size > 2) { "A route needs at least two waypoints" }
    require(position in waypoints.indices) {
      "Position must be within ${waypoints.indices}, was $position"
    }
    return withWaypoints(waypoints.toMutableList().apply { removeAt(position) })
  }

  private fun withWaypoints(newWaypoints: List<Knot>): Route {
    val knownLegs = legs.associateBy { it.from.id to it.to.id }
    return plan(newWaypoints, router) { from, to -> knownLegs[from.id to to.id] }
  }

  companion object {

    /** Plans a route through [waypoints], which must be at least two knots of the same network. */
    fun plan(waypoints: List<Knot>, router: Router): Route =
      plan(waypoints, router) { _, _ -> null }

    private fun plan(
      waypoints: List<Knot>,
      router: Router,
      knownLeg: (Knot, Knot) -> Leg?,
    ): Route {
      require(waypoints.size >= 2) { "A route needs at least two waypoints" }
      require(waypoints.map { it.network }.distinct().size == 1) {
        "All waypoints must belong to the same network"
      }
      val legs = waypoints.zipWithNext { from, to ->
        knownLeg(from, to) ?: Leg(from, to, router.shortestPath(from, to))
      }
      return Route(waypoints, legs, router)
    }
  }
}
