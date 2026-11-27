package com.knoopbyknoop.web

import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.KnotLocator
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.PathDataOrigin
import com.knoopbyknoop.core.sha256Hex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Reads files of the website, whole or in part. */
interface Fetcher {

  /** The whole file at [path]. */
  suspend fun bytes(path: String): ByteArray

  /** Exactly [size] bytes of the file at [path] from [offset]. */
  suspend fun range(path: String, offset: Long, size: Int): ByteArray
}

/** The network data can't be used: missing, unreachable, or not matching its index. */
class DataProblem(message: String) : Exception(message)

/**
 * A network ready to plan on: the graph, the courses as far as known, and the knots to find taps
 * on. Without the courses yet, connections are straight lines.
 */
class LoadedNetwork(
  val network: IndexedNetworkType,
  val graph: NetworkGraph,
  val geometry: NetworkGeometry,
  val hasGeometry: Boolean,
) {
  val locator = KnotLocator(graph.knots.values)
}

@Serializable private data class CurrentData(val folder: String)

private val json = Json { ignoreUnknownKeys = true }

/**
 * The network data the website serves, see `web-planner` "Network data from the website": the
 * current publication's folder named in `data/index.json`, and only files from that folder, each
 * checked against its index.
 */
class PublishedData(private val fetcher: Fetcher, private val base: String) {

  private var current: Pair<String, NetworkIndex>? = null

  /** The current publication's folder, e.g. `data/20261123T155202Z/`, and its index. */
  suspend fun index(): Pair<String, NetworkIndex> {
    current?.let {
      return it
    }
    val folder =
      try {
        base +
          json
            .decodeFromString<CurrentData>(fetcher.bytes(base + "index.json").decodeToString())
            .folder
      } catch (exception: Exception) {
        throw DataProblem("No network data: ${exception.message}")
      }
    val index =
      try {
        NetworkIndex.parse(fetcher.bytes(folder + "index.json").decodeToString())
      } catch (exception: Exception) {
        throw DataProblem("No network index: ${exception.message}")
      }
    return (folder to index).also { current = it }
  }

  /** The graph of [network], checked against the index. */
  suspend fun graph(network: IndexedNetworkType): Pair<GraphFile, String> {
    val bytes = checkedFile(network, IndexedFileKind.GRAPH)
    return GraphFile.read(bytes) to sha256Hex(bytes)
  }

  /** The courses of [graph], checked against the index. */
  suspend fun geometry(
    network: IndexedNetworkType,
    graph: NetworkGraph,
    graphSha256: String,
  ): NetworkGeometry =
    NetworkGeometry.read(checkedFile(network, IndexedFileKind.GEOMETRY), graph, graphSha256)

  private suspend fun checkedFile(network: IndexedNetworkType, kind: IndexedFileKind): ByteArray {
    val (folder, index) = index()
    val entry =
      index.files.firstOrNull { it.network == network && it.kind == kind }
        ?: throw DataProblem("No ${kind.name.lowercase()} for ${network.name.lowercase()}")
    val bytes =
      try {
        fetcher.bytes(folder + entry.file)
      } catch (exception: Exception) {
        throw DataProblem("Can't load ${entry.file}: ${exception.message}")
      }
    if (!entry.matches(bytes)) throw DataProblem("${entry.file} doesn't match the index")
    return bytes
  }

  /**
   * Runs [load] with an origin of the current publication's squares. The core's downloaders read
   * ranges one after the other and wait for each; the browser can't, so each run notes the ranges
   * it lacked and gets blank bytes, which fail their checksums and are left out. The ranges are
   * then fetched together and [load] runs again, until nothing is lacking.
   */
  suspend fun <T> withSquares(load: (PathDataOrigin) -> T): T {
    val (folder, index) = index()
    val origin = PrefetchedOrigin(index.toJson())
    var result = load(origin)
    repeat(MAX_ROUNDS) {
      if (origin.lacking.isEmpty()) return result
      val wanted = origin.lacking.toList()
      origin.lacking.clear()
      val fetched = coroutineScope {
        wanted
          .map { range ->
            async { range to fetcher.range(folder + range.file, range.offset, range.size) }
          }
          .awaitAll()
      }
      fetched.forEach { (range, bytes) -> origin.fetched[range] = bytes }
      result = load(origin)
    }
    return result
  }

  private data class Range(val file: String, val offset: Long, val size: Int)

  private class PrefetchedOrigin(private val indexText: String) : PathDataOrigin {
    val fetched = mutableMapOf<Range, ByteArray>()
    val lacking = mutableSetOf<Range>()

    override fun index(): String = indexText

    override fun range(file: String, offset: Long, size: Int): ByteArray {
      val range = Range(file, offset, size)
      return fetched[range] ?: ByteArray(size).also { lacking += range }
    }
  }

  private companion object {
    /** The directory, the squares, and once more if the file changed meanwhile. */
    const val MAX_ROUNDS = 4
  }
}
