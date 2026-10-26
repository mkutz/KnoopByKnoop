package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkType
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainTest {

  private val directory = createTempDirectory()

  private val nodeNetwork = mapOf("network:type" to "node_network")

  @Test
  fun `build writes one graph file per network and index lists them`() {
    val extract = directory.resolve("utrecht.osm.pbf")
    PbfWriter.write(
      extract,
      replicationTimestamp = 1_793_000_000,
      nodes =
        listOf(
          OsmNode(1, 52.0, 5.0, nodeNetwork + ("rcn_ref" to "12") + ("rwn_ref" to "3")),
          OsmNode(2, 52.001, 5.0, nodeNetwork + ("rcn_ref" to "45") + ("rwn_ref" to "4")),
        ),
      ways = listOf(OsmWay(100, listOf(1, 2))),
      relations =
        listOf(
          OsmRelation(10, nodeNetwork + mapOf("type" to "route", "network" to "rcn"), listOf(100)),
          OsmRelation(11, nodeNetwork + mapOf("type" to "route", "network" to "rwn"), listOf(100)),
        ),
    )
    val output = directory.resolve("output")

    build("utrecht", extract, output)
    index(output)

    val cycling = GraphFile.read(output.resolve("utrecht-cycling.graph").readBytes())
    assertEquals(NetworkType.CYCLING, cycling.network)
    assertEquals("2026-10-26", cycling.dataDate)
    assertEquals(ATTRIBUTION, cycling.attribution)

    val index = NetworkIndex.parse(output.resolve("index.json").readText())
    assertEquals(
      listOf("utrecht-cycling.graph", "utrecht-hiking.graph"),
      index.files.map { it.file },
    )
    assertEquals(
      listOf(IndexedNetworkType.CYCLING, IndexedNetworkType.HIKING),
      index.files.map { it.network },
    )
    index.files.forEach { entry ->
      assertTrue(entry.matches(output.resolve(entry.file).readBytes()), "${entry.file} matches")
    }
  }
}
