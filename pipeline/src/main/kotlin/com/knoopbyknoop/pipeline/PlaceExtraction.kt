package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.OpeningHours
import com.knoopbyknoop.core.PathSquare
import com.knoopbyknoop.core.PlaceDetails
import com.knoopbyknoop.core.PlaceFile
import com.knoopbyknoop.core.PlaceKind

/**
 * Collects the details of the highlighted places, see `network-data` "Published place details".
 * Ways first, then nodes: a place drawn as an area needs the positions of its way's nodes.
 */
class PlaceExtraction {

  private class WayPlace(val id: Long, val nodeIds: LongArray, val details: PlaceDetails)

  private val wayPlaces = ArrayList<WayPlace>()
  private val wantedNodes = HashSet<Long>()
  private val nodePositions = HashMap<Long, Pair<Double, Double>>()
  private val squares = HashMap<PathSquare, MutableMap<Long, PlaceDetails>>()
  private val kindCounts = sortedMapOf<PlaceKind, Int>()

  /** How many places have opening hours, and how many of those the apps can read. */
  var openingHoursCount = 0
    private set

  var readableOpeningHoursCount = 0
    private set

  /** The number of places with details per kind. */
  val counts: Map<PlaceKind, Int>
    get() = kindCounts

  fun acceptWay(way: OsmWay) {
    val details = placeDetails(way.tags) ?: return
    wayPlaces += WayPlace(way.id, way.nodeIds.toLongArray(), details)
    wantedNodes.addAll(way.nodeIds)
  }

  fun acceptNode(node: OsmNode) {
    if (node.id in wantedNodes) nodePositions[node.id] = node.latitude to node.longitude
    val details = placeDetails(node.tags) ?: return
    add(PathSquare.of(node.latitude, node.longitude), PlaceFile.nodeFeatureId(node.id), details)
  }

  /**
   * The details by feature id per square. A way is in every square its outline touches, as the base
   * map puts its icon somewhere inside it.
   */
  fun squares(): Map<PathSquare, Map<Long, PlaceDetails>> {
    for (place in wayPlaces) {
      val positions = place.nodeIds.toList().mapNotNull(nodePositions::get)
      if (positions.isEmpty()) continue
      val low = PathSquare.of(positions.minOf { it.first }, positions.minOf { it.second })
      val high = PathSquare.of(positions.maxOf { it.first }, positions.maxOf { it.second })
      for (latitude in low.latitudeIndex..high.latitudeIndex) {
        for (longitude in low.longitudeIndex..high.longitudeIndex) {
          add(PathSquare(latitude, longitude), PlaceFile.wayFeatureId(place.id), place.details)
        }
      }
    }
    wayPlaces.clear()
    return squares
  }

  /** The details of a highlighted place with [tags], `null` for other objects or without any. */
  private fun placeDetails(tags: Map<String, String>): PlaceDetails? {
    if (tags.isEmpty()) return null
    val kind = PlaceKind.ofTags(tags) ?: return null
    val details = PlaceDetails.ofTags(tags) ?: return null
    kindCounts.merge(kind, 1, Int::plus)
    details.openingHours?.let {
      openingHoursCount++
      if (OpeningHours.parse(it) != null) readableOpeningHoursCount++
    }
    return details
  }

  private fun add(square: PathSquare, featureId: Long, details: PlaceDetails) {
    squares.getOrPut(square) { HashMap() }[featureId] = details
  }
}
