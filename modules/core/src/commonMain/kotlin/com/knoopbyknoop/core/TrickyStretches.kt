package com.knoopbyknoop.core

import kotlin.math.abs

/**
 * A stretch of a course with two or more sharp turns close together, see `knot-navigation` "Tricky
 * spots on the route".
 *
 * @property startMeters where its first turn is, along the course
 * @property endMeters where its last turn is, along the course
 * @property turns how many sharp turns it has
 */
data class TrickyStretch(val startMeters: Double, val endMeters: Double, val turns: Int)

/** The tricky stretches of each leg's course, empty for every leg without [courses]. */
internal fun trickyStretchesOf(
  courses: List<List<GeoPoint>>?,
  legs: Int,
): List<List<TrickyStretch>> = courses?.map(::trickyStretches) ?: List(legs) { listOf() }

/**
 * The tricky stretches along [course]: the turn at each of its points is measured between the
 * direction over the [TRICKY_SPAN_METERS] before it and after it, which smooths out small zigzags
 * of the data. Neighbouring points that measure one turn count once, and turns of [degrees] or more
 * within [gapMeters] of each other make one stretch, by default [TRICKY_DEGREES] and
 * [TRICKY_GAP_METERS].
 */
fun trickyStretches(
  course: List<GeoPoint>,
  degrees: Double = TRICKY_DEGREES,
  gapMeters: Double = TRICKY_GAP_METERS,
): List<TrickyStretch> {
  if (course.size < 3) return listOf()
  val along = course.runningCumulative()
  val length = along.last()
  val sharp = mutableListOf<Double>()
  for (index in 1 until course.lastIndex) {
    val at = along[index]
    if (at < TRICKY_SPAN_METERS || length - at < TRICKY_SPAN_METERS) continue
    val before = pointAlongCourse(course, along, at - TRICKY_SPAN_METERS)
    val after = pointAlongCourse(course, along, at + TRICKY_SPAN_METERS)
    val angle = signedAngle(heading(before, course[index]), heading(course[index], after))
    if (abs(angle) >= degrees) sharp += at
  }
  // Points closer than the two spans of a measurement see the same bend.
  val turns = mutableListOf<Double>()
  sharp.forEachIndexed { index, at ->
    if (index == 0 || at - sharp[index - 1] > 2 * TRICKY_SPAN_METERS) turns += at
  }
  val stretches = mutableListOf<TrickyStretch>()
  var first = 0
  for (index in 1..turns.size) {
    if (index == turns.size || turns[index] - turns[index - 1] > gapMeters) {
      val count = index - first
      if (count >= 2) stretches += TrickyStretch(turns[first], turns[index - 1], count)
      first = index
    }
  }
  return stretches
}

/** How far before and after a point its turn is measured. */
const val TRICKY_SPAN_METERS = 10.0

/** A turn at least this sharp counts for a tricky stretch. */
const val TRICKY_DEGREES = 60.0

/** Sharp turns at most this far apart make one stretch. */
const val TRICKY_GAP_METERS = 100.0

private fun List<GeoPoint>.runningCumulative(): List<Double> {
  var total = 0.0
  return mapIndexed { index, point ->
    if (index > 0) {
      val before = this[index - 1]
      total += distanceMeters(before.latitude, before.longitude, point.latitude, point.longitude)
    }
    total
  }
}

private fun pointAlongCourse(
  course: List<GeoPoint>,
  along: List<Double>,
  meters: Double,
): GeoPoint {
  val index = along.indexOfFirst { it >= meters }.coerceAtLeast(1)
  val step = along[index] - along[index - 1]
  val share = if (step == 0.0) 0.0 else (meters - along[index - 1]) / step
  val from = course[index - 1]
  val to = course[index]
  return GeoPoint(
    from.latitude + (to.latitude - from.latitude) * share,
    from.longitude + (to.longitude - from.longitude) * share,
  )
}
