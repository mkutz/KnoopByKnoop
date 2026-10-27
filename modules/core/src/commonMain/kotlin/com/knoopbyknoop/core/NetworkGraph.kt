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
 * @param ferries the connections that include a ferry, with its details
 */
class NetworkGraph(
  knots: Collection<Knot>,
  connections: Collection<Connection>,
  ferries: Map<Connection, Ferry> = mapOf(),
) {

  /** All knots of this network by their [Knot.id]. */
  val knots: Map<Long, Knot> = knots.associateBy { it.id }

  /** All connections between the knots of this network. */
  val connections: List<Connection> = connections.toList()

  /** The connections that include a ferry, with its details. */
  val ferries: Map<Connection, Ferry> = ferries.toMap()

  /** The type shared by all knots of this network, or `null` for an empty network. */
  val network: NetworkType? = knots.firstOrNull()?.network

  private val connectionsByKnotId: Map<Long, List<Connection>>

  private val ferriesByKnotIds: Map<Pair<Long, Long>, Ferry> =
    ferries.entries.associate { (connection, ferry) -> knotIdsOf(connection) to ferry }

  init {
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
    connectionsByKnotId =
      connections
        .flatMap { listOf(it.fromId to it, it.toId to it) }
        .groupBy({ it.first }, { it.second })
  }

  operator fun contains(knot: Knot): Boolean = knots[knot.id] == knot

  /** The ferry on the connection between [from] and [to], in either direction, if it has one. */
  fun ferryBetween(from: Knot, to: Knot): Ferry? =
    ferriesByKnotIds[knotIdsOf(Connection(from.id, to.id, 0))]

  private fun knotIdsOf(connection: Connection) =
    minOf(connection.fromId, connection.toId) to maxOf(connection.fromId, connection.toId)

  /** The neighbors of the given knot with the length of the connection to each. */
  fun neighborsOf(knot: Knot): List<Pair<Knot, Int>> =
    connectionsByKnotId[knot.id].orEmpty().map { connection ->
      val neighborId = if (connection.fromId == knot.id) connection.toId else connection.fromId
      knots.getValue(neighborId) to connection.lengthMeters
    }
}
