package com.knoopbyknoop.android

import com.knoopbyknoop.core.CalendarDay
import com.knoopbyknoop.core.LocalMoment
import java.time.Instant
import java.time.ZoneId

/**
 * The networks' local time: the Netherlands, Belgium and Nordrhein-Westfalen share it, whatever
 * time zone the phone is set to, see `map-planning` "Ferry open now".
 */
private val ferryZone = ZoneId.of("Europe/Amsterdam")

/** [instant] in the ferries' local time. */
fun ferryMoment(instant: Instant): LocalMoment {
  val local = instant.atZone(ferryZone)
  return LocalMoment(
    CalendarDay(local.year, local.monthValue, local.dayOfMonth),
    local.hour * 60 + local.minute,
  )
}

/** Milliseconds from [instant] to the next full minute. */
fun millisToNextMinute(instant: Instant): Long = 60_000 - instant.toEpochMilli().mod(60_000L)
