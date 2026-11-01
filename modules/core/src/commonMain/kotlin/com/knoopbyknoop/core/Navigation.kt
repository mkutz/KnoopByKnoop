package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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

  /**
   * Off route, the distance and direction to the next knot again, see `knot-navigation` "Off
   * route".
   */
  GUIDANCE,
}

/**
 * The straight way to the next knot while off route, see `knot-navigation` "Direction to the
 * route".
 *
 * @property distance e.g. `1.7 km`
 * @property bearing the direction to the knot in degrees clockwise from north
 * @property direction the compass direction in words, e.g. `north-east`
 */
data class Guidance(
  val distance: String,
  val bearing: Double,
  val direction: String,
  /** What screen readers say after the next knot, e.g. `1.7 km to the north-east`. */
  val text: String = EnglishTexts.way(distance, direction),
)

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
 * @property close whether the rider is close to the next knot, from its reminder on: only then is
 *   the number to follow the most prominent, see `knot-navigation` "Number to follow"
 */
data class NavigationDisplay(
  val next: String,
  val follow: String?,
  val offRoute: Boolean,
  val language: Language = Language.EN,
  val close: Boolean = true,
) {

  /** E.g. `At 45 follow 12`, or `31 is the end`. */
  val text: String
    get() =
      if (follow != null) language.texts.follow(next, follow) else language.texts.isTheEnd(next)

  /** E.g. `Off route, next 45`, shown instead of [text] while off route. */
  val offRouteText: String
    get() = language.texts.offRouteNext(next)
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
  private val guided: Guided?,
  /** The language of the texts, see `app-languages`. */
  val language: Language,
  /**
   * The language of the spoken notices: [language], or English where the phone has no voice for it,
   * see `app-languages` "Spoken notices in the language".
   */
  private val speechLanguage: Language,
) {

  private val texts = language.texts
  private val speech = speechLanguage.texts

  /**
   * What the rider was last told off route: the distance and bearing of the last notice, and the
   * distance in [progressLine].
   */
  private data class Guided(
    val noticeMeters: Double,
    val noticeBearing: Double,
    val lineMeters: Double,
  )

  /** The straight way to the next knot from [fix] while off route, else `null`. */
  fun guidance(fix: Fix): Guidance? {
    if (!offRoute || arrived) return null
    return wayTo(fix, nextKnot, language)
  }

  /**
   * Where the line back to the route ends from [fix] while off route, else `null`: the nearest
   * point of the course ahead of the last place on the route, or the next knot without a course,
   * see `knot-navigation` "Way to the route on the map".
   */
  fun wayToRoute(fix: Fix): GeoPoint? {
    if (!offRoute || arrived) return null
    return route.nearestAhead(fix, nextIndex, place)
      ?: GeoPoint(nextKnot.latitude, nextKnot.longitude)
  }

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
    val straightDistance =
      if (offRoute && !arrived) route.straightTo(fix, nextIndex) else straightToFirst(fix)
    val place =
      if (offRoute || arrived || fix.accuracyMeters > PRECISE_ACCURACY_METERS) this.place
      else route.placeOf(fix, nextIndex)
    return route.view(
      place,
      nextIndex,
      straightDistance,
      offRoute && !arrived,
      pace.metersPerSecond,
      fix.timeMillis,
      localTime,
      { ferryWatch?.arrivalAt(it)?.state },
      language,
    )
  }

  val nextKnot: Knot
    get() = knots[nextIndex]

  val display: NavigationDisplay
    get() =
      NavigationDisplay(
        nextKnot.ref,
        knots.getOrNull(nextIndex + 1)?.ref,
        offRoute,
        language,
        close = remindedIndex == nextIndex,
      )

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
        notices += if (offRoute) backOnRouteNotice(newNext) else nextNotice(newNext)
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
              route.line(place, newNext, pace.metersPerSecond, fix.timeMillis, localTime, language),
            guided = null,
          )
      }
    } else {
      val offRouteKnot = enteredOffRouteKnot(fix)
      val distanceToCourse = distanceToCourse(fix)
      when {
        offRouteKnot != null -> {
          if (!offRoute) notices += offRouteNotice(nextIndex, fix)
          navigation = navigation.copy(offRoute = true, offRouteKnot = offRouteKnot)
        }
        distanceToCourse != null && distanceToCourse > OFF_COURSE_METERS -> {
          if (!offRoute) notices += offRouteNotice(nextIndex, fix)
          navigation = navigation.copy(offRoute = true)
        }
        distanceToCourse != null &&
          distanceToCourse <= ON_COURSE_METERS &&
          offRoute &&
          (this.offRouteKnot == null ||
            distanceToNearestNode(fix, this.offRouteKnot) > OFF_COURSE_METERS) -> {
          notices += backOnRouteNotice(nextIndex)
          navigation = navigation.copy(offRoute = false, offRouteKnot = null, guided = null)
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
      // Back on route: the route's progress line again, see "Direction outside the app".
      if (offRoute) navigation = navigation.copy(progressLine = navigation.routeLine(fix))
    }
    if (navigation.offRoute) {
      val (guided, guidance) = navigation.guide(fix, startedNow = !offRoute)
      guidance?.let { notices += it }
      navigation = guided
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
   * The position of the last route knot passed with [fix], or `null`: the first one from the next
   * one that [fix] entered, and the knots right after it that [fix] is within the circle of, as at
   * a double knot. The end of a round trip only counts once the knots before it are passed, so
   * returning to the start is not taken for arriving.
   */
  private fun passedRouteIndex(fix: Fix): Int? {
    val roundTrip = knots.size > 2 && knots.first().id == knots.last().id
    val entered =
      (nextIndex..knots.lastIndex).firstOrNull { index ->
        !(roundTrip && index == knots.lastIndex && index != nextIndex) && entered(fix, knots[index])
      } ?: return null
    return lastWithin(knots, entered, knots.lastIndex, fix)
  }

  private fun enteredOffRouteKnot(fix: Fix): Knot? =
    locator.nearestAtAnyNode(fix.latitude, fix.longitude, PASS_RADIUS_METERS)?.takeIf {
      it.id !in routeKnotIds && entered(fix, it)
    }

  /**
   * Whether the rider entered the circle around [knot], around any of the nodes it is tagged on,
   * with [fix]: checked on the line from the previous precise fix, as fixes can be farther apart
   * than the circle is wide. Being inside a circle already does not count.
   */
  private fun entered(fix: Fix, knot: Knot): Boolean {
    val previous = lastPreciseFix
    if (previous != null && distanceToNearestNode(previous, knot) <= PASS_RADIUS_METERS)
      return false
    val line =
      previous?.takeIf { fix.timeMillis - it.timeMillis <= LINE_MAX_MILLIS }
        ?: return distanceToNearestNode(fix, knot) <= PASS_RADIUS_METERS
    return knot.positions.any { position ->
      distanceToLine(
        position.latitude,
        position.longitude,
        GeoPoint(line.latitude, line.longitude),
        GeoPoint(fix.latitude, fix.longitude),
      ) <= PASS_RADIUS_METERS
    }
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

  private fun nextNotice(index: Int): Notice {
    val next = knots[index].ref
    val after = knots.getOrNull(index + 1)?.ref
    return notice(
      NoticeKind.NEXT,
      texts.next(next, after),
      speech.next(spoken(next), after?.let(::spoken)),
    )
  }

  private fun reminderNotice(): Notice {
    val next = nextKnot.ref
    val after = knots.getOrNull(nextIndex + 1)?.ref
    return notice(
      NoticeKind.REMINDER,
      texts.ahead(next, after),
      speech.ahead(spoken(next), after?.let(::spoken)),
    )
  }

  /** E.g. `Off route, next 45, 1.7 km to the north-east`, see `knot-navigation` "Off route". */
  private fun offRouteNotice(index: Int, fix: Fix): Notice {
    return notice(
      NoticeKind.OFF_ROUTE,
      texts.offRoute(knots[index].ref, wayTo(fix, knots[index], language).text),
      speech.offRoute(spoken(knots[index].ref), wayTo(fix, knots[index], speechLanguage).text),
    )
  }

  private fun routeLine(fix: Fix): String =
    route.line(
      place,
      nextIndex,
      pace.metersPerSecond,
      fix.timeMillis,
      localTime,
      language,
      straightToFirst(fix),
    )

  /**
   * The straight distance to the first knot while it is the next one, as there is no route before
   * it, see `knot-navigation` "Number to follow"; else `null`.
   */
  private fun straightToFirst(fix: Fix): Double? =
    if (nextIndex == 0) route.straightTo(fix, 0) else null

  /**
   * While off route: the guidance notice when the rider got [GUIDANCE_METERS] closer or the
   * direction turned by more than [GUIDANCE_DEGREES] since the last notice, and the line outside
   * the app renewed per [LINE_METERS]; [startedNow] when the off-route notice was just given.
   */
  private fun guide(fix: Fix, startedNow: Boolean): Pair<Navigation, Notice?> {
    val meters = distanceTo(fix, nextKnot)
    val bearing = bearing(fix, nextKnot)
    val way = wayTo(fix, nextKnot, language)
    val last = guided
    val notice =
      if (startedNow || last == null) null
      else if (
        meters <= last.noticeMeters - GUIDANCE_METERS ||
          angleBetween(bearing, last.noticeBearing) > GUIDANCE_DEGREES
      )
        notice(
          NoticeKind.GUIDANCE,
          texts.guidance(nextKnot.ref, way.text),
          speech.guidance(spoken(nextKnot.ref), wayTo(fix, nextKnot, speechLanguage).text),
        )
      else null
    val told = startedNow || last == null || notice != null
    val renewLine = last == null || kotlin.math.abs(meters - last.lineMeters) >= LINE_METERS
    val guided =
      Guided(
        noticeMeters = if (told) meters else last.noticeMeters,
        noticeBearing = if (told) bearing else last.noticeBearing,
        lineMeters = if (renewLine) meters else last.lineMeters,
      )
    val line =
      if (renewLine) texts.wayTo(way.distance, way.direction, nextKnot.ref) else progressLine
    return copy(guided = guided, progressLine = line) to notice
  }

  private fun backOnRouteNotice(index: Int): Notice =
    notice(
      NoticeKind.BACK_ON_ROUTE,
      texts.backOnRoute(knots[index].ref),
      speech.backOnRoute(spoken(knots[index].ref)),
    )

  private fun arrivedNotice(): Notice = notice(NoticeKind.ARRIVED, texts.arrived, speech.arrived)

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
    guided: Guided? = this.guided,
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
      guided,
      language,
      speechLanguage,
    )

  companion object {

    /** A knot counts as passed within this distance, see `knot-navigation` "Passing a knot". */
    const val PASS_RADIUS_METERS = 30.0

    /** The reminder before the next knot, see `knot-navigation` "Reminder before a knot". */
    const val REMINDER_METERS = 200.0

    /** Off route farther from the course, see `knot-navigation` "Off route". */
    const val OFF_COURSE_METERS = 100.0

    /**
     * Off route, back on route only this close to the course, so a path beside it is not the route,
     * see `knot-navigation` "Off route".
     */
    const val ON_COURSE_METERS = 30.0

    /** Fixes less accurate than this are coarse: they only choose the precision. */
    const val PRECISE_ACCURACY_METERS = 50.0

    /** Within this distance of the next knot, the apps sample with [Precision.FINE]. */
    const val FINE_DISTANCE_METERS = 400.0

    /** Off route, a guidance notice when the rider got this much closer, see "Off route". */
    private const val GUIDANCE_METERS = 500.0

    /** Off route, a guidance notice when the direction turned by more than this. */
    private const val GUIDANCE_DEGREES = 45.0

    /** Off route, the line outside the app is renewed when the distance changed by this much. */
    private const val LINE_METERS = 100.0

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
      language: Language = Language.EN,
      speechLanguage: Language = language,
    ): NavigationUpdate? {
      if (knots.size < 2) return null
      val courses = geometry?.let { network ->
        knots.zipWithNext { from, to -> network.courseOf(from, to) ?: return@let null }
      }
      val startIndex =
        knots
          .dropLast(1)
          .indexOfFirst { distanceToNearestNode(fix, it) <= PASS_RADIUS_METERS }
          .let { if (it >= 0) lastWithin(knots, it, knots.lastIndex - 1, fix) else it }
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
          // Away from the route: to its start, see `knot-navigation` "Start navigation"; without
          // courses, the app cannot tell the rider is away, so the nearest knot.
          courses != null -> 0 to true
          else -> knots.indices.minBy { distanceTo(fix, knots[it]) } to false
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
          language,
          speechLanguage,
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
              route.line(
                place,
                nextIndex,
                pace.metersPerSecond,
                fix.timeMillis,
                localTime,
                language,
                if (nextIndex == 0) route.straightTo(fix, 0) else null,
              ),
            guided = null,
            language = language,
            speechLanguage = speechLanguage,
          )
          .let { if (offRoute) it.guide(fix, startedNow = true).first else it }
      val notice =
        if (offRoute) navigation.offRouteNotice(nextIndex, fix)
        else navigation.nextNotice(nextIndex)
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

/**
 * The position of the last knot in [knots] from [from] up to [until] that [fix] is within the pass
 * circle of without a gap, see `knot-navigation` "Passing a double knot".
 */
private fun lastWithin(knots: List<Knot>, from: Int, until: Int, fix: Fix): Int {
  var index = from
  while (
    index < until && distanceToNearestNode(fix, knots[index + 1]) <= Navigation.PASS_RADIUS_METERS
  ) {
    index++
  }
  return index
}

private fun distanceTo(fix: Fix, knot: Knot): Double =
  distanceMeters(fix.latitude, fix.longitude, knot.latitude, knot.longitude)

/** The distance from [fix] to the nearest of the nodes [knot] is tagged on. */
private fun distanceToNearestNode(fix: Fix, knot: Knot): Double =
  knot.positions.minOf { distanceMeters(fix.latitude, fix.longitude, it.latitude, it.longitude) }

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

/** The compass direction of [bearing] in words, e.g. `north-east` from 22.5° to 67.5°. */
internal fun compass(bearing: Double, language: Language = Language.EN): String =
  language.texts.compassDirections[(((bearing + 22.5) % 360 + 360) % 360 / 45).toInt() % 8]

/** The initial great-circle bearing from [fix] to [knot], clockwise from north. */
private fun bearing(fix: Fix, knot: Knot): Double {
  val fromLatitude = fix.latitude * PI / 180
  val toLatitude = knot.latitude * PI / 180
  val deltaLongitude = (knot.longitude - fix.longitude) * PI / 180
  val y = sin(deltaLongitude) * cos(toLatitude)
  val x =
    cos(fromLatitude) * sin(toLatitude) - sin(fromLatitude) * cos(toLatitude) * cos(deltaLongitude)
  return (atan2(y, x) * 180 / PI + 360) % 360
}

/** The smaller angle between two bearings. */
private fun angleBetween(first: Double, second: Double): Double {
  val difference = kotlin.math.abs(first - second) % 360
  return if (difference > 180) 360 - difference else difference
}

private fun wayTo(fix: Fix, knot: Knot, language: Language): Guidance {
  val bearing = bearing(fix, knot)
  val distance = MapFeatures.formatDistance(distanceTo(fix, knot).roundToInt(), language)
  val direction = compass(bearing, language)
  return Guidance(distance, bearing, direction, language.texts.way(distance, direction))
}
