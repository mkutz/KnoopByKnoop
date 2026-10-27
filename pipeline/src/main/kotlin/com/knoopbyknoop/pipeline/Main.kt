package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.NetworkGeometry
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
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.system.exitProcess

/** How far a drawn course may deviate from the OpenStreetMap ways, see `network-data`. */
const val COURSE_TOLERANCE_METERS = 5.0

const val ATTRIBUTION =
  "© OpenStreetMap contributors, ODbL 1.0, https://www.openstreetmap.org/copyright"

private const val USAGE =
  """Usage:
  pipeline build <area> <output-dir> <extract.osm.pbf>...   extract one graph per network type from all extracts
  pipeline index <output-dir>                               write index.json for all graph files
  pipeline bundle [--allow-unavailable] <base-url> <output-dir>
                                                            fetch and verify the published graphs for the apps"""

/** Turns OpenStreetMap node network data into the graph files the apps download. */
fun main(args: Array<String>) {
  when (args.firstOrNull()) {
    "build" if args.size >= 4 -> build(args[1], Path(args[2]), args.drop(3).map(::Path))
    "index" if args.size == 2 -> index(Path(args[1]))
    "bundle" if args.size == 3 -> bundle(args[1], Path(args[2]))
    "bundle" if args.size == 4 && args[1] == "--allow-unavailable" ->
      bundle(args[2], Path(args[3]), allowUnavailable = true)
    else -> {
      System.err.println(USAGE)
      exitProcess(2)
    }
  }
}

/**
 * Builds one graph and one geometry file per network type from [extracts] together, so connections
 * across the borders between extracts are complete, see `network-data`.
 */
fun build(area: String, outputDirectory: Path, extracts: List<Path>) {
  val extraction = NetworkExtraction()
  val dataDate =
    PbfExtract.readAll(extracts.map(::PbfExtract), extraction) ?: LocalDate.now(ZoneOffset.UTC)
  val result = extraction.result()
  outputDirectory.createDirectories()
  for ((network, graph) in result.graphs) {
    val file = outputDirectory.resolve(graphFileName(IndexedNetworkType.of(network)))
    val graphBytes = GraphFile(network, area, dataDate.toString(), ATTRIBUTION, graph).toBytes()
    file.writeBytes(graphBytes)
    val networkCourses = result.courses.getValue(network)
    val courses =
      graph.connections.map { simplify(networkCourses.getValue(it), COURSE_TOLERANCE_METERS) }
    val geometryFile = outputDirectory.resolve(geometryFileName(IndexedNetworkType.of(network)))
    geometryFile.writeBytes(NetworkGeometry(graph, courses).toBytes(sha256Hex(graphBytes)))
    val lengths = graph.connections.map { it.lengthMeters }.sorted()
    val connectedKnotIds = graph.connections.flatMap { listOf(it.fromId, it.toId) }.toSet()
    println(
      "${file.name}: ${graph.knots.size} knots, ${graph.connections.size} connections, " +
        "${file.toFile().length()} bytes, " +
        "median connection ${lengths.getOrElse(lengths.size / 2) { 0 }} m, " +
        "${graph.knots.size - connectedKnotIds.size} knots without connections"
    )
    println(
      "${geometryFile.name}: ${courses.sumOf { it.size }} points " +
        "(${networkCourses.values.sumOf { it.size }} before simplification), " +
        "${geometryFile.toFile().length()} bytes"
    )
    // How much OpenStreetMap knows about the ferries, see `show-ferry-schedules`.
    val ferries = graph.ferries.values
    println(
      "${file.name}: ${ferries.size} ferries, " +
        "${ferries.count { it.openingHours != null }} with operating times, " +
        "${ferries.count { it.openingHours == null && it.seasonal != null }} only seasonal, " +
        "${ferries.count { it.charge != null || it.fee != null }} with a fee or charge, " +
        "${ferries.count { it.name == null && it.operator == null }} without a name"
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
      .flatMap { file ->
        val bytes = file.readBytes()
        val graphFile = GraphFile.read(bytes)
        val network = IndexedNetworkType.of(graphFile.network)
        val graphEntry =
          NetworkIndexEntry(
            area = graphFile.area,
            network = network,
            file = file.name,
            sizeBytes = bytes.size.toLong(),
            sha256 = sha256Hex(bytes),
            dataDate = graphFile.dataDate,
            kind = IndexedFileKind.GRAPH,
          )
        val geometryFile = directory.resolve(geometryFileName(network))
        val geometryBytes = geometryFile.takeIf { it.exists() }?.readBytes()
        if (geometryBytes == null || !NetworkGeometry.belongsTo(geometryBytes, graphEntry.sha256)) {
          error("${geometryFile.name} is missing or does not belong to ${file.name}")
        }
        val geometryEntry =
          graphEntry.copy(
            file = geometryFile.name,
            sizeBytes = geometryBytes.size.toLong(),
            sha256 = sha256Hex(geometryBytes),
            kind = IndexedFileKind.GEOMETRY,
          )
        listOf(graphEntry, geometryEntry)
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

fun graphFileName(network: IndexedNetworkType) = "${network.name.lowercase()}.graph"

fun geometryFileName(network: IndexedNetworkType) = "${network.name.lowercase()}.geometry"
