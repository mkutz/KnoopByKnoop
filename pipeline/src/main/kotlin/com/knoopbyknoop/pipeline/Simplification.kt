package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GeoPoint
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Simplifies a course with the Douglas-Peucker algorithm: drops points as long as the simplified
 * line stays within [toleranceMeters] of every original point. The first and last point are kept.
 */
fun simplify(points: List<GeoPoint>, toleranceMeters: Double): List<GeoPoint> {
  if (points.size <= 2) return points
  // A local flat projection in meters is precise enough for single connections.
  val metersPerDegreeLatitude = Math.toRadians(EARTH_RADIUS_METERS)
  val metersPerDegreeLongitude = metersPerDegreeLatitude * cos(Math.toRadians(points[0].latitude))
  val x =
    DoubleArray(points.size) {
      (points[it].longitude - points[0].longitude) * metersPerDegreeLongitude
    }
  val y =
    DoubleArray(points.size) {
      (points[it].latitude - points[0].latitude) * metersPerDegreeLatitude
    }

  val keep =
    BooleanArray(points.size).apply {
      this[0] = true
      this[lastIndex] = true
    }
  val ranges = ArrayDeque(listOf(0 to points.lastIndex))
  while (ranges.isNotEmpty()) {
    val (first, last) = ranges.removeLast()
    var farthest = -1
    var farthestDistance = toleranceMeters
    for (index in first + 1 until last) {
      val distance = distanceToSegment(x[index], y[index], x[first], y[first], x[last], y[last])
      if (distance > farthestDistance) {
        farthest = index
        farthestDistance = distance
      }
    }
    if (farthest >= 0) {
      keep[farthest] = true
      ranges.addLast(first to farthest)
      ranges.addLast(farthest to last)
    }
  }
  return points.filterIndexed { index, _ -> keep[index] }
}

private fun distanceToSegment(
  pointX: Double,
  pointY: Double,
  startX: Double,
  startY: Double,
  endX: Double,
  endY: Double,
): Double {
  val segmentX = endX - startX
  val segmentY = endY - startY
  val lengthSquared = segmentX * segmentX + segmentY * segmentY
  val fraction =
    if (lengthSquared == 0.0) 0.0
    else
      (((pointX - startX) * segmentX + (pointY - startY) * segmentY) / lengthSquared).coerceIn(
        0.0,
        1.0,
      )
  return hypot(pointX - (startX + fraction * segmentX), pointY - (startY + fraction * segmentY))
}
