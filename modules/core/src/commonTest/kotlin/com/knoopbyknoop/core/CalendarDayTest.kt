package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarDayTest {

  @Test
  fun `epoch days count from 1970-01-01`() {
    assertEquals(0L, CalendarDay(1970, 1, 1).epochDay)
    assertEquals(20756L, CalendarDay(2026, 10, 30).epochDay)
    assertEquals(
      CalendarDay(2028, 2, 29),
      CalendarDay.ofEpochDay(CalendarDay(2028, 2, 29).epochDay),
    )
    assertEquals(CalendarDay(1969, 12, 31), CalendarDay(1970, 1, 1).plusDays(-1))
  }

  @Test
  fun `days of the week`() {
    assertEquals(5, CalendarDay(2026, 10, 30).dayOfWeek, "Friday")
    assertEquals(3, CalendarDay(2027, 6, 16).dayOfWeek, "Wednesday")
    assertEquals(7, CalendarDay(2027, 3, 28).dayOfWeek, "Sunday")
  }

  @Test
  fun `next day across the year end`() {
    assertEquals(CalendarDay(2027, 1, 1), CalendarDay(2026, 12, 31).plusDays(1))
  }

  @Test
  fun `easter`() {
    assertEquals(CalendarDay(2024, 3, 31), CalendarDay.easter(2024))
    assertEquals(CalendarDay(2026, 4, 5), CalendarDay.easter(2026))
    assertEquals(CalendarDay(2027, 3, 28), CalendarDay.easter(2027))
    assertEquals(CalendarDay(2038, 4, 25), CalendarDay.easter(2038))
  }

  @Test
  fun `nth weekday of a month`() {
    assertEquals(CalendarDay(2027, 3, 28), CalendarDay.nthDayOfWeek(2027, 3, 7, -1))
    assertEquals(CalendarDay(2026, 10, 25), CalendarDay.nthDayOfWeek(2026, 10, 7, -1))
    assertEquals(CalendarDay(2026, 11, 3), CalendarDay.nthDayOfWeek(2026, 11, 2, 1))
    assertEquals(CalendarDay(2027, 5, 8), CalendarDay.nthDayOfWeek(2027, 5, 6, 2))
  }

  @Test
  fun `month lengths`() {
    assertEquals(29, CalendarDay.lengthOfMonth(2028, 2))
    assertEquals(28, CalendarDay.lengthOfMonth(2027, 2))
    assertEquals(28, CalendarDay.lengthOfMonth(2100, 2))
    assertEquals(30, CalendarDay.lengthOfMonth(2027, 9))
  }
}

/** Covers `map-planning` "Ferry open now": "Public holiday", "Holiday in only some countries". */
class PublicHolidaysTest {

  @Test
  fun `christmas is a holiday everywhere`() {
    assertEquals(true, PublicHolidays.isHoliday(CalendarDay(2026, 12, 25)))
    assertEquals(true, PublicHolidays.isHoliday(CalendarDay(2027, 1, 1)))
  }

  @Test
  fun `easter monday and ascension and whit monday are holidays everywhere`() {
    assertEquals(true, PublicHolidays.isHoliday(CalendarDay(2027, 3, 29)))
    assertEquals(true, PublicHolidays.isHoliday(CalendarDay(2027, 5, 6)))
    assertEquals(true, PublicHolidays.isHoliday(CalendarDay(2027, 5, 17)))
  }

  @Test
  fun `good friday is a holiday only in nordrhein-westfalen`() {
    assertNull(PublicHolidays.isHoliday(CalendarDay(2027, 3, 26)))
  }

  @Test
  fun `holidays of only some countries cannot be told`() {
    assertNull(PublicHolidays.isHoliday(CalendarDay(2027, 4, 27)), "King's Day")
    assertNull(PublicHolidays.isHoliday(CalendarDay(2027, 7, 21)), "Belgian national day")
    assertNull(PublicHolidays.isHoliday(CalendarDay(2027, 10, 3)), "German Unity Day")
    assertNull(PublicHolidays.isHoliday(CalendarDay(2026, 12, 26)), "not in Belgium")
  }

  @Test
  fun `king's day moves to saturday when the 27th is a sunday`() {
    assertNull(PublicHolidays.isHoliday(CalendarDay(2025, 4, 26)))
    assertEquals(false, PublicHolidays.isHoliday(CalendarDay(2025, 4, 27)))
  }

  @Test
  fun `other days are no holidays`() {
    assertEquals(false, PublicHolidays.isHoliday(CalendarDay(2027, 6, 16)))
  }
}
