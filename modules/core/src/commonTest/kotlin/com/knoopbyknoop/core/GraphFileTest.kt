package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    // A version 1 file is a version 2 file without the ferry count and the counts of merged knots
    // and of sign regions at the end.
    val written = graphFile.toBytes()
    val version1 = written.copyOf(written.size - 3).also { it[4] = 1 }

    val read = GraphFile.read(version1)

    assertEquals(graphFile.graph.knots, read.graph.knots)
    assertEquals(graphFile.graph.connections.toSet(), read.graph.connections.toSet())
    assertEquals(mapOf(), read.graph.ferries)
  }

  @Test
  fun `positions of a merged knot`() {
    val knot = TestNetwork.graph.knots.values.first()
    val otherNode = GeoPoint(knot.latitude + 0.0012, knot.longitude - 0.0019)
    val merged = knot.copy(otherNodes = listOf(otherNode))
    val withMergedKnot =
      GraphFile(
        NetworkType.CYCLING,
        "utrecht",
        "2026-10-25",
        "",
        NetworkGraph(
          TestNetwork.graph.knots.values.map { if (it.id == knot.id) merged else it },
          TestNetwork.graph.connections,
        ),
      )

    val read = GraphFile.read(withMergedKnot.toBytes())

    assertEquals(listOf(otherNode), read.graph.knots.getValue(knot.id).otherNodes)
    assertEquals(withMergedKnot.graph.knots, read.graph.knots)
  }

  @Test
  fun `graph files without the positions of merged knots`() {
    // Written before the section: a version 2 file ending after the ferries.
    val written = graphFile.toBytes()
    val beforeMergedKnots = written.copyOf(written.size - 2)

    val read = GraphFile.read(beforeMergedKnots)

    assertEquals(graphFile.graph.knots, read.graph.knots)
    assertEquals(graphFile.graph.connections.toSet(), read.graph.connections.toSet())
  }

  private val withRegions: GraphFile
    get() {
      val knots = TestNetwork.graph.knots.values.sortedBy { it.id }
      val regions =
        listOf(
          SignRegion.NETHERLANDS,
          SignRegion.BELGIUM,
          SignRegion.NORTH_RHINE_WESTPHALIA,
          null,
        )
      return GraphFile(
        NetworkType.CYCLING,
        "utrecht",
        "2026-10-25",
        "",
        NetworkGraph(
          knots.mapIndexed { index, knot -> knot.copy(signRegion = regions[index % regions.size]) },
          TestNetwork.graph.connections,
        ),
      )
    }

  @Test
  fun `sign regions read back the same`() {
    val read = GraphFile.read(withRegions.toBytes())

    assertEquals(withRegions.graph.knots, read.graph.knots)
    assertEquals(
      setOf(
        SignRegion.NETHERLANDS,
        SignRegion.BELGIUM,
        SignRegion.NORTH_RHINE_WESTPHALIA,
        null,
      ),
      read.graph.knots.values.map { it.signRegion }.toSet(),
    )
  }

  @Test
  fun `graph files without sign regions`() {
    // Written before the section: a version 2 file ending after the merged knots.
    val written = graphFile.toBytes()
    val beforeRegions = written.copyOf(written.size - 1)

    val read = GraphFile.read(beforeRegions)

    assertEquals(graphFile.graph.knots, read.graph.knots)
    assertEquals(setOf(null), read.graph.knots.values.map { it.signRegion }.toSet())
  }

  @Test
  fun `apps without sign regions`() {
    // Older readers stop after the merged knots, so everything up to there must stay the same.
    val withoutRegions =
      GraphFile(
        NetworkType.CYCLING,
        "utrecht",
        "2026-10-25",
        "",
        NetworkGraph(
          withRegions.graph.knots.values.map { it.copy(signRegion = null) },
          TestNetwork.graph.connections,
        ),
      )
    val before = withoutRegions.toBytes().let { it.copyOf(it.size - 1) }

    val written = withRegions.toBytes()

    assertEquals(before.toList(), written.copyOf(before.size).toList())
  }

  @Test
  fun `Apps without the positions of merged knots read new files as before`() {
    // Older readers stop after the ferries, so everything up to there must stay the same: each knot
    // at its own position.
    val merged =
      GraphFile(
        NetworkType.CYCLING,
        "utrecht",
        "2026-10-25",
        "",
        NetworkGraph(
          TestNetwork.graph.knots.values.map {
            if (it.id == 12L)
              it.copy(otherNodes = listOf(GeoPoint(it.latitude + 0.0002, it.longitude)))
            else it
          },
          TestNetwork.graph.connections,
        ),
      )
    val unmerged = GraphFile(NetworkType.CYCLING, "utrecht", "2026-10-25", "", TestNetwork.graph)
    // Without merged knots and sign regions, the file ends with two empty sections.
    val before = unmerged.toBytes().let { it.copyOf(it.size - 2) }

    val written = merged.toBytes()

    assertTrue(written.size > before.size + 2, "the merged knot's node is written")
    assertEquals(before.toList(), written.copyOf(before.size).toList())
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
