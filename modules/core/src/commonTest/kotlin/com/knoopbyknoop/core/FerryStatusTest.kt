package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/** Covers `map-planning` "Ferry open now". */
class FerryStatusTest {

  private fun status(openingHours: String, day: CalendarDay, time: String): String? {
    val (hours, minutes) = time.split(":").map { it.toInt() }
    return RouteFerry(Ferry(openingHours = openingHours), 0)
      .statusAt(LocalMoment(day, hours * 60 + minutes))
      ?.text
  }

  private val seasonal = "Apr-Oct: Mo-Su 07:00-19:00"
  private val wednesday = CalendarDay(2027, 6, 16)

  @Test
  fun `open now`() {
    assertEquals("Open until 19:00", status(seasonal, wednesday, "14:00"))
  }

  @Test
  fun `opens later today`() {
    assertEquals("Closed, opens at 07:00", status(seasonal, wednesday, "06:15"))
  }

  @Test
  fun `opens tomorrow`() {
    assertEquals("Closed, opens tomorrow at 07:00", status(seasonal, wednesday, "20:00"))
  }

  @Test
  fun `closed for the season`() {
    assertEquals(
      "Closed, opens on 1 April at 07:00",
      status(seasonal, CalendarDay(2026, 12, 2), "12:00"),
    )
  }

  @Test
  fun `open past midnight`() {
    assertEquals(
      "Open until 01:00",
      status("Mo-Th 06:00-24:00; Fr-Su 06:00-01:00", CalendarDay(2027, 6, 19), "00:30"),
    )
  }

  @Test
  fun `open around the clock`() {
    assertEquals("Open", status("24/7; \"Not in ice conditions\"", wednesday, "03:00"))
  }

  @Test
  fun `public holiday`() {
    assertEquals(
      "Closed, opens at 09:00",
      status("Mo-Sa 06:00-19:00; PH,Su 09:00-19:00", CalendarDay(2026, 12, 25), "08:00"),
    )
  }

  @Test
  fun `holiday in only some countries`() {
    assertNull(status("Mo-Sa 06:00-19:00; PH,Su 09:00-19:00", CalendarDay(2027, 3, 26), "08:00"))
  }

  @Test
  fun `a holiday of only some countries is no matter when the times are the same`() {
    assertEquals(
      "Open until 19:00",
      status("Mo-Sa 06:00-19:00; PH,Su 09:00-19:00", CalendarDay(2027, 3, 28), "10:00"),
      "Easter Sunday",
    )
  }

  @Test
  fun `the next change is not shown when it depends on a holiday`() {
    assertEquals(
      "Closed",
      status("Mo-Sa 06:00-19:00; PH,Su 09:00-19:00", CalendarDay(2027, 3, 25), "20:00"),
      "the day before Good Friday",
    )
  }

  @Test
  fun `school holidays`() {
    assertNull(status("Sa,Su,PH 09:30-19:40; Mar-Sep SH 09:30-19:40", wednesday, "10:00"))
  }

  @Test
  fun `unreadable times`() {
    assertNull(status("April-October", wednesday, "10:00"))
    assertNull(status("Zie website", wednesday, "10:00"))
  }

  @Test
  fun `sunrise and sunset cannot be told`() {
    assertNull(status("Apr-Sep: sunrise-sunset", wednesday, "12:00"))
    assertEquals("Closed", status("Apr-Sep: sunrise-sunset", CalendarDay(2026, 12, 2), "12:00"))
  }

  @Test
  fun `unknown times cannot be told`() {
    assertNull(
      status(
        "Mo-Th 06:00-24:00; Fr-Su 06:00-01:00, PH unknown \"irregular opening times\"",
        CalendarDay(2026, 12, 25),
        "12:00",
      )
    )
  }

  @Test
  fun `a day with only a comment cannot be told`() {
    val times = "Mo-Fr 06:00-22:00; Dec 24,Dec 31 \"stopt om 20 uur\""
    assertNull(status(times, CalendarDay(2026, 12, 24), "12:00"))
    assertEquals("Open until 22:00", status(times, CalendarDay(2026, 12, 23), "12:00"))
  }

  @Test
  fun `a later rule replaces the times of its days`() {
    val times =
      "Mo-Fr 07:00-18:00, Sa-Su 10:00-17:00; Oct 15-Mar 15 Sa-Su closed; Dec 24-Jan 07 closed"
    assertEquals("Closed, opens tomorrow at 07:00", status(times, CalendarDay(2027, 1, 7), "12:00"))
    assertEquals(
      "Closed, opens on 8 January at 07:00",
      status(times, CalendarDay(2026, 12, 23), "20:00"),
    )
    assertEquals(
      "Closed, opens on 15 March at 07:00",
      status(times, CalendarDay(2027, 3, 13), "12:00"),
      "Saturday, the weekend still closed",
    )
  }

  @Test
  fun `additional rules add times`() {
    assertEquals(
      "Open until 18:00",
      status("Mo-Fr 07:30-08:30,11:00-18:00; Sa,Su 11:00-18:00", wednesday, "11:30"),
    )
    assertEquals(
      "Closed, opens at 11:00",
      status("Mo-Fr 07:30-08:30,11:00-18:00; Sa,Su 11:00-18:00", wednesday, "09:00"),
    )
  }

  @Test
  fun `additional rules without dates are read both ways`() {
    val times =
      "Apr-Sep: Mo-Fr 06:00-21:00, Sa 08:00-21:00; Oct-Mar Mo-Fr 06:00-20:00, Sa 08:00-20:00"
    assertNull(
      status(times, CalendarDay(2026, 10, 31), "20:30"),
      "a winter Saturday: until 21:00 by the specification, until 20:00 as meant",
    )
    assertEquals(
      "Open",
      status(times, CalendarDay(2026, 10, 31), "12:00"),
      "open in both readings, but they close at different times",
    )
    assertEquals("Open until 20:00", status(times, CalendarDay(2026, 10, 30), "12:00"), "Friday")
  }

  @Test
  fun `summer time by the last sunday of march and october`() {
    val times = "Mar Su[-1]-Oct Su[-1] Mo-Sa 06:30-21:00; Oct Su[-1]-Mar Su[-1] Mo-Sa 06:30-20:00"
    assertEquals("Open until 21:00", status(times, wednesday, "20:30"))
    assertEquals(
      "Closed, opens tomorrow at 06:30",
      status(times, CalendarDay(2026, 11, 4), "20:30"),
    )
  }

  @Test
  fun `easter with an offset`() {
    val times = "May-Sep 10:00-18:00; easter +39 days 08:00-18:00"
    assertEquals("Open until 18:00", status(times, CalendarDay(2027, 5, 6), "08:30"), "Ascension")
  }

  @Test
  fun `open until midnight`() {
    assertEquals("Open until 24:00", status("Mo-Fr 07:00-24:00", wednesday, "23:00"))
  }

  @Test
  fun `open for more than a day`() {
    assertEquals(
      "Open until 31 October at 24:00",
      status("Apr-Oct", CalendarDay(2027, 10, 20), "12:00"),
    )
  }

  @Test
  fun `the status tells whether the ferry is open`() {
    val ferry = RouteFerry(Ferry(openingHours = seasonal), 0)

    assertEquals(true, ferry.statusAt(LocalMoment(wednesday, 14 * 60))?.open)
    assertEquals(false, ferry.statusAt(LocalMoment(wednesday, 20 * 60))?.open)
  }

  @Test
  fun `ferries without times have no status`() {
    assertNull(RouteFerry(Ferry(), 0).statusAt(LocalMoment(wednesday, 600)))
  }

  @Test
  fun `the real ferry times can be read`() {
    val unreadable = RealFerryTimes.values.filter { OpeningHours.parse(it) == null }

    assertEquals(
      listOf(
        "\"(sunrise + 0.5 hour) - (sunset - 0.5 hour)\"",
        "\"2026-09-29: Vaartijden voor 2027 nog niet bekend\"",
        "\"Zie website\"",
      ),
      unreadable,
    )
  }

  @Test
  fun `the real ferry times are evaluated quickly`() {
    val readable = RealFerryTimes.values.mapNotNull { OpeningHours.parse(it) }
    val moments =
      listOf(CalendarDay(2026, 12, 2), CalendarDay(2027, 3, 26), wednesday).map {
        LocalMoment(it, 12 * 60)
      }

    val started = TimeSource.Monotonic.markNow()
    val statuses = readable.flatMap { hours -> moments.map { hours.statusAt(it) } }
    val elapsed = started.elapsedNow()

    assertTrue(statuses.filterNotNull().size > statuses.size / 2, "most have a status")
    assertTrue(
      elapsed.inWholeMilliseconds < 20L * readable.size * moments.size,
      "took $elapsed for ${statuses.size} statuses",
    )
    assertNotNull(statuses.first())
  }
}
