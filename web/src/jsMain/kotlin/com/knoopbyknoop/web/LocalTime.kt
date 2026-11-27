package com.knoopbyknoop.web

import com.knoopbyknoop.core.CalendarDay
import com.knoopbyknoop.core.LocalMoment
import kotlin.js.Date

/**
 * [millis] in the local time of the covered area, for opening hours and ferries, see `map-planning`
 * "Ferry open now". The Netherlands, Belgium and North Rhine-Westphalia share one time zone.
 */
fun nowInCoveredArea(millis: Double = Date.now()): LocalMoment {
  // Built in two steps: written as one expression, the compiler printed `new` with the wrong
  // precedence, and the browser took `formatToParts` for a constructor.
  val format: dynamic =
    js(
      "new Intl.DateTimeFormat('en-CA', {timeZone: 'Europe/Amsterdam', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23'})"
    )
  val date: dynamic = js("new Date(millis)")
  val parts: dynamic = format.formatToParts(date)
  val values = mutableMapOf<String, Int>()
  for (index in 0 until (parts.length as Int)) {
    val part = parts[index]
    (part.value as String).toIntOrNull()?.let { values[part.type as String] = it }
  }
  return LocalMoment(
    CalendarDay(values.getValue("year"), values.getValue("month"), values.getValue("day")),
    values.getValue("hour") * 60 + values.getValue("minute"),
  )
}
