package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Where a route point joins the network, see `map-planning` "A route point on a course" and "A
 * route point away from the network".
 *
 * @property fromId the [Knot.id] of the connection's `from` knot, as in the installed graph
 * @property toId the [Knot.id] of the connection's `to` knot
 * @property fraction where on the connection's course the point joins, from 0 at [fromId] to 1 at
 *   [toId], by length along the course
 * @property place the position where the point joins the course
 * @property way from the point's own position to [place], empty if the point is on the course
 * @property straight whether [way] is only a straight line, as no way along paths is known
 * @property placeKind for a point made from a highlighted place, its [MapPlace.kindKey], see
 *   `map-planning` "Route points from a place"
 * @property placeName that place's name, `null` without one
 */
data class RoutePoint(
  val fromId: Long,
  val toId: Long,
  val fraction: Double,
  val place: GeoPoint,
  val way: List<GeoPoint> = listOf(),
  val straight: Boolean = false,
  val placeKind: String? = null,
  val placeName: String? = null,
) {

  /**
   * Where a way along paths begins near the point: a way runs from the point to the nearest path,
   * then along paths, so this is its second point. The point also counts as reached here, see
   * `knot-navigation` "A point the way does not reach". `null` for a straight line or no way.
   */
  val wayEnd: GeoPoint?
    get() =
      if (straight) null
      else
        way.getOrNull(1)?.takeIf {
          way.size > 2 && roughMeters(way.first(), it) <= WAY_END_REACH_METERS
        }

  /** The length of [way], 0 for a point on the course. */
  val wayMeters: Int
    get() = lengthOf(way).roundToInt()

  /** The fraction from the knot with ID [knotId], one of the connection's two knots. */
  internal fun fractionFrom(knotId: Long): Double = if (knotId == fromId) fraction else 1 - fraction

  internal fun sharesConnectionWith(other: RoutePoint): Boolean =
    (fromId == other.fromId && toId == other.toId) || (fromId == other.toId && toId == other.fromId)
}

/**
 * The nearest place on the course of [connection] to a position.
 *
 * @property fraction from the connection's `from` knot, by length along the course
 * @property distanceMeters from the position to [place]
 */
data class CoursePlace(
  val connection: Connection,
  val fraction: Double,
  val place: GeoPoint,
  val distanceMeters: Double,
)

/**
 * A route point at [position] of [network]: on the course if [way] is empty, otherwise connected to
 * [at] by [way], which runs from [position] to the place. Its ID is derived from [position], and is
 * negative as OpenStreetMap IDs are positive.
 */
fun routePointKnot(
  position: GeoPoint,
  network: NetworkType,
  at: CoursePlace,
  way: List<GeoPoint> = listOf(),
  straight: Boolean = false,
  placeKind: String? = null,
  placeName: String? = null,
): Knot {
  val point = if (way.isEmpty()) at.place else position
  return Knot(
    id = routePointIdOf(point),
    ref = "",
    network = network,
    latitude = point.latitude,
    longitude = point.longitude,
    routePoint =
      RoutePoint(
        at.connection.fromId,
        at.connection.toId,
        at.fraction,
        at.place,
        way,
        straight,
        placeKind,
        placeName,
      ),
  )
}

/** How far from a route point the end of its way may be to count, see [RoutePoint.wayEnd]. */
internal const val WAY_END_REACH_METERS = 100.0

/** A negative ID that is the same for the same position, to the microdegree. */
internal fun routePointIdOf(position: GeoPoint): Long {
  val latitude = ((position.latitude + 90) * 1_000_000).roundToLong()
  val longitude = ((position.longitude + 180) * 1_000_000).roundToLong()
  return -(latitude * 400_000_000L + longitude + 1)
}

/**
 * The connections that join [points] to the network of [graph]: each to both knots of its
 * connection, with the length split by its fraction plus its way, and to each other point on the
 * same connection. A point whose connection is not in [graph] gets none.
 */
internal fun routePointConnections(graph: NetworkGraph, points: List<Knot>): List<Connection> {
  val joined = points.mapNotNull { point ->
    val routePoint = point.routePoint ?: return@mapNotNull null
    graph.connectionBetween(routePoint.fromId, routePoint.toId)?.let { point to it }
  }
  val toKnots = joined.flatMap { (point, connection) ->
    val routePoint = point.routePoint!!
    listOf(connection.fromId, connection.toId).map { knotId ->
      Connection(
        point.id,
        knotId,
        (routePoint.fractionFrom(knotId) * connection.lengthMeters).roundToInt() +
          routePoint.wayMeters,
      )
    }
  }
  val between =
    joined.withIndex().flatMap { (index, first) ->
      joined.drop(index + 1).mapNotNull { second ->
        val (firstPoint, connection) = first
        val (secondPoint, _) = second
        val firstRoutePoint = firstPoint.routePoint!!
        val secondRoutePoint = secondPoint.routePoint!!
        if (!firstRoutePoint.sharesConnectionWith(secondRoutePoint)) return@mapNotNull null
        val apart =
          abs(
            firstRoutePoint.fractionFrom(connection.fromId) -
              secondRoutePoint.fractionFrom(connection.fromId)
          )
        Connection(
          firstPoint.id,
          secondPoint.id,
          (apart * connection.lengthMeters).roundToInt() +
            firstRoutePoint.wayMeters +
            secondRoutePoint.wayMeters,
        )
      }
    }
  return toKnots + between
}

/** Where on a course a position is nearest. */
internal class OnCourse(val fraction: Double, val place: GeoPoint, val distanceMeters: Double)

/** The place on [course] nearest to [position], with its fraction by length along the course. */
internal fun nearestOn(course: List<GeoPoint>, position: GeoPoint): OnCourse =
  Polyline(course).nearest(position).let { OnCourse(it.fraction, it.place, it.distance) }

/**
 * [course] up to and from the place at [fraction] by length, both including the place, which is
 * [at] if given, e.g. the place stored with a route point.
 */
internal fun cutAt(
  course: List<GeoPoint>,
  fraction: Double,
  at: GeoPoint? = null,
): Pair<List<GeoPoint>, List<GeoPoint>> {
  val line = Polyline(course)
  val target = line.length * fraction.coerceIn(0.0, 1.0)
  // The first segment reaching the target, as before, also a segment of no length.
  val index =
    (0 until course.lastIndex).firstOrNull { line.meters[it + 1] >= target }
      ?: return course to listOf(course.last(), course.last())
  val place = at ?: line.pointAt(target)
  val before = (course.take(index + 1) + place).dedupeEnd()
  val after = (listOf(place) + course.drop(index + 1)).dedupeStart()
  return before to after
}

private fun List<GeoPoint>.dedupeEnd() =
  if (size > 2 && this[size - 1] == this[size - 2]) dropLast(1) else this

private fun List<GeoPoint>.dedupeStart() = if (size > 2 && this[0] == this[1]) drop(1) else this

/** A rough distance in meters between two positions, for quick checks. */
internal fun roughMeters(from: GeoPoint, to: GeoPoint): Double {
  val deltaY = (to.latitude - from.latitude) * METERS_PER_DEGREE
  val deltaX = (to.longitude - from.longitude) * METERS_PER_DEGREE * cos(from.latitude * PI / 180)
  return kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
}

/** The stretch of [course] from fraction [from] to fraction [to], in that direction. */
internal fun between(course: List<GeoPoint>, from: Double, to: Double): List<GeoPoint> {
  if (from > to) return between(course, to, from).asReversed()
  if (to <= 0.0) return listOf(course.first(), course.first())
  val upTo = cutAt(course, to).first
  return cutAt(upTo, from / to).second
}

/** [first] followed by [second], without repeating the point where they meet. */
internal fun joined(first: List<GeoPoint>, second: List<GeoPoint>): List<GeoPoint> =
  when {
    first.isEmpty() -> second
    second.isEmpty() -> first
    first.last() == second.first() -> first + second.drop(1)
    else -> first + second
  }

/**
 * What a tap on the map means while planning, see `map-planning` "Choose a route point on the map".
 */
sealed interface MapTap {

  /** A knot was tapped; it is planned as before. */
  data class OnKnot(val knot: Knot) : MapTap

  /** A route point was tapped; nothing is offered and the route does not change. */
  data class OnRoutePoint(val point: Knot) : MapTap

  /**
   * A place to offer as a route point.
   *
   * @property onCourse the nearest place on a course within the touch tolerance, if any
   * @property canAddStop whether "Add stop here" is offered, once a route exists
   * @property place the highlighted place tapped, shown with its name, see `map-planning` "Tapping
   *   a place on the map"; its position is the offer's
   */
  data class Offer(
    val position: GeoPoint,
    val onCourse: CoursePlace?,
    val canAddStop: Boolean,
    val place: MapPlace? = null,
  ) : MapTap

  /**
   * A highlighted place too far from the network to plan to: only its name is shown, see
   * `map-planning` "Place far from the network".
   */
  data class PlaceOnly(val place: MapPlace) : MapTap

  /**
   * The place is farther than [limitMeters] from every course of the network; nothing is offered,
   * see `map-planning` "Too far from the network".
   */
  data class TooFar(val limitMeters: Double) : MapTap
}

/**
 * How far from the network a route point may be, see `map-planning` "Choose a route point on the
 * map": as far as ways are looked for on a cycling route, and an hour's walk on a hiking route.
 */
fun maxRoutePointMeters(network: NetworkType): Double =
  when (network) {
    NetworkType.CYCLING -> RoutePointWays.MAX_METERS
    NetworkType.HIKING -> 5_000.0
  }

/**
 * What a tap at [position] means, with [knot] the knot within [radiusMeters] if any, see
 * `map-planning` "Knots go first" and "A route point goes first". [geometry] is the installed
 * network's, to find a course within reach. [place] is the highlighted place under the tap, if any,
 * which comes after knots and route points, see `map-planning` "Tapping a place on the map".
 */
fun PlanningSession.tapAt(
  position: GeoPoint,
  radiusMeters: Double,
  knot: Knot?,
  geometry: NetworkGeometry?,
  place: MapPlace? = null,
): MapTap {
  if (knot != null) return MapTap.OnKnot(knot)
  routePoints
    .firstOrNull { roughMeters(position, GeoPoint(it.latitude, it.longitude)) <= radiusMeters }
    ?.let {
      return MapTap.OnRoutePoint(it)
    }
  if (place != null) {
    // At the place itself, not on a course beside it, see `map-planning` "Place next to a course".
    if (geometry != null) {
      val limit = maxRoutePointMeters(network)
      if (geometry.nearestOnCourses(place.position, limit).isEmpty()) return MapTap.PlaceOnly(place)
    }
    return MapTap.Offer(place.position, onCourse = null, canAddStop = route != null, place = place)
  }
  val onCourse = geometry?.nearestOnCourses(position, radiusMeters)?.firstOrNull()
  if (onCourse == null && geometry != null) {
    val limit = maxRoutePointMeters(network)
    if (geometry.nearestOnCourses(position, limit).isEmpty()) return MapTap.TooFar(limit)
  }
  return MapTap.Offer(position, onCourse, canAddStop = route != null)
}

/**
 * The route point for [offer]: on the course where one is within reach, otherwise connected to the
 * nearest place on a course by a straight line, until a way along paths is found, see
 * `map-planning` "A route point away from the network". `null` if the network has no course without
 * a ferry.
 */
fun PlanningSession.routePointFor(offer: MapTap.Offer, geometry: NetworkGeometry): Knot? {
  offer.onCourse?.let {
    return routePointKnot(
      offer.position,
      network,
      it,
      placeKind = offer.place?.kindKey,
      placeName = offer.place?.name,
    )
  }
  val nearest =
    geometry.nearestOnCourses(offer.position, ANY_DISTANCE_METERS).firstOrNull() ?: return null
  return routePointKnot(
    offer.position,
    network,
    nearest,
    way = listOf(offer.position, nearest.place),
    straight = true,
    placeKind = offer.place?.kindKey,
    placeName = offer.place?.name,
  )
}

/** Far enough to find the nearest course from anywhere in the networks' area. */
private const val ANY_DISTANCE_METERS = 1_000_000.0

/**
 * This session with route points whose connection is no longer in the network joined anew to the
 * nearest course of [geometry], as a newly chosen place is, see `map-planning` "Connection gone".
 */
fun PlanningSession.reattached(geometry: NetworkGeometry): PlanningSession {
  val gone = routePoints.filter { point ->
    val routePoint = point.routePoint!!
    graph.connectionBetween(routePoint.fromId, routePoint.toId) == null
  }
  return gone.fold(this) { session, point ->
    val position = GeoPoint(point.latitude, point.longitude)
    val nearest =
      geometry.nearestOnCourses(position, 1_000_000.0).firstOrNull() ?: return@fold session
    val joined =
      point.copy(
        routePoint =
          RoutePoint(
            nearest.connection.fromId,
            nearest.connection.toId,
            nearest.fraction,
            nearest.place,
            if (nearest.distanceMeters < 1.0) listOf() else listOf(position, nearest.place),
            straight = nearest.distanceMeters >= 1.0,
            placeKind = point.routePoint?.placeKind,
            placeName = point.routePoint?.placeName,
          )
      )
    session.withRoutePoint(joined)
  }
}
