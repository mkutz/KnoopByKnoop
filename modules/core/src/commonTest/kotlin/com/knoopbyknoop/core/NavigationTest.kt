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

  // Requirement: Joining the course on the way to the start (`join-route-before-first-knot`)

  @Test
  fun `reaching the course and following it`() {
    // Like the ride of 2026-11-08: started 250 m off, reaching the course between 45 and 12.
    val started = start(east = 1500.0, north = -250.0)
    assertEquals(knot24, started.navigation.nextKnot)

    val (navigation, notices) =
      started.navigation.ride(
        fix(1500.0, -100.0),
        fix(1500.0, -10.0),
        fix(1540.0, -5.0),
        fix(1600.0),
      )

    assertEquals(knot12, navigation.nextKnot)
    assertFalse(navigation.offRoute)
    assertEquals("Back on route, next 12", notices.last().text)
    assertNotNull(navigation.courseAhead, "the line ahead")
  }

  @Test
  fun `reaching the course says nothing until following it`() {
    val started = start(east = 1500.0, north = -250.0)

    val (navigation, notices) = started.navigation.ride(fix(1500.0, -10.0), fix(1510.0))

    assertEquals(knot24, navigation.nextKnot)
    assertTrue(navigation.offRoute)
    assertTrue(notices.none { it.kind == NoticeKind.BACK_ON_ROUTE })
  }

  @Test
  fun `crossing the course`() {
    val started = start(east = 1500.0, north = -250.0)

    val (navigation, notices) =
      started.navigation.ride(
        fix(1500.0, -40.0),
        fix(1500.0, 0.0),
        fix(1500.0, 40.0),
        fix(1500.0, 250.0),
      )

    assertEquals(knot24, navigation.nextKnot)
    assertTrue(navigation.offRoute)
    assertTrue(notices.none { it.kind == NoticeKind.BACK_ON_ROUTE })
  }

  @Test
  fun `reaching the course of the first leg near the start`() {
    val started = start(east = 300.0, north = -250.0)
    assertEquals(knot24, started.navigation.nextKnot)

    val (navigation, _) = started.navigation.ride(fix(300.0, -10.0), fix(360.0), fix(420.0))

    assertEquals(knot45, navigation.nextKnot)
  }

  @Test
  fun `following the course against the route`() {
    val started = start(east = 1500.0, north = -250.0)

    val (joined, _) = started.navigation.ride(fix(1500.0, -10.0), fix(1440.0))
    assertEquals(knot12, joined.nextKnot)
    val (riding, notices) = joined.ride(*(1..8).map { fix(1440.0 - it * 40.0) }.toTypedArray())

    assertTrue(
      notices.any { it.kind == NoticeKind.WRONG_WAY } || riding.wrongWay,
      "told they ride the wrong way: ${notices.map { it.text }}",
    )
  }

  @Test
  fun `joining without courses`() {
    val started = start(east = 1700.0, north = -250.0, geometry = null)

    assertEquals(knot12, started.navigation.nextKnot, "the nearest knot, as before")
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

    // Back within 30 m of the first leg's course, 80 m straight from 24 behind the rider.
    val (navigation, notices) = started.navigation.ride(fix(77.5, north = 20.0))

    assertEquals(knot24, navigation.nextKnot)
    assertFalse(navigation.offRoute)
    assertEquals("Back on route, next 24", notices.first().text)
    assertTrue(navigation.display.close)
    assertEquals("80 m", navigation.progress(navigation.lastFix).nextKnotDistance)
    assertEquals("80 m to 24", navigation.progressLine)
  }

  @Test
  fun `far from the next knot`() {
    val navigation = start(east = 0.0).navigation

    assertFalse(navigation.display.close)
    assertEquals("45", navigation.display.next)
    assertEquals("12", navigation.display.follow)
  }

  @Test
  fun `coming close`() {
    val started = start(east = 0.0).navigation
    val (far, _) = started.ride(fix(700.0))
    assertFalse(far.display.close)

    val update = far.update(fix(820.0))

    assertTrue(update.notices.any { it.kind == NoticeKind.REMINDER })
    assertTrue(update.navigation.display.close)
  }

  @Test
  fun `knot right after the last one`() {
    val knot50 = knot(50, "50", east = 1150.0)
    val closeGraph =
      NetworkGraph(
        knots = listOf(knot24, knot45, knot50, knot12),
        connections =
          listOf(Connection(24, 45, 1000), Connection(45, 50, 150), Connection(50, 12, 850)),
      )
    val started =
      assertNotNull(
        Navigation.start(listOf(knot24, knot45, knot50, knot12), closeGraph, null, fix(0.0))
      )

    val (navigation, _) = ride(started.navigation, from = 600.0, to = 1000.0)

    assertEquals(knot50, navigation.nextKnot)
    assertTrue(navigation.display.close)
  }

  @Test
  fun `next knot in the middle of the route is close within 200 m`() {
    val navigation = start(east = 850.0).navigation

    assertEquals("45", navigation.display.next)
    assertTrue(navigation.display.close)
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

    val (navigation, notices) = offRoute.ride(fix(500.0, north = 20.0))

    assertFalse(navigation.offRoute)
    assertEquals(listOf("Back on route, next 45"), notices.map { it.text })
  }

  @Test
  fun `parallel path`() {
    val (offRoute, _) = start(east = 0.0).navigation.ride(fix(300.0), fix(400.0, north = 150.0))

    val (navigation, notices) =
      offRoute.ride(fix(500.0, north = 60.0), fix(600.0, north = 60.0), fix(700.0, north = 60.0))

    assertTrue(navigation.offRoute)
    assertTrue(notices.none { it.text.startsWith("Back on route") }, notices.toString())
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

  // Requirement: Riding the line through a signpost

  private val branchPoint = GeoPoint(latitude(-300.0), longitude(500.0))

  /** The connection 24-45 also has a course bending 300 m south, further than off route is. */
  private val branchGeometry =
    NetworkGeometry(
      graph,
      geometry.courses,
      mapOf(
        graph.connections.indexOfFirst {
          24L in listOf(it.fromId, it.toId) && 45L in listOf(it.fromId, it.toId)
        } to
          listOf(
            SignpostCourse(
              0,
              0,
              listOf(
                GeoPoint(knot24.latitude, knot24.longitude),
                branchPoint,
                GeoPoint(knot45.latitude, knot45.longitude),
              ),
            )
          )
      ),
    )

  @Test
  fun `following the line shown`() {
    val (navigation, notices) =
      start(east = 0.0, geometry = branchGeometry)
        .navigation
        .ride(fix(250.0, north = -150.0), fix(500.0, north = -300.0), fix(750.0, north = -150.0))

    assertFalse(navigation.offRoute)
    assertTrue(notices.none { it.text.startsWith("Off route") }, notices.toString())
  }

  @Test
  fun `the other branch`() {
    val (navigation, _) = start(east = 0.0, geometry = branchGeometry).navigation.ride(fix(500.0))

    assertFalse(navigation.offRoute)
  }

  @Test
  fun `away from all courses`() {
    val (navigation, _) =
      start(east = 0.0, geometry = branchGeometry)
        .navigation
        .ride(fix(500.0, north = -300.0), fix(500.0, north = -500.0))

    assertTrue(navigation.offRoute)
  }

  // Requirement: Straight ways to a knot with several signposts

  /** A navigation with [knot] also tagged on a node, and the courses of [legs] as given. */
  private inner class Signposts(
    knot: Knot,
    east: Double,
    north: Double,
    knots: (Knot) -> List<Knot>,
    legs: Map<Pair<Long, Long>, List<GeoPoint>>,
  ) {
    val signpost = GeoPoint(latitude(north), longitude(east))
    val mergedGraph = mergedGraph(knot, east, north).first
    val merged = mergedGraph.knots.getValue(knot.id)
    val route = knots(merged)
    val geometry =
      NetworkGeometry(
        mergedGraph,
        mergedGraph.connections.map { connection ->
          legs[connection.fromId to connection.toId]
            ?: listOf(connection.fromId, connection.toId).map {
              point(mergedGraph.knots.getValue(it))
            }
        },
      )

    fun start(fix: Fix) = assertNotNull(Navigation.start(route, mergedGraph, geometry, fix))
  }

  private fun at(east: Double, north: Double) = GeoPoint(latitude(north), longitude(east))

  /** The route leaves 24 at a signpost 195 m north of 24's main position. */
  private fun leavingFirstKnotAtSignpost() =
    Signposts(
      knot24,
      east = 0.0,
      north = 195.0,
      { listOf(it, knot45, knot12) },
      mapOf((24L to 45L) to listOf(at(0.0, 0.0), at(0.0, 195.0), at(1000.0, 0.0))),
    )

  /** The route 24, 45, 12 reaches 45 at a signpost [north] m north of its main position. */
  private fun throughSignpostOf45(north: Double = 195.0) =
    Signposts(
      knot45,
      east = 1000.0,
      north = north,
      { listOf(knot24, it, knot12) },
      mapOf(
        (24L to 45L) to listOf(at(0.0, 0.0), at(1000.0, north), at(1000.0, 0.0)),
        (45L to 12L) to listOf(at(1000.0, 0.0), at(1000.0, north), at(2000.0, 0.0)),
      ),
    )

  @Test
  fun `approaching the first knot at the signpost the route leaves from`() {
    val signposts = leavingFirstKnotAtSignpost()
    val started = signposts.start(fix(-500.0, north = 115.0))
    assertTrue(started.navigation.offRoute)

    // Back on the course, 80 m from the signpost and 115 m from the main position.
    val near = fix(0.0, north = 115.0)
    val back = started.navigation.update(near).navigation
    assertFalse(back.offRoute)
    assertEquals(knot24.ref, back.nextKnot.ref)
    assertEquals("80 m", back.progress(near).nextKnotDistance)
    assertTrue(back.progressLine.startsWith("80 m to 24"), back.progressLine)

    val nearer = fix(0.0, north = 135.0)
    assertEquals("60 m", back.update(nearer).navigation.progress(nearer).nextKnotDistance)
  }

  @Test
  fun `dashed line to the first knot at the signpost the route leaves from`() {
    val signposts = leavingFirstKnotAtSignpost()
    val away = fix(-500.0, north = 195.0)

    val navigation = signposts.start(away).navigation

    assertTrue(navigation.offRoute)
    assertEquals(signposts.signpost, navigation.wayToRoute(away))
  }

  @Test
  fun `off route heading for a knot on the route at the signpost used`() {
    val signposts = throughSignpostOf45()
    val started = signposts.start(fix(0.0))
    // 1.2 km north of the signpost, 1.4 km from the main position.
    val away = fix(1000.0, north = 1395.0)

    val update = started.navigation.update(away)

    assertEquals(listOf("Off route, next 45, 1.2 km to the south"), update.notices.map { it.text })
    assertEquals("1.2 km", update.navigation.guidance(away)?.distance)
    assertEquals("1.2 km", update.navigation.progress(away).nextKnotDistance)
    assertTrue(update.navigation.progressLine.startsWith("1.2 km"), update.navigation.progressLine)
  }

  @Test
  fun `the route uses the other signpost`() {
    // The courses keep to the road, so the line reaches 45 at its main position.
    val signposts =
      Signposts(knot45, east = 1000.0, north = 195.0, { listOf(knot24, it, knot12) }, mapOf())
    val started = signposts.start(fix(0.0))
    val away = fix(1000.0, north = 1395.0)

    val navigation = started.navigation.update(away).navigation

    assertEquals("1.4 km", navigation.guidance(away)?.distance)
  }

  @Test
  fun `reminder at the signpost used`() {
    val signposts = throughSignpostOf45()
    val started = signposts.start(fix(0.0))

    // On the course: 305 m from the signpost, then 150 m from it and 222 m from the main position.
    val (navigation, notices) =
      started.navigation.ride(fix(700.0, north = 136.5), fix(853.0, north = 166.0))

    assertEquals(
      listOf("45 ahead, then 12"),
      notices.filter { it.kind == NoticeKind.REMINDER }.map { it.text },
    )
    assertEquals(
      listOf(),
      navigation.ride(fix(880.0, north = 171.6)).second.filter { it.kind == NoticeKind.REMINDER },
    )
  }

  /** The line runs through both places 45 is signposted at, on the way in or on the way out. */
  private fun throughBothSignpostsOf45(arrivingThroughSignpost: Boolean) =
    Signposts(
      knot45,
      east = 1000.0,
      north = 195.0,
      { listOf(knot24, it, knot12) },
      if (arrivingThroughSignpost)
        mapOf((24L to 45L) to listOf(at(0.0, 0.0), at(1000.0, 195.0), at(1000.0, 0.0)))
      else mapOf((45L to 12L) to listOf(at(1000.0, 0.0), at(1000.0, 195.0), at(2000.0, 0.0))),
    )

  @Test
  fun `line through both signposts reaching the other node first`() {
    val signposts = throughBothSignpostsOf45(arrivingThroughSignpost = true)
    val away = fix(1000.0, north = 1395.0)

    val update = signposts.start(fix(0.0)).navigation.update(away)

    assertEquals("1.2 km", update.navigation.guidance(away)?.distance)

    // On the line: 150 m from the signpost it gets to first, 222 m from the other.
    val (_, notices) =
      signposts
        .start(fix(0.0))
        .navigation
        .ride(fix(700.0, north = 136.5), fix(853.0, north = 166.0))
    assertEquals(
      listOf("45 ahead, then 12"),
      notices.filter { it.kind == NoticeKind.REMINDER }.map { it.text },
    )
  }

  @Test
  fun `line through both signposts reaching the main position first`() {
    val signposts = throughBothSignpostsOf45(arrivingThroughSignpost = false)
    val away = fix(1000.0, north = 1395.0)

    val update = signposts.start(fix(0.0)).navigation.update(away)

    assertEquals("1.4 km", update.navigation.guidance(away)?.distance)
  }

  @Test
  fun `passing the other signpost`() {
    val signposts = throughSignpostOf45()
    val started = signposts.start(fix(0.0))

    val update = started.navigation.update(fix(1000.0))

    assertEquals(knot12, update.navigation.nextKnot)
    assertEquals(listOf(NoticeKind.NEXT), update.notices.map { it.kind })
  }

  @Test
  fun `a knot with one signpost is measured to its position`() {
    val away = fix(1000.0, north = 1395.0)

    val navigation = start(east = 0.0).navigation.update(away).navigation

    assertEquals("1.4 km", navigation.guidance(away)?.distance)
    assertEquals(point(knot45), navigation.courseAhead?.last())
  }

  @Test
  fun `without courses the ways go to the main position`() {
    val signposts = throughSignpostOf45()
    val started =
      assertNotNull(Navigation.start(signposts.route, signposts.mergedGraph, null, fix(0.0)))
    val passing77 = fix(500.0, north = 300.0)
    val away = fix(1000.0, north = 1395.0)

    val navigation = started.navigation.ride(passing77, away).first

    assertTrue(navigation.offRoute)
    assertEquals("1.4 km", navigation.guidance(away)?.distance)
    assertEquals(point(knot45), navigation.wayToRoute(away))
  }

  @Test
  fun `sampling is fine near the signpost used`() {
    val signposts = throughSignpostOf45(north = 500.0)
    val started = signposts.start(fix(0.0))

    // On the course, 335 m from the signpost and 461 m from the main position.
    val navigation = started.navigation.update(fix(700.0, north = 350.0)).navigation

    assertEquals(Precision.FINE, navigation.precision(visible = false))
  }

  // Requirement: Leg ahead on the map

  private fun point(knot: Knot) = GeoPoint(knot.latitude, knot.longitude)

  @Test
  fun `which way at a fork`() {
    // Out to 45 and back: the course is the one in riding direction, also on the way back.
    val there = listOf(knot24, knot45, knot24)
    val started = assertNotNull(Navigation.start(there, graph, geometry, fix(0.0)))
    assertEquals(listOf(point(knot24), point(knot45)), started.navigation.courseAhead)

    val (back, _) = started.navigation.ride(fix(1000.0))

    assertEquals(knot24, back.nextKnot)
    assertEquals(listOf(point(knot45), point(knot24)), back.courseAhead)
  }

  @Test
  fun `starting on a stretch used twice`() {
    // Two connections between the knots, so the way back can run 5 m nearer to the rider.
    val knot46 = knot(46, "46", east = 1000.0)
    val graph =
      NetworkGraph(
        listOf(knot24, knot45, knot46),
        listOf(Connection(24, 45, 1000), Connection(45, 46, 10), Connection(46, 24, 1000)),
      )
    val geometry =
      NetworkGeometry(
        graph,
        listOf(
          listOf(point(knot24), GeoPoint(latitude(10.0), longitude(500.0)), point(knot45)),
          listOf(point(knot45), point(knot46)),
          listOf(point(knot46), GeoPoint(latitude(5.0), longitude(500.0)), point(knot24)),
        ),
      )

    val started =
      assertNotNull(
        Navigation.start(
          listOf(knot24, knot45, knot46, knot24),
          graph,
          geometry,
          fix(500.0, north = 5.0),
        )
      )

    assertEquals(1, started.navigation.nextIndex)
  }

  @Test
  fun `passing a knot moves the leg ahead`() {
    val (navigation, _) = start(east = 0.0).navigation.ride(fix(1000.0))

    assertEquals(knot12, navigation.nextKnot)
    assertEquals(listOf(point(knot45), point(knot12)), navigation.courseAhead)
  }

  @Test
  fun `before the first knot no leg is ahead`() {
    val navigation = start(east = -500.0, north = 300.0).navigation

    assertEquals(knot24, navigation.nextKnot)
    assertNull(navigation.courseAhead)
  }

  @Test
  fun `off route the leg ahead stays`() {
    val (navigation, _) = start(east = 0.0).navigation.ride(fix(300.0), fix(400.0, north = 150.0))

    assertTrue(navigation.offRoute)
    assertEquals(listOf(point(knot24), point(knot45)), navigation.courseAhead)
  }

  @Test
  fun `without courses the leg ahead is a straight line`() {
    val navigation = start(east = 0.0, geometry = null).navigation

    assertEquals(listOf(point(knot24), point(knot45)), navigation.courseAhead)
  }

  // app-languages, Requirement: Language change while navigating

  @Test
  fun `words and line outside the app`() {
    val (close, _) = start(east = 850.0).navigation.ride(fix(850.0))
    assertEquals("150 m to 45", close.progressLine.substringBefore(" ·"))

    val german = close.inLanguage(Language.DE, Language.DE, close.lastFix)

    assertEquals("Bei 45 folge 12", german.display.text)
    assertEquals("150 m bis 45", german.progressLine.substringBefore(" ·"))
  }

  @Test
  fun `off route in the new language`() {
    val (offRoute, _) = start(east = 0.0).navigation.ride(fix(300.0), fix(400.0, north = 150.0))

    val german = offRoute.inLanguage(Language.DE, Language.DE, offRoute.lastFix)

    assertTrue(german.offRoute)
    assertEquals("Abseits der Route, nächster 45", german.display.offRouteText)
    assertTrue(
      german.progressLine.contains("bis 45") || german.progressLine.contains("nach 45"),
      german.progressLine,
    )
    assertEquals(Language.DE, german.language)
  }

  @Test
  fun `later notices in the new language`() {
    val started = start(east = 0.0).navigation
    val german = started.inLanguage(Language.DE, Language.DE, started.lastFix)

    val (_, notices) = german.ride(fix(1000.0))

    assertEquals("Nächster Knotenpunkt 12, dann 31", notices.last().text)
    assertTrue(notices.last().speech.startsWith("Nächster Knotenpunkt"), notices.last().speech)
  }

  @Test
  fun `no voice for the new language`() {
    val started = start(east = 0.0).navigation
    val french = started.inLanguage(Language.FR, Language.EN, started.lastFix)

    val (_, notices) = french.ride(fix(1000.0))

    assertTrue(notices.last().text.startsWith("Prochain"), notices.last().text)
    assertTrue(notices.last().speech.startsWith("Next"), notices.last().speech)
  }

  @Test
  fun `nothing else changes with the language`() {
    val (riding, _) = start(east = 0.0).navigation.ride(fix(300.0))

    val german = riding.inLanguage(Language.DE, Language.DE, riding.lastFix)
    val (afterwards, notices) = german.ride(fix(320.0))

    assertEquals(riding.nextIndex, german.nextIndex)
    assertEquals(
      riding.progress(riding.lastFix).strip.rider,
      german.progress(german.lastFix).strip.rider,
    )
    assertFalse(afterwards.offRoute)
    assertEquals(listOf(), notices)
  }

  // Requirement: Riding the wrong way

  private val wrongWayText = "Wrong direction, turn around, next 45"

  private fun ridingBack() = start(east = 0.0).navigation.ride(fix(500.0), fix(340.0))

  @Test
  fun `riding back along the route`() {
    val (told, notices) = ridingBack()
    val (navigation, later) = told.ride(fix(300.0), fix(250.0), fix(200.0))

    assertTrue(navigation.wrongWay)
    assertFalse(navigation.offRoute)
    assertEquals(listOf(wrongWayText), notices.map { it.text })
    assertEquals(listOf(), later)
    assertEquals(NoticeKind.WRONG_WAY, notices.single().kind)
  }

  @Test
  fun `wrong direction in the panel`() {
    val (navigation, _) = ridingBack()
    val fix = fix(300.0)
    val (riding, _) = navigation.ride(fix)

    assertEquals("Wrong direction, next 45", riding.display.text)
    assertTrue(riding.display.wrongWay)
    assertEquals("700 m", riding.progress(fix).nextKnotDistance)
    assertTrue(riding.progress(fix).strip.offRoute)
    assertNull(riding.wayToRoute(fix))
    assertNull(riding.guidance(fix))
    assertEquals(Precision.FINE, riding.precision(visible = false))
  }

  @Test
  fun `in the app's language`() {
    val german = start(east = 0.0).navigation.inLanguage(Language.DE, Language.DE, fix(0.0))

    val (navigation, notices) = german.ride(fix(500.0), fix(340.0))

    assertEquals("Falsche Richtung, nächster 45", navigation.display.text)
    assertEquals(
      "Falsche Richtung, bitte wenden, nächster Knotenpunkt 45",
      notices.single().text,
    )
    val riding = NavigationDisplay("45", "12", offRoute = false, wrongWay = true)
    assertEquals(
      "Verkeerde richting, volgende 45",
      riding.copy(language = Language.NL).text,
    )
    assertEquals("Mauvaise direction, suivant 45", riding.copy(language = Language.FR).text)
  }

  @Test
  fun `turning around`() {
    val (told, _) = ridingBack()

    val (navigation, notices) = told.ride(fix(300.0), fix(360.0))

    assertFalse(navigation.wrongWay)
    assertEquals(listOf("Back on route, next 45"), notices.map { it.text })
    assertEquals("Next 45, then 12", navigation.display.copy(close = false).text)
  }

  @Test
  fun `turning back shortly`() {
    val (navigation, notices) =
      start(east = 0.0).navigation.ride(fix(500.0), fix(400.0), fix(600.0))

    assertFalse(navigation.wrongWay)
    assertEquals(listOf(), notices)
  }

  /** The course 24-45 runs back past itself 20 m further north, and on again 40 m north. */
  private val doublingBack =
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
            .map { (east, north) -> GeoPoint(latitude(north), longitude(east)) }
        else course
      },
    )

  @Test
  fun `a place that jumps`() {
    val (navigation, notices) =
      start(east = 0.0, geometry = doublingBack)
        .navigation
        .ride(
          fix(500.0),
          fix(505.0, north = 14.0),
          fix(510.0, north = 2.0),
          fix(600.0),
          fix(700.0),
        )

    assertFalse(navigation.wrongWay)
    assertEquals(listOf(), notices)
  }

  @Test
  fun `leaving the course while riding the wrong way`() {
    val (told, _) = ridingBack()

    val (navigation, notices) = told.ride(fix(300.0, north = 60.0), fix(300.0, north = 150.0))

    assertTrue(navigation.offRoute)
    assertFalse(navigation.wrongWay)
    assertEquals("Off route, next 45, 716 m to the east", notices.single().text)
  }

  @Test
  fun `passing the next knot while riding the wrong way`() {
    val (told, _) = ridingBack()

    val (navigation, notices) = told.ride(fix(1000.0))

    assertFalse(navigation.wrongWay)
    assertEquals(knot12, navigation.nextKnot)
    assertTrue(notices.any { it.text == "Next 12, then 31" }, notices.toString())
  }

  @Test
  fun `riding back again later`() {
    val (told, first) = ridingBack()

    val (navigation, notices) = told.ride(fix(300.0), fix(360.0), fix(600.0), fix(440.0))

    assertTrue(navigation.wrongWay)
    assertEquals(listOf(wrongWayText), first.map { it.text })
    assertEquals(listOf("Back on route, next 45", wrongWayText), notices.map { it.text })
  }

  @Test
  fun `on the way back of an out-and-back route`() {
    val (navigation, notices) =
      start(east = 0.0, knots = listOf(knot24, knot45, knot24))
        .navigation
        .ride(fix(500.0), fix(1000.0), fix(800.0), fix(600.0), fix(400.0))

    assertFalse(navigation.wrongWay)
    assertTrue(notices.none { it.kind == NoticeKind.WRONG_WAY }, notices.toString())
  }

  @Test
  fun `following the branch to another signpost`() {
    val (navigation, notices) =
      start(east = 0.0, geometry = branchGeometry)
        .navigation
        .ride(
          fix(250.0, north = -150.0),
          fix(500.0, north = -300.0),
          fix(400.0, north = -240.0),
          fix(750.0, north = -150.0),
        )

    assertFalse(navigation.wrongWay)
    assertTrue(notices.none { it.kind == NoticeKind.WRONG_WAY }, notices.toString())
  }

  @Test
  fun `riding the wrong way before the first knot`() {
    val started = start(east = -400.0)
    assertEquals(0, started.navigation.nextIndex)

    val (navigation, notices) = started.navigation.ride(fix(-200.0), fix(-400.0))

    assertFalse(navigation.wrongWay)
    assertTrue(notices.none { it.kind == NoticeKind.WRONG_WAY }, notices.toString())
  }

  @Test
  fun `riding the wrong way without geometry`() {
    val (navigation, notices) =
      start(east = 0.0, geometry = null).navigation.ride(fix(500.0), fix(340.0), fix(100.0))

    assertFalse(navigation.wrongWay)
    assertTrue(notices.none { it.kind == NoticeKind.WRONG_WAY }, notices.toString())
  }

  // Requirement: Way to the route on the map

  @Test
  fun `line to the route`() {
    val (offRoute, _) = start(east = 0.0).navigation.ride(fix(300.0), fix(400.0, north = 150.0))

    val (navigation, _) = offRoute.ride(fix(600.0, north = 150.0))

    assertNear(east = 600.0, north = 0.0, assertNotNull(navigation.wayToRoute(navigation.lastFix)))
  }

  @Test
  fun `route behind the rider`() {
    val (offRoute, _) = start(east = 0.0).navigation.ride(fix(300.0), fix(400.0, north = 150.0))

    val (navigation, _) = offRoute.ride(fix(100.0, north = 150.0))

    // The last place on the route, not back along the part already ridden.
    assertNear(east = 300.0, north = 0.0, assertNotNull(navigation.wayToRoute(navigation.lastFix)))
  }

  @Test
  fun `line to the knot`() {
    val (withoutCourses, _) =
      start(east = 0.0, geometry = null)
        .navigation
        .ride(fix(300.0, north = 200.0), fix(500.0, north = 290.0))
    assertTrue(withoutCourses.offRoute)
    assertNear(
      east = 1000.0,
      north = 0.0,
      assertNotNull(withoutCourses.wayToRoute(fix(500.0, 290.0))),
    )

    val beforeFirst = start(east = -500.0, north = 300.0).navigation
    assertTrue(beforeFirst.offRoute)
    assertNear(east = 0.0, north = 0.0, assertNotNull(beforeFirst.wayToRoute(fix(-500.0, 300.0))))
  }

  @Test
  fun `no line on the route`() {
    val (navigation, _) = start(east = 0.0).navigation.ride(fix(300.0))

    assertNull(navigation.wayToRoute(navigation.lastFix))
  }

  private fun assertNear(east: Double, north: Double, point: GeoPoint) {
    assertEquals(latitude(north), point.latitude, 1.0 / METERS_PER_DEGREE, "latitude")
    assertEquals(longitude(east), point.longitude, 2.0 / METERS_PER_DEGREE, "longitude")
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
    val display = start(east = 850.0).navigation.display

    assertEquals("At 45 follow 12", display.text)
    assertEquals("12", display.follow)
  }

  @Test
  fun `far from the next knot outside the app`() {
    val display = start(east = 0.0).navigation.display

    assertEquals("Next 45, then 12", display.text)
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

  // Requirement: Number to follow, the header's row

  private fun regioned(region: SignRegion?) = route.map { it.copy(signRegion = region) }

  private fun closeTo45(
    knots: List<Knot> = route,
    stops: Set<Int> = setOf(),
  ): Pair<Navigation, Fix> {
    val regionGraph =
      NetworkGraph(knots + knot77.copy(signRegion = knots.first().signRegion), graph.connections)
    val started =
      assertNotNull(Navigation.start(knots, regionGraph, geometry, fix(0.0), stops = stops))
    val (navigation, _) = ride(started.navigation, from = 40.0, to = 840.0)
    return navigation to fix(850.0)
  }

  @Test
  fun `next knot in the middle of the route in the header`() {
    val (navigation, fix) = closeTo45()

    val header = navigation.header(fix) as HeaderRow.Close

    assertEquals("45", header.next.ref)
    assertEquals("150 m", header.distance)
    assertEquals("12", header.follow.ref)
    assertTrue(header.follow.onPlate, "12 on a sign plate")
    assertFalse(header.next.onPlate)
  }

  @Test
  fun `far from the next knot in the header`() {
    val navigation = start(east = 0.0).navigation
    val fix = fix(100.0)

    val header = navigation.update(fix).navigation.header(fix) as HeaderRow.Far

    assertEquals("45", header.next.ref)
    assertEquals("900 m", header.distance)
    assertEquals("12", header.follow.ref)
    assertFalse(header.follow.onPlate, "no sign plate between knots")
  }

  @Test
  fun `coming close in the header`() {
    val navigation = start(east = 0.0).navigation
    val (far, _) = ride(navigation, from = 40.0, to = 760.0)
    val (close, _) = far.ride(fix(800.0))

    assertTrue(far.header(fix(760.0)) is HeaderRow.Far)
    assertTrue(close.header(fix(800.0)) is HeaderRow.Close)
  }

  @Test
  fun `knot right after the last one in the header`() {
    val knot50 = knot(50, "50", east = 1150.0)
    val closeGraph =
      NetworkGraph(
        knots = listOf(knot24, knot45, knot50, knot12),
        connections =
          listOf(Connection(24, 45, 1000), Connection(45, 50, 150), Connection(50, 12, 850)),
      )
    val started =
      assertNotNull(
        Navigation.start(listOf(knot24, knot45, knot50, knot12), closeGraph, null, fix(0.0))
      )
    val (navigation, _) = ride(started.navigation, from = 600.0, to = 1000.0)

    val header = navigation.header(fix(1000.0)) as HeaderRow.Close

    assertEquals("50", header.next.ref)
    assertEquals("12", header.follow.ref)
  }

  @Test
  fun `next knot is the end in the header`() {
    val (navigation, _) = ride(start(east = 0.0).navigation, from = 40.0, to = 2080.0)
    val fix = fix(2100.0)

    val header = navigation.update(fix).navigation.header(fix) as HeaderRow.End

    assertEquals("31", header.next.ref)
    assertEquals(KnotRole.END, header.next.role)
    assertEquals("900 m", header.distance)
  }

  @Test
  fun `distance to the first knot in the header`() {
    val started = start(east = -500.0, north = -1000.0).navigation
    val (navigation, _) = started.ride(fix(80.0))

    val header = navigation.header(fix(80.0))

    assertEquals(
      "80 m",
      (header as? HeaderRow.Close)?.distance ?: (header as HeaderRow.Far).distance,
    )
  }

  @Test
  fun `Dutch knots in the header`() {
    val (navigation, fix) = closeTo45(regioned(SignRegion.NETHERLANDS))

    val header = navigation.header(fix) as HeaderRow.Close

    assertTrue(header.next.filled, "45 filled, as at the knot")
    assertFalse(header.follow.filled, "12 white, as on the Dutch arrow signs")
    assertEquals(KnotPlate.of(SignRegion.NETHERLANDS), header.follow.plate)
  }

  @Test
  fun `German knots in the header`() {
    val (navigation, fix) = closeTo45(regioned(SignRegion.NORTH_RHINE_WESTPHALIA))

    val header = navigation.header(fix) as HeaderRow.Close

    assertTrue(header.next.filled)
    assertTrue(header.follow.filled)
    assertEquals(KnotPlate.of(SignRegion.NORTH_RHINE_WESTPHALIA), header.next.plate)
  }

  @Test
  fun `picked knot in the header`() {
    val (navigation, fix) = closeTo45(stops = setOf(2))

    val header = navigation.header(fix) as HeaderRow.Close

    assertEquals(KnotRole.STOP, header.follow.role)
    assertEquals(KnotRole.PASSED, header.next.role)
  }

  // Requirement: Direction to the route

  /** Off route 300 m south of the road at 700 m, after riding the road to 600 m. */
  private fun offRouteSouth(): Pair<Navigation, Fix> {
    val (onRoad, _) = ride(start(east = 0.0).navigation, from = 40.0, to = 600.0)
    val (off, _) = onRoad.ride(fix(650.0, north = -150.0), fix(700.0, north = -300.0))
    return off to fix(700.0, north = -300.0)
  }

  @Test
  fun `direction in the header`() {
    val (navigation, fix) = offRouteSouth()

    val header =
      navigation.header(fix, ridingDirection = 90.0, network = geometry) as HeaderRow.ToRoute

    assertTrue(header.offRoute, "dark: the rider was on the route")
    assertEquals(
      270.0,
      header.arrowDegrees,
      3.0,
      "riding east, the road to the north is to the left",
    )
  }

  @Test
  fun `back to the route and not to the knot`() {
    val (navigation, fix) = offRouteSouth()

    val header = navigation.header(fix) as HeaderRow.ToRoute

    assertEquals("300 m", header.distance, "to the road, not the 420 m to 45")
  }

  @Test
  fun `standing still in the header`() {
    val (navigation, fix) = offRouteSouth()

    val header = navigation.header(fix, ridingDirection = null) as HeaderRow.ToRoute

    assertEquals(0.0, normalized(header.arrowDegrees + 3.0) - 3.0, 3.0, "north, as on the map")
  }

  @Test
  fun `without courses in the header`() {
    val started = start(east = 0.0, geometry = null).navigation
    val (navigation, _) = started.ride(fix(250.0, north = 150.0), fix(500.0, north = 300.0))
    val fix = fix(500.0, north = 300.0)

    val header = navigation.header(fix, ridingDirection = null) as HeaderRow.ToRoute

    assertTrue(navigation.offRoute, "passed 77")
    assertEquals(121.0, header.arrowDegrees, 3.0, "to 45 to the east-south-east")
  }

  @Test
  fun `distance off route in the header`() {
    val (navigation, fix) = offRouteSouth()

    assertTrue(navigation.header(fix) is HeaderRow.ToRoute, "not the number to follow")
  }

  @Test
  fun `off the network`() {
    val (navigation, fix) = offRouteSouth()

    val header = navigation.header(fix, network = geometry) as HeaderRow.ToRoute

    assertNull(header.next)
  }

  @Test
  fun `wrong turn within the network`() {
    val started = start(east = 0.0).navigation
    val (navigation, _) =
      started.ride(fix(100.0, north = 60.0), fix(250.0, north = 150.0), fix(500.0, north = 300.0))
    val onCourse = fix(750.0, north = 150.0)
    val (riding, _) = navigation.ride(onCourse)

    val header = riding.header(onCourse, network = geometry) as HeaderRow.ToRoute

    assertEquals("45", header.next?.ref)
    assertFalse(header.next!!.onPlate)
  }

  // Requirement: Header on the way to the route

  @Test
  fun `starting at home`() {
    val navigation = start(east = 1000.0, north = -2000.0).navigation
    val fix = fix(1000.0, north = -2000.0)

    val header = navigation.header(fix, network = geometry) as HeaderRow.ToRoute

    assertFalse(header.offRoute, "not dark: no mistake")
    assertNull(header.next, "no path of the network here")
  }

  @Test
  fun `reaching the network on the way`() {
    val navigation = start(east = 1000.0, north = -2000.0).navigation
    val onCourseTo77 = fix(250.0, north = 150.0)
    val (riding, _) = navigation.ride(onCourseTo77)

    val header = riding.header(onCourseTo77, network = geometry) as HeaderRow.ToRoute

    assertFalse(header.offRoute)
    assertEquals("24", header.next?.ref)
  }

  @Test
  fun `on the route after starting away`() {
    val navigation = start(east = 1000.0, north = -2000.0).navigation
    val (riding, _) = navigation.ride(fix(-60.0, north = 10.0), fix(-20.0))

    assertFalse(riding.header(fix(-20.0)) is HeaderRow.ToRoute)
  }

  @Test
  fun `leaving the route later`() {
    val navigation = start(east = 1000.0, north = -2000.0).navigation
    val (joined, _) = navigation.ride(fix(-20.0), fix(10.0), fix(50.0))
    val (left, _) = joined.ride(fix(100.0, north = -150.0), fix(150.0, north = -300.0))

    val header = left.header(fix(150.0, north = -300.0)) as HeaderRow.ToRoute

    assertTrue(header.offRoute, "dark now")
  }

  @Test
  fun `wrong direction in the header`() {
    val (navigation, _) = ridingBack()
    val fix = fix(300.0)
    val (riding, _) = navigation.ride(fix)

    val header = riding.header(fix) as HeaderRow.WrongWay

    assertEquals("45", header.next.ref)
    assertEquals("700 m", header.distance)
  }

  // Requirement: Number to follow, screen readers

  @Test
  fun `far from the next knot with a screen reader`() {
    val navigation = start(east = 0.0).navigation
    val fix = fix(100.0)

    assertEquals("Next 45, 900 m, then 12", navigation.update(fix).navigation.header(fix).spoken)
  }

  @Test
  fun `close to the next knot with a screen reader`() {
    val (navigation, fix) = closeTo45()

    assertEquals("At 45 follow 12 straight on, 150 m", navigation.header(fix).spoken)
  }

  @Test
  fun `direction with a screen reader`() {
    val (navigation, fix) = offRouteSouth()

    assertEquals("Off route, next 45, 300 m to the north", navigation.header(fix).spoken)
  }

  @Test
  fun `on the way with a screen reader`() {
    val navigation = start(east = 1000.0, north = -2000.0).navigation

    assertEquals(
      "Next 24, 2.2 km to the north-west",
      navigation.header(fix(1000.0, north = -2000.0)).spoken,
    )
  }

  // Requirement: Navigation with the screen off, without words

  @Test
  fun `far from the next knot outside the app without words`() {
    val navigation = start(east = 0.0).navigation
    val fix = fix(100.0)

    val outside = navigation.update(fix).navigation.outside(fix)

    assertEquals("45 · 900 m › 12", outside.title)
    assertEquals("45", outside.chip)
    assertFalse(outside.inverted)
  }

  @Test
  fun `far from the next knot outside the app in German`() {
    val german = start(east = 0.0).navigation.inLanguage(Language.DE, Language.DE, fix(0.0))
    val fix = fix(100.0)

    assertEquals("45 · 900 m › 12", german.update(fix).navigation.outside(fix).title)
  }

  @Test
  fun `lock screen close to a knot`() {
    val (navigation, fix) = closeTo45()

    val outside = navigation.outside(fix)

    assertEquals("45 → ↑12", outside.title, "straight on at 45")
    assertEquals("150 m", outside.line, "no end arrival without the local time")
    assertEquals("→12", outside.chip)
  }

  @Test
  fun `off route outside the app`() {
    val (navigation, fix) = offRouteSouth()

    val outside = navigation.outside(fix, ridingDirection = 90.0)

    assertEquals("← 300 m", outside.title)
    assertEquals("←300 m", outside.chip)
    assertTrue(outside.inverted)
  }

  @Test
  fun `on the way to the route outside the app`() {
    val navigation = start(east = 1000.0, north = -2000.0).navigation

    val outside = navigation.outside(fix(1000.0, north = -2000.0), ridingDirection = null)

    assertEquals("↖ 2.2 km", outside.title)
    assertFalse(outside.inverted, "no mistake")
  }

  @Test
  fun `wrong direction outside the app`() {
    val (navigation, _) = ridingBack()
    val fix = fix(300.0)
    val (riding, _) = navigation.ride(fix)

    val outside = riding.outside(fix)

    assertEquals("↩ 45 · 700 m", outside.title)
    assertEquals("↩45", outside.chip)
    assertTrue(outside.inverted)
  }

  @Test
  fun `updated when the arrow turns`() {
    val (navigation, fix) = offRouteSouth()

    val riding = navigation.outside(fix, ridingDirection = 90.0)
    val turned = navigation.outside(fix, ridingDirection = 140.0)

    assertEquals("←", riding.chip.take(1))
    assertEquals("↙", turned.chip.take(1), "turned by 50°, so the arrow changes")
  }

  @Test
  fun `chips fit 7 characters`() {
    val (close, closeFix) = closeTo45()
    val (off, offFix) = offRouteSouth()
    listOf(close.outside(closeFix), off.outside(offFix, ridingDirection = 90.0)).forEach {
      assertTrue(it.chip.length <= 7, it.chip)
    }
  }

  @Test
  fun `guided way on the lock screen`() {
    val row =
      HeaderRow.ToRoute(
        offRoute = true,
        arrowDegrees = 0.0,
        distance = "800 m",
        next = null,
        guided = GuidedHeader(TurnDirection.LEFT, "120 m", "Weierstraße", "1.9 km", ""),
        spoken = "",
      )

    assertEquals("↰ 120 m · 1.9 km", row.outside(null).title)
  }

  // Requirement: Turn at the next knot. Routes 24 → 45 → a third knot, riding east to 150 m
  // before 45 at 1000 m.

  private fun turnAt45(vararg after: Knot, geometry: Boolean = true): HeaderRow.Close {
    val knots = listOf(knot24, knot45) + after
    val turnGraph =
      NetworkGraph(
        knots.distinct(),
        knots.zipWithNext { from, to -> Connection(from.id, to.id, 1000) },
      )
    val started =
      assertNotNull(
        Navigation.start(
          knots,
          turnGraph,
          if (geometry) NetworkGeometry.straightLines(turnGraph) else null,
          fix(0.0),
        )
      )
    val (navigation, _) = ride(started.navigation, from = 40.0, to = 840.0)
    return navigation.header(fix(850.0)) as HeaderRow.Close
  }

  @Test
  fun `right turn at a knot`() {
    val close = turnAt45(knot(60, "60", east = 1000.0, north = -1000.0))

    assertEquals(TurnDirection.RIGHT, close.turn)
    assertEquals("45 → ↱60", close.outside(null).title)
  }

  @Test
  fun `straight on at a knot`() {
    assertEquals(TurnDirection.STRAIGHT, turnAt45(knot12).turn)
  }

  @Test
  fun `slight left at a knot`() {
    val angle = 40 * PI / 180
    val close =
      turnAt45(
        knot(61, "61", east = 1000.0 + 1000 * cos(angle), north = 1000 * kotlin.math.sin(angle))
      )

    assertEquals(TurnDirection.SLIGHT_LEFT, close.turn)
  }

  @Test
  fun `turning back at an out-and-back knot`() {
    assertEquals(TurnDirection.BACK, turnAt45(knot24).turn)
  }

  @Test
  fun `knot right after the next one turns across it`() {
    // 46 is 10 m after 45, and the route turns right there.
    val close =
      turnAt45(knot(46, "46", east = 1010.0), knot(62, "62", east = 1010.0, north = -1000.0))

    assertEquals(TurnDirection.RIGHT, close.turn, "measured over the 40 m after 45, across 46")
  }

  @Test
  fun `without courses no turn`() {
    assertNull(turnAt45(knot(60, "60", east = 1000.0, north = -1000.0), geometry = false).turn)
  }

  @Test
  fun `start knot without a turn`() {
    val started = start(east = -150.0)

    assertNull(started.navigation.turnAt(0))
  }

  @Test
  fun `far from the knot no turn`() {
    val navigation = start(east = 0.0).navigation
    val fix = fix(100.0)

    assertTrue(navigation.update(fix).navigation.header(fix) is HeaderRow.Far, "no plate, no turn")
  }

  @Test
  fun `turn with a screen reader`() {
    val close = turnAt45(knot(60, "60", east = 1000.0, north = -1000.0))

    assertEquals("At 45 follow 60 to the right, 150 m", close.spoken)
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
