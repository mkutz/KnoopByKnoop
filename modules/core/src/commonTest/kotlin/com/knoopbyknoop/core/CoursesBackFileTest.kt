package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the file part of `network-data` "Course in each riding direction" of
 * `openspec/changes/follow-route-directions`.
 */
class CoursesBackFileTest {

  private val graphSha256 =
    sha256Hex(GraphFile(NetworkType.CYCLING, "utrecht", "2026-10-25", "", graph).toBytes())

  private val courses =
    graph.connections.map { connection ->
      val from = graph.knots.getValue(connection.fromId)
      val to = graph.knots.getValue(connection.toId)
      listOf(GeoPoint(from.latitude, from.longitude), GeoPoint(to.latitude, to.longitude))
    }

  /** 12–30 back on a way 100 m east of the way there, with a bridge on it. */
  private val back =
    courses[0].asReversed().let { (to, from) ->
      CourseBack(
        listOf(
          to,
          GeoPoint((to.latitude + from.latitude) / 2, (to.longitude + from.longitude) / 2 + 0.0015),
          from,
        ),
        listOf(Stretch(0, 1)),
      )
    }

  private val withBack =
    NetworkGeometry(
      graph,
      courses,
      bridges = mapOf(1 to listOf(Stretch(0, 1))),
      coursesBack = mapOf(0 to back),
    )

  @Test
  fun `a written file reads back with the same courses back`() {
    val read = NetworkGeometry.read(withBack.toBytes(graphSha256), graph, graphSha256)

    assertEquals(setOf(0), read.coursesBack.keys)
    assertEquals(back.bridges, read.coursesBack.getValue(0).bridges)
    assertEquals(3, read.coursesBack.getValue(0).course.size)
    assertEquals(withBack.bridges, read.bridges)
  }

  @Test
  fun `a file without the section reads with none`() {
    val bytes = NetworkGeometry(graph, courses).toBytes(graphSha256)

    // A file of the bridges' version ends before the counts of both kinds of courses back.
    val read = NetworkGeometry.read(bytes.copyOf(bytes.size - 2), graph, graphSha256)

    assertEquals(mapOf(), read.coursesBack)
  }

  @Test
  fun `released apps`() {
    // 0.10.0 reads the courses and the signpost courses, and stops there; it ignores the rest.
    val bytes = withBack.toBytes(graphSha256)
    val reader = ByteReader(bytes)
    reader.bytes(4)
    reader.byte()
    reader.bytes(32)
    val readCourses =
      List(reader.count()) {
        val from = graph.knots.getValue(graph.connections[it].fromId)
        var latitude = toMicrodegrees(from.latitude)
        var longitude = toMicrodegrees(from.longitude)
        List(reader.count()) {
          latitude += unzigzag(reader.varint())
          longitude += unzigzag(reader.varint())
          GeoPoint(fromMicrodegrees(latitude), fromMicrodegrees(longitude))
        }
      }
    val signpostCount = reader.count()

    assertEquals(NetworkGeometry.read(bytes, graph, graphSha256).courses, readCourses)
    assertEquals(0, signpostCount)
  }
}
