package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers `knot-navigation` "Doubtful junctions": a way from the west through a junction at 500 m to
 * the east, with other ways at the junction as each test needs.
 */
class DoubtfulJunctionsTest {

  private val origin = GeoPoint(50.80, 7.20)

  private fun at(east: Double, north: Double) =
    GeoPoint(origin.latitude + north / 111_195.0, origin.longitude + east / 70_300.0)

  private fun segment(
    from: Long,
    to: Long,
    vararg points: GeoPoint,
    kind: PathKind = PathKind.STREET,
    rideable: Boolean = true,
  ) = PathSegment(from, to, kind, rideable, rideable, 1.2, null, points.toList())

  private fun routerOf(vararg segments: PathSegment): PathRouter {
    val squares = HashMap<PathSquare, MutableList<PathSegment>>()
    segments.forEach { segment ->
      listOf(segment.points.first(), segment.points.last())
        .map { PathSquare.of(it.latitude, it.longitude) }
        .distinct()
        .forEach { squares.getOrPut(it) { mutableListOf() }.add(segment) }
    }
    val bytes = PathFile.toBytes(squares, "2026-11-08", "OSM")
    return PathRouter(
      PathFile.open { offset, size -> bytes.copyOfRange(offset.toInt(), offset.toInt() + size) }
    )
  }

  private val start = at(0.0, 0.0)
  private val junction = at(500.0, 0.0)
  private val end = at(1000.0, 0.0)

  private fun doubtfulOf(vararg others: PathSegment, taken: PathKind = PathKind.STREET) =
    assertNotNull(
        routerOf(
            segment(1, 2, start, junction),
            segment(2, 3, junction, end, kind = taken),
            *others,
          )
          .way(start, end)
      )
      .doubtful

  @Test
  fun `a similar exit close by`() {
    // 30° to the left of the way taken.
    val doubtful = doubtfulOf(segment(2, 4, junction, at(900.0, 231.0)))

    assertEquals(setOf(JunctionDoubt.SIMILAR_EXIT), doubtful.single().doubts)
    assertEquals(500.0, doubtful.single().startMeters, 2.0)
  }

  @Test
  fun `crossing a main road`() {
    val doubtful =
      doubtfulOf(
        segment(4, 2, at(500.0, 300.0), junction, kind = PathKind.MAIN),
        segment(2, 5, junction, at(500.0, -300.0), kind = PathKind.MAIN),
      )

    assertEquals(setOf(JunctionDoubt.MAIN_ROAD), doubtful.single().doubts)
  }

  @Test
  fun `crossing a motorway that cannot be ridden`() {
    val doubtful =
      doubtfulOf(
        segment(4, 2, at(500.0, 300.0), junction, kind = PathKind.MAJOR, rideable = false),
        segment(2, 5, junction, at(500.0, -300.0), kind = PathKind.MAJOR, rideable = false),
      )

    assertEquals(setOf(JunctionDoubt.MAIN_ROAD), doubtful.single().doubts)
  }

  @Test
  fun `riding along a main road is no crossing`() {
    val doubtful =
      assertNotNull(
          routerOf(
              segment(1, 2, start, junction, kind = PathKind.MAIN),
              segment(2, 3, junction, end, kind = PathKind.MAIN),
              segment(2, 4, junction, at(500.0, 300.0), kind = PathKind.MAIN),
            )
            .way(start, end)
        )
        .doubtful

    assertTrue(doubtful.isEmpty())
  }

  @Test
  fun `leaving the road for a cycle path`() {
    val doubtful = doubtfulOf(segment(2, 4, junction, at(500.0, -300.0)), taken = PathKind.CYCLE)

    assertEquals(setOf(JunctionDoubt.OFF_ROAD), doubtful.single().doubts)
  }

  @Test
  fun `a plain turn`() {
    val doubtful =
      doubtfulOf(
        segment(2, 4, junction, at(500.0, 300.0)),
        segment(2, 5, junction, at(500.0, -300.0)),
      )

    assertTrue(doubtful.isEmpty())
  }

  @Test
  fun `parts within 40 m are one junction`() {
    val second = at(530.0, 0.0)
    val way =
      assertNotNull(
        routerOf(
            segment(1, 2, start, junction),
            segment(2, 3, junction, second),
            segment(3, 6, second, end),
            segment(4, 2, at(500.0, 300.0), junction, kind = PathKind.MAIN),
            segment(2, 5, junction, at(500.0, -300.0), kind = PathKind.MAIN),
            segment(3, 7, second, at(930.0, 231.0)),
          )
          .way(start, end)
      )

    val junction = way.doubtful.single()
    assertEquals(setOf(JunctionDoubt.MAIN_ROAD, JunctionDoubt.SIMILAR_EXIT), junction.doubts)
    assertEquals(500.0, junction.startMeters, 2.0)
    assertEquals(530.0, junction.endMeters, 2.0)
  }
}
