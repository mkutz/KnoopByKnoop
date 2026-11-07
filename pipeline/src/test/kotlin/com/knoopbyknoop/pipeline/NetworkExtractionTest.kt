package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.Ferry
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.SignRegion
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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
  fun `positions of a merged knot`() {
    // As 02 in Siegburg: tagged on two nodes about 195 m apart, the knot at the lower node ID.
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100), route(11, "rcn", 101)),
        ways = listOf(OsmWay(100, listOf(1, 2)), OsmWay(101, listOf(3, 4))),
        nodes =
          listOf(
            cyclingKnot(1, 0, "12"),
            knot30(2, 52.001),
            knot30(3, 52.00275),
            cyclingKnot(4, 5, "45"),
          ),
      )

    val knot30 = result.graphs.getValue(NetworkType.CYCLING).knots.getValue(2)
    assertEquals(52.001, knot30.latitude)
    assertEquals(listOf(GeoPoint(52.00275, 5.0)), knot30.otherNodes)
  }

  private fun merged30Course(vararg connector: OsmNode): Pair<Connection, List<GeoPoint>> {
    // Knot 30 on nodes 2 and 3; route 30-45 reaches it at node 3, the position is at node 2.
    val result =
      extract(
        relations =
          listOf(route(10, "rcn", 100), route(11, "rcn", 101)) +
            if (connector.isEmpty()) listOf() else listOf(route(12, "rcn", 102)),
        ways =
          listOf(OsmWay(100, listOf(1, 2)), OsmWay(101, listOf(3, 4))) +
            if (connector.isEmpty()) listOf()
            else listOf(OsmWay(102, listOf(2L) + connector.map { it.id } + 3L)),
        nodes =
          listOf(
            cyclingKnot(1, 0, "12"),
            knot30(2, 52.001),
            knot30(3, 52.00275),
            cyclingKnot(4, 5, "45"),
          ) + connector,
      )
    return result.courses
      .getValue(NetworkType.CYCLING)
      .entries
      .single { 4L in listOf(it.key.fromId, it.key.toId) }
      .toPair()
  }

  private val position30 = GeoPoint(52.001, 5.0)
  private val otherNode30 = GeoPoint(52.00275, 5.0)
  private val position45 = GeoPoint(52.005, 5.0)

  @Test
  fun `along the ways to the knot's position`() {
    val (connection, course) = merged30Course(OsmNode(7, 52.0018, 5.001))

    assertEquals(listOf(position30, GeoPoint(52.0018, 5.001), otherNode30, position45), course)
    assertEquals(
      haversineMeters(52.00275, 5.0, 52.005, 5.0).roundToInt(),
      connection.lengthMeters,
      "measured on the route's own path",
    )
  }

  @Test
  fun `no way between the nodes`() {
    assertEquals(listOf(position30, otherNode30, position45), merged30Course().second)
  }

  @Test
  fun `a detour between the nodes is no way to the position`() {
    val (_, course) = merged30Course(OsmNode(7, 52.0018, 5.01))

    assertEquals(listOf(position30, otherNode30, position45), course)
  }

  // Requirement: Courses to each signpost

  private fun signpostCoursesOf(
    ways: List<OsmWay>,
    extraNodes: List<OsmNode> = listOf(),
  ): List<com.knoopbyknoop.core.SignpostCourse> {
    // Knot 30 on nodes 2 and 3 and knot 45 on node 4; relation 10 holds all the ways.
    val result =
      extract(
        relations = listOf(route(10, "rcn", *ways.map { it.id }.toLongArray())),
        ways = ways,
        nodes =
          listOf(knot30(2, 52.001), knot30(3, 52.00275), cyclingKnot(4, 5, "45")) + extraNodes,
      )
    return result.signpostCourses.getValue(NetworkType.CYCLING).values.singleOrNull().orEmpty()
  }

  @Test
  fun `two branches of a route`() {
    // From 45 to node 5, where the route splits to the two nodes of 30.
    val fork = OsmNode(5, 52.0035, 5.001, mapOf())
    val pairs =
      signpostCoursesOf(
        ways =
          listOf(OsmWay(100, listOf(4, 5)), OsmWay(101, listOf(5, 2)), OsmWay(102, listOf(5, 3))),
        extraNodes = listOf(fork),
      )

    // The connection runs from 45, the first knot on the route's ways, to 30.
    assertEquals(setOf(0, 1), pairs.map { it.toPosition }.toSet())
    assertEquals(
      listOf(GeoPoint(52.005, 5.0), GeoPoint(52.0035, 5.001), GeoPoint(52.00275, 5.0)),
      pairs.single { it.toPosition == 1 }.course,
    )
  }

  @Test
  fun `only through another node`() {
    // The route reaches node 2 of 30 only past node 3.
    assertEquals(
      listOf(),
      signpostCoursesOf(ways = listOf(OsmWay(100, listOf(4, 3)), OsmWay(101, listOf(3, 2)))),
    )
  }

  @Test
  fun `one node`() {
    assertEquals(listOf(), signpostCoursesOf(ways = listOf(OsmWay(100, listOf(4, 3)))))
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

  /** Knots 12 and 45 on both banks of a river, connected by the ferry way 100. */
  private fun extractFerry(
    ferryWayTags: Map<String, String>,
    otherRelations: List<OsmRelation> = listOf(),
  ): Map<Connection, Ferry> =
    extract(
        relations = listOf(route(10, "rcn", 99, 100, 101)) + otherRelations,
        ways =
          listOf(
            OsmWay(99, listOf(1, 2)),
            OsmWay(100, listOf(2, 3), mapOf("route" to "ferry") + ferryWayTags),
            OsmWay(101, listOf(3, 4)),
          ),
        nodes = listOf(cyclingKnot(1, 0, "12"), node(2, 1), node(3, 3), cyclingKnot(4, 4, "45")),
      )
      .graphs
      .getValue(NetworkType.CYCLING)
      .ferries

  @Test
  fun `ferry with times and fee`() {
    val ferries =
      extractFerry(
        mapOf(
          "name" to "Veerpont Beusichem",
          "opening_hours" to "Apr-Oct: Mo-Su 07:00-19:00",
          "charge" to "1.50 EUR",
        )
      )

    assertEquals(
      listOf(
        Ferry(
          name = "Veerpont Beusichem",
          openingHours = "Apr-Oct: Mo-Su 07:00-19:00",
          charge = "1.50 EUR",
        )
      ),
      ferries.values.toList(),
    )
    assertEquals(listOf(1L to 4L), ferries.keys.map { it.fromId to it.toId })
  }

  @Test
  fun `details on the ferry relation`() {
    val ferryLine =
      OsmRelation(
        20,
        mapOf(
          "type" to "route",
          "route" to "ferry",
          "name" to "Pont Beusichem–Culemborg",
          "opening_hours" to "Mo-Su 07:00-19:00",
          "fee" to "yes",
        ),
        listOf(100),
      )

    val ferries = extractFerry(mapOf("name" to "Veerpont Beusichem"), listOf(ferryLine))

    // The way's name comes first; the times and the fee only the relation has.
    assertEquals(
      listOf(Ferry(name = "Veerpont Beusichem", openingHours = "Mo-Su 07:00-19:00", fee = "yes")),
      ferries.values.toList(),
    )
  }

  @Test
  fun `ferry without details`() {
    assertEquals(listOf(Ferry()), extractFerry(mapOf()).values.toList())
  }

  @Test
  fun `connections without a ferry have no ferry details`() {
    val result =
      extract(
        relations = listOf(route(10, "rcn", 100)),
        ways = listOf(OsmWay(100, listOf(1, 2))),
        nodes = listOf(cyclingKnot(1, 0, "12"), cyclingKnot(2, 2, "45")),
      )

    assertEquals(mapOf(), result.graphs.getValue(NetworkType.CYCLING).ferries)
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

  // Requirement: Sign region of each knot. Borders are squares from 4.99° to 5.01° longitude, one
  // above the other: the Netherlands (as the province of Limburg) up to 52.001°, Belgium up to
  // 52.003°, North Rhine-Westphalia
  // up to 52.005°, each drawn as two ways that have to be joined.

  private var nextBorderId = 1000L

  private class BorderSquare(
    val relation: OsmRelation,
    val ways: List<OsmWay>,
    val nodes: List<OsmNode>,
  )

  private fun borderSquare(
    relationId: Long,
    tags: Map<String, String>,
    south: Double,
    north: Double,
  ): BorderSquare {
    val corners =
      listOf(south to 4.99, south to 5.01, north to 5.01, north to 4.99).map { (latitude, longitude)
        ->
        OsmNode(nextBorderId++, latitude, longitude)
      }
    val ids = corners.map { it.id }
    // The second way runs backwards, so joining has to turn it around.
    val ways =
      listOf(
        OsmWay(nextBorderId++, listOf(ids[0], ids[1], ids[2])),
        OsmWay(nextBorderId++, listOf(ids[0], ids[3], ids[2])),
      )
    val relation =
      OsmRelation(
        relationId,
        mapOf("type" to "boundary", "boundary" to "administrative", "name" to "Test") + tags,
        ways.map { it.id },
        ways.map { "outer" },
      )
    return BorderSquare(relation, ways, corners)
  }

  private val netherlands =
    borderSquare(9001, mapOf("admin_level" to "4", "ISO3166-2" to "NL-LI"), 51.999, 52.001)
  private val belgium =
    borderSquare(9002, mapOf("admin_level" to "2", "ISO3166-1:alpha2" to "BE"), 52.001, 52.003)
  private val northRhineWestphalia =
    borderSquare(9003, mapOf("admin_level" to "4", "ISO3166-2" to "DE-NW"), 52.003, 52.005)
  private val borders = listOf(netherlands, belgium, northRhineWestphalia)

  private fun extractWithBorders(nodes: List<OsmNode>, times: Int = 1): ExtractionResult =
    NetworkExtraction()
      .apply {
        repeat(times) { borders.forEach { acceptRelation(it.relation) } }
        repeat(times) { borders.flatMap { it.ways }.forEach(::acceptWay) }
        repeat(times) { (borders.flatMap { it.nodes } + nodes).forEach(::acceptNode) }
      }
      .result()

  private fun regionOf(
    result: ExtractionResult,
    ref: String,
    network: NetworkType = NetworkType.CYCLING,
  ) = result.graphs.getValue(network).knots.values.single { it.ref == ref }.signRegion

  @Test
  fun `knots in each sign region`() {
    val result =
      extractWithBorders(
        listOf(cyclingKnot(1, 0, "95"), cyclingKnot(2, 2, "150"), cyclingKnot(3, 4, "60"))
      )

    assertEquals(SignRegion.NETHERLANDS, regionOf(result, "95"), "knot in Limburg")
    assertEquals(SignRegion.BELGIUM, regionOf(result, "150"), "knot in Flanders")
    assertEquals(SignRegion.NORTH_RHINE_WESTPHALIA, regionOf(result, "60"))
  }

  @Test
  fun `knot in another German state`() {
    val result = extractWithBorders(listOf(cyclingKnot(1, 7, "12")))

    assertNull(regionOf(result, "12"))
  }

  @Test
  fun `knot near a border`() {
    // 50 m north of the border between the Dutch and the Belgian square, read from two extracts.
    val knot = OsmNode(1, 52.00145, 5.0, mapOf("rcn_ref" to "33", "network:type" to "node_network"))

    val result = extractWithBorders(listOf(knot), times = 2)

    assertEquals(SignRegion.BELGIUM, regionOf(result, "33"))
  }

  @Test
  fun `hiking knots have no region`() {
    val hikingKnot = node(1, 0, mapOf("rwn_ref" to "4", "network:type" to "node_network"))

    val result = extractWithBorders(listOf(hikingKnot))

    assertNull(regionOf(result, "4", NetworkType.HIKING))
  }

  @Test
  fun `the Dutch national relation is not used`() {
    // It also holds the Caribbean islands, whose ways no European extract contains.
    val kingdom =
      borderSquare(9004, mapOf("admin_level" to "2", "ISO3166-1" to "NL"), 51.999, 52.001)
    val extraction = NetworkExtraction()
    extraction.acceptRelation(kingdom.relation)
    extraction.acceptWay(kingdom.ways[0])
    (kingdom.nodes + cyclingKnot(1, 0, "95")).forEach(extraction::acceptNode)

    val result = extraction.result()

    assertNull(regionOf(result, "95"))
  }

  @Test
  fun `incomplete border`() {
    val extraction = NetworkExtraction()
    borders.forEach { extraction.acceptRelation(it.relation) }
    // The Belgian square's second way is missing from the extract.
    borders.flatMap { it.ways }.filter { it != belgium.ways[1] }.forEach(extraction::acceptWay)
    (borders.flatMap { it.nodes } + cyclingKnot(1, 0, "95")).forEach(extraction::acceptNode)

    val exception = assertFailsWith<IncompleteBorderException> { extraction.result() }
    assertEquals(
      "The border of Test (relation 9002) is incomplete: way ${belgium.ways[1].id} is missing",
      exception.message,
    )
  }

  @Test
  fun `a hole in a border is outside it`() {
    // A Belgian enclave inside the Dutch square, like Baarle-Hertog in Baarle-Nassau.
    val hole =
      listOf(51.9995 to 4.999, 51.9995 to 5.001, 52.0005 to 5.001, 52.0005 to 4.999).map {
        (latitude, longitude) ->
        OsmNode(nextBorderId++, latitude, longitude)
      }
    val holeWay = OsmWay(nextBorderId++, hole.map { it.id } + hole.first().id)
    val withHole =
      netherlands.relation.copy(
        memberWayIds = netherlands.relation.memberWayIds + holeWay.id,
        memberRoles = netherlands.relation.memberRoles + "inner",
      )
    val extraction = NetworkExtraction()
    extraction.acceptRelation(withHole)
    (netherlands.ways + holeWay).forEach(extraction::acceptWay)
    val outsideTheHole =
      OsmNode(2, 51.9992, 5.0, mapOf("rcn_ref" to "8", "network:type" to "node_network"))
    (netherlands.nodes + hole + cyclingKnot(1, 0, "7") + outsideTheHole).forEach(
      extraction::acceptNode
    )

    val result = extraction.result()

    assertNull(regionOf(result, "7"), "inside the hole")
    assertEquals(SignRegion.NETHERLANDS, regionOf(result, "8"))
  }
}
