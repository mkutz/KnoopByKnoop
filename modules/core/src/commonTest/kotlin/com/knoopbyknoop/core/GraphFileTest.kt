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
