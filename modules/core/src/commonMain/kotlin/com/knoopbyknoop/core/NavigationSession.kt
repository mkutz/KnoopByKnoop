package com.knoopbyknoop.core

/**
 * A route to navigate: what [Navigation.start] needs. Without courses, [geometry] is `null`:
 * straight lines do not count as a course.
 *
 * @property stops the positions of the stops in [knots], see `knot-navigation` "Distances and times
 *   ahead"
 */
class NavigationRoute(
  val knots: List<Knot>,
  val graph: NetworkGraph,
  val geometry: NetworkGeometry?,
  val stops: Set<Int> = setOf(),
)

/**
 * What the navigation shows.
 *
 * @property navigation `null` while waiting for the first fix
 * @property position the rider's last position
 * @property track the track ridden since navigation started, see `knot-navigation` "Ridden track"
 * @property direction the riding direction from the most recent movement, `null` when standing
 * @property guidedWay the way being followed, see `knot-navigation` "Guided way back to the route"
 */
data class NavigationState(
  val navigation: Navigation?,
  val position: Fix?,
  val track: RiddenTrack = RiddenTrack(),
  val direction: Double? = null,
  val guidedWay: GuidedWay? = null,
)

/** When the app ends the navigation after a [SessionStep]. */
enum class SessionEnd {
  /** Not now. */
  NONE,

  /** Once the notices are spoken, see `knot-navigation` "Arriving". */
  AFTER_SPEECH,

  /** Right away: the navigation could not start. */
  NOW,
}

/**
 * What the app has to do after a fix, a found way or a language change.
 *
 * @property state the state to show, also outside the app; `null` if it didn't change
 * @property notices what to speak, in order
 * @property search the way to look for, off the main thread; its result goes to
 *   [NavigationSession.onWay]
 * @property precision how precisely to sample from now on; `null` to keep it
 */
class SessionStep(
  val state: NavigationState? = null,
  val notices: List<Notice> = listOf(),
  val search: WaySearch? = null,
  val precision: Precision? = null,
  val end: SessionEnd = SessionEnd.NONE,
) {
  companion object {
    /** Nothing to do. */
    val NOTHING = SessionStep()
  }
}

/**
 * One navigation from start to end, the same for both apps, see `knot-navigation`: it takes the
 * fixes and the found ways and says what to show, speak, look for and sample. The apps keep the
 * location updates, speech, threads and the notification or Live Activity. Not thread-safe: call it
 * from one thread.
 *
 * @param localTime the local time at the ferries for a moment, see [Navigation.start]
 * @param language the language of the texts
 * @param speechLanguage the language the notices are spoken in
 */
class NavigationSession(
  private var route: NavigationRoute,
  private val localTime: ((Long) -> LocalMoment)? = null,
  language: Language = Language.EN,
  speechLanguage: Language = language,
) {

  /** The language of the texts. */
  var language: Language = language
    private set

  /** The language the notices are spoken in. */
  var speechLanguage: Language = speechLanguage
    private set

  /** What the navigation shows now. */
  var state: NavigationState = NavigationState(null, null, RiddenTrack(network = network))
    private set

  private val network: NetworkType
    get() = route.graph.network ?: NetworkType.CYCLING

  private val wayGuide = WayGuide()

  /** Set once navigation ends: later fixes and ways change nothing. */
  private var ending = false

  /**
   * Follows the rider to [fix].
   *
   * @param visible whether the app is on screen, which decides the precision
   * @param nowMillis any clock going forward, for the time between way searches
   */
  fun onFix(fix: Fix, visible: Boolean, nowMillis: Long): SessionStep {
    if (ending) return SessionStep.NOTHING
    val update =
      state.navigation?.update(fix, visible)
        ?: Navigation.start(
          route.knots,
          route.graph,
          route.geometry,
          fix,
          visible,
          route.stops,
          localTime,
          language,
          speechLanguage,
        )
        ?: run {
          ending = true
          return SessionStep(end = SessionEnd.NOW)
        }
    val navigation = update.navigation
    val track = state.track
    val guide =
      wayGuide.onFix(
        navigation,
        update.notices,
        fix,
        route.graph.network,
        track.directionAt(fix),
        nowMillis,
        speechLanguage,
      )
    val newTrack = track.add(fix)
    state = NavigationState(navigation, fix, newTrack, newTrack.directionAt(fix), wayGuide.way)
    if (navigation.arrived) {
      // "You've arrived" is spoken before navigation ends, see `knot-navigation` "Arriving".
      ending = true
      return SessionStep(state, guide.notices, end = SessionEnd.AFTER_SPEECH)
    }
    return SessionStep(state, guide.notices, guide.search, update.precision)
  }

  /**
   * Takes the result of the last [SessionStep.search], `null` if there is no way. Shows it only
   * while it still matters: navigating and off route.
   */
  fun onWay(way: PathWay?): SessionStep {
    val navigation = state.navigation
    wayGuide.found(way, navigation.takeUnless { ending })
    if (ending || navigation == null || !navigation.offRoute || navigation.arrived) {
      return SessionStep.NOTHING
    }
    state = state.copy(guidedWay = wayGuide.way)
    return SessionStep(state)
  }

  /**
   * Navigates along the courses of [geometry] from now on, when navigation started before they had
   * loaded, see `knot-navigation` "Courses loaded after the start". Before the first fix the start
   * takes them; nothing is told, and nothing changes after the end or with courses already there.
   */
  fun withCourses(geometry: NetworkGeometry): SessionStep {
    if (ending || route.geometry != null) return SessionStep.NOTHING
    route = NavigationRoute(route.knots, route.graph, geometry, route.stops)
    val navigation = state.navigation ?: return SessionStep.NOTHING
    val changed = navigation.withCourses(geometry, route.graph, route.stops)
    if (changed === navigation) return SessionStep.NOTHING
    state = state.copy(navigation = changed)
    return SessionStep(state)
  }

  /** Switches the texts and the notices to new languages, see `app-languages`. */
  fun inLanguage(newLanguage: Language, newSpeechLanguage: Language): SessionStep {
    language = newLanguage
    speechLanguage = newSpeechLanguage
    val navigation = state.navigation ?: return SessionStep.NOTHING
    if (ending) return SessionStep.NOTHING
    state =
      state.copy(
        navigation = navigation.inLanguage(newLanguage, newSpeechLanguage, navigation.lastFix)
      )
    return SessionStep(state)
  }
}
