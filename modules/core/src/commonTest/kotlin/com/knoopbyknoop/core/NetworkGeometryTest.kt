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
}
