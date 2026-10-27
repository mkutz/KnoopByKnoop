package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.NetworkType
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

/** Covers the extraction scenarios of `openspec/changes/add-network-graph-pipeline`. */
class NetworkExtractionTest {

  // A straight north-south line of nodes, about 111 m apart (0.001° latitude).
  private fun node(id: Long, step: Int, tags: Map<String, String> = mapOf()) =
    OsmNode(id, 52.0 + step * 0.001, 5.0, tags)

  private val stepMeters = haversineMeters(52.0, 5.0, 52.001, 5.0)

  private fun cyclingKnot(id: Long, step: Int, ref: String) =
    node(id, step, mapOf("rcn_ref" to ref, "network:type" to "node_network"))

  private fun route(id: Long, network: String, vararg wayIds: Long) =
    OsmRelation(
      id,
      mapOf("type" to "route", "network" to network, "network:type" to "node_network"),
      wayIds.toList(),
    )

  private fun extract(
    relations: List<OsmRelation>,
    ways: List<OsmWay>,
    nodes: List<OsmNode>,
  ): ExtractionResult =
    NetworkExtraction()
      .apply {
        relations.forEach(::acceptRelation)
        ways.forEach(::acceptWay)
        nodes.forEach(::acceptNode)
      }
      .result()

  @Test
  fun `cycling and hiking are kept apart`() {
    val sharedKnot =
      node(1, 0, mapOf("rcn_ref" to "12", "rwn_ref" to "3", "network:type" to "node_network"))
    val cyclingKnot = cyclingKnot(2, 2, "45")
    val hikingKnot = node(3, 4, mapOf("rwn_ref" to "4", "network:type" to "node_network"))

    val result =
      extract(
        relations = listOf(route(10, "rcn", 100), route(11, "rwn", 101)),
        ways = listOf(OsmWay(100, listOf(1, 2)), OsmWay(101, listOf(1, 2, 3))),
        nodes = listOf(sharedKnot, cyclingKnot, hikingKnot),
      )

    val cycling = result.graphs.getValue(NetworkType.CYCLING)
    val hiking = result.graphs.getValue(NetworkType.HIKING)
    assertEquals(setOf("12", "45"), cycling.knots.values.map { it.ref }.toSet())
    assertEquals(setOf("3", "4"), hiking.knots.values.map { it.ref }.toSet())
    assertEquals(listOf(1L to 2L), cycling.connections.map { it.fromId to it.toId })
    assertEquals(listOf(1L to 3L), hiking.connections.map { it.fromId to it.toId })
  }

  @Test
  fun `route lengths`() {
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100, 101)),
        ways = listOf(OsmWay(100, listOf(1, 2, 3)), OsmWay(101, listOf(3, 4))),
        nodes = listOf(cyclingKnot(1, 0, "12"), node(2, 1), node(3, 2), cyclingKnot(4, 3, "45")),
      )

    val connection = result.graphs.getValue(NetworkType.CYCLING).connections.single()
    assertEquals(Connection(1, 4, (3 * stepMeters).roundToInt()), connection)
  }

  @Test
  fun `ways for both riding directions are not counted twice`() {
    // A dual carriageway: way 100 goes 1→2→4, way 101 goes back 4→3→1 one step longer.
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100, 101)),
        ways = listOf(OsmWay(100, listOf(1, 2, 4)), OsmWay(101, listOf(4, 3, 5, 1))),
        nodes =
          listOf(
            cyclingKnot(1, 0, "12"),
            node(2, 1),
            cyclingKnot(4, 2, "45"),
            node(3, 3),
            node(5, 1),
          ),
      )

    val connection = result.graphs.getValue(NetworkType.CYCLING).connections.single()
    assertEquals((2 * stepMeters).roundToInt(), connection.lengthMeters)
  }

  @Test
  fun `routes with a wrong number of knots are skipped`() {
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100), route(11, "rcn", 101)),
        ways = listOf(OsmWay(100, listOf(1, 2)), OsmWay(101, listOf(1, 3, 4))),
        nodes =
          listOf(
            cyclingKnot(1, 0, "12"),
            node(2, 1),
            cyclingKnot(3, 2, "30"),
            cyclingKnot(4, 3, "45"),
          ),
      )

    assertEquals(
      mapOf(SkipReason.ONE_KNOT to 1, SkipReason.MORE_THAN_TWO_KNOTS to 1),
      result.skippedRoutes,
    )
    assertEquals(listOf(), result.graphs.getValue(NetworkType.CYCLING).connections)
  }

  private fun knot30(id: Long, latitude: Double) =
    OsmNode(id, latitude, 5.0, mapOf("rcn_ref" to "30", "network:type" to "node_network"))

  @Test
  fun `junction tagged on several nodes`() {
    // Junction 30 is tagged on both sides of a wide junction (nodes 2 and 3, about 330 m apart).
    // Route 12-30 arrives at node 2, route 30-45 leaves from node 3.
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100), route(11, "rcn", 101)),
        ways = listOf(OsmWay(100, listOf(1, 2)), OsmWay(101, listOf(3, 4))),
        nodes =
          listOf(
            cyclingKnot(1, 0, "12"),
            knot30(2, 52.001),
            knot30(3, 52.004),
            cyclingKnot(4, 5, "45"),
          ),
      )

    val graph = result.graphs.getValue(NetworkType.CYCLING)
    assertEquals(listOf("12", "30", "45"), graph.knots.values.map { it.ref }.sorted())
    assertEquals(setOf(1L to 2L, 2L to 4L), graph.connections.map { it.fromId to it.toId }.toSet())
  }

  @Test
  fun `connector inside a junction`() {
    // A short route of its own (relation 11) links the two nodes tagged 30, without a ferry.
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100), route(11, "rcn", 101), route(12, "rcn", 102)),
        ways =
          listOf(OsmWay(100, listOf(1, 2)), OsmWay(101, listOf(2, 3)), OsmWay(102, listOf(3, 4))),
        nodes =
          listOf(
            cyclingKnot(1, 0, "12"),
            knot30(2, 52.001),
            knot30(3, 52.0013),
            cyclingKnot(4, 3, "45"),
          ),
      )

    val graph = result.graphs.getValue(NetworkType.CYCLING)
    assertEquals(listOf("12", "30", "45"), graph.knots.values.map { it.ref }.sorted())
    assertEquals(setOf(1L to 2L, 2L to 4L), graph.connections.map { it.fromId to it.toId }.toSet())
    assertEquals(mapOf(SkipReason.ONE_KNOT to 1), result.skippedRoutes)
  }

  @Test
  fun `equal numbers on both banks of a river`() {
    // Knot 30 on both banks (nodes 2 and 3, within the merge distance), linked by a ferry route.
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100), route(11, "rcn", 101), route(12, "rcn", 102)),
        ways =
          listOf(
            OsmWay(100, listOf(1, 2)),
            OsmWay(101, listOf(2, 3), mapOf("route" to "ferry")),
            OsmWay(102, listOf(3, 4)),
          ),
        nodes =
          listOf(
            cyclingKnot(1, 0, "12"),
            knot30(2, 52.001),
            knot30(3, 52.003),
            cyclingKnot(4, 5, "45"),
          ),
      )

    val graph = result.graphs.getValue(NetworkType.CYCLING)
    assertEquals(listOf("12", "30", "30", "45"), graph.knots.values.map { it.ref }.sorted())
    assertEquals(
      setOf(1L to 2L, 2L to 3L, 3L to 4L),
      graph.connections.map { it.fromId to it.toId }.toSet(),
    )
  }

  @Test
  fun `a ferry relation keeps equal numbers apart too`() {
    val ferry =
      OsmRelation(
        11,
        mapOf(
          "type" to "route",
          "route" to "ferry",
          "network" to "rcn",
          "network:type" to "node_network",
        ),
        listOf(101),
      )
    val result =
      extract(
        relations = listOf(ferry),
        ways = listOf(OsmWay(101, listOf(2, 3))),
        nodes = listOf(knot30(2, 52.001), knot30(3, 52.003)),
      )

    assertEquals(2, result.graphs.getValue(NetworkType.CYCLING).knots.size)
  }

  @Test
  fun `distant nodes with the same ref are different knots`() {
    val result =
      extract(
        relations = listOf(),
        ways = listOf(),
        nodes = listOf(cyclingKnot(1, 0, "12"), cyclingKnot(2, 10, "12")), // about 1.1 km apart
      )

    assertEquals(2, result.graphs.getValue(NetworkType.CYCLING).knots.size)
  }

  @Test
  fun `other routes are ignored`() {
    val busRoute = OsmRelation(10, mapOf("type" to "route", "route" to "bus"), listOf(100))

    val result =
      extract(
        relations = listOf(busRoute),
        ways = listOf(OsmWay(100, listOf(1, 2))),
        nodes = listOf(cyclingKnot(1, 0, "12"), cyclingKnot(2, 1, "45")),
      )

    assertEquals(mapOf(), result.skippedRoutes)
    assertEquals(listOf(), result.graphs.getValue(NetworkType.CYCLING).connections)
  }

  @Test
  fun `the shortest of two routes between the same knots is kept`() {
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100), route(11, "rcn", 101)),
        ways = listOf(OsmWay(100, listOf(1, 2, 3)), OsmWay(101, listOf(3, 1))),
        nodes = listOf(cyclingKnot(1, 0, "12"), node(2, 3), cyclingKnot(3, 1, "45")),
      )

    val connection = result.graphs.getValue(NetworkType.CYCLING).connections.single()
    assertEquals(stepMeters.roundToInt(), connection.lengthMeters)
  }

  @Test
  fun `a relation fed twice, as from two overlapping extracts, is used once`() {
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100), route(11, "rcn", 101), route(11, "rcn", 101)),
        ways = listOf(OsmWay(100, listOf(1, 2)), OsmWay(101, listOf(2, 3, 4))),
        nodes =
          listOf(
            cyclingKnot(1, 0, "12"),
            cyclingKnot(2, 1, "45"),
            cyclingKnot(3, 2, "7"),
            cyclingKnot(4, 3, "8"),
          ),
      )

    assertEquals(1, result.graphs.getValue(NetworkType.CYCLING).connections.size)
    assertEquals(1, result.skippedRoutes.values.sum())
  }
}
