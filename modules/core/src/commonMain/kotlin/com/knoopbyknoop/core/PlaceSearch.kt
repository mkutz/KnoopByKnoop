package com.knoopbyknoop.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A place found by [PlaceSearch].
 *
 * @property description what tells it apart from places of the same name, e.g. its address or
 *   "Train station, Utrecht"
 */
data class PlaceHit(
  val name: String,
  val description: String,
  val latitude: Double,
  val longitude: Double,
)

/** The two requests of one search, see [PlaceSearch.requests]. */
data class PlaceSearchRequests(val prominent: String, val nearby: String)

/** The extent of a network, to limit searches to the area it covers. */
data class GeoBounds(
  val minLatitude: Double,
  val minLongitude: Double,
  val maxLatitude: Double,
  val maxLongitude: Double,
) {
  companion object {
    fun of(graph: NetworkGraph): GeoBounds? {
      val knots = graph.knots.values.takeIf { it.isNotEmpty() } ?: return null
      return GeoBounds(
        knots.minOf { it.latitude },
        knots.minOf { it.longitude },
        knots.maxOf { it.latitude },
        knots.maxOf { it.longitude },
      )
    }
  }
}

/**
 * Searching places with the Photon geocoder (OpenStreetMap data), see `place-search`. Only builds
 * requests and reads responses; the apps send them.
 */
object PlaceSearch {

  const val BASE_URL = "https://photon.komoot.io/api/"

  /** Search starts from this many characters on. */
  const val MIN_QUERY_LENGTH = 3

  /** Hits farther than this from any knot are outside the area the networks cover. */
  const val MAX_DISTANCE_TO_NETWORK_METERS = 3_000.0

  /** Hits of the same name and town closer than this are one place, e.g. a station and its hall. */
  const val SAME_PLACE_METERS = 500.0

  private val LANGUAGES = setOf("de", "en", "fr")

  /** Photon's `zoom` for the prominent request: a wide, weak location bias. */
  private const val PROMINENT_ZOOM = 10

  private const val MAX_IMPORTANT_HITS = 3

  private const val MAX_HITS = 10

  /** Kinds of places that are found wherever they are: towns, municipalities, stations. */
  private val IMPORTANT_KEYS = setOf("place", "boundary", "railway", "public_transport", "aeroway")

  private val IMPORTANT_VALUES = setOf("train_station")

  /** Kinds of places that are shown with their address rather than their kind. */
  private val ADDRESSED_KEYS = setOf("amenity", "shop", "craft", "office", "tourism", "leisure")

  private val json = Json { ignoreUnknownKeys = true }

  /**
   * The two requests for [query], see [merge]: one ranked by importance, so that towns and stations
   * of that name are found wherever they are, and one ranked by nearness to the map's center, so
   * that nearby streets and businesses come first.
   *
   * @param language the device's language code, e.g. `nl`; Photon only knows some languages
   */
  fun requests(
    query: String,
    centerLatitude: Double,
    centerLongitude: Double,
    area: GeoBounds,
    language: String,
  ): PlaceSearchRequests =
    PlaceSearchRequests(
      prominent =
        requestUrl(query, centerLatitude, centerLongitude, area, language) +
          "&zoom=$PROMINENT_ZOOM",
      nearby = requestUrl(query, centerLatitude, centerLongitude, area, language),
    )

  /** A request ranked by nearness to the map's center, with Photon's default strength. */
  fun requestUrl(
    query: String,
    centerLatitude: Double,
    centerLongitude: Double,
    area: GeoBounds,
    language: String,
  ): String {
    val parameters =
      listOf(
        "q" to query.trim(),
        "limit" to "10",
        "lang" to (language.lowercase().takeIf { it in LANGUAGES } ?: "default"),
        "lat" to centerLatitude.toString(),
        "lon" to centerLongitude.toString(),
        "bbox" to
          listOf(area.minLongitude, area.minLatitude, area.maxLongitude, area.maxLatitude)
            .joinToString(","),
      )
    return BASE_URL + "?" + parameters.joinToString("&") { (key, value) -> "$key=${encode(value)}" }
  }

  /**
   * The hits of both [requests]' responses: up to three important places (towns, stations, …) of
   * the [prominent] response first, then the [nearby] response's hits, without hits outside the
   * area of [network] and with duplicates merged. A response is `null` when its request failed.
   *
   * @throws IllegalArgumentException if no response is a Photon response
   */
  fun merge(prominent: String?, nearby: String?, network: KnotLocator): List<PlaceHit> {
    val prominentCandidates = prominent?.let { runCatching { candidates(it, network) }.getOrNull() }
    val nearbyCandidates = nearby?.let { runCatching { candidates(it, network) }.getOrNull() }
    require(prominentCandidates != null || nearbyCandidates != null) { "No Photon response" }
    val important =
      prominentCandidates
        .orEmpty()
        .filter { it.isImportant }
        // A town's place is its center; its boundary's position is somewhere in the municipality.
        .sortedBy { it.key == "boundary" }
        .distinctBy { it.hit.name to it.hit.description }
        .take(MAX_IMPORTANT_HITS)
    return distinct(important + nearbyCandidates.orEmpty())
      .filterNot { candidate ->
        candidate !in important &&
          important.any {
            it.hit.name == candidate.hit.name && it.hit.description == candidate.hit.description
          }
      }
      .take(MAX_HITS)
      .map { it.hit }
  }

  /**
   * The hits of one Photon response, in Photon's order, without hits outside the area of [network]
   * and with duplicates merged.
   *
   * @throws IllegalArgumentException if [response] is not a Photon response
   */
  fun parse(response: String, network: KnotLocator): List<PlaceHit> =
    distinct(candidates(response, network)).map { it.hit }

  private class Candidate(val hit: PlaceHit, val city: String?, val key: String?, value: String?) {
    val isImportant = key in IMPORTANT_KEYS || value in IMPORTANT_VALUES
  }

  private fun candidates(response: String, network: KnotLocator): List<Candidate> {
    val features =
      try {
        json.parseToJsonElement(response).jsonObject.getValue("features").jsonArray
      } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Not a Photon response", exception)
      }
    return features.mapNotNull { feature ->
      val properties = feature.jsonObject["properties"]?.jsonObject ?: return@mapNotNull null
      val coordinates =
        feature.jsonObject["geometry"]?.jsonObject?.get("coordinates")?.jsonArray
          ?: return@mapNotNull null
      val longitude = coordinates[0].jsonPrimitive.double
      val latitude = coordinates[1].jsonPrimitive.double
      if (network.nearest(latitude, longitude, MAX_DISTANCE_TO_NETWORK_METERS) == null) {
        return@mapNotNull null
      }
      val hit = hitOf(properties, latitude, longitude) ?: return@mapNotNull null
      Candidate(
        hit,
        properties.text("city"),
        properties.text("osm_key"),
        properties.text("osm_value"),
      )
    }
  }

  /** Hits of the same name and town close to each other are one place, e.g. a station's hall. */
  private fun distinct(candidates: List<Candidate>): List<Candidate> {
    val kept = mutableListOf<Candidate>()
    for (candidate in candidates) {
      val hit = candidate.hit
      val isDuplicate = kept.any {
        it.hit.name == hit.name &&
          it.city == candidate.city &&
          distanceMeters(it.hit.latitude, it.hit.longitude, hit.latitude, hit.longitude) <
            SAME_PLACE_METERS
      }
      if (!isDuplicate) kept.add(candidate)
    }
    return kept
  }

  private fun hitOf(properties: JsonObject, latitude: Double, longitude: Double): PlaceHit? {
    val street =
      listOfNotNull(properties.text("street"), properties.text("housenumber"))
        .joinToString(" ")
        .ifEmpty { null }
    val city = properties.text("city")
    val name = properties.text("name") ?: street ?: return null
    val key = properties.text("osm_key")
    val description =
      when {
          (key == "place" || key == "boundary") && properties.text("name") != null ->
            listOfNotNull(properties.text("state"), properties.text("country"))
          key in ADDRESSED_KEYS || name == street ->
            listOfNotNull(street.takeIf { it != name }, city)
          else -> listOfNotNull(kindOf(properties), city)
        }
        .distinct()
        .joinToString(", ")
    return PlaceHit(name, description, latitude, longitude)
  }

  /**
   * E.g. "Train station" for `osm_value=train_station`. Roads are just streets, whatever their road
   * class (`residential`, `tertiary`, …). Where the value says little (`yes`, `movable`), the key
   * tells the kind, e.g. "Bridge".
   */
  private fun kindOf(properties: JsonObject): String? {
    val key = properties.text("osm_key")
    if (key == "highway") return "Street"
    val value = properties.text("osm_value")
    val kind = if (value == null || value in VAGUE_VALUES) key else value
    return kind?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
  }

  private val VAGUE_VALUES = setOf("yes", "movable")

  private fun JsonObject.text(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

  /** Percent-encodes [value] as UTF-8, like `application/x-www-form-urlencoded` without `+`. */
  private fun encode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
      val char = byte.toInt().toChar()
      if (char.isLetterOrDigit() && byte >= 0 || char in "-._~") {
        append(char)
      } else {
        append('%')
        append(HEX[(byte.toInt() shr 4) and 0xF])
        append(HEX[byte.toInt() and 0xF])
      }
    }
  }

  private const val HEX = "0123456789ABCDEF"
}
