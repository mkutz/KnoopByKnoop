package com.knoopbyknoop.core

import kotlin.math.roundToLong

/** Thrown when bytes are not a graph or geometry file this version of the core can read. */
open class GraphFormatException(message: String) : Exception(message)

/** Thrown for a file written in a format version newer than this version of the core supports. */
class UnsupportedFormatVersionException(message: String) : GraphFormatException(message)

/**
 * The network graph of one region and network type as it is published and downloaded.
 *
 * The binary format is described in `openspec/specs/network-data` and its design.
 *
 * @property region the region ID, e.g. `netherlands`
 * @property dataDate the date of the OpenStreetMap data as ISO date, e.g. `2026-10-25`
 * @property attribution the attribution the data license requires
 */
class GraphFile(
  val network: NetworkType,
  val region: String,
  val dataDate: String,
  val attribution: String,
  val graph: NetworkGraph,
) {
  init {
    require(graph.network == null || graph.network == network) {
      "The graph's network ${graph.network} does not match $network"
    }
  }

  fun toBytes(): ByteArray {
    val writer = ByteWriter()
    writer.bytes(MAGIC)
    writer.byte(FORMAT_VERSION)
    writer.byte(NETWORK_CODES.getValue(network))
    writer.string(region)
    writer.string(dataDate)
    writer.string(attribution)

    val knots = graph.knots.values.sortedBy { it.id }
    val indexById = knots.withIndex().associate { (index, knot) -> knot.id to index }
    writer.varint(knots.size.toLong())
    var previousId = 0L
    var previousLatitude = 0L
    var previousLongitude = 0L
    for (knot in knots) {
      val latitude = toMicrodegrees(knot.latitude)
      val longitude = toMicrodegrees(knot.longitude)
      writer.varint(knot.id - previousId)
      writer.string(knot.ref)
      writer.varint(zigzag(latitude - previousLatitude))
      writer.varint(zigzag(longitude - previousLongitude))
      previousId = knot.id
      previousLatitude = latitude
      previousLongitude = longitude
    }

    writer.varint(graph.connections.size.toLong())
    for (connection in graph.connections) {
      writer.varint(indexById.getValue(connection.fromId).toLong())
      writer.varint(indexById.getValue(connection.toId).toLong())
      writer.varint(connection.lengthMeters.toLong())
    }
    return writer.toByteArray()
  }

  companion object {

    const val FORMAT_VERSION = 1

    private val MAGIC = "KBKG".encodeToByteArray()

    private val NETWORK_CODES = mapOf(NetworkType.CYCLING to 0, NetworkType.HIKING to 1)

    /** @throws GraphFormatException if [bytes] are not a graph file of a supported version */
    @Throws(GraphFormatException::class)
    fun read(bytes: ByteArray): GraphFile {
      val reader = ByteReader(bytes)
      if (!reader.bytes(MAGIC.size).contentEquals(MAGIC)) {
        throw GraphFormatException("Not a graph file")
      }
      val formatVersion = reader.byte()
      if (formatVersion != FORMAT_VERSION) {
        throw UnsupportedFormatVersionException(
          "Unsupported graph file format version $formatVersion"
        )
      }
      val networkCode = reader.byte()
      val network =
        NETWORK_CODES.entries.firstOrNull { it.value == networkCode }?.key
          ?: throw GraphFormatException("Unknown network type $networkCode")
      val region = reader.string()
      val dataDate = reader.string()
      val attribution = reader.string()

      val knotCount = reader.count()
      var id = 0L
      var latitude = 0L
      var longitude = 0L
      val knots =
        List(knotCount) {
          id += reader.varint()
          val ref = reader.string()
          latitude += unzigzag(reader.varint())
          longitude += unzigzag(reader.varint())
          Knot(id, ref, network, fromMicrodegrees(latitude), fromMicrodegrees(longitude))
        }

      val connectionCount = reader.count()
      val connections =
        List(connectionCount) {
          val from = knots.getOrNull(reader.count()) ?: throw GraphFormatException("Bad knot index")
          val to = knots.getOrNull(reader.count()) ?: throw GraphFormatException("Bad knot index")
          Connection(from.id, to.id, reader.count())
        }
      return GraphFile(network, region, dataDate, attribution, NetworkGraph(knots, connections))
    }
  }
}

internal class ByteWriter {

  private val buffer = ArrayList<Byte>()

  fun byte(value: Int) {
    buffer.add(value.toByte())
  }

  fun bytes(values: ByteArray) {
    values.forEach { buffer.add(it) }
  }

  /** Writes an unsigned LEB128 varint. */
  fun varint(value: Long) {
    var remaining = value
    while (remaining and 0x7FL.inv() != 0L) {
      byte(((remaining and 0x7F) or 0x80).toInt())
      remaining = remaining ushr 7
    }
    byte(remaining.toInt())
  }

  fun string(value: String) {
    val encoded = value.encodeToByteArray()
    varint(encoded.size.toLong())
    bytes(encoded)
  }

  fun toByteArray(): ByteArray = buffer.toByteArray()
}

internal class ByteReader(private val bytes: ByteArray) {

  private var position = 0

  fun byte(): Int {
    if (position >= bytes.size) throw GraphFormatException("Unexpected end of graph file")
    return bytes[position++].toInt() and 0xFF
  }

  fun bytes(count: Int): ByteArray {
    if (position + count > bytes.size) throw GraphFormatException("Unexpected end of graph file")
    return bytes.copyOfRange(position, position + count).also { position += count }
  }

  fun varint(): Long {
    var result = 0L
    var shift = 0
    while (true) {
      val byte = byte()
      result = result or ((byte and 0x7F).toLong() shl shift)
      if (byte and 0x80 == 0) return result
      shift += 7
      if (shift > 63) throw GraphFormatException("Malformed number in graph file")
    }
  }

  /** Reads a varint that must fit into a non-negative [Int], like a count or an index. */
  fun count(): Int {
    val value = varint()
    if (value !in 0..Int.MAX_VALUE) throw GraphFormatException("Malformed count in graph file")
    return value.toInt()
  }

  fun string(): String = bytes(count()).decodeToString()
}

internal fun toMicrodegrees(degrees: Double): Long = (degrees * 1_000_000).roundToLong()

internal fun fromMicrodegrees(microdegrees: Long): Double = microdegrees / 1_000_000.0

internal fun zigzag(value: Long): Long = (value shl 1) xor (value shr 63)

internal fun unzigzag(value: Long): Long = (value ushr 1) xor -(value and 1)
