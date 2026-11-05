package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.PathKind
import com.knoopbyknoop.core.PathSegment
import com.knoopbyknoop.core.PathSquare

/** What the router and the drawings need of a way, or `null` for ways neither needs. */
internal data class PathAttributes(
  val kind: PathKind,
  val forward: Boolean,
  val backward: Boolean,
  val costFactor: Double,
  val name: String?,
) {

  companion object {

    private val RIDDEN =
      setOf(
        "primary",
        "primary_link",
        "secondary",
        "secondary_link",
        "tertiary",
        "tertiary_link",
        "unclassified",
        "residential",
        "living_street",
        "service",
        "track",
        "path",
        "cycleway",
        "footway",
        "pedestrian",
        "bridleway",
      )
    private val CLOSED_UNLESS_TAGGED =
      setOf("motorway", "motorway_link", "trunk", "trunk_link", "steps")
    private val OPEN = setOf("yes", "designated")
    private val PERMITTED = OPEN + "permissive"
    private val LANES = setOf("track", "lane", "separate", "opposite_track", "opposite_lane")
    private val LANE_KEYS = listOf("cycleway", "cycleway:left", "cycleway:right", "cycleway:both")
    private val SIDEWALK_KEYS =
      listOf("sidewalk:right:bicycle", "sidewalk:left:bicycle", "sidewalk:both:bicycle")
    private val EXCLUDED_SERVICES = setOf("parking_aisle", "driveway", "drive-through")
    private val OPPOSITE = setOf("opposite", "opposite_lane", "opposite_track")

    /** See the change's design. */
    private val BASE_FACTORS =
      mapOf(
        "cycleway" to 1.0,
        "living_street" to 1.15,
        "residential" to 1.2,
        "service" to 1.5,
        "unclassified" to 1.3,
        "tertiary" to 1.7,
        "secondary" to 2.5,
        "primary" to 4.0,
        "track" to 1.3,
        "path" to 1.4,
        "bridleway" to 2.0,
        "footway" to 3.0,
        "pedestrian" to 2.2,
        "trunk" to 5.0,
      )
    private val SURFACE_FACTORS =
      mapOf(
        "gravel" to 1.15,
        "fine_gravel" to 1.05,
        "compacted" to 1.05,
        "unpaved" to 1.3,
        "dirt" to 1.6,
        "ground" to 1.6,
        "earth" to 1.6,
        "grass" to 2.0,
        "mud" to 2.5,
        "sand" to 2.5,
        "sett" to 1.4,
        "cobblestone" to 1.7,
        "unhewn_cobblestone" to 2.0,
        "pebblestone" to 1.6,
        "woodchips" to 1.8,
      )

    fun of(tags: Map<String, String>): PathAttributes? {
      val highway = tags["highway"] ?: return null
      if (highway !in RIDDEN && highway !in CLOSED_UNLESS_TAGGED) return null
      val bicycle = tags["bicycle"]
      if (highway == "service" && tags["service"] in EXCLUDED_SERVICES) return null
      if ((tags["access"] == "private" || tags["access"] == "no") && bicycle !in PERMITTED) {
        return null
      }
      val kind = kindOf(highway, tags)
      val closed =
        bicycle == "no" ||
          bicycle == "use_sidepath" ||
          (highway in CLOSED_UNLESS_TAGGED && bicycle !in OPEN)
      if (closed) {
        // Only roads are drawn; a closed footway or steps add nothing to a junction's picture.
        if (kind == PathKind.PATH || kind == PathKind.CYCLE || highway == "steps") return null
        return PathAttributes(kind, false, false, 0.0, tags["name"] ?: tags["ref"])
      }
      val oneWay =
        (tags["oneway"] == "yes" || tags["junction"] == "roundabout") &&
          tags["oneway:bicycle"] != "no" &&
          tags["cycleway"] !in OPPOSITE
      val reversedOneWay = tags["oneway"] == "-1"
      return PathAttributes(
        kind,
        forward = !reversedOneWay,
        backward = !oneWay,
        costFactor = factorOf(highway, tags),
        name = tags["name"],
      )
    }

    private fun kindOf(highway: String, tags: Map<String, String>): PathKind {
      val road = highway.removeSuffix("_link")
      return when {
        road in setOf("motorway", "trunk", "primary", "secondary") -> PathKind.MAJOR
        road == "tertiary" -> PathKind.MAIN
        road == "cycleway" || tags["bicycle_road"] == "yes" -> PathKind.CYCLE
        road in setOf("path", "footway", "track") && tags["bicycle"] in OPEN -> PathKind.CYCLE
        road in setOf("residential", "unclassified", "living_street", "service", "pedestrian") ->
          PathKind.STREET
        else -> PathKind.PATH
      }
    }

    private fun hasLane(tags: Map<String, String>) =
      LANE_KEYS.any { tags[it] in LANES } || SIDEWALK_KEYS.any { tags[it] in OPEN }

    private fun factorOf(highway: String, tags: Map<String, String>): Double {
      val road = highway.removeSuffix("_link")
      var factor = BASE_FACTORS[road] ?: 3.0
      if (tags["bicycle_road"] == "yes") factor = 1.0
      if (road in setOf("path", "footway", "pedestrian", "track") && tags["bicycle"] in OPEN) {
        factor = minOf(factor, 1.05)
      }
      if (
        road in setOf("tertiary", "secondary", "primary", "unclassified", "residential") &&
          hasLane(tags)
      ) {
        factor = 1 + (factor - 1) * 0.25
      }
      if (road == "track" && tags["tracktype"] in setOf("grade3", "grade4", "grade5")) {
        factor *= 1.4
      }
      if (tags["bicycle"] == "dismount") factor = maxOf(factor, 3.0)
      return factor * (SURFACE_FACTORS[tags["surface"]] ?: 1.0)
    }
  }
}

/**
 * Collects the ways around the networks: all ways first, then all nodes, as the extracts' order is.
 * Ways are cut at junctions into segments, each in the squares of both of its ends. Keeps nodes in
 * primitive arrays, as there are millions of them.
 */
class PathExtraction {

  private class KeptWay(val attributes: PathAttributes, val nodeIds: LongArray)

  private val ways = ArrayList<KeptWay>()
  private val seenWays = HashSet<Long>()
  private var nodeIds = LongArray(0)
  private var latitudes = DoubleArray(0)
  private var longitudes = DoubleArray(0)
  private var uses = ByteArray(0)
  private var nodesPrepared = false

  fun acceptWay(way: OsmWay) {
    if (way.nodeIds.size < 2 || !seenWays.add(way.id)) return
    val attributes = PathAttributes.of(way.tags) ?: return
    ways.add(KeptWay(attributes, way.nodeIds.toLongArray()))
  }

  fun acceptNode(node: OsmNode) {
    if (!nodesPrepared) prepareNodes()
    val index = nodeIds.binarySearch(node.id)
    if (index < 0) return
    latitudes[index] = node.latitude
    longitudes[index] = node.longitude
  }

  private fun prepareNodes() {
    nodesPrepared = true
    val all = LongArray(ways.sumOf { it.nodeIds.size })
    var position = 0
    ways.forEach {
      it.nodeIds.copyInto(all, position)
      position += it.nodeIds.size
    }
    all.sort()
    var unique = 0
    for (index in all.indices) {
      if (index == 0 || all[index] != all[index - 1]) all[unique++] = all[index]
    }
    nodeIds = all.copyOf(unique)
    latitudes = DoubleArray(unique) { Double.NaN }
    longitudes = DoubleArray(unique) { Double.NaN }
    uses = ByteArray(unique)
    ways.forEach { way ->
      way.nodeIds.forEachIndexed { index, id ->
        val position = nodeIds.binarySearch(id)
        // A way's ends count twice, so they are always junctions.
        val count = if (index == 0 || index == way.nodeIds.lastIndex) 2 else 1
        uses[position] = minOf(uses[position] + count, 2).toByte()
      }
    }
  }

  /** The segments of all squares. */
  fun squares(): Map<PathSquare, List<PathSegment>> {
    if (!nodesPrepared) prepareNodes()
    val squares = HashMap<PathSquare, MutableList<PathSegment>>()
    ways.forEach { way -> segments(way).forEach { add(it, squares) } }
    return squares
  }

  private fun add(segment: PathSegment, squares: MutableMap<PathSquare, MutableList<PathSegment>>) {
    val first = PathSquare.of(segment.points.first().latitude, segment.points.first().longitude)
    val last = PathSquare.of(segment.points.last().latitude, segment.points.last().longitude)
    squares.getOrPut(first) { mutableListOf() }.add(segment)
    if (last != first) squares.getOrPut(last) { mutableListOf() }.add(segment)
  }

  private fun segments(way: KeptWay): List<PathSegment> {
    val result = ArrayList<PathSegment>()
    var points = ArrayList<GeoPoint>()
    var startId = 0L
    fun close(endId: Long) {
      if (points.size >= 2) {
        val attributes = way.attributes
        result.add(
          PathSegment(
            startId,
            endId,
            attributes.kind,
            attributes.forward,
            attributes.backward,
            attributes.costFactor,
            attributes.name,
            simplify(points, PATH_TOLERANCE_METERS),
          )
        )
      }
    }
    for (id in way.nodeIds) {
      val index = nodeIds.binarySearch(id)
      val latitude = latitudes[index]
      // A node missing from the extracts ends the piece of the way, as at the extracts' border.
      if (latitude.isNaN()) {
        points = ArrayList()
        continue
      }
      val point = GeoPoint(latitude, longitudes[index])
      if (points.isEmpty()) {
        startId = id
        points.add(point)
        continue
      }
      points.add(point)
      if (uses[index] >= 2) {
        close(id)
        points = arrayListOf(point)
        startId = id
      }
    }
    return result
  }

  private companion object {

    const val PATH_TOLERANCE_METERS = 5.0
  }
}
