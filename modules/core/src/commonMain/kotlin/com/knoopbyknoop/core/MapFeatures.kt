package com.knoopbyknoop.core

import kotlin.math.roundToInt

/** What the planner shows next to the map, see `map-planning`. */
sealed interface RouteDescription {

  /** No waypoint yet: the next tap sets the start. */
  data object Empty : RouteDescription

  /** Only the start is set: the next tap sets the end. */
  data class StartOnly(val start: String) : RouteDescription

  /**
   * The route's [knots] in riding order with their roles, e.g. the distance `3.5 km`, and the
   * [ferries] the route takes in the order it reaches them, see `map-planning` "Ferries on the
   * route".
   */
  data class Complete(
    val knots: List<RouteKnot>,
    val distance: String,
    val ferries: List<RouteFerry> = listOf(),
  ) : RouteDescription

  /** The legs that cannot be ridden, e.g. `45 → 99`. */
  data class Unreachable(val legs: List<String>) : RouteDescription
}

/** How a knot takes part in a route, see `map-planning`. */
enum class KnotRole {
  START,
  STOP,
  END,

  /** On the route between two waypoints. */
  PASSED,

  /** Start and end of a round trip, which share one marker on the map. */
  ROUND_TRIP,
}

data class RouteKnot(val knot: Knot, val role: KnotRole)

/** Whether a ferry is open now, and the [text] that tells it, e.g. `Open until 19:00`. */
data class FerryStatus(val open: Boolean, val text: String)

/**
 * A ferry the route takes, between the knots at [fromPosition] and [fromPosition] + 1 of
 * [RouteDescription.Complete.knots], with the texts the planner shows for it.
 *
 * @property place where the ferry is on the route: the numbers of the knots on both sides in riding
 *   order, e.g. `76 → 02`
 */
data class RouteFerry(val ferry: Ferry, val fromPosition: Int, val place: String = "") {

  /** The ferry's name, or just "Ferry". */
  val title: String = ferry.name ?: "Ferry"

  /** Whether OpenStreetMap has the ferry's operating times. */
  val timesKnown: Boolean = ferry.openingHours != null

  /**
   * The operating times as tagged, e.g. `Apr-Oct: Mo-Su 07:00-19:00`, or a note to check them,
   * which tells whether the ferry only runs in some seasons.
   */
  val times: String =
    ferry.openingHours
      ?: when (val seasonal = ferry.seasonal) {
        null,
        "no" -> UNKNOWN_TIMES
        "yes" -> "Seasonal. $UNKNOWN_TIMES"
        else -> "Seasonal ($seasonal). $UNKNOWN_TIMES"
      }

  /** e.g. `Fee: 1.50 EUR`, `Free` or `Fee charged`, or `null` if OpenStreetMap does not know. */
  val fee: String? =
    ferry.charge?.let { "Fee: $it" }
      ?: when (ferry.fee) {
        "no" -> "Free"
        "yes" -> "Fee charged"
        else -> null
      }

  private val openingHours = ferry.openingHours?.let(OpeningHours::parse)

  /**
   * Whether the ferry is open at [now], in the ferry's local time, and when that changes, e.g.
   * `Open until 19:00` or `Closed until tomorrow 07:00`; `null` when the operating times do not
   * tell, see `map-planning` "Ferry open now".
   */
  fun statusAt(now: LocalMoment): FerryStatus? {
    val status = openingHours?.statusAt(now) ?: return null
    return FerryStatus(status.open, statusText(status, now))
  }

  companion object {

    /**
     * [status] at [now] in words, e.g. `Open until 19:00`, `Closed until tomorrow 07:00` or `Closed
     * until 1 April 07:00`.
     */
    internal fun statusText(status: OpeningStatus, now: LocalMoment): String {
      val next = status.next
      return if (status.open) {
        val closing = next?.let(::endOfDayBefore)
        when {
          next == null || closing == null -> "Open"
          minutesBetween(now, next) <= MINUTES_PER_DAY -> "Open until ${clock(closing)}"
          else -> "Open until ${date(closing)} ${clock(closing)}"
        }
      } else {
        when {
          next == null -> "Closed"
          next.day == now.day -> "Closed until ${clock(next)}"
          next.day == now.day.plusDays(1) -> "Closed until tomorrow ${clock(next)}"
          else -> "Closed until ${date(next)} ${clock(next)}"
        }
      }
    }

    const val UNKNOWN_TIMES = "Operating times not known, check before you go"

    private const val MINUTES_PER_DAY = 24 * 60

    private val monthNames =
      listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December",
      )

    internal fun minutesBetween(from: LocalMoment, to: LocalMoment) =
      (to.day.epochDay - from.day.epochDay) * MINUTES_PER_DAY + to.minute - from.minute

    /** A closing at midnight reads as the end of the day before, e.g. `24:00`. */
    internal fun endOfDayBefore(moment: LocalMoment) =
      if (moment.minute == 0) LocalMoment(moment.day.plusDays(-1), MINUTES_PER_DAY) else moment

    internal fun clock(moment: LocalMoment): String {
      val minute = moment.minute
      return "${(minute / 60).toString().padStart(2, '0')}:${(minute % 60).toString().padStart(2, '0')}"
    }

    internal fun date(moment: LocalMoment): String {
      val day = moment.day
      return "${day.day} ${monthNames[day.month - 1]}"
    }
  }
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

  /**
   * The knots of the complete route in riding order with their roles. Waypoints keep their role,
   * also where the route passes them again. Without a complete route, only the waypoints.
   */
  fun routeKnots(session: PlanningSession): List<RouteKnot> {
    val legs = session.route?.legs ?: return waypointKnots(session)
    if (legs.any { !it.isReachable }) return waypointKnots(session)
    val lastLeg = legs.lastIndex
    return legs.flatMapIndexed { index, leg ->
      val knots = leg.path!!.knots
      val passed = knots.drop(1).dropLast(1).map { RouteKnot(it, KnotRole.PASSED) }
      val start = RouteKnot(knots.first(), if (index == 0) KnotRole.START else KnotRole.STOP)
      val end = if (index == lastLeg) listOf(RouteKnot(knots.last(), KnotRole.END)) else listOf()
      listOf(start) + passed + end
    }
  }

  /** The waypoints with their roles, for the waypoint list. */
  fun waypointKnots(session: PlanningSession): List<RouteKnot> =
    session.waypoints.mapIndexed { index, knot ->
      val role =
        when {
          index == 0 -> KnotRole.START
          index == session.waypoints.lastIndex -> KnotRole.END
          else -> KnotRole.STOP
        }
      RouteKnot(knot, role)
    }

  /**
   * The route's knots as markers on the map: like [routeKnots], but the start and end of a round
   * trip share one marker with the role [KnotRole.ROUND_TRIP], since they are at the same place.
   */
  fun routeKnotMarkers(session: PlanningSession): List<RouteKnot> {
    val knots = routeKnots(session)
    val start = knots.firstOrNull()
    val end = knots.lastOrNull()
    if (knots.size < 2 || start?.role != KnotRole.START || end?.role != KnotRole.END) return knots
    if (start.knot != end.knot) return knots
    return listOf(RouteKnot(start.knot, KnotRole.ROUND_TRIP)) + knots.subList(1, knots.lastIndex)
  }

  /** Points of the route's markers with the properties `ref` and `role` (lowercase). */
  fun routeKnotsLayer(session: PlanningSession): String =
    featureCollection(routeKnotMarkers(session)) { routeKnot ->
      point(routeKnot.knot)
      properties {
        property("ref", quoted(routeKnot.knot.ref))
        property("role", quoted(routeKnot.role.name.lowercase()))
      }
    }

  fun describe(session: PlanningSession): RouteDescription {
    val route =
      session.route
        ?: return session.waypoints.firstOrNull()?.let { RouteDescription.StartOnly(it.ref) }
          ?: RouteDescription.Empty
    return when (val summary = route.summary) {
      is RouteSummary.Complete -> {
        val knots = routeKnots(session)
        RouteDescription.Complete(
          knots = knots,
          distance = formatDistance(summary.lengthMeters),
          ferries =
            knots.zipWithNext().mapIndexedNotNull { position, (from, to) ->
              session.graph.ferryBetween(from.knot, to.knot)?.let {
                RouteFerry(it, position, "${from.knot.ref} → ${to.knot.ref}")
              }
            },
        )
      }
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
