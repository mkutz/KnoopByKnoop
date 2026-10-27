package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Covers the scenarios of `network-sync`. */
class NetworkSyncTest {

  private val area = "nl-be-nrw"

  private fun bytes(network: NetworkType, dataDate: String) =
    GraphFile(network, area, dataDate, "", TestNetwork.graph.ofNetwork(network)).toBytes()

  private fun NetworkGraph.ofNetwork(network: NetworkType) =
    NetworkGraph(knots.values.map { it.copy(network = network) }, connections)

  private fun entry(
    network: IndexedNetworkType,
    kind: IndexedFileKind,
    bytes: ByteArray,
    dataDate: String,
  ) =
    NetworkIndexEntry(
      area = area,
      network = network,
      file = "${network.name.lowercase()}.${kind.name.lowercase()}",
      sizeBytes = bytes.size.toLong(),
      sha256 = sha256Hex(bytes),
      dataDate = dataDate,
      kind = kind,
    )

  private fun graphEntry(network: IndexedNetworkType, dataDate: String) =
    entry(network, IndexedFileKind.GRAPH, bytes(network.network, dataDate), dataDate)

  private fun geometryEntry(network: IndexedNetworkType, dataDate: String) =
    entry(
      network,
      IndexedFileKind.GEOMETRY,
      NetworkGeometry.straightLines(TestNetwork.graph)
        .toBytes(graphEntry(network, dataDate).sha256),
      dataDate,
    )

  private fun publication(dataDate: String) =
    NetworkIndex(
      formatVersion = 1,
      generatedAt = "${dataDate}T04:00:00Z",
      files =
        listOf(
          graphEntry(IndexedNetworkType.HIKING, dataDate),
          geometryEntry(IndexedNetworkType.HIKING, dataDate),
          graphEntry(IndexedNetworkType.CYCLING, dataDate),
          geometryEntry(IndexedNetworkType.CYCLING, dataDate),
        ),
    )

  private val index = publication("2026-10-26")

  private val bundled =
    NetworkIndex(
      formatVersion = 1,
      generatedAt = "2026-10-26T04:00:00Z",
      files =
        listOf(
          graphEntry(IndexedNetworkType.CYCLING, "2026-10-26"),
          graphEntry(IndexedNetworkType.HIKING, "2026-10-26"),
        ),
    )

  // Requirement: Download the networks automatically

  @Test
  fun `first start`() {
    val catalog = NetworkCatalog(index, installed = bundled.files)

    assertEquals(
      listOf("cycling.geometry", "hiking.geometry"),
      catalog.filesToFetch.map { it.file },
    )
  }

  @Test
  fun `first start without bundled graphs fetches graphs too and cycling first`() {
    val catalog = NetworkCatalog(index, installed = listOf())

    assertEquals(
      listOf("cycling.graph", "cycling.geometry", "hiking.graph", "hiking.geometry"),
      catalog.filesToFetch.map { it.file },
    )
    assertTrue(catalog.networks.all { it.state == NetworkState.MISSING })
  }

  @Test
  fun `completely downloaded files are not downloaded again`() {
    val installed = bundled.files + geometryEntry(IndexedNetworkType.CYCLING, "2026-10-26")

    val catalog = NetworkCatalog(index, installed)

    assertEquals(listOf("hiking.geometry"), catalog.filesToFetch.map { it.file })
  }

  // Requirement: Bundled graphs

  @Test
  fun `planning right after installation`() {
    val catalog = NetworkCatalog(index = null, installed = listOf())

    val toInstall = catalog.bundledToInstall(bundled)

    assertEquals(listOf("cycling.graph", "hiking.graph"), toInstall.map { it.file })
    val cycling = toInstall.first()
    val graphFile =
      assertIs<InstallCheck.Valid<GraphFile>>(
          NetworkInstaller.checkGraph(cycling, bytes(NetworkType.CYCLING, cycling.dataDate))
        )
        .content
    val planned =
      PlanningSession.start(graphFile.graph).tap(TestNetwork.knot12).tap(TestNetwork.knot45)
    assertIs<RouteSummary.Complete>(planned.route?.summary)
    val installedCatalog = NetworkCatalog(index = null, installed = toInstall)
    assertTrue(installedCatalog.networks.all { it.state == NetworkState.INSTALLED })
    assertTrue(installedCatalog.networks.none { it.hasGeometry })
  }

  @Test
  fun `newer data already installed`() {
    val installed = publication("2026-11-02").files

    val catalog = NetworkCatalog(index = null, installed)

    assertEquals(listOf(), catalog.bundledToInstall(bundled))
  }

  @Test
  fun `bundled graphs replace older installed ones`() {
    val installed = publication("2026-10-19").files

    val toInstall = NetworkCatalog(index = null, installed).bundledToInstall(bundled)

    assertEquals(listOf("cycling.graph", "hiking.graph"), toInstall.map { it.file })
  }

  @Test
  fun `per-region data is replaced although its data date is newer`() {
    // Before the networks were merged, each region was installed on its own, and the merged data
    // has the oldest date of its extracts.
    val perRegion = publication("2026-10-27").files.map { it.copy(area = "netherlands") }

    val catalog = NetworkCatalog(index, perRegion)

    assertEquals(
      listOf("cycling.graph", "hiking.graph"),
      catalog.bundledToInstall(bundled).map { it.file },
    )
    assertEquals(4, catalog.filesToFetch.size)
  }

  @Test
  fun `data date of a release`() {
    val catalog = NetworkCatalog(index = null, installed = bundled.files)

    assertEquals("2026-10-26", catalog.status(IndexedNetworkType.CYCLING).dataDate)
  }

  // Requirement: Courses still loading

  @Test
  fun `cycling is ready first`() {
    val installed = bundled.files + geometryEntry(IndexedNetworkType.CYCLING, "2026-10-26")

    val catalog = NetworkCatalog(index, installed, downloading = setOf(IndexedNetworkType.HIKING))

    assertTrue(catalog.status(IndexedNetworkType.CYCLING).hasGeometry)
    assertEquals(NetworkState.INSTALLED, catalog.status(IndexedNetworkType.CYCLING).state)
    assertFalse(catalog.status(IndexedNetworkType.HIKING).hasGeometry)
    assertEquals(NetworkState.DOWNLOADING, catalog.status(IndexedNetworkType.HIKING).state)
  }

  @Test
  fun `geometry of an older graph does not count`() {
    val installed = bundled.files + geometryEntry(IndexedNetworkType.CYCLING, "2026-10-19")

    val catalog = NetworkCatalog(index = null, installed)

    assertFalse(catalog.status(IndexedNetworkType.CYCLING).hasGeometry)
  }

  // Requirement: Keep the networks current

  @Test
  fun `newer data available`() {
    val installed = publication("2026-10-19").files

    val catalog = NetworkCatalog(index, installed)

    assertTrue(catalog.networks.all { it.state == NetworkState.UPDATABLE })
    assertEquals("2026-10-19", catalog.status(IndexedNetworkType.CYCLING).dataDate)
    assertEquals(4, catalog.filesToFetch.size)
  }

  @Test
  fun `current data is installed`() {
    val catalog = NetworkCatalog(index, index.files)

    assertEquals(listOf(), catalog.filesToFetch)
    assertTrue(catalog.networks.all { it.state == NetworkState.INSTALLED && it.hasGeometry })
  }

  @Test
  fun `the update check is due once a day`() {
    val now = 1_793_000_000_000L

    assertTrue(UpdateCheck.isDue(lastCheckEpochMillis = null, nowEpochMillis = now))
    assertFalse(UpdateCheck.isDue(now - UpdateCheck.INTERVAL_MILLIS + 1, now))
    assertTrue(UpdateCheck.isDue(now - UpdateCheck.INTERVAL_MILLIS, now))
  }

  // Requirement: Info sheet

  @Test
  fun `failed update`() {
    val installed = publication("2026-10-19").files

    val catalog = NetworkCatalog(index, installed, failed = setOf(IndexedNetworkType.HIKING))

    assertTrue(catalog.status(IndexedNetworkType.HIKING).lastAttemptFailed)
    assertFalse(catalog.status(IndexedNetworkType.CYCLING).lastAttemptFailed)
    assertEquals(NetworkState.UPDATABLE, catalog.status(IndexedNetworkType.HIKING).state)
  }

  // Requirement: Install only verified data

  private val graphBytes = bytes(NetworkType.CYCLING, "2026-10-26")
  private val cyclingGraph = graphEntry(IndexedNetworkType.CYCLING, "2026-10-26")
  private val geometryBytes =
    NetworkGeometry.straightLines(TestNetwork.graph).toBytes(sha256Hex(graphBytes))
  private val cyclingGeometry = geometryEntry(IndexedNetworkType.CYCLING, "2026-10-26")

  @Test
  fun `verified files are valid`() {
    val graph =
      assertIs<InstallCheck.Valid<GraphFile>>(NetworkInstaller.checkGraph(cyclingGraph, graphBytes))
    assertIs<InstallCheck.Valid<NetworkGeometry>>(
      NetworkInstaller.checkGeometry(
        cyclingGeometry,
        geometryBytes,
        cyclingGraph,
        graph.content.graph,
      )
    )
  }

  @Test
  fun `corrupted download`() {
    val corrupted = graphBytes.copyOf().also { it[it.lastIndex] = (it.last() + 1).toByte() }

    assertEquals(
      InstallCheck.Rejected(RejectionReason.CORRUPTED),
      NetworkInstaller.checkGraph(cyclingGraph, corrupted),
    )
    assertEquals(
      InstallCheck.Rejected(RejectionReason.CORRUPTED),
      NetworkInstaller.checkGraph(cyclingGraph, graphBytes.copyOf(graphBytes.size - 1)),
    )
  }

  @Test
  fun `unsupported file version`() {
    val newer = graphBytes.copyOf().also { it[4] = 2 }

    assertEquals(
      InstallCheck.Rejected(RejectionReason.UNSUPPORTED_VERSION),
      NetworkInstaller.checkGraph(
        entry(IndexedNetworkType.CYCLING, IndexedFileKind.GRAPH, newer, "2026-10-26"),
        newer,
      ),
    )
  }

  @Test
  fun `geometry of another graph build is rejected`() {
    val otherGraphEntry = cyclingGraph.copy(sha256 = sha256Hex("other build".encodeToByteArray()))

    assertEquals(
      InstallCheck.Rejected(RejectionReason.MISMATCHING_GEOMETRY),
      NetworkInstaller.checkGeometry(
        cyclingGeometry,
        geometryBytes,
        otherGraphEntry,
        TestNetwork.graph,
      ),
    )
  }
}
