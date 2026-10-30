package com.knoopbyknoop.core

/**
 * The public holidays of the regions the networks cover: the Netherlands, Belgium and
 * Nordrhein-Westfalen. A ferry's country is not known, so a day only counts as a holiday when it is
 * one in all of them, see `map-planning` "Ferry open now".
 */
object PublicHolidays {

  /** `true` in all regions, `false` in none, `null` when the regions differ. */
  fun isHoliday(day: CalendarDay): Boolean? {
    val regions = regions(day.year).count { day in it }
    return when (regions) {
      0 -> false
      REGION_COUNT -> true
      else -> null
    }
  }

  private const val REGION_COUNT = 3

  private fun regions(year: Int): List<Set<CalendarDay>> =
    listOf(netherlands(year), belgium(year), nordrheinWestfalen(year))

  private fun netherlands(year: Int): Set<CalendarDay> {
    val easter = CalendarDay.easter(year)
    val kingsDay = CalendarDay(year, 4, 27).let { if (it.dayOfWeek == 7) it.plusDays(-1) else it }
    return setOf(
      CalendarDay(year, 1, 1),
      easter,
      easter.plusDays(1),
      kingsDay,
      CalendarDay(year, 5, 5),
      easter.plusDays(39),
      easter.plusDays(49),
      easter.plusDays(50),
      CalendarDay(year, 12, 25),
      CalendarDay(year, 12, 26),
    )
  }

  private fun belgium(year: Int): Set<CalendarDay> {
    val easter = CalendarDay.easter(year)
    return setOf(
      CalendarDay(year, 1, 1),
      easter,
      easter.plusDays(1),
      CalendarDay(year, 5, 1),
      easter.plusDays(39),
      easter.plusDays(49),
      easter.plusDays(50),
      CalendarDay(year, 7, 21),
      CalendarDay(year, 8, 15),
      CalendarDay(year, 11, 1),
      CalendarDay(year, 11, 11),
      CalendarDay(year, 12, 25),
    )
  }

  private fun nordrheinWestfalen(year: Int): Set<CalendarDay> {
    val easter = CalendarDay.easter(year)
    return setOf(
      CalendarDay(year, 1, 1),
      easter.plusDays(-2),
      easter.plusDays(1),
      CalendarDay(year, 5, 1),
      easter.plusDays(39),
      easter.plusDays(50),
      easter.plusDays(60),
      CalendarDay(year, 10, 3),
      CalendarDay(year, 11, 1),
      CalendarDay(year, 12, 25),
      CalendarDay(year, 12, 26),
    )
  }
}
