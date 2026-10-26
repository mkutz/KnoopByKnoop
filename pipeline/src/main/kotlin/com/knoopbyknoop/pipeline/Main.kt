package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.sha256Hex
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.system.exitProcess

const val ATTRIBUTION =
  "© OpenStreetMap contributors, ODbL 1.0, https://www.openstreetmap.org/copyright"

private const val USAGE =
  """Usage:
  pipeline build <region> <extract.osm.pbf> <output-dir>   extract graph files for one region
  pipeline index <output-dir>                              write index.json for all graph files"""

/** Turns OpenStreetMap node network data into the graph files the apps download. */
fun main(args: Array<String>) {
  when (args.firstOrNull()) {
    "build" if args.size == 4 -> build(args[1], Path(args[2]), Path(args[3]))
    "index" if args.size == 2 -> index(Path(args[1]))
    else -> {
      System.err.println(USAGE)
      exitProcess(2)
    }
  }
}

fun build(region: String, extract: Path, outputDirectory: Path) {
  val extraction = NetworkExtraction()
  val dataDate = PbfExtract(extract).readInto(extraction) ?: LocalDate.now(ZoneOffset.UTC)
  val result = extraction.result()
  outputDirectory.createDirectories()
  for ((network, graph) in result.graphs) {
    val file = outputDirectory.resolve(graphFileName(region, IndexedNetworkType.of(network)))
    file.writeBytes(GraphFile(network, region, dataDate.toString(), ATTRIBUTION, graph).toBytes())
    val lengths = graph.connections.map { it.lengthMeters }.sorted()
    val connectedKnotIds = graph.connections.flatMap { listOf(it.fromId, it.toId) }.toSet()
    println(
      "${file.name}: ${graph.knots.size} knots, ${graph.connections.size} connections, " +
        "${file.toFile().length()} bytes, " +
        "median connection ${lengths.getOrElse(lengths.size / 2) { 0 }} m, " +
        "${graph.knots.size - connectedKnotIds.size} knots without connections"
    )
  }
  for ((reason, count) in result.skippedRoutes.toSortedMap()) {
    println("Skipped $count routes: ${reason.name.lowercase().replace('_', ' ')}")
  }
}

fun index(directory: Path) {
  val files =
    directory
      .listDirectoryEntries("*.graph")
      .sortedBy { it.name }
      .map { file ->
        val bytes = file.readBytes()
        val graphFile = GraphFile.read(bytes)
        NetworkIndexEntry(
          region = graphFile.region,
          network = IndexedNetworkType.of(graphFile.network),
          file = file.name,
          sizeBytes = bytes.size.toLong(),
          sha256 = sha256Hex(bytes),
          dataDate = graphFile.dataDate,
        )
      }
  val index =
    NetworkIndex(
      formatVersion = GraphFile.FORMAT_VERSION,
      generatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
      files = files,
    )
  directory.resolve("index.json").writeText(index.toJson())
  println("index.json: ${files.size} files")
}

fun graphFileName(region: String, network: IndexedNetworkType) =
  "$region-${network.name.lowercase()}.graph"
