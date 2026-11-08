package com.knoopbyknoop.core

/**
 * Finds ways along paths for route points away from the network, on the device from the installed
 * path data, see `map-planning` "A route point away from the network". The point's position never
 * leaves the device. Not safe for use from several threads at once, as [router] is not.
 *
 * @property geometry the installed network's courses
 */
class RoutePointWays(private val router: PathRouter, private val geometry: NetworkGeometry) {

  /**
   * [point] joined by the shortest way along paths to one of the nearest places on the courses of
   * the [CANDIDATES] nearest connections, or `null` if no way is found, e.g. as the path data is
   * not installed there.
   */
  fun wayFor(point: Knot): Knot? {
    if (point.network != NetworkType.CYCLING) return null
    val routePoint = point.routePoint ?: return null
    val position = GeoPoint(point.latitude, point.longitude)
    return geometry
      .nearestOnCourses(position, MAX_METERS, CANDIDATES)
      .mapNotNull { place -> router.way(position, place.place)?.let { place to it } }
      .minByOrNull { (_, way) -> way.lengthMeters }
      ?.let { (place, way) ->
        val points = joined(joined(listOf(position), way.points), listOf(place.place))
        routePointKnot(
          position,
          point.network,
          place,
          points,
          straight = false,
          placeKind = routePoint.placeKind,
          placeName = routePoint.placeName,
        )
      }
  }

  companion object {

    /**
     * How many connections are tried, so a course across a river does not win, see "Joining a
     * nearby course".
     */
    const val CANDIDATES = 3

    /** A route point further from every course stays a straight line, as for the guided way. */
    const val MAX_METERS = 20_000.0
  }
}

/**
 * This session with ways along paths in place of the straight lines of its route points, where
 * [ways] finds them. Unchanged while [navigating], and on a hiking route, see `map-planning` "A
 * route point away from the network".
 */
fun PlanningSession.withWaysFrom(ways: RoutePointWays, navigating: Boolean): PlanningSession {
  if (navigating || network != NetworkType.CYCLING) return this
  return routePoints
    .filter { it.routePoint?.straight == true }
    .fold(this) { session, point ->
      ways.wayFor(point)?.let { session.withRoutePoint(it) } ?: session
    }
}

/** Whether any route point of this session is joined by a straight line only. */
val PlanningSession.hasStraightLines: Boolean
  get() = routePoints.any { it.routePoint?.straight == true }
