package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** Covers the bar on the lock screen of `knot-navigation` "Navigation with the screen off". */
class BarMarksTest {

  private fun mark(kind: ProgressMarkKind, fraction: Double, state: FerryArrivalState? = null) =
    ProgressMark(kind, "", fraction, state = state)

  private val strip =
    ProgressStrip(
      marks =
        listOf(
          mark(ProgressMarkKind.START, 0.0),
          mark(ProgressMarkKind.STOP, 0.1),
          mark(ProgressMarkKind.FERRY, 0.3),
          mark(ProgressMarkKind.STOP, 0.4),
          mark(ProgressMarkKind.KNOT, 0.5),
          mark(ProgressMarkKind.FERRY, 0.6, FerryArrivalState.WARNING),
          mark(ProgressMarkKind.STOP, 0.7),
          mark(ProgressMarkKind.END, 1.0),
        ),
      rider = 0.2,
      offRoute = false,
      description = "",
    )

  @Test
  fun `closed ferries first, then stops, then ferries, only ahead of the rider`() {
    val marks = strip.barMarks(limit = 8)

    assertEquals(
      listOf(
        BarMarkKind.CLOSED_FERRY to 0.6,
        BarMarkKind.STOP to 0.4,
        BarMarkKind.STOP to 0.7,
        BarMarkKind.FERRY to 0.3,
      ),
      marks.map { it.kind to it.fraction },
    )
  }

  @Test
  fun `at most the limit, the most important kept`() {
    val marks = strip.barMarks(limit = 2)

    assertEquals(listOf(0.6, 0.4), marks.map { it.fraction })
  }

  @Test
  fun `turn angles as the apps draw them`() {
    assertEquals(
      listOf(0.0, -45.0, -90.0, -135.0, 45.0, 90.0, 135.0, 180.0),
      TurnDirection.entries.map { it.degrees },
    )
  }
}
