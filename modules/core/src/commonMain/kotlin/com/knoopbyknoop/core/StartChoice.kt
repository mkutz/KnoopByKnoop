package com.knoopbyknoop.core

import kotlin.math.roundToInt

/**
 * What Navigate offers when the rider is away from the start but much closer to the route, see
 * `knot-navigation` "Start on the route nearby": riding to [start], or starting the plan at
 * [nearby] with [PlanningSession.startOnRouteAt].
 *
 * @property start the plan's start
 * @property nearby a knot of the route, or a route point at the nearest place on its course, before
 *   the first stop
 * @property startMeters the straight distance to [start]
 * @property nearbyMeters the straight distance to [nearby]
 */
data class StartChoice(
  val start: Knot,
  val nearby: Knot,
  val startMeters: Int,
  val nearbyMeters: Int,
) {

  companion object {

    /** Farther than this from the start, the rider is not at it. */
    const val AT_START_METERS = 30.0

    /** Up to this far along the route from the start, riding to the start is no detour. */
    const val NEAR_START_ALONG_METERS = 200.0

    /** A place within this of a knot of the route is that knot. */
    const val AT_KNOT_METERS = 30.0

    /** The nearby place must be less than this share of the distance to the start. */
    const val NEARBY_SHARE = 0.5

    /**
     * Whether to ask at [position], and what, for the route planned in [session]; `null` when
     * navigation simply leads to the start. [geometry] is the installed network's courses, `null`
     * without them; then only the knots of the route count.
     */
    fun of(session: PlanningSession, geometry: NetworkGeometry?, position: GeoPoint): StartChoice? {
      val route = session.route ?: return null
      // The part before the first stop, or the whole route without stops.
      val knots = route.legs.firstOrNull()?.path?.knots?.takeIf { it.size >= 2 } ?: return null
      val start = knots.first()
      val startMeters = start.reachPositions.minOf { distanceMeters(position, it) }
      if (startMeters <= AT_START_METERS) return null
      val courses =
        geometry?.withRoutePoints(session.routeGraph)?.let { network ->
          knots.zipWithNext { from, to -> network.courseOf(from, to) ?: return@let null }
        }
      val nearby =
        if (courses != null) nearOnCourse(session, geometry, knots, courses, position)
        else nearKnot(session.routeGraph, knots, position)
      // Starting at the end would leave no route.
      if (nearby == null || (route.legs.size == 1 && nearby.id == knots.last().id)) return null
      val nearbyMeters = distanceMeters(position, GeoPoint(nearby.latitude, nearby.longitude))
      if (nearbyMeters >= startMeters * NEARBY_SHARE) return null
      return StartChoice(start, nearby, startMeters.roundToInt(), nearbyMeters.roundToInt())
    }

    /**
     * The knot of [knots] at the nearest place on their [courses], or a route point there; `null`
     * when that place is close to the start along the route.
     */
    private fun nearOnCourse(
      session: PlanningSession,
      geometry: NetworkGeometry?,
      knots: List<Knot>,
      courses: List<List<GeoPoint>>,
      position: GeoPoint,
    ): Knot? {
      var before = 0.0
      var best: Triple<Int, OnLine, Double>? = null
      for ((leg, course) in courses.withIndex()) {
        val line = Polyline(course)
        val on = line.nearest(position)
        if (best == null || on.distance < best.second.distance) best = Triple(leg, on, before)
        before += line.length
      }
      val (leg, on, legStart) = best ?: return null
      if (legStart + on.meters <= NEAR_START_ALONG_METERS) return null
      val from = knots[leg]
      val to = knots[leg + 1]
      val atKnot =
        listOf(from, to).firstOrNull { knot ->
          knot.reachPositions.any { distanceMeters(on.place, it) <= AT_KNOT_METERS }
        }
      if (atKnot != null) return atKnot
      // The connection of the leg at that place, for a route point as "Start here" places it.
      val coursePlace =
        geometry?.nearestOnCourses(on.place, AT_KNOT_METERS, count = 8)?.firstOrNull {
          // A route point splits its connection, so its knots count for the leg too.
          val ids = idsOf(from) + idsOf(to)
          it.connection.fromId in ids && it.connection.toId in ids
        } ?: return null
      return routePointKnot(on.place, session.network, coursePlace)
    }

    private fun idsOf(knot: Knot): Set<Long> =
      setOfNotNull(knot.id, knot.routePoint?.fromId, knot.routePoint?.toId)

    /** Without courses: the knot of [knots] nearest to [position], away from the start. */
    private fun nearKnot(graph: NetworkGraph, knots: List<Knot>, position: GeoPoint): Knot? {
      var along = 0.0
      val candidates = mutableListOf<Knot>()
      for ((from, to) in knots.zipWithNext()) {
        along += graph.connectionBetween(from.id, to.id)?.lengthMeters ?: 0
        if (along > NEAR_START_ALONG_METERS && !to.isRoutePoint) candidates += to
      }
      return candidates.minByOrNull { knot ->
        knot.reachPositions.minOf { distanceMeters(position, it) }
      }
    }
  }
}
