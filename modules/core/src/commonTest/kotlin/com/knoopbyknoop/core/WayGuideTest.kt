package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ```
 *   24 ------ 45 ------ 12 ------ 31      the route, west to east, knots 1000 m apart
 *   0 m      1000 m    2000 m    3000 m
 * ```
 */
class WayGuideTest {

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

  private var time = 0L
  private var millisBetweenFixes = 5_000L

  private fun fix(east: Double, north: Double = 0.0, accuracy: Double = 5.0): Fix {
    time += millisBetweenFixes
    return Fix(latitude(north), longitude(east), accuracy, time)
  }

  private fun point(east: Double, north: Double = 0.0) = GeoPoint(latitude(north), longitude(east))

  private fun start(from: Fix, geometry: NetworkGeometry? = NetworkGeometry.straightLines(graph)) =
    assertNotNull(Navigation.start(knots, graph, geometry, from)).navigation

  private val guide = WayGuide()

  private fun Navigation.step(
    fix: Fix,
    network: NetworkType? = NetworkType.CYCLING,
    target: WayGuide = guide,
  ): Pair<Navigation, WayGuideStep> {
    val update = update(fix)
    val step =
      target.onFix(
        update.navigation,
        update.notices,
        fix,
        network,
        null,
        fix.timeMillis,
        Language.EN,
      )
    return update.navigation to step
  }

  private fun wayOf(vararg points: GeoPoint, turns: List<PathTurn> = listOf()): PathWay {
    var total = 0.0
    val meters = points.mapIndexed { index, place ->
      if (index > 0) {
        val before = points[index - 1]
        total += distanceMeters(before.latitude, before.longitude, place.latitude, place.longitude)
      }
      total
    }
    return PathWay(points.toList(), meters, turns)
  }

  /** On the leg to 45, then 2 km north of it, without a way yet. */
  private fun leftTheRoute(): Navigation {
    val started = start(fix(0.0))
    val (on, _) = started.step(fix(300.0))
    val (off, _) = on.step(fix(300.0, north = 500.0))
    assertTrue(off.offRoute)
    guide.reset()
    return off
  }

  private fun waitForNextSearch() {
    time += WayGuide.SEARCH_INTERVAL_MILLIS
  }

  // Requirement: Guided way back to the route

  @Test
  fun `Starting away from the route looks for a way to the first knot`() {
    val navigation = start(fix(0.0, north = 2000.0))
    assertTrue(navigation.offRoute)
    val (_, step) = navigation.step(fix(0.0, north = 1900.0))
    val search = assertNotNull(step.search)
    assertEquals(knot24.latitude, search.to.latitude, 0.0001)
    assertEquals(knot24.longitude, search.to.longitude, 0.0001)
  }

  // Scenario: Wrong turn
  @Test
  fun `A wrong turn looks for a way to the course ahead`() {
    val started = start(fix(0.0))
    val (on, _) = started.step(fix(300.0))
    val (_, step) = on.step(fix(600.0, north = 500.0))
    val search = assertNotNull(step.search)
    assertEquals(latitude(0.0), search.to.latitude, 0.0001)
    assertEquals(longitude(600.0), search.to.longitude, 0.001)
  }

  @Test
  fun `Without courses it looks for a way to the next knot`() {
    val knot77 = Knot(77, "77", NetworkType.CYCLING, latitude(500.0), longitude(500.0))
    val withSideKnot =
      NetworkGraph(
        knots = knots + knot77,
        connections = graph.connections + Connection(45, 77, 700),
      )
    val started = assertNotNull(Navigation.start(knots, withSideKnot, null, fix(0.0))).navigation
    val (on, _) = started.step(fix(10.0))
    val (off, step) = on.step(fix(500.0, north = 500.0))
    assertTrue(off.offRoute)
    val search = assertNotNull(step.search)
    assertEquals(knot45.longitude, search.to.longitude, 0.001)
    assertEquals(knot45.latitude, search.to.latitude, 0.001)
  }

  @Test
  fun `Path data arriving on the way is used at a following fix`() {
    val navigation = leftTheRoute()
    val (first, step) = navigation.step(fix(300.0, north = 600.0))
    guide.found(null, first)
    assertNull(guide.way)
    assertNotNull(step.search)
    waitForNextSearch()
    val (second, again) = first.step(fix(300.0, north = 650.0))
    assertNotNull(again.search)
    guide.found(wayOf(point(300.0, 650.0), point(400.0, 0.0)), second)
    assertNotNull(guide.way)
  }

  // Scenario: Reaching the path data
  @Test
  fun `Beyond the path data there is no way and the search goes on until reaching it`() {
    val empty =
      PathRouter(
        object : PathSquareSource {
          override fun segments(square: PathSquare) = listOf<PathSegment>()
        }
      )
    val navigation = leftTheRoute()
    val (first, step) = navigation.step(fix(300.0, north = 600.0))
    val search = assertNotNull(step.search)
    guide.found(empty.way(search.from, search.to, search.heading), first)
    assertNull(guide.way)
    waitForNextSearch()
    assertNotNull(first.step(fix(300.0, north = 590.0)).second.search)
  }

  // Scenario: No way, no path data, far away or hiking
  @Test
  fun `A hiking route gets no way`() {
    val navigation = leftTheRoute()
    assertNull(navigation.step(fix(300.0, north = 600.0), NetworkType.HIKING).second.search)
  }

  @Test
  fun `More than 20 km from the target there is no search`() {
    val navigation = leftTheRoute()
    assertNull(navigation.step(fix(300.0, north = 25_000.0)).second.search)
  }

  @Test
  fun `Leaving a way for more than 20 km from the target ends it`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0, 200.0)), first)
    val (away, _) = first.step(fix(300.0, north = 25_000.0))
    assertNotNull(guide.way, "a single fix away keeps it")
    val (_, step) = away.step(fix(300.0, north = 25_100.0))
    assertNull(guide.way)
    assertNull(step.search)
  }

  @Test
  fun `A coarse fix starts no search`() {
    val navigation = leftTheRoute()
    assertNull(navigation.step(fix(300.0, north = 600.0, accuracy = 200.0)).second.search)
  }

  @Test
  fun `On route there is no search`() {
    assertNull(start(fix(0.0)).step(fix(300.0)).second.search)
  }

  // Scenario: Back on route with a guided way
  @Test
  fun `Back on route the guided way ends`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0)), first)
    assertNotNull(guide.way)
    val (back, _) = first.step(fix(300.0, north = 20.0))
    assertFalse(back.offRoute)
    assertNull(guide.way)
  }

  @Test
  fun `Skipping ahead on the guided way ends it`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(2000.0)), first)
    val (skipped, _) = first.step(fix(2000.0))
    assertFalse(skipped.offRoute)
    assertEquals(knot31.ref, skipped.nextKnot.ref)
    assertNull(guide.way)
  }

  @Test
  fun `A way found after coming back on route is dropped`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    val (back, _) = first.step(fix(300.0, north = 20.0))
    guide.found(wayOf(point(300.0, 20.0), point(300.0)), back)
    assertNull(guide.way)
  }

  // Requirement: Leaving the guided way

  @Test
  fun `Missing a turn looks for a new way after two fixes away`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0, 200.0)), first)
    waitForNextSearch()
    val (away, one) = first.step(fix(500.0, north = 500.0))
    assertNull(one.search)
    assertNotNull(away.step(fix(520.0, north = 500.0)).second.search)
  }

  @Test
  fun `A single inaccurate fix keeps the way`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0, 200.0)), first)
    val kept = guide.way?.way
    waitForNextSearch()
    val (away, _) = first.step(fix(500.0, north = 500.0))
    val (on, step) = away.step(fix(300.0, north = 480.0))
    assertNull(step.search)
    assertEquals(kept, guide.way?.way)
    assertTrue(on.offRoute)
  }

  // Scenario: No new way
  @Test
  fun `With no new way off route goes on without one`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0, 200.0)), first)
    waitForNextSearch()
    val (away, _) = first.step(fix(500.0, north = 500.0))
    val (further, step) = away.step(fix(520.0, north = 500.0))
    assertNotNull(step.search)
    guide.found(null, further)
    assertNull(guide.way)
  }

  @Test
  fun `The way stays until a new one is found`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0, 200.0)), first)
    waitForNextSearch()
    val (away, _) = first.step(fix(500.0, north = 500.0))
    away.step(fix(520.0, north = 500.0))
    assertNotNull(guide.way)
  }

  @Test
  fun `Searches are at most every 10 seconds and one at a time`() {
    val navigation = leftTheRoute()
    millisBetweenFixes = 1_000
    val (first, step) = navigation.step(fix(300.0, north = 600.0))
    assertNotNull(step.search)
    assertNull(first.step(fix(300.0, north = 610.0)).second.search, "still searching")
    guide.found(null, first)
    assertNull(first.step(fix(300.0, north = 620.0)).second.search, "within 10 s")
    waitForNextSearch()
    assertNotNull(first.step(fix(300.0, north = 630.0)).second.search)
  }

  @Test
  fun `A new navigation starts afresh`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0)), first)
    guide.reset()
    assertNull(guide.way)
    assertNotNull(first.step(fix(300.0, north = 610.0)).second.search)
  }

  // Requirement: Turn notices on the guided way and "Off route"

  // Scenario: No guidance notices on a guided way
  @Test
  fun `On a guided way only turn notices are said`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    val turn = PathTurn(700.0, TurnDirection.LEFT, null)
    guide.found(wayOf(point(300.0, 2000.0), point(300.0, 0.0), turns = listOf(turn)), first)
    val guidance =
      Notice(NoticeKind.GUIDANCE, "Next 45, 1.4 km to the south", "Next 45, 1.4 km to the south")
    val closer = fix(300.0, north = 1350.0)
    val step =
      guide.onFix(first, listOf(guidance), closer, NetworkType.CYCLING, null, time, Language.EN)
    assertEquals(listOf(NoticeKind.TURN), step.notices.map { it.kind })
  }

  @Test
  fun `Without a guided way distance and direction are still said`() {
    val navigation = leftTheRoute()
    val guidance = Notice(NoticeKind.GUIDANCE, "Next 45", "Next 45")
    val step =
      guide.onFix(
        navigation,
        listOf(guidance),
        fix(300.0, north = 600.0),
        NetworkType.CYCLING,
        null,
        time,
        Language.EN,
      )
    assertEquals(listOf(guidance), step.notices)
  }

  // Scenario: Turn notices on the guided way / Screen off
  @Test
  fun `With the screen off a guided way needs no more fixes`() {
    val navigation = leftTheRoute()
    val before = navigation.precision(visible = false)
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0, 200.0)), first)
    assertEquals(before, first.step(fix(300.0, north = 590.0)).first.precision(visible = false))
    assertEquals(Precision.FINE, before)
  }

  // Requirement: Guided way in the header and outside the app

  private fun onWay(): Pair<Navigation, GuidedWay> {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    val turn = PathTurn(120.0, TurnDirection.LEFT, "Weierstraße")
    guide.found(wayOf(point(300.0, 600.0), point(300.0, -1300.0), turns = listOf(turn)), first)
    return first to assertNotNull(guide.way)
  }

  @Test
  fun `In the header the turn and street and what is left show`() {
    val (navigation, way) = onWay()
    assertEquals("Off route, next 45", navigation.display.offRouteText)
    val view = way.view(Language.EN)
    assertEquals("Left in 120 m", view.turn)
    assertEquals("Weierstraße", view.street)
    assertEquals("1.9 km", view.remaining)
  }

  @Test
  fun `On the lock screen the line has the turn and what is left`() {
    assertEquals("Left in 120 m · 1.9 km", onWay().second.view(Language.EN).line)
  }

  // Scenario: Distance to the course
  @Test
  fun `The distance shown is to the route and not to the knot`() {
    val navigation = leftTheRoute()
    val (first, _) = navigation.step(fix(300.0, north = 600.0))
    guide.found(wayOf(point(300.0, 600.0), point(300.0, 300.0)), first)
    assertEquals("300 m", assertNotNull(guide.way).view(Language.EN).remaining)
  }

  @Test
  fun `Header with a screen reader says off route with the next knot and the way`() {
    val (navigation, way) = onWay()
    val fix = fix(300.0, north = 590.0)

    assertEquals(
      "Off route, next 45, Left in 120 m, Weierstraße, 1.9 km",
      navigation.header(fix, guidedWay = way).spoken,
    )
  }

  @Test
  fun `A screen reader hears the same`() {
    assertEquals("Left in 120 m, Weierstraße, 1.9 km", onWay().second.view(Language.EN).description)
  }

  // Texts

  @Test
  fun `Turn texts exist in all app languages`() {
    Language.entries.forEach { language ->
      TurnDirection.entries.forEach { direction ->
        val texts = language.texts
        assertTrue(texts.turnName(direction).isNotBlank(), "$language $direction")
        assertTrue("100 m" in texts.turnAhead(direction, "100 m"), "$language $direction")
        assertTrue(texts.turnNow(direction).isNotBlank(), "$language $direction")
      }
      assertTrue(language.texts.followTheWay.isNotBlank())
    }
    val german = Language.DE.texts
    assertEquals("Umkehren", german.turnName(TurnDirection.BACK))
    assertEquals("Jetzt umkehren", german.turnNow(TurnDirection.BACK))
    assertEquals("Omkeren", Language.NL.texts.turnName(TurnDirection.BACK))
    assertEquals("Demi-tour", Language.FR.texts.turnName(TurnDirection.BACK))
  }
}
