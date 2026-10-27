package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.PlanningSession
import com.knoopbyknoop.core.RouteSummary
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MainTest {

  private val directory = createTempDirectory()

  private val nodeNetwork = mapOf("network:type" to "node_network")

  private val cyclingRoute = nodeNetwork + mapOf("type" to "route", "network" to "rcn")
  private val hikingRoute = nodeNetwork + mapOf("type" to "route", "network" to "rwn")

  private fun knot(id: Long, latitude: Double, cyclingRef: String, hikingRef: String) =
    OsmNode(id, latitude, 5.0, nodeNetwork + ("rcn_ref" to cyclingRef) + ("rwn_ref" to hikingRef))

  /** Two neighboring extracts, like the Netherlands and Belgium, overlapping in knot 45 / 4. */
  private fun neighboringExtracts(): List<Path> {
    val north = directory.resolve("north.osm.pbf")
    PbfWriter.write(
      north,
      replicationTimestamp = 1_793_000_000,
      nodes = listOf(knot(1, 52.0, "12", "3"), knot(2, 52.001, "45", "4")),
      ways = listOf(OsmWay(100, listOf(1, 2))),
      relations =
        listOf(
          OsmRelation(10, cyclingRoute, listOf(100)),
          OsmRelation(11, hikingRoute, listOf(100)),
        ),
    )
    val south = directory.resolve("south.osm.pbf")
    PbfWriter.write(
      south,
      replicationTimestamp = 1_793_000_000,
      nodes = listOf(knot(2, 52.001, "45", "4"), knot(3, 52.002, "7", "5")),
      ways = listOf(OsmWay(101, listOf(2, 3))),
      relations =
        listOf(
          OsmRelation(12, cyclingRoute, listOf(101)),
          OsmRelation(13, hikingRoute, listOf(101)),
        ),
    )
    return listOf(north, south)
  }

  @Test
  fun `covered area`() {
    val output = directory.resolve("output")

    build("nl-be-nrw", output, neighboringExtracts())
    index(output)

    val cycling = GraphFile.read(output.resolve("cycling.graph").readBytes())
    assertEquals(NetworkType.CYCLING, cycling.network)
    assertEquals("nl-be-nrw", cycling.area)
    assertEquals(listOf("12", "45", "7"), cycling.graph.knots.values.map { it.ref }.sorted())
    assertEquals(2, cycling.graph.connections.size)
    assertEquals("2026-10-26", cycling.dataDate)
    assertEquals(ATTRIBUTION, cycling.attribution)

    val index = NetworkIndex.parse(output.resolve("index.json").readText())
    assertEquals(
      listOf(
        "cycling.graph",
        "cycling.geometry",
        "hiking.graph",
        "hiking.geometry",
      ),
      index.files.map { it.file },
    )
    assertEquals(
      listOf(
        IndexedFileKind.GRAPH,
        IndexedFileKind.GEOMETRY,
        IndexedFileKind.GRAPH,
        IndexedFileKind.GEOMETRY,
      ),
      index.files.map { it.kind },
    )
    for ((graphEntry, geometryEntry) in index.files.chunked(2).map { it[0] to it[1] }) {
      assertEquals(graphEntry.network, geometryEntry.network)
      val geometryBytes = output.resolve(geometryEntry.file).readBytes()
      assertTrue(NetworkGeometry.belongsTo(geometryBytes, graphEntry.sha256), geometryEntry.file)
    }
    assertEquals(setOf("nl-be-nrw"), index.files.map { it.area }.toSet())
    index.files.forEach { entry ->
      assertTrue(entry.matches(output.resolve(entry.file).readBytes()), "${entry.file} matches")
    }
  }

  @Test
  fun `route from the Netherlands into Belgium`() {
    val output = directory.resolve("across")
    build("nl-be-nrw", output, neighboringExtracts())
    val graph = GraphFile.read(output.resolve("cycling.graph").readBytes()).graph
    val inTheNorth = graph.knots.values.single { it.ref == "12" }
    val inTheSouth = graph.knots.values.single { it.ref == "7" }

    val session = PlanningSession.start(graph).tap(inTheNorth).tap(inTheSouth)

    val summary = assertIs<RouteSummary.Complete>(session.route?.summary)
    assertEquals(listOf("12", "45", "7"), summary.knots.map { it.ref })
  }
}
