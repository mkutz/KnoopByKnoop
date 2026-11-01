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
 * Covers the scenarios of `knot-navigation` that the core decides.
 *
 * The knots lie on a straight road from west to east, 1000 m apart; 77 is a knot of the network 300
 * m north of the road, not on the route.
 *
 * ```
 *              77
 *   24 ------ 45 ------ 12 ------ 31
 *   0 m      1000 m    2000 m    3000 m
 * ```
 */
class NavigationTest {

  private val knot24 = knot(24, "24", east = 0.0)
  private val knot45 = knot(45, "45", east = 1000.0)
  private val knot12 = knot(12, "12", east = 2000.0)
  private val knot31 = knot(31, "31", east = 3000.0)
  private val knot77 = knot(77, "77", east = 500.0, north = 300.0)

  private val graph =
    NetworkGraph(
      knots = listOf(knot24, knot45, knot12, knot31, knot77),
      connections =
        listOf(
          Connection(24, 45, 1000),
          Connection(45, 12, 1000),
          Connection(12, 31, 1000),
          Connection(12, 24, 2000),
          Connection(24, 77, 600),
          Connection(77, 45, 600),
        ),
    )

  private val geometry = NetworkGeometry.straightLines(graph)
  private val route = listOf(knot24, knot45, knot12, knot31)

  private var time = 0L

  private fun fix(east: Double, north: Double = 0.0, accuracy: Double = 5.0): Fix {
    time += 5_000
    return Fix(latitude(north), longitude(east), accuracy, time)
  }

  private fun start(
    east: Double,
    north: Double = 0.0,
    knots: List<Knot> = route,
    geometry: NetworkGeometry? = this.geometry,
  ) = assertNotNull(Navigation.start(knots, graph, geometry, fix(east, north)))

  /** Rides along the fixes, collecting every notice. */
  private fun Navigation.ride(vararg fixes: Fix): Pair<Navigation, List<Notice>> =
    fixes.fold(this to listOf()) { (navigation, notices), fix ->
      val update = navigation.update(fix)
      update.navigation to notices + update.notices
    }

  private fun ride(navigation: Navigation, from: Double, to: Double, north: Double = 0.0) =
    navigation.ride(
      *(0..((to - from) / 40).toInt()).map { fix(from + it * 40.0, north) }.toTypedArray()
    )

  // Requirement: Start navigation

  @Test
  fun `start at the first knot`() {
    val started = start(east = 0.0)

    assertEquals(knot45, started.navigation.nextKnot)
    assertFalse(started.navigation.offRoute)
    assertEquals(listOf("Next 45, then 12"), started.notices.map { it.text })
  }

  @Test
  fun `start at a knot in the middle of the route`() {
    val started = start(east = 2000.0)

    assertEquals(knot31, started.navigation.nextKnot)
  }

  @Test
  fun `start between two knots`() {
    val started = start(east = 1500.0, north = 20.0)

    assertEquals(knot12, started.navigation.nextKnot)
    assertFalse(started.navigation.offRoute)
  }

  @Test
  fun `start away from the route`() {
    val started = start(east = 1000.0, north = -2000.0)

    assertEquals(knot24, started.navigation.nextKnot, "the first knot, not 45 nearest")
    assertTrue(started.navigation.offRoute)
    assertEquals(
      listOf("Off route, next 24, 2.2 km to the north-west"),
      started.notices.map { it.text },
    )
  }

  @Test
  fun `start away from the route without courses`() {
    val started = start(east = 1000.0, north = -2000.0, geometry = null)

    assertEquals(knot45, started.navigation.nextKnot)
  }

  @Test
  fun `joining the route on the way to the start`() {
    val started = start(east = 2000.0, north = -500.0)
    assertEquals(knot24, started.navigation.nextKnot)

    val (navigation, notices) = started.navigation.ride(fix(2000.0, north = -40.0), fix(2000.0))

    assertEquals(knot31, navigation.nextKnot)
    assertFalse(navigation.offRoute)
    assertEquals("Back on route, next 31", notices.last().text)
  }

  @Test
  fun `incomplete route`() {
    assertNull(Navigation.start(listOf(knot24), graph, geometry, fix(0.0)))
  }

  // Requirement: Passing a knot

  @Test
  fun `passing the next knot`() {
    val (navigation, notices) = ride(start(east = 0.0).navigation, from = 600.0, to = 1100.0)

    assertEquals(knot12, navigation.nextKnot)
    assertEquals(
      listOf("Next 12, then 31"),
      notices.filter { it.kind == NoticeKind.NEXT }.map { it.text },
    )
  }

  @Test
  fun `a knot passed between two fixes 40 m apart`() {
    val (navigation, _) =
      start(east = 0.0).navigation.ride(fix(960.0, north = 10.0), fix(1040.0, north = 10.0))

    assertEquals(knot12, navigation.nextKnot)
  }

  @Test
  fun `riding close to a knot without passing it`() {
    val (navigation, notices) =
      ride(start(east = 0.0).navigation, from = 600.0, to = 1400.0, north = 80.0)

    assertEquals(knot45, navigation.nextKnot)
    assertTrue(notices.none { it.kind == NoticeKind.NEXT })
  }

  @Test
  fun `round trip`() {
    val roundTrip = listOf(knot24, knot45, knot12, knot24)
    val started = start(east = 0.0, knots = roundTrip)
    assertEquals(knot45, started.navigation.nextKnot)

    // Riding a little away from the start and back is not arriving.
    val (back, backNotices) = started.navigation.ride(fix(60.0), fix(120.0), fix(60.0), fix(0.0))
    assertFalse(back.arrived)
    assertTrue(backNotices.none { it.kind == NoticeKind.ARRIVED })

    val (atTwelve, _) = ride(back, from = 40.0, to = 2000.0)
    assertEquals(3, atTwelve.nextIndex)

    // Back west from 12 to 24, which is the end now.
    val (arrived, notices) = atTwelve.ride(*(1..50).map { fix(2000.0 - it * 40.0) }.toTypedArray())
    assertTrue(arrived.arrived)
    assertEquals(NoticeKind.ARRIVED, notices.last().kind)
  }

  /** 46 stands 10 m after 45, so a fix at 45 is within 30 m of both. */
  private val knot46 = knot(46, "46", east = 1010.0)
  private val doubleKnotGraph =
    NetworkGraph(
      knots = listOf(knot24, knot45, knot46, knot12),
      connections =
        listOf(Connection(24, 45, 1000), Connection(45, 46, 10), Connection(46, 12, 990)),
    )
  private val doubleKnotRoute = listOf(knot24, knot45, knot46, knot12)

  private fun startDoubleKnot(east: Double) =
    assertNotNull(
      Navigation.start(
        doubleKnotRoute,
        doubleKnotGraph,
        NetworkGeometry.straightLines(doubleKnotGraph),
        fix(east),
      )
    )

  @Test
  fun `passing a double knot`() {
    val (navigation, notices) =
      ride(startDoubleKnot(east = 0.0).navigation, from = 600.0, to = 1000.0)

    assertEquals(knot12, navigation.nextKnot)
    assertEquals(listOf("Next 12"), notices.filter { it.kind == NoticeKind.NEXT }.map { it.text })
    assertEquals(
      listOf("45 ahead, then 46"),
      notices.filter { it.kind == NoticeKind.REMINDER }.map { it.text },
    )
  }

  @Test
  fun `riding on after a double knot`() {
    val (atDoubleKnot, _) = ride(startDoubleKnot(east = 0.0).navigation, from = 600.0, to = 1000.0)
    val atDoubleKnotStrip = atDoubleKnot.progress(atDoubleKnot.lastFix).strip.rider

    val (navigation, _) = ride(atDoubleKnot, from = 1040.0, to = 1500.0)
    val progress = navigation.progress(navigation.lastFix)

    assertEquals(knot12, navigation.nextKnot)
    assertEquals("520 m", progress.nextKnotDistance)
    assertTrue(progress.strip.rider > atDoubleKnotStrip)
  }

  @Test
  fun `start at a double knot`() {
    val started = startDoubleKnot(east = 1005.0)

    assertEquals(knot12, started.navigation.nextKnot)
    assertEquals(listOf("Next 12"), started.notices.map { it.text })
  }

  /** The test network with [knot] also tagged on a node at [east], [north]. */
  private fun mergedGraph(knot: Knot, east: Double, north: Double): Pair<NetworkGraph, Knot> {
    val merged = knot.copy(otherNodes = listOf(GeoPoint(latitude(north), longitude(east))))
    return NetworkGraph(
      graph.knots.values.map { if (it.id == knot.id) merged else it },
      graph.connections,
    ) to merged
  }

  @Test
  fun `passing a merged knot at its other node`() {
    // 45 is also tagged 195 m north of its position; the course from 24 passes that node, and runs
    // on to 45's position, as the pipeline draws it.
    val (mergedGraph, merged45) = mergedGraph(knot45, east = 1000.0, north = 195.0)
    val mergedRoute = listOf(knot24, merged45, knot12, knot31)
    val courses =
      mergedGraph.connections.map { connection ->
        if (connection.fromId == 24L && connection.toId == 45L)
          listOf(
            GeoPoint(latitude(0.0), longitude(0.0)),
            GeoPoint(latitude(195.0), longitude(1000.0)),
            GeoPoint(latitude(0.0), longitude(1000.0)),
          )
        else
          listOf(connection.fromId, connection.toId).map {
            val knot = mergedGraph.knots.getValue(it)
            GeoPoint(knot.latitude, knot.longitude)
          }
      }
    val started =
      assertNotNull(
        Navigation.start(mergedRoute, mergedGraph, NetworkGeometry(mergedGraph, courses), fix(0.0))
      )

    val (navigation, notices) =
      started.navigation.ride(
        *(1..25).map { fix(east = it * 40.0, north = it * 40.0 * 0.195) }.toTypedArray()
      )

    assertEquals(knot12, navigation.nextKnot)
    assertFalse(navigation.offRoute)
    assertEquals(
      listOf("Next 12, then 31"),
      notices.filter { it.kind == NoticeKind.NEXT }.map { it.text },
    )
  }

  @Test
  fun `passing a merged knot that is not on the route`() {
    // 77 is also tagged 40 m north of the road, where the rider passes 20 m from it.
    val (mergedGraph, _) = mergedGraph(knot77, east = 500.0, north = 40.0)
    val started = assertNotNull(Navigation.start(route, mergedGraph, geometry, fix(0.0)))

    val (_, notices) = ride(started.navigation, from = 40.0, to = 600.0, north = 20.0)

    val offRoute = notices.filter { it.kind == NoticeKind.OFF_ROUTE }.map { it.text }
    assertEquals(1, offRoute.size)
    assertTrue(offRoute.single().startsWith("Off route, next 45, "), offRoute.single())
  }

  @Test
  fun `start at another node of a merged knot`() {
    val (mergedGraph, merged24) = mergedGraph(knot24, east = 0.0, north = 195.0)
    val mergedRoute = listOf(merged24, knot45, knot12, knot31)

    val started =
      assertNotNull(Navigation.start(mergedRoute, mergedGraph, geometry, fix(0.0, north = 195.0)))

    assertEquals(knot45, started.navigation.nextKnot)
    assertFalse(started.navigation.offRoute)
  }

  // Requirement: Number to follow

  @Test
  fun `distance to the first knot`() {
    val started = start(east = -500.0, north = 300.0)
    assertTrue(started.navigation.offRoute)

    val (navigation, notices) = started.navigation.ride(fix(-80.0))

    assertEquals(knot24, navigation.nextKnot)
    assertFalse(navigation.offRoute)
    assertEquals("Back on route, next 24", notices.first().text)
    assertEquals("80 m", navigation.progress(navigation.lastFix).nextKnotDistance)
    assertEquals("80 m to 24", navigation.progressLine)
  }

  // Requirement: Reminder before a knot

  @Test
  fun `approaching the next knot`() {
    val (_, notices) = ride(start(east = 0.0).navigation, from = 600.0, to = 960.0)

    assertEquals(
      listOf("45 ahead, then 12"),
      notices.filter { it.kind == NoticeKind.REMINDER }.map { it.text },
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
    val started =
      assertNotNull(Navigation.start(listOf(knot24, knot45, knot50), closeGraph, null, fix(0.0)))

    val (navigation, notices) = ride(started.navigation, from = 600.0, to = 1100.0)

    assertEquals(knot50, navigation.nextKnot)
    assertEquals(
      listOf("45 ahead, then 50"),
      notices.filter { it.kind == NoticeKind.REMINDER }.map { it.text },
    )
  }

  // Requirement: Skipping ahead

  @Test
  fun `shortcut`() {
    // From 24 straight to 12, around 45 on a road 100 m south of it.
    val (navigation, notices) =
      start(east = 0.0)
        .navigation
        .ride(
          fix(500.0, north = -60.0),
          fix(1000.0, north = -90.0),
          fix(1500.0, north = -60.0),
          fix(1980.0),
          fix(2010.0),
        )

    assertEquals(knot31, navigation.nextKnot)
    assertTrue(notices.any { it.text == "Next 31" })
  }

  @Test
  fun `a route that passes a knot twice is followed in order`() {
    val twice = listOf(knot24, knot45, knot12, knot45, knot31)
    val started = start(east = 0.0, knots = twice, geometry = null)

    val (atFortyFive, _) = ride(started.navigation, from = 600.0, to = 1100.0)

    assertEquals(2, atFortyFive.nextIndex)
  }

  // Requirement: Off route

  @Test
  fun `passing a knot that is not on the route`() {
    val started = start(east = 0.0, geometry = null)

    val (navigation, notices) =
      started.navigation.ride(fix(300.0, north = 200.0), fix(500.0, north = 290.0))

    assertTrue(navigation.offRoute)
    assertEquals(listOf("Off route, next 45, 578 m to the south-east"), notices.map { it.text })
  }

  @Test
  fun `wrong turn between knots`() {
    val (navigation, notices) =
      start(east = 0.0)
        .navigation
        .ride(fix(300.0), fix(400.0, north = 60.0), fix(450.0, north = 130.0))

    assertTrue(navigation.offRoute)
    assertEquals(listOf("Off route, next 45, 565 m to the east"), notices.map { it.text })
  }

  @Test
  fun `back on route`() {
    val (offRoute, _) = start(east = 0.0).navigation.ride(fix(300.0), fix(400.0, north = 150.0))

    val (navigation, notices) = offRoute.ride(fix(500.0, north = 40.0))

    assertFalse(navigation.offRoute)
    assertEquals(listOf("Back on route, next 45"), notices.map { it.text })
  }

  @Test
  fun `back on route by passing a knot of the route`() {
    // Without courses, so only passing knots tells off and back on route: via 77 to 45.
    val started = start(east = 0.0, geometry = null)

    val (navigation, notices) =
      started.navigation.ride(
        fix(300.0, north = 200.0),
        fix(500.0, north = 290.0),
        fix(750.0, north = 150.0),
        fix(990.0, north = 10.0),
      )

    assertFalse(navigation.offRoute)
    assertEquals(knot12, navigation.nextKnot)
    assertTrue(notices.any { it.text == "Back on route, next 12" })
  }

  @Test
  fun `without geometry`() {
    val started = start(east = 0.0, geometry = null)

    val (navigation, notices) = started.navigation.ride(fix(300.0), fix(400.0, north = 150.0))

    assertFalse(navigation.offRoute)
    assertTrue(notices.isEmpty())
  }

  @Test
  fun `off route is announced once`() {
    val started = start(east = 0.0)

    val (navigation, notices) =
      started.navigation.ride(*(1..60).map { fix(300.0 + it, north = 150.0) }.toTypedArray())

    assertTrue(navigation.offRoute)
    assertEquals(1, notices.count { it.kind == NoticeKind.OFF_ROUTE })
  }

  @Test
  fun `a coarse fix far from the course only asks for a fine fix`() {
    val started = start(east = 0.0)

    val update = started.navigation.update(fix(300.0, north = 300.0, accuracy = 250.0))

    assertFalse(update.navigation.offRoute)
    assertTrue(update.notices.isEmpty())
    assertEquals(Precision.FINE, update.precision)
  }

  // Requirement: Arriving and stopping

  @Test
  fun `arriving`() {
    val started = start(east = 2000.0)

    val (navigation, notices) = ride(started.navigation, from = 2500.0, to = 3000.0)

    assertTrue(navigation.arrived)
    assertEquals("You've arrived", notices.last().text)
    assertEquals(Precision.COARSE, navigation.update(fix(3100.0)).precision)
  }

  // Requirement: Number to follow

  @Test
  fun `next knot in the middle of the route`() {
    val display = start(east = 0.0).navigation.display

    assertEquals("At 45 follow 12", display.text)
    assertEquals("12", display.follow)
  }

  @Test
  fun `next knot is the end`() {
    val display = start(east = 2000.0).navigation.display

    assertEquals("31 is the end", display.text)
    assertNull(display.follow)
  }

  @Test
  fun `off route text names the next knot`() {
    assertEquals(
      "Off route, next 24",
      start(east = 1000.0, north = -2000.0).navigation.display.offRouteText,
    )
  }

  // Requirement: Spoken notices

  @Test
  fun `number with a leading zero`() {
    val knot08 = knot(8, "08", east = 1000.0)
    val zeroGraph =
      NetworkGraph(knots = listOf(knot24, knot08), connections = listOf(Connection(24, 8, 1000)))

    val started = assertNotNull(Navigation.start(listOf(knot24, knot08), zeroGraph, null, fix(0.0)))

    assertEquals("Next 08", started.notices.single().text)
    assertEquals("Next 8", started.notices.single().speech)
    assertEquals("8", Navigation.spoken("08"))
    assertEquals("0", Navigation.spoken("00"))
    assertEquals("A1", Navigation.spoken("A1"))
  }

  // Precision, see the design's precision table

  @Test
  fun `far from the next knot and on route is coarse`() {
    assertEquals(Precision.COARSE, start(east = 0.0).navigation.update(fix(300.0)).precision)
  }

  @Test
  fun `close to the next knot is fine`() {
    assertEquals(Precision.FINE, start(east = 0.0).navigation.update(fix(700.0)).precision)
  }

  @Test
  fun `off route is fine`() {
    assertEquals(Precision.FINE, start(east = 1000.0, north = -2000.0).precision)
  }

  @Test
  fun `a coarse fix worse than the fine distance is fine`() {
    val update = start(east = 0.0).navigation.update(fix(100.0, accuracy = 500.0))

    assertEquals(Precision.FINE, update.precision)
  }

  @Test
  fun `the visible app is live`() {
    val navigation = start(east = 0.0).navigation

    assertEquals(Precision.LIVE, navigation.update(fix(300.0), visible = true).precision)
    assertEquals(Precision.LIVE, navigation.precision(visible = true))
  }

  private companion object {
    const val ORIGIN_LATITUDE = 52.0
    const val ORIGIN_LONGITUDE = 5.0
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180

    fun latitude(north: Double) = ORIGIN_LATITUDE + north / METERS_PER_DEGREE

    fun longitude(east: Double) =
      ORIGIN_LONGITUDE + east / (METERS_PER_DEGREE * cos(ORIGIN_LATITUDE * PI / 180))

    fun knot(id: Long, ref: String, east: Double, north: Double = 0.0) =
      Knot(id, ref, NetworkType.CYCLING, latitude(north), longitude(east))
  }
}
