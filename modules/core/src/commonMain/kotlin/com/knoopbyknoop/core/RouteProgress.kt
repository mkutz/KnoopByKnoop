package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
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
 * @property distance for a stop, ferry or the end still ahead, the distance to it along the route,
 *   e.g. `5.2 km`, see `knot-navigation` "Distances and arrivals on the strip"
 * @property arrival when the rider gets there, e.g. `18:40`, where the local time is known
 * @property position for a ferry, the route position of the knot before it, as
 *   [RouteFerry.fromPosition]
 * @property state for a ferry ahead, whether it runs when the rider gets there, see
 *   `knot-navigation` "Ferries on the strip"
 */
data class ProgressMark(
  val kind: ProgressMarkKind,
  val label: String,
  val fraction: Double,
  val distance: String? = null,
  val arrival: String? = null,
  val position: Int? = null,
  val state: FerryArrivalState? = null,
)

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
 * A knot or ferry still ahead on the route, see `knot-navigation` "Coming knots".
 *
 * @property knot the knot, to show it on the map; `null` for a ferry
 * @property label the knot's number, or `⛴` for a ferry
 * @property distance the distance to it along the route, e.g. `5.2 km`, as on the strip
 * @property arrival when the rider gets there, e.g. `18:40`, where the local time is known
 * @property position for a ferry, the route position of the knot before it, as
 *   [RouteFerry.fromPosition]
 * @property state for a ferry, whether it runs when the rider gets there
 * @property description what screen readers say, e.g. `Knot 30, stop, 5.2 km, 18:40`
 */
data class ComingKnot(
  val kind: ProgressMarkKind,
  val knot: Knot?,
  val label: String,
  val distance: String,
  val arrival: String?,
  val position: Int? = null,
  val state: FerryArrivalState? = null,
  val description: String,
)

/**
 * What the panel shows about the rider's progress while navigating.
 *
 * @property nextKnotDistance the distance to the next knot, along the route or straight while off
 *   route, e.g. `800 m`, see `knot-navigation` "Number to follow"
 * @property coming the knots and ferries ahead in riding order, from the next knot to the end
 */
data class ProgressView(
  val strip: ProgressStrip,
  val nextKnotDistance: String,
  val coming: List<ComingKnot> = listOf(),
)

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

  /**
   * The point of the leg's course before the knot at [nextIndex] nearest to [fix], not before
   * [place], see `knot-navigation` "Way to the route on the map"; `null` before the first knot or
   * without the leg's course.
   */
  fun nearestAhead(fix: Fix, nextIndex: Int, place: Double): GeoPoint? {
    if (nextIndex == 0) return null
    val leg = nextIndex - 1
    val course = courses?.getOrNull(leg)?.takeIf { it.size >= 2 } ?: return null
    val fromFraction =
      if (legLength(leg) > 0) ((place - offsets[leg]) / legLength(leg)).coerceIn(0.0, 1.0) else 0.0
    return nearestAfter(fix.latitude, fix.longitude, course, fromFraction)
  }

  /** The straight distance from [fix] to the knot at [index]. */
  fun straightTo(fix: Fix, index: Int): Double {
    val knot = knots[index]
    return distanceMeters(fix.latitude, fix.longitude, knot.latitude, knot.longitude)
  }

  /**
   * The panel's progress with the rider at [place] along the route, before the knot at [nextIndex];
   * off route or before the first knot, the distances count from the next knot plus
   * [straightDistance].
   *
   * @param straightDistance the straight distance to the next knot, off route or before the first
   *   knot, else `null`
   * @param offRoute whether the rider is off route, see `knot-navigation` "Off route on the strip
   *   ahead"
   * @param now the instant of the position, in epoch milliseconds
   * @param localTime the networks' local time, for the arrivals; without it, there are none
   * @param ferryState whether the ferry after the knot at a route position runs at the arrival
   */
  fun view(
    place: Double,
    nextIndex: Int,
    straightDistance: Double?,
    offRoute: Boolean,
    metersPerSecond: Double,
    now: Long,
    localTime: ((Long) -> LocalMoment)?,
    ferryState: (Int) -> FerryArrivalState? = { null },
    language: Language = Language.EN,
  ): ProgressView {
    val texts = language.texts
    fun distanceTo(offset: Double) =
      max(
        0.0,
        if (straightDistance != null) offset - offsets[nextIndex] + straightDistance
        else offset - place,
      )
    val ahead = setOf(ProgressMarkKind.STOP, ProgressMarkKind.FERRY, ProgressMarkKind.END)
    val marks =
      marks().map { mark ->
        val offset = mark.fraction * length
        // Half a meter of slack, as the offset comes back from the fraction.
        if (mark.kind !in ahead || offset <= aheadFrom(place, nextIndex, straightDistance) + 0.5)
          mark
        else {
          val meters = distanceTo(offset)
          mark.copy(
            distance = MapFeatures.formatDistance(meters.roundToInt(), language),
            arrival = localTime?.let { arrivalText(now, meters / metersPerSecond, it) },
            state = mark.position?.let(ferryState),
          )
        }
      }
    fun arrival(meters: Double) = localTime?.let { arrivalText(now, meters / metersPerSecond, it) }
    // The knots from the next one, and the ferries ahead between them, see `knot-navigation`
    // "Coming knots".
    val from = aheadFrom(place, nextIndex, straightDistance)
    val comingKnots =
      (nextIndex..knots.lastIndex).map { index ->
        val kind =
          when (index) {
            0 -> ProgressMarkKind.START
            knots.lastIndex -> ProgressMarkKind.END
            in stops -> ProgressMarkKind.STOP
            else -> ProgressMarkKind.KNOT
          }
        offsets[index] to
          coming(kind, knots[index], distanceTo(offsets[index]), ::arrival, language)
      }
    val comingFerries = ferryPositions.mapNotNull { position ->
      val offset = (offsets[position] + offsets[position + 1]) / 2
      if (offset <= from + 0.5) null
      else
        offset to
          comingFerry(position, distanceTo(offset), ::arrival, ferryState(position), language)
    }
    val coming = (comingKnots + comingFerries).sortedBy { it.first }.map { it.second }
    val toNext = straightDistance ?: max(0.0, offsets[nextIndex] - place)
    val rider = if (length > 0) (place / length).coerceIn(0.0, 1.0) else 0.0
    return ProgressView(
      ProgressStrip(
        marks,
        rider,
        offRoute,
        texts.ridden(ridden(place, texts), knots[nextIndex].ref),
      ),
      MapFeatures.formatDistance(toNext.roundToInt(), language),
      coming,
    )
  }

  private fun coming(
    kind: ProgressMarkKind,
    knot: Knot,
    meters: Double,
    arrival: (Double) -> String?,
    language: Language,
  ): ComingKnot {
    val texts = language.texts
    val distance = MapFeatures.formatDistance(meters.roundToInt(), language)
    val at = arrival(meters)
    val role =
      when (kind) {
        ProgressMarkKind.START -> texts.start
        ProgressMarkKind.STOP -> texts.stop
        ProgressMarkKind.END -> texts.end
        else -> null
      }
    return ComingKnot(
      kind,
      knot,
      knot.ref,
      distance,
      at,
      description = listOfNotNull(texts.knot(knot.ref), role, distance, at).joinToString(", "),
    )
  }

  private fun comingFerry(
    position: Int,
    meters: Double,
    arrival: (Double) -> String?,
    state: FerryArrivalState?,
    language: Language,
  ): ComingKnot {
    val distance = MapFeatures.formatDistance(meters.roundToInt(), language)
    val at = arrival(meters)
    val legs = "${knots[position].ref} → ${knots[position + 1].ref}"
    return ComingKnot(
      ProgressMarkKind.FERRY,
      null,
      "⛴",
      distance,
      at,
      position,
      state,
      listOfNotNull(language.texts.ferryBetween(legs), distance, at).joinToString(", "),
    )
  }

  /**
   * Where the marks ahead begin: the rider's place, or with a [straightDistance] the knot before
   * the next one, so the next knot and what follows still count as ahead.
   */
  private fun aheadFrom(place: Double, nextIndex: Int, straightDistance: Double?): Double =
    if (straightDistance != null) offsets[maxOf(0, nextIndex - 1)] else place

  /**
   * The line outside the app, e.g. `0.8 km to 45 · end around 19:05`, with the rider at [place]
   * before the knot at [nextIndex], see `knot-navigation` "Navigation with the screen off".
   *
   * @param straightDistance the straight distance to the next knot before the first knot, else
   *   `null`
   */
  fun line(
    place: Double,
    nextIndex: Int,
    metersPerSecond: Double,
    now: Long,
    localTime: ((Long) -> LocalMoment)?,
    language: Language = Language.EN,
    straightDistance: Double? = null,
  ): String {
    val texts = language.texts
    val toNextMeters = straightDistance ?: max(0.0, offsets[nextIndex] - place)
    val toNext = MapFeatures.formatDistance(toNextMeters.roundToInt(), language)
    val toEnd = length - offsets[nextIndex] + toNextMeters
    val arrival = localTime?.let { arrivalText(now, max(0.0, toEnd) / metersPerSecond, it) }
    return if (nextIndex == knots.lastIndex) texts.toTheEnd(toNext, arrival)
    else texts.toKnot(toNext, knots[nextIndex].ref, arrival)
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
        position = position,
      )
    }
    return (knotMarks + ferryMarks).sortedBy { it.fraction }
  }

  /** e.g. `4.0 of 12.4 km` or `300 of 800 m`. */
  private fun ridden(place: Double, texts: Texts): String {
    if (length < 1000) return texts.of("${place.roundToInt()}", "${length.roundToInt()}", "m")
    return texts.of(tenths(place, texts), tenths(length, texts), "km")
  }

  private fun tenths(meters: Double, texts: Texts): String {
    val tenths = (meters / 100).roundToInt()
    return "${tenths / 10}${texts.decimalSeparator}${tenths % 10}"
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

    /**
     * The point of [points] nearest to the position, not before [fromFraction] of their length, in
     * a local flat projection as [fractionAlong].
     */
    private fun nearestAfter(
      latitude: Double,
      longitude: Double,
      points: List<GeoPoint>,
      fromFraction: Double,
    ): GeoPoint {
      val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(latitude * PI / 180)
      fun x(point: GeoPoint) = (point.longitude - longitude) * metersPerDegreeLongitude
      fun y(point: GeoPoint) = (point.latitude - latitude) * METERS_PER_DEGREE_LATITUDE
      val segments = points.zipWithNext()
      val lengths = segments.map { (from, to) -> hypot(x(to) - x(from), y(to) - y(from)) }
      val fromAlong = fromFraction * lengths.sum()
      var start = 0.0
      var best = Double.MAX_VALUE
      var bestPoint = points.last()
      for ((index, segment) in segments.withIndex()) {
        val (from, to) = segment
        val length = lengths[index]
        val end = start + length
        if (end >= fromAlong) {
          val deltaX = x(to) - x(from)
          val deltaY = y(to) - y(from)
          val lowest = if (length > 0) ((fromAlong - start) / length).coerceIn(0.0, 1.0) else 0.0
          val along =
            if (length == 0.0) lowest
            else (-(x(from) * deltaX + y(from) * deltaY) / (length * length)).coerceIn(lowest, 1.0)
          val nearestX = x(from) + along * deltaX
          val nearestY = y(from) + along * deltaY
          val distance = nearestX * nearestX + nearestY * nearestY
          if (distance < best) {
            best = distance
            bestPoint =
              GeoPoint(
                from.latitude + along * (to.latitude - from.latitude),
                from.longitude + along * (to.longitude - from.longitude),
              )
          }
        }
        start = end
      }
      return bestPoint
    }
  }
}
