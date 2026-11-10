package com.knoopbyknoop.core

/** Which rule decides what the map shows while following the rider, see "Zoom while following". */
enum class MapFocusKind {
  /** The navigation zoom of the platform. */
  BASE,

  /** From the reminder before the next knot until it is passed, see "Zoom at a knot". */
  KNOT,

  /** For a while after passing a knot, see "Zoom out after passing a knot". */
  AFTER_KNOT,

  /** Close before and on a tricky stretch of the route, see "Zoom at a tricky spot". */
  TRICKY_SPOT,

  /**
   * Off route: the rider and the end of the way to the route, see "Way to the route on the map".
   */
  OFF_ROUTE,
}

/**
 * What the map shows while following the rider, see `knot-navigation` "Zoom while following". The
 * platform sets its zoom once when [kind] or [key] change: a circle around the rider reaching the
 * farthest of [points], no closer than [closestMeters] and no farther than [farthestMeters] across
 * the screen's narrower side; after a knot never closer than the navigation zoom, and for [BASE]
 * the navigation zoom. While they stay the same, it only pans.
 *
 * @property key tells apart two foci of the same kind, e.g. two knots in a row
 * @property points what has to be in view with the rider
 */
data class MapFocus(
  val kind: MapFocusKind,
  val key: Int,
  val points: List<GeoPoint>,
  val closestMeters: Double?,
  val farthestMeters: Double?,
) {
  /**
   * How much the view spans across the narrower side of the map with the rider at [rider]: twice
   * the distance to the farthest of [points], within the limits; `null` for the navigation zoom.
   */
  fun acrossMeters(rider: Fix): Double? {
    if (kind == MapFocusKind.BASE) return null
    val radius =
      points.maxOfOrNull {
        distanceMeters(rider.latitude, rider.longitude, it.latitude, it.longitude)
      } ?: return null
    var across = radius * 2
    closestMeters?.let { across = maxOf(across, it) }
    farthestMeters?.let { across = minOf(across, it) }
    return across
  }

  /**
   * The square of [acrossMeters] around [rider], for the platform to fit its zoom to, so its own
   * screen units apply; `null` for the navigation zoom.
   */
  fun viewAround(rider: Fix): GeoBounds? {
    val half = (acrossMeters(rider) ?: return null) / 2
    return boxAround(rider.point, half)
  }

  companion object {
    internal val BASE = MapFocus(MapFocusKind.BASE, 0, listOf(), null, null)

    /** The closest view at a knot. */
    const val KNOT_CLOSEST_METERS = 300.0

    /** How much of the route after the knot is shown at a knot. */
    const val KNOT_AFTER_METERS = 100.0

    /** The farthest view after a knot. */
    const val AFTER_KNOT_FARTHEST_METERS = 3_000.0

    /** How long the view stays zoomed out after passing a knot. */
    const val AFTER_KNOT_MILLIS = 10_000L

    /** The closest view at a tricky spot. */
    const val TRICKY_CLOSEST_METERS = 200.0

    /** From this far before a tricky spot, the map zooms in on it. */
    const val TRICKY_AHEAD_METERS = 150.0

    /** Until this far past a tricky spot, the map stays zoomed in. */
    const val TRICKY_PAST_METERS = 20.0

    /** How much of the way after a tricky spot is shown. */
    const val TRICKY_AFTER_METERS = 30.0
  }
}

/**
 * A tricky stretch on the route: from [start] to [end] along the route as [Navigation]'s places, on
 * the course of [leg] from [startMeters] to [endMeters].
 */
internal class RouteTricky(
  val start: Double,
  val end: Double,
  val leg: Int,
  val startMeters: Double,
  val endMeters: Double,
)

/** The tricky stretches of the route's legs, placed along the route. */
internal fun routeTricky(
  courses: List<List<GeoPoint>>?,
  offsets: List<Double>,
): List<RouteTricky> {
  courses ?: return listOf()
  return courses.flatMapIndexed { leg, course ->
    val length = lengthOf(course)
    if (length <= 0) return@flatMapIndexed listOf()
    val legLength = offsets[leg + 1] - offsets[leg]
    trickyStretches(course).map {
      RouteTricky(
        offsets[leg] + it.startMeters / length * legLength,
        offsets[leg] + it.endMeters / length * legLength,
        leg,
        it.startMeters,
        it.endMeters,
      )
    }
  }
}
