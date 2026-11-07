package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers `map-planning` "Route on the side of its direction" of
 * `openspec/changes/follow-route-directions`.
 *
 * 28 in the west, 02 1 km east of it. The course from 28 to 02 runs straight; the course back runs
 * on a way 100 m north of it, like the other side of a wide road.
 */
class RouteDirectionsTest {

  private fun latitude(north: Double) = 52.0 + north / 111_195.0

  private fun longitude(east: Double) = 5.0 + east / (111_195.0 * cos(52.0 * PI / 180))

  private fun point(east: Double, north: Double = 0.0) = GeoPoint(latitude(north), longitude(east))

  private val knot28 = Knot(28, "28", NetworkType.CYCLING, latitude(0.0), longitude(0.0))
  private val knot02 = Knot(2, "02", NetworkType.CYCLING, latitude(0.0), longitude(1000.0))
  private val graph = NetworkGraph(listOf(knot28, knot02), listOf(Connection(28, 2, 1000)))

  private val there = listOf(point(0.0), point(500.0), point(1000.0))
  private val back = listOf(point(1000.0), point(900.0, 100.0), point(100.0, 100.0), point(0.0))
  private val geometry =
    NetworkGeometry(graph, listOf(there), coursesBack = mapOf(0 to CourseBack(back)))

  private fun routeThrough(vararg knots: Knot) =
    PlanningSession.start(graph, knots.toList()).route!!

  @Test
  fun `route on its side of the road`() {
    assertEquals(there, geometry.courseOf(knot28, knot02))
    assertEquals(back, geometry.courseOf(knot02, knot28))
    assertEquals(back, geometry.courseOf(routeThrough(knot02, knot28)))
  }

  @Test
  fun `out and back`() {
    val line = assertNotNull(geometry.courseOf(routeThrough(knot28, knot02, knot28)))

    assertEquals(there + back.drop(1), line)
  }

  @Test
  fun `line ahead and off route`() {
    var time = 0L
    fun fix(east: Double, north: Double) =
      Fix(latitude(north), longitude(east), 5.0, (time++) * 5_000)
    val started =
      assertNotNull(Navigation.start(listOf(knot02, knot28), graph, geometry, fix(1000.0, 0.0)))
        .navigation

    assertEquals(back, started.courseAhead)
    // Along the way back, 100 m north of the course there: never off route.
    val riding =
      (9 downTo 1)
        .map { fix(it * 100.0, 100.0) }
        .fold(started) { navigation, fix ->
          navigation.update(fix).navigation.also { assertFalse(it.offRoute, "off route at $fix") }
        }
    assertTrue(abs(riding.lastFix.longitude - longitude(100.0)) < 0.0001)
  }

  @Test
  fun `from a route point on the way back`() {
    // A stop halfway along 28–02, then on to 28: the rest of the way runs on the course back.
    val place = point(500.0)
    val stop =
      routePointKnot(place, NetworkType.CYCLING, CoursePlace(graph.connections[0], 0.5, place, 0.0))
    val session = PlanningSession.start(graph, listOf(stop, knot28))

    val line = assertNotNull(geometry.withRoutePoints(session.routeGraph).courseOf(session.route!!))

    assertTrue(point(100.0, 100.0) in line, "on the course back, not $line")
    assertEquals(point(0.0), line.last())
  }

  @Test
  fun `to a route point on the way back`() {
    // From 02 to a stop halfway along 28–02: the way there runs on the course back too.
    val place = point(500.0)
    val stop =
      routePointKnot(place, NetworkType.CYCLING, CoursePlace(graph.connections[0], 0.5, place, 0.0))
    val session = PlanningSession.start(graph, listOf(knot02, stop))

    val line = assertNotNull(geometry.withRoutePoints(session.routeGraph).courseOf(session.route!!))

    assertTrue(point(900.0, 100.0) in line, "on the course back, not $line")
    assertEquals(point(1000.0), line.first())
  }

  @Test
  fun `geometry without courses back`() {
    val plain = NetworkGeometry(graph, listOf(there))

    assertEquals(there.asReversed(), plain.courseOf(knot02, knot28))
  }
}

/**
 * Covers `network-data` "Courses to each signpost in each direction": 02 is signposted at its
 * position and 30 m north of it; the signpost courses back run 100 m north of the courses there.
 */
class SignpostDirectionsTest {

  private fun latitude(north: Double) = 52.0 + north / 111_195.0

  private fun longitude(east: Double) = 5.0 + east / (111_195.0 * cos(52.0 * PI / 180))

  private fun point(east: Double, north: Double = 0.0) = GeoPoint(latitude(north), longitude(east))

  private val knot28 = Knot(28, "28", NetworkType.CYCLING, latitude(0.0), longitude(0.0))
  private val knot02 =
    Knot(
      2,
      "02",
      NetworkType.CYCLING,
      latitude(0.0),
      longitude(1000.0),
      listOf(point(1000.0, 30.0)),
    )
  private val graph = NetworkGraph(listOf(knot28, knot02), listOf(Connection(28, 2, 1000)))

  private val toMain = listOf(point(0.0), point(1000.0))
  private val toSignpost = listOf(point(0.0), point(1000.0, 30.0))
  private val backFromSignpost = listOf(point(1000.0, 30.0), point(500.0, 130.0), point(0.0))
  private val backFromMain = listOf(point(1000.0), point(500.0, 100.0), point(0.0))
  private val geometry =
    NetworkGeometry(
      graph,
      listOf(toMain),
      mapOf(
        0 to
          listOf(
            SignpostCourse(0, 0, toMain, back = CourseBack(backFromMain)),
            SignpostCourse(0, 1, toSignpost, back = CourseBack(backFromSignpost)),
          )
      ),
      coursesBack = mapOf(0 to CourseBack(backFromMain)),
    )

  @Test
  fun `the way back takes a signpost course back`() {
    val leg = geometry.courses(listOf(knot02, knot28)).single()

    assertTrue(leg == backFromMain || leg == backFromSignpost, "a course back, not $leg")
  }

  @Test
  fun `signpost courses back survive the file`() {
    val sha = sha256Hex(GraphFile(NetworkType.CYCLING, "test", "2026-11-06", "", graph).toBytes())

    val read = NetworkGeometry.read(geometry.toBytes(sha), graph, sha)

    assertEquals(
      listOf(3, 3),
      read.signpostCourses.getValue(0).map { assertNotNull(it.back).course.size },
    )
  }

  /** Like Zange: 02's main signpost has no way back from 28, its second one no way there. */
  private val oneWays =
    NetworkGeometry(
      graph,
      listOf(toMain),
      mapOf(
        0 to
          listOf(
            SignpostCourse(0, 0, toMain, oneWay = OneWay.THERE),
            SignpostCourse(0, 1, backFromSignpost.asReversed(), oneWay = OneWay.BACK),
          )
      ),
      coursesBack = mapOf(0 to CourseBack(backFromSignpost + listOf(point(1000.0)))),
    )

  @Test
  fun `a signpost course is not ridden against its only direction`() {
    assertEquals(listOf(toMain), oneWays.courses(listOf(knot28, knot02)))
    // Back from 02, the main signpost's course reversed would be shortest, but it has no way back.
    val back = oneWays.courses(listOf(knot02, knot28)).single()
    assertTrue(back != toMain.asReversed(), "not the main signpost's course reversed")
  }

  @Test
  fun `one-way signpost courses survive the file`() {
    val sha = sha256Hex(GraphFile(NetworkType.CYCLING, "test", "2026-11-06", "", graph).toBytes())

    val read = NetworkGeometry.read(oneWays.toBytes(sha), graph, sha)

    assertEquals(
      listOf(OneWay.THERE, OneWay.BACK),
      read.signpostCourses.getValue(0).map { it.oneWay },
    )
  }

  @Test
  fun `a file without the section reads with none`() {
    val sha = sha256Hex(GraphFile(NetworkType.CYCLING, "test", "2026-11-06", "", graph).toBytes())
    val plain =
      NetworkGeometry(
        graph,
        listOf(toMain),
        mapOf(0 to listOf(SignpostCourse(0, 0, toMain), SignpostCourse(0, 1, toSignpost))),
      )
    val bytes = plain.toBytes(sha)

    // The courses back's version ends before the count of the signpost courses back.
    val read = NetworkGeometry.read(bytes.copyOf(bytes.size - 1), graph, sha)

    assertTrue(read.signpostCourses.getValue(0).all { it.back == null })
  }
}
