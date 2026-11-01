package com.knoopbyknoop.core

import kotlin.math.roundToLong

/** Thrown when bytes are not a graph or geometry file this version of the core can read. */
open class GraphFormatException(message: String) : Exception(message)

/** Thrown for a file written in a format version newer than this version of the core supports. */
class UnsupportedFormatVersionException(message: String) : GraphFormatException(message)

/**
 * The network graph of one network type as it is published and downloaded.
 *
 * The binary format is described in `openspec/specs/network-data` and its design.
 *
 * @property area the area the graph covers, e.g. `nl-be-nrw`
 * @property dataDate the date of the OpenStreetMap data as ISO date, e.g. `2026-10-25`
 * @property attribution the attribution the data license requires
 */
class GraphFile(
  val network: NetworkType,
  val area: String,
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
    writer.string(area)
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

    // Version 2: the ferries, by connection index, with an empty string for a missing detail.
    val connectionIndex = graph.connections.withIndex().associate { (index, it) -> it to index }
    val ferries = graph.ferries.entries.sortedBy { connectionIndex.getValue(it.key) }
    writer.varint(ferries.size.toLong())
    for ((connection, ferry) in ferries) {
      writer.varint(connectionIndex.getValue(connection).toLong())
      ferryDetails(ferry).forEach { writer.string(it.orEmpty()) }
    }

    // Then the other nodes of merged knots, by knot index, as offsets from the knot's position.
    // Readers from before stop after the ferries, so the version stays 2; later additions to
    // version 2 go after this section.
    val merged = knots.withIndex().filter { it.value.otherNodes.isNotEmpty() }
    writer.varint(merged.size.toLong())
    for ((index, knot) in merged) {
      writer.varint(index.toLong())
      writer.varint(knot.otherNodes.size.toLong())
      for (node in knot.otherNodes) {
        writer.varint(zigzag(toMicrodegrees(node.latitude) - toMicrodegrees(knot.latitude)))
        writer.varint(zigzag(toMicrodegrees(node.longitude) - toMicrodegrees(knot.longitude)))
      }
    }
    return writer.toByteArray()
  }

  companion object {

    /** The version written. Version 1 has no ferries, see `network-data` "Older graph files". */
    const val FORMAT_VERSION = 2

    private val READABLE_VERSIONS = 1..FORMAT_VERSION

    private val MAGIC = "KBKG".encodeToByteArray()

    private val NETWORK_CODES = mapOf(NetworkType.CYCLING to 0, NetworkType.HIKING to 1)

    /** How many bytes from the start of a graph file [formatVersion] needs. */
    const val HEADER_SIZE = 5

    /**
     * The format version of the graph file starting with [header], or `null` if it is not a graph
     * file, without reading the whole file.
     */
    fun formatVersion(header: ByteArray): Int? =
      if (header.size >= HEADER_SIZE && header.copyOf(MAGIC.size).contentEquals(MAGIC)) {
        header[MAGIC.size].toInt() and 0xFF
      } else {
        null
      }

    /** @throws GraphFormatException if [bytes] are not a graph file of a supported version */
    @Throws(GraphFormatException::class)
    fun read(bytes: ByteArray): GraphFile {
      val reader = ByteReader(bytes)
      if (!reader.bytes(MAGIC.size).contentEquals(MAGIC)) {
        throw GraphFormatException("Not a graph file")
      }
      val formatVersion = reader.byte()
      if (formatVersion !in READABLE_VERSIONS) {
        throw UnsupportedFormatVersionException(
          "Unsupported graph file format version $formatVersion"
        )
      }
      val networkCode = reader.byte()
      val network =
        NETWORK_CODES.entries.firstOrNull { it.value == networkCode }?.key
          ?: throw GraphFormatException("Unknown network type $networkCode")
      val area = reader.string()
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
      val ferries =
        if (formatVersion < 2) {
          mapOf()
        } else {
          List(reader.count()) {
              val connection =
                connections.getOrNull(reader.count())
                  ?: throw GraphFormatException("Bad connection index")
              val details = List(FERRY_DETAIL_COUNT) { reader.string().ifEmpty { null } }
              connection to
                Ferry(
                  name = details[0],
                  operator = details[1],
                  openingHours = details[2],
                  seasonal = details[3],
                  fee = details[4],
                  charge = details[5],
                  website = details[6],
                )
            }
            .toMap()
        }
      val otherNodes =
        if (formatVersion < 2 || !reader.hasMore()) {
          mapOf()
        } else {
          List(reader.count()) {
              val index = reader.count()
              val knot = knots.getOrNull(index) ?: throw GraphFormatException("Bad knot index")
              val latitude = toMicrodegrees(knot.latitude)
              val longitude = toMicrodegrees(knot.longitude)
              index to
                List(reader.count()) {
                  GeoPoint(
                    fromMicrodegrees(latitude + unzigzag(reader.varint())),
                    fromMicrodegrees(longitude + unzigzag(reader.varint())),
                  )
                }
            }
            .toMap()
        }
      return GraphFile(
        network,
        area,
        dataDate,
        attribution,
        NetworkGraph(
          knots.mapIndexed { index, knot ->
            otherNodes[index]?.let { knot.copy(otherNodes = it) } ?: knot
          },
          connections,
          ferries,
        ),
      )
    }

    private const val FERRY_DETAIL_COUNT = 7

    /** In the order they are written, [FERRY_DETAIL_COUNT] of them. */
    private fun ferryDetails(ferry: Ferry) =
      listOf(
        ferry.name,
        ferry.operator,
        ferry.openingHours,
        ferry.seasonal,
        ferry.fee,
        ferry.charge,
        ferry.website,
      )
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

  /** Whether bytes are left, for a section files written before it do not have. */
  fun hasMore(): Boolean = position < bytes.size
}

internal fun toMicrodegrees(degrees: Double): Long = (degrees * 1_000_000).roundToLong()

internal fun fromMicrodegrees(microdegrees: Long): Double = microdegrees / 1_000_000.0

internal fun zigzag(value: Long): Long = (value shl 1) xor (value shr 63)

internal fun unzigzag(value: Long): Long = (value ushr 1) xor -(value and 1)
