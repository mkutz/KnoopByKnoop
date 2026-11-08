package com.knoopbyknoop.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers `knot-navigation` "Junction picture": a way from the south up to a junction at 500 m,
 * where it turns right onto a cycle path over two main roads, as in "Picture of a junction".
 */
class JunctionPictureTest {

  private val origin = GeoPoint(50.80, 7.20)

  /** [east] and [north] meters from the origin. */
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
  private val junction = at(0.0, 500.0)
  private val end = at(400.0, 500.0)

  /** The way north to the junction, then east on a cycle path, with [others] around it. */
  private fun pictureWith(vararg others: PathSegment): JunctionPicture {
    val way =
      assertNotNull(
        routerOf(
            segment(1, 2, start, junction),
            segment(2, 3, junction, end, kind = PathKind.CYCLE),
            segment(2, 9, junction, at(0.0, 900.0)),
            *others,
          )
          .way(start, end)
      )
    return assertNotNull(way.doubtful.first().picture)
  }

  private fun JunctionPicture.of(kind: PictureLineKind) = lines.filter { it.kind == kind }

  @Test
  fun `picture of a junction`() {
    val picture =
      pictureWith(
        segment(4, 2, at(-300.0, 500.0), junction, kind = PathKind.MAIN),
        segment(2, 5, junction, at(30.0, 500.0), kind = PathKind.MAIN),
        segment(5, 6, at(30.0, 500.0), at(30.0, 900.0), kind = PathKind.MAJOR),
        segment(5, 7, at(30.0, 500.0), at(30.0, 100.0), kind = PathKind.MAJOR),
      )

    // The way from the bottom up to the junction, then to the right.
    val before = picture.of(PictureLineKind.WAY_BEFORE).single().points
    assertEquals(PicturePoint(0, -34), before.first())
    assertEquals(PicturePoint(0, 0), before.last())
    val after = picture.of(PictureLineKind.WAY_AFTER).single().points
    assertEquals(PicturePoint(40, 0), after.last())
    // Both roads, wide; the way's own cycle path is the way.
    assertTrue(picture.of(PictureLineKind.MAIN_ROAD).size >= 2)
    assertTrue(picture.of(PictureLineKind.CYCLE_PATH).isEmpty())
    // Roads below the way.
    assertEquals(PictureLineKind.WAY_AFTER, picture.lines.last().kind)
  }

  @Test
  fun `footways at a busy junction`() {
    val picture =
      pictureWith(
        segment(4, 2, at(-300.0, 500.0), junction, kind = PathKind.MAIN),
        segment(2, 5, junction, at(-300.0, 520.0), kind = PathKind.PATH),
      )

    assertTrue(picture.lines.none { line -> line.points.any { it == PicturePoint(-50, 20) } })
    assertEquals(
      setOf(
        PictureLineKind.MAIN_ROAD,
        PictureLineKind.STREET,
        PictureLineKind.WAY_BEFORE,
        PictureLineKind.WAY_AFTER,
      ),
      picture.lines.map { it.kind }.toSet(),
    )
  }

  @Test
  fun `roads that cannot be ridden`() {
    val picture =
      pictureWith(
        segment(4, 2, at(-300.0, 500.0), junction, kind = PathKind.MAJOR, rideable = false),
        segment(2, 5, junction, at(-300.0, 400.0), kind = PathKind.MAJOR, rideable = false),
      )

    assertEquals(2, picture.of(PictureLineKind.MAIN_ROAD).size)
  }

  @Test
  fun `streets and cycle paths only close by`() {
    val picture = pictureWith(segment(2, 4, junction, at(-300.0, 500.0)))

    val street = picture.of(PictureLineKind.STREET).flatMap { it.points }
    assertTrue(street.all { abs(it.x) <= 50 && abs(it.y) <= 50 })
  }

  @Test
  fun `the riding direction is up`() {
    // The same junction, approached from the west.
    val way =
      assertNotNull(
        routerOf(
            segment(1, 2, at(-500.0, 500.0), junction),
            segment(2, 3, junction, at(0.0, 100.0), kind = PathKind.CYCLE),
            segment(2, 9, junction, at(400.0, 500.0)),
          )
          .way(at(-500.0, 500.0), at(0.0, 100.0))
      )
    val picture = assertNotNull(way.doubtful.first().picture)

    assertEquals(
      PicturePoint(0, -34),
      picture.of(PictureLineKind.WAY_BEFORE).single().points.first(),
    )
    // Turning south is to the right.
    assertEquals(PicturePoint(40, 0), picture.of(PictureLineKind.WAY_AFTER).single().points.last())
  }

  @Test
  fun `the picture as text and back`() {
    val picture =
      JunctionPicture(
        listOf(
          PictureLine(
            PictureLineKind.MAIN_ROAD,
            listOf(PicturePoint(-47, 3), PicturePoint(47, -2)),
          ),
          PictureLine(PictureLineKind.WAY_BEFORE, listOf(PicturePoint(0, -34), PicturePoint(0, 0))),
        )
      )

    assertEquals("M-47,3 47,-2|B0,-34 0,0", picture.encoded())
    assertEquals(picture, JunctionPicture.decoded(picture.encoded()))
  }
}
