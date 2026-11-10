package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.IndexedNetworkType.CYCLING
import com.knoopbyknoop.core.IndexedNetworkType.HIKING
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.PathFile
import com.knoopbyknoop.core.PathKind
import com.knoopbyknoop.core.PathSegment
import com.knoopbyknoop.core.PathSquare
import com.knoopbyknoop.core.sha256Hex
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Covers `network-data` "Checked before publication". */
class VerifyTest {

  private val directory = createTempDirectory()

  /** A network of [knots] knots in a line to the north, 111 m apart, each connected to the next. */
  private fun line(network: IndexedNetworkType, knots: Int): NetworkGraph {
    val all =
      (0 until knots).map { Knot(it + 1L, "${it + 1}", network.network, 52.0 + it * 0.001, 5.0) }
    return NetworkGraph(
      all,
      all.zipWithNext().map { (from, to) -> Connection(from.id, to.id, 111) },
    )
  }

  /** Path data of [squares] squares with one segment each. */
  private fun pathBytes(squares: Int = 2): ByteArray =
    PathFile.toBytes(
      (0 until squares).associate { index ->
        val from = GeoPoint(52.0 + index / 16.0 + 0.01, 5.01)
        val to = GeoPoint(from.latitude + 0.001, 5.01)
        PathSquare.of(from.latitude, from.longitude) to
          listOf(
            PathSegment(
              index * 2L,
              index * 2L + 1,
              PathKind.CYCLE,
              true,
              true,
              1.0,
              null,
              listOf(from, to),
            )
          )
      },
      "2026-11-10",
      ATTRIBUTION,
    )

  /** Writes the data of a rebuild into [into] as the build job does, and its index. */
  private fun rebuild(
    into: Path = directory.resolve("rebuilt"),
    graphs: Map<IndexedNetworkType, NetworkGraph> =
      mapOf(CYCLING to line(CYCLING, 100), HIKING to line(HIKING, 100)),
    geometryOf: (IndexedNetworkType, ByteArray) -> ByteArray = { network, graphBytes ->
      NetworkGeometry.straightLines(graphs.getValue(network)).toBytes(sha256Hex(graphBytes))
    },
    paths: ByteArray? = pathBytes(),
  ): Path {
    into.createDirectories()
    for ((network, graph) in graphs) {
      val graphBytes =
        GraphFile(network.network, "nl-be-nrw", "2026-11-10", ATTRIBUTION, graph).toBytes()
      into.resolve(graphFileName(network)).writeBytes(graphBytes)
      into.resolve(geometryFileName(network)).writeBytes(geometryOf(network, graphBytes))
    }
    if (paths != null) into.resolve(PATHS_FILE).writeBytes(paths)
    index(into)
    return into
  }

  private val floors = mapOf(CYCLING to NetworkFloor(90, 89), HIKING to NetworkFloor(90, 89))
  private val pathFloor = PathFloor(squares = 2, segments = 2)

  private fun verifyRebuild(
    rebuilt: Path,
    published: Path? = null,
    allowDrop: Boolean = false,
    knownRoutes: List<KnownRoute> = listOf(),
  ) = verify(rebuilt, published?.toUri()?.toString(), allowDrop, floors, pathFloor, knownRoutes)

  /**
   * Lists [file] in the index of [data] as it is now, for files `index` would refuse, so they reach
   * the checks like a corrupted upload would.
   */
  private fun relist(data: Path, file: String) {
    val bytes = data.resolve(file).readBytes()
    val index = NetworkIndex.parse(data.resolve("index.json").readText())
    val files =
      index.files.map {
        if (it.file != file) it
        else it.copy(sizeBytes = bytes.size.toLong(), sha256 = sha256Hex(bytes))
      }
    data.resolve("index.json").writeText(index.copy(files = files).toJson())
  }

  private fun published(cyclingKnots: Int = 100, hikingKnots: Int = 100) =
    rebuild(
      directory.resolve("published"),
      mapOf(CYCLING to line(CYCLING, cyclingKnots), HIKING to line(HIKING, hikingKnots)),
    )

  @Test
  fun `a rebuild passing every check passes`() {
    val verification = verifyRebuild(rebuild(), published())

    assertEquals(listOf(), verification.failures)
    assertTrue(verification.log.any { it.startsWith("cycling.paths: 2 squares, 2 segments") })
  }

  // Scenario: File the apps would reject

  @Test
  fun `a geometry of another graph fails`() {
    val rebuilt = rebuild()
    val other =
      GraphFile(HIKING.network, "nl-be-nrw", "2026-11-10", ATTRIBUTION, line(HIKING, 3)).toBytes()
    rebuilt
      .resolve("hiking.geometry")
      .writeBytes(NetworkGeometry.straightLines(line(HIKING, 100)).toBytes(sha256Hex(other)))
    relist(rebuilt, "hiking.geometry")

    val verification = verifyRebuild(rebuilt)

    assertEquals(listOf("hiking.geometry is MISMATCHING_GEOMETRY"), verification.failures)
  }

  @Test
  fun `a changed byte in a path square fails`() {
    val rebuilt = rebuild()
    val paths = rebuilt.resolve(PATHS_FILE)
    val bytes = paths.readBytes()
    bytes[bytes.size - 1] = (bytes[bytes.size - 1] + 1).toByte()
    paths.writeBytes(bytes)

    val verification = verifyRebuild(rebuilt)

    // The broken square's segments are not counted, so the floor fails as well.
    assertTrue(verification.failures.first().startsWith("cycling.paths has 1 broken squares"))
  }

  @Test
  fun `a graph changed after the index fails`() {
    val rebuilt = rebuild()
    rebuilt.resolve("cycling.graph").writeBytes(rebuilt.resolve("cycling.graph").readBytes() + 0)

    val verification = verifyRebuild(rebuilt)

    // The network can't be read, so its floor fails as well.
    assertEquals(
      listOf(
        "cycling.graph is CORRUPTED",
        "cycling has 0 knots and 0 connections, fewer than 90 and 89",
      ),
      verification.failures,
    )
  }

  // Scenario: Too few knots

  @Test
  fun `a network below its floor fails`() {
    val rebuilt = rebuild(graphs = mapOf(CYCLING to line(CYCLING, 60), HIKING to line(HIKING, 100)))

    val verification = verifyRebuild(rebuilt)

    assertEquals(
      listOf("cycling has 60 knots and 59 connections, fewer than 90 and 89"),
      verification.failures,
    )
  }

  @Test
  fun `a missing network fails as zero knots`() {
    val rebuilt = rebuild(graphs = mapOf(CYCLING to line(CYCLING, 100)))

    val verification = verifyRebuild(rebuilt)

    assertEquals(
      listOf("hiking has 0 knots and 0 connections, fewer than 90 and 89"),
      verification.failures,
    )
  }

  @Test
  fun `path data below its floor fails`() {
    val rebuilt = rebuild(paths = pathBytes(squares = 1))

    val verification = verifyRebuild(rebuilt)

    assertEquals(
      listOf("cycling.paths has 1 squares and 1 segments, fewer than 2 and 2"),
      verification.failures,
    )
  }

  @Test
  fun `missing path data fails`() {
    val verification = verifyRebuild(rebuild(paths = null))

    assertEquals(listOf("the path data is missing"), verification.failures)
  }

  // Scenario: Sudden drop

  @Test
  fun `losing more than 5 percent of the knots fails`() {
    val published = published(hikingKnots = 100)
    val rebuilt = rebuild(graphs = mapOf(CYCLING to line(CYCLING, 100), HIKING to line(HIKING, 92)))

    val verification = verifyRebuild(rebuilt, published)

    assertEquals(
      listOf(
        "hiking dropped from 100 → 92 knots (-8.0 %), more than -5.0 %",
        "hiking dropped from 99 → 91 connections (-8.1 %), more than -5.0 %",
      ),
      verification.failures,
    )
  }

  @Test
  fun `losing more than 5 percent of the connections fails`() {
    val published = published()
    val knots = line(HIKING, 100).knots.values
    val fewerConnections =
      NetworkGraph(
        knots,
        line(HIKING, 100).connections.filterIndexed { index, _ -> index % 12 != 0 },
      )
    val rebuilt = rebuild(graphs = mapOf(CYCLING to line(CYCLING, 100), HIKING to fewerConnections))

    val verification = verifyRebuild(rebuilt, published, knownRoutes = listOf())

    assertEquals(
      listOf("hiking dropped from 99 → 90 connections (-9.1 %), more than -5.0 %"),
      verification.failures,
    )
  }

  // Scenario: Small change

  @Test
  fun `a small loss and growth pass`() {
    val published = published(cyclingKnots = 100, hikingKnots = 97)
    val rebuilt = rebuild(graphs = mapOf(CYCLING to line(CYCLING, 98), HIKING to line(HIKING, 100)))

    val verification = verifyRebuild(rebuilt, published)

    assertEquals(listOf(), verification.failures)
    assertTrue("cycling: 100 → 98 knots (-2.0 %)" in verification.log)
    assertTrue("hiking: 97 → 100 knots (+3.1 %)" in verification.log)
  }

  // Scenario: Deliberate drop

  @Test
  fun `an allowed drop passes`() {
    val published = published(hikingKnots = 100)
    val rebuilt = rebuild(graphs = mapOf(CYCLING to line(CYCLING, 100), HIKING to line(HIKING, 92)))

    val verification = verifyRebuild(rebuilt, published, allowDrop = true)

    assertEquals(listOf(), verification.failures)
    assertTrue("drop not checked: allowed for this run" in verification.log)
  }

  @Test
  fun `an allowed drop below the floor still fails`() {
    val published = published(hikingKnots = 100)
    val rebuilt = rebuild(graphs = mapOf(CYCLING to line(CYCLING, 100), HIKING to line(HIKING, 50)))

    val verification = verifyRebuild(rebuilt, published, allowDrop = true)

    assertEquals(
      listOf("hiking has 50 knots and 49 connections, fewer than 90 and 89"),
      verification.failures,
    )
  }

  // Scenario: Nothing published yet

  @Test
  fun `nothing published skips the drop with a warning`() {
    val nothing = directory.resolve("nothing-published")

    val verification = verifyRebuild(rebuild(), nothing)

    assertEquals(listOf(), verification.failures)
    assertTrue(
      "WARNING: drop not checked, the published index.json cannot be fetched" in verification.log
    )
  }

  @Test
  fun `a published graph of another format skips its drop with a warning`() {
    val published = published()
    val graph = published.resolve("hiking.graph")
    val bytes = graph.readBytes()
    // The format version follows the magic number, see GraphFile.
    bytes[4] = 99
    graph.writeBytes(bytes)
    relist(published, "hiking.graph")

    val verification = verifyRebuild(rebuild(), published)

    assertEquals(listOf(), verification.failures)
    assertTrue(
      "WARNING: hiking drop not checked, the published hiking.graph is UNSUPPORTED_VERSION" in
        verification.log
    )
  }

  // Scenario: Known route lost

  private val northward =
    KnownRoute("Northward", CYCLING, GeoPoint(52.0, 5.0), GeoPoint(52.099, 5.0))

  @Test
  fun `a known route along the network passes`() {
    val verification = verifyRebuild(rebuild(), knownRoutes = listOf(northward))

    assertEquals(listOf(), verification.failures)
    assertTrue(verification.log.any { it.startsWith("Northward (1 → 100): 10989 m") })
  }

  @Test
  fun `no knot near a known route's position fails`() {
    val faraway = northward.copy(to = GeoPoint(53.0, 5.0))

    val verification = verifyRebuild(rebuild(), knownRoutes = listOf(faraway))

    assertEquals(listOf("Northward: no knot within 1000 m of 53.0, 5.0"), verification.failures)
  }

  @Test
  fun `a known route without a way between its knots fails`() {
    val knots = line(CYCLING, 100).knots.values
    val split = NetworkGraph(knots, line(CYCLING, 100).connections.filter { it.fromId != 50L })
    val rebuilt = rebuild(graphs = mapOf(CYCLING to split, HIKING to line(HIKING, 100)))

    val verification = verify(rebuilt, null, false, mapOf(), pathFloor, listOf(northward))

    assertEquals(listOf("Northward (1 → 100): no route"), verification.failures)
  }

  @Test
  fun `a known route much longer than the straight line fails`() {
    // 1 and 2 are 111 m apart, but only connected through 3, 1 km to the east.
    val knots =
      listOf(
        Knot(1, "1", CYCLING.network, 52.0, 5.0),
        Knot(2, "2", CYCLING.network, 52.001, 5.0),
        Knot(3, "3", CYCLING.network, 52.0005, 5.015),
      )
    val detour = NetworkGraph(knots, listOf(Connection(1, 3, 1_100), Connection(3, 2, 1_100)))
    val rebuilt = rebuild(graphs = mapOf(CYCLING to detour, HIKING to line(HIKING, 100)))
    val route = KnownRoute("Detour", CYCLING, GeoPoint(52.0, 5.0), GeoPoint(52.001, 5.0))

    val verification = verify(rebuilt, null, false, mapOf(), pathFloor, listOf(route))

    assertEquals(
      listOf("Detour (1 → 2): 2200 m, more than 2.0 times the straight line of 111 m"),
      verification.failures,
    )
  }
}
