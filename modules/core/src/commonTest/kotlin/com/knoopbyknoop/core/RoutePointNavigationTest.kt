package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers `knot-navigation` "Navigating with route points" and "Riding the way to a route point".
 *
 * The knots lie on a straight road from west to east, 1000 m apart, as in [NavigationTest]. Route
 * points are put on 45-12, or north of it with a way.
 *
 * ```
 *                       P (way north of the road)
 *                       |
 *   24 ------ 45 ------ + ------ 12 ------ 31
 *   0 m      1000 m   1500 m    2000 m    3000 m
 * ```
 */
class RoutePointNavigationTest {

  private val knot24 = knot(24, "24", east = 0.0)
  private val knot45 = knot(45, "45", east = 1000.0)
  private val knot12 = knot(12, "12", east = 2000.0)
  private val knot31 = knot(31, "31", east = 3000.0)

  private val graph =
    NetworkGraph(
      knots = listOf(knot24, knot45, knot12, knot31),
      connections =
        listOf(Connection(24, 45, 1000), Connection(45, 12, 1000), Connection(12, 31, 1000)),
    )

  private val geometry = NetworkGeometry.straightLines(graph)

  private var time = 0L

  private fun fix(east: Double, north: Double = 0.0): Fix {
    time += 5_000
    return Fix(latitude(north), longitude(east), 5.0, time)
  }

  private fun at(east: Double, north: Double = 0.0) = GeoPoint(latitude(north), longitude(east))

  /**
   * A route point on 45-12 at [east]: on the road, or [north] of it, reached by a way through
   * [wayPoints] (north offsets at [east]), straight if [straight].
   */
  private fun point(
    east: Double,
    north: Double = 0.0,
    wayPoints: List<Double> = listOf(),
    straight: Boolean = false,
  ): Knot {
    val connection = graph.connectionBetween(45, 12)!!
    val place = CoursePlace(connection, (east - 1000) / 1000, at(east), north)
    val way =
      if (north == 0.0) listOf()
      else listOf(at(east, north)) + wayPoints.map { at(east, it) } + at(east)
    return routePointKnot(at(east, north), NetworkType.CYCLING, place, way, straight)
  }

  private class Ride(val navigation: Navigation, val notices: List<Notice>)

  /** Starts navigating [waypoints] at [east], [north], as the apps do with the route graph. */
  private fun start(vararg waypoints: Knot, east: Double, north: Double = 0.0): Ride {
    val routeGraph = graph.withRoutePoints(waypoints.toList())
    val route = Route.plan(waypoints.toList(), ShortestPathRouter(routeGraph))
    val knots = (route.summary as RouteSummary.Complete).knots
    val stops = waypoints.drop(1).dropLast(1).map { stop -> knots.indexOf(stop) }.toSet()
    val update =
      assertNotNull(
        Navigation.start(
          knots,
          routeGraph,
          geometry.withRoutePoints(routeGraph),
          fix(east, north),
          stops = stops,
        )
      )
    return Ride(update.navigation, update.notices)
  }

  private fun Ride.through(vararg fixes: Fix): Ride =
    fixes.fold(this) { ride, fix ->
      val update = ride.navigation.update(fix)
      Ride(update.navigation, ride.notices + update.notices)
    }

  private fun Ride.along(from: Double, to: Double, north: Double = 0.0): Ride {
    val steps = (kotlin.math.abs(to - from) / 40).toInt()
    val sign = if (to >= from) 1 else -1
    return through(*(1..steps).map { fix(from + sign * it * 40.0, north) }.toTypedArray())
  }

  private fun Ride.texts(kind: NoticeKind) = notices.filter { it.kind == kind }.map { it.text }

  // Requirement: Navigating with route points

  @Test
  fun `starting at the point`() {
    val started = start(point(1500.0), knot31, east = 1500.0)

    assertEquals(knot12, started.navigation.nextKnot)
    assertEquals(listOf("Next 12, then 31"), started.notices.map { it.text })
  }

  @Test
  fun `starting away from the point`() {
    val home = point(1500.0)
    val started = start(home, knot31, east = 1500.0, north = -400.0)

    assertTrue(started.navigation.offRoute)
    assertEquals(home, started.navigation.nextKnot)
    assertEquals(listOf("To the start, 400 m to the north"), started.notices.map { it.text })
    assertEquals("Off route, on to the start", started.navigation.display.offRouteText)
  }

  @Test
  fun `joining further along`() {
    // On the route between the start point and 12: the point stays next, as any start.
    val home = point(1500.0)
    val started = start(home, knot31, east = 1700.0)

    assertTrue(started.navigation.offRoute)
    assertEquals(home, started.navigation.nextKnot)
  }

  @Test
  fun `a stop next`() {
    val stop = point(1500.0)
    val passed45 = start(knot24, stop, knot31, east = 0.0).along(0.0, 1080.0)

    assertEquals(listOf("On to the stop, then 12"), passed45.texts(NoticeKind.NEXT).takeLast(1))
    assertEquals(
      listOf("On to the stop, then 12"),
      passed45.notices.filter { it.kind == NoticeKind.NEXT }.map { it.speech }.takeLast(1),
    )
    val display = passed45.navigation.display
    assertEquals(WaypointRole.STOP, display.nextRole)
    assertEquals("On to the stop, then 12", display.text)
  }

  @Test
  fun `the last knot before the end`() {
    val end = point(1500.0)
    val passed45 = start(knot24, end, east = 0.0).along(0.0, 1080.0)

    assertEquals(listOf("On to the end"), passed45.texts(NoticeKind.NEXT).takeLast(1))
    assertEquals("On to the end", passed45.navigation.display.text)
    assertEquals(WaypointRole.END, passed45.navigation.display.nextRole)
  }

  @Test
  fun `reminder and arrival`() {
    val end = point(1500.0)
    val arrived = start(knot24, end, east = 0.0).along(0.0, 1500.0)

    assertEquals(listOf("The end ahead"), arrived.texts(NoticeKind.REMINDER).takeLast(1))
    assertTrue(arrived.navigation.arrived)
    assertEquals(NoticeKind.ARRIVED, arrived.notices.last().kind)
  }

  @Test
  fun `a point the way does not reach`() {
    // Inside a park: the way along paths ends 60 m before the point.
    val end = point(1500.0, north = 100.0, wayPoints = listOf(40.0))
    val atWayEnd =
      start(knot24, end, east = 0.0).along(0.0, 1500.0).along(0.0, 40.0).let {
        it.through(fix(1500.0, 20.0), fix(1500.0, 40.0))
      }

    assertTrue(atWayEnd.navigation.arrived)
  }

  @Test
  fun `off route with the end next`() {
    val end = point(1500.0, north = 100.0, wayPoints = listOf(40.0))
    val passed45 = start(knot24, end, east = 0.0).along(0.0, 1080.0)

    val off = passed45.through(fix(1200.0, -200.0))

    assertTrue(off.navigation.offRoute)
    assertTrue(
      off.texts(NoticeKind.OFF_ROUTE).single().startsWith("Off route, on to the end, "),
      off.texts(NoticeKind.OFF_ROUTE).single(),
    )
    val lineEnd = assertNotNull(off.navigation.wayToRoute(fix(1200.0, -200.0)))
    assertTrue(
      roughMeters(lineEnd, at(1200.0)) < 5.0,
      "the dashed line goes to the route ahead, on the way to the point: $lineEnd",
    )
  }

  @Test
  fun `a place's icon on the strip`() {
    val plain = point(1500.0, north = 100.0, wayPoints = listOf(40.0))
    val cafe =
      plain.copy(routePoint = plain.routePoint!!.copy(placeKind = "food", placeName = "Café"))
    val started = start(knot24, cafe, knot31, east = 0.0)

    val mark = started.navigation.progress(fix(0.0)).strip.marks.single { it.routePoint }

    assertEquals("food", mark.place)
  }

  @Test
  fun `on the strip`() {
    val stop = point(1500.0, north = 100.0, wayPoints = listOf(40.0))
    val started = start(knot24, stop, knot31, east = 0.0)

    val progress = started.navigation.progress(fix(0.0))
    val mark = progress.strip.marks.single { it.routePoint }

    assertEquals(ProgressMarkKind.STOP, mark.kind)
    // 24 → 45 → place (1500 m), the way there and back (2 × 100 m), place → 12 → 31 (1500 m).
    val total = (mark.fraction * 3200).toInt()
    assertEquals(1600.0, total.toDouble(), 20.0, "the point after 1500 m and its way of 100 m")
    assertEquals(
      "Stop, ${progress.coming.single { it.knot == stop }.distance}",
      progress.coming.single { it.knot == stop }.description,
    )
  }

  @Test
  fun `a round trip from a route point`() {
    val home = point(1500.0)
    val started = start(home, knot31, home, east = 1500.0)

    val left = started.along(1500.0, 1600.0).along(1600.0, 1500.0)
    assertFalse(left.navigation.arrived, "leaving does not count as arriving")

    val back = left.along(1500.0, 3000.0).along(3000.0, 1500.0)
    assertTrue(back.navigation.arrived)
  }

  @Test
  fun `a route point within 30 m of a knot`() {
    val beside45 = point(1020.0)
    val passed = start(knot24, beside45, knot31, east = 0.0).along(0.0, 1080.0)

    assertEquals(knot12, passed.navigation.nextKnot, "both count as passed")
  }

  // Requirement: Riding the way to a route point

  @Test
  fun `following a way along paths`() {
    val stop = point(1500.0, north = 300.0, wayPoints = listOf(290.0, 150.0))
    val atPlace = start(knot24, stop, knot31, east = 0.0).along(0.0, 1500.0)
    val distanceAtPlace = atPlace.navigation.progress(fix(1500.0)).nextKnotDistance

    val up = atPlace.along(0.0, 200.0).let { it }
    val onTheWay =
      atPlace.through(fix(1500.0, 40.0), fix(1500.0, 80.0), fix(1500.0, 120.0), fix(1500.0, 160.0))

    assertFalse(onTheWay.navigation.offRoute)
    assertEquals(stop, onTheWay.navigation.nextKnot)
    val distanceUp = onTheWay.navigation.progress(fix(1500.0, 160.0)).nextKnotDistance
    assertTrue(distanceUp != distanceAtPlace, "$distanceUp counts down from $distanceAtPlace")
    assertNotNull(up)
  }

  @Test
  fun `leaving a way along paths`() {
    val stop = point(1500.0, north = 300.0, wayPoints = listOf(290.0, 150.0))
    val onTheWay =
      start(knot24, stop, knot31, east = 0.0)
        .along(0.0, 1500.0)
        .through(fix(1500.0, 40.0), fix(1500.0, 80.0))

    val away = onTheWay.through(fix(1600.0, 120.0), fix(1700.0, 160.0))

    assertTrue(away.navigation.offRoute)
  }

  @Test
  fun `a straight line`() {
    // 800 m straight to the stop; the rider goes around a building 200 m beside the line.
    val stop = point(1500.0, north = 800.0, straight = true)
    val atPlace = start(knot24, stop, knot31, east = 0.0).along(0.0, 1500.0)

    val around = atPlace.through(fix(1600.0, 200.0), fix(1700.0, 400.0), fix(1650.0, 600.0))

    assertFalse(around.navigation.offRoute)
    assertTrue(around.texts(NoticeKind.OFF_ROUTE).isEmpty())
    assertTrue(
      around.navigation.progressLine.endsWith("to the stop"),
      around.navigation.progressLine,
    )
  }

  @Test
  fun `turning round at a detour`() {
    val stop = point(1500.0, north = 300.0, wayPoints = listOf(290.0, 150.0))
    val atStop =
      start(knot24, stop, knot31, east = 0.0).along(0.0, 1500.0).along(0.0, 300.0).let {
        it.through(*(1..7).map { step -> fix(1500.0, step * 40.0) }.toTypedArray())
      }
    assertEquals(knot12, atStop.navigation.nextKnot, "the next knot is named")

    val back =
      atStop.through(*(1..7).map { step -> fix(1500.0, 300.0 - step * 40.0) }.toTypedArray())

    assertTrue(
      back.texts(NoticeKind.WRONG_WAY).isEmpty(),
      back.texts(NoticeKind.WRONG_WAY).toString(),
    )
    assertFalse(back.navigation.offRoute)
  }

  @Test
  fun `without internet access`() {
    // The way is stored with the point; navigating reads nothing else.
    val stop = point(1500.0, north = 300.0, wayPoints = listOf(290.0, 150.0))
    val started = start(knot24, stop, knot31, east = 0.0)

    assertEquals(4, stop.routePoint!!.way.size, "point, connector end, path, place")
    assertFalse(started.navigation.offRoute)
  }

  private companion object {
    const val ORIGIN_LATITUDE = 52.0
    const val ORIGIN_LONGITUDE = 5.0
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180

    fun latitude(north: Double) = ORIGIN_LATITUDE + north / METERS_PER_DEGREE

    fun longitude(east: Double) =
      ORIGIN_LONGITUDE + east / (METERS_PER_DEGREE * cos(ORIGIN_LATITUDE * PI / 180))

    fun knot(id: Long, ref: String, east: Double) =
      Knot(id, ref, NetworkType.CYCLING, latitude(0.0), longitude(east))
  }
}
