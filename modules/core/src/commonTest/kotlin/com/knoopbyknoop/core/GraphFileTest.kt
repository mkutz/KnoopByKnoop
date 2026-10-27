package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GraphFileTest {

  private val graphFile =
    GraphFile(
      network = NetworkType.CYCLING,
      area = "utrecht",
      dataDate = "2026-10-25",
      attribution = "© OpenStreetMap contributors, ODbL",
      graph = TestNetwork.graph,
    )

  @Test
  fun `a written graph file reads back the same`() {
    val read = GraphFile.read(graphFile.toBytes())

    assertEquals(graphFile.network, read.network)
    assertEquals(graphFile.area, read.area)
    assertEquals(graphFile.dataDate, read.dataDate)
    assertEquals(graphFile.attribution, read.attribution)
    assertEquals(graphFile.graph.knots, read.graph.knots)
    assertEquals(graphFile.graph.connections.toSet(), read.graph.connections.toSet())
  }

  @Test
  fun `ferry details read back the same`() {
    val ferryConnection = TestNetwork.graph.connections.first()
    val ferry =
      Ferry(
        name = "Veerpont Beusichem",
        openingHours = "Apr-Oct: Mo-Su 07:00-19:00",
        charge = "1.50 EUR",
        website = "https://example.org/veer",
      )
    val withFerry =
      GraphFile(
        NetworkType.CYCLING,
        "utrecht",
        "2026-10-25",
        "",
        NetworkGraph(
          TestNetwork.graph.knots.values,
          TestNetwork.graph.connections,
          mapOf(ferryConnection to ferry),
        ),
      )

    val read = GraphFile.read(withFerry.toBytes())

    assertEquals(mapOf(ferryConnection to ferry), read.graph.ferries)
  }

  @Test
  fun `older graph files`() {
    // A version 1 file is a version 2 file without ferries, without the ferry count at the end.
    val written = graphFile.toBytes()
    val version1 = written.copyOf(written.size - 1).also { it[4] = 1 }

    val read = GraphFile.read(version1)

    assertEquals(graphFile.graph.knots, read.graph.knots)
    assertEquals(graphFile.graph.connections.toSet(), read.graph.connections.toSet())
    assertEquals(mapOf(), read.graph.ferries)
  }

  @Test
  fun `unsupported format version`() {
    val bytes = graphFile.toBytes().also { it[4] = 99 }

    val exception = assertFailsWith<GraphFormatException> { GraphFile.read(bytes) }
    assertEquals("Unsupported graph file format version 99", exception.message)
  }

  @Test
  fun `other files are rejected`() {
    assertFailsWith<GraphFormatException> { GraphFile.read("<osm/>".encodeToByteArray()) }
  }

  @Test
  fun `truncated files are rejected`() {
    val bytes = graphFile.toBytes()

    assertFailsWith<GraphFormatException> { GraphFile.read(bytes.copyOf(bytes.size - 3)) }
  }

  @Test
  fun `the network of the graph must match the file`() {
    assertFailsWith<IllegalArgumentException> {
      GraphFile(NetworkType.HIKING, "utrecht", "2026-10-25", "", TestNetwork.graph)
    }
  }
}
