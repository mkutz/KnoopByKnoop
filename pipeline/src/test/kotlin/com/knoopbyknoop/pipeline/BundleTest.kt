package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.sha256Hex
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Covers the bundling behind `network-sync` "Bundled graphs". */
class BundleTest {

  private val directory = createTempDirectory()
  private val published = directory.resolve("published").createDirectories()
  private val output = directory.resolve("bundled")

  private fun graphBytes(network: NetworkType): ByteArray {
    val knots = listOf(Knot(1, "12", network, 52.0, 5.0), Knot(2, "45", network, 52.001, 5.0))
    val graph = NetworkGraph(knots, listOf(Connection(1, 2, 111)))
    return GraphFile(network, "nl-be-nrw", "2026-10-26", ATTRIBUTION, graph).toBytes()
  }

  /** Publishes graph and geometry files of both networks like the weekly workflow does. */
  private fun publish(corruptHiking: Boolean = false) {
    val files =
      IndexedNetworkType.entries.flatMap { network ->
        val bytes = graphBytes(network.network)
        val graph = GraphFile.read(bytes).graph
        val geometry = NetworkGeometry.straightLines(graph).toBytes(sha256Hex(bytes))
        val name = network.name.lowercase()
        val published =
          if (corruptHiking && network == IndexedNetworkType.HIKING) bytes.copyOf(bytes.size - 1)
          else bytes
        this.published.resolve("$name.graph").writeBytes(published)
        this.published.resolve("$name.geometry").writeBytes(geometry)
        listOf(
          NetworkIndexEntry(
            "nl-be-nrw",
            network,
            "$name.graph",
            bytes.size.toLong(),
            sha256Hex(bytes),
            "2026-10-26",
            IndexedFileKind.GRAPH,
          ),
          NetworkIndexEntry(
            "nl-be-nrw",
            network,
            "$name.geometry",
            geometry.size.toLong(),
            sha256Hex(geometry),
            "2026-10-26",
            IndexedFileKind.GEOMETRY,
          ),
        )
      }
    published
      .resolve("index.json")
      .writeText(NetworkIndex(1, "2026-10-26T04:00:00Z", files).toJson())
  }

  private val baseUrl
    get() = published.toUri().toString()

  private fun bundledIndex() =
    NetworkIndex.parse(output.resolve(NetworkIndex.BUNDLED_INDEX_FILE).readText())

  private fun Path.fileNames() = listDirectoryEntries().map { it.name }.sorted()

  @Test
  fun `bundles the verified graphs without geometry`() {
    publish()

    bundle(baseUrl, output)

    assertEquals(listOf("bundled-index.json", "cycling.graph", "hiking.graph"), output.fileNames())
    assertEquals(listOf("cycling.graph", "hiking.graph"), bundledIndex().files.map { it.file })
    bundledIndex().files.forEach { entry ->
      assertEquals(true, entry.matches(output.resolve(entry.file).readBytes()), entry.file)
    }
  }

  @Test
  fun `a corrupted graph fails the bundle`() {
    publish(corruptHiking = true)

    val exception = assertFailsWith<BundleException> { bundle(baseUrl, output) }

    assertEquals("hiking.graph is CORRUPTED", exception.message)
  }

  @Test
  fun `unavailable data fails the bundle unless allowed`() {
    val unreachable = directory.resolve("nothing-published").toUri().toString()

    assertFailsWith<BundleException> { bundle(unreachable, output) }

    bundle(unreachable, output, allowUnavailable = true)
    assertEquals(listOf("bundled-index.json"), output.fileNames())
    assertEquals(listOf(), bundledIndex().files)
  }

  @Test
  fun `bundling again replaces the previous bundle`() {
    publish()
    output.createDirectories()
    output.resolve("netherlands-cycling.graph").writeBytes(byteArrayOf(1))

    bundle(baseUrl, output)

    assertEquals(false, output.resolve("netherlands-cycling.graph").exists())
  }
}
