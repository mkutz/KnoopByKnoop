package com.knoopbyknoop.core

import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

/**
 * Whole rides through [Navigation], fix by fix, compared with the texts recorded in
 * `src/jvmTest/resources/navigation-rides`, so a change of [Navigation]'s inner workings shows
 * every difference in what the apps get. A missing text is recorded and the test fails once; to
 * record a ride anew, delete its text.
 *
 * The knots lie on a straight road from west to east, 1000 m apart, as in [NavigationTest]; 77 is a
 * knot of the network 300 m north of the road, not on the route. The leg from 12 to 31 has a ferry
 * closing at 18:58; the rides start at 18:50.
 *
 * ```
 *              77
 *   24 ------ 45 ------ 12 ~~~~~~ 31
 *   0 m      1000 m    2000 m    3000 m
 * ```
 */
class NavigationReplayTest {

  private val knot24 = knot(24, "24", east = 0.0)
  private val knot45 = knot(45, "45", east = 1000.0)
  private val knot12 = knot(12, "12", east = 2000.0)
  private val knot31 = knot(31, "31", east = 3000.0)
  private val knot77 = knot(77, "77", east = 500.0, north = 300.0)

  private val connections =
    listOf(
      Connection(24, 45, 1000),
      Connection(45, 12, 1000),
      Connection(12, 31, 1000),
      Connection(12, 24, 2000),
      Connection(24, 77, 600),
      Connection(77, 45, 600),
    )

  private val graph =
    NetworkGraph(
      knots = listOf(knot24, knot45, knot12, knot31, knot77),
      connections = connections,
      ferries = mapOf(connections[2] to Ferry(name = "Veer", openingHours = "Mo-Su 07:00-18:58")),
    )

  private val geometry = NetworkGeometry.straightLines(graph)
  private val route = listOf(knot24, knot45, knot12, knot31)

  /** One ride: the navigation, the time of its last fix, and its lines so far. */
  private inner class Ride(
    val navigation: Navigation,
    val time: Long,
    val lines: List<String>,
  ) {

    /** The ride with a fix at [east], [north] [seconds] after the last one. */
    fun at(
      east: Double,
      north: Double = 0.0,
      accuracy: Double = 5.0,
      seconds: Long = 5,
      visible: Boolean = false,
    ): Ride {
      val fix = Fix(latitude(north), longitude(east), accuracy, time + seconds * 1000)
      val update = navigation.update(fix, visible)
      return Ride(update.navigation, fix.timeMillis, lines + line(fix, update))
    }

    /** The ride along fixes 40 m apart from [from] to [to], [from] itself not included. */
    fun along(
      from: Pair<Double, Double>,
      to: Pair<Double, Double>,
      accuracy: Double = 5.0,
    ): Ride {
      val (fromEast, fromNorth) = from
      val (toEast, toNorth) = to
      val length = kotlin.math.hypot(toEast - fromEast, toNorth - fromNorth)
      val steps = (length / 40).toInt().coerceAtLeast(1)
      return (1..steps).fold(this) { ride, step ->
        val share = step.toDouble() / steps
        ride.at(
          fromEast + (toEast - fromEast) * share,
          fromNorth + (toNorth - fromNorth) * share,
          accuracy,
        )
      }
    }

    /** The ride east along the road from [from] to [to]. */
    fun east(from: Double, to: Double, north: Double = 0.0) = along(from to north, to to north)

    /** The ride with the languages changed at [east], [north]. */
    fun inLanguage(language: Language, east: Double, north: Double = 0.0): Ride {
      val fix = Fix(latitude(north), longitude(east), 5.0, time)
      val changed = navigation.inLanguage(language, language, fix)
      return Ride(changed, time, lines + "language $language | ${state(changed, fix)}")
    }
  }

  private fun start(
    east: Double,
    north: Double = 0.0,
    knots: List<Knot> = route,
    graph: NetworkGraph = this.graph,
    geometry: NetworkGeometry? = this.geometry,
    stops: Set<Int> = setOf(),
  ): Ride {
    val fix = Fix(latitude(north), longitude(east), 5.0, START_MILLIS)
    val update =
      assertNotNull(
        Navigation.start(knots, graph, geometry, fix, stops = stops, localTime = ::localTime)
      )
    return Ride(update.navigation, fix.timeMillis, listOf(line(fix, update)))
  }

  private fun line(fix: Fix, update: NavigationUpdate): String {
    val notices = update.notices.joinToString("; ") { "${it.kind} ${it.text} / ${it.speech}" }
    return "${place(fix)} | $notices | ${update.precision} | ${state(update.navigation, fix)}"
  }

  private fun place(fix: Fix): String =
    "%.1f,%.1f ±%.0f @%d"
      .format(
        (fix.longitude - ORIGIN_LONGITUDE) * METERS_PER_DEGREE * cos(ORIGIN_LATITUDE * PI / 180),
        (fix.latitude - ORIGIN_LATITUDE) * METERS_PER_DEGREE,
        fix.accuracyMeters,
        (fix.timeMillis - START_MILLIS) / 1000,
      )

  /** Everything the apps read of [navigation] with the rider at [fix]. */
  private fun state(navigation: Navigation, fix: Fix): String {
    val progress = navigation.progress(fix)
    val focus = navigation.mapFocus(fix)
    val toRoute = navigation.wayToRoute(fix)?.let { "%.6f,%.6f".format(it.latitude, it.longitude) }
    return listOf(
        "next ${navigation.nextIndex}",
        "off ${navigation.offRoute}",
        "arrived ${navigation.arrived}",
        "wrong ${navigation.wrongWay}",
        "display ${navigation.display.text} / ${navigation.display.offRouteText}",
        "line ${navigation.progressLine}",
        "header ${navigation.header(fix).spoken}",
        "distance ${progress.nextKnotDistance}",
        "rider %.6f".format(progress.strip.rider),
        "strip ${progress.strip.description}",
        "coming ${progress.coming.joinToString { "${it.label} ${it.distance} ${it.arrival}" }}",
        "ferry ${navigation.ferryArrival(2)}",
        "guidance ${navigation.guidance(fix)?.text}",
        "to route $toRoute",
        "focus ${focus.kind} ${focus.key}",
        "ahead ${navigation.courseAhead?.size}",
        "live ${navigation.precision(true)}",
      )
      .joinToString(" | ")
  }

  private fun assertRide(name: String, ride: Ride) {
    val actual = ride.lines.joinToString("\n", postfix = "\n")
    val file = File("src/jvmTest/resources/navigation-rides/$name.txt")
    if (!file.exists()) {
      file.parentFile.mkdirs()
      file.writeText(actual)
      fail("Recorded $file, check it and run again")
    }
    val expected = file.readText()
    if (expected != actual) {
      val expectedLines = expected.lines()
      val actualLines = actual.lines()
      val first =
        expectedLines.indices.firstOrNull { expectedLines[it] != actualLines.getOrNull(it) }
      assertEquals(
        expectedLines.getOrNull(first ?: 0),
        actualLines.getOrNull(first ?: 0),
        "$name differs first in line ${(first ?: 0) + 1}",
      )
      assertEquals(expected, actual, name)
    }
  }

  @Test
  fun `along the route to the end`() {
    // Past the end too: arrived, nothing changes any more.
    assertRide("along-the-route", start(east = 0.0).east(0.0, 3200.0))
  }

  @Test
  fun `along the route seen in the app`() {
    val ride = start(east = 0.0).east(0.0, 600.0)
    assertRide("seen-in-the-app", ride.at(640.0, visible = true).at(680.0, visible = true))
  }

  @Test
  fun `a detour off the route and back`() {
    assertRide(
      "detour",
      start(east = 0.0)
        .east(0.0, 1400.0)
        .along(1400.0 to 0.0, 1400.0 to 400.0)
        .along(1400.0 to 400.0, 2600.0 to 400.0)
        .along(2600.0 to 400.0, 2600.0 to 0.0)
        .east(2600.0, 3000.0),
    )
  }

  @Test
  fun `a detour beside the route back onto the leg`() {
    // Back on route between knots, by the distance to the course alone.
    assertRide(
      "detour-between-knots",
      start(east = 0.0)
        .east(0.0, 1200.0)
        .along(1200.0 to 0.0, 1300.0 to 200.0)
        .along(1300.0 to 200.0, 1700.0 to 200.0)
        .along(1700.0 to 200.0, 1800.0 to 0.0)
        .east(1800.0, 2400.0),
    )
  }

  @Test
  fun `the wrong way and turning around`() {
    assertRide(
      "wrong-way",
      start(east = 0.0).east(0.0, 1700.0).along(1700.0 to 0.0, 1150.0 to 0.0).east(1150.0, 2300.0),
    )
  }

  @Test
  fun `back past the knot passed last`() {
    // Back past 45 and forward again: 45 is not passed a second time.
    assertRide(
      "back-past-a-knot",
      start(east = 0.0).east(0.0, 1300.0).along(1300.0 to 0.0, 700.0 to 0.0).east(700.0, 2100.0),
    )
  }

  @Test
  fun `starting away from the route and joining a leg`() {
    // As on the ride of 2026-11-08: reaching the course between 45 and 12 and following it.
    assertRide(
      "joining-a-leg",
      start(east = 1500.0, north = -250.0)
        .along(1500.0 to -250.0, 1500.0 to 0.0)
        .east(1500.0, 2100.0),
    )
  }

  @Test
  fun `starting away from the route and leaving the course again`() {
    // On a course for a moment, then away: not joined; then onto another leg.
    assertRide(
      "touching-a-leg",
      start(east = 1500.0, north = -250.0)
        .along(1500.0 to -250.0, 1500.0 to 0.0)
        .along(1500.0 to 0.0, 1500.0 to -200.0)
        .along(1500.0 to -200.0, 2600.0 to -200.0)
        .along(2600.0 to -200.0, 2600.0 to 0.0)
        .east(2600.0, 2800.0),
    )
  }

  @Test
  fun `starting away from the route and riding to the start`() {
    // Far away: guidance notices on the way, then the start knot.
    assertRide(
      "to-the-start",
      start(east = 1000.0, north = -2000.0)
        .along(1000.0 to -2000.0, 0.0 to -400.0)
        .along(0.0 to -400.0, 0.0 to 0.0)
        .east(0.0, 1100.0),
    )
  }

  @Test
  fun `starting away from the route and reaching a knot in the middle`() {
    assertRide(
      "onto-a-knot",
      start(east = 2000.0, north = -500.0)
        .along(2000.0 to -500.0, 2000.0 to 0.0)
        .east(2000.0, 2400.0),
    )
  }

  @Test
  fun `starting at a knot in the middle and between knots`() {
    assertRide("start-in-the-middle", start(east = 2000.0).east(2000.0, 2300.0))
    assertRide("start-between", start(east = 1500.0, north = 20.0).east(1500.0, 2100.0))
  }

  @Test
  fun `through a knot that is not on the route`() {
    assertRide(
      "off-route-knot",
      start(east = 0.0)
        .east(0.0, 200.0)
        .along(200.0 to 0.0, 500.0 to 300.0)
        .along(500.0 to 300.0, 1000.0 to 0.0)
        .east(1000.0, 1300.0),
    )
  }

  @Test
  fun `coarse and less accurate fixes far from the course`() {
    assertRide(
      "coarse",
      start(east = 0.0)
        .east(0.0, 600.0)
        .at(640.0, 300.0, accuracy = 80.0)
        .at(680.0, 160.0, accuracy = 30.0)
        .at(720.0, 160.0, accuracy = 30.0)
        .at(760.0, 160.0)
        .at(800.0, 120.0)
        .along(800.0 to 120.0, 900.0 to 0.0)
        .at(940.0, 0.0, accuracy = 80.0)
        .east(940.0, 1200.0),
    )
  }

  @Test
  fun `positions delivered again and after a long gap`() {
    assertRide(
      "repeated-and-gaps",
      start(east = 0.0)
        .east(0.0, 800.0)
        .at(800.0, seconds = 0)
        .at(840.0, seconds = -5)
        // A minute later beyond 45: passed on the line from the last fix only within 30 s.
        .at(1100.0, seconds = 60)
        .east(1100.0, 1300.0),
    )
  }

  @Test
  fun `changing the language on route and off route`() {
    assertRide(
      "languages",
      start(east = 0.0)
        .east(0.0, 900.0)
        .inLanguage(Language.DE, 900.0)
        .east(900.0, 1300.0)
        .along(1300.0 to 0.0, 1300.0 to 300.0)
        .inLanguage(Language.NL, 1300.0, 300.0)
        .along(1300.0 to 300.0, 1300.0 to 0.0)
        .east(1300.0, 2100.0),
    )
  }

  @Test
  fun `a round trip`() {
    val roundTrip = listOf(knot24, knot45, knot12, knot24)
    assertRide(
      "round-trip",
      start(east = 0.0, knots = roundTrip)
        .east(0.0, 2000.0)
        .along(2000.0 to 0.0, 0.0 to 0.0)
        .at(-40.0),
    )
  }

  @Test
  fun `without courses`() {
    assertRide(
      "without-courses",
      start(east = 1000.0, north = -2000.0, geometry = null)
        .along(1000.0 to -2000.0, 1000.0 to 0.0)
        .east(1000.0, 2100.0),
    )
  }

  @Test
  fun `without courses near the first knot`() {
    assertRide(
      "without-courses-at-the-start",
      start(east = -200.0, north = -300.0, geometry = null)
        .along(-200.0 to -300.0, 0.0 to 0.0)
        .at(40.0, 300.0, accuracy = 80.0)
        .east(40.0, 1100.0),
    )
  }

  @Test
  fun `knots close together`() {
    val knot50 = knot(50, "50", east = 1150.0)
    val closeGraph =
      NetworkGraph(
        knots = listOf(knot24, knot45, knot50),
        connections = listOf(Connection(24, 45, 1000), Connection(45, 50, 150)),
      )
    val ride =
      start(
        east = 0.0,
        knots = listOf(knot24, knot45, knot50),
        graph = closeGraph,
        geometry = NetworkGeometry.straightLines(closeGraph),
      )
    assertRide("close-knots", ride.east(0.0, 1300.0))
  }

  @Test
  fun `joining one leg and then the next`() {
    // On 45-12 short of 12, then a minute later on 12-31: joining starts anew on the other leg.
    assertRide(
      "joining-another-leg",
      start(east = 1960.0, north = -250.0)
        .along(1960.0 to -250.0, 1960.0 to -20.0)
        .at(2045.0, -20.0, seconds = 40)
        .along(2045.0 to -20.0, 2300.0 to -20.0)
        .east(2300.0, 2500.0),
    )
  }

  @Test
  fun `through a knot that is not on the route while off route`() {
    assertRide(
      "off-route-knot-while-off-route",
      start(east = 0.0)
        .east(0.0, 200.0)
        .along(200.0 to 0.0, 200.0 to 200.0)
        .along(200.0 to 200.0, 500.0 to 300.0)
        .along(500.0 to 300.0, 1000.0 to 0.0)
        .east(1000.0, 1200.0),
    )
  }

  @Test
  fun `beside a knot that is not on the route`() {
    // 78 is 60 m beside the road: on the course again close to it is still off route.
    val knot78 = knot(78, "78", east = 1500.0, north = 60.0)
    val sideGraph =
      NetworkGraph(
        knots = listOf(knot24, knot45, knot12, knot31, knot78),
        connections = connections.take(3) + Connection(45, 78, 520) + Connection(78, 12, 520),
      )
    assertRide(
      "beside-an-off-route-knot",
      start(east = 0.0, graph = sideGraph, geometry = NetworkGeometry.straightLines(sideGraph))
        .east(0.0, 1400.0)
        .along(1400.0 to 0.0, 1500.0 to 60.0)
        .at(1520.0, 20.0)
        .at(1560.0, 10.0)
        .east(1600.0, 2100.0),
    )
  }

  @Test
  fun `a course doubling back past itself`() {
    // The course 24-45 runs back past itself 20 m further north, and on again 40 m north.
    val doublingBack =
      NetworkGeometry(
        graph,
        geometry.courses.mapIndexed { index, course ->
          val connection = graph.connections[index]
          if (connection.fromId == 24L && connection.toId == 45L)
            listOf(
                0.0 to 0.0,
                800.0 to 0.0,
                800.0 to 20.0,
                200.0 to 20.0,
                200.0 to 40.0,
                1000.0 to 40.0,
                1000.0 to 0.0,
              )
              .map { (east, north) -> at(east, north) }
          else course
        },
      )
    assertRide(
      "doubling-back",
      start(east = 0.0, geometry = doublingBack)
        .east(0.0, 500.0)
        .at(505.0, 14.0)
        .at(510.0, 2.0)
        .east(510.0, 800.0)
        .along(800.0 to 20.0, 200.0 to 20.0)
        .along(200.0 to 40.0, 1000.0 to 40.0)
        .east(1000.0, 1200.0),
    )
  }

  // Route points, as in RoutePointNavigationTest: on 45-12, or north of it with a way.

  private fun at(east: Double, north: Double = 0.0) = GeoPoint(latitude(north), longitude(east))

  private fun point(east: Double, north: Double, wayPoints: List<Double>, straight: Boolean): Knot {
    val connection = graph.connectionBetween(45, 12)!!
    val place = CoursePlace(connection, (east - 1000) / 1000, at(east), north)
    val way = listOf(at(east, north)) + wayPoints.map { at(east, it) } + at(east)
    return routePointKnot(at(east, north), NetworkType.CYCLING, place, way, straight)
  }

  private fun startWith(vararg waypoints: Knot, east: Double): Ride {
    val routeGraph = graph.withRoutePoints(waypoints.toList())
    val planned = Route.plan(waypoints.toList(), ShortestPathRouter(routeGraph))
    val knots = (planned.summary as RouteSummary.Complete).knots
    val stops = waypoints.drop(1).dropLast(1).map { knots.indexOf(it) }.toSet()
    return start(
      east,
      knots = knots,
      graph = routeGraph,
      geometry = geometry.withRoutePoints(routeGraph),
      stops = stops,
    )
  }

  @Test
  fun `a stop at a straight line`() {
    val stop = point(1500.0, north = 800.0, wayPoints = listOf(), straight = true)
    assertRide(
      "straight-line",
      startWith(knot24, stop, knot31, east = 0.0)
        .east(0.0, 1500.0)
        .at(1600.0, 200.0)
        .at(1700.0, 400.0)
        .at(1650.0, 600.0)
        .along(1650.0 to 600.0, 1500.0 to 800.0)
        .along(1500.0 to 800.0, 1500.0 to 0.0)
        .east(1500.0, 2100.0),
    )
  }

  @Test
  fun `a stop along a way`() {
    val stop = point(1500.0, north = 300.0, wayPoints = listOf(290.0, 150.0), straight = false)
    assertRide(
      "way-to-a-stop",
      startWith(knot24, stop, knot31, east = 0.0)
        .east(0.0, 1500.0)
        .along(1500.0 to 0.0, 1500.0 to 300.0)
        .along(1500.0 to 300.0, 1500.0 to 0.0)
        .east(1500.0, 2100.0),
    )
  }

  @Test
  fun `off route back onto a straight line`() {
    val stop = point(1500.0, north = 800.0, wayPoints = listOf(), straight = true)
    assertRide(
      "straight-line-from-off-route",
      startWith(knot24, stop, knot31, east = 0.0)
        .east(0.0, 1200.0)
        .along(1200.0 to 0.0, 1200.0 to -300.0)
        .along(1200.0 to -300.0, 1400.0 to 100.0)
        .along(1400.0 to 100.0, 1500.0 to 800.0),
    )
  }

  @Test
  fun `an end flag off the network`() {
    // As in RoutePointGuidanceTest: the way leaves the road at 1500 m north, then west.
    val connection = graph.connectionBetween(45, 12)!!
    val end =
      routePointKnot(
        at(1350.0, 200.0),
        NetworkType.CYCLING,
        CoursePlace(connection, 0.5, at(1500.0), 0.0),
        listOf(at(1350.0, 200.0), at(1500.0, 200.0), at(1500.0)),
        false,
      )
    val routeGraph = graph.withRoutePoints(listOf(end))
    val ride =
      start(
        east = 0.0,
        knots = listOf(knot24, knot45, end),
        graph = routeGraph,
        geometry = geometry.withRoutePoints(routeGraph),
      )
    assertRide(
      "end-flag-off-the-network",
      ride
        .east(0.0, 1500.0)
        .along(1500.0 to 0.0, 1500.0 to 200.0)
        .along(1500.0 to 200.0, 1350.0 to 200.0),
    )
  }

  private fun localTime(millis: Long): LocalMoment {
    val minutes = (millis / 60_000).toInt()
    return LocalMoment(DAY.plusDays(minutes / MINUTES_PER_DAY), minutes % MINUTES_PER_DAY)
  }

  private companion object {
    const val ORIGIN_LATITUDE = 52.0
    const val ORIGIN_LONGITUDE = 5.0
    val DAY = CalendarDay(2027, 6, 16)
    const val START_MILLIS = (18 * 60 + 50) * 60_000L

    fun latitude(north: Double) = ORIGIN_LATITUDE + north / METERS_PER_DEGREE

    fun longitude(east: Double) =
      ORIGIN_LONGITUDE + east / (METERS_PER_DEGREE * cos(ORIGIN_LATITUDE * PI / 180))

    fun knot(id: Long, ref: String, east: Double, north: Double = 0.0) =
      Knot(id, ref, NetworkType.CYCLING, latitude(north), longitude(east))
  }
}
