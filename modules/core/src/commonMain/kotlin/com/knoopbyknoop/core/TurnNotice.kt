package com.knoopbyknoop.core

import kotlin.math.roundToInt

/**
 * When and how turns are told, the same on the guided way and at turn-offs from the network, see
 * `knot-navigation` "Turn notices on the guided way" and "Guidance to a route point off the
 * network".
 */
internal object TurnNotice {
  /** From this distance before a turn, the rider is reminded of it. */
  const val NOTICE_METERS = 100.0

  /** Within this distance of a turn, the rider is told to turn now. */
  const val NOW_METERS = 40.0

  /** Distances in notices are rounded to this. */
  const val ROUNDING_METERS = 10

  /** [meters] rounded for a notice, never below 0. */
  fun rounded(meters: Double): Int =
    (meters.coerceAtLeast(0.0) / ROUNDING_METERS).roundToInt() * ROUNDING_METERS
}
