package com.knoopbyknoop.web

import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.PlanningSession
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteFile
import com.knoopbyknoop.core.RouteSummary
import com.knoopbyknoop.web.TestNetwork.K02
import com.knoopbyknoop.web.TestNetwork.K12
import com.knoopbyknoop.web.TestNetwork.K34
import com.knoopbyknoop.web.TestNetwork.K40
import com.knoopbyknoop.web.TestNetwork.K73
import com.knoopbyknoop.web.TestNetwork.K83
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class PlannerTest {

  private val graph = TestNetwork.graph()

  private fun knot(id: Long): Knot = graph.knots.getValue(id)

  private fun Planner.clickOn(id: Long) {
    val knot = knot(id)
    click(knot.latitude, knot.longitude, 30.0, null)
  }

  private val Planner.waypoints: List<String>
    get() = session!!.waypoints.map { it.ref }

  private suspend fun TestScope.ready(planner: Planner = TestNetwork.planner(this)): Planner {
    advanceUntilIdle()
    return planner
  }

  @Test
  fun `Start, end and a stop - the route the core finds for the same waypoints`() = runTest {
    val planner = ready()

    planner.clickOn(K02)
    planner.clickOn(K73)
    planner.clickOn(K83)

    assertEquals(listOf("02", "83", "73"), planner.waypoints)
    val expected = PlanningSession.start(graph, listOf(knot(K02), knot(K83), knot(K73))).route!!
    assertEquals(
      (expected.summary as RouteSummary.Complete).knots.map { it.id },
      (planner.session!!.route!!.summary as RouteSummary.Complete).knots.map { it.id },
    )
  }

  @Test
  fun `Double click on a waypoint - the route stays`() = runTest {
    val planner = ready()
    planner.clickOn(K02)
    planner.clickOn(K83)

    planner.clickOn(K02)
    planner.clickOn(K02)

    assertEquals(listOf("02", "83"), planner.waypoints)
  }

  @Test
  fun `Reorder by dragging - the dragged waypoint takes its new place`() = runTest {
    val planner = ready()
    listOf(K02, K73, K83).forEach { planner.clickOn(it) }

    planner.move(2, 1)

    assertEquals(listOf("02", "73", "83"), planner.waypoints)
  }

  @Test
  fun `Round trip in the planner - back to the start`() = runTest {
    val planner = ready()
    planner.clickOn(K02)
    planner.clickOn(K83)

    planner.returnToStart()

    assertEquals(listOf("02", "83", "02"), planner.waypoints)
  }

  @Test
  fun `Distance and knots - the panel's summary in riding order`() = runTest {
    val planner = ready()
    planner.clickOn(K02)
    planner.clickOn(K83)

    val description = assertIs<RouteDescription.Complete>(planner.description)

    assertEquals("8.0 km", description.distance)
    assertEquals(listOf("02", "28", "29", "30", "83"), description.knots.map { it.knot.ref })
  }

  @Test
  fun `Planner in Dutch - distances in Dutch`() = runTest {
    val planner = ready(TestNetwork.planner(this, language = Language.NL))
    planner.clickOn(K02)
    planner.clickOn(K12)

    assertEquals("3,0 km", assertIs<RouteDescription.Complete>(planner.description).distance)
  }

  @Test
  fun `Route point at a café - a spot off the network joins it`() = runTest {
    val source = TestNetwork.Source()
    val planner = ready(TestNetwork.planner(this, source = source))
    planner.clickOn(K02)
    planner.clickOn(K83)
    val spot = TestNetwork.position(-5_000.0, 300.0)

    val offer = assertIs<Clicked.Offer>(planner.click(spot.latitude, spot.longitude, 30.0, null))
    planner.add(offer.offer)
    advanceUntilIdle()

    val point = planner.session!!.waypoints.single { it.isRoutePoint }
    assertEquals(spot.latitude, point.latitude, 1e-9)
    assertTrue(planner.session!!.route!!.summary is RouteSummary.Complete)
    assertTrue(source.squareLoads > 0, "path data around the route is asked for")
  }

  @Test
  fun `Undo a removed stop - the route is back`() = runTest {
    val planner = ready()
    listOf(K02, K73, K83).forEach { planner.clickOn(it) }

    planner.remove(1)
    assertEquals(listOf("02", "73"), planner.waypoints)
    planner.undo()

    assertEquals(listOf("02", "83", "73"), planner.waypoints)
    assertTrue(planner.canUndo)
  }

  // Scenario: Reload
  @Test
  fun `Reload - the route and its undo history come back`() = runTest {
    val storage = TestNetwork.Storage()
    val first = ready(TestNetwork.planner(this, storage))
    listOf(K02, K73, K83).forEach { first.clickOn(it) }

    val reloaded = ready(TestNetwork.planner(this, storage))

    assertEquals(listOf("02", "83", "73"), reloaded.waypoints)
    reloaded.undo()
    assertEquals(listOf("02", "73"), reloaded.waypoints)
  }

  @Test
  fun `Language switch keeps the route - the German page finds the English page's route`() =
    runTest {
      val storage = TestNetwork.Storage()
      val english = ready(TestNetwork.planner(this, storage))
      listOf(K02, K83).forEach { english.clickOn(it) }

      val german = ready(TestNetwork.planner(this, storage, language = Language.DE))

      assertEquals(listOf("02", "83"), german.waypoints)
    }

  @Test
  fun `Browser storage unavailable - the planner works without it`() = runTest {
    val blocked =
      object : Kept {
        override fun read(key: String): String? = throw IllegalStateException("SecurityError")

        override fun write(key: String, value: String) =
          throw IllegalStateException("SecurityError")
      }
    val planner = ready(TestNetwork.planner(this, blocked))

    listOf(K02, K83).forEach { planner.clickOn(it) }

    assertEquals(listOf("02", "83"), planner.waypoints)
  }

  @Test
  fun `Switch to hiking - an empty hiking route`() = runTest {
    val planner = ready()
    listOf(K02, K83).forEach { planner.clickOn(it) }

    planner.switchNetwork(IndexedNetworkType.HIKING)
    advanceUntilIdle()

    assertEquals(IndexedNetworkType.HIKING, planner.network)
    assertEquals(listOf(), planner.waypoints)
    assertFalse(planner.canUndo)
  }

  private fun fileOf(vararg ids: Long, on: Planner): String {
    val route =
      PlanningSession.start(on.session!!.graph, ids.map { on.session!!.graph.knots.getValue(it) })
        .route!!
    return RouteFile.write(route, RouteFile.titleOf(route))
  }

  // Scenario: Dropped file
  @Test
  fun `Dropped file - a hiking route opened while planning cycling`() = runTest {
    val planner = ready()
    listOf(K02, K83).forEach { planner.clickOn(it) }
    val hikingFile = "# Hike\nNetwork: hiking\n\n- 12 ${K12} Siegburg\n- 34\n"

    planner.open(hikingFile)
    advanceUntilIdle()

    assertEquals(IndexedNetworkType.HIKING, planner.network)
    assertEquals(listOf("12", "34"), planner.waypoints)
    assertEquals(1, planner.openedRoutes)
  }

  @Test
  fun `Not a route - the route stays and the visitor is told`() = runTest {
    val planner = ready()
    listOf(K02, K83).forEach { planner.clickOn(it) }

    planner.open("# Shopping\n\n- milk\n- bread\n")
    advanceUntilIdle()

    assertEquals(listOf("02", "83"), planner.waypoints)
    assertEquals(TestNetwork.texts["routeFileNone"], planner.message)
  }

  @Test
  fun `an opened route on the same network is an edit Undo takes back`() = runTest {
    val planner = ready()
    listOf(K02, K83).forEach { planner.clickOn(it) }
    val file = fileOf(K12, K34, on = planner)

    planner.open(file)
    assertEquals(listOf("12", "34"), planner.waypoints)
    planner.undo()

    assertEquals(listOf("02", "83"), planner.waypoints)
  }

  @Test
  fun `Copy the link - the link opens the same route`() = runTest {
    val planner = ready()
    listOf(K02, K73, K83).forEach { planner.clickOn(it) }

    val link = assertNotNull(planner.link())
    val other = ready()
    other.open(link)

    assertTrue(link.startsWith("https://knoopbyknoop.com/route/#"), link)
    assertEquals(listOf("02", "83", "73"), other.waypoints)
  }

  @Test
  fun `Same file as the app - name and text as the core writes them`() = runTest {
    val planner = ready()
    listOf(K12, K34, K40).forEach { planner.clickOn(it) }
    val route = planner.session!!.route!!

    val (name, text) = assertNotNull(planner.routeFile())

    assertEquals("knoop-by-knoop-cycling-12-34.knoop", name)
    assertEquals(RouteFile.write(route, RouteFile.titleOf(route)), text)
  }

  // Scenario: GPX
  @Test
  fun `the GPX download holds the route's line and knots`() = runTest {
    val planner = ready()
    listOf(K12, K34, K40).forEach { planner.clickOn(it) }

    val (name, gpx) = assertNotNull(planner.gpx())

    assertEquals("knoop-by-knoop-cycling-12-34.gpx", name)
    assertEquals(1, Regex("<trk>").findAll(gpx).count())
    assertTrue(gpx.contains("<name>40</name>"), gpx)
  }

  @Test
  fun `an incomplete route has nothing to download or link`() = runTest {
    val planner = ready()
    planner.clickOn(K02)

    assertNull(planner.routeFile())
    assertNull(planner.gpx())
    assertNull(planner.link())
  }

  @Test
  fun `a click far from the network tells how far knots are`() = runTest {
    val planner = ready()
    val far = TestNetwork.position(60_000.0, 60_000.0)

    assertIs<Clicked.Done>(planner.click(far.latitude, far.longitude, 30.0, null))
    assertEquals(true, planner.message?.startsWith("No cycling knots within"))
  }

  /** A source whose courses arrive only when [courses] completes. */
  private class SlowCourses(private val courses: kotlinx.coroutines.CompletableDeferred<Unit>) :
    NetworkSource {
    private val source = TestNetwork.Source()

    override suspend fun graph(network: IndexedNetworkType) = source.graph(network)

    override suspend fun courses(loaded: NetworkSource.Loaded): LoadedNetwork {
      courses.await()
      return source.courses(loaded)
    }

    override suspend fun <T> withSquares(load: (com.knoopbyknoop.core.PathDataOrigin) -> T): T? =
      null
  }

  @Test
  fun `Network still loading - and then the courses, and then nothing`() = runTest {
    val courses = kotlinx.coroutines.CompletableDeferred<Unit>()
    val planner =
      Planner(
        SlowCourses(courses),
        TestNetwork.Storage(),
        Language.EN,
        TestNetwork.texts,
        this,
        nowMillis = { 0L },
      )

    planner.start()
    assertEquals(Planner.Loading.NETWORK, planner.loadingStage)
    testScheduler.runCurrent()
    // Scenario: Courses still loading
    assertEquals(Planner.Loading.COURSES, planner.loadingStage)
    courses.complete(Unit)
    advanceUntilIdle()

    assertEquals(Planner.Loading.NOTHING, planner.loadingStage)
  }

  @Test
  fun `Kept route with a ferry - a redraw failing on it still lets the courses load`() = runTest {
    val storage = TestNetwork.Storage()
    val first = ready(TestNetwork.planner(this, storage))
    listOf(K02, K73).forEach { first.clickOn(it) }

    val reopened = TestNetwork.planner(this, storage)
    reopened.onChange = {
      if (reopened.description is RouteDescription.Complete) error("panel failed")
    }
    advanceUntilIdle()

    assertEquals(listOf("02", "73"), reopened.waypoints)
    assertEquals(Planner.Loading.NOTHING, reopened.loadingStage)
    assertTrue(reopened.loaded!!.hasGeometry)
  }
}

class LocalTimeTest {

  @Test
  fun `the covered area's local time, in summer and winter`() {
    // 2026-07-01 10:00 UTC is 12:00 in Amsterdam (CEST), 2026-12-01 10:00 UTC is 11:00 (CET).
    val summer = nowInCoveredArea(1_782_900_000_000.0)
    val winter = nowInCoveredArea(1_796_119_200_000.0)

    assertEquals(com.knoopbyknoop.core.CalendarDay(2026, 7, 1), summer.day)
    assertEquals(12 * 60, summer.minute)
    assertEquals(com.knoopbyknoop.core.CalendarDay(2026, 12, 1), winter.day)
    assertEquals(11 * 60, winter.minute)
  }
}
