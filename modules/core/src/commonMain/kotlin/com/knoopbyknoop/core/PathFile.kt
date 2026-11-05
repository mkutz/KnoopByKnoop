package com.knoopbyknoop.core

import kotlin.math.floor
import kotlin.math.roundToLong

/** What a way is, for drawing it and telling the rider about it, see `network-data`. */
enum class PathKind {
  /** Primary, secondary, trunk and motorway. */
  MAJOR,

  /** Tertiary. */
  MAIN,

  /** Residential, unclassified, living street, service and pedestrian. */
  STREET,

  /** Cycleway, bicycle road, or a path, footway or track designated for bicycles. */
  CYCLE,

  /** Any other path or track. */
  PATH,
}

/**
 * A stretch of a way between two junctions.
 *
 * @property fromId the OpenStreetMap node at its start, shared with the ways meeting there
 * @property forward whether it can be ridden from [fromId] to [toId]
 * @property backward whether it can be ridden the other way; neither means it is only for drawing
 * @property costFactor what a meter of it costs the router, see the change's design
 * @property points its course, from [fromId] to [toId]
 */
class PathSegment(
  val fromId: Long,
  val toId: Long,
  val kind: PathKind,
  val forward: Boolean,
  val backward: Boolean,
  val costFactor: Double,
  val name: String?,
  val points: List<GeoPoint>,
) {

  /** Its length in meters. */
  val lengthMeters: Double by lazy {
    points.zipWithNext().sumOf { (from, to) ->
      distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
    }
  }
}

/**
 * [bytes] packed with zlib, for the path file's squares. Only the pipeline writes real files, but
 * the core's tests write them on every platform.
 */
expect fun deflate(bytes: ByteArray): ByteArray

/**
 * What [deflate] packed, [size] bytes when unpacked.
 *
 * @throws GraphFormatException if it can't be unpacked to that size
 */
expect fun inflate(bytes: ByteArray, size: Int): ByteArray

/** A square of [PathFile]: its position in steps of 1/[PathFile.SQUARES_PER_DEGREE]°. */
data class PathSquare(val latitudeIndex: Int, val longitudeIndex: Int) {

  companion object {

    fun of(latitude: Double, longitude: Double) =
      PathSquare(
        floor(latitude * PathFile.SQUARES_PER_DEGREE).toInt(),
        floor(longitude * PathFile.SQUARES_PER_DEGREE).toInt(),
      )
  }
}

/**
 * The published ways around the networks, in squares that can be read on their own, see
 * `network-data` "Published path data". A segment is in the squares of both of its ends, so a
 * junction's square holds all segments meeting there.
 *
 * @property read reads a range of the file
 */
class PathFile
private constructor(
  val dataDate: String,
  val attribution: String,
  private val directory: Map<PathSquare, Entry>,
  private val dataOffset: Long,
  private val read: (offset: Long, size: Int) -> ByteArray,
) {

  private class Entry(val offset: Long, val size: Int, val rawSize: Int, val sha256: String)

  /** A square as it is in the file. */
  class EncodedSquare(val square: PathSquare, val bytes: ByteArray, val rawSize: Int)

  /** What the directory holds about a square. */
  class SquareInfo(val square: PathSquare, val size: Int, val rawSize: Int, val sha256: String)

  /** The segments of [square], none if it isn't in the file. */
  fun segments(square: PathSquare): List<PathSegment> {
    val entry = directory[square] ?: return listOf()
    val bytes = read(dataOffset + entry.offset, entry.size)
    if (sha256Hex(bytes) != entry.sha256)
      throw GraphFormatException("Path square $square is broken")
    return readSquare(inflate(bytes, entry.rawSize))
  }

  companion object {

    /** A square is 1/16° wide and high, see the change's design. */
    const val SQUARES_PER_DEGREE = 16

    const val FORMAT_VERSION = 2

    private val MAGIC = "KBKP".encodeToByteArray()
    private const val HEADER_SIZE = 9
    private const val COORDINATE_SCALE = 100_000.0
    private const val COST_SCALE = 100.0
    private const val FORWARD = 1
    private const val BACKWARD = 2

    /** [segments] as [square]'s part of the file. */
    fun encode(square: PathSquare, segments: List<PathSegment>): EncodedSquare {
      val raw = writeSquare(segments)
      return EncodedSquare(square, deflate(raw), raw.size)
    }

    /**
     * What comes before the squares' bytes in the file, for squares described by their sizes and
     * checksums in the order of their bytes.
     */
    fun header(squares: List<SquareInfo>, dataDate: String, attribution: String): ByteArray {
      val directory = ByteWriter()
      directory.string(dataDate)
      directory.string(attribution)
      directory.varint(squares.size.toLong())
      var offset = 0L
      squares.forEach {
        directory.varint(zigzag(it.square.latitudeIndex.toLong()))
        directory.varint(zigzag(it.square.longitudeIndex.toLong()))
        directory.varint(offset)
        directory.varint(it.size.toLong())
        directory.varint(it.rawSize.toLong())
        directory.string(it.sha256)
        offset += it.size
      }
      val directoryBytes = directory.toByteArray()
      val result = ByteArray(HEADER_SIZE + directoryBytes.size)
      MAGIC.copyInto(result)
      result[MAGIC.size] = FORMAT_VERSION.toByte()
      for (shift in 0..3) {
        result[MAGIC.size + 1 + shift] = (directoryBytes.size ushr (24 - 8 * shift)).toByte()
      }
      directoryBytes.copyInto(result, HEADER_SIZE)
      return result
    }

    /** The whole file's bytes for [squares], for files small enough to hold in memory. */
    fun toBytes(
      squares: Map<PathSquare, List<PathSegment>>,
      dataDate: String,
      attribution: String,
    ): ByteArray {
      val encoded =
        squares.entries
          .sortedWith(compareBy({ it.key.latitudeIndex }, { it.key.longitudeIndex }))
          .map { encode(it.key, it.value) }
      val header =
        header(
          encoded.map { SquareInfo(it.square, it.bytes.size, it.rawSize, sha256Hex(it.bytes)) },
          dataDate,
          attribution,
        )
      val result = header.copyOf(header.size + encoded.sumOf { it.bytes.size })
      var position = header.size
      encoded.forEach {
        it.bytes.copyInto(result, position)
        position += it.bytes.size
      }
      return result
    }

    /**
     * Opens a path file through [read], which reads a range of it.
     *
     * @throws UnsupportedFormatVersionException for a format version this core doesn't know, see
     *   `network-data` "Versioned path format"
     */
    fun open(read: (offset: Long, size: Int) -> ByteArray): PathFile {
      val header = read(0, HEADER_SIZE)
      if (header.size < HEADER_SIZE || !header.copyOf(MAGIC.size).contentEquals(MAGIC)) {
        throw GraphFormatException("Not a path file")
      }
      val version = header[MAGIC.size].toInt() and 0xFF
      if (version != FORMAT_VERSION) {
        throw UnsupportedFormatVersionException("Path file format $version is not supported")
      }
      var directorySize = 0
      for (shift in 0..3) {
        directorySize = (directorySize shl 8) or (header[MAGIC.size + 1 + shift].toInt() and 0xFF)
      }
      val reader = ByteReader(read(HEADER_SIZE.toLong(), directorySize))
      val dataDate = reader.string()
      val attribution = reader.string()
      val directory = HashMap<PathSquare, Entry>()
      repeat(reader.count()) {
        val square =
          PathSquare(unzigzag(reader.varint()).toInt(), unzigzag(reader.varint()).toInt())
        directory[square] = Entry(reader.varint(), reader.count(), reader.count(), reader.string())
      }
      return PathFile(dataDate, attribution, directory, HEADER_SIZE.toLong() + directorySize, read)
    }

    private fun scaled(degrees: Double) = (degrees * COORDINATE_SCALE).roundToLong()

    private fun writeSquare(segments: List<PathSegment>): ByteArray {
      val writer = ByteWriter()
      val names = segments.mapNotNull { it.name }.distinct()
      val nameIndex = names.withIndex().associate { (index, name) -> name to index + 1 }
      writer.varint(names.size.toLong())
      names.forEach(writer::string)

      val nodes = HashMap<Long, GeoPoint>()
      segments.forEach {
        nodes[it.fromId] = it.points.first()
        nodes[it.toId] = it.points.last()
      }
      val nodeIds = nodes.keys.sorted()
      val nodeIndex = nodeIds.withIndex().associate { (index, id) -> id to index }
      writer.varint(nodeIds.size.toLong())
      var previousId = 0L
      var previousLatitude = 0L
      var previousLongitude = 0L
      nodeIds.forEach { id ->
        val point = nodes.getValue(id)
        writer.varint(id - previousId)
        writer.varint(zigzag(scaled(point.latitude) - previousLatitude))
        writer.varint(zigzag(scaled(point.longitude) - previousLongitude))
        previousId = id
        previousLatitude = scaled(point.latitude)
        previousLongitude = scaled(point.longitude)
      }

      writer.varint(segments.size.toLong())
      segments.forEach { segment ->
        writer.varint(nodeIndex.getValue(segment.fromId).toLong())
        writer.varint(nodeIndex.getValue(segment.toId).toLong())
        writer.byte(segment.kind.ordinal)
        writer.byte(
          (if (segment.forward) FORWARD else 0) or (if (segment.backward) BACKWARD else 0)
        )
        writer.varint((segment.costFactor * COST_SCALE).roundToLong())
        writer.varint((segment.name?.let(nameIndex::getValue) ?: 0).toLong())
        val inner = segment.points.subList(1, segment.points.size - 1)
        writer.varint(inner.size.toLong())
        var latitude = scaled(segment.points.first().latitude)
        var longitude = scaled(segment.points.first().longitude)
        inner.forEach { point ->
          writer.varint(zigzag(scaled(point.latitude) - latitude))
          writer.varint(zigzag(scaled(point.longitude) - longitude))
          latitude = scaled(point.latitude)
          longitude = scaled(point.longitude)
        }
      }
      return writer.toByteArray()
    }

    private fun readSquare(bytes: ByteArray): List<PathSegment> {
      val reader = ByteReader(bytes)
      val names = List(reader.count()) { reader.string() }
      val nodeCount = reader.count()
      val nodeIds = LongArray(nodeCount)
      val latitudes = LongArray(nodeCount)
      val longitudes = LongArray(nodeCount)
      var id = 0L
      var latitude = 0L
      var longitude = 0L
      for (index in 0 until nodeCount) {
        id += reader.varint()
        latitude += unzigzag(reader.varint())
        longitude += unzigzag(reader.varint())
        nodeIds[index] = id
        latitudes[index] = latitude
        longitudes[index] = longitude
      }
      fun nodePoint(index: Int) =
        GeoPoint(latitudes[index] / COORDINATE_SCALE, longitudes[index] / COORDINATE_SCALE)
      val kinds = PathKind.entries
      return List(reader.count()) {
        val from = reader.count()
        val to = reader.count()
        val kind = kinds[reader.byte()]
        val flags = reader.byte()
        val cost = reader.varint() / COST_SCALE
        val nameIndex = reader.count()
        val innerCount = reader.count()
        val points = ArrayList<GeoPoint>(innerCount + 2)
        points.add(nodePoint(from))
        var pointLatitude = latitudes[from]
        var pointLongitude = longitudes[from]
        repeat(innerCount) {
          pointLatitude += unzigzag(reader.varint())
          pointLongitude += unzigzag(reader.varint())
          points.add(GeoPoint(pointLatitude / COORDINATE_SCALE, pointLongitude / COORDINATE_SCALE))
        }
        points.add(nodePoint(to))
        PathSegment(
          nodeIds[from],
          nodeIds[to],
          kind,
          flags and FORWARD != 0,
          flags and BACKWARD != 0,
          cost,
          names.getOrNull(nameIndex - 1),
          points,
        )
      }
    }
  }
}
