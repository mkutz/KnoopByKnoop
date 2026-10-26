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
}
