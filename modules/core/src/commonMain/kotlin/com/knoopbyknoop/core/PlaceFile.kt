package com.knoopbyknoop.core

/**
 * The published details of the highlighted places, in squares that can be read on their own, see
 * `network-data` "Published place details". Each place is keyed by the id the base map's tiles give
 * its feature, see [nodeFeatureId] and [wayFeatureId], and is in the squares around where its icon
 * can be.
 */
class PlaceFile private constructor(private val opened: SquareFile.Opened) {

  val dataDate: String
    get() = opened.directory.dataDate

  /** The details of the places in [square] by feature id, none if it isn't in the file. */
  fun details(square: PathSquare): Map<Long, PlaceDetails> {
    val (entry, bytes) = opened.bytes(square) ?: return mapOf()
    return readSquare(SQUARES.unpack(square, entry, bytes))
  }

  companion object {

    const val FORMAT_VERSION = 1

    /** The container of the place details' squares. */
    val SQUARES = SquareFile("KBKL", FORMAT_VERSION, "place details file")

    private const val WEBSITE = 1
    private const val PHONE = 2
    private const val OPENING_HOURS = 4

    /** The tiles' feature id of the OpenStreetMap node [id], checked on real tiles. */
    fun nodeFeatureId(id: Long) = id * 10 + 1

    /** The tiles' feature id of the OpenStreetMap way [id], checked on real tiles. */
    fun wayFeatureId(id: Long) = id * 10 + 2

    /** [details] by feature id as a square's bytes before packing. */
    fun writeSquare(details: Map<Long, PlaceDetails>): ByteArray {
      val writer = ByteWriter()
      writer.varint(details.size.toLong())
      var previousId = 0L
      details.entries
        .sortedBy { it.key }
        .forEach { (id, place) ->
          writer.varint(id - previousId)
          previousId = id
          writer.byte(
            (if (place.website != null) WEBSITE else 0) or
              (if (place.phone != null) PHONE else 0) or
              (if (place.openingHours != null) OPENING_HOURS else 0)
          )
          place.website?.let(writer::string)
          place.phone?.let(writer::string)
          place.openingHours?.let(writer::string)
        }
      return writer.toByteArray()
    }

    /** The details in a square's unpacked [bytes]. */
    internal fun readSquare(bytes: ByteArray): Map<Long, PlaceDetails> {
      val reader = ByteReader(bytes)
      val result = HashMap<Long, PlaceDetails>()
      var id = 0L
      repeat(reader.count()) {
        id += reader.varint()
        val flags = reader.byte()
        val website = if (flags and WEBSITE != 0) reader.string() else null
        val phone = if (flags and PHONE != 0) reader.string() else null
        val openingHours = if (flags and OPENING_HOURS != 0) reader.string() else null
        result[id] = PlaceDetails(website, phone, openingHours)
      }
      return result
    }

    /** The whole file's bytes for [squares], for files small enough to hold in memory. */
    fun toBytes(
      squares: Map<PathSquare, Map<Long, PlaceDetails>>,
      dataDate: String,
      attribution: String,
    ): ByteArray =
      SQUARES.toBytes(squares.mapValues { writeSquare(it.value) }, dataDate, attribution)

    /**
     * Opens a place details file through [read], which reads a range of it.
     *
     * @throws UnsupportedFormatVersionException for a format version this core doesn't know, see
     *   `network-data` "Versioned place details format"
     */
    fun open(read: (offset: Long, size: Int) -> ByteArray): PlaceFile =
      PlaceFile(SQUARES.open(read))
  }
}
