package com.knoopbyknoop.web

import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.sha256Hex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class PublishedDataTest {

  private val graph = TestNetwork.graph()
  private val graphBytes =
    GraphFile(NetworkType.CYCLING, "test", "2026-11-22", "OSM", graph).toBytes()
  private val geometryBytes = TestNetwork.geometry(graph).toBytes(sha256Hex(graphBytes))

  private fun entry(file: String, bytes: ByteArray, kind: IndexedFileKind) =
    NetworkIndexEntry(
      "test",
      IndexedNetworkType.CYCLING,
      file,
      bytes.size.toLong(),
      sha256Hex(bytes),
      "2026-11-22",
      kind,
    )

  /** The website's files; an older publication still lies next to the current one. */
  private fun site(graphServed: ByteArray = graphBytes) =
    mapOf(
      "data/index.json" to """{"folder":"20261123T155202Z/"}""".encodeToByteArray(),
      "data/20261123T155202Z/index.json" to
        NetworkIndex(
            2,
            "2026-11-23T15:52:02Z",
            listOf(
              entry("cycling.graph", graphBytes, IndexedFileKind.GRAPH),
              entry("cycling.geometry", geometryBytes, IndexedFileKind.GEOMETRY),
            ),
          )
          .toJson()
          .encodeToByteArray(),
      "data/20261123T155202Z/cycling.graph" to graphServed,
      "data/20261123T155202Z/cycling.geometry" to geometryBytes,
      "data/20261116T155202Z/cycling.graph" to "older".encodeToByteArray(),
    )

  private class Site(private val files: Map<String, ByteArray>) : Fetcher {
    val requested = mutableListOf<String>()

    override suspend fun bytes(path: String): ByteArray {
      requested += path
      return files[path] ?: error("404 $path")
    }

    override suspend fun range(path: String, offset: Long, size: Int): ByteArray {
      requested += path
      return bytes(path).copyOfRange(offset.toInt(), offset.toInt() + size)
    }
  }

  @Test
  fun `Data of the latest publication - only files of the publication the index names are read`() =
    runTest {
      val site = Site(site())
      val data = PublishedData(site, "data/")

      val (graphFile, sha) = data.graph(IndexedNetworkType.CYCLING)
      data.geometry(IndexedNetworkType.CYCLING, graphFile.graph, sha)

      assertEquals(graph.knots.keys, graphFile.graph.knots.keys)
      assertEquals("data/index.json", site.requested.first())
      assertTrue(
        site.requested.drop(1).all { it.startsWith("data/20261123T155202Z/") },
        site.requested.toString(),
      )
    }

  @Test
  fun `File not matching the index - the data is not used`() = runTest {
    val broken = graphBytes.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
    val data = PublishedData(Site(site(graphServed = broken)), "data/")

    val problem = assertFailsWith<DataProblem> { data.graph(IndexedNetworkType.CYCLING) }

    assertEquals("cycling.graph doesn't match the index", problem.message)
  }

  @Test
  fun `no data on the site is a problem to tell`() = runTest {
    assertFailsWith<DataProblem> { PublishedData(Site(mapOf()), "data/").index() }
  }
}

class RoutePageTest {

  @Test
  fun `Link without the app - the planner in the browser's language, else English`() {
    assertEquals("nl/plan/", plannerPathFor(listOf("nl-BE", "en")))
    assertEquals("de/plan/", plannerPathFor(listOf("es-ES", "de-AT")))
    assertEquals("plan/", plannerPathFor(listOf("es-ES", "it")))
    assertEquals("plan/", plannerPathFor(listOf("en-GB", "fr")))
  }
}

class FirstViewTest {

  @Test
  fun `Click while zoomed out - zooms in on the spot until the knots show`() {
    assertEquals(CLICK_ZOOM, zoomForClick(7.0))
    assertEquals(CLICK_ZOOM, zoomForClick(9.9))
    assertEquals(null, zoomForClick(KNOT_MIN_ZOOM))
    assertTrue(CLICK_ZOOM >= KNOT_MIN_ZOOM, "the knots show after the zoom")
  }
}
