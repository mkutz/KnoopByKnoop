package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.PathKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathExtractionTest {

  private fun attributes(vararg tags: Pair<String, String>) = PathAttributes.of(mapOf(*tags))

  private fun extraction(vararg ways: OsmWay, nodes: List<OsmNode>) =
    PathExtraction().apply {
      ways.forEach(::acceptWay)
      nodes.forEach(::acceptNode)
    }

  @Test
  fun `Roads that can't be ridden are kept for drawing`() {
    val motorway = assertNotNull(attributes("highway" to "motorway"))
    assertEquals(PathKind.MAJOR, motorway.kind)
    assertFalse(motorway.forward)
    assertFalse(motorway.backward)
  }

  @Test
  fun `Mandatory separate cycle path keeps the road only for drawing`() {
    val road = assertNotNull(attributes("highway" to "primary", "bicycle" to "use_sidepath"))
    assertFalse(road.forward)
  }

  @Test
  fun `Closed footways, steps, driveways and private ways are left out`() {
    assertNull(attributes("highway" to "footway", "bicycle" to "no"))
    assertNull(attributes("highway" to "steps"))
    assertNull(attributes("highway" to "service", "service" to "driveway"))
    assertNull(attributes("highway" to "residential", "access" to "private"))
    assertNull(attributes("route" to "ferry"))
  }

  @Test
  fun `A cycle lane brings a road's cost closer to a path's`() {
    val plain = assertNotNull(attributes("highway" to "secondary"))
    val lane = assertNotNull(attributes("highway" to "secondary", "cycleway:right" to "track"))
    assertEquals(2.5, plain.costFactor, 0.001)
    assertEquals(1.375, lane.costFactor, 0.001)
  }

  @Test
  fun `One-way streets are ridden in their direction unless open to bicycles`() {
    assertFalse(assertNotNull(attributes("highway" to "residential", "oneway" to "yes")).backward)
    assertTrue(
      assertNotNull(
          attributes("highway" to "residential", "oneway" to "yes", "oneway:bicycle" to "no")
        )
        .backward
    )
    assertFalse(assertNotNull(attributes("highway" to "tertiary", "oneway" to "-1")).forward)
  }

  @Test
  fun `A surface makes a way more expensive`() {
    val mud = assertNotNull(attributes("highway" to "path", "surface" to "mud"))
    assertEquals(3.5, mud.costFactor, 0.001)
  }

  @Test
  fun `Ways are cut at shared nodes into segments`() {
    val squares =
      extraction(
          OsmWay(1, listOf(1, 2, 3), mapOf("highway" to "residential")),
          OsmWay(2, listOf(2, 4), mapOf("highway" to "cycleway")),
          nodes =
            listOf(
              OsmNode(1, 50.8, 7.2),
              OsmNode(2, 50.801, 7.2),
              OsmNode(3, 50.802, 7.2),
              OsmNode(4, 50.801, 7.201),
            ),
        )
        .squares()
    val segments = squares.values.flatten()
    assertEquals(3, segments.size)
    assertEquals(setOf(1L to 2L, 2L to 3L, 2L to 4L), segments.map { it.fromId to it.toId }.toSet())
  }

  @Test
  fun `Ways across a square's border are in the squares of both ends`() {
    val squares =
      extraction(
          OsmWay(1, listOf(1, 2), mapOf("highway" to "cycleway")),
          nodes = listOf(OsmNode(1, 50.81, 7.2), OsmNode(2, 50.82, 7.2)),
        )
        .squares()
    assertEquals(2, squares.size)
    assertTrue(squares.values.all { it.size == 1 })
  }

  @Test
  fun `A way read from two extracts counts once`() {
    val way = OsmWay(1, listOf(1, 2), mapOf("highway" to "cycleway"))
    val squares =
      extraction(way, way, nodes = listOf(OsmNode(1, 50.81, 7.2), OsmNode(2, 50.811, 7.2)))
        .squares()
    assertEquals(1, squares.values.flatten().size)
  }

  @Test
  fun `A node missing from the extracts ends the way there`() {
    val squares =
      extraction(
          OsmWay(1, listOf(1, 2, 3), mapOf("highway" to "cycleway")),
          nodes = listOf(OsmNode(1, 50.81, 7.2), OsmNode(2, 50.811, 7.2)),
        )
        .squares()
    assertEquals(0, squares.values.flatten().size)
  }
}
