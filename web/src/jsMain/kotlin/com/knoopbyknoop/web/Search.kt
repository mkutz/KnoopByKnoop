package com.knoopbyknoop.web

import com.knoopbyknoop.core.GeoBounds
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.PlaceHit
import com.knoopbyknoop.core.PlaceSearch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** What the search field shows, see `web-planner` "Place search in the planner". */
sealed interface SearchResult {
  data object None : SearchResult

  data object Searching : SearchResult

  data object NoHits : SearchResult

  /** The search service did not answer. */
  data object Unavailable : SearchResult

  data class Hits(val hits: List<PlaceHit>) : SearchResult
}

/**
 * The place search of the apps, see `place-search`: two Photon requests, around the map's centre
 * and for prominent places, merged and limited to the network's area. [get] fetches a URL's text,
 * `null` when it fails.
 */
class PlaceSearcher(private val get: suspend (String) -> String?) {

  suspend fun search(
    query: String,
    center: GeoPoint?,
    network: LoadedNetwork,
    language: Language,
  ): SearchResult {
    val area = GeoBounds.of(network.graph) ?: return SearchResult.None
    if (query.trim().length < PlaceSearch.MIN_QUERY_LENGTH) return SearchResult.None
    val requests =
      PlaceSearch.requests(
        query,
        center?.latitude ?: ((area.minLatitude + area.maxLatitude) / 2),
        center?.longitude ?: ((area.minLongitude + area.maxLongitude) / 2),
        area,
        language.name.lowercase(),
      )
    val (prominent, nearby) =
      coroutineScope {
        listOf(requests.prominent, requests.nearby)
          .map { url -> async { runCatching { get(url) }.getOrNull() } }
          .awaitAll()
      }
    val hits =
      try {
        PlaceSearch.merge(prominent, nearby, network.locator)
      } catch (exception: IllegalArgumentException) {
        return SearchResult.Unavailable
      }
    return if (hits.isEmpty()) SearchResult.NoHits else SearchResult.Hits(hits)
  }
}
