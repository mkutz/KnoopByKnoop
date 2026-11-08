package com.knoopbyknoop.core

import kotlin.math.roundToInt

/** A way to look for, off the main thread; hand the result to [WayGuide.found]. */
class WaySearch(val from: GeoPoint, val to: GeoPoint, val heading: Double?)

/**
 * What a fix brings.
 *
 * @property notices what to tell the rider, from the navigation and the guided way
 * @property search the way to look for now, if any
 */
class WayGuideStep(val notices: List<Notice>, val search: WaySearch?)

/**
 * Decides when to look for a way and follows it: back to the route while off route, see
 * `knot-navigation` "Guided way while off route" and "Leaving the guided way", and on the way of a
 * route point off the network, see "Guidance to a route point off the network" and "Guidance from a
 * route point onto the network". It stands next to [Navigation], which knows nothing of ways:
 * without one, everything works as it does without path data.
 */
class WayGuide {

  /** The way being followed. */
  var way: GuidedWay? = null
    private set

  private var searching = false
  private var lastSearchMillis: Long? = null

  /** Whether the way being followed leads back to the route, not along the way of a route point. */
  private var offRouteWay = false

  /** What was told and found on the current leg with a way off the network. */
  private var legState: LegState? = null

  /** The search for [LegState.ahead] running now, if any. */
  private var aheadSearch: LegState? = null

  /**
   * On the leg at [leg]: how far the turn-off and the join were told (0 not, 1 the reminder, 2
   * "now"), and the way from the turn-off to the route point, found ahead of it for the street.
   */
  private class LegState(val leg: Int) {
    var turnOffTold = 0
    var joinTold = 0
    var aheadSearched = false
    var ahead: PathWay? = null
  }

  /**
   * Follows the way with [fix] and says what to do.
   *
   * @param notices what the navigation had to say at this fix
   * @param network the route's network, as only cycling routes get a way
   * @param heading the riding direction, if known
   * @param nowMillis any clock going forward, for the time between searches
   */
  fun onFix(
    navigation: Navigation,
    notices: List<Notice>,
    fix: Fix,
    network: NetworkType?,
    heading: Double?,
    nowMillis: Long,
    speechLanguage: Language,
  ): WayGuideStep {
    if (navigation.arrived) {
      way = null
      legState = null
      return WayGuideStep(notices, null)
    }
    if (navigation.offRoute) {
      if (!offRouteWay) way = null
      offRouteWay = true
      return follow(navigation, notices, fix, network, heading, nowMillis, speechLanguage) {
        navigation.wayToRoute(fix)
      }
    }
    if (offRouteWay) way = null
    offRouteWay = false
    val leg = navigation.networkLeg()
    if (leg == null) {
      way = null
      legState = null
      return WayGuideStep(notices, null)
    }
    val state = legState?.takeIf { it.leg == leg.leg } ?: LegState(leg.leg).also { way = null }
    legState = state
    val turnNotices = turnNotices(navigation, leg, state, fix, speechLanguage)
    val target = leg.target
    if (target == null) {
      way = null
      return WayGuideStep(notices + turnNotices, searchAhead(leg, state, network, fix))
    }
    val ahead = state.ahead
    // From the turn-off on, the way found ahead of it is followed.
    if (way == null && ahead != null && target == leg.pointPosition) {
      way = GuidedWay.of(ahead)
      state.ahead = null
    }
    val step =
      follow(navigation, notices, fix, network, heading, nowMillis, speechLanguage) { target }
    return WayGuideStep(step.notices + turnNotices, step.search)
  }

  /** Follows [way] to [targetOf] and looks for a new one where needed. */
  private fun follow(
    navigation: Navigation,
    notices: List<Notice>,
    fix: Fix,
    network: NetworkType?,
    heading: Double?,
    nowMillis: Long,
    speechLanguage: Language,
    targetOf: () -> GeoPoint?,
  ): WayGuideStep {
    val target = targetOf()
    val update = way?.update(fix, navigation.language, speechLanguage)
    update?.let { way = it.way }
    // Left behind with no new way to look for, the old one would only mislead.
    if (update?.left == true && !withinReach(target, fix)) way = null
    val search =
      if (update == null || update.left) {
        searchFor(target, fix, network, heading, nowMillis)
      } else {
        null
      }
    // Its turn notices replace the distance and direction, see "Off route".
    val told = notices.filter { way == null || it.kind != NoticeKind.GUIDANCE }
    return WayGuideStep(told + listOfNotNull(update?.notice), search)
  }

  private fun searchFor(
    target: GeoPoint?,
    fix: Fix,
    network: NetworkType?,
    heading: Double?,
    nowMillis: Long,
  ): WaySearch? {
    if (searching || network != NetworkType.CYCLING) return null
    val last = lastSearchMillis
    if (last != null && nowMillis - last < SEARCH_INTERVAL_MILLIS) return null
    if (fix.accuracyMeters > Navigation.PRECISE_ACCURACY_METERS) return null
    if (target == null || !withinReach(target, fix)) return null
    searching = true
    lastSearchMillis = nowMillis
    return WaySearch(GeoPoint(fix.latitude, fix.longitude), target, heading)
  }

  /**
   * The way from the turn-off to the route point, looked for once [AHEAD_METERS] before the
   * turn-off, so its street can be named and its turns followed from there.
   */
  private fun searchAhead(
    leg: NetworkLeg,
    state: LegState,
    network: NetworkType?,
    fix: Fix,
  ): WaySearch? {
    val turnOff = leg.turns.turnOff ?: return null
    if (state.aheadSearched || searching || network != NetworkType.CYCLING) return null
    if (fix.accuracyMeters > Navigation.PRECISE_ACCURACY_METERS) return null
    if (turnOff.meters - leg.meters > AHEAD_METERS) return null
    state.aheadSearched = true
    searching = true
    aheadSearch = state
    return WaySearch(turnOff.position, leg.pointPosition, turnOff.heading)
  }

  private fun withinReach(target: GeoPoint?, fix: Fix): Boolean {
    target ?: return false
    return distanceMeters(fix.latitude, fix.longitude, target.latitude, target.longitude) <=
      MAX_METERS
  }

  /** The reminder and the "now" of the turn-off and the join on [leg], each told once. */
  private fun turnNotices(
    navigation: Navigation,
    leg: NetworkLeg,
    state: LegState,
    fix: Fix,
    speechLanguage: Language,
  ): List<Notice> {
    val texts = navigation.language.texts
    val speech = speechLanguage.texts
    val notices = mutableListOf<Notice>()
    leg.turns.join?.let { join ->
      val direction = join.direction ?: return@let
      val stage = stageOf(join.meters - leg.meters)
      if (stage <= state.joinTold) return@let
      state.joinTold = stage
      val meters = rounded(join.meters - leg.meters)
      val towards = join.towards?.ref
      notices +=
        Notice(
          NoticeKind.TURN,
          turn(texts, direction, stage, MapFeatures.formatDistance(meters, navigation.language))
            .let { if (towards != null) texts.ontoRoute(it, towards) else it },
          turn(speech, direction, stage, speech.spokenDistanceAhead(meters)).let {
            if (towards != null) speech.ontoRoute(it, Navigation.spoken(towards)) else it
          },
        )
    }
    leg.turns.turnOff?.let { turnOff ->
      val direction = turnOff.direction ?: return@let
      val stage = stageOf(turnOff.meters - leg.meters)
      if (stage <= state.turnOffTold) return@let
      state.turnOffTold = stage
      val meters = rounded(turnOff.meters - leg.meters)
      val street = state.ahead?.firstName
      // Without a way along paths, the distance and direction to the point have to do.
      val noWay = stage == NOW && state.ahead == null && way == null && aheadSearch == null
      val to = leg.pointPosition
      val pointMeters =
        distanceMeters(fix.latitude, fix.longitude, to.latitude, to.longitude).roundToInt()
      val bearing = (heading(GeoPoint(fix.latitude, fix.longitude), to) + 360) % 360
      fun told(texts: Texts, distance: String, language: Language, pointDistance: String) =
        turn(texts, direction, stage, distance)
          .let { if (street != null) texts.into(it, street) else it }
          .let { if (noWay) "$it, ${texts.way(pointDistance, compass(bearing, language))}" else it }
      notices +=
        Notice(
          NoticeKind.TURN,
          told(
            texts,
            MapFeatures.formatDistance(meters, navigation.language),
            navigation.language,
            MapFeatures.formatDistance(pointMeters, navigation.language),
          ),
          told(
            speech,
            speech.spokenDistanceAhead(meters),
            speechLanguage,
            speech.spokenDistance(pointMeters),
          ),
        )
    }
    return notices
  }

  private fun turn(texts: Texts, direction: TurnDirection, stage: Int, distance: String) =
    if (stage == NOW) texts.turnNow(direction) else texts.turnAhead(direction, distance)

  /** 0 far from a turn or past it, [REMINDED] within the reminder distance, [NOW] at it. */
  private fun stageOf(ahead: Double): Int =
    when {
      ahead < -NETWORK_TURN_NOW_METERS -> 0
      // A meter more, as the place along the course is measured with some error.
      ahead <= NETWORK_TURN_NOW_METERS + 1 -> NOW
      ahead <= NETWORK_TURN_NOTICE_METERS + 1 -> REMINDED
      else -> 0
    }

  private fun rounded(meters: Double): Int =
    (meters.coerceAtLeast(0.0) / ROUNDING_METERS).roundToInt() * ROUNDING_METERS

  /** The result of the last [WaySearch], `null` if there is no way; [navigation] as it is now. */
  fun found(found: PathWay?, navigation: Navigation?) {
    searching = false
    val ahead = aheadSearch
    aheadSearch = null
    if (navigation == null || navigation.arrived) return
    if (ahead != null) {
      ahead.ahead = found
      return
    }
    if (navigation.offRoute || navigation.offNetworkTarget() != null) {
      way = found?.let { GuidedWay.of(it, way) }
    }
  }

  /** Forgets everything, for a new navigation. */
  fun reset() {
    way = null
    searching = false
    lastSearchMillis = null
    offRouteWay = false
    legState = null
    aheadSearch = null
  }

  companion object {
    /** See "Leaving the guided way". */
    const val SEARCH_INTERVAL_MILLIS = 10_000L

    /** See "Guided way back to the route": further away, the straight line has to do. */
    private const val MAX_METERS = 20_000.0

    /** How far before a turn-off the way after it is looked for. */
    private const val AHEAD_METERS = 300.0

    private const val REMINDED = 1
    private const val NOW = 2
    private const val ROUNDING_METERS = 10
  }
}
