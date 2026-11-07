package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.SignRegion
import kotlin.math.floor

/** Thrown when a border cannot be closed from the ways in the extracts. */
class IncompleteBorderException(message: String) : IllegalStateException(message)

/**
 * The administrative boundary relation that outlines [region], see `network-data` "Sign region of
 * each knot".
 */
internal data class BorderRelation(
  val id: Long,
  val name: String,
  val region: SignRegion,
  val wayIds: List<Long>,
)

/** The region whose border the relation with [tags] is, or `null` if it is none of them. */
internal fun signRegionOfBorder(tags: Map<String, String>): SignRegion? {
  if (tags["boundary"] != "administrative") return null
  if (tags["type"] != "boundary" && tags["type"] != "multipolygon") return null
  val country = tags["ISO3166-1"] ?: tags["ISO3166-1:alpha2"]
  return when {
    // The Dutch national relation also holds the Caribbean islands, whose ways no European extract
    // contains, so the Netherlands are their twelve provinces.
    tags["admin_level"] == "4" && tags["ISO3166-2"] in DUTCH_PROVINCES -> SignRegion.NETHERLANDS
    tags["admin_level"] == "2" && country == "BE" -> SignRegion.BELGIUM
    tags["admin_level"] == "4" && tags["ISO3166-2"] == "DE-NW" -> SignRegion.NORTH_RHINE_WESTPHALIA
    else -> null
  }
}

/** The ISO 3166-2 codes of the twelve Dutch provinces. */
private val DUTCH_PROVINCES =
  setOf("DR", "FL", "FR", "GE", "GR", "LI", "NB", "NH", "OV", "UT", "ZE", "ZH").map { "NL-$it" }

/**
 * The closed rings of a border, as latitudes and longitudes. A point lies inside the border if it
 * lies inside an odd number of rings, so inner rings, e.g. the Belgian enclaves in Baarle-Nassau,
 * are holes without looking at member roles.
 */
internal class Border(val region: SignRegion, rings: List<List<Pair<Double, Double>>>) {

  private val edges: Map<Int, List<DoubleArray>>
  private val minLatitude = rings.minOf { ring -> ring.minOf { it.first } }
  private val maxLatitude = rings.maxOf { ring -> ring.maxOf { it.first } }
  private val minLongitude = rings.minOf { ring -> ring.minOf { it.second } }
  private val maxLongitude = rings.maxOf { ring -> ring.maxOf { it.second } }

  init {
    // Edges by latitude band, so a point is only tested against the edges near its latitude.
    val bands = HashMap<Int, MutableList<DoubleArray>>()
    for (ring in rings) {
      for ((from, to) in ring.zipWithNext()) {
        val edge = doubleArrayOf(from.first, from.second, to.first, to.second)
        for (band in bandOf(minOf(from.first, to.first))..bandOf(maxOf(from.first, to.first))) {
          bands.getOrPut(band) { mutableListOf() }.add(edge)
        }
      }
    }
    edges = bands
  }

  fun contains(latitude: Double, longitude: Double): Boolean {
    if (latitude !in minLatitude..maxLatitude || longitude !in minLongitude..maxLongitude) {
      return false
    }
    var inside = false
    for (edge in edges[bandOf(latitude)].orEmpty()) {
      val (latitude1, longitude1, latitude2, longitude2) = edge.toList()
      if ((latitude1 > latitude) != (latitude2 > latitude)) {
        val crossing =
          longitude1 + (latitude - latitude1) / (latitude2 - latitude1) * (longitude2 - longitude1)
        if (longitude < crossing) inside = !inside
      }
    }
    return inside
  }

  private fun bandOf(latitude: Double) = floor(latitude / BAND_DEGREES).toInt()

  companion object {
    private const val BAND_DEGREES = 0.01

    /**
     * Joins the ways of [relation] into closed rings.
     *
     * @throws IncompleteBorderException if a way is missing or a ring does not close
     */
    fun of(
      relation: BorderRelation,
      wayNodeIds: (Long) -> LongArray?,
      pointOf: (Long) -> Pair<Double, Double>?,
    ): Border {
      fun incomplete(reason: String) =
        IncompleteBorderException(
          "The border of ${relation.name} (relation ${relation.id}) is incomplete: $reason"
        )
      val ways =
        relation.wayIds
          .distinct()
          .map { id -> wayNodeIds(id)?.toList() ?: throw incomplete("way $id is missing") }
          .toMutableList()
      val rings = mutableListOf<List<Long>>()
      while (ways.isNotEmpty()) {
        val ring = ways.removeAt(ways.lastIndex).toMutableList()
        while (ring.first() != ring.last()) {
          val end = ring.last()
          val next =
            ways.indexOfFirst { it.first() == end || it.last() == end }.takeIf { it >= 0 }
              ?: throw incomplete("a ring does not close at node $end")
          val way = ways.removeAt(next)
          ring.addAll((if (way.first() == end) way else way.reversed()).drop(1))
        }
        rings.add(ring)
      }
      return Border(
        relation.region,
        rings.map { ring ->
          ring.map { id -> pointOf(id) ?: throw incomplete("node $id is missing") }
        },
      )
    }
  }
}
