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

/** An immutable node network: its knots and the connections between them. */
class NetworkGraph(knots: Collection<Knot>, connections: Collection<Connection>) {

  /** All knots of this network by their [Knot.id]. */
  val knots: Map<Long, Knot> = knots.associateBy { it.id }

  /** All connections between the knots of this network. */
  val connections: List<Connection> = connections.toList()

  /** The type shared by all knots of this network, or `null` for an empty network. */
  val network: NetworkType? = knots.firstOrNull()?.network

  private val connectionsByKnotId: Map<Long, List<Connection>>

  init {
    require(knots.all { it.network == network }) { "All knots must belong to the same network" }
    connections.forEach { connection ->
      require(connection.fromId in this.knots && connection.toId in this.knots) {
        "Connection $connection references an unknown knot"
      }
    }
    connectionsByKnotId =
      connections
        .flatMap { listOf(it.fromId to it, it.toId to it) }
        .groupBy({ it.first }, { it.second })
  }

  operator fun contains(knot: Knot): Boolean = knots[knot.id] == knot

  /** The neighbors of the given knot with the length of the connection to each. */
  fun neighborsOf(knot: Knot): List<Pair<Knot, Int>> =
    connectionsByKnotId[knot.id].orEmpty().map { connection ->
      val neighborId = if (connection.fromId == knot.id) connection.toId else connection.fromId
      knots.getValue(neighborId) to connection.lengthMeters
    }
}
