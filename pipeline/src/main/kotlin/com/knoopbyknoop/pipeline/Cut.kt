package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.PathFile
import com.knoopbyknoop.core.PathSquare
import com.knoopbyknoop.core.sha256Hex
import java.io.RandomAccessFile
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/** An area between [south] and [north] latitude and [west] and [east] longitude, in degrees. */
data class Box(val south: Double, val west: Double, val north: Double, val east: Double) {
  init {
    require(south < north && west < east) { "A box needs south < north and west < east" }
  }

  fun contains(latitude: Double, longitude: Double) =
    latitude in south..north && longitude in west..east

  /** The path squares this box touches. */
  val squares: List<PathSquare>
    get() {
      val southWest = PathSquare.of(south, west)
      val northEast = PathSquare.of(north, east)
      return (southWest.latitudeIndex..northEast.latitudeIndex).flatMap { latitude ->
        (southWest.longitudeIndex..northEast.longitudeIndex).map { PathSquare(latitude, it) }
      }
    }
}

/** The knots of [file] in [box], and the connections and ferries between them. */
fun cutGraph(file: GraphFile, box: Box): GraphFile {
  val graph = file.graph
  val knots = graph.knots.values.filter { box.contains(it.latitude, it.longitude) }
  val kept = knots.map { it.id }.toSet()
  val connections = graph.connections.filter { it.fromId in kept && it.toId in kept }
  val ferries = graph.ferries.filterKeys { it in connections }
  return GraphFile(
    file.network,
    file.area,
    file.dataDate,
    file.attribution,
    NetworkGraph(knots, connections, ferries),
  )
}

/**
 * The courses of [geometry] for the connections of [cut], a cut of its graph, with their signpost
 * courses, bridge stretches and courses back on the connections' new places.
 */
fun cutGeometry(geometry: NetworkGeometry, cut: NetworkGraph): NetworkGeometry {
  val newIndexOf =
    geometry.graph.connections
      .withIndex()
      .filter { it.value in cut.connections }
      .withIndex()
      .associate { (newIndex, old) -> old.index to newIndex }
  fun <T> moved(byIndex: Map<Int, T>) =
    byIndex.entries.mapNotNull { (index, value) -> newIndexOf[index]?.let { it to value } }.toMap()
  return NetworkGeometry(
    cut,
    newIndexOf.keys.sorted().map { geometry.courses[it] },
    moved(geometry.signpostCourses),
    moved(geometry.bridges),
    moved(geometry.coursesBack),
  )
}

/**
 * The squares of the path file read through [read] that [box] touches, their bytes unchanged, as a
 * path file of their own.
 */
fun cutPaths(read: (offset: Long, size: Int) -> ByteArray, box: Box): ByteArray {
  val info = PathFile.directoryInfo(read)
  val directory = PathFile.parseDirectory(read(info.offset, info.size))
  val dataOffset = info.offset + info.size
  val squares =
    box.squares
      .mapNotNull { square -> directory.entries[square]?.let { square to it } }
      .sortedWith(compareBy({ it.first.latitudeIndex }, { it.first.longitudeIndex }))
  val bytes = squares.map { (_, entry) -> read(dataOffset + entry.offset, entry.size) }
  val header =
    PathFile.header(
      squares.map { (square, entry) ->
        PathFile.SquareInfo(square, entry.size, entry.rawSize, entry.sha256)
      },
      directory.dataDate,
      directory.attribution,
    )
  return bytes.fold(header) { file, square -> file + square }
}

/**
 * Writes the cycling data of [box] from the published files in [published] into [output], as files
 * of the same formats, see the change `replay-rides-on-real-data`.
 */
fun cut(published: Path, box: Box, output: Path) {
  val graphBytes = published.resolve("cycling.graph").readBytes()
  val graphFile = GraphFile.read(graphBytes)
  val geometry =
    NetworkGeometry.read(
      published.resolve("cycling.geometry").readBytes(),
      graphFile.graph,
      sha256Hex(graphBytes),
    )
  val cutGraph = cutGraph(graphFile, box)
  val cutGraphBytes = cutGraph.toBytes()
  val cutGeometryBytes = cutGeometry(geometry, cutGraph.graph).toBytes(sha256Hex(cutGraphBytes))
  val cutPathBytes =
    RandomAccessFile(published.resolve("cycling.paths").toFile(), "r").use { file ->
      cutPaths(
        { offset, size ->
          file.seek(offset)
          ByteArray(size).also(file::readFully)
        },
        box,
      )
    }
  output.createDirectories()
  output.resolve("cycling.graph").writeBytes(cutGraphBytes)
  output.resolve("cycling.geometry").writeBytes(cutGeometryBytes)
  output.resolve("cycling.paths").writeBytes(cutPathBytes)
  println(
    "${cutGraph.graph.knots.size} knots, ${cutGraph.graph.connections.size} connections, " +
      "${cutGraph.graph.ferries.size} ferries; graph ${cutGraphBytes.size} B, " +
      "geometry ${cutGeometryBytes.size} B, paths ${cutPathBytes.size} B"
  )
}
