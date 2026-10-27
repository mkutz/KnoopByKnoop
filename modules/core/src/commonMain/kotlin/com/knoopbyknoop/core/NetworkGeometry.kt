package com.knoopbyknoop.core

/** A position on the map. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * The course of every connection of a [NetworkGraph], as published in geometry files.
 *
 * A geometry file belongs to exactly one graph file, identified by the graph file's SHA-256, and
 * stores the courses in the graph's connection order. See `openspec/specs/network-data`.
 *
 * @property courses one course per connection of [graph], in the same order, each running from the
 *   connection's `from` knot to its `to` knot
 */
class NetworkGeometry(val graph: NetworkGraph, val courses: List<List<GeoPoint>>) {

  init {
    require(courses.size == graph.connections.size) {
      "Expected ${graph.connections.size} courses, got ${courses.size}"
    }
    require(courses.all { it.size >= 2 }) { "A course needs at least two points" }
  }

  private val courseIndexByKnotIds: Map<Pair<Long, Long>, Int> =
    graph.connections.withIndex().associate { (index, connection) ->
      (connection.fromId to connection.toId) to index
    }

  /** The course from [from] to [to] along their direct connection, or `null` if there is none. */
  fun courseOf(from: Knot, to: Knot): List<GeoPoint>? {
    courseIndexByKnotIds[from.id to to.id]?.let {
      return courses[it]
    }
    return courseIndexByKnotIds[to.id to from.id]?.let { courses[it].asReversed() }
  }

  /**
   * The course of a whole route in riding direction, as one continuous line, or `null` if the route
   * is incomplete.
   */
  fun courseOf(route: Route): List<GeoPoint>? {
    val knots = (route.summary as? RouteSummary.Complete)?.knots ?: return null
    if (knots.size == 1) return listOf(GeoPoint(knots.single().latitude, knots.single().longitude))
    val course = mutableListOf<GeoPoint>()
    for ((from, to) in knots.zipWithNext()) {
      val leg = courseOf(from, to) ?: return null
      course.addAll(if (course.isEmpty()) leg else leg.drop(1))
    }
    return course
  }

  /** The geometry file for the graph file whose SHA-256 is [graphSha256]. */
  fun toBytes(graphSha256: String): ByteArray {
    val writer = ByteWriter()
    writer.bytes(MAGIC)
    writer.byte(FORMAT_VERSION)
    writer.bytes(hexToBytes(graphSha256))
    writer.varint(courses.size.toLong())
    for ((connection, course) in graph.connections.zip(courses)) {
      val from = graph.knots.getValue(connection.fromId)
      var previousLatitude = toMicrodegrees(from.latitude)
      var previousLongitude = toMicrodegrees(from.longitude)
      writer.varint(course.size.toLong())
      for (point in course) {
        val latitude = toMicrodegrees(point.latitude)
        val longitude = toMicrodegrees(point.longitude)
        writer.varint(zigzag(latitude - previousLatitude))
        writer.varint(zigzag(longitude - previousLongitude))
        previousLatitude = latitude
        previousLongitude = longitude
      }
    }
    return writer.toByteArray()
  }

  companion object {

    const val FORMAT_VERSION = 1

    private val MAGIC = "KBKO".encodeToByteArray()

    /**
     * Whether [bytes] are a geometry file built for the graph file whose SHA-256 is [graphSha256].
     */
    fun belongsTo(bytes: ByteArray, graphSha256: String): Boolean =
      try {
        readHeader(ByteReader(bytes)) == graphSha256
      } catch (exception: GraphFormatException) {
        false
      }

    /**
     * Reads a geometry file for [graph], whose graph file has the SHA-256 [graphSha256].
     *
     * @throws GraphFormatException if [bytes] are no geometry file of a supported version, or one
     *   that belongs to another graph file
     */
    @Throws(GraphFormatException::class)
    fun read(bytes: ByteArray, graph: NetworkGraph, graphSha256: String): NetworkGeometry {
      val reader = ByteReader(bytes)
      if (readHeader(reader) != graphSha256) {
        throw GraphFormatException("Geometry file belongs to another graph file")
      }
      val courseCount = reader.count()
      if (courseCount != graph.connections.size) {
        throw GraphFormatException(
          "Geometry file has $courseCount courses for ${graph.connections.size} connections"
        )
      }
      val courses =
        graph.connections.map { connection ->
          val from = graph.knots.getValue(connection.fromId)
          var latitude = toMicrodegrees(from.latitude)
          var longitude = toMicrodegrees(from.longitude)
          List(reader.count()) {
            latitude += unzigzag(reader.varint())
            longitude += unzigzag(reader.varint())
            GeoPoint(fromMicrodegrees(latitude), fromMicrodegrees(longitude))
          }
        }
      return NetworkGeometry(graph, courses)
    }

    /** Straight lines between the knots, for a graph without matching geometry. */
    fun straightLines(graph: NetworkGraph): NetworkGeometry =
      NetworkGeometry(
        graph,
        graph.connections.map { connection ->
          listOf(connection.fromId, connection.toId).map {
            val knot = graph.knots.getValue(it)
            GeoPoint(knot.latitude, knot.longitude)
          }
        },
      )

    /** @return the SHA-256 of the graph file the geometry file belongs to */
    private fun readHeader(reader: ByteReader): String {
      if (!reader.bytes(MAGIC.size).contentEquals(MAGIC)) {
        throw GraphFormatException("Not a geometry file")
      }
      val formatVersion = reader.byte()
      if (formatVersion != FORMAT_VERSION) {
        throw UnsupportedFormatVersionException(
          "Unsupported geometry file format version $formatVersion"
        )
      }
      return reader.bytes(32).joinToString("") {
        (it.toInt() and 0xFF).toString(16).padStart(2, '0')
      }
    }

    private fun hexToBytes(hex: String): ByteArray {
      require(hex.length == 64) { "A SHA-256 has 64 hex digits, was ${hex.length}" }
      return ByteArray(32) { hex.substring(2 * it, 2 * it + 2).toInt(16).toByte() }
    }
  }
}
