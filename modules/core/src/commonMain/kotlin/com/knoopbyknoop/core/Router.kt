package com.knoopbyknoop.core

/**
 * A sequence of adjacent knots.
 *
 * @property knots the knots in riding order, starting and ending with the path's end points
 * @property lengthMeters the sum of the connection lengths along the path
 */
data class Path(val knots: List<Knot>, val lengthMeters: Int) {
  init {
    require(knots.isNotEmpty()) { "A path needs at least one knot" }
  }

  val start: Knot
    get() = knots.first()

  val end: Knot
    get() = knots.last()
}

/** Finds paths between two knots of a network. */
fun interface Router {

  /** @return the path from [from] to [to], or `null` if [to] cannot be reached from [from] */
  fun shortestPath(from: Knot, to: Knot): Path?
}

/**
 * A [Router] finding the shortest path over a [NetworkGraph] (Dijkstra).
 *
 * @param avoided connections left out, as pairs of knot IDs, the smaller first, e.g. closed
 *   ferries, see `knot-navigation` "Riding around a closed ferry"
 */
class ShortestPathRouter(
  private val graph: NetworkGraph,
  private val avoided: Set<Pair<Long, Long>> = setOf(),
) : Router {

  override fun shortestPath(from: Knot, to: Knot): Path? {
    if (from !in graph || to !in graph) return null

    val distances = mutableMapOf(from.id to 0)
    val predecessors = mutableMapOf<Long, Knot>()
    val settled = mutableSetOf<Long>()
    val queue = MinHeap<Knot>().apply { add(0.0, from) }

    while (queue.isNotEmpty()) {
      val entry = queue.poll() ?: break
      val distance = entry.priority.toInt()
      val knot = entry.value
      if (!settled.add(knot.id)) continue
      if (knot.id == to.id) break
      // Routes don't pass through a route point, only start or end there, see `route-planning`
      // "Passing a route point that is not on this leg".
      if (knot.isRoutePoint && knot.id != from.id) continue
      for ((neighbor, lengthMeters) in graph.neighborsOf(knot)) {
        if (avoided.isNotEmpty() && pairOf(knot.id, neighbor.id) in avoided) continue
        val neighborDistance = distance + lengthMeters
        if (neighborDistance < (distances[neighbor.id] ?: Int.MAX_VALUE)) {
          distances[neighbor.id] = neighborDistance
          predecessors[neighbor.id] = knot
          queue.add(neighborDistance.toDouble(), neighbor)
        }
      }
    }

    val lengthMeters = distances[to.id] ?: return null
    val knots = generateSequence(to) { predecessors[it.id] }.toList().asReversed()
    return Path(knots, lengthMeters)
  }
}

/** The knot IDs [first] and [second] as an avoided connection, the smaller first. */
fun pairOf(first: Long, second: Long): Pair<Long, Long> =
  minOf(first, second) to maxOf(first, second)
