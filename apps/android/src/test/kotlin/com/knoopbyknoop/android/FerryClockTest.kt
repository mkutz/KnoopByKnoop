package com.knoopbyknoop.android

import com.knoopbyknoop.core.CalendarDay
import com.knoopbyknoop.core.Ferry
import com.knoopbyknoop.core.LocalMoment
import com.knoopbyknoop.core.RouteFerry
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** Covers `map-planning` "Ferry open now": "Phone in another time zone", "Closing while shown". */
class FerryClockTest {

  private val ferry = RouteFerry(Ferry(openingHours = "Apr-Oct: Mo-Su 07:00-19:00"), 0)

  @Test
  fun `phone in another time zone`() {
    val london = ZonedDateTime.of(2027, 6, 16, 18, 30, 0, 0, ZoneId.of("Europe/London"))

    val moment = ferryMoment(london.toInstant())

    assertEquals(LocalMoment(CalendarDay(2027, 6, 16), 19 * 60 + 30), moment)
    assertEquals("Closed until tomorrow 07:00", ferry.statusAt(moment)?.text)
  }

  @Test
  fun `winter time`() {
    assertEquals(
      LocalMoment(CalendarDay(2026, 12, 2), 13 * 60),
      ferryMoment(Instant.parse("2026-12-02T12:00:00Z")),
    )
  }

  @Test
  fun `closing while shown`() {
    val beforeClosing = Instant.parse("2027-06-16T16:59:20Z")
    val nextUpdate = beforeClosing.plusMillis(millisToNextMinute(beforeClosing))

    assertEquals(Instant.parse("2027-06-16T17:00:00Z"), nextUpdate)
    assertEquals("Open until 19:00", ferry.statusAt(ferryMoment(beforeClosing))?.text)
    assertEquals("Closed until tomorrow 07:00", ferry.statusAt(ferryMoment(nextUpdate))?.text)
  }
}
