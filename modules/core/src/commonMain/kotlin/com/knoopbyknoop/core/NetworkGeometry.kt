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
 * @property bridges the stretches of [course] on a bridge
 * @property back the course from that node of the `to` knot back to that node of the `from` knot,
 *   where it is not [course] reversed, see `network-data` "Courses to each signpost in each
 *   direction"
 * @property oneWay the only direction it can be ridden in, if only one
 */
data class SignpostCourse(
  val fromPosition: Int,
  val toPosition: Int,
  val course: List<GeoPoint>,
  val bridges: List<Stretch> = listOf(),
  val back: CourseBack? = null,
  val oneWay: OneWay? = null,
)

/**
 * The only direction a course to a signpost can be ridden in, where the route's ways lead there in
 * one direction only, see `network-data` "Courses to each signpost in each direction".
 */
enum class OneWay {
  /** From the connection's `from` knot to its `to` knot only. */
  THERE,
  /** From the connection's `to` knot back to its `from` knot only. */
  BACK,
}

/**
 * A stretch of a course from the point at [first] to the point at [last], e.g. on a bridge, see
 * `network-data` "Bridge stretches of courses".
 */
data class Stretch(val first: Int, val last: Int)

/**
 * The course of a connection from its `to` knot back to its `from` knot, where it is not the course
 * reversed, with its own [bridges].
 */
data class CourseBack(val course: List<GeoPoint>, val bridges: List<Stretch> = listOf())

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
 * @property bridges per index into [courses], the stretches of the course on a bridge, see
 *   `network-data` "Bridge stretches of courses"
 * @property coursesBack per index into [courses], the course from the connection's `to` knot back
 *   to its `from` knot where it is not the course reversed, see `network-data` "Course in each
 *   riding direction"
 */
class NetworkGeometry
private constructor(
  val graph: NetworkGraph,
  val courses: List<List<GeoPoint>>,
  val signpostCourses: Map<Int, List<SignpostCourse>>,
  val bridges: Map<Int, List<Stretch>>,
  val coursesBack: Map<Int, CourseBack>,
  /** The installed geometry this one adds route points to, see [withRoutePoints]. */
  private val base: NetworkGeometry?,
) {

  constructor(
    graph: NetworkGraph,
    courses: List<List<GeoPoint>>,
    signpostCourses: Map<Int, List<SignpostCourse>> = mapOf(),
    bridges: Map<Int, List<Stretch>> = mapOf(),
    coursesBack: Map<Int, CourseBack> = mapOf(),
  ) : this(graph, courses, signpostCourses, bridges, coursesBack, null)

  init {
    require(courses.size == graph.connections.size) {
      "Expected ${graph.connections.size} courses, got ${courses.size}"
    }
    require(courses.all { it.size >= 2 }) { "A course needs at least two points" }
  }

  private val courseIndexByKnotIds: Map<Pair<Long, Long>, Int> =
    graph.connections
      .withIndex()
      .drop(base?.courses?.size ?: 0)
      .associate { (index, connection) -> (connection.fromId to connection.toId) to index }
      .let { added -> base?.let { OverlayMap(it.courseIndexByKnotIds, added) } ?: added }

  /**
   * The installed geometry without route points: this one, or the one [withRoutePoints] was called
   * on.
   */
  val installed: NetworkGeometry
    get() = base ?: this

  /** Whether any course or signpost course has a stretch on a bridge, see `map-planning`. */
  val hasBridges: Boolean
    get() = installed.bridgeEnds.isNotEmpty()

  /**
   * The segments of all courses on a bridge, from each end to the other ends. A segment is found by
   * its points, whichever course, direction or join it is part of.
   */
  private val bridgeEnds: Map<GeoPoint, List<GeoPoint>> by lazy {
    val ends = HashMap<GeoPoint, MutableList<GeoPoint>>()
    fun addStretches(course: List<GeoPoint>, stretches: List<Stretch>) {
      for (stretch in stretches) {
        for (index in stretch.first until stretch.last.coerceAtMost(course.lastIndex)) {
          val from = course[index]
          val to = course[index + 1]
          ends.getOrPut(from) { mutableListOf() }.add(to)
          ends.getOrPut(to) { mutableListOf() }.add(from)
        }
      }
    }
    bridges.forEach { (index, stretches) ->
      courses.getOrNull(index)?.let { addStretches(it, stretches) }
    }
    signpostCourses.values.flatten().forEach {
      addStretches(it.course, it.bridges)
      it.back?.let { back -> addStretches(back.course, back.bridges) }
    }
    coursesBack.values.forEach { addStretches(it.course, it.bridges) }
    ends
  }

  /**
   * Whether the segment from [from] to [to] of a line drawn from the courses runs on a bridge. A
   * segment cut at a route point or at the rider's position counts by its remaining end and the cut
   * point lying on a bridge segment.
   */
  fun isOnBridge(from: GeoPoint, to: GeoPoint): Boolean {
    val ends = installed.bridgeEnds
    if (ends.isEmpty()) return false
    fun reaches(end: GeoPoint, other: GeoPoint) =
      ends[end]?.any { it == other || onSegment(other, end, it) } == true
    return reaches(to, from) || reaches(from, to)
  }

  private fun onSegment(point: GeoPoint, from: GeoPoint, to: GeoPoint) =
    distanceToLine(point.latitude, point.longitude, from, to) <= ON_BRIDGE_SEGMENT_METERS

  /** Each course's bounds as south, west, north, east, to find places quickly. */
  private val bounds: List<DoubleArray> by lazy {
    courses.map { course ->
      doubleArrayOf(
        course.minOf { it.latitude },
        course.minOf { it.longitude },
        course.maxOf { it.latitude },
        course.maxOf { it.longitude },
      )
    }
  }

  /**
   * The nearest places on the courses of up to [count] connections within [maxMeters] of
   * [position], nearest first, one per connection. Connections with a ferry are left out, as the
   * data does not say where on the course the ferry is, see `map-planning` "On a ferry connection".
   */
  fun nearestOnCourses(position: GeoPoint, maxMeters: Double, count: Int = 1): List<CoursePlace> {
    val installed = installed
    val latitudeMargin = maxMeters / 111_195.0
    val longitudeMargin = latitudeMargin / kotlin.math.cos(position.latitude * kotlin.math.PI / 180)
    val ferries = installed.graph.ferries.keys
    return installed.courses.indices
      .asSequence()
      .filter { index ->
        val box = installed.bounds[index]
        position.latitude >= box[0] - latitudeMargin &&
          position.latitude <= box[2] + latitudeMargin &&
          position.longitude >= box[1] - longitudeMargin &&
          position.longitude <= box[3] + longitudeMargin
      }
      .map { index -> index to nearestOn(installed.courses[index], position) }
      .filter { (index, on) ->
        on.distanceMeters <= maxMeters && installed.graph.connections[index] !in ferries
      }
      .sortedBy { (_, on) -> on.distanceMeters }
      .take(count)
      .map { (index, on) ->
        CoursePlace(installed.graph.connections[index], on.fraction, on.place, on.distanceMeters)
      }
      .toList()
  }

  /**
   * This geometry for [routeGraph], the installed graph with route points: the courses of their
   * connections are cut from the installed courses at their places, with their ways in front, see
   * `route-planning` "Route from waypoints". Signpost courses passing within 10 m of a place are
   * cut too, so a route from a point picks the fitting signpost.
   */
  fun withRoutePoints(routeGraph: NetworkGraph): NetworkGeometry {
    val installed = installed
    if (routeGraph.connections.size == installed.courses.size) return installed
    val added = routeGraph.connections.drop(installed.courses.size)
    val addedCourses = added.map { connection ->
      installed.routePointCourse(routeGraph, connection)
    }
    val addedSignposts =
      added
        .withIndex()
        .mapNotNull { (offset, connection) ->
          installed
            .routePointSignposts(routeGraph, connection)
            .takeIf { it.isNotEmpty() }
            ?.let {
              installed.courses.size + offset to it
            }
        }
        .toMap()
    return NetworkGeometry(
      routeGraph,
      installed.courses + addedCourses,
      if (addedSignposts.isEmpty()) installed.signpostCourses
      else installed.signpostCourses + addedSignposts,
      installed.bridges,
      installed.coursesBack,
      installed,
    )
  }

  /** The course of [connection], which starts at a route point. */
  private fun routePointCourse(routeGraph: NetworkGraph, connection: Connection): List<GeoPoint> {
    val point = routeGraph.knots.getValue(connection.fromId)
    val routePoint = point.routePoint!!
    val other = routeGraph.knots.getValue(connection.toId)
    val main = mainCourseOf(routePoint)
    val way = routePoint.way
    val otherRoutePoint = other.routePoint
    val onCourse =
      when {
        otherRoutePoint != null ->
          between(main, routePoint.fraction, otherRoutePoint.fractionFrom(routePoint.fromId)) +
            otherRoutePoint.way.asReversed()
        other.id == routePoint.fromId ->
          cutAt(main, routePoint.fraction, routePoint.place).first.asReversed()
        else -> cutAt(main, routePoint.fraction, routePoint.place).second
      }
    return joined(way, onCourse)
  }

  /** The signpost courses of [connection], which starts at a route point and ends at a knot. */
  private fun routePointSignposts(
    routeGraph: NetworkGraph,
    connection: Connection,
  ): List<SignpostCourse> {
    val routePoint = routeGraph.knots.getValue(connection.fromId).routePoint!!
    if (routeGraph.knots.getValue(connection.toId).isRoutePoint) return listOf()
    val index = courseIndexByKnotIds[routePoint.fromId to routePoint.toId] ?: return listOf()
    return signpostCourses[index].orEmpty().mapNotNull { signpost ->
      val on = nearestOn(signpost.course, routePoint.place)
      if (on.distanceMeters > ON_SIGNPOST_METERS) return@mapNotNull null
      val (before, after) = cutAt(signpost.course, on.fraction, routePoint.place)
      if (connection.toId == routePoint.fromId) {
        SignpostCourse(0, signpost.fromPosition, joined(routePoint.way, before.asReversed()))
      } else {
        SignpostCourse(0, signpost.toPosition, joined(routePoint.way, after))
      }
    }
  }

  /** The main course of a route point's connection, from its `from` knot. */
  private fun mainCourseOf(routePoint: RoutePoint): List<GeoPoint> =
    courseIndexByKnotIds[routePoint.fromId to routePoint.toId]?.let { courses[it] }
      ?: courseIndexByKnotIds[routePoint.toId to routePoint.fromId]?.let {
        courses[it].asReversed()
      }
      ?: listOf(routePoint.place, routePoint.place)

  /**
   * The course from [from] to [to] along their direct connection, or `null` if there is none: the
   * course back where the connection has one, see `network-data` "Course in each riding direction",
   * otherwise the course reversed.
   */
  fun courseOf(from: Knot, to: Knot): List<GeoPoint>? {
    courseIndexByKnotIds[from.id to to.id]?.let {
      return courses[it]
    }
    return courseIndexByKnotIds[to.id to from.id]?.let {
      coursesBack[it]?.course ?: courses[it].asReversed()
    }
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
      signpostCourses[index].orEmpty().filter { it.oneWay != OneWay.BACK }.map { it.course }
    }
      ?: courseIndexByKnotIds[to.id to from.id]?.let { index ->
        signpostCourses[index]
          .orEmpty()
          .filter { it.oneWay != OneWay.THERE }
          .map {
            it.back?.course ?: it.course.asReversed()
          }
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
        signpostCourses[index]
          .orEmpty()
          .filter { it.oneWay != OneWay.BACK }
          .map {
            LegOption(it.fromPosition, it.toPosition, it.course)
          }
      }
    val backward =
      courseIndexByKnotIds[to.id to from.id]?.let { index ->
        signpostCourses[index]
          .orEmpty()
          .filter { it.oneWay != OneWay.THERE }
          .map {
            LegOption(it.toPosition, it.fromPosition, it.back?.course ?: it.course.asReversed())
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
    // After the signpost courses, so apps since 0.7.0 read the file as before, see `network-data`
    // "Released apps".
    val sortedBridges = bridges.entries.filter { it.value.isNotEmpty() }.sortedBy { it.key }
    writer.varint(sortedBridges.size.toLong())
    var previousIndex = 0
    for ((index, stretches) in sortedBridges) {
      writer.varint((index - previousIndex).toLong())
      writeStretches(writer, stretches)
      previousIndex = index
    }
    val signpostBridges =
      signpostCourses.entries
        .sortedBy { it.key }
        .flatMap { (index, pairs) ->
          pairs
            .withIndex()
            .filter { it.value.bridges.isNotEmpty() }
            .map { Triple(index, it.index, it.value.bridges) }
        }
    writer.varint(signpostBridges.size.toLong())
    for ((index, position, stretches) in signpostBridges) {
      writer.varint(index.toLong())
      writer.varint(position.toLong())
      writeStretches(writer, stretches)
    }
    // After the bridge sections, see `network-data` "Course in each riding direction". Each course
    // back is coded from the connection's `to` knot, where it starts.
    val sortedBack = coursesBack.entries.sortedBy { it.key }
    writer.varint(sortedBack.size.toLong())
    previousIndex = 0
    for ((index, back) in sortedBack) {
      writer.varint((index - previousIndex).toLong())
      writePoints(writer, graph.knots.getValue(graph.connections[index].toId), back.course)
      writeStretches(writer, back.bridges)
      previousIndex = index
    }
    // Then the signpost courses back, see "Courses to each signpost in each direction".
    // Each entry: the kind, 0 for a course back, 1 for "there only", 2 for "back only".
    val signpostBacks =
      signpostCourses.entries
        .sortedBy { it.key }
        .flatMap { (index, pairs) ->
          pairs
            .withIndex()
            .filter { it.value.back != null || it.value.oneWay != null }
            .map {
              Triple(index, it.index, it.value)
            }
        }
    writer.varint(signpostBacks.size.toLong())
    for ((index, position, pair) in signpostBacks) {
      writer.varint(index.toLong())
      writer.varint(position.toLong())
      val back = pair.back
      when {
        back != null -> {
          writer.varint(0)
          writePoints(writer, graph.knots.getValue(graph.connections[index].toId), back.course)
          writeStretches(writer, back.bridges)
        }
        pair.oneWay == OneWay.THERE -> writer.varint(1)
        else -> writer.varint(2)
      }
    }
    return writer.toByteArray()
  }

  private fun writePoints(writer: ByteWriter, start: Knot, points: List<GeoPoint>) {
    var previousLatitude = toMicrodegrees(start.latitude)
    var previousLongitude = toMicrodegrees(start.longitude)
    writer.varint(points.size.toLong())
    for (point in points) {
      val latitude = toMicrodegrees(point.latitude)
      val longitude = toMicrodegrees(point.longitude)
      writer.varint(zigzag(latitude - previousLatitude))
      writer.varint(zigzag(longitude - previousLongitude))
      previousLatitude = latitude
      previousLongitude = longitude
    }
  }

  private fun writeStretches(writer: ByteWriter, stretches: List<Stretch>) {
    writer.varint(stretches.size.toLong())
    var previousLast = 0
    for (stretch in stretches) {
      writer.varint((stretch.first - previousLast).toLong())
      writer.varint((stretch.last - stretch.first).toLong())
      previousLast = stretch.last
    }
  }

  companion object {

    /** A course passes a signpost this close: published courses are simplified to within 5 m. */
    private const val ON_SIGNPOST_METERS = 10.0

    /** A cut point this close to a bridge segment lies on it; cuts are made on the segment. */
    private const val ON_BRIDGE_SEGMENT_METERS = 1.0

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
      val signpostCourses = readSignpostCourses(reader, graph)
      if (!reader.hasMore()) return NetworkGeometry(graph, courses, signpostCourses)
      var index = 0
      val bridges =
        List(reader.count()) {
            index += reader.count()
            index to readStretches(reader)
          }
          .toMap()
      val signpostBridges =
        List(reader.count()) { (reader.count() to reader.count()) to readStretches(reader) }.toMap()
      val signpostsWithBridges = signpostCourses.mapValues { (courseIndex, pairs) ->
        pairs.mapIndexed { position, pair ->
          signpostBridges[courseIndex to position]?.let { pair.copy(bridges = it) } ?: pair
        }
      }
      if (!reader.hasMore()) return NetworkGeometry(graph, courses, signpostsWithBridges, bridges)
      var backIndex = 0
      val coursesBack =
        List(reader.count()) {
            backIndex += reader.count()
            val to = graph.knots.getValue(graph.connections[backIndex].toId)
            var latitude = toMicrodegrees(to.latitude)
            var longitude = toMicrodegrees(to.longitude)
            val points =
              List(reader.count()) {
                latitude += unzigzag(reader.varint())
                longitude += unzigzag(reader.varint())
                GeoPoint(fromMicrodegrees(latitude), fromMicrodegrees(longitude))
              }
            backIndex to CourseBack(points, readStretches(reader))
          }
          .toMap()
      if (!reader.hasMore()) {
        return NetworkGeometry(graph, courses, signpostsWithBridges, bridges, coursesBack)
      }
      val signpostBacks =
        List(reader.count()) {
            val index = reader.count()
            val position = reader.count()
            val change: (SignpostCourse) -> SignpostCourse =
              when (reader.count()) {
                0 -> {
                  val to = graph.knots.getValue(graph.connections[index].toId)
                  var latitude = toMicrodegrees(to.latitude)
                  var longitude = toMicrodegrees(to.longitude)
                  val points =
                    List(reader.count()) {
                      latitude += unzigzag(reader.varint())
                      longitude += unzigzag(reader.varint())
                      GeoPoint(fromMicrodegrees(latitude), fromMicrodegrees(longitude))
                    }
                  val back = CourseBack(points, readStretches(reader))
                  ({ it.copy(back = back) })
                }
                1 -> ({ it.copy(oneWay = OneWay.THERE) })
                else -> ({ it.copy(oneWay = OneWay.BACK) })
              }
            (index to position) to change
          }
          .toMap()
      val signpostsBothWays = signpostsWithBridges.mapValues { (index, pairs) ->
        pairs.mapIndexed { position, pair ->
          signpostBacks[index to position]?.invoke(pair) ?: pair
        }
      }
      return NetworkGeometry(graph, courses, signpostsBothWays, bridges, coursesBack)
    }

    /** Stretches as written by `writeStretches`: each one's start after the last one's end. */
    private fun readStretches(reader: ByteReader): List<Stretch> {
      var previousLast = 0
      return List(reader.count()) {
        val first = previousLast + reader.count()
        val last = first + reader.count()
        previousLast = last
        Stretch(first, last)
      }
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
