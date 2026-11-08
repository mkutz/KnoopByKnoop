package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** What a line of a [JunctionPicture] is, for its width and colour. */
enum class PictureLineKind(internal val code: Char) {
  /** The way up to the junction. */
  WAY_BEFORE('B'),

  /** The way from the junction on, with an arrow at its end. */
  WAY_AFTER('A'),

  /** Primary, secondary, tertiary, trunk and motorway, drawn wide. */
  MAIN_ROAD('M'),

  /** Another street, as a short exit. */
  STREET('S'),

  /** A cycle path, as a short exit in its own colour. */
  CYCLE_PATH('C'),
}

/**
 * A point of a [JunctionPicture] in meters from the junction, [x] to the right and [y] ahead in the
 * riding direction.
 */
data class PicturePoint(val x: Int, val y: Int)

data class PictureLine(val kind: PictureLineKind, val points: List<PicturePoint>)

/**
 * A drawing of a doubtful junction, see `knot-navigation` "Junction picture": the riding direction
 * up, about [ACROSS_METERS] across with the junction at two thirds of the height, so the view runs
 * from [BOTTOM_METERS] to [TOP_METERS] ahead and [ACROSS_METERS] / 2 to either side.
 */
data class JunctionPicture(val lines: List<PictureLine>) {

  /**
   * The picture as short text for the iPhone's Live Activity, e.g. `B0,-34 0,0|A0,0 -40,0`: per
   * line its kind and points, see "Live Activity content".
   */
  fun encoded(): String =
    lines.joinToString("|") { line ->
      line.kind.code + line.points.joinToString(" ") { "${it.x},${it.y}" }
    }

  companion object {
    const val ACROSS_METERS = 95.0
    const val TOP_METERS = 32.0
    const val BOTTOM_METERS = -63.0

    /** How much of the way before and after the junction is drawn. */
    const val BEFORE_METERS = 34.0
    const val AFTER_METERS = 40.0

    /** Streets and cycle paths are drawn up to this far from the junction. */
    const val EXIT_METERS = 50.0

    /** The riding direction is taken from the way between these distances before the junction. */
    internal const val DIRECTION_FROM_METERS = 34.0
    internal const val DIRECTION_TO_METERS = 10.0

    /** Reads [encoded] text back, as the iPhone's widget does; `null` for anything else. */
    fun decoded(text: String): JunctionPicture? {
      if (text.isEmpty()) return null
      val lines =
        text.split("|").map { part ->
          val kind = PictureLineKind.entries.firstOrNull { it.code == part.first() } ?: return null
          val points =
            part.drop(1).split(" ").map { pair ->
              val (x, y) = pair.split(",").map { it.toIntOrNull() ?: return null }
              PicturePoint(x, y)
            }
          PictureLine(kind, points)
        }
      return JunctionPicture(lines)
    }
  }
}

/**
 * Builds a [JunctionPicture] in local meters around [center] with [heading], clockwise from north,
 * pointing up.
 */
internal class PictureFrame(private val center: GeoPoint, heading: Double) {
  private val angle = heading * PI / 180
  private val metersPerLongitude = METERS_PER_DEGREE * cos(center.latitude * PI / 180)

  /** [point] in meters to the right of and ahead of [center]. */
  fun local(point: GeoPoint): Pair<Double, Double> {
    val east = (point.longitude - center.longitude) * metersPerLongitude
    val north = (point.latitude - center.latitude) * METERS_PER_DEGREE
    // Turning the map so the heading points up.
    val x = east * cos(angle) - north * sin(angle)
    val y = east * sin(angle) + north * cos(angle)
    return x to y
  }

  /** [points] cut to the parts within [radius] of the center, as lines of whole meters. */
  fun lines(points: List<GeoPoint>, radius: Double): List<List<PicturePoint>> {
    val local = points.map(::local)
    val result = mutableListOf<List<PicturePoint>>()
    var current = mutableListOf<PicturePoint>()
    fun inside(point: Pair<Double, Double>) = hypot(point.first, point.second) <= radius
    fun add(point: Pair<Double, Double>) {
      val rounded = PicturePoint(point.first.roundToInt(), point.second.roundToInt())
      if (current.lastOrNull() != rounded) current += rounded
    }
    for (index in local.indices) {
      val point = local[index]
      val previous = local.getOrNull(index - 1)
      if (previous != null && inside(previous) != inside(point)) {
        add(edge(previous, point, radius))
        if (!inside(point)) {
          if (current.size >= 2) result += current
          current = mutableListOf()
        }
      }
      if (inside(point)) add(point)
    }
    if (current.size >= 2) result += current
    return result
  }

  /** Where the line from [from] to [to] crosses the circle of [radius]. */
  private fun edge(
    from: Pair<Double, Double>,
    to: Pair<Double, Double>,
    radius: Double,
  ): Pair<Double, Double> {
    var low = 0.0
    var high = 1.0
    val fromInside = hypot(from.first, from.second) <= radius
    repeat(20) {
      val middle = (low + high) / 2
      val x = from.first + (to.first - from.first) * middle
      val y = from.second + (to.second - from.second) * middle
      if ((hypot(x, y) <= radius) == fromInside) low = middle else high = middle
    }
    val share = (low + high) / 2
    return from.first + (to.first - from.first) * share to
      from.second + (to.second - from.second) * share
  }

  private companion object {
    const val METERS_PER_DEGREE = 111_195.0
  }
}
