package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathRouterTest {

  private val origin = GeoPoint(50.80, 7.20)

  /** A point [east] and [north] meters from [origin]. */
  private fun at(east: Double, north: Double) =
    GeoPoint(origin.latitude + north / 111_195.0, origin.longitude + east / 70_300.0)

  private fun segment(
    from: Long,
    to: Long,
    vararg points: GeoPoint,
    kind: PathKind = PathKind.STREET,
    forward: Boolean = true,
    backward: Boolean = true,
    cost: Double = 1.2,
    name: String? = null,
  ) = PathSegment(from, to, kind, forward, backward, cost, name, points.toList())

  private fun routerOf(vararg segments: PathSegment): PathRouter {
    val squares = HashMap<PathSquare, MutableList<PathSegment>>()
    segments.forEach { segment ->
      val ends = listOf(segment.points.first(), segment.points.last())
      ends
        .map { PathSquare.of(it.latitude, it.longitude) }
        .distinct()
        .forEach { squares.getOrPut(it) { mutableListOf() }.add(segment) }
    }
    val bytes = PathFile.toBytes(squares, "2026-11-02", "OSM")
    return PathRouter(
      PathFile.open { offset, size ->
        bytes.copyOfRange(offset.toInt(), offset.toInt() + size)
      }
    )
  }

  /** Two places 1 km apart, over a straight way and a detour 20 % longer through [detourNorth]. */
  private val start = at(0.0, 0.0)
  private val end = at(1000.0, 0.0)

  private fun straightAndDetour(straight: PathSegment, detour: PathSegment) =
    routerOf(straight, detour)

  private fun detourOf(
    cost: Double,
    kind: PathKind = PathKind.CYCLE,
    north: Double = 330.0,
  ) = segment(1, 2, start, at(500.0, north), end, kind = kind, cost = cost)

  private fun usesDetour(way: PathWay?, north: Double = 330.0) =
    assertNotNull(way).points.any { it.latitude > at(0.0, north / 2).latitude }

  @Test
  fun `Without network access a way is found from the installed squares`() {
    val way = routerOf(segment(1, 2, start, end)).way(start, end)
    assertEquals(1000.0, assertNotNull(way).lengthMeters, 15.0)
  }

  @Test
  fun `Cycle path beside a main road is preferred when 20 percent longer`() {
    val router =
      straightAndDetour(
        segment(1, 2, start, end, kind = PathKind.MAJOR, cost = 4.0),
        detourOf(cost = 1.0),
      )
    assertTrue(usesDetour(router.way(start, end)))
  }

  @Test
  fun `A much longer detour loses to a tertiary road`() {
    val router =
      straightAndDetour(
        segment(1, 2, start, end, kind = PathKind.MAIN, cost = 1.7),
        detourOf(cost = 1.0, north = 1400.0),
      )
    assertTrue(!usesDetour(router.way(start, end), north = 1400.0))
  }

  @Test
  fun `Poor surface loses to a paved path of about the same length`() {
    val router =
      straightAndDetour(
        segment(1, 2, start, end, kind = PathKind.PATH, cost = 1.4 * 2.5),
        detourOf(cost = 1.05, north = 100.0),
      )
    assertTrue(usesDetour(router.way(start, end), north = 100.0))
  }

  @Test
  fun `Ways closed to bicycles are not used`() {
    val router =
      straightAndDetour(
        segment(1, 2, start, end, kind = PathKind.MAJOR, forward = false, backward = false),
        detourOf(cost = 1.2),
      )
    assertTrue(usesDetour(router.way(start, end)))
  }

  @Test
  fun `One-way street is not ridden against its direction`() {
    val router = routerOf(segment(1, 2, start, end, backward = false))
    assertNull(router.way(end, start))
    assertNotNull(router.way(start, end))
  }

  @Test
  fun `One-way street open to bicycles is ridden both ways`() {
    assertNotNull(routerOf(segment(1, 2, start, end)).way(end, start))
  }

  @Test
  fun `A stray piece of path nearest to the rider is skipped`() {
    val stray = segment(8, 9, at(-100.0, 10.0), at(100.0, 10.0), kind = PathKind.PATH)
    val connected = segment(1, 2, at(-100.0, -60.0), end)
    val way = routerOf(stray, connected).way(start, end)
    assertNotNull(way)
  }

  @Test
  fun `Far from any path no way is found`() {
    assertNull(routerOf(segment(1, 2, at(500.0, 0.0), end)).way(at(0.0, 0.0), end))
  }

  @Test
  fun `Squares missing in between give no way`() {
    val far = GeoPoint(origin.latitude + 0.2, origin.longitude)
    val router =
      routerOf(
        segment(1, 2, start, end),
        segment(3, 4, far, GeoPoint(far.latitude, far.longitude + 0.01)),
      )
    assertNull(router.way(start, far))
  }

  @Test
  fun `A way does not turn back when a slightly longer one continues`() {
    val junction = at(500.0, 0.0)
    val hairpin = at(400.0, 30.0)
    val target = at(100.0, 150.0)
    val router =
      routerOf(
        segment(1, 2, start, junction),
        segment(2, 3, junction, hairpin, target),
        segment(1, 4, start, at(150.0, 80.0), at(150.0, 100.0)),
        segment(4, 3, at(150.0, 100.0), target),
      )
    val way = assertNotNull(router.way(start, target))
    assertTrue(way.points.none { it.longitude > at(450.0, 0.0).longitude })
  }

  @Test
  fun `A way along one segment follows it between the two places`() {
    val course = segment(1, 2, at(-500.0, 0.0), at(1500.0, 0.0))
    val way = assertNotNull(routerOf(course).way(start, end))
    assertEquals(1000.0, way.lengthMeters, 15.0)
    assertEquals(listOf(), way.turns)
  }

  private fun crossing(east: Double, north: Double, name: String? = "Weierstraße") =
    routerOf(
      segment(1, 2, start, at(500.0, 0.0)),
      segment(2, 3, at(500.0, 0.0), at(500.0 + east, north), name = name),
      segment(2, 4, at(500.0, 0.0), at(1000.0, 0.0)),
      segment(2, 5, at(500.0, 0.0), at(500.0, -500.0)),
    )

  @Test
  fun `A left turn at a junction is a turn of the way`() {
    val way = assertNotNull(crossing(0.0, 500.0).way(start, at(500.0, 500.0)))
    val turn = way.turns.single()
    assertEquals(TurnDirection.LEFT, turn.direction)
    assertEquals(500.0, turn.atMeters, 15.0)
    assertEquals("Weierstraße", turn.name)
  }

  @Test
  fun `A right turn at a junction is a turn of the way`() {
    val way = assertNotNull(crossing(0.0, -500.0).way(start, at(500.0, -500.0)))
    assertEquals(TurnDirection.RIGHT, way.turns.single().direction)
  }

  @Test
  fun `Straight on over a junction needs no turn`() {
    val way = assertNotNull(crossing(0.0, 500.0).way(start, at(1000.0, 0.0)))
    assertEquals(listOf(), way.turns)
  }

  @Test
  fun `A bend without another way is no turn`() {
    val way =
      assertNotNull(
        routerOf(
            segment(1, 2, start, at(500.0, 0.0)),
            segment(2, 3, at(500.0, 0.0), at(500.0, 500.0)),
          )
          .way(start, at(500.0, 500.0))
      )
    assertEquals(listOf(), way.turns)
  }

  @Test
  fun `Two turns close together give one with their overall direction`() {
    val router =
      routerOf(
        segment(1, 2, start, at(500.0, 0.0)),
        segment(2, 3, at(500.0, 0.0), at(500.0, -30.0)),
        segment(3, 4, at(500.0, -30.0), at(1000.0, -530.0)),
        segment(2, 5, at(500.0, 0.0), at(1000.0, 0.0)),
        segment(3, 6, at(500.0, -30.0), at(0.0, -30.0)),
        segment(3, 7, at(500.0, -30.0), at(500.0, -500.0)),
      )
    val way = assertNotNull(router.way(start, at(1000.0, -530.0)))
    assertEquals(1, way.turns.size)
    assertEquals(TurnDirection.SLIGHT_RIGHT, way.turns.single().direction)
  }

  private val riderPlace = at(400.0, 0.0)

  /** A path east past the rider at 400 m, with a way on to the target from each end. */
  private fun pathBothWays(westMeters: Double, eastMeters: Double): PathRouter {
    val target = at(400.0, 500.0)
    return routerOf(
      segment(1, 2, at(0.0, 0.0), at(1000.0, 0.0), cost = 1.0),
      segment(1, 3, at(0.0, 0.0), at(0.0, 500.0), target, cost = westMeters / 900.0),
      segment(2, 3, at(1000.0, 0.0), at(1000.0, 500.0), target, cost = eastMeters / 1100.0),
    )
  }

  private val pathTarget = at(400.0, 500.0)

  @Test
  fun `A way starts on the path the rider is on and not on one 120 m away`() {
    val target = at(1000.0, 120.0)
    val router =
      routerOf(
        segment(1, 2, at(-1000.0, 0.0), at(1000.0, 0.0), cost = 3.0),
        segment(2, 3, at(1000.0, 0.0), target, cost = 3.0),
        segment(4, 3, at(0.0, 120.0), target, cost = 1.0),
      )
    val way = assertNotNull(router.way(at(0.0, 0.0), target))
    assertTrue(
      way.points.none {
        it.latitude > at(0.0, 60.0).latitude && it.longitude < at(900.0, 0.0).longitude
      }
    )
  }

  @Test
  fun `A way ahead about as short as the way back is taken`() {
    val way = assertNotNull(pathBothWays(900.0, 800.0).way(riderPlace, pathTarget, heading = 90.0))
    assertTrue(way.points.any { it.longitude > at(900.0, 0.0).longitude })
    assertTrue(!way.startsBack)
  }

  @Test
  fun `A way back much shorter is taken and reported`() {
    val way = assertNotNull(pathBothWays(500.0, 1500.0).way(riderPlace, pathTarget, heading = 90.0))
    assertTrue(way.points.any { it.longitude < at(100.0, 0.0).longitude })
    assertTrue(way.startsBack)
  }

  @Test
  fun `Without a riding direction the cheapest way is taken and not reported`() {
    val way = assertNotNull(pathBothWays(500.0, 1000.0).way(riderPlace, pathTarget))
    assertTrue(way.points.any { it.longitude < at(100.0, 0.0).longitude })
    assertTrue(!way.startsBack)
  }

  /** At 500 m, the way on to the north-east, and another one at [otherEast], [otherNorth]. */
  private fun bend(otherEast: Double, otherNorth: Double) =
    routerOf(
      segment(1, 2, start, at(500.0, 0.0)),
      segment(2, 3, at(500.0, 0.0), at(1000.0, -400.0)),
      segment(2, 4, at(500.0, 0.0), at(otherEast, otherNorth)),
    )

  @Test
  fun `A bend without a choice needs no turn`() {
    val way = assertNotNull(bend(300.0, 400.0).way(start, at(1000.0, -400.0)))
    assertEquals(listOf(), way.turns)
  }

  @Test
  fun `A bend with a choice is a turn`() {
    val way = assertNotNull(bend(1000.0, 0.0).way(start, at(1000.0, -400.0)))
    assertEquals(TurnDirection.SLIGHT_RIGHT, way.turns.single().direction)
  }

  @Test
  fun `A crossing with an offset is said as straight on`() {
    val router =
      routerOf(
        segment(1, 2, start, at(500.0, 0.0)),
        segment(2, 3, at(500.0, 0.0), at(500.0, -30.0)),
        segment(3, 4, at(500.0, -30.0), at(1000.0, -30.0)),
        segment(2, 5, at(500.0, 0.0), at(500.0, 400.0)),
        segment(3, 6, at(500.0, -30.0), at(500.0, -400.0)),
      )
    val way = assertNotNull(router.way(start, at(1000.0, -30.0)))
    assertEquals(TurnDirection.STRAIGHT, way.turns.single().direction)
  }

  @Test
  fun `A file in another format version is refused`() {
    val bytes = PathFile.toBytes(mapOf(), "2026-11-02", "OSM")
    bytes[4] = 99
    assertFailsWith<UnsupportedFormatVersionException> {
      PathFile.open { offset, size -> bytes.copyOfRange(offset.toInt(), offset.toInt() + size) }
    }
  }

  @Test
  fun `A broken square is refused`() {
    val square = PathSquare.of(start.latitude, start.longitude)
    val bytes = PathFile.toBytes(mapOf(square to listOf(segment(1, 2, start, end))), "d", "OSM")
    bytes[bytes.lastIndex] = (bytes.last() + 1).toByte()
    val file = PathFile.open { offset, size ->
      bytes.copyOfRange(offset.toInt(), offset.toInt() + size)
    }
    assertFailsWith<GraphFormatException> { file.segments(square) }
  }
}
