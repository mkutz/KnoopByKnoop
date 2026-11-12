package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `knot-navigation` "Start on the route nearby": when Navigate asks, and what starting
 * nearby does to the plan.
 *
 * The route is planned 06 → 02 → 28, with 02 a stop. It runs east from 06 through 05 to 02, then
 * north through 03 to 28, 1000 m between the knots.
 *
 * ```
 *                                 28  2000 m north
 *                                 |
 *                                 03  1000 m north
 *                                 |
 *   06 ------------ 05 ---------- 02
 *   0 m            1000 m       2000 m east
 * ```
 */
class StartChoiceTest {

  private val knot06 = knot(6, "06", east = 0.0)
  private val knot05 = knot(5, "05", east = 1000.0)
  private val knot02 = knot(2, "02", east = 2000.0)
  private val knot03 = knot(3, "03", east = 2000.0, north = 1000.0)
  private val knot28 = knot(28, "28", east = 2000.0, north = 2000.0)

  private val graph =
    NetworkGraph(
      knots = listOf(knot06, knot05, knot02, knot03, knot28),
      connections =
        listOf(
          Connection(6, 5, 1000),
          Connection(5, 2, 1000),
          Connection(2, 3, 1000),
          Connection(3, 28, 1000),
        ),
    )

  private val geometry = NetworkGeometry.straightLines(graph)

  private val session = PlanningSession.start(graph, listOf(knot06, knot02, knot28))

  private fun choiceAt(east: Double, north: Double, geometry: NetworkGeometry? = this.geometry) =
    StartChoice.of(session, geometry, at(east, north))

  private val PlanningSession.knots: List<Knot>
    get() = (route!!.summary as RouteSummary.Complete).knots

  @Test
  fun `Asked near the course`() {
    val choice = assertNotNull(choiceAt(east = 1700.0, north = -250.0))

    assertEquals(knot06, choice.start)
    assertEquals(1718, choice.startMeters)
    assertEquals(250, choice.nearbyMeters)
    assertTrue(choice.nearby.isRoutePoint, "a route point on the course of 05 → 02")
  }

  @Test
  fun `Starting nearby`() {
    val choice = assertNotNull(choiceAt(east = 1700.0, north = -250.0))

    val cut = session.startOnRouteAt(choice.nearby)

    assertEquals(listOf(choice.nearby, knot02, knot28), cut.waypoints)
    assertEquals(listOf(choice.nearby, knot02, knot03, knot28), cut.knots)
    assertFalse(knot06 in cut.knots)
    assertEquals(WaypointRole.START, cut.roleOf(0))
    assertEquals(WaypointRole.STOP, cut.roleOf(1))
    assertEquals(setOf(choice.nearby.id), cut.temporary, "the start nearby is for this ride")
    assertEquals(listOf(knot06, knot02, knot28), session.waypoints, "the plan still starts at 06")
  }

  @Test
  fun `Riding to the start`() {
    assertNotNull(choiceAt(east = 1700.0, north = -250.0))

    val started =
      assertNotNull(Navigation.start(session.knots, graph, geometry, fix(1700.0, -250.0)))

    assertEquals(listOf(knot06, knot02, knot28), session.waypoints, "the plan as it was")
    assertEquals(knot06, started.navigation.nextKnot)
    assertEquals("To the start, 06, 1.7 km to the west", started.notices.single().text)
  }

  @Test
  fun `Nearby at a knot`() {
    val choice = assertNotNull(choiceAt(east = 1010.0, north = -250.0))

    assertEquals(knot05, choice.nearby)
    val cut = session.startOnRouteAt(choice.nearby)
    assertEquals(listOf(knot05, knot02, knot28), cut.waypoints)
    assertEquals(setOf(knot05.id), cut.temporary)
  }

  @Test
  fun `On the course past the start`() {
    val choice = assertNotNull(choiceAt(east = 1500.0, north = 0.0))
    assertEquals(0, choice.nearbyMeters)

    val cut = session.startOnRouteAt(choice.nearby)
    val started = assertNotNull(Navigation.start(cut.knots, cut.routeGraph, null, fix(1500.0)))

    assertEquals(knot02, started.navigation.nextKnot)
  }

  @Test
  fun `At the start`() {
    assertNull(choiceAt(east = 10.0, north = -10.0))
  }

  @Test
  fun `Rough position at the start asks nothing in any direction`() {
    for ((east, north) in
      listOf(70.0 to 0.0, -70.0 to 0.0, 0.0 to 70.0, 0.0 to -70.0, 50.0 to 50.0)) {
      assertNull(choiceAt(east, north), "at $east m east, $north m north")
    }
  }

  @Test
  fun `Close to the start along the course`() {
    assertNull(choiceAt(east = 150.0, north = -100.0))
  }

  @Test
  fun `Course not much closer`() {
    assertNull(choiceAt(east = 1500.0, north = -1500.0))
  }

  @Test
  fun `Only before the first stop`() {
    // 100 m from the course of 03 → 28, after the stop 02, but far from 06 → 02.
    assertNull(choiceAt(east = 2100.0, north = 1500.0))
  }

  @Test
  fun `Without courses`() {
    val choice = assertNotNull(choiceAt(east = 1000.0, north = -300.0, geometry = null))

    assertEquals(knot05, choice.nearby)
    assertEquals(300, choice.nearbyMeters)
    assertEquals(knot05, session.startOnRouteAt(choice.nearby).waypoints.first())
  }

  @Test
  fun `Starting on a stretch used twice`() {
    // The round trip 02, 27, 02 rides 02, 28, 27, 28, 02: only the way out is before the stop 27.
    val home = knot(2, "02", east = 0.0)
    val knot27 = knot(27, "27", east = 1000.0, north = 1000.0)
    val knot28 = knot(28, "28", east = 1000.0)
    val graph =
      NetworkGraph(
        knots = listOf(home, knot28, knot27),
        connections = listOf(Connection(2, 28, 1000), Connection(28, 27, 1000)),
      )
    val geometry = NetworkGeometry.straightLines(graph)
    val roundTrip = PlanningSession.start(graph, listOf(home, knot27, home))
    val choice = assertNotNull(StartChoice.of(roundTrip, geometry, at(500.0, 0.0)))

    val cut = roundTrip.startOnRouteAt(choice.nearby)
    val started =
      assertNotNull(
        Navigation.start(
          cut.knots,
          cut.routeGraph,
          geometry.withRoutePoints(cut.routeGraph),
          fix(500.0),
        )
      )

    assertEquals(knot28, started.navigation.nextKnot)
    assertEquals(1, started.navigation.nextIndex)
  }

  @Test
  fun `the end is never the nearby start`() {
    val direct = PlanningSession.start(graph, listOf(knot06, knot02))

    assertNull(StartChoice.of(direct, geometry, at(2000.0, -100.0)))
  }

  private var time = 0L

  private fun fix(east: Double, north: Double = 0.0): Fix {
    time += 5_000
    return Fix(latitude(north), longitude(east), 5.0, time)
  }

  private companion object {
    const val ORIGIN_LATITUDE = 52.0
    const val ORIGIN_LONGITUDE = 5.0
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180

    fun latitude(north: Double) = ORIGIN_LATITUDE + north / METERS_PER_DEGREE

    fun longitude(east: Double) =
      ORIGIN_LONGITUDE + east / (METERS_PER_DEGREE * cos(ORIGIN_LATITUDE * PI / 180))

    fun at(east: Double, north: Double) = GeoPoint(latitude(north), longitude(east))

    fun knot(id: Long, ref: String, east: Double, north: Double = 0.0) =
      Knot(id, ref, NetworkType.CYCLING, latitude(north), longitude(east))
  }
}
