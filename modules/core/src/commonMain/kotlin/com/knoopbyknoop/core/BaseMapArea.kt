package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Where the base map is stored for a route, see `network-sync` "Base map around the route": a
 * detailed corridor along its course and a coarse overview around it, within OpenFreeMap's terms
 * for offline downloads, see "Base map requests".
 *
 * @property detail the corridor along the course
 * @property overview the route's surroundings at low zoom
 * @property key what the stored map belongs to: the same for the same course, to tell the current
 *   route's map from an earlier one
 */
data class BaseMapArea(val detail: BaseMapRegion, val overview: BaseMapRegion, val key: String) {

  /** The tiles of both regions, at most [MAX_TILES]. */
  val tiles: Int
    get() = detail.tiles + overview.tiles

  companion object {
    /** OpenFreeMap's limit for an offline download. */
    const val MAX_TILES = 3_000

    /** OpenFreeMap's size limit for an offline download. */
    const val MAX_BYTES = 50L * 1024 * 1024

    /** OpenFreeMap's limit of requests at a time for an offline download. */
    const val MAX_PARALLEL_REQUESTS = 4

    /** How far from the course the detailed map reaches. */
    const val CORRIDOR_METERS = 500.0

    /** How far around the route the overview reaches. */
    const val OVERVIEW_METERS = 5_000.0

    /** The vector tiles' most detailed level; MapLibre draws closer zooms from it. */
    const val DETAIL_MAX_ZOOM = 14
    const val DETAIL_MIN_ZOOM = 10
    const val OVERVIEW_MAX_ZOOM = 9
    const val OVERVIEW_MIN_ZOOM = 5

    /** Points along the course are taken this far apart, well below a tile at zoom 14. */
    private const val STEP_METERS = 200.0

    /**
     * The area for [course], or `null` for an empty course. Its most detailed levels are left out
     * while the tiles would exceed [MAX_TILES], see "A long route".
     */
    fun of(course: List<GeoPoint>): BaseMapArea? {
      if (course.isEmpty()) return null
      val corridor = corridorTiles(course, DETAIL_MAX_ZOOM)
      val overviewBox = boxAround(course, OVERVIEW_METERS)
      val overview =
        BaseMapRegion(
          rings = listOf(ring(overviewBox)),
          minZoom = OVERVIEW_MIN_ZOOM,
          maxZoom = OVERVIEW_MAX_ZOOM,
          tiles =
            (OVERVIEW_MIN_ZOOM..OVERVIEW_MAX_ZOOM).sumOf { zoom -> tilesIn(overviewBox, zoom) },
        )
      var maxZoom = DETAIL_MAX_ZOOM
      var detail = detailRegion(corridor, maxZoom)
      while (detail.tiles + overview.tiles > MAX_TILES && maxZoom > DETAIL_MIN_ZOOM) {
        maxZoom -= 1
        detail = detailRegion(corridor, maxZoom)
      }
      return BaseMapArea(detail, overview, keyOf(course))
    }

    /** The detailed region down to [maxZoom]: the corridor's tiles at that zoom as its shape. */
    private fun detailRegion(corridor: Set<Tile>, maxZoom: Int): BaseMapRegion {
      val shift = DETAIL_MAX_ZOOM - maxZoom
      val atMax = corridor.map { Tile(maxZoom, it.x shr shift, it.y shr shift) }.toSet()
      val tiles =
        (DETAIL_MIN_ZOOM..maxZoom).sumOf { zoom ->
          val up = maxZoom - zoom
          atMax.map { it.x shr up to (it.y shr up) }.toSet().size
        }
      return BaseMapRegion(
        rings = atMax.sortedWith(compareBy({ it.y }, { it.x })).map { it.ring() },
        minZoom = DETAIL_MIN_ZOOM,
        maxZoom = maxZoom,
        tiles = tiles,
      )
    }

    /** The tiles at [zoom] within [CORRIDOR_METERS] of the course. */
    private fun corridorTiles(course: List<GeoPoint>, zoom: Int): Set<Tile> {
      val tiles = mutableSetOf<Tile>()
      val points =
        Polyline(course).let { line ->
          val steps = (line.length / STEP_METERS).toInt().coerceAtLeast(1)
          (0..steps).map { line.pointAt(line.length * it / steps) }
        }
      for (point in points) {
        val box = boxAround(point, CORRIDOR_METERS)
        val west = tileX(box.minLongitude, zoom)
        val east = tileX(box.maxLongitude, zoom)
        val north = tileY(box.maxLatitude, zoom)
        val south = tileY(box.minLatitude, zoom)
        for (x in west..east) for (y in north..south) tiles += Tile(zoom, x, y)
      }
      return tiles
    }

    private fun boxAround(course: List<GeoPoint>, meters: Double): GeoBounds {
      val margins = marginsAt(course.sumOf { it.latitude } / course.size, meters)
      return GeoBounds(
        minLatitude = course.minOf { it.latitude } - margins.latitude,
        minLongitude = course.minOf { it.longitude } - margins.longitude,
        maxLatitude = course.maxOf { it.latitude } + margins.latitude,
        maxLongitude = course.maxOf { it.longitude } + margins.longitude,
      )
    }

    private fun tilesIn(box: GeoBounds, zoom: Int): Int =
      (tileX(box.maxLongitude, zoom) - tileX(box.minLongitude, zoom) + 1) *
        (tileY(box.minLatitude, zoom) - tileY(box.maxLatitude, zoom) + 1)

    private fun ring(box: GeoBounds): List<GeoPoint> =
      listOf(
        GeoPoint(box.minLatitude, box.minLongitude),
        GeoPoint(box.minLatitude, box.maxLongitude),
        GeoPoint(box.maxLatitude, box.maxLongitude),
        GeoPoint(box.maxLatitude, box.minLongitude),
        GeoPoint(box.minLatitude, box.minLongitude),
      )

    internal fun tileX(longitude: Double, zoom: Int): Int =
      floor((longitude + 180) / 360 * (1 shl zoom)).toInt()

    internal fun tileY(latitude: Double, zoom: Int): Int {
      val radians = latitude * PI / 180
      return floor((1 - asinh(tan(radians)) / PI) / 2 * (1 shl zoom)).toInt()
    }

    /** A stable name for [course], FNV-1a over its points to the metre. */
    private fun keyOf(course: List<GeoPoint>): String {
      var hash = -0x340d631b7bdddcdbL
      for (point in course) {
        for (value in listOf(point.latitude, point.longitude)) {
          var bits = (value * 100_000).toLong()
          repeat(8) {
            hash = (hash xor (bits and 0xff)) * 0x100000001b3L
            bits = bits shr 8
          }
        }
      }
      return hash.toULong().toString(16)
    }
  }

  /** A map tile. */
  private data class Tile(val zoom: Int, val x: Int, val y: Int) {
    /** The tile's square, shrunk a little so it doesn't touch its neighbours. */
    fun ring(): List<GeoPoint> {
      val count = (1 shl zoom).toDouble()
      fun longitude(x: Double) = x / count * 360 - 180
      fun latitude(y: Double) = atan(sinh(PI * (1 - 2 * y / count))) * 180 / PI
      val inset = 0.02
      val west = longitude(x + inset)
      val east = longitude(x + 1 - inset)
      val north = latitude(y + inset)
      val south = latitude(y + 1 - inset)
      return listOf(
        GeoPoint(south, west),
        GeoPoint(south, east),
        GeoPoint(north, east),
        GeoPoint(north, west),
        GeoPoint(south, west),
      )
    }
  }
}

/**
 * A part of [BaseMapArea], as MapLibre stores it: a shape and a zoom range.
 *
 * @property rings closed rings of the shape, each its own polygon
 * @property tiles how many tiles the shape covers at the zooms from [minZoom] to [maxZoom]
 */
data class BaseMapRegion(
  val rings: List<List<GeoPoint>>,
  val minZoom: Int,
  val maxZoom: Int,
  val tiles: Int,
) {
  /** The shape as GeoJSON, a MultiPolygon, for the apps' offline regions. */
  val geoJson: String
    get() =
      rings.joinToString(",", """{"type":"MultiPolygon","coordinates":[""", "]}") { ring ->
        ring.joinToString(",", "[[", "]]") { "[${it.longitude},${it.latitude}]" }
      }
}

/** What to store the base map for, see `network-sync` "Base map around the route". */
object BaseMapSquares {
  /**
   * The course whose base map to store: the planned route's, cycling or hiking, unless its map was
   * deleted for that course, see "Deleting the base map with a route planned".
   */
  fun courseToLoad(course: List<GeoPoint>?, deletedFor: List<GeoPoint>? = null): List<GeoPoint>? =
    course?.takeIf {
      it.isNotEmpty() && it != deletedFor
    }
}
