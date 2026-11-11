package com.knoopbyknoop.core

import kotlin.math.max

/** Which squares to request, see `network-sync` "Path data around the route". */
object PathSquares {

  /** How far from a route's course ways are downloaded. */
  private const val AROUND_METERS = 5000.0

  /** Course points further apart than this get points added between them. */
  private const val STEP_METERS = 2000.0

  /**
   * The course to download squares for, or `null` if nothing is to be downloaded: on a hiking
   * route, without a complete route, or for the course the ways were just deleted for, see
   * `network-sync` "Path data in the info sheet". It never depends on the rider's position.
   *
   * @param deletedFor the course of the route planned when the ways were deleted, until the route
   *   changes or navigation starts
   */
  fun courseToLoad(
    network: NetworkType?,
    course: List<GeoPoint>?,
    deletedFor: List<GeoPoint>? = null,
  ) = course?.takeIf {
    network == NetworkType.CYCLING && it.isNotEmpty() && it != deletedFor
  }

  /** The squares within [AROUND_METERS] of [course]. */
  fun near(course: List<GeoPoint>): Set<PathSquare> {
    val squares = LinkedHashSet<PathSquare>()
    fun addAround(point: GeoPoint) {
      val margins = marginsAt(point.latitude, AROUND_METERS)
      val latitudeMargin = margins.latitude
      val longitudeMargin = margins.longitude
      val low = PathSquare.of(point.latitude - latitudeMargin, point.longitude - longitudeMargin)
      val high = PathSquare.of(point.latitude + latitudeMargin, point.longitude + longitudeMargin)
      for (latitude in low.latitudeIndex..high.latitudeIndex) {
        for (longitude in low.longitudeIndex..high.longitudeIndex) {
          squares.add(PathSquare(latitude, longitude))
        }
      }
    }
    course.firstOrNull()?.let(::addAround)
    course.zipWithNext().forEach { (from, to) ->
      val meters = distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
      val steps = max(1, (meters / STEP_METERS).toInt() + 1)
      for (step in 1..steps) {
        val along = step.toDouble() / steps
        addAround(
          GeoPoint(
            from.latitude + (to.latitude - from.latitude) * along,
            from.longitude + (to.longitude - from.longitude) * along,
          )
        )
      }
    }
    return squares
  }
}

/**
 * The squares of path data on the device, see `network-sync` "Keeping path data". Read by the
 * router and written by [PathDownloader], possibly at the same time.
 */
class PathStore(files: PathStoreFiles) : SquareStore(files, PathFile.SQUARES), PathSquareSource {

  override fun segments(square: PathSquare): List<PathSegment> =
    rawBytes(square)?.let(PathFile::readSquare) ?: listOf()
}

/**
 * Brings the squares around a course onto the device, see `network-sync`. Blocks, so call it off
 * the main thread.
 */
class PathDownloader(store: PathStore, origin: PathDataOrigin) {

  private val squares =
    SquareDownloader(store, origin, PathFile.SQUARES, { it.paths }, MAX_STORE_BYTES)

  /**
   * Downloads what is missing or older than the published path data around [course].
   *
   * @param isCancelled asked between requests
   * @param onStatus told when it is known whether there is anything to download
   * @return what it came to; a failure leaves everything installed as it was
   */
  fun load(
    course: List<GeoPoint>,
    nowMillis: Long,
    isCancelled: () -> Boolean = { false },
    onStatus: (PathDataStatus) -> Unit = {},
  ): PathDataStatus =
    squares.load(
      PathSquares.near(course),
      nowMillis,
      isCancelled = isCancelled,
      onStatus = onStatus,
    )

  companion object {
    /** See `network-sync` "Keeping path data". */
    const val MAX_STORE_BYTES = 50_000_000L
  }
}
