package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos

/**
 * The track ridden during a navigation, from the fixes it gets, see `knot-navigation` "Ridden
 * track".
 *
 * @property points the fixes on the track, in the order they were ridden
 */
data class RiddenTrack(val points: List<Fix> = listOf()) {

  /**
   * This track with [fix] appended, unless it is too inaccurate to place the rider, or so close to
   * the last point that the rider has not really moved, e.g. waiting at a ferry.
   */
  fun add(fix: Fix): RiddenTrack {
    if (fix.accuracyMeters > MAXIMUM_ACCURACY_METERS) return this
    val last = points.lastOrNull()
    if (last != null && distanceBetween(last, fix) < MINIMUM_DISTANCE_METERS) return this
    return RiddenTrack(points + fix)
  }

  /**
   * The riding direction at [fix], clockwise from north, from the most recent movement on this
   * track rather than from the phone's compass or course, see `knot-navigation` "Riding direction
   * on the map". `null` when the rider has not moved recently, or not far enough for a direction.
   */
  fun directionAt(fix: Fix): Double? {
    val newest = points.lastOrNull() ?: return null
    if (fix.timeMillis - newest.timeMillis > STANDING_AFTER_MILLIS) return null
    val from =
      points.asReversed().drop(1).firstOrNull {
        distanceBetween(it, newest) >= DIRECTION_DISTANCE_METERS
      } ?: return null
    if (newest.timeMillis - from.timeMillis > DIRECTION_SPAN_MILLIS) return null
    return bearing(from, newest)
  }

  private companion object {
    const val MAXIMUM_ACCURACY_METERS = 50.0
    const val MINIMUM_DISTANCE_METERS = 5.0
    const val DIRECTION_DISTANCE_METERS = 10.0
    const val DIRECTION_SPAN_MILLIS = 30_000L
    const val STANDING_AFTER_MILLIS = 10_000L
  }
}

private fun distanceBetween(from: Fix, to: Fix) =
  distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)

/** The bearing from [from] to [to], clockwise from north, on the short distances of a track. */
private fun bearing(from: Fix, to: Fix): Double {
  val north = to.latitude - from.latitude
  val east = (to.longitude - from.longitude) * cos(from.latitude * PI / 180)
  val degrees = atan2(east, north) * 180 / PI
  return (degrees + 360) % 360
}
