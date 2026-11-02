package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot45
import com.knoopbyknoop.core.TestNetwork.knot7
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** Covers the geometry scenarios of `openspec/changes/add-connection-geometry`. */
class NetworkGeometryTest {

  private val graphBytes =
    GraphFile(NetworkType.CYCLING, "utrecht", "2026-10-25", "", graph).toBytes()
  private val graphSha256 = sha256Hex(graphBytes)

  /** Every connection bends through a point north of its midpoint. */
  private val geometry =
    NetworkGeometry(
      graph,
      graph.connections.map { connection ->
        val from = graph.knots.getValue(connection.fromId)
        val to = graph.knots.getValue(connection.toId)
        listOf(
          point(from),
          GeoPoint((from.latitude + to.latitude) / 2 + 0.001, (from.longitude + to.longitude) / 2),
          point(to),
        )
      },
    )

  private fun point(knot: Knot) = GeoPoint(knot.latitude, knot.longitude)

  private fun assertSamePoints(expected: List<GeoPoint>, actual: List<GeoPoint>?) {
    assertEquals(expected.size, actual?.size, "number of points")
    expected.zip(actual!!).forEach { (expectedPoint, actualPoint) ->
      assertTrue(
        abs(expectedPoint.latitude - actualPoint.latitude) < 0.000001 &&
          abs(expectedPoint.longitude - actualPoint.longitude) < 0.000001,
        "$actualPoint should be $expectedPoint",
      )
    }
  }

  @Test
  fun `a written geometry file reads back the same`() {
    val read = NetworkGeometry.read(geometry.toBytes(graphSha256), graph, graphSha256)

    geometry.courses.zip(read.courses).forEach { (expected, actual) ->
      assertSamePoints(expected, actual)
    }
  }

  // Requirement: Geometry belongs to one graph

  @Test
  fun `matching files`() {
    val bytes = geometry.toBytes(graphSha256)

    assertTrue(NetworkGeometry.belongsTo(bytes, graphSha256))
    val read = NetworkGeometry.read(bytes, graph, graphSha256)
    assertSamePoints(geometry.courseOf(knot12, knot7)!!, read.courseOf(knot12, knot7))
  }

  @Test
  fun `mismatching or missing geometry`() {
    val otherGraphSha256 = sha256Hex("another build".encodeToByteArray())
    val bytes = geometry.toBytes(otherGraphSha256)

    assertFalse(NetworkGeometry.belongsTo(bytes, graphSha256))
    assertFailsWith<GraphFormatException> { NetworkGeometry.read(bytes, graph, graphSha256) }

    // Without matching geometry, routing is unchanged and connections are straight lines.
    val route = Route.plan(listOf(knot12, knot45), ShortestPathRouter(graph))
    assertEquals(RouteSummary.Complete(listOf(knot12, knot7, knot45), 3500), route.summary)
    assertEquals(
      listOf(point(knot12), point(knot7), point(knot45)),
      NetworkGeometry.straightLines(graph).courseOf(route),
    )
  }

  @Test
  fun `other files are no geometry`() {
    assertFalse(NetworkGeometry.belongsTo(graphBytes, graphSha256))
  }

  // Requirement: Course of every connection

  @Test
  fun `route in riding direction`() {
    // Connections are stored as 12→7 and 7→45; the route runs the other way.
    val route = Route.plan(listOf(knot45, knot12), ShortestPathRouter(graph))

    val course = geometry.courseOf(route)!!

    assertEquals(point(knot45), course.first())
    assertEquals(point(knot12), course.last())
    assertTrue(point(knot7) in course)
    assertEquals(5, course.size, "the shared point at knot 7 appears once")
  }

  @Test
  fun `an incomplete route has no course`() {
    val route = Route.plan(listOf(knot12, TestNetwork.knot99), ShortestPathRouter(graph))

    assertNull(geometry.courseOf(route))
  }

  @Test
  fun `knots without a direct connection have no course between them`() {
    assertNull(geometry.courseOf(knot12, TestNetwork.knot99))
  }

  // Requirement: Knots signposted at several places

  private val river = GeoPoint(52.1000, 5.1000)
  private val path = GeoPoint(52.1006, 5.0995)
  private val signpost = GeoPoint(52.1012, 5.0985)
  private val west = GeoPoint(52.1020, 5.0900)
  private val east = GeoPoint(52.0990, 5.1100)
  private val knot02 =
    Knot(2, "02", NetworkType.CYCLING, river.latitude, river.longitude, listOf(signpost))
  private val knot28 = Knot(28, "28", NetworkType.CYCLING, west.latitude, west.longitude)
  private val knot01 = Knot(1, "01", NetworkType.CYCLING, east.latitude, east.longitude)
  private val signposts =
    NetworkGraph(
      listOf(knot02, knot28, knot01),
      listOf(Connection(2, 28, 700), Connection(2, 1, 800)),
    )
  private val signpostGeometry =
    NetworkGeometry(signposts, listOf(listOf(river, path, signpost, west), listOf(river, east)))

  private fun routeThrough(vararg knots: Knot) =
    Route.plan(knots.toList(), ShortestPathRouter(signposts))

  @Test
  fun `route starting at the other signpost`() {
    assertEquals(listOf(signpost, west), signpostGeometry.courseOf(routeThrough(knot02, knot28)))
  }

  @Test
  fun `route ending at the other signpost`() {
    assertEquals(listOf(west, signpost), signpostGeometry.courseOf(routeThrough(knot28, knot02)))
  }

  @Test
  fun `route leaving at the main position`() {
    assertEquals(listOf(river, east), signpostGeometry.courseOf(routeThrough(knot02, knot01)))
  }

  @Test
  fun `no shared signpost`() {
    assertEquals(
      listOf(west, signpost, path, river, east),
      signpostGeometry.courseOf(routeThrough(knot28, knot02, knot01)),
    )
  }

  // Requirement: Routes through a knot with several signposts

  private val fork = GeoPoint(52.1020, 5.1010)
  private val knot06 = Knot(6, "06", NetworkType.CYCLING, 52.1100, 5.1100)
  private val six = GeoPoint(knot06.latitude, knot06.longitude)
  private val branches =
    NetworkGraph(
      listOf(knot02, knot28, knot01, knot06),
      listOf(Connection(2, 28, 700), Connection(2, 1, 800), Connection(6, 2, 1400)),
    )
  private val branchGeometry =
    NetworkGeometry(
      branches,
      listOf(listOf(river, path, signpost, west), listOf(river, east), listOf(six, fork, river)),
      mapOf(
        2 to
          listOf(
            SignpostCourse(0, 0, listOf(six, fork, river)),
            SignpostCourse(0, 1, listOf(six, fork, signpost)),
          )
      ),
    )

  private fun branchRoute(vararg knots: Knot) =
    Route.plan(knots.toList(), ShortestPathRouter(branches))

  @Test
  fun `through the signpost of the next leg`() {
    assertEquals(
      listOf(six, fork, signpost, west),
      branchGeometry.courseOf(branchRoute(knot06, knot02, knot28)),
    )
  }

  @Test
  fun `the other direction out of the knot`() {
    assertEquals(
      listOf(six, fork, river, east),
      branchGeometry.courseOf(branchRoute(knot06, knot02, knot01)),
    )
  }

  @Test
  fun `a stop at the knot`() {
    val session = PlanningSession.start(branches).tap(knot06).tap(knot28).tap(knot02)
    assertEquals(listOf(knot06, knot02, knot28), session.waypoints)

    val legs =
      Json.parseToJsonElement(MapFeatures.route(session, branchGeometry))
        .jsonObject
        .getValue("features")
        .jsonArray
        .map {
          it.jsonObject.getValue("geometry").jsonObject.getValue("coordinates").jsonArray.size
        }

    assertEquals(listOf(3, 2), legs, "06, fork, signpost; signpost, 28")
  }

  @Test
  fun `back the same way`() {
    assertEquals(
      listOf(west, signpost, west),
      signpostGeometry.courseOf(routeThrough(knot28, knot02, knot28)),
    )
  }

  @Test
  fun `same distance`() {
    assertEquals(
      RouteSummary.Complete(listOf(knot06, knot02, knot28), 2100),
      branchRoute(knot06, knot02, knot28).summary,
    )
  }

  // Requirement: Courses to each signpost

  @Test
  fun `the courses to each signpost survive the file`() {
    val graphBytes = GraphFile(NetworkType.CYCLING, "test", "2026-10-25", "", branches).toBytes()
    val sha = sha256Hex(graphBytes)

    val read = NetworkGeometry.read(branchGeometry.toBytes(sha), branches, sha)

    assertEquals(setOf(2), read.signpostCourses.keys)
    assertEquals(
      listOf(0 to 0, 0 to 1),
      read.signpostCourses.getValue(2).map { it.fromPosition to it.toPosition },
    )
    assertSamePoints(
      listOf(six, fork, signpost),
      read.signpostCourses.getValue(2)[1].course,
    )
    assertEquals(
      listOf(listOf(six, fork, river), listOf(six, fork, signpost)).map { it.size },
      read.signpostCoursesOf(knot06, knot02).map { it.size },
    )
  }

  @Test
  fun `a file without the courses to each signpost`() {
    val graphBytes = GraphFile(NetworkType.CYCLING, "test", "2026-10-25", "", branches).toBytes()
    val sha = sha256Hex(graphBytes)
    val bytes = NetworkGeometry(branches, branchGeometry.courses).toBytes(sha)

    // A file of an earlier version ends right after the courses, before the section's count.
    val read = NetworkGeometry.read(bytes.copyOf(bytes.size - 1), branches, sha)

    assertEquals(branches.connections.size, read.courses.size)
    assertEquals(mapOf(), read.signpostCourses)
  }

  @Test
  fun `a course passing the signpost much later is not cut`() {
    val farAway = GeoPoint(52.1100, 5.1300)
    val graph = NetworkGraph(listOf(knot02, knot28), listOf(Connection(2, 28, 5000)))
    val geometry = NetworkGeometry(graph, listOf(listOf(river, farAway, signpost, west)))

    val route = Route.plan(listOf(knot02, knot28), ShortestPathRouter(graph))

    assertEquals(listOf(river, farAway, signpost, west), geometry.courseOf(route))
  }

  @Test
  fun `the route on the map starts at the signpost it uses`() {
    val session = PlanningSession.start(signposts).tap(knot02).tap(knot28)

    val coordinates =
      Json.parseToJsonElement(MapFeatures.route(session, signpostGeometry))
        .jsonObject
        .getValue("features")
        .jsonArray
        .single()
        .jsonObject
        .getValue("geometry")
        .jsonObject
        .getValue("coordinates")
        .jsonArray

    assertEquals(2, coordinates.size)
  }
}
