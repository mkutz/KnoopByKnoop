package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The mean radius of the earth, as used for every distance in the apps and the pipeline. */
const val EARTH_RADIUS_METERS = 6_371_008.8

/** How long a degree of latitude is, and a degree of longitude at the equator. */
const val METERS_PER_DEGREE = EARTH_RADIUS_METERS * PI / 180

/** The great-circle distance between two positions. */
fun distanceMeters(
  fromLatitude: Double,
  fromLongitude: Double,
  toLatitude: Double,
  toLongitude: Double,
): Double {
  val deltaLatitude = (toLatitude - fromLatitude) * PI / 180
  val deltaLongitude = (toLongitude - fromLongitude) * PI / 180
  val a =
    sin(deltaLatitude / 2).let { it * it } +
      cos(fromLatitude * PI / 180) *
        cos(toLatitude * PI / 180) *
        sin(deltaLongitude / 2).let { it * it }
  return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

/** The great-circle distance between two points. */
internal fun distanceMeters(from: GeoPoint, to: GeoPoint): Double =
  distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)

/** How many degrees [meters] are at [latitude]: north–south and east–west. */
internal class DegreeMargins(val latitude: Double, val longitude: Double)

/** The degrees [meters] span at [latitude]. */
internal fun marginsAt(latitude: Double, meters: Double): DegreeMargins {
  val latitudeMargin = meters / METERS_PER_DEGREE
  return DegreeMargins(
    latitudeMargin,
    latitudeMargin / cos(latitude * PI / 180).coerceAtLeast(0.01),
  )
}

/** The box reaching [meters] from [point] in every direction. */
internal fun boxAround(point: GeoPoint, meters: Double): GeoBounds {
  val margins = marginsAt(point.latitude, meters)
  return GeoBounds(
    point.latitude - margins.latitude,
    point.longitude - margins.longitude,
    point.latitude + margins.latitude,
    point.longitude + margins.longitude,
  )
}

/** The initial great-circle bearing from [from] to [to], clockwise from north, 0° to 360°. */
internal fun bearing(from: GeoPoint, to: GeoPoint): Double {
  val fromLatitude = from.latitude * PI / 180
  val toLatitude = to.latitude * PI / 180
  val deltaLongitude = (to.longitude - from.longitude) * PI / 180
  val y = sin(deltaLongitude) * cos(toLatitude)
  val x =
    cos(fromLatitude) * sin(toLatitude) - sin(fromLatitude) * cos(toLatitude) * cos(deltaLongitude)
  return (atan2(y, x) * 180 / PI + 360) % 360
}

/** The turn from bearing [from] to bearing [to]: negative to the left, positive to the right. */
internal fun signedAngle(from: Double, to: Double): Double {
  var angle = (to - from) % 360
  if (angle > 180) angle -= 360
  if (angle < -180) angle += 360
  return angle
}

/** The smaller angle between two bearings, 0° to 180°. */
internal fun angleBetween(first: Double, second: Double): Double = abs(signedAngle(first, second))

/** The point [share] of the way from [from] to [to], not clamped. */
internal fun pointBetween(from: GeoPoint, to: GeoPoint, share: Double): GeoPoint =
  GeoPoint(
    from.latitude + (to.latitude - from.latitude) * share,
    from.longitude + (to.longitude - from.longitude) * share,
  )

/** Where on a [Polyline] a position is nearest. */
internal class OnLine(
  /** How far along the line, in meters. */
  val meters: Double,
  /** How far along the line, from 0 to 1. */
  val fraction: Double,
  /** The nearest place on the line. */
  val place: GeoPoint,
  /** How far the position is from [place]. */
  val distance: Double,
)

/**
 * A line through [points] with the meters along it, the one way the core measures along courses,
 * ways and tracks. Meters are great-circle distances; the nearest place is found in a flat
 * projection around the position, which differs by centimeters on the segments used.
 */
internal class Polyline(val points: List<GeoPoint>, knownMeters: List<Double>? = null) {

  init {
    require(points.isNotEmpty()) { "A line needs a point" }
    require(knownMeters == null || knownMeters.size == points.size) { "A meter for every point" }
  }

  /** The meters along the line at each of [points], from 0; measured unless given. */
  val meters: List<Double> by lazy {
    knownMeters
      ?: run {
        var total = 0.0
        points.mapIndexed { index, point ->
          if (index > 0) total += distanceMeters(points[index - 1], point)
          total
        }
      }
  }

  val length: Double
    get() = meters.last()

  /** The point [at] meters along the line, clamped to its ends. */
  fun pointAt(at: Double): GeoPoint {
    if (points.size == 1) return points.first()
    val clamped = at.coerceIn(0.0, length)
    val index = meters.indexOfFirst { it >= clamped }.coerceAtLeast(1)
    val step = meters[index] - meters[index - 1]
    val share = if (step == 0.0) 0.0 else (clamped - meters[index - 1]) / step
    return pointBetween(points[index - 1], points[index], share)
  }

  /**
   * The line from [from] to [to] meters along it, both clamped to its ends, with the points
   * between; reversed when [from] is beyond [to].
   */
  fun stretch(from: Double, to: Double): List<GeoPoint> {
    if (from > to) return stretch(to, from).asReversed()
    val start = from.coerceIn(0.0, length)
    val end = to.coerceIn(0.0, length)
    val between =
      points.indices.filter { meters[it] > start && meters[it] < end }.map { points[it] }
    return listOf(pointAt(start)) + between + pointAt(end)
  }

  /** The place on the line nearest to [position]. */
  fun nearest(position: GeoPoint): OnLine {
    if (points.size == 1) {
      return OnLine(0.0, 0.0, points.first(), distanceMeters(position, points.first()))
    }
    val flat = FlatProjection(position)
    var bestIndex = 0
    var bestShare = 0.0
    var bestDistance = Double.MAX_VALUE
    for (index in 0 until points.lastIndex) {
      val (share, distance) = flat.nearestOnSegment(points[index], points[index + 1])
      if (distance < bestDistance) {
        bestIndex = index
        bestShare = share
        bestDistance = distance
      }
    }
    val along = meters[bestIndex] + (meters[bestIndex + 1] - meters[bestIndex]) * bestShare
    return OnLine(
      along,
      if (length > 0) along / length else 0.0,
      pointBetween(points[bestIndex], points[bestIndex + 1], bestShare),
      bestDistance,
    )
  }

  /** How far [position] is from the line, without the meters along it. */
  fun distanceTo(position: GeoPoint): Double {
    if (points.size == 1) return distanceMeters(position, points.first())
    val flat = FlatProjection(position)
    return (0 until points.lastIndex).minOf {
      flat.nearestOnSegment(points[it], points[it + 1]).second
    }
  }
}

/** Meters east and north of [origin], flat, for the short distances around a position. */
internal class FlatProjection(private val origin: GeoPoint) {
  private val metersPerDegreeLongitude = METERS_PER_DEGREE * cos(origin.latitude * PI / 180)

  fun x(point: GeoPoint) = (point.longitude - origin.longitude) * metersPerDegreeLongitude

  fun y(point: GeoPoint) = (point.latitude - origin.latitude) * METERS_PER_DEGREE

  /** How far along the segment, from 0 to 1, its place nearest to the origin is, and how far. */
  fun nearestOnSegment(from: GeoPoint, to: GeoPoint): Pair<Double, Double> {
    val fromX = x(from)
    val fromY = y(from)
    val deltaX = x(to) - fromX
    val deltaY = y(to) - fromY
    val lengthSquared = deltaX * deltaX + deltaY * deltaY
    val share =
      if (lengthSquared == 0.0) 0.0
      else (-(fromX * deltaX + fromY * deltaY) / lengthSquared).coerceIn(0.0, 1.0)
    val nearestX = fromX + share * deltaX
    val nearestY = fromY + share * deltaY
    return share to sqrt(nearestX * nearestX + nearestY * nearestY)
  }
}

/** The length of a line through [points], 0 without any. */
internal fun lengthOf(points: List<GeoPoint>): Double =
  if (points.isEmpty()) 0.0 else Polyline(points).length

/**
 * The point [at] meters along [points], clamped to their ends, `null` without any; reads only as
 * many of [points] as it needs, e.g. of the rest of a route.
 */
internal fun pointAlong(points: Sequence<GeoPoint>, at: Double): GeoPoint? {
  val needed = ArrayList<GeoPoint>()
  var covered = 0.0
  for (point in points) {
    needed.lastOrNull()?.let { covered += distanceMeters(it, point) }
    needed += point
    if (covered >= at) break
  }
  return if (needed.isEmpty()) null else Polyline(needed).pointAt(at)
}
