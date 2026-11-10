package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.CourseBack
import com.knoopbyknoop.core.Ferry
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.PathFile
import com.knoopbyknoop.core.PathKind
import com.knoopbyknoop.core.PathSegment
import com.knoopbyknoop.core.PathSquare
import com.knoopbyknoop.core.SignpostCourse
import com.knoopbyknoop.core.Stretch
import com.knoopbyknoop.core.sha256Hex
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the `cut` command of the change `replay-rides-on-real-data`.
 *
 * Knots 1, 2 and 3 lie in the box, 4 north of it:
 * ```
 *   4            (52.20)
 *   |
 *   3 ~~ 2       (52.05)   ~~ ferry
 *    \  /
 *     1          (52.01)
 * ```
 */
class CutTest {

  private fun knot(id: Long, latitude: Double, longitude: Double) =
    Knot(id, "$id", NetworkType.CYCLING, latitude, longitude)

  private val knots =
    listOf(knot(1, 52.01, 5.02), knot(2, 52.05, 5.03), knot(3, 52.05, 5.01), knot(4, 52.20, 5.01))

  private val connections =
    listOf(
      Connection(1, 4, 20_000),
      Connection(1, 2, 5_000),
      Connection(3, 4, 17_000),
      Connection(2, 3, 1_400),
      Connection(1, 3, 5_000),
    )

  private val graph =
    NetworkGraph(knots, connections, mapOf(connections[3] to Ferry(name = "Veer")))

  private val box = Box(52.0, 5.0, 52.1, 5.05)

  private fun line(vararg points: Pair<Double, Double>) = points.map { (latitude, longitude) ->
    GeoPoint(latitude, longitude)
  }

  private val geometry =
    NetworkGeometry(
      graph,
      listOf(
        line(52.01 to 5.02, 52.20 to 5.01),
        line(52.01 to 5.02, 52.03 to 5.025, 52.05 to 5.03),
        line(52.05 to 5.01, 52.20 to 5.01),
        line(52.05 to 5.03, 52.05 to 5.01),
        line(52.01 to 5.02, 52.03 to 5.012, 52.05 to 5.01),
      ),
      signpostCourses =
        mapOf(1 to listOf(SignpostCourse(0, 0, line(52.01 to 5.021, 52.05 to 5.031)))),
      bridges = mapOf(0 to listOf(Stretch(0, 1)), 4 to listOf(Stretch(1, 2))),
      coursesBack = mapOf(1 to CourseBack(line(52.05 to 5.03, 52.03 to 5.026, 52.01 to 5.02))),
    )

  @Test
  fun `the knots in the box are kept with the connections and ferries between them`() {
    val cut =
      cutGraph(GraphFile(NetworkType.CYCLING, "test", "2026-11-08", ATTRIBUTION, graph), box)

    assertEquals(setOf(1L, 2L, 3L), cut.graph.knots.keys)
    assertEquals(listOf(connections[1], connections[3], connections[4]), cut.graph.connections)
    assertEquals(mapOf(connections[3] to Ferry(name = "Veer")), cut.graph.ferries)
    assertEquals("2026-11-08", cut.dataDate)
  }

  @Test
  fun `the geometry keeps every section on its connection and reads back for the cut graph`() {
    val cutGraph =
      cutGraph(GraphFile(NetworkType.CYCLING, "test", "2026-11-08", ATTRIBUTION, graph), box)
    val graphBytes = cutGraph.toBytes()

    val bytes = cutGeometry(geometry, cutGraph.graph).toBytes(sha256Hex(graphBytes))
    val read = NetworkGeometry.read(bytes, cutGraph.graph, sha256Hex(graphBytes))

    assertEquals(
      listOf(geometry.courses[1], geometry.courses[3], geometry.courses[4]),
      read.courses,
    )
    assertEquals(mapOf(0 to geometry.signpostCourses.getValue(1)), read.signpostCourses)
    assertEquals(mapOf(2 to listOf(Stretch(1, 2))), read.bridges.filterValues { it.isNotEmpty() })
    assertEquals(mapOf(0 to geometry.coursesBack.getValue(1)), read.coursesBack)
  }

  @Test
  fun `the path squares the box touches are copied unchanged`() {
    // Squares are 1/16° wide: the box touches the squares of 52.0..52.1 and 5.0..5.05 only.
    val inside = listOf(PathSquare.of(52.01, 5.02), PathSquare.of(52.09, 5.04))
    val outside = PathSquare.of(52.20, 5.01)
    val segments =
      (inside + outside).withIndex().associate { (index, square) ->
        val from =
          GeoPoint(
            (square.latitudeIndex + 0.5) / PathFile.SQUARES_PER_DEGREE,
            (square.longitudeIndex + 0.5) / PathFile.SQUARES_PER_DEGREE,
          )
        square to
          listOf(
            PathSegment(
              index * 2L,
              index * 2L + 1,
              PathKind.CYCLE,
              true,
              true,
              1.0,
              null,
              listOf(from, GeoPoint(from.latitude + 0.001, from.longitude)),
            )
          )
      }
    val file = createTempDirectory().resolve("cycling.paths")
    file.writeBytes(PathFile.toBytes(segments, "2026-11-08", ATTRIBUTION))
    val whole = file.readBytes()
    val read = { offset: Long, size: Int ->
      whole.copyOfRange(offset.toInt(), offset.toInt() + size)
    }

    val cut = cutPaths(read, box)

    val original = PathFile.open(read)
    val copied = PathFile.open { offset, size ->
      cut.copyOfRange(offset.toInt(), offset.toInt() + size)
    }
    fun described(segments: List<PathSegment>) = segments.map {
      listOf(it.fromId, it.toId, it.kind, it.points)
    }
    inside.distinct().forEach {
      assertEquals(described(original.segments(it)), described(copied.segments(it)))
    }
    assertEquals(1, copied.segments(inside.first()).size)
    assertEquals(listOf(), copied.segments(outside))
    assertEquals("2026-11-08", copied.dataDate)
  }
}
