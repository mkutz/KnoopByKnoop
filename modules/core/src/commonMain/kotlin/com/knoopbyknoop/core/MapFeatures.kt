package com.knoopbyknoop.core

import kotlin.math.roundToInt

/** What the planner shows next to the map, see `map-planning`. */
sealed interface RouteDescription {

  /** No waypoint yet: the next tap sets the start. */
  data object Empty : RouteDescription

  /** Only the start is set: the next tap sets the end. */
  data class StartOnly(val start: String) : RouteDescription

  /** e.g. knots `12 → 7 → 45` and distance `3.5 km`. */
  data class Complete(val knots: String, val distance: String) : RouteDescription

  /** The legs that cannot be ridden, e.g. `45 → 99`. */
  data class Unreachable(val legs: List<String>) : RouteDescription
}

/**
 * The map layers and texts of the planner. Layers are GeoJSON feature collections with coordinates
 * in `[longitude, latitude]` order, ready for the map SDKs' GeoJSON sources.
 */
object MapFeatures {

  /** Points with the properties `id` and `ref` (the knot's number). */
  fun knots(graph: NetworkGraph): String =
    featureCollection(graph.knots.values) { knot ->
      point(knot)
      properties {
        property("id", knot.id.toString())
        property("ref", quoted(knot.ref))
      }
    }

  /** One line per connection along its course. */
  fun connections(geometry: NetworkGeometry): String =
    featureCollection(geometry.courses) { course ->
      lineString(course)
      properties {}
    }

  /**
   * One line per leg of the route along its course, with the property `reachable`. Unreachable legs
   * are straight lines between their waypoints.
   */
  fun route(session: PlanningSession, geometry: NetworkGeometry): String =
    featureCollection(session.route?.legs.orEmpty()) { leg ->
      lineString(
        leg.path?.let { courseOf(it, geometry) } ?: listOf(leg.from, leg.to).map(::pointOf)
      )
      properties { property("reachable", leg.isReachable.toString()) }
    }

  /** Points with the properties `position` and `role` (`start`, `stop` or `end`). */
  fun waypoints(session: PlanningSession): String =
    featureCollection(session.waypoints.withIndex().toList()) { (index, knot) ->
      point(knot)
      properties {
        property("position", index.toString())
        property(
          "role",
          when {
            index == 0 -> "\"start\""
            index == session.waypoints.lastIndex -> "\"end\""
            else -> "\"stop\""
          },
        )
      }
    }

  fun describe(session: PlanningSession): RouteDescription {
    val route =
      session.route
        ?: return session.waypoints.firstOrNull()?.let { RouteDescription.StartOnly(it.ref) }
          ?: RouteDescription.Empty
    return when (val summary = route.summary) {
      is RouteSummary.Complete ->
        RouteDescription.Complete(
          knots = summary.knots.joinToString(" → ") { it.ref },
          distance = formatDistance(summary.lengthMeters),
        )
      is RouteSummary.Incomplete ->
        RouteDescription.Unreachable(
          summary.unreachableLegs.map { "${it.from.ref} → ${it.to.ref}" }
        )
    }
  }

  /** e.g. `800 m` below one kilometer, `3.5 km` above. */
  fun formatDistance(meters: Int): String {
    if (meters < 1000) return "$meters m"
    val tenthsOfKilometers = (meters / 100.0).roundToInt()
    return "${tenthsOfKilometers / 10}.${tenthsOfKilometers % 10} km"
  }

  private fun courseOf(path: Path, geometry: NetworkGeometry): List<GeoPoint> {
    if (path.knots.size == 1) return listOf(pointOf(path.start))
    val course = mutableListOf<GeoPoint>()
    for ((from, to) in path.knots.zipWithNext()) {
      val leg = geometry.courseOf(from, to) ?: listOf(pointOf(from), pointOf(to))
      course.addAll(if (course.isEmpty()) leg else leg.drop(1))
    }
    return course
  }

  private fun pointOf(knot: Knot) = GeoPoint(knot.latitude, knot.longitude)

  // GeoJSON is written as text directly: networks have hundreds of thousands of points, and an
  // object tree for them takes seconds to build on a phone.

  private fun <T> featureCollection(
    items: Collection<T>,
    feature: FeatureWriter.(T) -> Unit,
  ): String {
    val writer = FeatureWriter(StringBuilder(items.size * 64))
    writer.text.append("""{"type":"FeatureCollection","features":[""")
    items.forEachIndexed { index, item ->
      if (index > 0) writer.text.append(',')
      writer.text.append("""{"type":"Feature",""")
      writer.feature(item)
      writer.text.append('}')
    }
    writer.text.append("]}")
    return writer.text.toString()
  }

  private class FeatureWriter(val text: StringBuilder) {

    private var hasProperty = false

    fun point(knot: Knot) {
      text.append(""""geometry":{"type":"Point","coordinates":""")
      coordinates(knot.longitude, knot.latitude)
      text.append("},")
    }

    fun lineString(course: List<GeoPoint>) {
      text.append(""""geometry":{"type":"LineString","coordinates":[""")
      course.forEachIndexed { index, point ->
        if (index > 0) text.append(',')
        coordinates(point.longitude, point.latitude)
      }
      text.append("]},")
    }

    fun properties(block: FeatureWriter.() -> Unit) {
      text.append(""""properties":{""")
      hasProperty = false
      block()
      text.append('}')
    }

    /** Writes a property whose [value] is already JSON, e.g. a number or a quoted string. */
    fun property(name: String, value: String) {
      if (hasProperty) text.append(',')
      text.append('"').append(name).append("\":").append(value)
      hasProperty = true
    }

    private fun coordinates(longitude: Double, latitude: Double) {
      text.append('[')
      appendDegrees(longitude)
      text.append(',')
      appendDegrees(latitude)
      text.append(']')
    }

    /** Degrees with six decimals (about 0.1 m), the precision of the data. */
    private fun appendDegrees(degrees: Double) {
      val microdegrees = toMicrodegrees(degrees)
      if (microdegrees < 0) text.append('-')
      val absolute = if (microdegrees < 0) -microdegrees else microdegrees
      text.append(absolute / 1_000_000).append('.')
      val fraction = (absolute % 1_000_000).toString()
      repeat(6 - fraction.length) { text.append('0') }
      text.append(fraction)
    }
  }

  private fun quoted(value: String): String = buildString {
    append('"')
    value.forEach { character ->
      when {
        character == '"' || character == '\\' -> append('\\').append(character)
        character < ' ' -> append("\\u").append(character.code.toString(16).padStart(4, '0'))
        else -> append(character)
      }
    }
    append('"')
  }
}
