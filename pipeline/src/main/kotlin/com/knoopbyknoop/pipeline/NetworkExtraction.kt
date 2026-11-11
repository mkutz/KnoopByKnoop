package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.CourseBack
import com.knoopbyknoop.core.Crossing
import com.knoopbyknoop.core.Ferry
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.OneWay
import com.knoopbyknoop.core.Region
import com.knoopbyknoop.core.SignRegion
import com.knoopbyknoop.core.SignpostCourse
import com.knoopbyknoop.core.Stretch
import com.knoopbyknoop.core.distanceMeters
import java.util.PriorityQueue
import kotlin.math.roundToInt

/**
 * Extracts node networks from OpenStreetMap elements.
 *
 * Feed it all relations first, then all ways, then all nodes, like they are ordered in PBF files.
 * It keeps only the elements it needs: node network routes, their ways, and the nodes on them, and
 * the borders of the sign regions, see `network-data` "Sign region of each knot". Elements fed more
 * than once, e.g. from overlapping extracts, are used once.
 */
class NetworkExtraction {

  private val routes = LinkedHashMap<Long, NetworkRoute>()
  private val wayNodeIds = HashMap<Long, LongArray>()
  /** The ferry ways of the routes, with their [FERRY_TAGS]. */
  private val ferryWayTags = HashMap<Long, Map<String, String>>()
  /** The [FERRY_TAGS] of ferry relations outside the node networks, by their member ways. */
  private val ferryRelationTagsByWayId = HashMap<Long, MutableList<Map<String, String>>>()
  /**
   * The node pairs next to each other on a way of the routes with a `bridge` tag other than `no`,
   * the smaller ID first, see `network-data` "Bridge stretches of courses".
   */
  private val bridgeEdges = HashSet<Pair<Long, Long>>()
  private val knots = NetworkType.entries.associateWith { HashMap<Long, Knot>() }
  /** The boundary relations of the sign regions, by relation ID. */
  private val borderRelations = LinkedHashMap<Long, BorderRelation>()
  private val borderWayNodeIds = HashMap<Long, LongArray>()
  /** The boundary relations of the regions, see `network-data` "Borders between knots". */
  private val regionRelations = LinkedHashMap<Long, Pair<Region, OsmRelation>>()
  /** Villages, towns and cities mapped as points, see `network-data` "Place of each knot". */
  private val placePoints = ArrayList<LocatedPlace>()
  /** Those mapped as closed ways, with their nodes, placed once the nodes are read. */
  private val placeWays = ArrayList<Pair<Pair<com.knoopbyknoop.core.Place, PlaceSize>, LongArray>>()

  private val wantedWayIds by lazy { routes.values.flatMapTo(HashSet()) { it.wayIds } }
  private val wantedBorderWayIds by lazy {
    borderRelations.values.flatMapTo(HashSet()) { it.wayIds } +
      regionRelations.values.flatMap { it.second.memberWayIds }
  }
  private val coordinates by lazy {
    NodeCoordinates(wayNodeIds.values + borderWayNodeIds.values + placeWays.map { it.second })
  }
  /** The nodes next to each other on the ways of each network's routes, without ferries. */
  private val networkNeighbors by lazy {
    NetworkType.entries.associateWith { network ->
      neighborsAlong(
        routes.values
          .filter { it.network == network }
          .flatMapTo(HashSet()) { it.wayIds }
          .filter { it !in ferryWayTags }
          .mapNotNull { wayNodeIds[it] }
      )
    }
  }

  fun acceptRelation(relation: OsmRelation) {
    // A Dutch province or North Rhine-Westphalia is a region and part of a sign region too.
    regionOfBorder(relation.tags)?.let { regionRelations[relation.id] = it to relation }
    signRegionOfBorder(relation.tags)?.let { region ->
      borderRelations[relation.id] =
        BorderRelation(
          relation.id,
          relation.tags["name:en"] ?: relation.tags["name"] ?: region.name,
          region,
          relation.memberWayIds,
        )
      return
    }
    if (relation.tags["type"] != "route") return
    val isFerry = relation.tags["route"] == "ferry"
    if (relation.tags["network:type"] != "node_network") {
      // Ferry lines are often mapped as relations of their own, which carry the times and fees.
      if (isFerry) {
        val tags = ferryTagsOf(relation.tags)
        relation.memberWayIds.forEach {
          ferryRelationTagsByWayId.getOrPut(it) { mutableListOf() }.add(tags)
        }
      }
      return
    }
    val network = NETWORKS_BY_TAG[relation.tags["network"]] ?: return
    routes[relation.id] =
      NetworkRoute(
        relation.id,
        network,
        relation.memberWayIds,
        isFerry,
        if (isFerry) ferryTagsOf(relation.tags) else mapOf(),
        relation.memberRoles,
      )
  }

  fun acceptWay(way: OsmWay) {
    if (way.id in wantedBorderWayIds) borderWayNodeIds[way.id] = way.nodeIds.toLongArray()
    if (way.nodeIds.size >= 4 && way.nodeIds.first() == way.nodeIds.last()) {
      placeOfTags(way.tags)?.let { placeWays += it to way.nodeIds.toLongArray() }
    }
    if (way.id !in wantedWayIds) return
    wayNodeIds[way.id] = way.nodeIds.toLongArray()
    if (way.tags["route"] == "ferry") ferryWayTags[way.id] = ferryTagsOf(way.tags)
    if (way.tags["bridge"].let { it != null && it != "no" }) {
      way.nodeIds.zipWithNext().forEach { (from, to) -> bridgeEdges.add(edgeOf(from, to)) }
    }
  }

  fun acceptNode(node: OsmNode) {
    coordinates.set(node.id, node.latitude, node.longitude)
    placeOfTags(node.tags)?.let { (place, size) ->
      placePoints += LocatedPlace(place, size, node.latitude, node.longitude)
    }
    if (node.tags["network:type"] != "node_network") return
    for (network in NetworkType.entries) {
      val ref = node.tags[network.osmRefTag] ?: continue
      knots.getValue(network)[node.id] = Knot(node.id, ref, network, node.latitude, node.longitude)
    }
  }

  fun result(): ExtractionResult {
    val knotsByNodeId =
      NetworkType.entries.associateWith { network ->
        mergeSameKnots(knots.getValue(network).values, separateNodeIds(network))
      }
    val connections =
      NetworkType.entries.associateWith { HashMap<Pair<Long, Long>, RouteOutcome.Connected>() }
    val skippedRoutes = HashMap<SkipReason, Int>()
    for (route in routes.values) {
      val connected =
        when (val outcome = connectionOf(route, knotsByNodeId.getValue(route.network))) {
          is RouteOutcome.Connected -> outcome
          is RouteOutcome.Skipped -> {
            skippedRoutes.merge(outcome.reason, 1, Int::plus)
            continue
          }
        }
      val connection = connected.connection
      val key =
        minOf(connection.fromId, connection.toId) to maxOf(connection.fromId, connection.toId)
      connections.getValue(route.network).merge(key, connected) { existing, new ->
        if (new.connection.lengthMeters < existing.connection.lengthMeters) new else existing
      }
    }
    val networks = NetworkType.entries.filter { knots.getValue(it).isNotEmpty() }
    val borders = borders()
    val places = PlaceFinder(placePoints + placeAreas())
    val graphs = networks.associateWith {
      val networkKnots =
        knotsByNodeId.getValue(it).values.distinct().map { knot ->
          val placed = knot.copy(place = places.placeAt(knot.latitude, knot.longitude))
          if (it == NetworkType.CYCLING) placed.copy(signRegion = signRegionAt(borders, knot))
          else placed
        }
      val kept = connections.getValue(it).values
      NetworkGraph(
        networkKnots,
        kept.map { connected -> connected.connection },
        kept
          .mapNotNull { connected ->
            connected.ferry?.let { ferry -> connected.connection to ferry }
          }
          .toMap(),
      )
    }
    val courses = networks.associateWith {
      connections.getValue(it).values.associate { kept -> kept.connection to kept.course }
    }
    val bridges = networks.associateWith {
      connections
        .getValue(it)
        .values
        .filter { kept -> kept.bridges.isNotEmpty() }
        .associate { kept -> kept.connection to kept.bridges }
    }
    val coursesBack = networks.associateWith {
      connections
        .getValue(it)
        .values
        .mapNotNull { kept ->
          kept.courseBack?.let { back -> kept.connection to back }
        }
        .toMap()
    }
    val signpostCourses = networks.associateWith {
      connections
        .getValue(it)
        .values
        .filter { kept -> kept.signpostCourses.isNotEmpty() }
        .associate { kept -> kept.connection to kept.signpostCourses }
    }
    val regionBorders = regionBorders()
    val crossings = networks.associateWith {
      connections
        .getValue(it)
        .values
        .mapNotNull { kept ->
          crossingsAlong(kept.course, regionBorders)
            .takeIf { found -> found.isNotEmpty() }
            ?.let { found ->
              kept.connection to found
            }
        }
        .toMap()
    }
    return ExtractionResult(
      graphs,
      courses,
      skippedRoutes,
      signpostCourses,
      bridges,
      coursesBack,
      crossings,
    )
  }

  /**
   * The borders of the regions found in the extracts.
   *
   * @throws IncompleteBorderException if one cannot be closed from the ways in the extracts
   */
  private fun regionBorders(): List<Border<Region>> =
    regionRelations.values.map { (region, relation) ->
      Border.of(
        region,
        relation.id,
        relation.tags["name"] ?: region.key,
        relation.memberWayIds,
        { borderWayNodeIds[it] },
      ) { id ->
        coordinates.pointOf(id)?.let { it.latitude to it.longitude }
      }
    }

  /** The places mapped as closed ways, each at the middle of its outline. */
  private fun placeAreas(): List<LocatedPlace> = placeWays.mapNotNull { (placed, nodeIds) ->
    val ring = nodeIds.map { id ->
      coordinates.pointOf(id)?.let { it.latitude to it.longitude } ?: return@mapNotNull null
    }
    LocatedPlace(
      placed.first,
      placed.second,
      ring.sumOf { it.first } / ring.size,
      ring.sumOf { it.second } / ring.size,
      ring,
    )
  }

  /**
   * The borders of the sign regions found in the extracts.
   *
   * @throws IncompleteBorderException if one cannot be closed from the ways in the extracts
   */
  private fun borders(): List<Border<SignRegion>> =
    borderRelations.values.map { relation ->
      Border.of(relation, { borderWayNodeIds[it] }) { id ->
        coordinates.pointOf(id)?.let { it.latitude to it.longitude }
      }
    }

  /** The first region in [borders] whose border contains [knot]'s own position. */
  private fun signRegionAt(borders: List<Border<SignRegion>>, knot: Knot): SignRegion? =
    SignRegion.entries.firstOrNull { region ->
      borders.any { it.region == region && it.contains(knot.latitude, knot.longitude) }
    }

  /**
   * Pairs of knot nodes with the same ref that a route with a ferry connects, e.g. on both banks of
   * a river. They are different knots, so riders can choose which one to go to. Routes without a
   * ferry between nodes with the same ref are connectors inside one junction.
   */
  private fun separateNodeIds(network: NetworkType): Set<Pair<Long, Long>> {
    val networkKnots = knots.getValue(network)
    return routes.values
      .filter { it.network == network && it.includesFerry }
      .flatMapTo(HashSet()) { route ->
        val knotNodeIds = knotNodeIdsOf(route, networkKnots.keys)
        val refs = knotNodeIds.map { networkKnots.getValue(it).ref }.distinct()
        if (refs.size != 1) return@flatMapTo listOf()
        knotNodeIds.flatMap { first -> knotNodeIds.filter { it != first }.map { first to it } }
      }
  }

  /**
   * Mappers often tag the number of a junction on several nodes, e.g. on both sides of a dual
   * carriageway or around a roundabout. Nodes with the same ref close to each other are one knot,
   * unless they are in [separateNodeIds].
   *
   * @return the merged knot for each node ID, represented by the node with the lowest ID, with the
   *   positions of the other nodes
   */
  private fun mergeSameKnots(
    nodeKnots: Collection<Knot>,
    separateNodeIds: Set<Pair<Long, Long>>,
  ): Map<Long, Knot> {
    val knotsByNodeId = HashMap<Long, Knot>()
    for (sameRef in nodeKnots.groupBy { it.ref }.values) {
      val merged = mutableListOf<MutableList<Knot>>()
      for (knot in sameRef.sortedBy { it.id }) {
        val nearby = merged.firstOrNull { members ->
          val representative = members.first()
          distanceMeters(
            representative.latitude,
            representative.longitude,
            knot.latitude,
            knot.longitude,
          ) <= SAME_KNOT_DISTANCE_METERS && members.none { (it.id to knot.id) in separateNodeIds }
        }
        if (nearby != null) nearby.add(knot) else merged.add(mutableListOf(knot))
      }
      for (members in merged) {
        val representative =
          members
            .first()
            .copy(otherNodes = members.drop(1).map { GeoPoint(it.latitude, it.longitude) })
        members.forEach { knotsByNodeId[it.id] = representative }
      }
    }
    return knotsByNodeId
  }

  private fun knotNodeIdsOf(route: NetworkRoute, knotNodeIds: Set<Long>): Set<Long> =
    route.wayIds
      .mapNotNull { wayNodeIds[it] }
      .flatMapTo(LinkedHashSet()) { nodeIds -> nodeIds.filter { it in knotNodeIds } }

  /**
   * The connection between the two knots on [route], with the shortest distance between them along
   * the route's ways and its course, unless the route does not connect exactly two knots.
   */
  private fun connectionOf(route: NetworkRoute, knotsByNodeId: Map<Long, Knot>): RouteOutcome {
    val ways = route.wayIds.mapNotNull { wayNodeIds[it] }
    val knotNodeIds = knotNodeIdsOf(route, knotsByNodeId.keys)
    val routeKnots = knotNodeIds.map { knotsByNodeId.getValue(it) }.distinct()
    when (routeKnots.size) {
      0 -> return RouteOutcome.Skipped(SkipReason.NO_KNOTS)
      1 -> return RouteOutcome.Skipped(SkipReason.ONE_KNOT)
      2 -> {}
      else -> return RouteOutcome.Skipped(SkipReason.MORE_THAN_TWO_KNOTS)
    }

    val neighbors = neighborsAlong(ways)
    val (start, end) = routeKnots
    val startNodeIds = knotNodeIds.filter { knotsByNodeId[it] == start }
    val endNodeIds = knotNodeIds.filter { knotsByNodeId[it] == end }.toSet()
    // Each direction on the ways the route marks for it, see `network-data` "Course in each riding
    // direction"; without a way in one direction, the other reversed; without either, both ways.
    val directed = directedNeighbors(route)
    val there = shortestPath(directed, startNodeIds, endNodeIds)
    val back = shortestPath(directed, endNodeIds, startNodeIds.toSet())
    val (length, nodeIds) =
      there
        ?: back?.let { (backLength, backNodeIds) -> backLength to backNodeIds.asReversed() }
        ?: shortestPath(neighbors, startNodeIds, endNodeIds)
        ?: return RouteOutcome.Skipped(SkipReason.KNOTS_NOT_CONNECTED)
    val course = courseThrough(route.network, nodeIds, start, end)
    val courseBack =
      back
        ?.takeIf { there != null }
        ?.let { (_, backNodeIds) -> courseThrough(route.network, backNodeIds, end, start) }
        ?.takeIf {
          it.map { (_, point) -> point } != course.map { (_, point) -> point }.asReversed()
        }
    return RouteOutcome.Connected(
      Connection(start.id, end.id, length.roundToInt()),
      course.map { it.second },
      if (route.includesFerry) ferryOf(route) else null,
      signpostCourses(neighbors, directed, start, startNodeIds, end, endNodeIds),
      bridgesAlong(course.map { it.first }),
      courseBack?.let { points ->
        points.map { it.second } to bridgesAlong(points.map { it.first })
      },
    )
  }

  /**
   * The course along [nodeIds] from [from] to [to], with the node ID of each point. A merged
   * junction has its position at its representative node, so the course is extended to it, see
   * `network-data` "Course of every connection". The node IDs go along, to find bridges.
   */
  private fun courseThrough(
    network: NetworkType,
    nodeIds: List<Long>,
    from: Knot,
    to: Knot,
  ): List<Pair<Long, GeoPoint>> {
    val points = buildList {
      add(from.id to GeoPoint(from.latitude, from.longitude))
      addAll(wayToPosition(network, nodeIds.first(), from).asReversed())
      addAll(nodeIds.mapNotNull { id -> coordinates.pointOf(id)?.let { id to it } })
      addAll(wayToPosition(network, nodeIds.last(), to))
      add(to.id to GeoPoint(to.latitude, to.longitude))
    }
    return points.filterIndexed { index, (_, point) ->
      index == 0 || point != points[index - 1].second
    }
  }

  /**
   * The nodes next to each other on [route]'s ways in the directions the route uses them: a way
   * with the role `forward` only in its own direction, `backward` only against it, others both.
   */
  private fun directedNeighbors(route: NetworkRoute): Map<Long, List<Pair<Long, Double>>> {
    val neighbors = HashMap<Long, MutableList<Pair<Long, Double>>>()
    for ((index, wayId) in route.wayIds.withIndex()) {
      val nodeIds = wayNodeIds[wayId] ?: continue
      val role = route.roles.getOrNull(index).orEmpty()
      for (node in 1 until nodeIds.size) {
        val from = nodeIds[node - 1]
        val to = nodeIds[node]
        val distance = coordinates.distanceMeters(from, to) ?: continue
        if (role != "backward") neighbors.getOrPut(from) { mutableListOf() }.add(to to distance)
        if (role != "forward") neighbors.getOrPut(to) { mutableListOf() }.add(from to distance)
      }
    }
    return neighbors
  }

  /**
   * The courses between each node of [start] and each node of [end] that the route reaches without
   * passing another of them; none if there is just one, see `network-data` "Courses to each
   * signpost".
   */
  private fun signpostCourses(
    neighbors: Map<Long, List<Pair<Long, Double>>>,
    directed: Map<Long, List<Pair<Long, Double>>>,
    start: Knot,
    startNodeIds: Collection<Long>,
    end: Knot,
    endNodeIds: Set<Long>,
  ): List<SignpostCourse> {
    if (startNodeIds.size == 1 && endNodeIds.size == 1) return listOf()
    val knotNodeIds = (startNodeIds + endNodeIds).toSet()
    fun path(ways: Map<Long, List<Pair<Long, Double>>>, from: Long, to: Long) =
      shortestPath(ways, listOf(from), setOf(to), avoid = knotNodeIds - from - to)?.second
    fun located(nodeIds: List<Long>) = nodeIds.mapNotNull { id ->
      coordinates.pointOf(id)?.let { id to it }
    }
    val courses = startNodeIds.flatMap { from ->
      endNodeIds.mapNotNull { to ->
        // In each riding direction like the connection's course, see `network-data` "Courses to
        // each signpost in each direction".
        val there = path(directed, from, to)
        val back = path(directed, to, from)
        val nodeIds =
          there ?: back?.asReversed() ?: path(neighbors, from, to) ?: return@mapNotNull null
        val fromPosition = start.positions.indexOf(coordinates.pointOf(from))
        val toPosition = end.positions.indexOf(coordinates.pointOf(to))
        if (fromPosition < 0 || toPosition < 0) return@mapNotNull null
        val course = located(nodeIds)
        val courseBack =
          back
            ?.takeIf { there != null }
            ?.let(::located)
            ?.takeIf {
              it.map { (_, point) -> point } != course.map { (_, point) -> point }.asReversed()
            }
        SignpostCourse(
          fromPosition,
          toPosition,
          course.map { it.second },
          bridgesAlong(course.map { it.first }),
          courseBack?.let { points ->
            CourseBack(points.map { it.second }, bridgesAlong(points.map { it.first }))
          },
          // Unlike a connection's course, a signpost course without a way in one direction is not
          // used in that direction: the knot's other signposts are.
          when {
            there != null && back == null -> OneWay.THERE
            there == null && back != null -> OneWay.BACK
            else -> null
          },
        )
      }
    }
    return if (courses.size >= 2) courses else listOf()
  }

  /**
   * The ferry details of a route that includes a ferry, see `network-data` "Ferry details". Each
   * detail comes from the first ferry way of the route that has it, otherwise from the route itself
   * if it is a ferry route, otherwise from a ferry relation the ferry way is a member of.
   */
  private fun ferryOf(route: NetworkRoute): Ferry {
    val ferryWayIds = route.wayIds.filter { it in ferryWayTags }
    val sources =
      ferryWayIds.map { ferryWayTags.getValue(it) } +
        listOf(route.ferryTags) +
        ferryWayIds.flatMap { ferryRelationTagsByWayId[it].orEmpty() }
    fun detail(key: String) = sources.firstNotNullOfOrNull { it[key] }
    return Ferry(
      name = detail("name"),
      operator = detail("operator"),
      openingHours = detail("opening_hours"),
      seasonal = detail("seasonal"),
      fee = detail("fee"),
      charge = detail("charge"),
      website = detail("website"),
    )
  }

  private val NetworkRoute.includesFerry: Boolean
    get() = isFerry || wayIds.any { it in ferryWayTags }

  /** The shortest path from any of [startNodeIds] to any of [endNodeIds], with its node IDs. */
  /**
   * The points after [nodeId] to the position of [knot], along the ways of the network without
   * ferries; empty where no such way leads there without a detour, so the course goes straight.
   */
  private fun wayToPosition(
    network: NetworkType,
    nodeId: Long,
    knot: Knot,
  ): List<Pair<Long, GeoPoint>> {
    if (nodeId == knot.id) return listOf()
    val straight = coordinates.distanceMeters(nodeId, knot.id) ?: return listOf()
    val (_, path) =
      shortestPath(
        networkNeighbors.getValue(network),
        listOf(nodeId),
        setOf(knot.id),
        maximumDistance = straight * MAX_DETOUR_TO_POSITION,
      ) ?: return listOf()
    return path.drop(1).mapNotNull { id -> coordinates.pointOf(id)?.let { id to it } }
  }

  /**
   * The stretches along a course through [nodeIds] whose segments are on a bridge; consecutive
   * bridge ways form one stretch, see `network-data` "Several ways of one bridge".
   */
  private fun bridgesAlong(nodeIds: List<Long>): List<Stretch> {
    val stretches = mutableListOf<Stretch>()
    var first = -1
    for (index in 0 until nodeIds.lastIndex) {
      val onBridge = edgeOf(nodeIds[index], nodeIds[index + 1]) in bridgeEdges
      if (onBridge && first < 0) first = index
      if (!onBridge && first >= 0) {
        stretches += Stretch(first, index)
        first = -1
      }
    }
    if (first >= 0) stretches += Stretch(first, nodeIds.lastIndex)
    return stretches
  }

  private fun edgeOf(from: Long, to: Long) = minOf(from, to) to maxOf(from, to)

  private fun neighborsAlong(ways: Iterable<LongArray>): Map<Long, List<Pair<Long, Double>>> {
    val neighbors = HashMap<Long, MutableList<Pair<Long, Double>>>()
    for (nodeIds in ways) {
      for (index in 1 until nodeIds.size) {
        val from = nodeIds[index - 1]
        val to = nodeIds[index]
        val distance = coordinates.distanceMeters(from, to) ?: continue
        neighbors.getOrPut(from) { mutableListOf() }.add(to to distance)
        neighbors.getOrPut(to) { mutableListOf() }.add(from to distance)
      }
    }
    return neighbors
  }

  private fun shortestPath(
    neighbors: Map<Long, List<Pair<Long, Double>>>,
    startNodeIds: Collection<Long>,
    endNodeIds: Set<Long>,
    maximumDistance: Double = Double.MAX_VALUE,
    avoid: Set<Long> = setOf(),
  ): Pair<Double, List<Long>>? {
    val distances = startNodeIds.associateWithTo(HashMap()) { 0.0 }
    val predecessors = HashMap<Long, Long>()
    val queue = PriorityQueue<Pair<Double, Long>>(compareBy { it.first })
    startNodeIds.forEach { queue.add(0.0 to it) }
    while (queue.isNotEmpty()) {
      val (distance, nodeId) = queue.poll()
      if (nodeId in endNodeIds) {
        return distance to generateSequence(nodeId) { predecessors[it] }.toList().asReversed()
      }
      if (distance > distances.getValue(nodeId)) continue
      for ((neighborId, length) in neighbors[nodeId].orEmpty()) {
        val neighborDistance = distance + length
        if (neighborDistance > maximumDistance || neighborId in avoid) continue
        if (neighborDistance < (distances[neighborId] ?: Double.MAX_VALUE)) {
          distances[neighborId] = neighborDistance
          predecessors[neighborId] = nodeId
          queue.add(neighborDistance to neighborId)
        }
      }
    }
    return null
  }

  private sealed interface RouteOutcome {
    /** @property course the path from the connection's `from` to its `to` knot, not simplified */
    data class Connected(
      val connection: Connection,
      val course: List<GeoPoint>,
      val ferry: Ferry?,
      val signpostCourses: List<SignpostCourse>,
      /** The stretches of [course] on a bridge. */
      val bridges: List<Stretch>,
      /**
       * The course from `to` back to `from` with its bridge stretches, if not [course] reversed.
       */
      val courseBack: Pair<List<GeoPoint>, List<Stretch>>? = null,
    ) : RouteOutcome

    data class Skipped(val reason: SkipReason) : RouteOutcome
  }

  private data class NetworkRoute(
    val relationId: Long,
    val network: NetworkType,
    val wayIds: List<Long>,
    val isFerry: Boolean,
    /** The route's own [FERRY_TAGS] if it is a ferry route. */
    val ferryTags: Map<String, String>,
    /** The role of each of [wayIds], e.g. `forward`. */
    val roles: List<String> = listOf(),
  )

  companion object {
    /** The tags kept of ferry ways and relations, see [ferryOf]. */
    private val FERRY_TAGS =
      setOf("name", "operator", "opening_hours", "seasonal", "fee", "charge", "website")

    private fun ferryTagsOf(tags: Map<String, String>) = tags.filter { (key, value) ->
      key in FERRY_TAGS && value.isNotBlank()
    }

    private val NETWORKS_BY_TAG = mapOf("rcn" to NetworkType.CYCLING, "rwn" to NetworkType.HIKING)

    /**
     * Nodes with the same ref within this distance are one knot, see [mergeSameKnots]. In the Dutch
     * extract, nodes of one junction are up to about 500 m apart, e.g. at wide junctions.
     */
    const val SAME_KNOT_DISTANCE_METERS = 500.0

    /** Longer than this many times the straight distance, a way is a detour, not the junction. */
    const val MAX_DETOUR_TO_POSITION = 3.0
  }
}

/**
 * @property graphs the extracted network per network type, only for types found in the extract
 * @property courses per network type, the unsimplified course of each connection of its graph
 * @property skippedRoutes the number of node network routes that could not become a connection
 */
data class ExtractionResult(
  val graphs: Map<NetworkType, NetworkGraph>,
  val courses: Map<NetworkType, Map<Connection, List<GeoPoint>>>,
  val skippedRoutes: Map<SkipReason, Int>,
  val signpostCourses: Map<NetworkType, Map<Connection, List<SignpostCourse>>> = mapOf(),
  /** Per network type, the stretches of each connection's course on a bridge, if it has any. */
  val bridges: Map<NetworkType, Map<Connection, List<Stretch>>> = mapOf(),
  /**
   * Per network type, the course from `to` back to `from` with its bridge stretches, for each
   * connection where it is not the course reversed, unsimplified.
   */
  val coursesBack: Map<NetworkType, Map<Connection, Pair<List<GeoPoint>, List<Stretch>>>> = mapOf(),
  /** Per network type, where each connection's course crosses a border, if it does. */
  val crossings: Map<NetworkType, Map<Connection, List<Crossing>>> = mapOf(),
)

/** Why a node network route could not become a connection. */
enum class SkipReason {
  /** None of the route's ways is in the extract, or none passes a knot. */
  NO_KNOTS,
  /** Typically a route crossing the border of the extract. */
  ONE_KNOT,
  /** The route bundles several node-to-node routes, or has knots in the middle. */
  MORE_THAN_TWO_KNOTS,
  /** The route's ways have a gap between its two knots. */
  KNOTS_NOT_CONNECTED,
}

/**
 * Coordinates of a fixed set of nodes, in primitive arrays to keep millions of nodes affordable.
 */
private class NodeCoordinates(nodeIds: Collection<LongArray>) {

  private val ids =
    nodeIds.flatMap { it.asIterable() }.toLongArray().apply { sort() }.distinctSorted()
  private val latitudes = DoubleArray(ids.size) { Double.NaN }
  private val longitudes = DoubleArray(ids.size) { Double.NaN }

  fun set(id: Long, latitude: Double, longitude: Double) {
    val index = ids.binarySearch(id)
    if (index < 0) return
    latitudes[index] = latitude
    longitudes[index] = longitude
  }

  fun pointOf(id: Long): GeoPoint? {
    val index = ids.binarySearch(id).takeIf { it >= 0 && !latitudes[it].isNaN() } ?: return null
    return GeoPoint(latitudes[index], longitudes[index])
  }

  /** The great-circle distance between two nodes, or `null` if a node's coordinates are unknown. */
  fun distanceMeters(fromId: Long, toId: Long): Double? {
    val from = ids.binarySearch(fromId).takeIf { it >= 0 && !latitudes[it].isNaN() } ?: return null
    val to = ids.binarySearch(toId).takeIf { it >= 0 && !latitudes[it].isNaN() } ?: return null
    return distanceMeters(latitudes[from], longitudes[from], latitudes[to], longitudes[to])
  }

  private fun LongArray.distinctSorted(): LongArray {
    if (isEmpty()) return this
    var count = 1
    for (index in 1 until size) if (this[index] != this[count - 1]) this[count++] = this[index]
    return copyOf(count)
  }
}
