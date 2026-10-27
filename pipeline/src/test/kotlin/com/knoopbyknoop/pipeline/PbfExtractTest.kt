package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.NetworkType
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class PbfExtractTest {

  private val directory: Path = createTempDirectory()

  private val nodeNetwork = mapOf("network:type" to "node_network")

  @Test
  fun `reads node networks from a PBF file`() {
    val file = directory.resolve("test.osm.pbf")
    PbfWriter.write(
      file,
      replicationTimestamp = 1_793_000_000, // 2026-10-26T06:13:20Z
      nodes =
        listOf(
          OsmNode(1, 52.0, 5.0, nodeNetwork + ("rcn_ref" to "12")),
          OsmNode(2, 52.001, 5.0),
          OsmNode(3, 52.002, 5.0, nodeNetwork + ("rcn_ref" to "45") + ("name" to "Schoonhoven")),
        ),
      ways = listOf(OsmWay(100, listOf(1, 2, 3))),
      relations =
        listOf(
          OsmRelation(
            10,
            nodeNetwork + mapOf("type" to "route", "network" to "rcn"),
            listOf(100),
          )
        ),
    )
    val extraction = NetworkExtraction()

    val dataDate = PbfExtract(file).readInto(extraction)

    assertEquals(LocalDate.of(2026, 10, 26), dataDate)
    val graph = extraction.result().graphs.getValue(NetworkType.CYCLING)
    assertEquals(mapOf(1L to "12", 3L to "45"), graph.knots.mapValues { it.value.ref })
    assertEquals(52.002, graph.knots.getValue(3).latitude, 0.0000001)
    assertEquals(
      haversineMeters(52.0, 5.0, 52.002, 5.0),
      graph.connections.single().lengthMeters.toDouble(),
      1.0,
    )
  }

  @Test
  fun `reads ferries from way tags`() {
    val file = directory.resolve("ferry.osm.pbf")
    val knot30 = nodeNetwork + ("rcn_ref" to "30")
    PbfWriter.write(
      file,
      replicationTimestamp = 1_793_000_000,
      nodes = listOf(OsmNode(2, 52.001, 5.0, knot30), OsmNode(3, 52.003, 5.0, knot30)),
      ways = listOf(OsmWay(101, listOf(2, 3), mapOf("route" to "ferry"))),
      relations =
        listOf(
          OsmRelation(11, nodeNetwork + mapOf("type" to "route", "network" to "rcn"), listOf(101))
        ),
    )
    val extraction = NetworkExtraction()

    PbfExtract(file).readInto(extraction)

    assertEquals(2, extraction.result().graphs.getValue(NetworkType.CYCLING).knots.size)
  }

  private val cyclingRoute = nodeNetwork + mapOf("type" to "route", "network" to "rcn")

  /**
   * Two extracts overlapping like neighboring Geofabrik extracts: knot 45 lies in both, and route
   * 10 crosses the border, so each extract only has one of its ways.
   */
  private fun overlappingExtracts(
    northTimestamp: Long = 1_793_000_000,
    southTimestamp: Long = 1_793_000_000,
  ): List<PbfExtract> {
    val north = directory.resolve("north.osm.pbf")
    PbfWriter.write(
      north,
      replicationTimestamp = northTimestamp,
      nodes =
        listOf(
          OsmNode(1, 52.0, 5.0, nodeNetwork + ("rcn_ref" to "12")),
          OsmNode(2, 52.001, 5.0),
          OsmNode(3, 52.002, 5.0, nodeNetwork + ("rcn_ref" to "45")),
        ),
      ways = listOf(OsmWay(100, listOf(1, 2))),
      relations = listOf(OsmRelation(10, cyclingRoute, listOf(100, 101))),
    )
    val south = directory.resolve("south.osm.pbf")
    PbfWriter.write(
      south,
      replicationTimestamp = southTimestamp,
      nodes =
        listOf(
          OsmNode(2, 52.001, 5.0),
          OsmNode(3, 52.002, 5.0, nodeNetwork + ("rcn_ref" to "45")),
          OsmNode(4, 52.003, 5.0),
          OsmNode(5, 52.004, 5.0, nodeNetwork + ("rcn_ref" to "7")),
        ),
      ways = listOf(OsmWay(101, listOf(2, 3)), OsmWay(102, listOf(3, 4, 5))),
      relations =
        listOf(
          OsmRelation(10, cyclingRoute, listOf(100, 101)),
          OsmRelation(11, cyclingRoute, listOf(102)),
        ),
    )
    return listOf(PbfExtract(north), PbfExtract(south))
  }

  @Test
  fun `knot on a border`() {
    val extraction = NetworkExtraction()

    PbfExtract.readAll(overlappingExtracts(), extraction)

    val graph = extraction.result().graphs.getValue(NetworkType.CYCLING)
    assertEquals(listOf("12", "45", "7"), graph.knots.values.map { it.ref }.sorted())
    assertEquals(
      setOf(1L to 3L, 3L to 5L),
      graph.connections.map { minOf(it.fromId, it.toId) to maxOf(it.fromId, it.toId) }.toSet(),
    )
  }

  @Test
  fun `connection cut by an extract border`() {
    val extracts = overlappingExtracts()
    val northOnly = NetworkExtraction()
    extracts.first().readInto(northOnly)
    val merged = NetworkExtraction()

    PbfExtract.readAll(extracts, merged)

    assertEquals(
      listOf(),
      northOnly.result().graphs.getValue(NetworkType.CYCLING).connections.filter { it.toId == 3L },
    )
    val crossing =
      merged.result().graphs.getValue(NetworkType.CYCLING).connections.single {
        setOf(it.fromId, it.toId) == setOf(1L, 3L)
      }
    assertEquals(
      haversineMeters(52.0, 5.0, 52.002, 5.0),
      crossing.lengthMeters.toDouble(),
      1.0,
    )
    val course = merged.result().courses.getValue(NetworkType.CYCLING).getValue(crossing)
    assertEquals(3, course.size)
  }

  @Test
  fun `data date of several extracts`() {
    val extraction = NetworkExtraction()

    val dataDate =
      PbfExtract.readAll(
        overlappingExtracts(
          northTimestamp = 1_793_000_000,
          southTimestamp = 1_792_800_000,
        ), // 2026-10-24T00:00:00Z
        extraction,
      )

    assertEquals(LocalDate.of(2026, 10, 24), dataDate)
  }
}
