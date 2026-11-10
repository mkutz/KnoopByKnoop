package com.knoopbyknoop.core

/**
 * A binary min-heap, as the common standard library has no priority queue; used by both routers.
 * Entries of equal priority come out in an order that depends only on the order they went in.
 */
internal class MinHeap<T> {

  /** An entry with its [priority]. */
  class Entry<T>(val priority: Double, val value: T)

  private val entries = ArrayList<Entry<T>>()

  fun isNotEmpty() = entries.isNotEmpty()

  fun add(priority: Double, value: T) {
    entries.add(Entry(priority, value))
    var index = entries.lastIndex
    while (index > 0) {
      val parent = (index - 1) / 2
      if (entries[parent].priority <= entries[index].priority) break
      swap(parent, index)
      index = parent
    }
  }

  /** The entry with the lowest priority, taken out; `null` when empty. */
  fun poll(): Entry<T>? {
    if (entries.isEmpty()) return null
    val top = entries[0]
    val last = entries.removeAt(entries.lastIndex)
    if (entries.isNotEmpty()) {
      entries[0] = last
      var index = 0
      while (true) {
        val left = index * 2 + 1
        val right = left + 1
        var smallest = index
        if (left < entries.size && entries[left].priority < entries[smallest].priority) {
          smallest = left
        }
        if (right < entries.size && entries[right].priority < entries[smallest].priority) {
          smallest = right
        }
        if (smallest == index) break
        swap(index, smallest)
        index = smallest
      }
    }
    return top
  }

  private fun swap(first: Int, second: Int) {
    val entry = entries[first]
    entries[first] = entries[second]
    entries[second] = entry
  }
}
