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
}
