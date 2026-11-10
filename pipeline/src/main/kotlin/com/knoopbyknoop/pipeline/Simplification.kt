package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.METERS_PER_DEGREE
import com.knoopbyknoop.core.Stretch
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Simplifies a course with the Douglas-Peucker algorithm: drops points as long as the simplified
 * line stays within [toleranceMeters] of every original point. The first and last point are kept.
 */
fun simplify(points: List<GeoPoint>, toleranceMeters: Double): List<GeoPoint> {
  if (points.size <= 2) return points
  // A local flat projection in meters is precise enough for single connections.
  val metersPerDegreeLatitude = METERS_PER_DEGREE
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

/**
 * [simplify] with the points where [stretches] start and end kept exactly: the course is cut there,
 * each piece simplified on its own, and the stretches renumbered, see `network-data` "Stretch kept
 * through simplification".
 */
fun simplifyKeeping(
  points: List<GeoPoint>,
  toleranceMeters: Double,
  stretches: List<Stretch>,
): Pair<List<GeoPoint>, List<Stretch>> {
  if (stretches.isEmpty()) return simplify(points, toleranceMeters) to listOf()
  val cuts =
    (stretches.flatMap { listOf(it.first, it.last) } + 0 + points.lastIndex)
      .filter { it in points.indices }
      .toSortedSet()
      .toList()
  val simplified = mutableListOf<GeoPoint>()
  val newIndexOf = HashMap<Int, Int>()
  newIndexOf[cuts.first()] = 0
  simplified += points[cuts.first()]
  for ((from, to) in cuts.zipWithNext()) {
    simplified += simplify(points.subList(from, to + 1), toleranceMeters).drop(1)
    newIndexOf[to] = simplified.lastIndex
  }
  return simplified to
    stretches.map { Stretch(newIndexOf.getValue(it.first), newIndexOf.getValue(it.last)) }
}
