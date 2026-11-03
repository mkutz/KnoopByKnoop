package com.knoopbyknoop.core

/** A position on the map. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * A course of a connection between two of the nodes its knots are tagged on, see `network-data`
 * "Courses to each signpost".
 *
 * @property fromPosition the position in [Knot.positions] of the connection's `from` knot
 * @property toPosition the position in [Knot.positions] of the connection's `to` knot
 * @property course from that node of the `from` knot to that node of the `to` knot
 */
data class SignpostCourse(val fromPosition: Int, val toPosition: Int, val course: List<GeoPoint>)

/**
 * The course of every connection of a [NetworkGraph], as published in geometry files.
 *
 * A geometry file belongs to exactly one graph file, identified by the graph file's SHA-256, and
 * stores the courses in the graph's connection order. See `openspec/specs/network-data`.
 *
 * @property courses one course per connection of [graph], in the same order, each running from the
 *   connection's `from` knot to its `to` knot
 * @property signpostCourses per index into [courses], the courses to each signpost, for connections
 *   reaching their knots at several of them
 */
class NetworkGeometry(
  val graph: NetworkGraph,
  val courses: List<List<GeoPoint>>,
  val signpostCourses: Map<Int, List<SignpostCourse>> = mapOf(),
) {

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
    if (knots.zipWithNext().any { (from, to) -> courseOf(from, to) == null }) return null
    return courses(knots).fold(listOf()) { course, leg ->
      if (course.isEmpty()) leg else course + leg.drop(1)
    }
  }

  /**
   * The courses to draw between the neighbors in [knots], in riding direction, through the
   * signposts that fit the legs on both sides and make the line shortest, see `map-planning`
   * "Routes through a knot with several signposts". Without a course, a straight line.
   */
  fun courses(knots: List<Knot>): List<List<GeoPoint>> {
    // The shortest way so far to each position of the current knot, with the courses of that way.
    var ways = mapOf<Int, Pair<Double, List<List<GeoPoint>>>>()
    for ((from, to) in knots.zipWithNext()) {
      val next = mutableMapOf<Int, Pair<Double, List<List<GeoPoint>>>>()
      for (option in optionsOf(from, to)) {
        val before = if (ways.isEmpty()) 0.0 to listOf() else ways[option.fromPosition] ?: continue
        val meters = before.first + lengthOf(option.course)
        val best = next[option.toPosition]
        if (best == null || meters < best.first) {
          next[option.toPosition] = meters to before.second + listOf(option.course)
        }
      }
      ways = next
    }
    return ways.values.minByOrNull { it.first }?.second.orEmpty()
  }

  /**
   * The position of [knot] that [course], arriving at it, gets to first: it can run through one
   * signpost on to another, see `knot-navigation` "Straight ways to a knot with several signposts".
   */
  internal fun firstReachedOn(course: List<GeoPoint>, knot: Knot): GeoPoint {
    for ((from, to) in course.zipWithNext()) {
      knot.positions
        .filter { distanceToLine(it.latitude, it.longitude, from, to) <= ON_SIGNPOST_METERS }
        .minByOrNull { distanceMeters(from.latitude, from.longitude, it.latitude, it.longitude) }
        ?.let {
          return it
        }
    }
    return course.last()
  }

  /**
   * The other courses of the connection between [from] and [to], see "Courses to each signpost".
   */
  fun signpostCoursesOf(from: Knot, to: Knot): List<List<GeoPoint>> =
    courseIndexByKnotIds[from.id to to.id]?.let { index ->
      signpostCourses[index].orEmpty().map { it.course }
    }
      ?: courseIndexByKnotIds[to.id to from.id]?.let { index ->
        signpostCourses[index].orEmpty().map { it.course.asReversed() }
      }
      ?: listOf()

  /** A way from a position of one knot to a position of the next. */
  private class LegOption(val fromPosition: Int, val toPosition: Int, val course: List<GeoPoint>)

  private fun optionsOf(from: Knot, to: Knot): List<LegOption> {
    val main = courseOf(from, to) ?: listOf(pointOf(from), pointOf(to))
    val fromMain =
      startsOf(main, from).flatMap { (fromPosition, start) ->
        startsOf(start.asReversed(), to).map { (toPosition, course) ->
          LegOption(fromPosition, toPosition, course.asReversed())
        }
      }
    val forward =
      courseIndexByKnotIds[from.id to to.id]?.let { index ->
        signpostCourses[index].orEmpty().map {
          LegOption(it.fromPosition, it.toPosition, it.course)
        }
      }
    val backward =
      courseIndexByKnotIds[to.id to from.id]?.let { index ->
        signpostCourses[index].orEmpty().map {
          LegOption(it.toPosition, it.fromPosition, it.course.asReversed())
        }
      }
    return fromMain + (forward ?: backward).orEmpty()
  }

  /**
   * [course] from its start at the main position of [knot], and from each other signpost of the
   * knot that it passes: courses run to a knot's main position, also when they reach it at another
   * of its nodes.
   */
  private fun startsOf(course: List<GeoPoint>, knot: Knot): List<Pair<Int, List<GeoPoint>>> =
    listOf(0 to course) +
      knot.otherNodes.mapIndexedNotNull { index, signpost ->
        fromSignpost(course, knot, signpost)?.let { index + 1 to it }
      }

  private fun fromSignpost(
    course: List<GeoPoint>,
    knot: Knot,
    signpost: GeoPoint,
  ): List<GeoPoint>? {
    val limit =
      distanceMeters(knot.latitude, knot.longitude, signpost.latitude, signpost.longitude) *
        MAX_DETOUR_TO_SIGNPOST
    var covered = 0.0
    for ((index, segment) in course.zipWithNext().withIndex()) {
      val (from, to) = segment
      if (distanceToLine(signpost.latitude, signpost.longitude, from, to) <= ON_SIGNPOST_METERS) {
        return listOf(signpost) + course.drop(index + 1).dropWhile { it == signpost }
      }
      covered += distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
      if (covered > limit) return null
    }
    return null
  }

  private fun lengthOf(course: List<GeoPoint>): Double =
    course.zipWithNext().sumOf { (from, to) ->
      distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
    }

  private fun pointOf(knot: Knot) = GeoPoint(knot.latitude, knot.longitude)

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
    // After the courses, so apps not knowing this section read the file as before.
    writer.varint(signpostCourses.size.toLong())
    for ((index, pairs) in signpostCourses.entries.sortedBy { it.key }) {
      val from = graph.knots.getValue(graph.connections[index].fromId)
      writer.varint(index.toLong())
      writer.varint(pairs.size.toLong())
      for (pair in pairs) {
        writer.varint(pair.fromPosition.toLong())
        writer.varint(pair.toPosition.toLong())
        var previousLatitude = toMicrodegrees(from.latitude)
        var previousLongitude = toMicrodegrees(from.longitude)
        writer.varint(pair.course.size.toLong())
        for (point in pair.course) {
          val latitude = toMicrodegrees(point.latitude)
          val longitude = toMicrodegrees(point.longitude)
          writer.varint(zigzag(latitude - previousLatitude))
          writer.varint(zigzag(longitude - previousLongitude))
          previousLatitude = latitude
          previousLongitude = longitude
        }
      }
    }
    return writer.toByteArray()
  }

  companion object {

    /** A course passes a signpost this close: published courses are simplified to within 5 m. */
    private const val ON_SIGNPOST_METERS = 10.0

    /** How far along a course a signpost is looked for, in straight distances to it. */
    private const val MAX_DETOUR_TO_SIGNPOST = 3.0

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
      return NetworkGeometry(graph, courses, readSignpostCourses(reader, graph))
    }

    /** The optional section after the courses, missing in older files. */
    private fun readSignpostCourses(
      reader: ByteReader,
      graph: NetworkGraph,
    ): Map<Int, List<SignpostCourse>> {
      if (!reader.hasMore()) return mapOf()
      return List(reader.count()) {
          val index = reader.count()
          val from = graph.knots.getValue(graph.connections[index].fromId)
          index to
            List(reader.count()) {
              val fromPosition = reader.count()
              val toPosition = reader.count()
              var latitude = toMicrodegrees(from.latitude)
              var longitude = toMicrodegrees(from.longitude)
              SignpostCourse(
                fromPosition,
                toPosition,
                List(reader.count()) {
                  latitude += unzigzag(reader.varint())
                  longitude += unzigzag(reader.varint())
                  GeoPoint(fromMicrodegrees(latitude), fromMicrodegrees(longitude))
                },
              )
            }
        }
        .toMap()
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
