package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.CourseBack
import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.OpeningHours
import com.knoopbyknoop.core.PathFile
import com.knoopbyknoop.core.PathIndexEntry
import com.knoopbyknoop.core.PlaceFile
import com.knoopbyknoop.core.SquareFile
import com.knoopbyknoop.core.SquareInfo
import com.knoopbyknoop.core.sha256Hex
import java.io.RandomAccessFile
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.outputStream
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
  pipeline paths <output-file> <extract.osm.pbf>...         extract the ways around the networks from all extracts
  pipeline places <output-file> <extract.osm.pbf>...        extract the details of the highlighted places
  pipeline index <output-dir>                               write index.json for all graph files
  pipeline verify [--allow-drop] <dir> [<published-base-url>]
                                                            check the data in <dir> before publishing it
  pipeline bundle [--allow-unavailable] <base-url> <output-dir>
                                                            fetch and verify the published graphs for the apps
  pipeline dark-style <liberty-url-or-file> <output.json>   generate the dark map from OpenFreeMap's liberty
  pipeline cut <published-dir> <south> <west> <north> <east> <output-dir>
                                                            write the cycling data of an area, for tests"""

/** Turns OpenStreetMap node network data into the graph files the apps download. */
fun main(args: Array<String>) {
  when (args.firstOrNull()) {
    "build" if args.size >= 4 -> build(args[1], Path(args[2]), args.drop(3).map(::Path))
    "paths" if args.size >= 3 -> paths(Path(args[1]), args.drop(2).map(::Path))
    "places" if args.size >= 3 -> places(Path(args[1]), args.drop(2).map(::Path))
    "index" if args.size == 2 -> index(Path(args[1]))
    "bundle" if args.size == 3 -> bundle(args[1], Path(args[2]))
    "dark-style" if args.size == 3 -> DarkStyle.generate(args[1], Path(args[2]))
    "cut" if args.size == 7 ->
      cut(
        Path(args[1]),
        Box(args[2].toDouble(), args[3].toDouble(), args[4].toDouble(), args[5].toDouble()),
        Path(args[6]),
      )
    "verify" if args.size in 2..4 -> {
      val allowDrop = args[1] == "--allow-drop"
      val rest = args.drop(if (allowDrop) 2 else 1)
      if (rest.size !in 1..2) usage()
      if (!report(verify(Path(rest[0]), rest.getOrNull(1), allowDrop))) exitProcess(1)
    }
    "bundle" if args.size == 4 && args[1] == "--allow-unavailable" ->
      bundle(args[2], Path(args[3]), allowUnavailable = true)
    else -> usage()
  }
}

private fun usage(): Nothing {
  System.err.println(USAGE)
  exitProcess(2)
}

/**
 * Prints every check of [verification] and whether the data may be published, see `network-data`
 * "Checked before publication".
 *
 * @return whether it may be published
 */
fun report(verification: Verification): Boolean {
  verification.log.forEach(::println)
  if (verification.passed) println("verified: the data may be published")
  else System.err.println("not verified: ${verification.failures.size} checks failed")
  return verification.passed
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
    // See `network-data` "Place of each knot"; a fixed sample to judge against the map.
    val placed = graph.knots.values.filter { it.place != null }
    println(
      "${network.name.lowercase()}: ${placed.size} of ${graph.knots.size} knots in a place, " +
        "${placed.mapTo(HashSet()) { it.place }.size} places"
    )
    graph.knots.values
      .sortedBy { it.id }
      .shuffled(kotlin.random.Random(11))
      .take(20)
      .forEach {
        println(
          "  ${it.ref} ${"%.5f".format(it.latitude)},${"%.5f".format(it.longitude)} ${it.place?.name ?: "-"}"
        )
      }
    val file = outputDirectory.resolve(graphFileName(IndexedNetworkType.of(network)))
    val graphBytes = GraphFile(network, area, dataDate.toString(), ATTRIBUTION, graph).toBytes()
    file.writeBytes(graphBytes)
    val networkCourses = result.courses.getValue(network)
    val networkBridges = result.bridges[network].orEmpty()
    // Simplified with the bridges' ends kept, see `network-data` "Stretch kept through
    // simplification".
    val simplifiedCourses =
      graph.connections.map {
        simplifyKeeping(
          networkCourses.getValue(it),
          COURSE_TOLERANCE_METERS,
          networkBridges[it].orEmpty(),
        )
      }
    val courses = simplifiedCourses.map { it.first }
    val bridges =
      simplifiedCourses
        .withIndex()
        .filter { (_, simplified) -> simplified.second.isNotEmpty() }
        .associate { (index, simplified) -> index to simplified.second }
    // Only where the simplified course back is not the course there reversed, see `network-data`
    // "Course in each riding direction".
    val networkCoursesBack = result.coursesBack[network].orEmpty()
    val coursesBack =
      graph.connections
        .withIndex()
        .mapNotNull { (index, connection) ->
          networkCoursesBack[connection]?.let { (course, stretches) ->
            val (simplified, simplifiedStretches) =
              simplifyKeeping(course, COURSE_TOLERANCE_METERS, stretches)
            if (simplified == courses[index].asReversed()) null
            else index to CourseBack(simplified, simplifiedStretches)
          }
        }
        .toMap()
    val geometryFile = outputDirectory.resolve(geometryFileName(IndexedNetworkType.of(network)))
    val networkSignpostCourses = result.signpostCourses[network].orEmpty()
    val signpostCourses =
      graph.connections
        .withIndex()
        .mapNotNull { (index, connection) ->
          networkSignpostCourses[connection]?.let { pairs ->
            index to
              pairs.map {
                val (course, stretches) =
                  simplifyKeeping(it.course, COURSE_TOLERANCE_METERS, it.bridges)
                // Only where the simplified course back is not the course reversed.
                val back =
                  it.back?.let { back ->
                    val (backCourse, backStretches) =
                      simplifyKeeping(back.course, COURSE_TOLERANCE_METERS, back.bridges)
                    CourseBack(backCourse, backStretches).takeUnless {
                      backCourse == course.asReversed()
                    }
                  }
                it.copy(course = course, bridges = stretches, back = back)
              }
          }
        }
        .toMap()
    geometryFile.writeBytes(
      NetworkGeometry(graph, courses, signpostCourses, bridges, coursesBack)
        .toBytes(sha256Hex(graphBytes))
    )
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
    println(
      "${geometryFile.name}: ${signpostCourses.size} of ${graph.connections.size} connections " +
        "with courses to several signposts, " +
        "${signpostCourses.values.sumOf { pairs -> pairs.sumOf { it.course.size } }} points, " +
        "${bridges.values.sumOf { it.size }} bridge stretches on ${bridges.size} connections, " +
        "${coursesBack.size} courses back with ${coursesBack.values.sumOf { it.course.size }} points"
    )
    // How much OpenStreetMap knows about the ferries, see `show-ferry-schedules`.
    val ferries = graph.ferries.values
    println(
      "${file.name}: ${ferries.size} ferries, " +
        "${ferries.count { it.openingHours != null }} with operating times, " +
        "${ferries.count { hours -> hours.openingHours?.let(OpeningHours::parse) != null }} " +
        "of them readable, " +
        "${ferries.count { it.openingHours == null && it.seasonal != null }} only seasonal, " +
        "${ferries.count { it.charge != null || it.fee != null }} with a fee or charge, " +
        "${ferries.count { it.name == null && it.operator == null }} without a name"
    )
  }
  for ((reason, count) in result.skippedRoutes.toSortedMap()) {
    println("Skipped $count routes: ${reason.name.lowercase().replace('_', ' ')}")
  }
}

/** Writes the ways of [extracts] together, see `network-data` "Published path data". */
fun paths(output: Path, extracts: List<Path>) {
  val extraction = PathExtraction()
  val readers = extracts.map(::PbfExtract)
  val dataDates = readers.mapNotNull { it.forEachWay(extraction::acceptWay) }
  readers.forEach { it.forEachNode(extraction::acceptNode) }
  val dataDate = dataDates.minOrNull() ?: LocalDate.now(ZoneOffset.UTC)
  // The squares go to a file first, as the directory before them needs their sizes.
  val squareBytes = createTempFile(output.absolute().parent, output.name, ".squares")
  try {
    val infos = ArrayList<SquareInfo>()
    var segmentCount = 0L
    squareBytes.outputStream().buffered().use { stream ->
      extraction.forEachSquare { square, segments ->
        val encoded = PathFile.encode(square, segments)
        stream.write(encoded.bytes)
        infos.add(
          SquareInfo(
            square,
            encoded.bytes.size,
            encoded.rawSize,
            sha256Hex(encoded.bytes),
          )
        )
        segmentCount += segments.size
      }
    }
    output.outputStream().buffered().use { stream ->
      stream.write(PathFile.header(infos, dataDate.toString(), ATTRIBUTION))
      squareBytes.inputStream().use { it.copyTo(stream) }
    }
    println(
      "${output.name}: ${infos.size} squares, $segmentCount segments, " +
        "${infos.sumOf { it.rawSize.toLong() }} bytes unpacked, " +
        "${output.toFile().length()} bytes, largest ${infos.maxOfOrNull { it.size }}"
    )
  } finally {
    squareBytes.deleteIfExists()
  }
}

/**
 * Writes the details of the highlighted places of [extracts] together, see `network-data`
 * "Published place details".
 */
fun places(output: Path, extracts: List<Path>) {
  val extraction = PlaceExtraction()
  val readers = extracts.map(::PbfExtract)
  val dataDates = readers.mapNotNull { it.forEachWay(extraction::acceptWay) }
  readers.forEach { it.forEachNode(extraction::acceptNode) }
  val dataDate = dataDates.minOrNull() ?: LocalDate.now(ZoneOffset.UTC)
  val squares = extraction.squares()
  output.writeBytes(PlaceFile.toBytes(squares, dataDate.toString(), ATTRIBUTION))
  for ((kind, count) in extraction.counts) println("${kind.key}: $count places")
  println(
    "opening hours: ${extraction.openingHoursCount}, " +
      "readable ${extraction.readableOpeningHoursCount}"
  )
  println(
    "${output.name}: ${squares.size} squares, ${squares.values.sumOf { it.size }} entries, " +
      "${output.toFile().length()} bytes"
  )
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
      paths = squaresEntry(directory.resolve(PATHS_FILE), PathFile.SQUARES),
      places = squaresEntry(directory.resolve(PLACES_FILE), PlaceFile.SQUARES),
    )
  directory.resolve("index.json").writeText(index.toJson())
  println(
    "index.json: ${files.size} files" +
      (if (index.paths != null) ", path data" else "") +
      (if (index.places != null) ", place details" else "")
  )
}

/** The index's entry for the square file at [file], if there is one, without reading all of it. */
private fun squaresEntry(file: Path, format: SquareFile): PathIndexEntry? {
  if (!file.exists()) return null
  return RandomAccessFile(file.toFile(), "r").use { access ->
    val info = format.directoryInfo { offset, size ->
      access.seek(offset)
      ByteArray(size).also(access::readFully)
    }
    PathIndexEntry(
      file.name,
      access.length(),
      info.dataDate,
      info.formatVersion,
      info.offset,
      info.size,
      info.sha256,
    )
  }
}

const val PATHS_FILE = "cycling.paths"

const val PLACES_FILE = "places.details"

fun graphFileName(network: IndexedNetworkType) = "${network.name.lowercase()}.graph"

fun geometryFileName(network: IndexedNetworkType) = "${network.name.lowercase()}.geometry"
