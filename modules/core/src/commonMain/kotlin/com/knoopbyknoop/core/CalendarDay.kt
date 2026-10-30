package com.knoopbyknoop.core

/**
 * A day of the Gregorian calendar, without a time zone: the core has no date library, see
 * `show-ferry-open-now`.
 *
 * @property month 1 for January to 12 for December
 */
data class CalendarDay(val year: Int, val month: Int, val day: Int) : Comparable<CalendarDay> {

  /** Days since 1970-01-01. */
  val epochDay: Long by lazy {
    // Howard Hinnant's days_from_civil
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yearOfEra = y - era * 400
    val monthFromMarch = (month + 9) % 12
    val dayOfYear = (153 * monthFromMarch + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    era * 146097 + dayOfEra - 719468
  }

  /** 1 for Monday to 7 for Sunday. */
  val dayOfWeek: Int
    get() = ((epochDay + 3).mod(7)) + 1

  fun plusDays(days: Int): CalendarDay = ofEpochDay(epochDay + days)

  override fun compareTo(other: CalendarDay): Int = epochDay.compareTo(other.epochDay)

  companion object {

    fun ofEpochDay(epochDay: Long): CalendarDay {
      // Howard Hinnant's civil_from_days
      val z = epochDay + 719468
      val era = (if (z >= 0) z else z - 146096) / 146097
      val dayOfEra = z - era * 146097
      val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
      val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
      val monthFromMarch = (5 * dayOfYear + 2) / 153
      val day = (dayOfYear - (153 * monthFromMarch + 2) / 5 + 1).toInt()
      val month = (if (monthFromMarch < 10) monthFromMarch + 3 else monthFromMarch - 9).toInt()
      val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
      return CalendarDay(year, month, day)
    }

    fun lengthOfMonth(year: Int, month: Int): Int =
      when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4,
        6,
        9,
        11 -> 30
        else -> 31
      }

    private fun isLeapYear(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    /** Easter Sunday of [year], by the anonymous Gregorian computus. */
    fun easter(year: Int): CalendarDay {
      val a = year % 19
      val b = year / 100
      val c = year % 100
      val d = b / 4
      val e = b % 4
      val f = (b + 8) / 25
      val g = (b - f + 1) / 3
      val h = (19 * a + b - d - g + 15) % 30
      val i = c / 4
      val k = c % 4
      val l = (32 + 2 * e + 2 * i - h - k) % 7
      val m = (a + 11 * h + 22 * l) / 451
      val month = (h + l - 7 * m + 114) / 31
      val day = (h + l - 7 * m + 114) % 31 + 1
      return CalendarDay(year, month, day)
    }

    /**
     * The [nth] [dayOfWeek] (1 for Monday) of [month], counted from the end for a negative [nth],
     * e.g. -1 for the last Sunday of March.
     */
    fun nthDayOfWeek(year: Int, month: Int, dayOfWeek: Int, nth: Int): CalendarDay {
      return if (nth > 0) {
        val first = CalendarDay(year, month, 1)
        first.plusDays((dayOfWeek - first.dayOfWeek).mod(7) + 7 * (nth - 1))
      } else {
        val last = CalendarDay(year, month, lengthOfMonth(year, month))
        last.plusDays(-((last.dayOfWeek - dayOfWeek).mod(7)) - 7 * (-nth - 1))
      }
    }
  }
}
