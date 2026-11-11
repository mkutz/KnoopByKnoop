package com.knoopbyknoop.core

/**
 * A signposted route between two adjacent knots. Connections can be ridden in both directions.
 *
 * @property fromId the [Knot.id] of one end
 * @property toId the [Knot.id] of the other end
 * @property lengthMeters the length of the connection
 */
data class Connection(val fromId: Long, val toId: Long, val lengthMeters: Int) {
  init {
    require(lengthMeters >= 0) { "Connection length must not be negative, was $lengthMeters" }
  }
}

/**
 * What OpenStreetMap says about a ferry, as tagged, see `network-data` "Ferry details". `null`
 * where OpenStreetMap has nothing.
 *
 * @property openingHours the `opening_hours` tag, e.g. `Apr-Oct: Mo-Su 07:00-19:00`
 * @property seasonal the `seasonal` tag, e.g. `summer` or `yes`
 * @property fee the `fee` tag, e.g. `yes` or `no`
 * @property charge the `charge` tag, e.g. `1.50 EUR`
 */
data class Ferry(
  val name: String? = null,
  val operator: String? = null,
  val openingHours: String? = null,
  val seasonal: String? = null,
  val fee: String? = null,
  val charge: String? = null,
  val website: String? = null,
)

/**
 * An immutable node network: its knots and the connections between them.
 *
 * A graph made by [withRoutePoints] adds route points to an installed graph, sharing its indexes.
 *
 * @param ferries the connections that include a ferry, with its details
 */
class NetworkGraph
private constructor(
  /** All knots of this network by their [Knot.id]. */
  val knots: Map<Long, Knot>,
  /** All connections between the knots of this network. */
  val connections: List<Connection>,
  /** The connections that include a ferry, with its details. */
  val ferries: Map<Connection, Ferry>,
  /**
   * Where connections cross a border, from their `from` knot, see `network-data` "Borders between
   * knots".
   */
  val crossings: Map<Connection, List<Crossing>>,
  /** The type shared by all knots of this network, or `null` for an empty network. */
  val network: NetworkType?,
  private val connectionsByKnotId: Map<Long, List<Connection>>,
  private val ferriesByKnotIds: Map<Pair<Long, Long>, Ferry>,
  /** The installed graph this one adds route points to, see [withRoutePoints]. */
  private val base: NetworkGraph?,
) {

  constructor(
    knots: Collection<Knot>,
    connections: Collection<Connection>,
    ferries: Map<Connection, Ferry> = mapOf(),
    crossings: Map<Connection, List<Crossing>> = mapOf(),
  ) : this(
    knots = knots.associateBy { it.id },
    connections = connections.toList(),
    ferries = ferries.toMap(),
    crossings = crossings.toMap(),
    network = knots.firstOrNull()?.network,
    connectionsByKnotId =
      connections
        .flatMap { listOf(it.fromId to it, it.toId to it) }
        .groupBy({ it.first }, { it.second }),
    ferriesByKnotIds =
      ferries.entries.associate { (connection, ferry) -> knotIdsOf(connection) to ferry },
    base = null,
  ) {
    require(knots.all { it.network == network }) { "All knots must belong to the same network" }
    connections.forEach { connection ->
      require(connection.fromId in this.knots && connection.toId in this.knots) {
        "Connection $connection references an unknown knot"
      }
    }
    val connectionSet = this.connections.toSet()
    ferries.keys.forEach { connection ->
      require(connection in connectionSet) { "Ferry on $connection, which is not a connection" }
    }
  }

  /**
   * The installed graph without route points: this one, or the one [withRoutePoints] was called on.
   */
  val installed: NetworkGraph
    get() = base ?: this

  operator fun contains(knot: Knot): Boolean = knots[knot.id] == knot

  /** The ferry on the connection between [from] and [to], in either direction, if it has one. */
  fun ferryBetween(from: Knot, to: Knot): Ferry? =
    ferriesByKnotIds[knotIdsOf(Connection(from.id, to.id, 0))]

  /**
   * The borders the leg from [from] to [to] crosses, in riding direction, see `network-data`
   * "Borders between knots": crossings less than [MERGE_METERS] apart merge into one, and a border
   * crossed back to where it was gives none.
   */
  fun crossingsBetween(from: Knot, to: Knot): List<Crossing> {
    val connection = connectionBetween(from.id, to.id) ?: return listOf()
    val found = crossings[connection] ?: return listOf()
    val riding =
      if (connection.fromId == from.id) found
      else
        found.asReversed().map {
          Crossing(connection.lengthMeters - it.atMeters, from = it.to, to = it.from)
        }
    // A chain of crossings each less than MERGE_METERS after the one before is one.
    val merged = mutableListOf<Crossing>()
    var previous: Crossing? = null
    for (crossing in riding) {
      val last = merged.lastOrNull()
      if (
        last != null && previous != null && crossing.atMeters - previous.atMeters < MERGE_METERS
      ) {
        merged[merged.lastIndex] = Crossing(last.atMeters, last.from, crossing.to)
      } else {
        merged += crossing
      }
      previous = crossing
    }
    return merged.filter { it.from != it.to }
  }

  /** The connection between the knots with IDs [fromId] and [toId], in either direction. */
  fun connectionBetween(fromId: Long, toId: Long): Connection? =
    connectionsOf(fromId).firstOrNull {
      (it.fromId == fromId && it.toId == toId) || (it.fromId == toId && it.toId == fromId)
    }

  private fun connectionsOf(knotId: Long): List<Connection> =
    base?.connectionsOf(knotId).orEmpty() + connectionsByKnotId[knotId].orEmpty()

  /** The neighbors of the given knot with the length of the connection to each. */
  fun neighborsOf(knot: Knot): List<Pair<Knot, Int>> =
    connectionsOf(knot.id).map { connection ->
      val neighborId = if (connection.fromId == knot.id) connection.toId else connection.fromId
      knots.getValue(neighborId) to connection.lengthMeters
    }

  /**
   * This graph with [points] joined to the knots of their connections, and to each other where they
   * share one, see `route-planning` "Route from waypoints". The installed graph's indexes are
   * shared, so this is cheap to call on every edit. A point whose connection is not in this graph
   * is added without connections.
   */
  fun withRoutePoints(points: Collection<Knot>): NetworkGraph {
    val installed = installed
    val distinct = points.filter { it.isRoutePoint }.distinctBy { it.id }
    if (distinct.isEmpty()) return installed
    val added = routePointConnections(installed, distinct)
    return NetworkGraph(
      knots = OverlayMap(installed.knots, distinct.associateBy { it.id }),
      connections = installed.connections + added,
      ferries = installed.ferries,
      crossings = installed.crossings,
      network = installed.network,
      connectionsByKnotId =
        added
          .flatMap { listOf(it.fromId to it, it.toId to it) }
          .groupBy({ it.first }, { it.second }),
      ferriesByKnotIds = installed.ferriesByKnotIds,
      base = installed,
    )
  }

  private companion object {
    /** Crossings closer than this on one connection are one, e.g. a path along a border. */
    const val MERGE_METERS = 500

    fun knotIdsOf(connection: Connection) =
      minOf(connection.fromId, connection.toId) to maxOf(connection.fromId, connection.toId)
  }
}

/** [base] with [extra] added, without copying [base]. */
internal class OverlayMap<K, V>(private val base: Map<K, V>, private val extra: Map<K, V>) :
  AbstractMap<K, V>() {

  override val entries: Set<Map.Entry<K, V>> by lazy { (base + extra).entries }

  override fun get(key: K): V? = extra[key] ?: base[key]

  override fun containsKey(key: K): Boolean = key in extra || key in base

  override val size: Int
    get() = base.size + extra.count { it.key !in base }
}
