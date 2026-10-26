package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.NetworkType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Covers the course scenarios of `openspec/changes/add-connection-geometry`. */
class CourseTest {

  private val nodeNetwork = mapOf("network:type" to "node_network")

  private fun knot(id: Long, latitude: Double, longitude: Double, ref: String) =
    OsmNode(id, latitude, longitude, nodeNetwork + ("rcn_ref" to ref))

  private fun route(id: Long, vararg wayIds: Long) =
    OsmRelation(id, nodeNetwork + mapOf("type" to "route", "network" to "rcn"), wayIds.toList())

  private fun extract(relations: List<OsmRelation>, ways: List<OsmWay>, nodes: List<OsmNode>) =
    NetworkExtraction()
      .apply {
        relations.forEach(::acceptRelation)
        ways.forEach(::acceptWay)
        nodes.forEach(::acceptNode)
      }
      .result()

  @Test
  fun `winding path`() {
    // A 2 km path heading east that winds 50 m north and south four times, a node every 5 m.
    val nodes =
      (0..400).map { step ->
        val east = step * 5.0
        val north = 50 * sin(east / 2000 * 8 * PI)
        OsmNode(
          1000L + step,
          52.0 + north / METERS_PER_DEGREE,
          5.0 + east / (METERS_PER_DEGREE * cos(Math.toRadians(52.0))),
        )
      }
    val knots =
      listOf(
        knot(1000, nodes.first().latitude, nodes.first().longitude, "12"),
        knot(1400, nodes.last().latitude, nodes.last().longitude, "45"),
      )
    val result =
      extract(
        relations = listOf(route(10, 100)),
        ways = listOf(OsmWay(100, nodes.map { it.id })),
        nodes = knots + nodes.drop(1).dropLast(1),
      )

    val course = result.courses.getValue(NetworkType.CYCLING).values.single()
    val simplified = simplify(course, COURSE_TOLERANCE_METERS)

    assertEquals(401, course.size)
    assertTrue(simplified.size in 10..100, "${simplified.size} points keep the curves")
    for (node in nodes) {
      val distance = distanceToLineMeters(GeoPoint(node.latitude, node.longitude), simplified)
      assertTrue(distance <= COURSE_TOLERANCE_METERS + 0.01, "node ${node.id} is $distance m off")
    }
  }

  @Test
  fun `junction tagged on several nodes`() {
    // Junction 30 is tagged on nodes 2 and 3, 33 m apart; the route to 45 leaves from node 3.
    val result =
      extract(
        relations = listOf(route(10, 100), route(11, 101)),
        ways = listOf(OsmWay(100, listOf(1, 2)), OsmWay(101, listOf(3, 4))),
        nodes =
          listOf(
            knot(1, 52.0, 5.0, "12"),
            knot(2, 52.001, 5.0, "30"),
            knot(3, 52.0013, 5.0, "30"),
            knot(4, 52.003, 5.0, "45"),
          ),
      )

    val courses = result.courses.getValue(NetworkType.CYCLING)
    val toFortyFive = courses.entries.single { 4L in listOf(it.key.fromId, it.key.toId) }
    val course =
      if (toFortyFive.key.fromId == 2L) toFortyFive.value else toFortyFive.value.asReversed()
    assertEquals(GeoPoint(52.001, 5.0), course.first(), "starts at the merged knot's position")
    assertEquals(GeoPoint(52.0013, 5.0), course[1], "then follows the way from node 3")
    assertEquals(GeoPoint(52.003, 5.0), course.last())
  }

  @Test
  fun `the shorter of two routes between the same knots keeps its course`() {
    // Route 10 makes a detour via node 5, route 11 goes straight via node 6.
    val result =
      extract(
        relations = listOf(route(10, 100), route(11, 101)),
        ways = listOf(OsmWay(100, listOf(1, 5, 2)), OsmWay(101, listOf(1, 6, 2))),
        nodes =
          listOf(
            knot(1, 52.0, 5.0, "12"),
            OsmNode(5, 52.001, 5.01),
            OsmNode(6, 52.001, 5.0),
            knot(2, 52.002, 5.0, "45"),
          ),
      )

    val course = result.courses.getValue(NetworkType.CYCLING).values.single()
    assertTrue(GeoPoint(52.001, 5.0) in course, "goes via node 6")
  }

  private fun distanceToLineMeters(point: GeoPoint, line: List<GeoPoint>): Double {
    val scaleX = METERS_PER_DEGREE * cos(Math.toRadians(point.latitude))
    fun x(it: GeoPoint) = (it.longitude - point.longitude) * scaleX
    fun y(it: GeoPoint) = (it.latitude - point.latitude) * METERS_PER_DEGREE
    return line.zipWithNext().minOf { (start, end) ->
      val segmentX = x(end) - x(start)
      val segmentY = y(end) - y(start)
      val lengthSquared = segmentX * segmentX + segmentY * segmentY
      val fraction =
        if (lengthSquared == 0.0) 0.0
        else ((-x(start)) * segmentX + (-y(start)) * segmentY).div(lengthSquared).coerceIn(0.0, 1.0)
      hypot(x(start) + fraction * segmentX, y(start) + fraction * segmentY)
    }
  }

  private companion object {
    val METERS_PER_DEGREE = Math.toRadians(EARTH_RADIUS_METERS)
  }
}
