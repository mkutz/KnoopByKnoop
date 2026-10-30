package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * A position of the rider, as the platforms deliver it.
 *
 * @property accuracyMeters the radius the true position lies within, as the platform estimates it
 */
data class Fix(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Double,
  val timeMillis: Long,
)

/** How precisely the apps sample the position next, see `knot-navigation` and the design. */
enum class Precision {
  /** Far from the next knot and on route: rarely, mostly without GPS. */
  COARSE,

  /** Close to the next knot, off route, or to confirm a coarse fix far from the course. */
  FINE,

  /** The app is visible: as often and as precisely as possible. */
  LIVE,
}

/** What a notice is about. */
enum class NoticeKind {
  NEXT,
  REMINDER,
  OFF_ROUTE,
  BACK_ON_ROUTE,
  ARRIVED,

  /** A ferry ahead is closed when the rider gets there, see `knot-navigation` "Ferry warning". */
  FERRY,
}

/**
 * Something to tell the rider, see `knot-navigation`.
 *
 * @property text the text to show, e.g. `Next 08, then 12`
 * @property speech the text to speak, with numbers spelled so both apps speak them alike, e.g.
 *   `Next 8, then 12`
 */
data class Notice(val kind: NoticeKind, val text: String, val speech: String)

/**
 * The number to follow, the most prominent information while navigating, see `knot-navigation`
 * "Number to follow".
 *
 * @property next the next knot's number
 * @property follow the number to follow at the next knot, or `null` if it is the end of the route
 */
data class NavigationDisplay(val next: String, val follow: String?, val offRoute: Boolean) {

  /** E.g. `At 45 follow 12`, or `31 is the end`. */
  val text: String
    get() = if (follow != null) "At $next follow $follow" else "$next is the end"

  /** E.g. `Off route, next 45`, shown instead of [text] while off route. */
  val offRouteText: String
    get() = "Off route, next $next"
}

/**
 * The result of a fix: the new navigation, what to tell the rider, and how precisely to sample
 * next.
 */
data class NavigationUpdate(
  val navigation: Navigation,
  val notices: List<Notice>,
  val precision: Precision,
)

/**
 * Following a complete route knot by knot, see `knot-navigation`. Immutable: every fix returns a
 * new navigation in a [NavigationUpdate].
 *
 * @property knots the route's knots in riding order
 * @property nextIndex the position of the next knot in [knots]
 */
class Navigation
private constructor(
  val knots: List<Knot>,
  private val courses: List<List<GeoPoint>>?,
  private val locator: KnotLocator,
  private val routeKnotIds: Set<Long>,
  val nextIndex: Int,
  val offRoute: Boolean,
  val arrived: Boolean,
  private val remindedIndex: Int?,
  private val offRouteKnot: Knot?,
  private val confirming: Boolean,
  private val lastPreciseFix: Fix?,
  val lastFix: Fix,
  private val ferryWatch: FerryWatch?,
  private val route: RouteProgress,
  private val localTime: ((Long) -> LocalMoment)?,
  private val pace: Pace,
  private val place: Double,
  /**
   * The line outside the app, e.g. `0.8 km to 45 · end around 19:05`, as of the start or the last
   * knot passed, see `knot-navigation` "Navigation with the screen off".
   */
  val progressLine: String,
) {

  /**
   * The rider's pace: the route covered since [startOffset] at [startMillis], renewed when a knot
   * is passed, see `knot-navigation` "Ferry arrival estimate".
   */
  internal data class Pace(
    val startOffset: Double,
    val startMillis: Long,
    val metersPerSecond: Double,
  ) {
    fun at(offset: Double, timeMillis: Long, assumed: Double): Pace {
      val covered = offset - startOffset
      val elapsedMillis = timeMillis - startMillis
      val metersPerSecond =
        if (covered >= OWN_PACE_AFTER_METERS && elapsedMillis > 0) covered * 1000 / elapsedMillis
        else assumed
      return copy(metersPerSecond = metersPerSecond)
    }
  }

  /**
   * The progress strip and the distances and times ahead with the rider at [fix], see
   * `knot-navigation` "Progress strip" and "Distances and times ahead".
   */
  fun progress(fix: Fix): ProgressView {
    val offRouteDistance = if (offRoute && !arrived) route.straightTo(fix, nextIndex) else null
    val place =
      if (offRoute || arrived || fix.accuracyMeters > PRECISE_ACCURACY_METERS) this.place
      else route.placeOf(fix, nextIndex)
    return route.view(
      place,
      nextIndex,
      offRouteDistance,
      pace.metersPerSecond,
      fix.timeMillis,
      localTime,
    )
  }

  val nextKnot: Knot
    get() = knots[nextIndex]

  val display: NavigationDisplay
    get() = NavigationDisplay(nextKnot.ref, knots.getOrNull(nextIndex + 1)?.ref, offRoute)

  /**
   * When the rider gets to the ferry on the leg from the knot at [fromPosition] of [knots], as
   * estimated at the last knot passed, or `null` once it is passed or without an estimate, see
   * `knot-navigation` "Ferry arrival estimate".
   */
  fun ferryArrival(fromPosition: Int): FerryArrival? = ferryWatch?.arrivalAt(fromPosition)

  /** How precisely to sample next; [visible] is whether the app is on screen. */
  fun precision(visible: Boolean): Precision =
    when {
      visible -> Precision.LIVE
      arrived -> Precision.COARSE
      offRoute || confirming -> Precision.FINE
      lastFix.accuracyMeters > FINE_DISTANCE_METERS -> Precision.FINE
      distanceTo(lastFix, nextKnot) <= FINE_DISTANCE_METERS -> Precision.FINE
      else -> Precision.COARSE
    }

  /** Follows the route with [fix]; [visible] is whether the app is on screen. */
  fun update(fix: Fix, visible: Boolean = false): NavigationUpdate {
    if (arrived) return NavigationUpdate(this, listOf(), precision(visible))
    if (fix.accuracyMeters > PRECISE_ACCURACY_METERS) return updateCoarse(fix, visible)
    val notices = mutableListOf<Notice>()
    var navigation = this

    val passedIndex = passedRouteIndex(fix)
    if (passedIndex != null) {
      if (passedIndex == knots.lastIndex) {
        notices += arrivedNotice()
        navigation = navigation.copy(nextIndex = passedIndex, arrived = true, offRoute = false)
      } else {
        val newNext = passedIndex + 1
        notices +=
          if (offRoute) backOnRouteNotice(newNext) else nextNotice(newNext, prefix = "Next")
        val place = route.offsets[passedIndex]
        val pace = pace.at(place, fix.timeMillis, route.assumedMetersPerSecond)
        val ferries = ferryWatch?.atKnot(passedIndex, fix.timeMillis, pace.metersPerSecond)
        ferries?.let { notices += it.second }
        navigation =
          navigation.copy(
            nextIndex = newNext,
            offRoute = false,
            offRouteKnot = null,
            remindedIndex = if (closeToPrevious(newNext)) newNext else remindedIndex,
            ferryWatch = ferries?.first ?: ferryWatch,
            pace = pace,
            place = place,
            progressLine =
              route.line(place, newNext, pace.metersPerSecond, fix.timeMillis, localTime),
          )
      }
    } else {
      val offRouteKnot = enteredOffRouteKnot(fix)
      val distanceToCourse = distanceToCourse(fix)
      when {
        offRouteKnot != null -> {
          if (!offRoute) notices += offRouteNotice(nextIndex)
          navigation = navigation.copy(offRoute = true, offRouteKnot = offRouteKnot)
        }
        distanceToCourse != null && distanceToCourse > OFF_COURSE_METERS -> {
          if (!offRoute) notices += offRouteNotice(nextIndex)
          navigation = navigation.copy(offRoute = true)
        }
        distanceToCourse != null &&
          offRoute &&
          (this.offRouteKnot == null || distanceTo(fix, this.offRouteKnot) > OFF_COURSE_METERS) -> {
          notices += backOnRouteNotice(nextIndex)
          navigation = navigation.copy(offRoute = false, offRouteKnot = null)
        }
      }
    }

    if (
      !navigation.arrived &&
        navigation.remindedIndex != navigation.nextIndex &&
        distanceTo(fix, navigation.nextKnot) <= REMINDER_METERS
    ) {
      notices += navigation.reminderNotice()
      navigation = navigation.copy(remindedIndex = navigation.nextIndex)
    }

    if (passedIndex == null && !navigation.offRoute) {
      navigation = navigation.copy(place = route.placeOf(fix, navigation.nextIndex))
    }
    navigation = navigation.copy(lastPreciseFix = fix, lastFix = fix, confirming = false)
    return NavigationUpdate(navigation, notices, navigation.precision(visible))
  }

  private fun updateCoarse(fix: Fix, visible: Boolean): NavigationUpdate {
    // Coarse fixes never count a knot as passed or the rider as off route; one far from the
    // course is confirmed with a fine fix.
    val distanceToCourse = distanceToCourse(fix)
    val navigation =
      copy(
        lastFix = fix,
        confirming = distanceToCourse != null && distanceToCourse > OFF_COURSE_METERS,
      )
    return NavigationUpdate(navigation, listOf(), navigation.precision(visible))
  }

  /**
   * The position of the first route knot from the next one on that [fix] entered, or `null`. The
   * end of a round trip only counts once it is the next knot, so returning to the start is not
   * taken for arriving.
   */
  private fun passedRouteIndex(fix: Fix): Int? {
    val roundTrip = knots.size > 2 && knots.first().id == knots.last().id
    return (nextIndex..knots.lastIndex).firstOrNull { index ->
      !(roundTrip && index == knots.lastIndex && index != nextIndex) && entered(fix, knots[index])
    }
  }

  private fun enteredOffRouteKnot(fix: Fix): Knot? =
    locator.nearest(fix.latitude, fix.longitude, PASS_RADIUS_METERS)?.takeIf {
      it.id !in routeKnotIds && entered(fix, it)
    }

  /**
   * Whether the rider entered the circle around [knot] with [fix]: checked on the line from the
   * previous precise fix, as fixes can be farther apart than the circle is wide. Being inside the
   * circle already does not count.
   */
  private fun entered(fix: Fix, knot: Knot): Boolean {
    val previous = lastPreciseFix
    if (previous != null && distanceTo(previous, knot) <= PASS_RADIUS_METERS) return false
    val line =
      previous?.takeIf { fix.timeMillis - it.timeMillis <= LINE_MAX_MILLIS }
        ?: return distanceTo(fix, knot) <= PASS_RADIUS_METERS
    return distanceToLine(
      knot.latitude,
      knot.longitude,
      GeoPoint(line.latitude, line.longitude),
      GeoPoint(fix.latitude, fix.longitude),
    ) <= PASS_RADIUS_METERS
  }

  /**
   * The distance from [fix] to the course of the current leg and its neighbors, or `null` without
   * courses.
   */
  private fun distanceToCourse(fix: Fix): Double? {
    val courses = courses ?: return null
    val current = nextIndex - 1
    return (current - 1..current + 1)
      .filter { it in courses.indices }
      .minOfOrNull { distanceToPolyline(fix.latitude, fix.longitude, courses[it]) }
  }

  /** Whether the knot at [index] is so close to the one before it that the notice named it. */
  private fun closeToPrevious(index: Int): Boolean =
    index > 0 &&
      distanceMeters(
        knots[index - 1].latitude,
        knots[index - 1].longitude,
        knots[index].latitude,
        knots[index].longitude,
      ) < REMINDER_METERS

  private fun nextNotice(index: Int, prefix: String): Notice {
    val next = knots[index].ref
    val after = knots.getOrNull(index + 1)?.ref
    return notice(
      NoticeKind.NEXT,
      if (after != null) "$prefix $next, then $after" else "$prefix $next",
      if (after != null) "$prefix ${spoken(next)}, then ${spoken(after)}"
      else "$prefix ${spoken(next)}",
    )
  }

  private fun reminderNotice(): Notice {
    val next = nextKnot.ref
    val after = knots.getOrNull(nextIndex + 1)?.ref
    return notice(
      NoticeKind.REMINDER,
      if (after != null) "$next ahead, then $after" else "$next ahead, the end",
      if (after != null) "${spoken(next)} ahead, then ${spoken(after)}"
      else "${spoken(next)} ahead, the end",
    )
  }

  private fun offRouteNotice(index: Int): Notice =
    notice(
      NoticeKind.OFF_ROUTE,
      "Off route, next ${knots[index].ref}",
      "Off route, next ${spoken(knots[index].ref)}",
    )

  private fun backOnRouteNotice(index: Int): Notice =
    notice(
      NoticeKind.BACK_ON_ROUTE,
      "Back on route, next ${knots[index].ref}",
      "Back on route, next ${spoken(knots[index].ref)}",
    )

  private fun arrivedNotice(): Notice =
    notice(NoticeKind.ARRIVED, "You've arrived", "You've arrived")

  private fun notice(kind: NoticeKind, text: String, speech: String) = Notice(kind, text, speech)

  private fun copy(
    nextIndex: Int = this.nextIndex,
    offRoute: Boolean = this.offRoute,
    arrived: Boolean = this.arrived,
    remindedIndex: Int? = this.remindedIndex,
    offRouteKnot: Knot? = this.offRouteKnot,
    confirming: Boolean = this.confirming,
    lastPreciseFix: Fix? = this.lastPreciseFix,
    lastFix: Fix = this.lastFix,
    ferryWatch: FerryWatch? = this.ferryWatch,
    pace: Pace = this.pace,
    place: Double = this.place,
    progressLine: String = this.progressLine,
  ) =
    Navigation(
      knots,
      courses,
      locator,
      routeKnotIds,
      nextIndex,
      offRoute,
      arrived,
      remindedIndex,
      offRouteKnot,
      confirming,
      lastPreciseFix,
      lastFix,
      ferryWatch,
      route,
      localTime,
      pace,
      place,
      progressLine,
    )

  companion object {

    /** A knot counts as passed within this distance, see `knot-navigation` "Passing a knot". */
    const val PASS_RADIUS_METERS = 30.0

    /** The reminder before the next knot, see `knot-navigation` "Reminder before a knot". */
    const val REMINDER_METERS = 200.0

    /** Off route farther from the course, see `knot-navigation` "Off route". */
    const val OFF_COURSE_METERS = 100.0

    /** Fixes less accurate than this are coarse: they only choose the precision. */
    const val PRECISE_ACCURACY_METERS = 50.0

    /** Within this distance of the next knot, the apps sample with [Precision.FINE]. */
    const val FINE_DISTANCE_METERS = 400.0

    /** The assumed pace applies until the rider covered this much of the route. */
    private const val OWN_PACE_AFTER_METERS = 2000.0

    /** Passing is checked on the line between fixes at most this far apart in time. */
    private const val LINE_MAX_MILLIS = 30_000L

    /**
     * Starts navigating the route through [knots] where [fix] is, see `knot-navigation` "Start
     * navigation".
     *
     * @param graph the network, to tell knots that are not on the route
     * @param geometry the network's courses, or `null` if they are not installed: straight lines do
     *   not count as a course
     * @param stops the positions of the stops in [knots], see `knot-navigation` "Distances and
     *   times ahead"
     * @param localTime the ferries' local time of an instant in epoch milliseconds, for the ferry
     *   warnings and the arrivals; without it, there are none
     * @return the navigation with what to tell the rider, or `null` if [knots] is not a route
     */
    fun start(
      knots: List<Knot>,
      graph: NetworkGraph,
      geometry: NetworkGeometry?,
      fix: Fix,
      visible: Boolean = false,
      stops: Set<Int> = setOf(),
      localTime: ((Long) -> LocalMoment)? = null,
    ): NavigationUpdate? {
      if (knots.size < 2) return null
      val courses = geometry?.let { network ->
        knots.zipWithNext { from, to -> network.courseOf(from, to) ?: return@let null }
      }
      val startIndex = knots.dropLast(1).indexOfFirst { distanceTo(fix, it) <= PASS_RADIUS_METERS }
      val legIndex =
        courses
          ?.withIndex()
          ?.map { (index, course) ->
            index to distanceToPolyline(fix.latitude, fix.longitude, course)
          }
          ?.filter { (_, distance) -> distance <= OFF_COURSE_METERS }
          ?.minByOrNull { (_, distance) -> distance }
          ?.first
      val (nextIndex, offRoute) =
        when {
          startIndex >= 0 -> startIndex + 1 to false
          legIndex != null -> legIndex + 1 to false
          else -> knots.indices.minBy { distanceTo(fix, knots[it]) } to (courses != null)
        }
      val route = RouteProgress.of(knots, graph, courses, stops)
      val place = if (startIndex >= 0) route.offsets[startIndex] else route.placeOf(fix, nextIndex)
      val pace = Pace(place, fix.timeMillis, route.assumedMetersPerSecond)
      val ferries = localTime?.let {
        FerryWatch.start(
          route,
          graph,
          it,
          place,
          nextIndex,
          fix.timeMillis,
          pace.metersPerSecond,
        )
      }
      val navigation =
        Navigation(
          knots = knots,
          courses = courses,
          locator = KnotLocator(graph.knots.values),
          routeKnotIds = knots.map { it.id }.toSet(),
          nextIndex = nextIndex,
          offRoute = offRoute,
          arrived = false,
          remindedIndex = null,
          offRouteKnot = null,
          confirming = false,
          lastPreciseFix = fix.takeIf { it.accuracyMeters <= PRECISE_ACCURACY_METERS },
          lastFix = fix,
          ferryWatch = ferries?.first,
          route = route,
          localTime = localTime,
          pace = pace,
          place = place,
          progressLine =
            route.line(place, nextIndex, pace.metersPerSecond, fix.timeMillis, localTime),
        )
      val notice =
        if (offRoute) navigation.offRouteNotice(nextIndex)
        else navigation.nextNotice(nextIndex, prefix = "Next")
      // The notice on starting names the next knot, so a knot close by needs no reminder.
      val reminded =
        if (distanceTo(fix, navigation.nextKnot) <= REMINDER_METERS)
          navigation.copy(remindedIndex = nextIndex)
        else navigation
      val notices = listOf(notice) + ferries?.second.orEmpty()
      return NavigationUpdate(reminded, notices, reminded.precision(visible))
    }

    /** How to speak a knot's number: `08` as `8`, so it is spoken as "eight". */
    fun spoken(ref: String): String =
      if (ref.isNotEmpty() && ref.all { it.isDigit() }) ref.trimStart('0').ifEmpty { "0" } else ref
  }
}

private fun distanceTo(fix: Fix, knot: Knot): Double =
  distanceMeters(fix.latitude, fix.longitude, knot.latitude, knot.longitude)

private const val METERS_PER_DEGREE_LATITUDE = 6_371_008.8 * PI / 180

/** The distance from a position to the line between [from] and [to], in a local flat projection. */
private fun distanceToLine(
  latitude: Double,
  longitude: Double,
  from: GeoPoint,
  to: GeoPoint,
): Double {
  val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(latitude * PI / 180)
  val fromX = (from.longitude - longitude) * metersPerDegreeLongitude
  val fromY = (from.latitude - latitude) * METERS_PER_DEGREE_LATITUDE
  val toX = (to.longitude - longitude) * metersPerDegreeLongitude
  val toY = (to.latitude - latitude) * METERS_PER_DEGREE_LATITUDE
  val deltaX = toX - fromX
  val deltaY = toY - fromY
  val lengthSquared = deltaX * deltaX + deltaY * deltaY
  val along =
    if (lengthSquared == 0.0) 0.0
    else (-(fromX * deltaX + fromY * deltaY) / lengthSquared).coerceIn(0.0, 1.0)
  val nearestX = fromX + along * deltaX
  val nearestY = fromY + along * deltaY
  return sqrt(nearestX * nearestX + nearestY * nearestY)
}

private fun distanceToPolyline(
  latitude: Double,
  longitude: Double,
  points: List<GeoPoint>,
): Double =
  points.zipWithNext().minOf { (from, to) -> distanceToLine(latitude, longitude, from, to) }
