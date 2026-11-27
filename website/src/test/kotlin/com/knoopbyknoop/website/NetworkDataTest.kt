package com.knoopbyknoop.website

import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.PathIndexEntry
import com.knoopbyknoop.core.sha256Hex
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkDataTest {

  /** A small publication: a cycling graph and path data whose first 4 bytes are the directory. */
  private fun publication(): File {
    val directory = Files.createTempDirectory("network-data").toFile()
    val graph = "graph".encodeToByteArray()
    val paths = "DIR!squares".encodeToByteArray()
    directory.resolve("cycling.graph").writeBytes(graph)
    directory.resolve("cycling.paths").writeBytes(paths)
    val index =
      NetworkIndex(
        formatVersion = 2,
        generatedAt = "2026-11-23T15:52:02Z",
        files =
          listOf(
            NetworkIndexEntry(
              "nl-be-nrw",
              IndexedNetworkType.CYCLING,
              "cycling.graph",
              graph.size.toLong(),
              sha256Hex(graph),
              "2026-11-22",
            )
          ),
        paths =
          PathIndexEntry(
            "cycling.paths",
            paths.size.toLong(),
            "2026-11-22",
            2,
            0,
            4,
            sha256Hex("DIR!".encodeToByteArray()),
          ),
      )
    directory.resolve("index.json").writeText(index.toJson())
    return directory
  }

  @Test
  fun `a complete publication has no problems and is copied into its own folder`() {
    val data = NetworkData(publication())
    val site = Files.createTempDirectory("site").toFile()

    assertEquals(emptyList(), networkDataProblems(data))
    writeNetworkData(data, site)

    assertEquals(
      """{"folder":"20261123T155202Z/"}""",
      site.resolve("data/index.json").readText().trim(),
    )
    assertEquals(
      setOf("index.json", "cycling.graph", "cycling.paths"),
      site.resolve("data/20261123T155202Z").list()!!.toSet(),
    )
  }

  @Test
  fun `a file named in the index but missing fails naming it`() {
    val directory = publication()
    directory.resolve("cycling.graph").delete()

    assertEquals(
      listOf("Network data file missing: cycling.graph"),
      networkDataProblems(NetworkData(directory)),
    )
  }

  @Test
  fun `changed files fail naming them`() {
    val directory = publication()
    directory.resolve("cycling.graph").writeText("grapH")
    directory.resolve("cycling.paths").writeText("DIR?squares")

    assertEquals(
      listOf(
        "Network data file doesn't match the index: cycling.graph",
        "Network data file doesn't match the index: cycling.paths",
      ),
      networkDataProblems(NetworkData(directory)),
    )
  }

  @Test
  fun `a directory without an index fails`() {
    val empty = Files.createTempDirectory("network-data").toFile()

    assertEquals(1, networkDataProblems(NetworkData(empty)).size)
  }
}
