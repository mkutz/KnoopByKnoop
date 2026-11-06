package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Covers `map-planning` "A route point away from the network". */
class RoutePointWaysTest {

  private val origin = GeoPoint(50.80, 7.20)

  /** A point [east] and [north] meters from [origin]. */
  private fun at(east: Double, north: Double) =
    GeoPoint(origin.latitude + north / 111_195.0, origin.longitude + east / 70_300.0)

  private fun knot(id: Long, place: GeoPoint, network: NetworkType = NetworkType.CYCLING) =
    Knot(id, "0$id", network, place.latitude, place.longitude)

  /**
   * Two courses 600 m apart with a river between them, crossed only at their ends:
   * ```
   *   03 ---------- 04    north course, y = 600
   *   |       |      |
   *   |    path      |
   *   |       P      |    the place chosen, y = 250, nearer to the south course
   *   |  ~~river~~   |
   *   01 ---------- 02    south course, y = 0
   * ```
   */
  private fun world(network: NetworkType = NetworkType.CYCLING): NetworkGeometry {
    val knots =
      listOf(
        knot(1, at(0.0, 0.0), network),
        knot(2, at(2000.0, 0.0), network),
        knot(3, at(0.0, 600.0), network),
        knot(4, at(2000.0, 600.0), network),
      )
    val graph =
      NetworkGraph(
        knots,
        listOf(
          Connection(1, 2, 2000),
          Connection(3, 4, 2000),
          Connection(1, 3, 600),
          Connection(2, 4, 600),
        ),
      )
    return NetworkGeometry(
      graph,
      graph.connections.map { connection ->
        listOf(connection.fromId, connection.toId).map {
          GeoPoint(graph.knots.getValue(it).latitude, graph.knots.getValue(it).longitude)
        }
      },
    )
  }

  private val chosen = at(1000.0, 250.0)

  /** Only a path north from the chosen place to the north course, and that course itself. */
  private fun pathsNorth(): PathRouter {
    val segments =
      listOf(
        PathSegment(
          10,
          11,
          PathKind.PATH,
          true,
          true,
          1.2,
          null,
          listOf(chosen, at(1000.0, 600.0)),
        ),
        PathSegment(
          12,
          11,
          PathKind.STREET,
          true,
          true,
          1.2,
          null,
          listOf(at(0.0, 600.0), at(1000.0, 600.0)),
        ),
        PathSegment(
          11,
          13,
          PathKind.STREET,
          true,
          true,
          1.2,
          null,
          listOf(at(1000.0, 600.0), at(2000.0, 600.0)),
        ),
      )
    return routerOf(segments)
  }

  private fun routerOf(segments: List<PathSegment>): PathRouter {
    val squares = HashMap<PathSquare, MutableList<PathSegment>>()
    segments.forEach { segment ->
      listOf(segment.points.first(), segment.points.last())
        .map { PathSquare.of(it.latitude, it.longitude) }
        .distinct()
        .forEach { squares.getOrPut(it) { mutableListOf() }.add(segment) }
    }
    val bytes = PathFile.toBytes(squares, "2026-11-03", "OSM")
    return PathRouter(
      PathFile.open { offset, size -> bytes.copyOfRange(offset.toInt(), offset.toInt() + size) }
    )
  }

  private val noPathData = routerOf(listOf())

  private fun planned(geometry: NetworkGeometry): PlanningSession {
    val session = PlanningSession.start(geometry.graph)
    val offer = assertIs<MapTap.Offer>(session.tapAt(chosen, 30.0, null, geometry))
    val point = assertNotNull(session.routePointFor(offer, geometry))
    return session.startAt(point).tap(geometry.graph.knots.getValue(4), NewKnots.STOP)
  }

  private fun PlanningSession.point() = routePoints.single()

  @Test
  fun `while the path data loads`() {
    val geometry = world()
    val planned = planned(geometry)

    assertTrue(planned.point().routePoint!!.straight, "a straight line at once")
    assertEquals(1L to 2L, planned.point().routePoint!!.fromId to planned.point().routePoint!!.toId)

    val withWay = planned.withWaysFrom(RoutePointWays(pathsNorth(), geometry), navigating = false)

    assertFalse(withWay.point().routePoint!!.straight, "replaced when the squares have arrived")
  }

  @Test
  fun `a way is found`() {
    val geometry = world()
    val planned = planned(geometry)

    val withWay = planned.withWaysFrom(RoutePointWays(pathsNorth(), geometry), navigating = false)

    val routePoint = withWay.point().routePoint!!
    assertEquals(350.0, routePoint.wayMeters.toDouble(), 5.0, "along the path")
    assertEquals(chosen, routePoint.way.first())
    assertEquals(routePoint.place, routePoint.way.last())
    val length = assertIs<RouteSummary.Complete>(withWay.route?.summary).lengthMeters
    assertEquals(350.0 + 1000, length.toDouble(), 5.0, "the way and half of 03-04")
  }

  @Test
  fun `joining a nearby course`() {
    val geometry = world()

    val withWay =
      planned(geometry).withWaysFrom(RoutePointWays(pathsNorth(), geometry), navigating = false)

    val routePoint = withWay.point().routePoint!!
    assertEquals(3L to 4L, routePoint.fromId to routePoint.toId, "the course on the same side")
  }

  @Test
  fun `no path data`() {
    val geometry = world()
    val planned = planned(geometry)

    assertSame(
      planned,
      planned.withWaysFrom(RoutePointWays(noPathData, geometry), navigating = false),
    )
    assertTrue(planned.hasStraightLines)
  }

  @Test
  fun `no way found`() {
    val geometry = world()
    val planned = planned(geometry)
    // A path that leads nowhere near a course.
    val stray =
      routerOf(
        listOf(
          PathSegment(
            20,
            21,
            PathKind.PATH,
            true,
            true,
            1.2,
            null,
            listOf(chosen, at(1100.0, 250.0)),
          )
        )
      )

    assertSame(planned, planned.withWaysFrom(RoutePointWays(stray, geometry), navigating = false))
  }

  @Test
  fun `a hiking route`() {
    val geometry = world(NetworkType.HIKING)
    val planned = planned(geometry)

    assertTrue(planned.point().routePoint!!.straight)
    assertSame(
      planned,
      planned.withWaysFrom(RoutePointWays(pathsNorth(), geometry), navigating = false),
    )
  }

  @Test
  fun `a straight line is not replaced while navigating`() {
    val geometry = world()
    val planned = planned(geometry)

    assertSame(
      planned,
      planned.withWaysFrom(RoutePointWays(pathsNorth(), geometry), navigating = true),
    )
  }
}
