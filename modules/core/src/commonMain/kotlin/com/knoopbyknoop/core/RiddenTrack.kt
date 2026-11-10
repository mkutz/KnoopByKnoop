package com.knoopbyknoop.core

import kotlin.math.abs
import kotlin.math.max

/**
 * The track ridden during a navigation, from the fixes it gets, see `knot-navigation` "Ridden
 * track".
 *
 * @property points the fixes on the track, in the order they were ridden
 * @property network the route's network, for the speed a fix may not exceed
 */
data class RiddenTrack(
  val points: List<Fix> = listOf(),
  val network: NetworkType = NetworkType.CYCLING,
) {

  /**
   * This track with [fix] appended, unless it is too inaccurate to place the rider, so close to the
   * last point that it may be scatter rather than movement, or so far that the rider could not have
   * got there, see `knot-navigation` "Ridden track". A last point the new fix comes straight back
   * from, a spike out and back, is taken out first.
   */
  fun add(fix: Fix): RiddenTrack {
    if (fix.accuracyMeters > MAXIMUM_ACCURACY_METERS) return this
    val last = points.lastOrNull() ?: return copy(points = listOf(fix))
    val beforeLast = points.getOrNull(points.lastIndex - 1)
    if (beforeLast != null && isSpike(beforeLast, last, fix)) {
      return copy(points = points.dropLast(1)).addAfterLast(fix)
    }
    return addAfterLast(fix)
  }

  /** [fix] after the last point, if far enough from it and not too fast, without spike checks. */
  private fun addAfterLast(fix: Fix): RiddenTrack {
    val last = points.last()
    val distance = distanceBetween(last, fix)
    if (distance <= max(MINIMUM_DISTANCE_METERS, fix.accuracyMeters + last.accuracyMeters)) {
      return this
    }
    val seconds = max(fix.timeMillis - last.timeMillis, 1_000L) / 1000.0
    if (distance / seconds > maximumSpeed(network)) return this
    return copy(points = points + fix)
  }

  /**
   * Whether the track turns sharply at [spike] between [from] and [fix], with [spike] farther from
   * the line between them than their inaccuracies together: a wrong fix to the side, see the design
   * of `smooth-ridden-track`. A right-angle corner is not sharp enough.
   */
  private fun isSpike(from: Fix, spike: Fix, fix: Fix): Boolean {
    val back = bearing(spike.point, from.point)
    val on = bearing(spike.point, fix.point)
    val turn = abs(((back - on + 540) % 360) - 180)
    if (turn >= SPIKE_ANGLE_DEGREES) return false
    val off =
      distanceToLine(
        spike.latitude,
        spike.longitude,
        GeoPoint(from.latitude, from.longitude),
        GeoPoint(fix.latitude, fix.longitude),
      )
    if (off <= from.accuracyMeters + fix.accuracyMeters) return false
    val seconds = max(fix.timeMillis - from.timeMillis, 1_000L) / 1000.0
    val there = distanceBetween(from, spike) + distanceBetween(spike, fix)
    return there / seconds > maximumSpeed(network) / 2
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
    return bearing(from.point, newest.point)
  }

  private companion object {
    const val MAXIMUM_ACCURACY_METERS = 50.0
    const val SPIKE_ANGLE_DEGREES = 75.0

    /** Faster than this, a fix is a jump of the position, not the rider. */
    fun maximumSpeed(network: NetworkType) =
      when (network) {
        NetworkType.CYCLING -> 15.0
        NetworkType.HIKING -> 4.0
      }

    const val MINIMUM_DISTANCE_METERS = 5.0
    const val DIRECTION_DISTANCE_METERS = 10.0
    const val DIRECTION_SPAN_MILLIS = 30_000L
    const val STANDING_AFTER_MILLIS = 10_000L
  }
}

private fun distanceBetween(from: Fix, to: Fix) =
  distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
