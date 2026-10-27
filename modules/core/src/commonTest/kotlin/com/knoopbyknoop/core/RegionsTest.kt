package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Covers the region scenarios of `openspec/changes/add-planning-screens`. */
class RegionsTest {

  private val graphBytes =
    GraphFile(NetworkType.CYCLING, "netherlands", "2026-10-26", "", TestNetwork.graph).toBytes()
  private val graphEntry = entry("netherlands", IndexedFileKind.GRAPH, graphBytes, "2026-10-26")
  private val geometryBytes =
    NetworkGeometry.straightLines(TestNetwork.graph).toBytes(sha256Hex(graphBytes))
  private val geometryEntry =
    entry("netherlands", IndexedFileKind.GEOMETRY, geometryBytes, "2026-10-26")

  private fun entry(region: String, kind: IndexedFileKind, bytes: ByteArray, dataDate: String) =
    NetworkIndexEntry(
      region = region,
      network = IndexedNetworkType.CYCLING,
      file = "$region-cycling.${kind.name.lowercase()}",
      sizeBytes = bytes.size.toLong(),
      sha256 = sha256Hex(bytes),
      dataDate = dataDate,
      kind = kind,
    )

  private fun publishedEntry(region: String, sizeBytes: Long) =
    NetworkIndexEntry(
      region,
      IndexedNetworkType.CYCLING,
      "$region-cycling.graph",
      sizeBytes,
      "00",
      "2026-10-26",
    )

  private val index =
    NetworkIndex(
      formatVersion = 1,
      generatedAt = "2026-10-26T04:00:00Z",
      files =
        listOf(
          publishedEntry("netherlands", 3_700_000),
          publishedEntry("belgium", 1_700_000),
          publishedEntry("nordrhein-westfalen", 600_000),
        ),
    )

  // Requirement: List available regions

  @Test
  fun `first start`() {
    val catalog = RegionCatalog(index, installed = listOf())

    assertEquals(
      listOf("Belgium", "Netherlands", "Nordrhein-Westfalen"),
      catalog.regions.map { it.name },
    )
    assertEquals(
      listOf(1_700_000L, 3_700_000L, 600_000L),
      catalog.regions.map { it.sizeBytes },
    )
    assertTrue(
      catalog.regions.all { it.state == RegionState.AVAILABLE && it.dataDate == "2026-10-26" }
    )
  }

  @Test
  fun `no network access`() {
    val catalog = RegionCatalog(index = null, installed = listOf(graphEntry, geometryEntry))

    assertEquals(listOf("Netherlands"), catalog.regions.map { it.name })
    assertEquals(RegionState.INSTALLED, catalog.regions.single().state)
    assertTrue(catalog.otherRegionsNeedNetworkAccess)
  }

  @Test
  fun `downloading regions`() {
    val catalog = RegionCatalog(index, installed = listOf(), downloading = setOf("belgium"))

    assertEquals(RegionState.DOWNLOADING, catalog.regions.first { it.region == "belgium" }.state)
  }

  // Requirement: Update regions

  @Test
  fun `newer data available`() {
    val installed = listOf(graphEntry.copy(dataDate = "2026-10-19"))

    val catalog = RegionCatalog(index, installed)

    assertEquals(RegionState.UPDATABLE, catalog.regions.first { it.region == "netherlands" }.state)
  }

  @Test
  fun `current data is installed`() {
    val catalog = RegionCatalog(index, listOf(graphEntry))

    assertEquals(RegionState.INSTALLED, catalog.regions.first { it.region == "netherlands" }.state)
  }

  // Requirement: Install only verified data

  @Test
  fun `verified files are valid`() {
    val graph =
      assertIs<InstallCheck.Valid<GraphFile>>(RegionInstaller.checkGraph(graphEntry, graphBytes))
    assertIs<InstallCheck.Valid<NetworkGeometry>>(
      RegionInstaller.checkGeometry(geometryEntry, geometryBytes, graphEntry, graph.content.graph)
    )
  }

  @Test
  fun `corrupted download`() {
    val corrupted = graphBytes.copyOf().also { it[it.lastIndex] = (it.last() + 1).toByte() }

    assertEquals(
      InstallCheck.Rejected(RejectionReason.CORRUPTED),
      RegionInstaller.checkGraph(graphEntry, corrupted),
    )
    assertEquals(
      InstallCheck.Rejected(RejectionReason.CORRUPTED),
      RegionInstaller.checkGraph(graphEntry, graphBytes.copyOf(graphBytes.size - 1)),
    )
  }

  @Test
  fun `unsupported file version`() {
    val newer = graphBytes.copyOf().also { it[4] = 2 }

    assertEquals(
      InstallCheck.Rejected(RejectionReason.UNSUPPORTED_VERSION),
      RegionInstaller.checkGraph(
        entry("netherlands", IndexedFileKind.GRAPH, newer, "2026-10-26"),
        newer,
      ),
    )
  }

  @Test
  fun `geometry of another graph build is rejected`() {
    val otherGraphEntry = graphEntry.copy(sha256 = sha256Hex("other build".encodeToByteArray()))

    assertEquals(
      InstallCheck.Rejected(RejectionReason.MISMATCHING_GEOMETRY),
      RegionInstaller.checkGeometry(
        geometryEntry,
        geometryBytes,
        otherGraphEntry,
        TestNetwork.graph,
      ),
    )
  }

  @Test
  fun `unknown region IDs are capitalized`() {
    assertEquals("Noord Brabant", RegionCatalog.nameOf("noord-brabant"))
  }
}
