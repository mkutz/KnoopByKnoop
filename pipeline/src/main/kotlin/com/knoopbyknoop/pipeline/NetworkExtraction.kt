package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkType
import java.util.PriorityQueue
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Extracts node networks from OpenStreetMap elements.
 *
 * Feed it all relations first, then all ways, then all nodes, like they are ordered in PBF files.
 * It keeps only the elements it needs: node network routes, their ways, and the nodes on them.
 */
class NetworkExtraction {

  private val routes = mutableListOf<NetworkRoute>()
  private val wayNodeIds = HashMap<Long, LongArray>()
  private val knots = NetworkType.entries.associateWith { HashMap<Long, Knot>() }

  private val wantedWayIds by lazy { routes.flatMapTo(HashSet()) { it.wayIds } }
  private val coordinates by lazy { NodeCoordinates(wayNodeIds.values) }

  fun acceptRelation(relation: OsmRelation) {
    if (relation.tags["type"] != "route") return
    if (relation.tags["network:type"] != "node_network") return
    val network = NETWORKS_BY_TAG[relation.tags["network"]] ?: return
    routes.add(NetworkRoute(relation.id, network, relation.memberWayIds))
  }

  fun acceptWay(way: OsmWay) {
    if (way.id in wantedWayIds) wayNodeIds[way.id] = way.nodeIds.toLongArray()
  }

  fun acceptNode(node: OsmNode) {
    coordinates.set(node.id, node.latitude, node.longitude)
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
    val connections = NetworkType.entries.associateWith { HashMap<Pair<Long, Long>, Connection>() }
    val skippedRoutes = HashMap<SkipReason, Int>()
    for (route in routes) {
      val connection =
        when (val outcome = connectionOf(route, knotsByNodeId.getValue(route.network))) {
          is RouteOutcome.Connected -> outcome.connection
          is RouteOutcome.Skipped -> {
            skippedRoutes.merge(outcome.reason, 1, Int::plus)
            continue
          }
        }
      val key =
        minOf(connection.fromId, connection.toId) to maxOf(connection.fromId, connection.toId)
      connections.getValue(route.network).merge(key, connection) { existing, new ->
        if (new.lengthMeters < existing.lengthMeters) new else existing
      }
    }
    val graphs =
      NetworkType.entries
        .filter { knots.getValue(it).isNotEmpty() }
        .associateWith {
          val networkKnots = knotsByNodeId.getValue(it).values.distinct()
          NetworkGraph(networkKnots, connections.getValue(it).values)
        }
    return ExtractionResult(graphs, skippedRoutes)
  }

  /**
   * Pairs of knot nodes with the same ref that a route of their own connects, e.g. a ferry between
   * both banks of a river. They are different knots, so riders can choose which one to go to.
   */
  private fun separateNodeIds(network: NetworkType): Set<Pair<Long, Long>> {
    val networkKnots = knots.getValue(network)
    return routes
      .filter { it.network == network }
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
   * @return the merged knot for each node ID, represented by the node with the lowest ID
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
          haversineMeters(
            representative.latitude,
            representative.longitude,
            knot.latitude,
            knot.longitude,
          ) <= SAME_KNOT_DISTANCE_METERS && members.none { (it.id to knot.id) in separateNodeIds }
        }
        if (nearby != null) nearby.add(knot) else merged.add(mutableListOf(knot))
        knotsByNodeId[knot.id] = (nearby ?: merged.last()).first()
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
   * the route's ways, unless the route does not connect exactly two knots.
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
    val (start, end) = routeKnots
    val startNodeIds = knotNodeIds.filter { knotsByNodeId[it] == start }
    val endNodeIds = knotNodeIds.filter { knotsByNodeId[it] == end }.toSet()
    val length =
      shortestDistance(neighbors, startNodeIds, endNodeIds)
        ?: return RouteOutcome.Skipped(SkipReason.KNOTS_NOT_CONNECTED)
    return RouteOutcome.Connected(Connection(start.id, end.id, length.roundToInt()))
  }

  /** The shortest distance from any of [startNodeIds] to any of [endNodeIds]. */
  private fun shortestDistance(
    neighbors: Map<Long, List<Pair<Long, Double>>>,
    startNodeIds: Collection<Long>,
    endNodeIds: Set<Long>,
  ): Double? {
    val distances = startNodeIds.associateWithTo(HashMap()) { 0.0 }
    val queue = PriorityQueue<Pair<Double, Long>>(compareBy { it.first })
    startNodeIds.forEach { queue.add(0.0 to it) }
    while (queue.isNotEmpty()) {
      val (distance, nodeId) = queue.poll()
      if (nodeId in endNodeIds) return distance
      if (distance > distances.getValue(nodeId)) continue
      for ((neighborId, length) in neighbors[nodeId].orEmpty()) {
        val neighborDistance = distance + length
        if (neighborDistance < (distances[neighborId] ?: Double.MAX_VALUE)) {
          distances[neighborId] = neighborDistance
          queue.add(neighborDistance to neighborId)
        }
      }
    }
    return null
  }

  private sealed interface RouteOutcome {
    data class Connected(val connection: Connection) : RouteOutcome

    data class Skipped(val reason: SkipReason) : RouteOutcome
  }

  private data class NetworkRoute(
    val relationId: Long,
    val network: NetworkType,
    val wayIds: List<Long>,
  )

  companion object {
    private val NETWORKS_BY_TAG = mapOf("rcn" to NetworkType.CYCLING, "rwn" to NetworkType.HIKING)

    /** Nodes with the same ref within this distance are one knot, see [mergeSameKnots]. */
    const val SAME_KNOT_DISTANCE_METERS = 100.0
  }
}

/**
 * @property graphs the extracted network per network type, only for types found in the extract
 * @property skippedRoutes the number of node network routes that could not become a connection
 */
data class ExtractionResult(
  val graphs: Map<NetworkType, NetworkGraph>,
  val skippedRoutes: Map<SkipReason, Int>,
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

  /** The great-circle distance between two nodes, or `null` if a node's coordinates are unknown. */
  fun distanceMeters(fromId: Long, toId: Long): Double? {
    val from = ids.binarySearch(fromId).takeIf { it >= 0 && !latitudes[it].isNaN() } ?: return null
    val to = ids.binarySearch(toId).takeIf { it >= 0 && !latitudes[it].isNaN() } ?: return null
    return haversineMeters(latitudes[from], longitudes[from], latitudes[to], longitudes[to])
  }

  private fun LongArray.distinctSorted(): LongArray {
    if (isEmpty()) return this
    var count = 1
    for (index in 1 until size) if (this[index] != this[count - 1]) this[count++] = this[index]
    return copyOf(count)
  }
}

internal fun haversineMeters(
  fromLatitude: Double,
  fromLongitude: Double,
  toLatitude: Double,
  toLongitude: Double,
): Double {
  val deltaLatitude = Math.toRadians(toLatitude - fromLatitude)
  val deltaLongitude = Math.toRadians(toLongitude - fromLongitude)
  val a =
    sin(deltaLatitude / 2).let { it * it } +
      cos(Math.toRadians(fromLatitude)) *
        cos(Math.toRadians(toLatitude)) *
        sin(deltaLongitude / 2).let { it * it }
  return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

private const val EARTH_RADIUS_METERS = 6_371_008.8
