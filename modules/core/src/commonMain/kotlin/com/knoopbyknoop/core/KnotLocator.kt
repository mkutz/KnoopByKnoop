package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Finds the knot nearest to a tapped position, see `map-planning`.
 *
 * Knots are kept in a grid of cells of about 1 km, so a lookup only measures the knots of the few
 * cells around the position, even in networks with tens of thousands of knots.
 */
class KnotLocator(knots: Collection<Knot>) {

  /** Every knot in the cell of each of its [Knot.positions]. */
  private val cells: Map<Long, List<Knot>> =
    knots
      .flatMap { knot ->
        knot.positions.map { cellOf(it.latitude, it.longitude) }.distinct().map { it to knot }
      }
      .groupBy({ it.first }, { it.second })

  /** The knot nearest to the position, or `null` if no knot is within [radiusMeters]. */
  fun nearest(latitude: Double, longitude: Double, radiusMeters: Double): Knot? =
    nearest(latitude, longitude, radiusMeters) { listOf(GeoPoint(it.latitude, it.longitude)) }

  /**
   * The knot with the node nearest to the position, any of the nodes it is tagged on, or `null` if
   * none is within [radiusMeters], see `knot-navigation` "Passing a knot".
   */
  fun nearestAtAnyNode(latitude: Double, longitude: Double, radiusMeters: Double): Knot? =
    nearest(latitude, longitude, radiusMeters) { it.positions }

  private fun nearest(
    latitude: Double,
    longitude: Double,
    radiusMeters: Double,
    positionsOf: (Knot) -> List<GeoPoint>,
  ): Knot? {
    val latitudeRadius = radiusMeters / METERS_PER_DEGREE
    val longitudeRadius = latitudeRadius / cos(latitude * PI / 180).coerceAtLeast(0.01)
    val (minimumColumn, minimumRow) =
      cellCoordinates(latitude - latitudeRadius, longitude - longitudeRadius)
    val (maximumColumn, maximumRow) =
      cellCoordinates(latitude + latitudeRadius, longitude + longitudeRadius)
    var nearest: Knot? = null
    var nearestDistance = radiusMeters
    for (column in minimumColumn..maximumColumn) {
      for (row in minimumRow..maximumRow) {
        for (knot in cells[key(column, row)].orEmpty()) {
          val distance =
            positionsOf(knot).minOf {
              distanceMeters(latitude, longitude, it.latitude, it.longitude)
            }
          if (distance <= nearestDistance) {
            nearest = knot
            nearestDistance = distance
          }
        }
      }
    }
    return nearest
  }

  private fun cellOf(latitude: Double, longitude: Double): Long {
    val (column, row) = cellCoordinates(latitude, longitude)
    return key(column, row)
  }

  private fun cellCoordinates(latitude: Double, longitude: Double): Pair<Int, Int> =
    floor(longitude / CELL_DEGREES).toInt() to floor(latitude / CELL_DEGREES).toInt()

  private fun key(column: Int, row: Int): Long =
    (column.toLong() shl 32) or (row.toLong() and 0xFFFFFFFF)

  private companion object {
    const val CELL_DEGREES = 0.01
  }
}

private const val EARTH_RADIUS_METERS = 6_371_008.8

private const val METERS_PER_DEGREE = EARTH_RADIUS_METERS * PI / 180

/** The great-circle distance between two positions. */
internal fun distanceMeters(
  fromLatitude: Double,
  fromLongitude: Double,
  toLatitude: Double,
  toLongitude: Double,
): Double {
  val deltaLatitude = (toLatitude - fromLatitude) * PI / 180
  val deltaLongitude = (toLongitude - fromLongitude) * PI / 180
  val a =
    sin(deltaLatitude / 2).let { it * it } +
      cos(fromLatitude * PI / 180) *
        cos(toLatitude * PI / 180) *
        sin(deltaLongitude / 2).let { it * it }
  return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}
