package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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

  /** Riding along the route the wrong way, see `knot-navigation` "Riding the wrong way". */
  WRONG_WAY,
  ARRIVED,

  /** A ferry ahead is closed when the rider gets there, see `knot-navigation` "Ferry warning". */
  FERRY,

  /**
   * Off route, the distance and direction to the next knot again, see `knot-navigation` "Off
   * route".
   */
  GUIDANCE,

  /** A turn on the guided way, see `knot-navigation` "Turn notices on the guided way". */
  TURN,
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
 * @property wrongWay whether the rider rides back along the route, see `knot-navigation` "Riding
 *   the wrong way"
 * @property nextRole the role of the next knot if it is a route point, which has no number; the
 *   apps draw a flag in place of [next], see `knot-navigation` "Navigating with route points"
 * @property followRole the same for [follow]
 */
data class NavigationDisplay(
  val next: String,
  val follow: String?,
  val offRoute: Boolean,
  val language: Language = Language.EN,
  val close: Boolean = true,
  val wrongWay: Boolean = false,
  val nextRole: WaypointRole? = null,
  val followRole: WaypointRole? = null,
) {

  private val nextSpot: Spot
    get() = Spot(next, nextRole)

  /**
   * E.g. `At 45 follow 12` when [close], `Next 45, then 12` before, `31 is the end`, or `Wrong
   * direction, next 45` when [wrongWay].
   */
  val text: String
    get() =
      when {
        wrongWay -> language.texts.wrongWayNext(nextSpot)
        follow == null -> language.texts.isTheEnd(nextSpot)
        close -> language.texts.follow(nextSpot, Spot(follow, followRole))
        else -> language.texts.nextOutside(nextSpot, Spot(follow, followRole))
      }

  /** E.g. `Off route, next 45`, shown instead of [text] while off route. */
  val offRouteText: String
    get() = language.texts.offRouteNext(nextSpot)
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
  private val signpostCourses: List<List<List<GeoPoint>>>,
  private val drawnCourses: List<List<GeoPoint>>,
  private val locator: KnotLocator,
  private val routeKnotIds: Set<Long>,
  val nextIndex: Int,
  val offRoute: Boolean,
  val arrived: Boolean,
  private val remindedIndex: Int?,
  private val offRouteKnot: Knot?,
  /**
   * The knot passed last while the rider has not been more than [REMINDER_METERS] from it since,
   * see `knot-navigation` "A knot the route visits twice".
   */
  private val passedKnot: Knot?,
  private val confirming: Boolean,
  private val lastPreciseFix: Fix?,
  val lastFix: Fix,
  private val ferryWatch: FerryWatch?,
  private val route: RouteProgress,
  private val localTime: ((Long) -> LocalMoment)?,
  private val pace: Pace,
  private val place: Double,
  /**
   * The line outside the app, e.g. `0.8 km to 45 · end around 19:05`, renewed about every
   * [LINE_METERS] of the route, see `knot-navigation` "Navigation with the screen off".
   */
  val progressLine: String,
  /** Where on the route [progressLine] was last renewed. */
  private val linePlace: Double,
  private val guided: Guided?,
  private val directionWatch: DirectionWatch?,
  /**
   * Whether the rider has been on the route since navigation started, see `knot-navigation` "Header
   * on the way to the route".
   */
  private val joinedRoute: Boolean,
  /**
   * On the way to the first knot, the leg whose course the rider follows and where along the route
   * they first were on it, see `knot-navigation` "Joining the course on the way to the start".
   */
  private val joining: Joining?,
  /** Per leg, where its way along paths leaves or joins the network, see [LegTurns]. */
  private val legTurns: List<LegTurns?>,
  /** The route's tricky stretches, see `knot-navigation` "Tricky spots on the route". */
  private val tricky: List<RouteTricky>,
  /** When the last knot was passed, for "Zoom out after passing a knot". */
  private val passedAtMillis: Long?,
  /** The language of the texts, see `app-languages`. */
  val language: Language,
  /**
   * The language of the spoken notices: [language], or English where the phone has no voice for it,
   * see `app-languages` "Spoken notices in the language".
   */
  private val speechLanguage: Language,
  /** Where the line back to the route ends, kept while off route, see [wayToRoute]. */
  private val rejoin: RouteProgress.Rejoin? = null,
) {

  private val texts = language.texts
  private val speech = speechLanguage.texts

  /** A leg the rider follows on the way to the first knot, from [place] along the route on. */
  private data class Joining(val leg: Int, val place: Double)

  /**
   * What the rider was last told off route: the distance and bearing of the last notice, and the
   * distance in [progressLine].
   */
  private data class Guided(
    val noticeMeters: Double,
    val noticeBearing: Double,
    val lineMeters: Double,
  )

  /**
   * Which way the rider goes along the current leg, see `knot-navigation` "Riding the wrong way".
   *
   * @property furthestPlace the highest place reached on the leg
   * @property furthestFix where the rider was there
   * @property wrongWay whether the rider was told they ride the wrong way
   * @property lowestPlace how far back they got since
   */
  private data class DirectionWatch(
    val furthestPlace: Double,
    val furthestFix: Fix,
    val wrongWay: Boolean = false,
    val lowestPlace: Double = furthestPlace,
  )

  /** Whether the rider rides back along the route, see `knot-navigation` "Riding the wrong way". */
  val wrongWay: Boolean
    get() = directionWatch?.wrongWay == true && !offRoute && !arrived

  /** The straight way to the next knot from [fix] while off route, else `null`. */
  fun guidance(fix: Fix): Guidance? {
    if (!offRoute || arrived) return null
    return wayTo(fix, headedFor(nextIndex), language)
  }

  /**
   * Where the line back to the route ends from [fix] while off route, else `null`: the nearest
   * point of the course ahead of the last place on the route, or the next knot without a course,
   * see `knot-navigation` "Way to the route on the map".
   */
  fun wayToRoute(fix: Fix): GeoPoint? {
    if (!offRoute || arrived) return null
    return route.rejoinAhead(fix, nextIndex, place, rejoin)?.point ?: headedFor(nextIndex)
  }

  /** This navigation with the line back to the route renewed for [fix], see [wayToRoute]. */
  private fun withRejoin(fix: Fix): Navigation =
    copy(
      rejoin = if (offRoute && !arrived) route.rejoinAhead(fix, nextIndex, place, rejoin) else null
    )

  /**
   * What the header shows with the rider at [fix], see `knot-navigation` "Number to follow",
   * "Direction to the route" and "Header on the way to the route".
   *
   * @param ridingDirection the rider's direction in degrees clockwise from north, or `null` when
   *   they have not moved recently, so the arrow points relative to north as the map
   * @param guidedWay the way back to the route the rider follows, if any
   * @param network the installed network's courses, to tell whether the rider is on a path of the
   *   knot network while off route
   */
  fun header(
    fix: Fix,
    ridingDirection: Double? = null,
    guidedWay: GuidedWay? = null,
    network: NetworkGeometry? = null,
  ): HeaderRow {
    val texts = language.texts
    val next = spotAt(nextIndex)
    if (offRoute && !arrived) {
      val target = wayToRoute(fix) ?: headedFor(nextIndex)
      val meters = distanceTo(fix, target)
      val bearing = bearing(fix, target)
      val distance = MapFeatures.formatDistance(meters.roundToInt(), language)
      val onNetwork =
        network
          ?.nearestOnCourses(GeoPoint(fix.latitude, fix.longitude), ON_NETWORK_METERS)
          ?.isNotEmpty() == true
      val guided = guidedWay?.headerTurn(language)
      val way = texts.way(distance, compass(bearing, language))
      return HeaderRow.ToRoute(
        offRoute = joinedRoute,
        arrowDegrees = normalized(bearing - (ridingDirection ?: 0.0)),
        distance = distance,
        next = if (onNetwork) badge(nextIndex, sign = true) else null,
        guided = guided,
        spoken =
          when {
            !joinedRoute -> texts.guidance(next, guided?.description ?: way)
            else -> "${texts.offRouteNext(next)}, ${guided?.description ?: way}"
          },
      )
    }
    val offNetwork = offNetworkTarget()
    if (offNetwork != null) {
      // On the way of a route point off the network, as on the way to the route, see
      // "Guidance to a route point off the network".
      val bearing = bearing(fix, offNetwork)
      val distance = MapFeatures.formatDistance(distanceTo(fix, offNetwork).roundToInt(), language)
      val guided = guidedWay?.headerTurn(language)
      val way = texts.way(distance, compass(bearing, language))
      return HeaderRow.ToRoute(
        offRoute = false,
        arrowDegrees = normalized(bearing - (ridingDirection ?: 0.0)),
        distance = distance,
        next = badge(nextIndex, sign = true),
        guided = guided,
        spoken = texts.guidance(next, guided?.description ?: way),
      )
    }
    val distance = progress(fix, guidedWay).nextKnotDistance
    val after = knots.getOrNull(nextIndex + 1)?.let { spotAt(nextIndex + 1) }
    val display = display
    return when {
      wrongWay ->
        HeaderRow.WrongWay(badge(nextIndex), distance, "${texts.wrongWayNext(next)}, $distance")
      after == null || arrived ->
        HeaderRow.End(badge(nextIndex), distance, "${texts.isTheEnd(next)}, $distance")
      // A route point has no signs to follow, so it is shown as before the reminder.
      display.close && next.role == null && after.role == null -> {
        val turn = turnAt(nextIndex)
        val follow = texts.follow(next, after)
        HeaderRow.Close(
          badge(nextIndex),
          distance,
          badge(nextIndex + 1, sign = true, onPlate = true),
          if (turn == null) "$follow, $distance" else "$follow ${texts.towards(turn)}, $distance",
          turn,
        )
      }
      else ->
        HeaderRow.Far(
          badge(nextIndex),
          distance,
          badge(nextIndex + 1, sign = true),
          texts.nextAt(next, distance, after),
        )
    }
  }

  /**
   * What navigation shows outside the app with the rider at [fix], see `knot-navigation`
   * "Navigation with the screen off"; the parameters as for [header].
   */
  fun outside(
    fix: Fix,
    ridingDirection: Double? = null,
    guidedWay: GuidedWay? = null,
    network: NetworkGeometry? = null,
  ): OutsideView =
    header(fix, ridingDirection, guidedWay, network).outside(endArrival(fix, guidedWay))

  /** When the rider gets to the end at their pace, e.g. `19:05`, where the local time is known. */
  fun endArrival(fix: Fix, guidedWay: GuidedWay? = null): String? =
    progress(fix, guidedWay).strip.marks.lastOrNull { it.kind == ProgressMarkKind.END }?.arrival

  /**
   * The turn the route makes at the knot at [index], comparing its direction over the [TURN_METERS]
   * before the knot with the [TURN_METERS] after it, along the drawn courses and across knots
   * within them, see `knot-navigation` "Turn at the next knot"; `null` at the start, at the end,
   * without courses, or where a leg is too short to tell.
   */
  internal fun turnAt(index: Int): TurnDirection? {
    if (courses == null || index <= 0 || index >= knots.lastIndex) return null
    val arrivedAt = drawnCourses[index - 1].lastOrNull() ?: return null
    val leavesFrom = drawnCourses[index].firstOrNull() ?: return null
    val before =
      pointAlong(
        (index - 1 downTo 0).asSequence().flatMap { drawnCourses[it].asReversed() },
        TURN_METERS,
      ) ?: return null
    val after =
      pointAlong(
        (index..drawnCourses.lastIndex).asSequence().flatMap { drawnCourses[it] },
        TURN_METERS,
      ) ?: return null
    if (
      meters(before, arrivedAt) < TURN_MIN_METERS || meters(leavesFrom, after) < TURN_MIN_METERS
    ) {
      return null
    }
    return turnOf(signedAngle(heading(before, arrivedAt), heading(leavesFrom, after)))
  }

  /**
   * The badge of the knot at [index] in the header: filled as at the knot itself, or with [sign] as
   * on the arrow signs pointing to it, which are white in the Netherlands, see `knot-navigation`
   * "Number to follow".
   */
  private fun badge(index: Int, sign: Boolean = false, onPlate: Boolean = false): HeaderBadge {
    val knot = knots[index]
    return HeaderBadge(
      ref = knot.ref,
      plate = KnotPlate.of(knot.signRegion),
      filled = !sign || knot.signRegion != SignRegion.NETHERLANDS,
      role =
        when (index) {
          0 -> KnotRole.START
          knots.lastIndex -> KnotRole.END
          in route.stops -> KnotRole.STOP
          else -> KnotRole.PASSED
        },
      onPlate = onPlate,
    )
  }

  /**
   * The rider's pace over the last [PACE_WINDOW_METERS] of the route, without standing still, see
   * `knot-navigation` "Ferry arrival estimate".
   *
   * @property lastOffset where the rider was when the last stretch ended
   * @property lastMillis when the rider was there
   * @property stretches the newest stretches ridden, oldest first
   * @property coveredMeters the route covered in all
   */
  internal data class Pace(
    val lastOffset: Double,
    val lastMillis: Long,
    val metersPerSecond: Double,
    val stretches: List<Stretch> = listOf(),
    val coveredMeters: Double = 0.0,
  ) {

    /** A stretch of the route ridden in [seconds], leaving out standing still, see [at]. */
    data class Stretch(val meters: Double, val seconds: Double)

    /** The pace with the rider at [offset] at [timeMillis]; short steps wait for more. */
    fun at(offset: Double, timeMillis: Long, assumed: Double): Pace {
      val meters = offset - lastOffset
      if (meters < PACE_STRETCH_METERS) return this
      val seconds = (timeMillis - lastMillis) / 1000.0
      // Much faster than anyone rides, the place on the route jumped, e.g. on a leg there and back.
      if (seconds * MAX_PLAUSIBLE_PACE < meters) return restartedAt(offset, timeMillis)
      // Slower than a third of the assumed pace counts as standing still.
      val stretch = Stretch(meters, min(seconds, meters * STANDING_STILL_RATIO / assumed))
      var windowMeters = 0.0
      val window =
        (stretches + stretch).asReversed().takeWhile { newer ->
          (windowMeters < PACE_WINDOW_METERS).also { windowMeters += newer.meters }
        }
      val covered = coveredMeters + meters
      val windowSeconds = window.sumOf { it.seconds }
      return copy(
        lastOffset = offset,
        lastMillis = timeMillis,
        metersPerSecond =
          if (covered >= OWN_PACE_AFTER_METERS && windowSeconds > 0)
            window.sumOf { it.meters } / windowSeconds
          else assumed,
        stretches = window.asReversed(),
        coveredMeters = covered,
      )
    }

    /** Measures anew from [offset] at [timeMillis]: the way there does not count. */
    fun restartedAt(offset: Double, timeMillis: Long): Pace =
      copy(lastOffset = offset, lastMillis = timeMillis)
  }

  /**
   * The progress strip and the distances and times ahead with the rider at [fix], see
   * `knot-navigation` "Progress strip" and "Distances and times ahead"; on [guidedWay], along it,
   * see "Distances ahead on a guided way".
   */
  fun progress(fix: Fix, guidedWay: GuidedWay? = null): ProgressView {
    val straightDistance =
      when {
        offRoute && !arrived && guidedWay != null -> alongGuidedWay(guidedWay, fix)
        offRoute && !arrived -> route.straightTo(fix, nextIndex)
        else -> straightToFirst(fix)
      }
    val place =
      if (offRoute || arrived || fix.accuracyMeters > PRECISE_ACCURACY_METERS) this.place
      else route.placeOf(fix, nextIndex)
    return route.view(
      place,
      nextIndex,
      straightDistance,
      (offRoute || wrongWay) && !arrived,
      pace.metersPerSecond,
      fix.timeMillis,
      localTime,
      { ferryWatch?.arrivalAt(it)?.state },
      language,
    )
  }

  /** How far the next knot is along [guidedWay] and then along the route. */
  private fun alongGuidedWay(guidedWay: GuidedWay, fix: Fix): Double {
    val end = guidedWay.way.points.last()
    val endPlace =
      route.placeOf(fix.copy(latitude = end.latitude, longitude = end.longitude), nextIndex)
    return guidedWay.remainingMeters + max(0.0, route.offsets[nextIndex] - endPlace)
  }

  val nextKnot: Knot
    get() = knots[nextIndex]

  /**
   * The signpost the route's line uses at the knot at [index], see `knot-navigation` "Straight ways
   * to a knot with several signposts".
   */
  private fun headedFor(index: Int): GeoPoint = route.headedFor[index]

  /**
   * The course to the next knot as the map draws it, or `null` before the first knot, see
   * `knot-navigation` "Leg ahead on the map".
   */
  val courseAhead: List<GeoPoint>?
    get() = drawnCourses.getOrNull(nextIndex - 1).takeUnless { arrived }

  val display: NavigationDisplay
    get() =
      NavigationDisplay(
        nextKnot.ref,
        knots.getOrNull(nextIndex + 1)?.ref,
        offRoute,
        language,
        close = remindedIndex == nextIndex,
        wrongWay = wrongWay,
        nextRole = spotAt(nextIndex).role,
        followRole = knots.getOrNull(nextIndex + 1)?.let { spotAt(nextIndex + 1).role },
      )

  /** What sentences call the knot at [index]: its number, or a route point's role. */
  private fun spotAt(index: Int): Spot = Spot.at(knots, index)

  /**
   * When the rider gets to the ferry on the leg from the knot at [fromPosition] of [knots], as
   * estimated at the last knot passed, or `null` once it is passed or without an estimate, see
   * `knot-navigation` "Ferry arrival estimate".
   */
  fun ferryArrival(fromPosition: Int): FerryArrival? = ferryWatch?.arrivalAt(fromPosition)

  /**
   * This navigation in other languages with the rider at [fix], everything else as it was, see
   * `app-languages` "Language change while navigating".
   */
  fun inLanguage(newLanguage: Language, newSpeechLanguage: Language, fix: Fix): Navigation {
    val navigation =
      copy(
        language = newLanguage,
        speechLanguage = newSpeechLanguage,
        ferryWatch =
          ferryWatch?.inLanguage(
            newLanguage,
            newSpeechLanguage,
            place,
            fix.timeMillis,
            nextIndex,
            pace.metersPerSecond,
          ),
      )
    val line =
      if (offRoute && !arrived) {
        val way = wayTo(fix, headedFor(nextIndex), newLanguage)
        newLanguage.texts.wayTo(way.distance, way.direction, spotAt(nextIndex))
      } else {
        navigation.routeLine(fix)
      }
    return navigation.copy(progressLine = line)
  }

  /**
   * What the map shows while following the rider at [fix], see `knot-navigation` "Zoom while
   * following": a tricky spot wins over the next knot, the knot over the zoom-out after passing
   * one, and that over the navigation zoom. Route points get the navigation zoom, as they have no
   * signposts.
   */
  fun mapFocus(fix: Fix): MapFocus {
    if (arrived) return MapFocus.BASE
    if (offRoute) {
      return MapFocus(MapFocusKind.OFF_ROUTE, nextIndex, listOfNotNull(wayToRoute(fix)), null, null)
    }
    val trickyAhead =
      tricky.withIndex().firstOrNull { (_, stretch) ->
        stretch.start - place <= MapFocus.TRICKY_AHEAD_METERS &&
          place <= stretch.end + MapFocus.TRICKY_PAST_METERS
      }
    val course = trickyAhead?.let { courses?.getOrNull(it.value.leg) }
    if (trickyAhead != null && course != null) {
      val stretch = trickyAhead.value
      return MapFocus(
        MapFocusKind.TRICKY_SPOT,
        trickyAhead.index,
        stretchOfLine(
          course,
          stretch.startMeters,
          stretch.endMeters + MapFocus.TRICKY_AFTER_METERS,
        ),
        MapFocus.TRICKY_CLOSEST_METERS,
        null,
      )
    }
    val next = knots[nextIndex]
    if (next.isRoutePoint || nextIndex == 0) return MapFocus.BASE
    if (remindedIndex == nextIndex) {
      val after =
        drawnCourses.getOrNull(nextIndex)?.let {
          pointAlong(it.asSequence(), MapFocus.KNOT_AFTER_METERS)
        }
      return MapFocus(
        MapFocusKind.KNOT,
        nextIndex,
        listOfNotNull(headedFor(nextIndex), after),
        MapFocus.KNOT_CLOSEST_METERS,
        null,
      )
    }
    val passed = passedAtMillis
    val passedPlace = route.offsets[nextIndex - 1]
    // A tricky spot since the knot ends the zoom-out for good.
    val trickySincePassing = tricky.any {
      it.start >= passedPlace - 1 && it.start - place <= MapFocus.TRICKY_AHEAD_METERS
    }
    if (
      passed != null && fix.timeMillis - passed <= MapFocus.AFTER_KNOT_MILLIS && !trickySincePassing
    ) {
      return MapFocus(
        MapFocusKind.AFTER_KNOT,
        nextIndex,
        listOf(headedFor(nextIndex)),
        null,
        MapFocus.AFTER_KNOT_FARTHEST_METERS,
      )
    }
    return MapFocus.BASE
  }

  /**
   * The lines of [guidedWay] to draw on the map, see `knot-navigation` "Way to the route on the
   * map": off route the whole way ahead; on route only where it differs from the planned route,
   * more than [DIFFERS_METERS] from the leg's course, joined to the course at both ends, as the
   * route already shows the rest.
   */
  fun guidedLines(guidedWay: GuidedWay?): List<List<GeoPoint>> {
    val points = guidedWay?.pointsAhead()?.takeIf { it.size >= 2 } ?: return listOf()
    if (offRoute || arrived) return listOf(points)
    val course = courses?.getOrNull(nextIndex - 1)?.takeIf { it.size >= 2 } ?: return listOf(points)
    val dense = densified(points, DENSE_METERS)
    val far = dense.map { distanceToPolyline(it.latitude, it.longitude, course) > DIFFERS_METERS }
    val lines = mutableListOf<List<GeoPoint>>()
    var start = -1
    for (index in 0..dense.size) {
      val isFar = index < dense.size && far[index]
      if (isFar && start < 0) start = index
      if (!isFar && start >= 0) {
        val line = dense.subList((start - 1).coerceAtLeast(0), (index + 1).coerceAtMost(dense.size))
        if (line.size >= 2) lines += line.toList()
        start = -1
      }
    }
    return lines
  }

  /** How precisely to sample next; [visible] is whether the app is on screen. */
  fun precision(visible: Boolean): Precision =
    when {
      visible -> Precision.LIVE
      arrived -> Precision.COARSE
      offRoute || wrongWay || confirming -> Precision.FINE
      lastFix.accuracyMeters > FINE_DISTANCE_METERS -> Precision.FINE
      distanceTo(lastFix, headedFor(nextIndex)) <= FINE_DISTANCE_METERS -> Precision.FINE
      else -> Precision.COARSE
    }

  /** Follows the route with [fix]; [visible] is whether the app is on screen. */
  fun update(fix: Fix, visible: Boolean = false): NavigationUpdate {
    if (arrived) return NavigationUpdate(this, listOf(), precision(visible))
    // A position delivered again, e.g. by iOS after a changed request, counts only once, see
    // `knot-navigation` "Off route confirmed by a precise position".
    if (fix.timeMillis <= lastFix.timeMillis)
      return NavigationUpdate(this, listOf(), precision(visible))
    if (fix.accuracyMeters > PRECISE_ACCURACY_METERS) return updateCoarse(fix, visible)
    val notices = mutableListOf<Notice>()
    var confirming = false
    var navigation =
      if (passedKnot != null && distanceToNearestNode(fix, passedKnot) > REMINDER_METERS)
        copy(passedKnot = null)
      else this

    val passedIndex = navigation.passedRouteIndex(fix)
    if (passedIndex != null) {
      if (passedIndex == knots.lastIndex) {
        notices += arrivedNotice()
        navigation =
          navigation.copy(
            nextIndex = passedIndex,
            passedKnot = knots[passedIndex],
            passedAtMillis = null,
            arrived = true,
            offRoute = false,
            directionWatch = null,
          )
      } else {
        val newNext = passedIndex + 1
        notices += if (offRoute) backOnRouteNotice(newNext) else nextNotice(newNext)
        val place = route.offsets[passedIndex]
        val pace = paceAt(place, fix)
        val ferries = ferryWatch?.atKnot(passedIndex, fix.timeMillis, pace.metersPerSecond)
        ferries?.let { notices += it.second }
        navigation =
          navigation.copy(
            nextIndex = newNext,
            offRoute = false,
            offRouteKnot = null,
            passedKnot = knots[passedIndex],
            passedAtMillis = fix.timeMillis,
            remindedIndex = if (closeToPrevious(newNext)) newNext else remindedIndex,
            ferryWatch = ferries?.first ?: ferryWatch,
            pace = pace,
            place = place,
            progressLine =
              route.line(place, newNext, pace.metersPerSecond, fix.timeMillis, localTime, language),
            linePlace = place,
            guided = null,
            directionWatch = null,
          )
      }
    } else if (
      nextIndex == 0 &&
        offRoute &&
        courses != null &&
        distanceTo(fix, headedFor(0)) > REMINDER_METERS
    ) {
      // On the way to the first knot, only following a leg's course joins the route. Close to the
      // first knot, its course counts as the way there, see "Distance to the first knot".
      val leg = legOnCourse(fix, courses, ON_COURSE_METERS)
      val joining = this.joining
      navigation =
        when {
          leg == null -> navigation.copy(joining = null)
          joining == null || joining.leg != leg ->
            navigation.copy(joining = Joining(leg, route.placeOf(fix, leg + 1)))
          abs(route.placeOf(fix, leg + 1) - joining.place) < JOIN_ALONG_METERS -> navigation
          else -> {
            val newNext = leg + 1
            notices += backOnRouteNotice(newNext)
            val place = route.placeOf(fix, newNext)
            navigation.copy(
              nextIndex = newNext,
              offRoute = false,
              offRouteKnot = null,
              place = place,
              progressLine =
                route.line(
                  place,
                  newNext,
                  pace.metersPerSecond,
                  fix.timeMillis,
                  localTime,
                  language,
                ),
              linePlace = place,
              guided = null,
              directionWatch = null,
              joining = null,
            )
          }
        }
    } else {
      val offRouteKnot = enteredOffRouteKnot(fix)
      val distanceToCourse = distanceToCourse(fix)
      when {
        offRouteKnot == null && onStraightLine(fix) -> {
          // A straight line cannot be followed, see `knot-navigation` "A straight line".
          if (offRoute) notices += backOnRouteNotice(nextIndex)
          navigation = navigation.copy(offRoute = false, offRouteKnot = null, guided = null)
        }
        offRouteKnot != null -> {
          if (!offRoute) notices += offRouteNotice(nextIndex, fix)
          navigation =
            navigation.copy(offRoute = true, offRouteKnot = offRouteKnot, directionWatch = null)
        }
        // Off route by distance is confirmed with a fix of CONFIRM_ACCURACY_METERS or better.
        distanceToCourse != null &&
          distanceToCourse > OFF_COURSE_METERS &&
          !offRoute &&
          fix.accuracyMeters > CONFIRM_ACCURACY_METERS -> confirming = true
        distanceToCourse != null && distanceToCourse > OFF_COURSE_METERS -> {
          if (!offRoute) notices += offRouteNotice(nextIndex, fix)
          navigation = navigation.copy(offRoute = true, directionWatch = null)
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
        distanceTo(fix, headedFor(navigation.nextIndex)) <= REMINDER_METERS
    ) {
      notices += navigation.reminderNotice()
      navigation = navigation.copy(remindedIndex = navigation.nextIndex)
    }

    val straight = passedIndex == null && navigation.onStraightLine(fix)
    if (passedIndex == null && !navigation.offRoute) {
      val place = route.placeOf(fix, navigation.nextIndex)
      if (!straight) {
        val (watched, directionNotice) = navigation.watchingDirection(place, fix)
        directionNotice?.let { notices += it }
        navigation = watched
      }
      val pace = paceAt(place, fix)
      navigation =
        navigation.copy(
          place = place,
          pace = pace,
          ferryWatch =
            navigation.ferryWatch?.renewed(
              place,
              fix.timeMillis,
              navigation.nextIndex,
              pace.metersPerSecond,
            ),
        )
      // Also back on route: the route's progress line again, see "Direction outside the app".
      // Before the first knot, the place stands still while the distance to it changes.
      if (straight && navigation.knots[navigation.nextIndex].isRoutePoint) {
        // The distance and direction to the point, as its line cannot be followed.
        val target = navigation.headedFor(navigation.nextIndex)
        val way = wayTo(fix, target, language)
        navigation =
          navigation.copy(
            progressLine =
              texts.wayTo(way.distance, way.direction, navigation.spotAt(navigation.nextIndex))
          )
      } else if (offRoute || navigation.nextIndex == 0 || abs(place - linePlace) >= LINE_METERS) {
        navigation = navigation.copy(progressLine = navigation.routeLine(fix), linePlace = place)
      }
    }
    if (navigation.offRoute) {
      val (guided, guidance) = navigation.guide(fix, startedNow = !offRoute)
      guidance?.let { notices += it }
      navigation = guided
    }
    navigation =
      navigation.copy(lastPreciseFix = fix, lastFix = fix, confirming = confirming).withRejoin(fix)
    return NavigationUpdate(navigation, notices, navigation.precision(visible))
  }

  /**
   * This navigation with the rider at [place] and [fix], and what to tell them, see
   * `knot-navigation` "Riding the wrong way". [Navigation.place] is still the place before [fix].
   */
  private fun watchingDirection(place: Double, fix: Fix): Pair<Navigation, Notice?> {
    val course = courses?.getOrNull(nextIndex - 1)?.takeIf { it.size >= 2 } ?: return this to null
    val watch = directionWatch ?: return copy(directionWatch = DirectionWatch(place, fix)) to null
    // Where a course doubles back past itself, the nearest place can jump: no riding, start anew.
    val moved = lastPreciseFix?.let { distanceBetween(it, fix) } ?: 0.0
    if (abs(place - this.place) > moved * PLACE_JUMP_RATIO + PLACE_JUMP_METERS) {
      return copy(
        directionWatch = watch.copy(furthestPlace = place, furthestFix = fix, lowestPlace = place)
      ) to null
    }
    if (distanceToPolyline(fix.latitude, fix.longitude, course) > ON_COURSE_METERS)
      return this to null
    if (watch.wrongWay) {
      if (place >= watch.lowestPlace + TURNED_AROUND_METERS) {
        return copy(directionWatch = DirectionWatch(place, fix)) to backOnRouteNotice(nextIndex)
      }
      return copy(directionWatch = watch.copy(lowestPlace = min(watch.lowestPlace, place))) to null
    }
    if (place > watch.furthestPlace)
      return copy(directionWatch = DirectionWatch(place, fix)) to null
    val wrongWay =
      place <= watch.furthestPlace - WRONG_WAY_METERS &&
        distanceBetween(watch.furthestFix, fix) >= WRONG_WAY_MOVED_METERS
    if (!wrongWay) return this to null
    return copy(directionWatch = watch.copy(wrongWay = true, lowestPlace = place)) to
      wrongWayNotice()
  }

  private fun distanceBetween(from: Fix, to: Fix): Double =
    distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)

  private fun wrongWayNotice(): Notice =
    notice(
      NoticeKind.WRONG_WAY,
      texts.wrongWay(spotAt(nextIndex)),
      speech.wrongWay(spotAt(nextIndex).spoken()),
    )

  /** The pace with the rider at [place] at [fix]; time off route does not count. */
  private fun paceAt(place: Double, fix: Fix): Pace =
    if (offRoute) pace.restartedAt(place, fix.timeMillis)
    else pace.at(place, fix.timeMillis, route.assumedMetersPerSecond)

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
   * returning to the start is not taken for arriving. Neither is a later visit of [passedKnot], so
   * riding around at a knot does not skip ahead to it.
   */
  private fun passedRouteIndex(fix: Fix): Int? {
    val roundTrip = knots.size > 2 && knots.first().id == knots.last().id
    val entered =
      (nextIndex..knots.lastIndex).firstOrNull { index ->
        !(roundTrip && index == knots.lastIndex && index != nextIndex) &&
          knots[index].id != passedKnot?.id &&
          entered(fix, knots[index])
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
    return knot.reachPositions.any { position ->
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
      .flatMap { listOf(courses[it]) + signpostCourses.getOrElse(it) { listOf() } }
      .minOfOrNull { distanceToPolyline(fix.latitude, fix.longitude, it) }
  }

  /**
   * Whether the rider is on the way to or from a route point joined by a straight line only, near
   * enough to it that being away from the line is no detour, see `knot-navigation` "A straight
   * line".
   */
  private fun onStraightLine(fix: Fix): Boolean {
    if (nextIndex == 0) return false
    val toward = knots[nextIndex].routePoint?.takeIf { it.straight }
    val from = knots[nextIndex - 1].routePoint?.takeIf { it.straight }
    val point = if (toward != null) knots[nextIndex] else knots[nextIndex - 1]
    val routePoint = toward ?: from ?: return false
    val line = lengthOf(routePoint.way)
    val position = GeoPoint(point.latitude, point.longitude)
    return distanceTo(fix, position) <= line + OFF_COURSE_METERS
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
    val next = spotAt(index)
    val after = knots.getOrNull(index + 1)?.let { spotAt(index + 1) }
    val towards = towardsOnLeg(index - 1)
    if (towards != null) {
      // Along the course to the route point at [index], see "Guidance to a route point off the
      // network".
      return notice(
        NoticeKind.NEXT,
        texts.towardsThen(towards.ref, texts.name(next)),
        speech.towardsThen(spoken(towards.ref), speech.name(next.spoken())),
      )
    }
    val towardsAfter = towardsOnLeg(index)
    if (towardsAfter != null && after != null) {
      return notice(
        NoticeKind.NEXT,
        texts.next(next.ref, texts.towardsKnot(towardsAfter.ref)),
        speech.next(next.spoken().ref, speech.towardsKnot(spoken(towardsAfter.ref))),
      )
    }
    return notice(
      NoticeKind.NEXT,
      texts.next(next, after),
      speech.next(next.spoken(), after?.spoken()),
    )
  }

  private fun reminderNotice(): Notice {
    val next = spotAt(nextIndex)
    val after = knots.getOrNull(nextIndex + 1)?.let { spotAt(nextIndex + 1) }
    val towardsAfter = towardsOnLeg(nextIndex)
    if (towardsAfter != null && after != null && next.role == null) {
      return notice(
        NoticeKind.REMINDER,
        texts.ahead(next.ref, texts.towardsKnot(towardsAfter.ref)),
        speech.ahead(next.spoken().ref, speech.towardsKnot(spoken(towardsAfter.ref))),
      )
    }
    return notice(
      NoticeKind.REMINDER,
      texts.ahead(next, after),
      speech.ahead(next.spoken(), after?.spoken()),
    )
  }

  /** E.g. `Off route, next 45, 1.7 km to the north-east`, see `knot-navigation` "Off route". */
  private fun offRouteNotice(index: Int, fix: Fix): Notice {
    return notice(
      NoticeKind.OFF_ROUTE,
      texts.offRoute(spotAt(index), wayTo(fix, headedFor(index), language).text),
      speech.offRoute(spotAt(index).spoken(), spokenWay(fix, headedFor(index))),
    )
  }

  /** E.g. `528 meters to the south-west`: the voice would read `m` as a letter. */
  private fun spokenWay(fix: Fix, point: GeoPoint): String =
    speech.way(
      speech.spokenDistance(distanceTo(fix, point).roundToInt()),
      compass(bearing(fix, point), speechLanguage),
    )

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
    val target = headedFor(nextIndex)
    val meters = distanceTo(fix, target)
    val bearing = bearing(fix, target)
    val way = wayTo(fix, target, language)
    val last = guided
    val notice =
      if (startedNow || last == null) null
      else if (
        meters <= last.noticeMeters - GUIDANCE_METERS ||
          angleBetween(bearing, last.noticeBearing) > GUIDANCE_DEGREES
      )
        notice(
          NoticeKind.GUIDANCE,
          texts.guidance(spotAt(nextIndex), way.text),
          speech.guidance(spotAt(nextIndex).spoken(), spokenWay(fix, target)),
        )
      else null
    val told = startedNow || last == null || notice != null
    val renewLine = last == null || abs(meters - last.lineMeters) >= LINE_METERS
    val guided =
      Guided(
        noticeMeters = if (told) meters else last.noticeMeters,
        noticeBearing = if (told) bearing else last.noticeBearing,
        lineMeters = if (renewLine) meters else last.lineMeters,
      )
    val line =
      if (renewLine) texts.wayTo(way.distance, way.direction, spotAt(nextIndex)) else progressLine
    return copy(guided = guided, progressLine = line) to notice
  }

  private fun backOnRouteNotice(index: Int): Notice =
    notice(
      NoticeKind.BACK_ON_ROUTE,
      texts.backOnRoute(spotAt(index)),
      speech.backOnRoute(spotAt(index).spoken()),
    )

  /**
   * The knot the course of leg [leg] leads to while its way to a route point follows it, `null` if
   * the leg has no such way or follows the course for less than [TOWARDS_MIN_METERS].
   */
  private fun towardsOnLeg(leg: Int): Knot? {
    val turns = legTurns.getOrNull(leg) ?: return null
    val turnOff = turns.turnOff ?: return null
    val alongCourse = turnOff.meters - (turns.join?.meters ?: 0.0)
    return turnOff.towards.takeIf { alongCourse >= TOWARDS_MIN_METERS }
  }

  /**
   * The current leg's way to or from a route point off the network, with how far along its course
   * the rider is, or `null` off route, at the end, or on a leg without one, see `knot-navigation`
   * "Guidance to a route point off the network".
   */
  internal fun networkLeg(): NetworkLeg? {
    if (offRoute || arrived || nextIndex == 0) return null
    val leg = nextIndex - 1
    val turns = legTurns.getOrNull(leg) ?: return null
    val length = route.legLength(leg)
    val share = if (length > 0) ((place - route.offsets[leg]) / length).coerceIn(0.0, 1.0) else 0.0
    return NetworkLeg(leg, share * turns.courseMeters, turns, pointOf(knots[nextIndex]))
  }

  /**
   * Where the guided way leads while the rider is on the way of a route point off the network: the
   * place where it joins the course, or after the turn-off the route point; else `null`.
   */
  internal fun offNetworkTarget(): GeoPoint? = networkLeg()?.target

  private fun arrivedNotice(): Notice = notice(NoticeKind.ARRIVED, texts.arrived, speech.arrived)

  private fun notice(kind: NoticeKind, text: String, speech: String) = Notice(kind, text, speech)

  private fun copy(
    nextIndex: Int = this.nextIndex,
    offRoute: Boolean = this.offRoute,
    arrived: Boolean = this.arrived,
    remindedIndex: Int? = this.remindedIndex,
    offRouteKnot: Knot? = this.offRouteKnot,
    passedKnot: Knot? = this.passedKnot,
    confirming: Boolean = this.confirming,
    lastPreciseFix: Fix? = this.lastPreciseFix,
    lastFix: Fix = this.lastFix,
    ferryWatch: FerryWatch? = this.ferryWatch,
    pace: Pace = this.pace,
    place: Double = this.place,
    progressLine: String = this.progressLine,
    linePlace: Double = this.linePlace,
    guided: Guided? = this.guided,
    directionWatch: DirectionWatch? = this.directionWatch,
    joinedRoute: Boolean = this.joinedRoute,
    joining: Joining? = this.joining,
    passedAtMillis: Long? = this.passedAtMillis,
    language: Language = this.language,
    speechLanguage: Language = this.speechLanguage,
    rejoin: RouteProgress.Rejoin? = this.rejoin,
  ) =
    Navigation(
      knots,
      courses,
      signpostCourses,
      drawnCourses,
      locator,
      routeKnotIds,
      nextIndex,
      offRoute,
      arrived,
      remindedIndex,
      offRouteKnot,
      passedKnot,
      confirming,
      lastPreciseFix,
      lastFix,
      ferryWatch,
      route,
      localTime,
      pace,
      place,
      progressLine,
      linePlace,
      guided,
      directionWatch,
      joinedRoute || !offRoute,
      joining,
      legTurns,
      tricky,
      passedAtMillis,
      language,
      speechLanguage,
      rejoin,
    )

  companion object {

    /**
     * Off route within this distance of a course, the rider is on a path of the knot network, see
     * `knot-navigation` "Direction to the route".
     */
    const val ON_NETWORK_METERS = 30.0

    /** How far before and after a knot its turn is measured, see "Turn at the next knot". */
    const val TURN_METERS = 40.0

    /** Shorter than this before or after a knot, its turn cannot be told. */
    const val TURN_MIN_METERS = 2.0

    /**
     * Along a course for less than this, a way to a route point is not said to go towards a knot,
     * see `knot-navigation` "Guidance to a route point off the network".
     */
    const val TOWARDS_MIN_METERS = 30.0

    /** On route, a guided way farther than this from the course is drawn, see [guidedLines]. */
    const val DIFFERS_METERS = 20.0

    /** How finely [guidedLines] compares the guided way with the course. */
    private const val DENSE_METERS = 10.0

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

    /**
     * Off route by the distance to the course needs a fix this accurate, see `knot-navigation` "Off
     * route confirmed by a precise position".
     */
    const val CONFIRM_ACCURACY_METERS = 20.0

    /** Within this distance of the next knot, the apps sample with [Precision.FINE]. */
    const val FINE_DISTANCE_METERS = 400.0

    /** Back along the leg from the furthest place, see `knot-navigation` "Riding the wrong way". */
    private const val WRONG_WAY_METERS = 150.0

    /** In a straight line from the furthest place, so a jump of the place alone does not count. */
    private const val WRONG_WAY_MOVED_METERS = 100.0

    /** Along the route again after riding the wrong way, so the rider has turned around. */
    private const val TURNED_AROUND_METERS = 50.0

    /**
     * A change of the place by more than this times the distance moved, plus the next, is a jump.
     */
    private const val PLACE_JUMP_RATIO = 3.0

    private const val PLACE_JUMP_METERS = 50.0

    /** Off route, a guidance notice when the rider got this much closer, see "Off route". */
    private const val GUIDANCE_METERS = 500.0

    /** Off route, a guidance notice when the direction turned by more than this. */
    private const val GUIDANCE_DEGREES = 45.0

    /** The line outside the app is renewed when the distance changed by this much. */
    private const val LINE_METERS = 100.0

    /** The assumed pace applies until the rider covered this much of the route. */
    private const val OWN_PACE_AFTER_METERS = 2000.0

    /** At the start, legs this much further than the nearest count as just as near. */
    private const val SAME_LEG_METERS = 10.0

    /** How far a rider follows a leg's course before joining it on the way to the start. */
    private const val JOIN_ALONG_METERS = 50.0

    /**
     * The leg whose course is nearest to [fix] within [maxMeters], or `null`: the earliest of the
     * legs about as near, as on a stretch used both ways a rounding difference must not put the
     * rider on the way back, see `knot-navigation` "Leg ahead on the map".
     */
    private fun legOnCourse(fix: Fix, courses: List<List<GeoPoint>>, maxMeters: Double): Int? {
      val distances =
        courses
          .withIndex()
          .map { (index, course) ->
            index to distanceToPolyline(fix.latitude, fix.longitude, course)
          }
          .filter { (_, distance) -> distance <= maxMeters }
      val nearest = distances.minOfOrNull { (_, distance) -> distance } ?: return null
      return distances.first { (_, distance) -> distance <= nearest + SAME_LEG_METERS }.first
    }

    /** The rider's pace is measured over this much of the route, see "Ferry arrival estimate". */
    private const val PACE_WINDOW_METERS = 5000.0

    /** The pace is measured in stretches at least this long, so GPS noise does not count. */
    private const val PACE_STRETCH_METERS = 100.0

    /** The assumed pace is this many times the pace below which the rider stands still. */
    private const val STANDING_STILL_RATIO = 3.0

    /** Above this many meters per second, 72 km/h, the place on the route jumped. */
    private const val MAX_PLAUSIBLE_PACE = 20.0

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
      val legIndex = courses?.let { legOnCourse(fix, it, OFF_COURSE_METERS) }
      val (nextIndex, offRoute) =
        when {
          startIndex >= 0 -> startIndex + 1 to false
          legIndex != null -> legIndex + 1 to false
          // Away from the route: to its start, see `knot-navigation` "Start navigation"; without
          // courses, the app cannot tell the rider is away, so the nearest knot.
          courses != null -> 0 to true
          else -> knots.indices.minBy { distanceTo(fix, pointOf(knots[it])) } to false
        }
      val drawnCourses =
        geometry?.courses(knots)
          ?: knots.zipWithNext { from, to -> listOf(pointOf(from), pointOf(to)) }
      val headedFor = knots.mapIndexed { index, knot ->
        when {
          geometry == null -> pointOf(knot)
          index == 0 -> drawnCourses.firstOrNull()?.firstOrNull() ?: pointOf(knot)
          else ->
            drawnCourses.getOrNull(index - 1)?.let { geometry.firstReachedOn(it, knot) }
              ?: pointOf(knot)
        }
      }
      val route = RouteProgress.of(knots, graph, courses, stops, headedFor)
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
            drawnCourses = drawnCourses,
            signpostCourses =
              geometry?.let { network ->
                knots.zipWithNext { from, to -> network.signpostCoursesOf(from, to) }
              } ?: listOf(),
            locator = KnotLocator(graph.knots.values),
            routeKnotIds = knots.map { it.id }.toSet(),
            nextIndex = nextIndex,
            offRoute = offRoute,
            arrived = false,
            remindedIndex = null,
            offRouteKnot = null,
            passedKnot = null,
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
            linePlace = place,
            guided = null,
            directionWatch = null,
            joinedRoute = !offRoute,
            joining = null,
            legTurns = courses?.let { legTurnsOf(knots, graph, it) } ?: knots.drop(1).map { null },
            tricky = routeTricky(courses, route.offsets),
            passedAtMillis = null,
            language = language,
            speechLanguage = speechLanguage,
          )
          .let { if (offRoute) it.guide(fix, startedNow = true).first else it }
      val notice =
        if (offRoute) navigation.offRouteNotice(nextIndex, fix)
        else navigation.nextNotice(nextIndex)
      // The notice on starting names the next knot, so a knot close by needs no reminder.
      val reminded =
        if (distanceTo(fix, navigation.headedFor(nextIndex)) <= REMINDER_METERS)
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

private fun pointOf(knot: Knot) = GeoPoint(knot.latitude, knot.longitude)

private fun distanceTo(fix: Fix, point: GeoPoint): Double =
  distanceMeters(fix.latitude, fix.longitude, point.latitude, point.longitude)

/** The distance from [fix] to the nearest of the nodes [knot] is tagged on. */
private fun distanceToNearestNode(fix: Fix, knot: Knot): Double =
  knot.reachPositions.minOf {
    distanceMeters(fix.latitude, fix.longitude, it.latitude, it.longitude)
  }

private const val METERS_PER_DEGREE_LATITUDE = 6_371_008.8 * PI / 180

/** The distance from a position to the line between [from] and [to], in a local flat projection. */
internal fun distanceToLine(
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

/** The initial great-circle bearing from [fix] to [point], clockwise from north. */
private fun bearing(fix: Fix, point: GeoPoint): Double {
  val fromLatitude = fix.latitude * PI / 180
  val toLatitude = point.latitude * PI / 180
  val deltaLongitude = (point.longitude - fix.longitude) * PI / 180
  val y = sin(deltaLongitude) * cos(toLatitude)
  val x =
    cos(fromLatitude) * sin(toLatitude) - sin(fromLatitude) * cos(toLatitude) * cos(deltaLongitude)
  return (atan2(y, x) * 180 / PI + 360) % 360
}

/** The smaller angle between two bearings. */
private fun angleBetween(first: Double, second: Double): Double {
  val difference = abs(first - second) % 360
  return if (difference > 180) 360 - difference else difference
}

private fun wayTo(fix: Fix, point: GeoPoint, language: Language): Guidance {
  val bearing = bearing(fix, point)
  val distance = MapFeatures.formatDistance(distanceTo(fix, point).roundToInt(), language)
  val direction = compass(bearing, language)
  return Guidance(distance, bearing, direction, language.texts.way(distance, direction))
}

/**
 * Where the rider reaches [Knot]: its nodes, and for a route point also the end of its way, see
 * `knot-navigation` "A point the way does not reach".
 */
private val Knot.reachPositions: List<GeoPoint>
  get() = positions + listOfNotNull(routePoint?.wayEnd)

private fun meters(from: GeoPoint, to: GeoPoint): Double =
  distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)

/** The point [meters] along [points], or the last one if they are shorter, `null` without any. */
private fun pointAlong(points: Sequence<GeoPoint>, meters: Double): GeoPoint? {
  var previous: GeoPoint? = null
  var left = meters
  for (point in points) {
    val from = previous
    previous = point
    if (from == null) continue
    val step = meters(from, point)
    if (step >= left && step > 0) {
      val share = left / step
      return GeoPoint(
        from.latitude + (point.latitude - from.latitude) * share,
        from.longitude + (point.longitude - from.longitude) * share,
      )
    }
    left -= step
  }
  return previous
}

/** [points] with points added so none are more than [step] meters apart. */
private fun densified(points: List<GeoPoint>, step: Double): List<GeoPoint> {
  val result = mutableListOf(points.first())
  for ((from, to) in points.zipWithNext()) {
    val length = meters(from, to)
    val parts = (length / step).toInt().coerceAtLeast(1)
    for (part in 1..parts) {
      val share = part.toDouble() / parts
      result +=
        GeoPoint(
          from.latitude + (to.latitude - from.latitude) * share,
          from.longitude + (to.longitude - from.longitude) * share,
        )
    }
  }
  return result
}
