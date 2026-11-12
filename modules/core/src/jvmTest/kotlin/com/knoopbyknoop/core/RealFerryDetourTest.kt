package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * `knot-navigation` "Riding around a closed ferry" on the real data around the ferry
 * Lülsdorf–Wesseling (`real-areas/rhine-luelsdorf`). Within the cut-out the way around crosses the
 * Rhine on the ferry Mondorf–Graurheindorf, which runs until 21:00 in summer.
 */
class RealFerryDetourTest {

  private val area = RealArea.load("rhine-luelsdorf")
  private val wesselingSide = area.knot(258347663)
  private val luelsdorfSide = area.knot(2032120439)
  private val summerWednesday = CalendarDay(2027, 6, 16)

  private fun localTime(millis: Long): LocalMoment {
    val minutes = (millis / 60_000).toInt()
    return LocalMoment(
      summerWednesday.plusDays(minutes / MINUTES_PER_DAY),
      minutes % MINUTES_PER_DAY,
    )
  }

  @Test
  fun `a summer evening start rides around Lülsdorf–Wesseling over the ferry at Mondorf`() {
    val session = PlanningSession.start(area.graph, listOf(luelsdorfSide, wesselingSide))
    val position = GeoPoint(luelsdorfSide.latitude, luelsdorfSide.longitude)

    val detour =
      assertNotNull(
        FerryDetour.of(session, area.geometry, position, (18 * 60 + 50) * 60_000L, ::localTime)
      )

    assertEquals(
      listOf(
        "The ferry Personenfähre Lülsdorf-Wesseling closes at 19:00, shortly after you get there around 18:50"
      ),
      detour.warnings,
    )
    val around = assertNotNull(detour.around?.route?.summary as? RouteSummary.Complete)
    assertEquals(listOf("20", "19", "15", "14", "13", "12", "21"), around.knots.map { it.ref })
    assertEquals("21 km", detour.extraDistance)
    assertEquals("1 h 15 min", detour.extraTime)
    // The map shows both banks at Lülsdorf and the way around down to Mondorf.
    val bounds = detour.bounds
    for (knot in listOf(luelsdorfSide, wesselingSide, area.knot(258346290))) {
      assertTrue(knot.latitude in bounds.minLatitude..bounds.maxLatitude, knot.ref)
      assertTrue(knot.longitude in bounds.minLongitude..bounds.maxLongitude, knot.ref)
    }
    val line = detour.lines.single()
    assertTrue(line.size > 10, "the line follows the courses, ${line.size} points")
  }

  @Test
  fun `a winter evening way around Lülsdorf–Wesseling that would cross the ferry at Mondorf twice leaves it out too`() {
    // 20 → 14 → 15: Lülsdorf–Wesseling first, Mondorf–Graurheindorf (14 → 15) at the end. Around
    // Lülsdorf the way would cross at Mondorf and back, so one question names both ferries.
    val winterThursday = CalendarDay(2026, 11, 12)
    val winter = { millis: Long ->
      val minutes = (millis / 60_000).toInt()
      LocalMoment(winterThursday.plusDays(minutes / MINUTES_PER_DAY), minutes % MINUTES_PER_DAY)
    }
    val session =
      PlanningSession.start(
        area.graph,
        listOf(luelsdorfSide, area.knot(258346290), area.knot(248902813)),
      )
    val position = GeoPoint(luelsdorfSide.latitude, luelsdorfSide.longitude)

    val detour =
      assertNotNull(FerryDetour.of(session, area.geometry, position, 21 * 3_600_000L, winter))

    assertEquals(1, detour.count)
    assertEquals(2, detour.warnings.size)
    assertTrue(detour.warnings[1].startsWith("The ferry Autofähre Mondorf-Graurheindorf"))
    val around = (detour.around?.route?.summary as? RouteSummary.Complete)?.knots.orEmpty()
    assertTrue(around.zipWithNext().none { (a, b) -> area.graph.ferryBetween(a, b) != null })
  }
}
