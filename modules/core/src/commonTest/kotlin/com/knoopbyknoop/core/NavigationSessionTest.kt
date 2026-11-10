package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Covers the scenarios of `knot-navigation` that the loop of fixes, ways and notices decides, the
 * same for both apps.
 *
 * ```
 *   24 ------ 45 ------ 12 ------ 31      the route, west to east, knots 1000 m apart
 *   0 m      1000 m    2000 m    3000 m
 * ```
 */
class NavigationSessionTest {

  private fun latitude(north: Double) = 52.0 + north / 111_195.0

  private fun longitude(east: Double) = 5.0 + east / 68_460.0

  private fun knot(id: Long, ref: String, east: Double) =
    Knot(id, ref, NetworkType.CYCLING, latitude(0.0), longitude(east))

  private val knot24 = knot(24, "24", 0.0)
  private val knot45 = knot(45, "45", 1000.0)
  private val knot12 = knot(12, "12", 2000.0)
  private val knot31 = knot(31, "31", 3000.0)
  private val knots = listOf(knot24, knot45, knot12, knot31)

  private val graph =
    NetworkGraph(
      knots = knots,
      connections =
        listOf(Connection(24, 45, 1000), Connection(45, 12, 1000), Connection(12, 31, 1000)),
    )

  private val route = NavigationRoute(knots, graph, NetworkGeometry.straightLines(graph))

  private var time = 0L

  private fun fix(east: Double, north: Double = 0.0, accuracy: Double = 5.0): Fix {
    time += 5_000
    return Fix(latitude(north), longitude(east), accuracy, time)
  }

  private val session = NavigationSession(route)

  private fun NavigationSession.at(fix: Fix) =
    onFix(fix, visible = true, nowMillis = fix.timeMillis)

  /** Rides east along the road in steps of 40 m, collecting every notice. */
  private fun NavigationSession.ride(from: Double, to: Double): List<SessionStep> =
    (0..((to - from) / 40).toInt()).map { at(fix(from + it * 40.0)) }

  private fun wayTo(east: Double, fromNorth: Double, fromEast: Double): PathWay {
    val from = GeoPoint(latitude(fromNorth), longitude(fromEast))
    val to = GeoPoint(latitude(0.0), longitude(east))
    val meters = distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
    return PathWay(listOf(from, to), listOf(0.0, meters), listOf())
  }

  @Test
  fun `before the first fix there is nothing to show`() {
    assertNull(session.state.navigation)
    assertNull(session.state.position)
  }

  // Scenario: Arriving

  @Test
  fun `arriving speaks the arrival and ends after it`() {
    session.at(fix(2000.0))

    val steps = session.ride(2500.0, 3000.0)

    val arriving = steps.last { it.notices.isNotEmpty() }
    assertEquals("You've arrived", arriving.notices.last().text)
    assertEquals(SessionEnd.AFTER_SPEECH, arriving.end)
    assertTrue(assertNotNull(arriving.state).navigation!!.arrived)
  }

  @Test
  fun `after arriving later fixes and ways change nothing`() {
    session.at(fix(2000.0))
    session.ride(2500.0, 3000.0)
    val arrived = session.state

    val later = session.at(fix(3100.0))
    val way = session.onWay(wayTo(3000.0, fromNorth = 100.0, fromEast = 3100.0))

    assertSame(SessionStep.NOTHING, later)
    assertSame(SessionStep.NOTHING, way)
    assertSame(arrived, session.state)
  }

  // Scenario: Starting away from the route

  @Test
  fun `starting away from the route looks for a way and shows the one found`() {
    val first = session.at(fix(0.0, north = 2000.0))

    assertTrue(assertNotNull(first.state).navigation!!.offRoute)
    val search = assertNotNull(first.search)
    assertEquals(knot24.latitude, search.to.latitude, 0.0001)

    val shown = session.onWay(wayTo(0.0, fromNorth = 2000.0, fromEast = 0.0))

    assertNotNull(assertNotNull(shown.state).guidedWay)
    assertEquals(shown.state, session.state)
  }

  // Scenario: Path data arrives on the way

  @Test
  fun `without a way the search goes on at a later fix`() {
    assertNotNull(session.at(fix(0.0, north = 2000.0)).search)

    val none = session.onWay(null)
    val tooSoon = session.at(fix(0.0, north = 1950.0))
    time += WayGuide.SEARCH_INTERVAL_MILLIS
    val again = session.at(fix(0.0, north = 1900.0))

    assertNull(assertNotNull(none.state).guidedWay)
    assertNull(tooSoon.search)
    assertNotNull(again.search)
  }

  // A way found after it stopped mattering, as both apps checked before

  @Test
  fun `a way found after the rider is back on route is not shown`() {
    session.at(fix(0.0))
    session.at(fix(300.0))
    val off = session.at(fix(300.0, north = 500.0))
    assertNotNull(off.search)
    session.ride(300.0, 500.0)
    assertTrue(!session.state.navigation!!.offRoute)

    val late = session.onWay(wayTo(300.0, fromNorth = 500.0, fromEast = 300.0))

    assertSame(SessionStep.NOTHING, late)
    assertNull(session.state.guidedWay)
  }

  // Positions delivered again, see "the same position delivered again" in NavigationTest

  @Test
  fun `a position delivered again tells nothing again`() {
    session.at(fix(0.0))
    val moved = session.at(fix(300.0))

    val again = session.at(assertNotNull(moved.state).position!!)

    assertTrue(again.notices.isEmpty())
    assertEquals(moved.state!!.navigation, again.state!!.navigation)
  }

  // Scenario: Ridden track

  @Test
  fun `the track keeps the fixes in the route's network`() {
    val hiking = knots.map { it.copy(network = NetworkType.HIKING) }
    val hikingGraph = NetworkGraph(hiking, graph.connections)
    val hikingSession =
      NavigationSession(
        NavigationRoute(hiking, hikingGraph, NetworkGeometry.straightLines(hikingGraph))
      )

    hikingSession.at(fix(0.0))
    hikingSession.at(fix(15.0))
    // 40 m in 5 s is too fast on foot, though not on a bike.
    hikingSession.at(fix(55.0))

    assertEquals(NetworkType.HIKING, hikingSession.state.track.network)
    assertEquals(2, hikingSession.state.track.points.size)
  }

  @Test
  fun `a route that can't be navigated ends at once`() {
    val single = NavigationSession(NavigationRoute(listOf(knot24), graph, null))

    val step = single.at(fix(0.0))

    assertEquals(SessionEnd.NOW, step.end)
    assertSame(SessionStep.NOTHING, single.at(fix(40.0)))
  }

  @Test
  fun `the precision follows the navigation`() {
    val step = session.at(fix(500.0))

    assertEquals(
      session.state.navigation!!.update(fix(540.0), visible = true).precision,
      session.at(fix(540.0)).precision,
    )
    assertNotNull(step.precision)
  }

  // app-languages, Requirement: Language setting

  @Test
  fun `switching the language switches the texts of a running navigation`() {
    session.at(fix(850.0))

    val step = session.inLanguage(Language.DE, Language.DE)

    assertEquals(Language.DE, assertNotNull(step.state).navigation!!.language)
    assertEquals(Language.DE, session.state.navigation!!.language)
  }
}
