package com.knoopbyknoop.android

import com.knoopbyknoop.core.NavigationRoute
import com.knoopbyknoop.core.NavigationState
import com.knoopbyknoop.core.NetworkGeometry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

  /** Whether the phone is locked with the app over the lock screen. */
  val overLockScreen = MutableStateFlow(false)

  /**
   * The network's courses, once they have loaded after navigation started without them, see
   * `knot-navigation` "Courses loaded after the start"; `null` until then.
   */
  val lateCourses = MutableStateFlow<NetworkGeometry?>(null)

  /** The route for the next start of [NavigationService]. */
  @Volatile var route: NavigationRoute? = null

  internal fun update(state: NavigationState?) {
    mutableState.value = state
  }
}
