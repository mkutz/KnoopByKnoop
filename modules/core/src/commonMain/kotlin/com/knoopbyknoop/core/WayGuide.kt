package com.knoopbyknoop.core

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
 * Decides when to look for a way back to the route and follows it, see `knot-navigation` "Guided
 * way while off route" and "Leaving the guided way". It stands next to [Navigation], which knows
 * nothing of ways: without one, everything works as it does without path data.
 */
class WayGuide {

  /** The way being followed. */
  var way: GuidedWay? = null
    private set

  private var searching = false
  private var lastSearchMillis: Long? = null

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
    if (!navigation.offRoute || navigation.arrived) {
      way = null
      return WayGuideStep(notices, null)
    }
    val update = way?.update(fix, navigation.language, speechLanguage)
    update?.let { way = it.way }
    // Left behind with no new way to look for, the old one would only mislead.
    if (update?.left == true && !withinReach(navigation, fix)) way = null
    val search =
      if (update == null || update.left) {
        searchFor(navigation, fix, network, heading, nowMillis)
      } else {
        null
      }
    // Its turn notices replace the distance and direction, see "Off route".
    val told = notices.filter { way == null || it.kind != NoticeKind.GUIDANCE }
    return WayGuideStep(told + listOfNotNull(update?.notice), search)
  }

  private fun searchFor(
    navigation: Navigation,
    fix: Fix,
    network: NetworkType?,
    heading: Double?,
    nowMillis: Long,
  ): WaySearch? {
    if (searching || network != NetworkType.CYCLING) return null
    val last = lastSearchMillis
    if (last != null && nowMillis - last < SEARCH_INTERVAL_MILLIS) return null
    if (fix.accuracyMeters > Navigation.PRECISE_ACCURACY_METERS) return null
    if (!withinReach(navigation, fix)) return null
    val to = navigation.wayToRoute(fix) ?: return null
    searching = true
    lastSearchMillis = nowMillis
    return WaySearch(GeoPoint(fix.latitude, fix.longitude), to, heading)
  }

  private fun withinReach(navigation: Navigation, fix: Fix): Boolean {
    val to = navigation.wayToRoute(fix) ?: return false
    return distanceMeters(fix.latitude, fix.longitude, to.latitude, to.longitude) <= MAX_METERS
  }

  /** The result of the last [WaySearch], `null` if there is no way; [navigation] as it is now. */
  fun found(found: PathWay?, navigation: Navigation?) {
    searching = false
    if (navigation == null || !navigation.offRoute || navigation.arrived) return
    way = found?.let { GuidedWay.of(it, way) }
  }

  /** Forgets everything, for a new navigation. */
  fun reset() {
    way = null
    searching = false
    lastSearchMillis = null
  }

  companion object {
    /** See "Leaving the guided way". */
    const val SEARCH_INTERVAL_MILLIS = 10_000L

    /** See "Guided way while off route": further away, the straight line has to do. */
    private const val MAX_METERS = 20_000.0
  }
}
