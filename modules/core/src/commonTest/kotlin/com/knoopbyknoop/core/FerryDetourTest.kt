package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `knot-navigation` "Riding around a closed ferry" and the line of "Detour on the map".
 *
 * A road from the start (1) to the river (2), the ferry across it to (3), and on to the end (4).
 * The bridge (5) lies upstream: 2–5–3 is 6.3 km longer than the ferry. Times count in minutes from
 * midnight in the ferry's local time; the cycling pace is 16 km/h.
 */
class FerryDetourTest {

  private val day = CalendarDay(2027, 6, 16)
  private val untilSeven = "Mo-Su 07:00-19:00"

  private fun localTime(millis: Long): LocalMoment {
    val minutes = (millis / 60_000).toInt()
    return LocalMoment(day.plusDays(minutes / MINUTES_PER_DAY), minutes % MINUTES_PER_DAY)
  }

  private fun millisAt(time: String): Long {
    val (hours, minutes) = time.split(":").map { it.toLong() }
    return (hours * 60 + minutes) * 60_000
  }

  private fun knot(id: Long, east: Double, north: Double = 0.0) =
    Knot(
      id,
      id.toString().padStart(2, '0'),
      NetworkType.CYCLING,
      LATITUDE + north / METERS_PER_DEGREE_LATITUDE,
      LONGITUDE + east / metersPerDegreeLongitude,
    )

  private val start = knot(1, 0.0)
  private val river = knot(2, 21_233.0)
  private val farBank = knot(3, 21_433.0)
  private val end = knot(4, 22_433.0)
  private val bridge = knot(5, 21_333.0, north = 3_000.0)
  private val downstream = knot(6, 21_333.0, north = -8_000.0)

  /**
   * The network with [ferry] from 2 to 3, the bridge unless [withBridge] is false, and with
   * [bridgeFerry] the way over 5 a ferry too, with a long way downstream over 6.
   */
  private fun graph(
    ferry: Ferry = Ferry(name = "Lülsdorf–Wesseling", openingHours = untilSeven),
    withBridge: Boolean = true,
    bridgeFerry: Ferry? = null,
  ): NetworkGraph {
    val crossing = Connection(river.id, farBank.id, 200)
    val toBridge = Connection(river.id, bridge.id, 3_300)
    val connections =
      listOfNotNull(
        Connection(start.id, river.id, 21_233),
        crossing,
        Connection(farBank.id, end.id, 1_000),
        toBridge.takeIf { withBridge },
        Connection(bridge.id, farBank.id, 3_200).takeIf { withBridge },
        Connection(river.id, downstream.id, 9_000).takeIf { bridgeFerry != null },
        Connection(downstream.id, farBank.id, 9_000).takeIf { bridgeFerry != null },
      )
    val ferries = mapOf(crossing to ferry) + listOfNotNull(bridgeFerry?.let { toBridge to it })
    return NetworkGraph(
      listOf(start, river, farBank, end, bridge, downstream),
      connections,
      ferries,
    )
  }

  private fun planned(graph: NetworkGraph): PlanningSession =
    PlanningSession.start(graph, listOf(graph.knots.getValue(1), graph.knots.getValue(4)))

  private fun detour(graph: NetworkGraph, time: String, language: Language = Language.EN) =
    FerryDetour.of(
      planned(graph),
      null,
      GeoPoint(start.latitude, start.longitude),
      millisAt(time),
      ::localTime,
      language,
    )

  private fun routeIds(session: PlanningSession?): List<Long> =
    assertNotNull((session?.route?.summary as? RouteSummary.Complete)?.knots).map { it.id }

  @Test
  fun `Ferry closed on the way`() {
    val detour = assertNotNull(detour(graph(), "18:20"))

    assertEquals(
      listOf("The ferry Lülsdorf–Wesseling closes at 19:00, before you get there around 19:40"),
      detour.warnings,
    )
    assertEquals("6.3 km", detour.extraDistance)
    assertEquals("24 min", detour.extraTime)
    assertEquals(listOf(1L, 2, 5, 3, 4), routeIds(detour.around))
  }

  @Test
  fun `Ferry closed on the way counting the way to the start`() {
    // The rider is 4.4 km before the start: 16.5 minutes more, so 19:00 becomes about 19:17.
    val session = planned(graph())
    val away = GeoPoint(start.latitude, start.longitude - 4_400 / metersPerDegreeLongitude)

    val detour = assertNotNull(FerryDetour.of(session, null, away, millisAt("17:40"), ::localTime))

    assertEquals(
      listOf("The ferry Lülsdorf–Wesseling closes at 19:00, before you get there around 19:15"),
      detour.warnings,
    )
  }

  @Test
  fun `Ferry open when the rider gets there`() {
    assertNull(detour(graph(), "10:00"))
  }

  @Test
  fun `Ferry open when the rider gets there but closing within 15 minutes after asks too`() {
    // Arrival 18:50, closing 19:00.
    assertNotNull(detour(graph(), "17:30"))
  }

  @Test
  fun `Another ferry on the detour`() {
    val bridgeFerry = Ferry(name = "Zündorf", openingHours = "Mo-Su 08:00-18:00")

    val detour = assertNotNull(detour(graph(bridgeFerry = bridgeFerry), "18:20"))

    assertEquals(2, detour.warnings.size)
    assertTrue(detour.warnings[0].startsWith("The ferry Lülsdorf–Wesseling"), detour.warnings[0])
    assertTrue(detour.warnings[1].startsWith("The ferry Zündorf"), detour.warnings[1])
    assertEquals(listOf(1L, 2, 6, 3, 4), routeIds(detour.around))
  }

  @Test
  fun `No way around`() {
    val detour = assertNotNull(detour(graph(withBridge = false), "18:20"))

    assertEquals(1, detour.warnings.size)
    assertNull(detour.around)
    assertNull(detour.extraDistance)
    assertEquals(listOf(), detour.lines)
  }

  @Test
  fun `Ferry without operating times`() {
    assertNull(detour(graph(ferry = Ferry(name = "Lülsdorf–Wesseling")), "18:20"))
  }

  @Test
  fun `the line is the way around where it parts from the route and the map shows it with the ferry`() {
    val detour = assertNotNull(detour(graph(), "18:20"))

    val points = listOf(river, bridge, farBank).map { GeoPoint(it.latitude, it.longitude) }
    assertEquals(points, detour.lines.single().distinct())
    assertEquals(river.longitude, detour.bounds.minLongitude, 1e-9)
    assertEquals(bridge.latitude, detour.bounds.maxLatitude, 1e-9)
    assertEquals(river.latitude, detour.bounds.minLatitude, 1e-9)
  }

  /** The first river with a second one 40 km further east, its closed ferry 8–9 and bridge 10. */
  private val secondFerry = Ferry(name = "Second", openingHours = "Mo-Su 07:00-19:00")
  private val second = knot(8, 62_433.0)
  private val secondFarBank = knot(9, 62_633.0)
  private val secondBridge = knot(10, 62_533.0, north = 3_000.0)
  private val last = knot(11, 63_633.0)

  private fun twoRivers(): NetworkGraph {
    val first = graph()
    val crossing = Connection(second.id, secondFarBank.id, 200)
    return NetworkGraph(
      first.knots.values + listOf(second, secondFarBank, secondBridge, last),
      first.connections +
        listOf(
          Connection(end.id, second.id, 40_000),
          crossing,
          Connection(secondFarBank.id, last.id, 1_000),
          Connection(second.id, secondBridge.id, 3_300),
          Connection(secondBridge.id, secondFarBank.id, 3_200),
        ),
      first.ferries + (crossing to secondFerry),
    )
  }

  private fun ask(session: PlanningSession, kept: Set<Ferry> = setOf(), asked: Int = 0) =
    FerryDetour.of(
      session,
      null,
      GeoPoint(start.latitude, start.longitude),
      millisAt("18:20"),
      ::localTime,
      kept = kept,
      asked = asked,
    )

  @Test
  fun `Several closed ferries`() {
    val session = PlanningSession.start(twoRivers(), listOf(start, last))

    val first = assertNotNull(ask(session))
    assertEquals(1, first.number)
    assertEquals(2, first.count)
    assertTrue(first.warnings.single().startsWith("The ferry Lülsdorf–Wesseling"))
    assertEquals("6.3 km", first.extraDistance)
    assertEquals(listOf(1L, 2, 5, 3, 4, 8, 9, 11), routeIds(first.around))

    val next = assertNotNull(ask(assertNotNull(first.around), asked = 1))
    assertEquals(2, next.number)
    assertEquals(2, next.count)
    assertTrue(next.warnings.single().startsWith("The ferry Second"), next.warnings.single())
    assertEquals("6.3 km", next.extraDistance)
    assertEquals(listOf(1L, 2, 5, 3, 4, 8, 10, 9, 11), routeIds(next.around))
    assertNull(ask(assertNotNull(next.around), asked = 2))
  }

  @Test
  fun `Keeping one ferry and riding around another`() {
    val session = PlanningSession.start(twoRivers(), listOf(start, last))
    val around = assertNotNull(ask(session)?.around)
    val next = assertNotNull(ask(around, asked = 1))

    assertNull(ask(around, kept = setOf(next.ferry), asked = 2))
    assertEquals(listOf(1L, 2, 5, 3, 4, 8, 9, 11), routeIds(around), "over the second ferry")
  }

  @Test
  fun `Map for each of several ferries`() {
    val session = PlanningSession.start(twoRivers(), listOf(start, last))
    val first = assertNotNull(ask(session))
    val next = assertNotNull(ask(assertNotNull(first.around), asked = 1))

    assertTrue(first.bounds.maxLongitude < end.longitude, "only the first river is in view")
    assertEquals(1, first.lines.size)
    assertTrue(next.bounds.minLongitude > end.longitude, "only the second river is in view")
    assertEquals(secondBridge.latitude, next.bounds.maxLatitude, 1e-9)
    assertEquals(1, next.lines.size)
  }

  @Test
  fun `A ferry with two crossings`() {
    // The ferry also lands at 7, 100 m upstream of 2: the way around takes neither crossing.
    val ferry = Ferry(name = "Horsterveer", openingHours = untilSeven)
    val otherLanding = knot(7, 21_233.0, north = 100.0)
    val crossing = Connection(river.id, farBank.id, 200)
    val otherCrossing = Connection(otherLanding.id, farBank.id, 220)
    val base = graph(ferry = ferry)
    val graph =
      NetworkGraph(
        base.knots.values + otherLanding,
        base.connections + listOf(Connection(river.id, otherLanding.id, 100), otherCrossing),
        mapOf(crossing to ferry, otherCrossing to ferry),
      )

    val detour = assertNotNull(detour(graph, "18:20"))

    assertEquals(1, detour.warnings.size)
    assertEquals(1, detour.count)
    assertEquals(listOf(1L, 2, 5, 3, 4), routeIds(detour.around))
  }

  @Test
  fun `the warnings are in the app's language`() {
    val detour = assertNotNull(detour(graph(), "18:20", Language.DE))

    assertTrue(detour.warnings.single().startsWith("Die Fähre Lülsdorf–Wesseling"))
    assertEquals("6,3 km", detour.extraDistance)
  }

  @Test
  fun `Riding around it`() {
    val session = planned(graph())
    val around = assertNotNull(detour(graph(), "18:20")?.around)

    assertEquals(listOf(1L, 5, 4), around.waypoints.map { it.id })
    assertEquals(setOf(bridge.id), around.temporary)
    assertEquals(listOf(1L, 2, 5, 3, 4), routeIds(around))
    assertEquals(listOf(1L, 4), session.waypoints.map { it.id }, "the plan stays")
    assertEquals(listOf(1L, 2, 3, 4), routeIds(session))
  }

  @Test
  fun `Temporary knots are not stops`() {
    val around = assertNotNull(detour(graph(), "18:20")?.around)

    val description = assertIs<RouteDescription.Complete>(MapFeatures.describe(around))

    assertEquals(setOf(), description.stopPositions)
    assertEquals(KnotRole.PASSED, description.knots.single { it.knot.id == bridge.id }.role)
    assertFalse("\"position\":\"1\"" in MapFeatures.waypoints(around), "no marker for 5")
  }

  @Test
  fun `the way around holds where the way back from a temporary knot over the ferry is shorter`() {
    // Upstream 2–A–B–C–3: from B the ferry is nearer than C, yet the route goes on over C.
    val a = knot(11, 21_233.0, north = 1_000.0)
    val b = knot(12, 21_333.0, north = 2_000.0)
    val c = knot(13, 21_433.0, north = 1_000.0)
    val crossing = Connection(river.id, farBank.id, 200)
    val graph =
      NetworkGraph(
        listOf(start, river, farBank, end, a, b, c),
        listOf(
          Connection(start.id, river.id, 21_233),
          crossing,
          Connection(farBank.id, end.id, 1_000),
          Connection(river.id, a.id, 1_000),
          Connection(a.id, b.id, 1_000),
          Connection(b.id, c.id, 5_000),
          Connection(c.id, farBank.id, 1_000),
        ),
        mapOf(crossing to Ferry(name = "Lülsdorf–Wesseling", openingHours = untilSeven)),
      )

    val around = assertNotNull(detour(graph, "18:20")?.around)

    assertEquals(listOf(1L, 12, 4), around.waypoints.map { it.id })
    assertEquals(listOf(1L, 2, 11, 12, 13, 3, 4), routeIds(around))
    assertEquals("7.8 km", detour(graph, "18:20")?.extraDistance)
  }

  @Test
  fun `Started nearby and riding around`() {
    // Started nearby at the river, the ferry is reached right away, after 19:00.
    val nearby = planned(graph()).startOnRouteAt(river)
    val detour =
      assertNotNull(
        FerryDetour.of(
          nearby,
          null,
          GeoPoint(river.latitude, river.longitude),
          millisAt("19:10"),
          ::localTime,
        )
      )

    val around = assertNotNull(detour.around)
    assertEquals(listOf(2L, 5, 3, 4), routeIds(around))
    assertEquals(setOf(river.id, bridge.id), around.temporary)
  }

  @Test
  fun `Editing the plan afterwards`() {
    val graph = graph()
    val session = planned(graph)
    assertNotNull(detour(graph, "18:20")?.around)
    val further = knot(7, 23_433.0)
    val longer =
      NetworkGraph(
        graph.knots.values + further,
        graph.connections + Connection(end.id, further.id, 1_000),
        graph.ferries,
      )

    val edited = PlanningSession.start(longer, session.waypoints).tap(further)

    assertEquals(listOf(1L, 2, 3, 4, 7), routeIds(edited))
    val restored = RouteSnapshot.parse(RouteSnapshot.of(edited).toJson()).restore(longer).session
    assertEquals(listOf(1L, 2, 3, 4, 7), routeIds(restored))
  }

  @Test
  fun `Routes saved with avoided ferries`() {
    val text =
      """{"network":"cycling","waypoints":[{"id":1,"ref":"01"},{"id":4,"ref":"04"}],""" +
        """"avoidedFerries":[[2,3]]}"""

    val restored = RouteSnapshot.parse(text).restore(graph()).session

    assertEquals(listOf(1L, 2, 3, 4), routeIds(restored))
  }

  @Test
  fun `the plan as made is stored without the temporary knots`() {
    val around = assertNotNull(detour(graph(), "18:20")?.around)

    assertEquals(setOf(), around.clear().temporary)
    val restored = RouteSnapshot.parse(RouteSnapshot.of(around).toJson()).restore(graph()).session
    assertEquals(setOf(), restored.temporary)
  }

  private companion object {
    const val LATITUDE = 50.8
    const val LONGITUDE = 6.9
    const val METERS_PER_DEGREE_LATITUDE = 6_371_008.8 * PI / 180
    val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(LATITUDE * PI / 180)
  }
}
