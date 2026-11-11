package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Country
import com.knoopbyknoop.core.Crossing
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.Region
import com.knoopbyknoop.core.SignRegion
import com.knoopbyknoop.core.distanceMeters
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
internal class Border<T>(val region: T, rings: List<List<Pair<Double, Double>>>) {

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
    ): Border<SignRegion> =
      of(relation.region, relation.id, relation.name, relation.wayIds, wayNodeIds, pointOf)

    /**
     * Joins the ways [wayIds] of the relation [id] named [name] into the closed rings of [region].
     *
     * @throws IncompleteBorderException if a way is missing or a ring does not close
     */
    fun <T> of(
      region: T,
      id: Long,
      name: String,
      wayIds: List<Long>,
      wayNodeIds: (Long) -> LongArray?,
      pointOf: (Long) -> Pair<Double, Double>?,
    ): Border<T> {
      fun incomplete(reason: String) =
        IncompleteBorderException("The border of $name (relation $id) is incomplete: $reason")
      val ways =
        wayIds
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
        region,
        rings.map { ring ->
          ring.map { id -> pointOf(id) ?: throw incomplete("node $id is missing") }
        },
      )
    }
  }
}

/**
 * The region whose border the relation with [tags] is: a Dutch province, a Belgian province,
 * Brussels or North Rhine-Westphalia, the regions the extracts hold whole, see `network-data`
 * "Borders between knots"; `null` for any other.
 */
internal fun regionOfBorder(tags: Map<String, String>): Region? {
  if (tags["boundary"] != "administrative") return null
  if (tags["type"] != "boundary" && tags["type"] != "multipolygon") return null
  val key = tags["ISO3166-2"] ?: return null
  val level = tags["admin_level"]
  val country =
    when {
      level == "4" && key in DUTCH_PROVINCES -> Country.NETHERLANDS
      level == "6" && key in BELGIAN_PROVINCES -> Country.BELGIUM
      level == "4" && key == "BE-BRU" -> Country.BELGIUM
      level == "4" && key == "DE-NW" -> Country.GERMANY
      else -> return null
    }
  val name = tags["name"] ?: key
  val names =
    Language.entries
      .mapNotNull { language -> tags["name:${language.name.lowercase()}"]?.let { language to it } }
      .toMap()
  return Region(key, country, name, names)
}

/** The ISO 3166-2 codes of the ten Belgian provinces. */
private val BELGIAN_PROVINCES =
  setOf("VAN", "VBR", "VLI", "VOV", "VWV", "WBR", "WHT", "WLG", "WLX", "WNA").map { "BE-$it" }

/** How far apart the course is sampled for borders, and how closely a crossing is placed. */
private const val SAMPLE_METERS = 50.0
private const val CROSSING_PRECISION_METERS = 5.0

/**
 * Where [course] crosses the borders of [regions], from its start, see `network-data` "Borders
 * between knots". Only crossings from one region into another count, also with a gap outside all
 * regions between them; a course leaving the regions gives none.
 */
internal fun crossingsAlong(course: List<GeoPoint>, regions: List<Border<Region>>): List<Crossing> {
  if (course.size < 2 || regions.isEmpty()) return listOf()
  val lengths = DoubleArray(course.size)
  for (index in 1 until course.size) {
    lengths[index] =
      lengths[index - 1] +
        distanceMeters(
          course[index - 1].latitude,
          course[index - 1].longitude,
          course[index].latitude,
          course[index].longitude,
        )
  }
  val total = lengths.last()

  fun pointAt(meters: Double): GeoPoint {
    val index = lengths.indexOfFirst { it >= meters }.coerceAtLeast(1)
    val step = lengths[index] - lengths[index - 1]
    val share = if (step == 0.0) 0.0 else (meters - lengths[index - 1]) / step
    val from = course[index - 1]
    val to = course[index]
    return GeoPoint(
      from.latitude + (to.latitude - from.latitude) * share,
      from.longitude + (to.longitude - from.longitude) * share,
    )
  }

  fun regionAt(meters: Double): Region? {
    val point = pointAt(meters)
    return regions.firstOrNull { it.contains(point.latitude, point.longitude) }?.region
  }

  val crossings = mutableListOf<Crossing>()
  val steps = maxOf(1, (total / SAMPLE_METERS).toInt())
  var before = regionAt(0.0)
  var beforeMeters = 0.0
  // Outside the regions only crossings between two of them count, see "Beyond the covered area".
  var known = before
  for (step in 1..steps) {
    val meters = total * step / steps
    val here = regionAt(meters)
    val from = known
    if (here != before && here != null && from != null && here != from) {
      var low = beforeMeters
      var high = meters
      while (high - low > CROSSING_PRECISION_METERS) {
        val middle = (low + high) / 2
        if (regionAt(middle) == before) low = middle else high = middle
      }
      crossings += Crossing(((low + high) / 2).toInt(), from, here)
    }
    if (here != null) known = here
    before = here
    beforeMeters = meters
  }
  return crossings
}
