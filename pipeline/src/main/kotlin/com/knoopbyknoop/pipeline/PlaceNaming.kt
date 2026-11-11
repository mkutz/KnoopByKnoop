package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.Place
import com.knoopbyknoop.core.distanceMeters
import kotlin.math.cos
import kotlin.math.floor

/**
 * The places that count for "Place of each knot" in `network-data`, with how far from their point a
 * knot still lies in them.
 */
internal enum class PlaceSize(val tag: String, val reachMeters: Double) {
  VILLAGE("village", 1_000.0),
  TOWN("town", 2_000.0),
  CITY("city", 4_000.0),
}

/** The place of [tags], if they are a named village, town or city, with its size. */
internal fun placeOfTags(tags: Map<String, String>): Pair<Place, PlaceSize>? {
  val size = PlaceSize.entries.firstOrNull { it.tag == tags["place"] } ?: return null
  val name = tags["name"]?.takeIf { it.isNotBlank() } ?: return null
  val names =
    Language.entries
      .mapNotNull { language ->
        tags["name:${language.name.lowercase()}"]
          ?.takeIf { it.isNotBlank() && it != name }
          ?.let { language to it }
      }
      .toMap()
  return Place(name, names) to size
}

/**
 * A place with its point, and its outline where it is mapped as an area.
 *
 * @property ring the closed outline as latitudes and longitudes, `null` for a point
 */
internal class LocatedPlace(
  val place: Place,
  val size: PlaceSize,
  val latitude: Double,
  val longitude: Double,
  val ring: List<Pair<Double, Double>>? = null,
)

/** Finds the place a knot lies in, see `network-data` "Place of each knot". */
internal class PlaceFinder(places: List<LocatedPlace>) {

  private val cells = HashMap<Pair<Int, Int>, MutableList<LocatedPlace>>()

  init {
    for (place in places) cells.getOrPut(cellOf(place.latitude, place.longitude)) {
      mutableListOf()
    } += place
  }

  /**
   * Inside an area, that place; otherwise the point within its reach, the nearest relative to its
   * reach; `null` out in the open.
   */
  fun placeAt(latitude: Double, longitude: Double): Place? {
    val (row, column) = cellOf(latitude, longitude)
    val near =
      (row - SEARCH_CELLS..row + SEARCH_CELLS).flatMap { r ->
        (column - SEARCH_CELLS..column + SEARCH_CELLS).flatMap { c -> cells[r to c].orEmpty() }
      }
    near
      .filter { it.ring != null && inside(it.ring, latitude, longitude) }
      .minByOrNull { it.size.reachMeters }
      ?.let {
        return it.place
      }
    return near
      .map {
        it to distanceMeters(latitude, longitude, it.latitude, it.longitude) / it.size.reachMeters
      }
      .filter { (_, share) -> share <= 1.0 }
      .minByOrNull { (_, share) -> share }
      ?.first
      ?.place
  }

  private fun inside(
    ring: List<Pair<Double, Double>>,
    latitude: Double,
    longitude: Double,
  ): Boolean {
    var inside = false
    for ((from, to) in ring.zipWithNext()) {
      if ((from.first > latitude) != (to.first > latitude)) {
        val crossing =
          from.second +
            (latitude - from.first) / (to.first - from.first) * (to.second - from.second)
        if (longitude < crossing) inside = !inside
      }
    }
    return inside
  }

  private companion object {
    /** About 5.5 km north to south; wider than the largest reach together with the search. */
    const val CELL_DEGREES = 0.05

    /** Cells searched around a knot's: enough for a city 4 km away, also east to west at 51° N. */
    const val SEARCH_CELLS = 2

    fun cellOf(latitude: Double, longitude: Double) =
      floor(latitude / CELL_DEGREES).toInt() to
        floor(longitude * cos(Math.toRadians(51.0)) / CELL_DEGREES).toInt()
  }
}
