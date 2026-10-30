package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** What a mark on the progress strip stands for, see `knot-navigation` "Progress strip". */
enum class ProgressMarkKind {
  START,
  STOP,
  END,
  FERRY,

  /** A knot the route only passes, drawn as a small mark. */
  KNOT,
}

/**
 * A mark on the progress strip.
 *
 * @property label the knot's number, or `⛴` for a ferry
 * @property fraction where along the route, from 0 at the start to 1 at the end
 */
data class ProgressMark(val kind: ProgressMarkKind, val label: String, val fraction: Double)

/**
 * The route drawn to scale with the rider on it, see `knot-navigation` "Progress strip".
 *
 * @property rider where the rider is along the route, from 0 to 1; ridden up to there
 * @property description what screen readers say, e.g. `4.0 of 12.4 km ridden, next knot 12`
 */
data class ProgressStrip(
  val marks: List<ProgressMark>,
  val rider: Double,
  val offRoute: Boolean,
  val description: String,
)

/**
 * A line of the distances and times ahead, see `knot-navigation` "Distances and times ahead".
 *
 * @property label `Next knot`, `Next stop` or `End`
 * @property distance e.g. `800 m` or `5.2 km`
 * @property time e.g. `20 min`, or `null` where it is not told
 * @property arrival e.g. `18:40`, or `null` where it is not told
 */
data class ProgressRow(
  val label: String,
  val knot: String,
  val distance: String,
  val time: String?,
  val arrival: String?,
)

/** What the panel shows about the rider's progress while navigating. */
data class ProgressView(val strip: ProgressStrip, val rows: List<ProgressRow>)

/**
 * A navigated route measured along its connections: where each knot is, which knots are stops, and
 * where on the route a position is, see `knot-navigation` "Progress strip" and "Distances and times
 * ahead". Immutable.
 *
 * @property offsets the route distance of each knot from the first, along the connections
 * @property stops the positions of the stops in [knots], without the start and the end
 */
internal class RouteProgress
private constructor(
  val knots: List<Knot>,
  val offsets: List<Double>,
  private val courses: List<List<GeoPoint>>?,
  val stops: Set<Int>,
  private val ferryPositions: List<Int>,
  val assumedMetersPerSecond: Double,
) {

  val length: Double
    get() = offsets.last()

  /** The length of the leg from the knot at [position] to the next one. */
  fun legLength(position: Int): Double = offsets[position + 1] - offsets[position]

  /**
   * Where [fix] is along the route, before the knot at [nextIndex]: on the leg's course where it is
   * installed, otherwise from the straight distance to the next knot.
   */
  fun placeOf(fix: Fix, nextIndex: Int): Double {
    if (nextIndex == 0) return 0.0
    val leg = nextIndex - 1
    val course = courses?.getOrNull(leg)
    if (course != null && course.size >= 2) {
      val fraction = fractionAlong(fix.latitude, fix.longitude, course)
      return offsets[leg] + fraction * legLength(leg)
    }
    return offsets[nextIndex] - min(straightTo(fix, nextIndex), legLength(leg))
  }

  /** The straight distance from [fix] to the knot at [index]. */
  fun straightTo(fix: Fix, index: Int): Double {
    val knot = knots[index]
    return distanceMeters(fix.latitude, fix.longitude, knot.latitude, knot.longitude)
  }

  /**
   * The panel's progress with the rider at [place] along the route, before the knot at [nextIndex];
   * off route, the distances count from the next knot plus [offRouteDistance].
   *
   * @param now the instant of the position, in epoch milliseconds
   * @param localTime the networks' local time, for the arrivals; without it, there are none
   */
  fun view(
    place: Double,
    nextIndex: Int,
    offRouteDistance: Double?,
    metersPerSecond: Double,
    now: Long,
    localTime: ((Long) -> LocalMoment)?,
  ): ProgressView {
    fun distanceTo(index: Int) =
      if (offRouteDistance != null) offsets[index] - offsets[nextIndex] + offRouteDistance
      else max(0.0, offsets[index] - place)
    fun row(label: String, index: Int, arrival: Boolean, timed: Boolean = true): ProgressRow {
      val meters = distanceTo(index)
      val seconds = meters / metersPerSecond
      return ProgressRow(
        label,
        knots[index].ref,
        MapFeatures.formatDistance(meters.roundToInt()),
        if (timed) formatDuration(seconds) else null,
        if (arrival && localTime != null) arrivalText(now, seconds, localTime) else null,
      )
    }
    val rows = buildList {
      add(row("Next knot", nextIndex, arrival = false, timed = offRouteDistance == null))
      stops
        .filter { it in nextIndex until knots.lastIndex }
        .minOrNull()
        ?.let {
          add(row("Next stop", it, arrival = true))
        }
      add(row("End", knots.lastIndex, arrival = true))
    }
    val rider = if (length > 0) (place / length).coerceIn(0.0, 1.0) else 0.0
    return ProgressView(
      ProgressStrip(
        marks(),
        rider,
        offRoute = offRouteDistance != null,
        "${ridden(place)} ridden, next knot ${knots[nextIndex].ref}",
      ),
      rows,
    )
  }

  /**
   * The line outside the app, e.g. `0.8 km to 45 · end around 19:05`, with the rider at [place]
   * before the knot at [nextIndex], see `knot-navigation` "Navigation with the screen off".
   */
  fun line(
    place: Double,
    nextIndex: Int,
    metersPerSecond: Double,
    now: Long,
    localTime: ((Long) -> LocalMoment)?,
  ): String {
    val toNext = MapFeatures.formatDistance(max(0.0, offsets[nextIndex] - place).roundToInt())
    val arrival = localTime?.let {
      arrivalText(now, max(0.0, length - place) / metersPerSecond, it)
    }
    return when {
      nextIndex == knots.lastIndex && arrival != null -> "$toNext to the end, around $arrival"
      nextIndex == knots.lastIndex -> "$toNext to the end"
      arrival != null -> "$toNext to ${knots[nextIndex].ref} · end around $arrival"
      else -> "$toNext to ${knots[nextIndex].ref}"
    }
  }

  private fun marks(): List<ProgressMark> {
    fun fraction(offset: Double) = if (length > 0) offset / length else 0.0
    val knotMarks =
      knots.indices.map { index ->
        val kind =
          when (index) {
            0 -> ProgressMarkKind.START
            knots.lastIndex -> ProgressMarkKind.END
            in stops -> ProgressMarkKind.STOP
            else -> ProgressMarkKind.KNOT
          }
        ProgressMark(kind, knots[index].ref, fraction(offsets[index]))
      }
    val ferryMarks = ferryPositions.map { position ->
      ProgressMark(
        ProgressMarkKind.FERRY,
        "⛴",
        fraction((offsets[position] + offsets[position + 1]) / 2),
      )
    }
    return (knotMarks + ferryMarks).sortedBy { it.fraction }
  }

  /** e.g. `4.0 of 12.4 km` or `300 of 800 m`. */
  private fun ridden(place: Double): String {
    if (length < 1000) return "${place.roundToInt()} of ${length.roundToInt()} m"
    return "${tenths(place)} of ${tenths(length)} km"
  }

  private fun tenths(meters: Double): String {
    val tenths = (meters / 100).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
  }

  companion object {

    /** The assumed pace until the rider covered 2 km: 16 km/h, 4.5 km/h. */
    private val assumedKilometersPerHour =
      mapOf(NetworkType.CYCLING to 16.0, NetworkType.HIKING to 4.5)

    private const val SECONDS_PER_MINUTE = 60.0
    private const val MINUTES_PER_HOUR = 60

    /** The assumed pace on [network], in meters per second. */
    fun assumedMetersPerSecond(network: NetworkType?): Double =
      assumedKilometersPerHour.getValue(network ?: NetworkType.CYCLING) / 3.6

    /**
     * Measures the route through [knots] on [graph].
     *
     * @param courses the course of each leg, or `null` without courses
     * @param stops the positions of the stops in [knots]
     */
    fun of(
      knots: List<Knot>,
      graph: NetworkGraph,
      courses: List<List<GeoPoint>>?,
      stops: Set<Int>,
    ): RouteProgress {
      val legLengths = knots.zipWithNext { from, to -> lengthOf(graph, from, to) }
      val ferries =
        knots.zipWithNext().mapIndexedNotNull { position, (from, to) ->
          position.takeIf { graph.ferryBetween(from, to) != null }
        }
      return RouteProgress(
        knots,
        legLengths.runningFold(0.0) { sum, length -> sum + length },
        courses,
        stops.filter { it in 1 until knots.lastIndex }.toSet(),
        ferries,
        assumedMetersPerSecond(graph.network),
      )
    }

    /**
     * A duration as the app shows it, see `knot-navigation` "Rounded times": `20 min` under an
     * hour, at least `1 min`, `2 h 35 min` rounded to five minutes above.
     */
    fun formatDuration(seconds: Double): String {
      val minutes = (seconds / SECONDS_PER_MINUTE).roundToInt()
      if (minutes < MINUTES_PER_HOUR) return "${max(1, minutes)} min"
      val fiveMinutes = (seconds / (5 * SECONDS_PER_MINUTE)).roundToInt() * 5
      return "${fiveMinutes / MINUTES_PER_HOUR} h ${fiveMinutes % MINUTES_PER_HOUR} min"
    }

    /** The arrival [seconds] after [now], rounded to five minutes, e.g. `19:05`. */
    fun arrivalText(now: Long, seconds: Double, localTime: (Long) -> LocalMoment): String {
      // To the nearest minute first, as the local time only counts whole minutes.
      val arrival = localTime(now + (seconds * 1000).roundToLong() + 30_000)
      val minute = (arrival.minute + 2) / 5 * 5
      return RouteFerry.clock(
        if (minute >= 24 * 60) LocalMoment(arrival.day.plusDays(1), 0)
        else LocalMoment(arrival.day, minute)
      )
    }

    private fun lengthOf(graph: NetworkGraph, from: Knot, to: Knot): Double =
      graph.neighborsOf(from).filter { it.first.id == to.id }.minOfOrNull { it.second }?.toDouble()
        ?: distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)

    private const val METERS_PER_DEGREE_LATITUDE = 6_371_008.8 * PI / 180

    /**
     * How far along [points] the point nearest to the position is, from 0 to 1, in a local flat
     * projection.
     */
    private fun fractionAlong(latitude: Double, longitude: Double, points: List<GeoPoint>): Double {
      val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(latitude * PI / 180)
      fun x(point: GeoPoint) = (point.longitude - longitude) * metersPerDegreeLongitude
      fun y(point: GeoPoint) = (point.latitude - latitude) * METERS_PER_DEGREE_LATITUDE
      var total = 0.0
      var best = Double.MAX_VALUE
      var bestAlong = 0.0
      for ((from, to) in points.zipWithNext()) {
        val deltaX = x(to) - x(from)
        val deltaY = y(to) - y(from)
        val segment = sqrt(deltaX * deltaX + deltaY * deltaY)
        val lengthSquared = segment * segment
        val along =
          if (lengthSquared == 0.0) 0.0
          else (-(x(from) * deltaX + y(from) * deltaY) / lengthSquared).coerceIn(0.0, 1.0)
        val nearestX = x(from) + along * deltaX
        val nearestY = y(from) + along * deltaY
        val distance = nearestX * nearestX + nearestY * nearestY
        if (distance < best) {
          best = distance
          bestAlong = total + along * segment
        }
        total += segment
      }
      return if (total > 0) bestAlong / total else 0.0
    }
  }
}
