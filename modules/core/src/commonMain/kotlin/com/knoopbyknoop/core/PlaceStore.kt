package com.knoopbyknoop.core

/** Which squares of place details to request, see `network-sync`. */
object PlaceSquares {

  /**
   * The course to download place details around, or `null` if nothing is to be downloaded: without
   * a complete route, or for the course the place details were just deleted for. For cycling and
   * hiking alike, and never depending on the rider's position, see "Place details around the
   * route".
   *
   * @param deletedFor the course of the route planned when the place details were deleted, until
   *   the route changes or navigation starts
   */
  fun courseToLoad(course: List<GeoPoint>?, deletedFor: List<GeoPoint>? = null) = course?.takeIf {
    it.isNotEmpty() && it != deletedFor
  }

  /**
   * The square to download for a place tapped at [position], or `null`: while navigating nothing is
   * requested, and an installed square needs no request, see "Place details for a tapped place".
   */
  fun squareToLoad(position: GeoPoint, navigating: Boolean, store: PlaceStore): PathSquare? {
    if (navigating) return null
    val square = PathSquare.of(position.latitude, position.longitude)
    return square.takeIf { store.installed.none { it.square == square } }
  }
}

/** The squares of place details on the device, see `network-sync` "Keeping place details". */
class PlaceStore(files: PathStoreFiles) : SquareStore(files, PlaceFile.SQUARES) {

  /** The details of the places in [square] by feature id, none if it isn't installed. */
  fun details(square: PathSquare): Map<Long, PlaceDetails> =
    rawBytes(square)?.let(PlaceFile::readSquare) ?: mapOf()

  /**
   * The details of a tapped [place] from the installed square of its icon, `null` when they are not
   * installed or OpenStreetMap has none, see `map-planning` "Contacts on the place card".
   */
  fun detailsOf(place: MapPlace): PlaceDetails? {
    val id = place.featureId ?: return null
    return details(PathSquare.of(place.position.latitude, place.position.longitude))[id]
  }
}

/**
 * Brings squares of place details onto the device, see `network-sync`. Blocks, so call it off the
 * main thread.
 */
class PlaceDownloader(store: PlaceStore, origin: PathDataOrigin) {

  private val squares =
    SquareDownloader(store, origin, PlaceFile.SQUARES, { it.places }, MAX_STORE_BYTES)

  /**
   * Downloads what is missing or older than published around [course], see "Place details around
   * the route".
   *
   * @return what it came to; a failure leaves everything installed as it was
   */
  fun loadAround(
    course: List<GeoPoint>,
    nowMillis: Long,
    isCancelled: () -> Boolean = { false },
  ): PathDataStatus = squares.load(PathSquares.near(course), nowMillis, isCancelled = isCancelled)

  /**
   * Downloads [square] for a place tapped while planning, see "Place details for a tapped place",
   * keeping the squares around [course] if there is a route.
   */
  fun loadSquare(square: PathSquare, course: List<GeoPoint>?, nowMillis: Long): PathDataStatus =
    squares.load(
      setOf(square),
      nowMillis,
      keep = course?.let(PathSquares::near).orEmpty() + square,
    )

  companion object {
    /** See `network-sync` "Keeping place details". */
    const val MAX_STORE_BYTES = 10_000_000L
  }
}
