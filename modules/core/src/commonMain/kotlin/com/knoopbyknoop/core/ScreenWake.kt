package com.knoopbyknoop.core

/**
 * When a locked phone lights up during navigation, see `knot-navigation` "Waking the screen for
 * notices": with every spoken notice while the app is not on screen, silently, as the notice is the
 * sound.
 */
object ScreenWake {
  /**
   * The text to show for [notices], one line each, or `null` when nothing alerts: without a spoken
   * notice, or while the app is [visible], where the panel shows it.
   */
  fun alertText(notices: List<Notice>, visible: Boolean): String? =
    if (visible || notices.isEmpty()) null else notices.joinToString("\n") { it.text }
}
