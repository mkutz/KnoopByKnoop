package com.knoopbyknoop.android

import com.knoopbyknoop.core.Fix
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.Navigation
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.RiddenTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the navigation shows.
 *
 * @property navigation the navigation, or `null` while waiting for the first fix
 * @property position the rider's last position, for the map
 * @property track the track ridden since navigation started, see `knot-navigation` "Ridden track"
 * @property direction the riding direction from the most recent movement, or `null` when standing
 */
data class NavigationState(
  val navigation: Navigation?,
  val position: Fix?,
  val track: RiddenTrack = RiddenTrack(),
  val direction: Double? = null,
)

/** A route to navigate: what [Navigation.start] needs. */
class NavigationRoute(
  val knots: List<Knot>,
  val graph: NetworkGraph,
  /** The network's courses, or `null` without them: straight lines do not count as a course. */
  val geometry: NetworkGeometry?,
  /** The positions of the stops in [knots], see `knot-navigation` "Distances and times ahead". */
  val stops: Set<Int> = setOf(),
)

/**
 * The navigation shared by [NavigationService], which runs it, and the planner, which shows it, see
 * `knot-navigation`. Lives as long as the process: navigation does not survive a force-quit.
 */
object Navigating {

  private val mutableState = MutableStateFlow<NavigationState?>(null)

  /** The navigation, or `null` when not navigating. */
  val state: StateFlow<NavigationState?> = mutableState.asStateFlow()

  /**
   * Whether the app is on screen, so the service samples [com.knoopbyknoop.core.Precision.LIVE].
   */
  val visible = MutableStateFlow(false)

  /** The route for the next start of [NavigationService]. */
  @Volatile var route: NavigationRoute? = null

  internal fun update(state: NavigationState?) {
    mutableState.value = state
  }
}
