package com.knoopbyknoop.core

/** Where a square is in a [SquareFile], counted from the end of the directory, and its checksum. */
class SquareEntry(val offset: Long, val size: Int, val rawSize: Int, val sha256: String)

/** What a square file's directory says, for downloading single squares. */
class SquareDirectory(
  val dataDate: String,
  val attribution: String,
  val entries: Map<PathSquare, SquareEntry>,
)

/** What the index says about a square file's directory, see `network-data`. */
class SquareDirectoryInfo(
  val formatVersion: Int,
  val dataDate: String,
  val offset: Long,
  val size: Int,
  val sha256: String,
)

/** A square as it is in a square file. */
class EncodedSquare(val square: PathSquare, val bytes: ByteArray, val rawSize: Int)

/** What the directory holds about a square. */
class SquareInfo(val square: PathSquare, val size: Int, val rawSize: Int, val sha256: String)

/**
 * A published file of data in squares that can be read on their own, such as the path data and the
 * place details: a header with [magic] and [formatVersion], a directory of the squares with their
 * checksums, and the squares packed with zlib. What a square holds is up to the file.
 *
 * @property name what the file is called in errors, e.g. `path file`
 */
class SquareFile(private val magic: String, val formatVersion: Int, private val name: String) {

  private val magicBytes = magic.encodeToByteArray()

  /** A square of [raw] bytes as it goes into the file. */
  fun encode(square: PathSquare, raw: ByteArray) = EncodedSquare(square, deflate(raw), raw.size)

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
    magicBytes.copyInto(result)
    result[magicBytes.size] = formatVersion.toByte()
    for (shift in 0..3) {
      result[magicBytes.size + 1 + shift] = (directoryBytes.size ushr (24 - 8 * shift)).toByte()
    }
    directoryBytes.copyInto(result, HEADER_SIZE)
    return result
  }

  /** The whole file's bytes for squares of [raw] bytes, for files small enough for memory. */
  fun toBytes(raw: Map<PathSquare, ByteArray>, dataDate: String, attribution: String): ByteArray {
    val encoded =
      raw.entries.sortedWith(compareBy({ it.key.latitudeIndex }, { it.key.longitudeIndex })).map {
        encode(it.key, it.value)
      }
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
   * Opens a file through [read], which reads a range of it.
   *
   * @throws UnsupportedFormatVersionException for a format version this core doesn't know
   */
  fun open(read: (offset: Long, size: Int) -> ByteArray): Opened {
    val directorySize = directorySize(read)
    return Opened(
      parseDirectory(read(HEADER_SIZE.toLong(), directorySize)),
      HEADER_SIZE.toLong() + directorySize,
      read,
    )
  }

  /** The directory in [bytes], the range of the file the index names. */
  fun parseDirectory(bytes: ByteArray): SquareDirectory {
    val reader = ByteReader(bytes)
    val dataDate = reader.string()
    val attribution = reader.string()
    val directory = HashMap<PathSquare, SquareEntry>()
    repeat(reader.count()) {
      val square = PathSquare(unzigzag(reader.varint()).toInt(), unzigzag(reader.varint()).toInt())
      directory[square] =
        SquareEntry(reader.varint(), reader.count(), reader.count(), reader.string())
    }
    return SquareDirectory(dataDate, attribution, directory)
  }

  /** What the index needs to know of the file read through [read]. */
  fun directoryInfo(read: (offset: Long, size: Int) -> ByteArray): SquareDirectoryInfo {
    val size = directorySize(read)
    val bytes = read(HEADER_SIZE.toLong(), size)
    return SquareDirectoryInfo(
      formatVersion,
      ByteReader(bytes).string(),
      HEADER_SIZE.toLong(),
      size,
      sha256Hex(bytes),
    )
  }

  /** The unpacked bytes of a square, after checking its [entry]'s checksum. */
  fun unpack(square: PathSquare, entry: SquareEntry, bytes: ByteArray): ByteArray {
    if (sha256Hex(bytes) != entry.sha256) throw GraphFormatException("Square $square is broken")
    return inflate(bytes, entry.rawSize)
  }

  private fun directorySize(read: (offset: Long, size: Int) -> ByteArray): Int {
    val header = read(0, HEADER_SIZE)
    if (header.size < HEADER_SIZE || !header.copyOf(magicBytes.size).contentEquals(magicBytes)) {
      throw GraphFormatException("Not a $name")
    }
    val version = header[magicBytes.size].toInt() and 0xFF
    if (version != formatVersion) {
      throw UnsupportedFormatVersionException(
        "${name.replaceFirstChar { it.uppercase() }} format $version is not supported"
      )
    }
    var size = 0
    for (shift in 0..3) {
      size = (size shl 8) or (header[magicBytes.size + 1 + shift].toInt() and 0xFF)
    }
    return size
  }

  /** An opened file: its directory, and its squares read on demand. */
  class Opened
  internal constructor(
    val directory: SquareDirectory,
    private val dataOffset: Long,
    private val read: (offset: Long, size: Int) -> ByteArray,
  ) {

    /** The packed bytes of [square], `null` if it isn't in the file. */
    fun bytes(square: PathSquare): Pair<SquareEntry, ByteArray>? {
      val entry = directory.entries[square] ?: return null
      return entry to read(dataOffset + entry.offset, entry.size)
    }
  }

  private companion object {
    const val HEADER_SIZE = 9
  }
}
