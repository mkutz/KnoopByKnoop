package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Covers `knot-navigation` "Waking the screen for notices" in the core. */
class ScreenWakeTest {

  private fun notice(kind: NoticeKind, text: String) = Notice(kind, text, text)

  @Test
  fun `Reminder before a knot`() {
    assertEquals(
      "45 ahead, then 12",
      ScreenWake.alertText(
        listOf(notice(NoticeKind.REMINDER, "45 ahead, then 12")),
        visible = false,
      ),
    )
  }

  @Test
  fun `Passing a knot`() {
    assertEquals(
      "Next 12, then 31",
      ScreenWake.alertText(listOf(notice(NoticeKind.NEXT, "Next 12, then 31")), visible = false),
    )
  }

  @Test
  fun `A ferry closing`() {
    // Several notices of one update give one alert with their texts.
    val notices =
      listOf(
        notice(NoticeKind.NEXT, "Next 12, then 31"),
        notice(NoticeKind.FERRY, "The ferry Veer closes at 18:58, before you get there"),
      )

    assertEquals(
      "Next 12, then 31\nThe ferry Veer closes at 18:58, before you get there",
      ScreenWake.alertText(notices, visible = false),
    )
  }

  @Test
  fun `Off route and back`() {
    assertEquals(
      "Off route, next 28, 1.4 km to the north-west",
      ScreenWake.alertText(
        listOf(notice(NoticeKind.OFF_ROUTE, "Off route, next 28, 1.4 km to the north-west")),
        visible = false,
      ),
    )
    assertEquals(
      "Back on route, next 28",
      ScreenWake.alertText(
        listOf(notice(NoticeKind.BACK_ON_ROUTE, "Back on route, next 28")),
        visible = false,
      ),
    )
  }

  @Test
  fun `Riding on between notices`() {
    assertNull(ScreenWake.alertText(listOf(), visible = false))
  }

  @Test
  fun `App on screen`() {
    assertNull(
      ScreenWake.alertText(listOf(notice(NoticeKind.NEXT, "Next 12, then 31")), visible = true)
    )
  }
}
