package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot30
import com.knoopbyknoop.core.TestNetwork.knot45
import com.knoopbyknoop.core.TestNetwork.knot7
import com.knoopbyknoop.core.TestNetwork.knot99
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Covers `map-planning` "Export the route as GPX" of `openspec/changes/export-gpx`. */
class GpxTest {

  private val router = ShortestPathRouter(graph)

  /** Every connection bends through a point north of its midpoint. */
  private val geometry =
    NetworkGeometry(
      graph,
      graph.connections.map { connection ->
        val from = graph.knots.getValue(connection.fromId)
        val to = graph.knots.getValue(connection.toId)
        listOf(
          GeoPoint(from.latitude, from.longitude),
          GeoPoint((from.latitude + to.latitude) / 2 + 0.001, (from.longitude + to.longitude) / 2),
          GeoPoint(to.latitude, to.longitude),
        )
      },
    )

  /** 12 through stop 30 to 45: 12 → 30 directly, 30 → 45 directly. */
  private val route = Route.plan(listOf(knot12, knot30, knot45), router)
  private val course = geometry.courseOf(route)!!
  private val gpx = Gpx.write(route, course)

  private fun elements(name: String) = Regex("<$name [^>]*>").findAll(gpx).map { it.value }.toList()

  private fun attribute(element: String, name: String) =
    Regex("""$name="([^"]*)"""").find(element)!!.groupValues[1]

  // Scenario: Export a route
  @Test
  fun `the file is named after the network and the start and end knots`() {
    assertEquals("knoop-by-knoop-cycling-12-45.gpx", Gpx.fileName(route))
  }

  // Scenario: Track along the network
  @Test
  fun `the file is GPX 1 1 with the waypoints before one track`() {
    assertTrue(gpx.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
    assertTrue(
      """<gpx version="1.1" creator="Knoop by Knoop" xmlns="http://www.topografix.com/GPX/1/1">""" in
        gpx
    )
    assertTrue(gpx.trimEnd().endsWith("</gpx>"))
    assertEquals(1, Regex("<trk>").findAll(gpx).count())
    assertTrue(gpx.lastIndexOf("<wpt ") < gpx.indexOf("<trk>"))
  }

  // Scenario: Track along the network
  @Test
  fun `the track follows the course from the start to the end`() {
    val points =
      elements("trkpt").map {
        GeoPoint(attribute(it, "lat").toDouble(), attribute(it, "lon").toDouble())
      }

    assertEquals(course, points)
    assertEquals(GeoPoint(knot12.latitude, knot12.longitude), points.first())
    assertEquals(GeoPoint(knot45.latitude, knot45.longitude), points.last())
  }

  // Scenario: Knots as waypoints
  @Test
  fun `every knot passed is a waypoint with its number and role`() {
    val via7 = Route.plan(listOf(knot12, knot30, knot7, knot45), router)
    val waypoints =
      Regex("<wpt [^>]*><name>([^<]*)</name><type>([^<]*)</type></wpt>")
        .findAll(Gpx.write(via7, geometry.courseOf(via7)!!))
        .map { it.groupValues[1] to it.groupValues[2] }
        .toList()

    assertEquals(
      listOf("12" to "start", "30" to "stop", "7" to "stop", "45" to "end"),
      waypoints,
    )
  }

  // Scenario: Knots as waypoints
  @Test
  fun `knots between waypoints are typed as knots`() {
    // 12 → 45 runs through 7.
    val through7 = Route.plan(listOf(knot12, knot45), router)

    assertEquals(
      listOf(knot12 to Gpx.Role.START, knot7 to Gpx.Role.KNOT, knot45 to Gpx.Role.END),
      Gpx.knotsWithRoles(through7),
    )
  }

  // Scenario: Round trip
  @Test
  fun `a round trip starts and ends at the same knot`() {
    val roundTrip = Route.plan(listOf(knot12, knot30, knot12), router)
    val roundTripCourse = geometry.courseOf(roundTrip)!!
    val points = Gpx.write(roundTrip, roundTripCourse)

    assertEquals(
      listOf(knot12 to Gpx.Role.START, knot30 to Gpx.Role.STOP, knot12 to Gpx.Role.END),
      Gpx.knotsWithRoles(roundTrip),
    )
    assertEquals(roundTripCourse.first(), roundTripCourse.last())
    assertTrue("""<trkpt lat="52.090000" lon="5.120000"/>""" in points)
  }

  // Scenario: No export without a complete route
  @Test
  fun `only a complete route with its course can be exported`() {
    val unreachable = Route.plan(listOf(knot12, knot99), router)

    assertTrue(Gpx.canExport(route, course))
    assertFalse(Gpx.canExport(null, null))
    assertFalse(Gpx.canExport(route, null))
    assertFalse(Gpx.canExport(unreachable, geometry.courseOf(unreachable)))
  }

  @Test
  fun `coordinates have six decimals and a point`() {
    assertEquals("52.090000", Gpx.decimal(52.09))
    assertEquals("-0.000001", Gpx.decimal(-0.000001))
    assertEquals("5.123457", Gpx.decimal(5.1234567))
  }

  @Test
  fun `numbers are escaped and stripped from the file name`() {
    val odd = Knot(1, "A&<1>", NetworkType.HIKING, 52.0, 5.0)
    val other = Knot(2, "B/2", NetworkType.HIKING, 52.01, 5.0)
    val hiking = NetworkGraph(listOf(odd, other), listOf(Connection(1, 2, 1000)))
    val oddRoute = Route.plan(listOf(odd, other), ShortestPathRouter(hiking))
    val written = Gpx.write(oddRoute, listOf(GeoPoint(52.0, 5.0), GeoPoint(52.01, 5.0)))

    assertTrue("<name>A&amp;&lt;1&gt;</name>" in written)
    assertFalse("A&<" in written)
    assertEquals("knoop-by-knoop-hiking-A1-B2.gpx", Gpx.fileName(oddRoute))
  }
}
