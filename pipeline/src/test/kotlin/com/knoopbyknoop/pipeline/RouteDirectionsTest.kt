package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.NetworkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `network-data` "Course in each riding direction" of
 * `openspec/changes/follow-route-directions`.
 *
 * Knot 28 (node 1) in the south, knot 02 (node 9) in the north, about 111 m per step. Between nodes
 * 2 and 7 the road splits: a western side through 3 and 4, bulging a little further out so it is
 * longer, and an eastern side through 5 and 6.
 */
class RouteDirectionsTest {

  private val nodeNetwork = mapOf("network:type" to "node_network")

  private val nodes =
    listOf(
      OsmNode(1, 52.000, 5.0, nodeNetwork + ("rcn_ref" to "28")),
      OsmNode(2, 52.001, 5.0),
      OsmNode(3, 52.002, 4.9996),
      OsmNode(4, 52.003, 4.9996),
      OsmNode(5, 52.002, 5.0002),
      OsmNode(6, 52.003, 5.0002),
      OsmNode(7, 52.004, 5.0),
      OsmNode(8, 52.005, 5.0),
      OsmNode(9, 52.006, 5.0, nodeNetwork + ("rcn_ref" to "02")),
    )

  private val west = setOf(4.9996)
  private val east = setOf(5.0002)

  private fun way(id: Long, vararg nodeIds: Long, tags: Map<String, String> = mapOf()) =
    OsmWay(id, nodeIds.toList(), tags)

  private fun extract(members: List<Pair<OsmWay, String>>) =
    NetworkExtraction()
      .apply {
        acceptRelation(
          OsmRelation(
            10,
            nodeNetwork + mapOf("type" to "route", "network" to "rcn"),
            members.map { it.first.id },
            members.map { it.second },
          )
        )
        members.map { it.first }.distinct().forEach(::acceptWay)
        nodes.forEach(::acceptNode)
      }
      .result()

  /** The course from 28 to 02 and back, whichever way the connection was stored. */
  private fun coursesOf(result: ExtractionResult): Pair<List<GeoPoint>, List<GeoPoint>?> {
    val connection = result.graphs.getValue(NetworkType.CYCLING).connections.single()
    val course = result.courses.getValue(NetworkType.CYCLING).getValue(connection)
    val back = result.coursesBack[NetworkType.CYCLING]?.get(connection)?.first
    return if (connection.fromId == 1L) course to back else (back ?: course.asReversed()) to course
  }

  private fun List<GeoPoint>.sides() = mapNotNull { point ->
    when (point.longitude) {
      in west -> "west"
      in east -> "east"
      else -> null
    }
  }
    .toSet()

  private val south = way(100, 1, 2)
  private val north = way(101, 7, 8, 9)

  @Test
  fun `a cycle path on each side`() {
    // Northbound on the western side, southbound on the eastern side, mapped northwards.
    val (there, back) =
      coursesOf(
        extract(
          listOf(
            south to "",
            way(102, 2, 3, 4, 7) to "forward",
            way(103, 2, 5, 6, 7) to "backward",
            north to "",
          )
        )
      )

    assertEquals(setOf("west"), there.sides())
    assertEquals(setOf("east"), assertNotNull(back).sides())
  }

  @Test
  fun `one-way stretch`() {
    // The western street mapped southwards as `backward`, the eastern one northwards as `backward`.
    val (there, back) =
      coursesOf(
        extract(
          listOf(
            south to "",
            way(102, 7, 4, 3, 2) to "backward",
            way(103, 2, 5, 6, 7) to "backward",
            north to "",
          )
        )
      )

    assertEquals(setOf("west"), there.sides())
    assertEquals(setOf("east"), assertNotNull(back).sides())
  }

  @Test
  fun `no marks`() {
    val result =
      extract(
        listOf(south to "", way(102, 2, 3, 4, 7) to "", way(103, 2, 5, 6, 7) to "", north to "")
      )

    assertEquals(mapOf(), result.coursesBack[NetworkType.CYCLING].orEmpty())
    assertEquals(setOf("east"), coursesOf(result).first.sides(), "the shorter side, as before")
  }

  @Test
  fun `marks that leave no way`() {
    // Everything usable southwards only: northbound finds no way and takes the course back
    // reversed.
    val result =
      extract(
        listOf(
          way(100, 2, 1) to "forward",
          way(103, 7, 6, 5, 2) to "forward",
          way(101, 9, 8, 7) to "forward",
        )
      )

    assertEquals(1, result.graphs.getValue(NetworkType.CYCLING).connections.size)
    assertEquals(mapOf(), result.coursesBack[NetworkType.CYCLING].orEmpty())
    assertEquals(setOf("east"), coursesOf(result).first.sides())
  }

  @Test
  fun `courses to each signpost in each direction`() {
    // 02 is also signposted at node 10, 30 m east of node 9, reached from node 8.
    val signpost = OsmNode(10, 52.006, 5.00044, nodeNetwork + ("rcn_ref" to "02"))
    val members =
      listOf(
        south to "",
        way(102, 2, 3, 4, 7) to "forward",
        way(103, 2, 5, 6, 7) to "backward",
        north to "",
        way(104, 8, 10) to "",
      )
    val result =
      NetworkExtraction()
        .apply {
          acceptRelation(
            OsmRelation(
              10,
              nodeNetwork + mapOf("type" to "route", "network" to "rcn"),
              members.map { it.first.id },
              members.map { it.second },
            )
          )
          members.map { it.first }.forEach(::acceptWay)
          (nodes + signpost).forEach(::acceptNode)
        }
        .result()

    val connection = result.graphs.getValue(NetworkType.CYCLING).connections.single()
    val signposts = result.signpostCourses.getValue(NetworkType.CYCLING).getValue(connection)
    assertEquals(2, signposts.size)
    for (course in signposts) {
      val (there, back) =
        if (connection.fromId == 1L) course.course to assertNotNull(course.back).course
        // Stored from 02: its course back is the way from 28 to 02.
        else assertNotNull(course.back).course to course.course
      assertEquals(setOf("west"), there.sides(), "28 to 02 on the western side")
      assertEquals(setOf("east"), back.sides(), "02 to 28 on the eastern side")
    }
  }

  @Test
  fun `bridges on the course back`() {
    val result =
      extract(
        listOf(
          south to "",
          way(102, 2, 3, 4, 7) to "forward",
          way(103, 2, 5, 6, 7, tags = mapOf("bridge" to "yes")) to "backward",
          north to "",
        )
      )

    val connection = result.graphs.getValue(NetworkType.CYCLING).connections.single()
    val (_, backStretches) =
      assertNotNull(result.coursesBack.getValue(NetworkType.CYCLING)[connection])
    assertEquals(1, backStretches.size, "the eastern side is a bridge")
    assertNull(result.bridges[NetworkType.CYCLING]?.get(connection), "the western side is none")
    assertTrue(backStretches.single().last > backStretches.single().first)
  }
}
