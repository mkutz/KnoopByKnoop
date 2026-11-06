package com.knoopbyknoop.core

import kotlin.math.roundToInt

/** What the planner shows next to the map, see `map-planning`. */
sealed interface RouteDescription {

  /** No waypoint yet: the next tap sets the start. */
  data object Empty : RouteDescription

  /**
   * Only the start is set: the next tap sets the end.
   *
   * @property start the start's number, empty for a route point
   * @property routePoint whether the waypoint is a route point, which has no number
   * @property endFirst whether it is an end chosen before any start, so the next tap sets the
   *   start, see `map-planning` "End before the start"
   */
  data class StartOnly(
    val start: String,
    val routePoint: Boolean = false,
    val endFirst: Boolean = false,
  ) : RouteDescription

  /**
   * The route's [knots] in riding order with their roles, e.g. the distance `3.5 km`, the time it
   * takes at the assumed pace, e.g. `about 13 min`, and the [ferries] the route takes in the order
   * it reaches them, see `map-planning` "Show the route" and "Ferries on the route".
   */
  data class Complete(
    val knots: List<RouteKnot>,
    val distance: String,
    val ferries: List<RouteFerry> = listOf(),
    val duration: String = "",
  ) : RouteDescription {

    /** The positions of the stops in [knots], for `Navigation.start`. */
    val stopPositions: Set<Int>
      get() = knots.indices.filter { knots[it].role == KnotRole.STOP }.toSet()
  }

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
data class RouteFerry(
  val ferry: Ferry,
  val fromPosition: Int,
  val place: String = "",
  val language: Language = Language.EN,
) {

  private val texts = language.texts

  /** The ferry's name, or just "Ferry". */
  val title: String = ferry.name ?: texts.ferry

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
        "no" -> texts.unknownTimes
        "yes" -> "${texts.seasonal} ${texts.unknownTimes}"
        else -> "${texts.seasonalIn(seasonal)} ${texts.unknownTimes}"
      }

  /** e.g. `Fee: 1.50 EUR`, `Free` or `Fee charged`, or `null` if OpenStreetMap does not know. */
  val fee: String? =
    ferry.charge?.let { texts.fee(it) }
      ?: when (ferry.fee) {
        "no" -> texts.free
        "yes" -> texts.feeCharged
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
    return FerryStatus(status.open, statusText(status, now, language))
  }

  companion object {

    /**
     * [status] at [now] in words, e.g. `Open until 19:00`, `Closed until tomorrow 07:00` or `Closed
     * until 1 April 07:00`.
     */
    internal fun statusText(
      status: OpeningStatus,
      now: LocalMoment,
      language: Language = Language.EN,
    ): String {
      val texts = language.texts
      val next = status.next
      return if (status.open) {
        val closing = next?.let(::endOfDayBefore)
        when {
          next == null || closing == null -> texts.open
          minutesBetween(now, next) <= MINUTES_PER_DAY -> texts.openUntil(clock(closing))
          else -> texts.openUntil("${date(closing, language)} ${clock(closing)}")
        }
      } else {
        when {
          next == null -> texts.closed
          next.day == now.day -> texts.closedUntil(clock(next))
          next.day == now.day.plusDays(1) -> texts.closedUntilTomorrow(clock(next))
          else -> texts.closedUntil("${date(next, language)} ${clock(next)}")
        }
      }
    }

    val UNKNOWN_TIMES = EnglishTexts.unknownTimes

    private const val MINUTES_PER_DAY = 24 * 60

    internal fun minutesBetween(from: LocalMoment, to: LocalMoment) =
      (to.day.epochDay - from.day.epochDay) * MINUTES_PER_DAY + to.minute - from.minute

    /** A closing at midnight reads as the end of the day before, e.g. `24:00`. */
    internal fun endOfDayBefore(moment: LocalMoment) =
      if (moment.minute == 0) LocalMoment(moment.day.plusDays(-1), MINUTES_PER_DAY) else moment

    internal fun clock(moment: LocalMoment): String {
      val minute = moment.minute
      return "${(minute / 60).toString().padStart(2, '0')}:${(minute % 60).toString().padStart(2, '0')}"
    }

    internal fun date(moment: LocalMoment, language: Language = Language.EN): String =
      language.texts.date(moment.day.day, moment.day.month)
  }
}

/**
 * The map layers and texts of the planner. Layers are GeoJSON feature collections with coordinates
 * in `[longitude, latitude]` order, ready for the map SDKs' GeoJSON sources.
 */
object MapFeatures {

  /**
   * Points with the properties `id` and `ref` (the knot's number), one at every place a knot is
   * signposted, see `map-planning` "Knots signposted at several places".
   */
  fun knots(graph: NetworkGraph): String =
    featureCollection(graph.knots.values.flatMap { knot -> knot.positions.map { knot to it } }) {
      (knot, position) ->
      point(position)
      properties {
        property("id", knot.id.toString())
        property("ref", quoted(knot.ref))
      }
    }

  /**
   * One line per connection along its course, split where it runs on a bridge, with the property
   * `bridge`, see `map-planning` "Route and bridges".
   */
  fun connections(geometry: NetworkGeometry): String =
    featureCollection(geometry.courses.flatMap { pieces(it, geometry) }) { (line, bridge) ->
      lineString(line)
      properties { property("bridge", bridge.toString()) }
    }

  /**
   * [line] in pieces that each run entirely on a bridge or entirely off one, according to
   * [geometry]; the whole line off a bridge without bridge stretches.
   */
  internal fun pieces(
    line: List<GeoPoint>,
    geometry: NetworkGeometry?,
  ): List<Pair<List<GeoPoint>, Boolean>> {
    if (geometry == null || !geometry.hasBridges || line.size < 2) return listOf(line to false)
    val pieces = mutableListOf<Pair<List<GeoPoint>, Boolean>>()
    var start = 0
    var onBridge = geometry.isOnBridge(line[0], line[1])
    for (index in 1 until line.lastIndex) {
      val next = geometry.isOnBridge(line[index], line[index + 1])
      if (next != onBridge) {
        pieces += line.subList(start, index + 1) to onBridge
        start = index
        onBridge = next
      }
    }
    pieces += line.subList(start, line.size) to onBridge
    return pieces
  }

  /**
   * One line per leg of the route along its course, with the property `reachable`. Unreachable legs
   * are straight lines between their waypoints.
   */
  fun route(session: PlanningSession, geometry: NetworkGeometry): String {
    val legs = session.route?.legs.orEmpty()
    // With the route points' courses, see `route-planning` "Route from waypoints".
    val courses = legCourses(legs.map { it.path }, geometry.withRoutePoints(session.routeGraph))
    val pieces =
      legs.withIndex().flatMap { (index, leg) ->
        val line = courses[index]
        if (line == null) listOf(Triple(listOf(leg.from, leg.to).map(::pointOf), false, leg))
        else pieces(line, geometry).map { (piece, bridge) -> Triple(piece, bridge, leg) }
      }
    return featureCollection(pieces) { (line, bridge, leg) ->
      lineString(line)
      properties {
        property("reachable", leg.isReachable.toString())
        property("bridge", bridge.toString())
      }
    }
  }

  /**
   * The line of each path along [geometry], chosen over the paths that follow each other, so a stop
   * at a knot is drawn like a knot passed; `null` for a missing path.
   */
  private fun legCourses(paths: List<Path?>, geometry: NetworkGeometry): List<List<GeoPoint>?> {
    val lines = arrayOfNulls<List<GeoPoint>>(paths.size)
    var index = 0
    while (index < paths.size) {
      if (paths[index] == null) {
        index++
        continue
      }
      val first = index
      while (index < paths.size && paths[index] != null) index++
      val run = paths.subList(first, index).filterNotNull()
      val knots = run.flatMapIndexed { position, path ->
        if (position == 0) path.knots else path.knots.drop(1)
      }
      val courses = geometry.courses(knots).iterator()
      run.forEachIndexed { position, path ->
        val line =
          (1 until path.knots.size).fold(listOf<GeoPoint>()) { line, _ ->
            val leg = courses.next()
            if (line.isEmpty()) leg else line + leg.drop(1)
          }
        lines[first + position] = line.ifEmpty { listOf(pointOf(path.start)) }
      }
    }
    return lines.toList()
  }

  /**
   * The ways of the route points away from the network, from each point to the course, with the
   * property `straight`: a straight line is drawn differently from a way along paths, see
   * `map-planning` "The line on the map".
   */
  fun routePointWays(session: PlanningSession): String =
    featureCollection(
      session.routePoints.mapNotNull { it.routePoint?.takeIf { point -> point.way.size >= 2 } }
    ) { routePoint ->
      lineString(routePoint.way)
      properties { property("straight", routePoint.straight.toString()) }
    }

  /**
   * The line of [course], the leg ahead while navigating, or none, split where it runs on a bridge
   * of [geometry], with the property `bridge`.
   */
  fun legAhead(course: List<GeoPoint>?, geometry: NetworkGeometry? = null): String =
    featureCollection(course?.let { pieces(it, geometry) }.orEmpty()) { (line, bridge) ->
      lineString(line)
      properties { property("bridge", bridge.toString()) }
    }

  /**
   * Points with the properties `position`, `role` (`start`, `stop` or `end`) and `point`, whether
   * it is a route point.
   */
  fun waypoints(session: PlanningSession): String =
    featureCollection(session.waypoints.withIndex().toList()) { (index, knot) ->
      point(knot)
      properties {
        property("position", index.toString())
        property("role", quoted(session.roleOf(index).name.lowercase()))
        property("point", knot.isRoutePoint.toString())
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
        when (session.roleOf(index)) {
          WaypointRole.START -> KnotRole.START
          WaypointRole.STOP -> KnotRole.STOP
          WaypointRole.END -> KnotRole.END
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

  /**
   * Points of the route's markers with the properties `ref` and `role` (lowercase), one at every
   * place a knot is signposted, see `map-planning` "Knots signposted at several places".
   */
  fun routeKnotsLayer(session: PlanningSession): String =
    featureCollection(
      routeKnotMarkers(session).flatMap { routeKnot ->
        routeKnot.knot.positions.map { routeKnot to it }
      }
    ) { (routeKnot, position) ->
      point(position)
      properties {
        property("ref", quoted(routeKnot.knot.ref))
        property("role", quoted(routeKnot.role.name.lowercase()))
        property("point", routeKnot.knot.isRoutePoint.toString())
      }
    }

  fun describe(session: PlanningSession, language: Language = Language.EN): RouteDescription {
    val route =
      session.route
        ?: return session.waypoints.firstOrNull()?.let {
          RouteDescription.StartOnly(it.ref, it.isRoutePoint, session.endFirst)
        } ?: RouteDescription.Empty
    return when (val summary = route.summary) {
      is RouteSummary.Complete -> {
        val knots = routeKnots(session)
        RouteDescription.Complete(
          knots = knots,
          distance = formatDistance(summary.lengthMeters, language),
          duration =
            language.texts.about(
              RouteProgress.formatDuration(
                summary.lengthMeters / RouteProgress.assumedMetersPerSecond(session.graph.network)
              )
            ),
          ferries =
            knots.zipWithNext().mapIndexedNotNull { position, (from, to) ->
              session.graph.ferryBetween(from.knot, to.knot)?.let {
                RouteFerry(it, position, "${from.knot.ref} → ${to.knot.ref}", language)
              }
            },
        )
      }
      is RouteSummary.Incomplete ->
        RouteDescription.Unreachable(
          summary.unreachableLegs.map { leg ->
            val index = route.legs.indexOf(leg)
            val texts = language.texts
            "${texts.name(Spot.at(route.waypoints, index))} → " +
              texts.name(Spot.at(route.waypoints, index + 1))
          }
        )
    }
  }

  /**
   * e.g. `800 m` below one kilometer, `3.5 km` below ten, `200 km` above, see `knot-navigation`
   * "Rounded times and distances".
   */
  fun formatDistance(meters: Int, language: Language = Language.EN): String {
    if (meters < 1000) return "$meters m"
    return "${kilometers(meters, language.texts.decimalSeparator)} km"
  }

  /** The number of kilometers as written, e.g. `1.7`, or `12` from 10 km on. */
  internal fun kilometers(meters: Int, separator: Char): String {
    val tenthsOfKilometers = (meters / 100.0).roundToInt()
    if (tenthsOfKilometers >= 100) return "${(meters / 1000.0).roundToInt()}"
    return "${tenthsOfKilometers / 10}$separator${tenthsOfKilometers % 10}"
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

    fun point(knot: Knot) = point(GeoPoint(knot.latitude, knot.longitude))

    fun point(position: GeoPoint) {
      text.append(""""geometry":{"type":"Point","coordinates":""")
      coordinates(position.longitude, position.latitude)
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
