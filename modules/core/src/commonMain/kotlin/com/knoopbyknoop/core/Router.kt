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

/** A [Router] finding the shortest path over a [NetworkGraph] (Dijkstra). */
class ShortestPathRouter(private val graph: NetworkGraph) : Router {

  override fun shortestPath(from: Knot, to: Knot): Path? {
    if (from !in graph || to !in graph) return null

    val distances = mutableMapOf(from.id to 0)
    val predecessors = mutableMapOf<Long, Knot>()
    val settled = mutableSetOf<Long>()
    val queue = MinHeap<Knot>().apply { add(0, from) }

    while (queue.isNotEmpty()) {
      val (distance, knot) = queue.removeMin()
      if (!settled.add(knot.id)) continue
      if (knot.id == to.id) break
      for ((neighbor, lengthMeters) in graph.neighborsOf(knot)) {
        val neighborDistance = distance + lengthMeters
        if (neighborDistance < (distances[neighbor.id] ?: Int.MAX_VALUE)) {
          distances[neighbor.id] = neighborDistance
          predecessors[neighbor.id] = knot
          queue.add(neighborDistance, neighbor)
        }
      }
    }

    val lengthMeters = distances[to.id] ?: return null
    val knots = generateSequence(to) { predecessors[it.id] }.toList().asReversed()
    return Path(knots, lengthMeters)
  }
}

/** A minimal binary min-heap, as the common standard library has no priority queue. */
private class MinHeap<T> {

  private val entries = mutableListOf<Pair<Int, T>>()

  fun isNotEmpty() = entries.isNotEmpty()

  fun add(priority: Int, value: T) {
    entries.add(priority to value)
    var index = entries.lastIndex
    while (index > 0) {
      val parent = (index - 1) / 2
      if (entries[parent].first <= entries[index].first) break
      swap(index, parent)
      index = parent
    }
  }

  fun removeMin(): Pair<Int, T> {
    val min = entries.first()
    val last = entries.removeAt(entries.lastIndex)
    if (entries.isNotEmpty()) {
      entries[0] = last
      var index = 0
      while (true) {
        val left = 2 * index + 1
        val right = left + 1
        var smallest = index
        if (left < entries.size && entries[left].first < entries[smallest].first) smallest = left
        if (right < entries.size && entries[right].first < entries[smallest].first) smallest = right
        if (smallest == index) break
        swap(index, smallest)
        index = smallest
      }
    }
    return min
  }

  private fun swap(first: Int, second: Int) {
    val temporary = entries[first]
    entries[first] = entries[second]
    entries[second] = temporary
  }
}
