package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot30
import com.knoopbyknoop.core.TestNetwork.knot45
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Covers `map-planning` "Ferries on the route". */
class FerryTest {

  private val beusichem =
    Ferry(
      name = "Veerpont Beusichem",
      openingHours = "Apr-Oct: Mo-Su 07:00-19:00",
      charge = "€1.50",
    )
  private val culemborg = Ferry(name = "Pont Culemborg")

  // 12 -- 30 is crossed by one ferry, 30 -- 45 by another.
  private fun sessionWith(ferries: Map<Connection, Ferry>) =
    PlanningSession.start(
      NetworkGraph(
        graph.knots.values,
        graph.connections,
        ferries.mapKeys { (connection) -> graph.connections.single { it == connection } },
      )
    )

  private val ferry12To30 = Connection(12, 30, 2000)
  private val ferry30To45 = Connection(30, 45, 3000)

  private fun describe(session: PlanningSession) =
    MapFeatures.describe(session) as RouteDescription.Complete

  @Test
  fun `route over a ferry`() {
    val session = sessionWith(mapOf(ferry12To30 to beusichem)).tap(knot12).tap(knot30)

    val description = describe(session)

    val ferry = description.ferries.single()
    assertEquals(0, ferry.fromPosition, "between 12 and 30")
    assertEquals(listOf("12", "30"), description.knots.map { it.knot.ref })
    assertEquals("Veerpont Beusichem", ferry.title)
    assertEquals("Apr-Oct: Mo-Su 07:00-19:00", ferry.times)
    assertTrue(ferry.timesKnown)
    assertEquals("Fee: €1.50", ferry.fee)
  }

  @Test
  fun `the ferry is found in both riding directions`() {
    val session = sessionWith(mapOf(ferry12To30 to beusichem)).tap(knot30).tap(knot12)

    assertEquals(listOf(beusichem), describe(session).ferries.map { it.ferry })
  }

  @Test
  fun `ferry without known times`() {
    val ferry = RouteFerry(culemborg, 0)

    assertFalse(ferry.timesKnown)
    assertEquals("Operating times not known, check before you go", ferry.times)
    assertNull(ferry.fee)
  }

  @Test
  fun `seasonal ferry without times`() {
    assertEquals(
      "Seasonal (summer). Operating times not known, check before you go",
      RouteFerry(Ferry(seasonal = "summer"), 0).times,
    )
    assertEquals(
      "Seasonal. Operating times not known, check before you go",
      RouteFerry(Ferry(seasonal = "yes"), 0).times,
    )
    assertEquals(RouteFerry.UNKNOWN_TIMES, RouteFerry(Ferry(seasonal = "no"), 0).times)
  }

  @Test
  fun `fees`() {
    assertEquals("Free", RouteFerry(Ferry(fee = "no"), 0).fee)
    assertEquals("Fee charged", RouteFerry(Ferry(fee = "yes"), 0).fee)
    assertEquals("Fee: 1.50 EUR", RouteFerry(Ferry(fee = "yes", charge = "1.50 EUR"), 0).fee)
  }

  @Test
  fun `ferry without a name`() {
    assertEquals("Ferry", RouteFerry(Ferry(), 0).title)
  }

  @Test
  fun `several ferries`() {
    val session =
      sessionWith(mapOf(ferry12To30 to beusichem, ferry30To45 to culemborg))
        .tap(knot12)
        .tap(knot45)
        .tap(knot30)

    val description = describe(session)

    assertEquals(listOf("12", "30", "45"), description.knots.map { it.knot.ref })
    assertEquals(listOf(beusichem, culemborg), description.ferries.map { it.ferry })
    assertEquals(listOf(0, 1), description.ferries.map { it.fromPosition })
  }

  @Test
  fun `where the ferries are on the route`() {
    val session =
      sessionWith(mapOf(ferry12To30 to beusichem, ferry30To45 to culemborg))
        .tap(knot12)
        .tap(knot45)
        .tap(knot30)

    assertEquals(listOf("12 → 30", "30 → 45"), describe(session).ferries.map { it.place })
  }

  @Test
  fun `route without ferries`() {
    val session = sessionWith(mapOf()).tap(knot12).tap(knot45)

    assertEquals(listOf(), describe(session).ferries)
  }
}
