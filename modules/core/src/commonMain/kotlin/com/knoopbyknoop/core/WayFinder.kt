package com.knoopbyknoop.core

/**
 * Finds ways along paths with the squares in [store], see `path-routing`: one [PathRouter] that
 * forgets the squares it read whenever the store's squares changed, see `knot-navigation` "Path
 * data arrives on the way". Not thread-safe, like the router: keep one per thread that looks for
 * ways.
 */
class WayFinder(private val store: PathStore) {

  private val router = PathRouter(store)
  private var version = -1

  /** The way for [search], `null` if there is none, see [PathRouter.way]. */
  fun way(search: WaySearch): PathWay? {
    refresh()
    return router.way(search.from, search.to, search.heading)
  }

  /**
   * The ways of route points off the network, see `map-planning` "A route point away from the
   * network".
   */
  fun routePointWays(geometry: NetworkGeometry): RoutePointWays {
    refresh()
    return RoutePointWays(router, geometry)
  }

  /** Frees the squares read, e.g. when memory runs out. */
  fun forgetSquares() = router.forgetSquares()

  /** How many steps the last search took, for the apps' logs. */
  val visitedSteps: Int
    get() = router.visitedSteps

  /** How many squares are read, for the apps' logs. */
  val loadedSquares: Int
    get() = router.loadedSquares

  private fun refresh() {
    if (version == store.version) return
    router.forgetSquares()
    version = store.version
  }
}
