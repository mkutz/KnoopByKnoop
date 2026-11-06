package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the file part of `network-data` "Bridge stretches of courses" of
 * `openspec/changes/show-route-on-bridges`.
 */
class BridgeStretchesTest {

  private val graphSha256 =
    sha256Hex(GraphFile(NetworkType.CYCLING, "utrecht", "2026-10-25", "", graph).toBytes())

  /** Every connection runs through four points between its knots. */
  private val courses =
    graph.connections.map { connection ->
      val from = graph.knots.getValue(connection.fromId)
      val to = graph.knots.getValue(connection.toId)
      (0..5).map {
        GeoPoint(
          from.latitude + (to.latitude - from.latitude) * it / 5,
          from.longitude + (to.longitude - from.longitude) * it / 5,
        )
      }
    }

  private val signpostCourses =
    mapOf(
      1 to
        listOf(
          SignpostCourse(0, 0, courses[1]),
          SignpostCourse(0, 1, courses[1], bridges = listOf(Stretch(2, 4))),
        )
    )

  private val bridges = mapOf(0 to listOf(Stretch(1, 2), Stretch(3, 5)), 2 to listOf(Stretch(0, 1)))

  private val withBridges = NetworkGeometry(graph, courses, signpostCourses, bridges)

  @Test
  fun `a written file reads back with the same stretches`() {
    val read = NetworkGeometry.read(withBridges.toBytes(graphSha256), graph, graphSha256)

    assertEquals(bridges, read.bridges)
    assertEquals(
      listOf(listOf(), listOf(Stretch(2, 4))),
      read.signpostCourses.getValue(1).map { it.bridges },
    )
  }

  @Test
  fun `a file without the sections reads with none`() {
    val withoutStretches = signpostCourses.mapValues { (_, pairs) ->
      pairs.map { it.copy(bridges = listOf()) }
    }
    val bytes = NetworkGeometry(graph, courses, withoutStretches).toBytes(graphSha256)

    // A file of 0.9.0 ends right after the signpost courses, before the two bridge counts.
    val read = NetworkGeometry.read(bytes.copyOf(bytes.size - 2), graph, graphSha256)

    assertEquals(mapOf(), read.bridges)
    assertEquals(courses.size, read.courses.size)
    assertEquals(2, read.signpostCourses.getValue(1).size)
  }

  @Test
  fun `released apps`() {
    val bytes = withBridges.toBytes(graphSha256)

    val (readCourses, readSignposts) = readLike090(bytes)

    val current = NetworkGeometry.read(bytes, graph, graphSha256)
    assertEquals(current.courses, readCourses)
    assertEquals(
      current.signpostCourses.mapValues { (_, pairs) -> pairs.map { it.copy(bridges = listOf()) } },
      readSignposts,
    )
  }

  /**
   * The rules of the geometry reader of 0.9.0 (389a8d3): header, courses, then the signpost courses
   * if more bytes follow; anything after them is ignored.
   */
  private fun readLike090(
    bytes: ByteArray
  ): Pair<List<List<GeoPoint>>, Map<Int, List<SignpostCourse>>> {
    val reader = ByteReader(bytes)
    reader.bytes(4)
    reader.byte()
    reader.bytes(32)
    fun pointsFrom(knot: Knot): List<GeoPoint> {
      var latitude = toMicrodegrees(knot.latitude)
      var longitude = toMicrodegrees(knot.longitude)
      return List(reader.count()) {
        latitude += unzigzag(reader.varint())
        longitude += unzigzag(reader.varint())
        GeoPoint(fromMicrodegrees(latitude), fromMicrodegrees(longitude))
      }
    }
    val readCourses =
      List(reader.count()) { pointsFrom(graph.knots.getValue(graph.connections[it].fromId)) }
    if (!reader.hasMore()) return readCourses to mapOf()
    val readSignposts =
      List(reader.count()) {
          val index = reader.count()
          val from = graph.knots.getValue(graph.connections[index].fromId)
          index to
            List(reader.count()) {
              val fromPosition = reader.count()
              val toPosition = reader.count()
              SignpostCourse(fromPosition, toPosition, pointsFrom(from))
            }
        }
        .toMap()
    return readCourses to readSignposts
  }
}
