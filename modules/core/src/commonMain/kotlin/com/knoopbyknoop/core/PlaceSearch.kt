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

  /** Kinds of places that are shown with their address rather than their kind. */
  private val ADDRESSED_KEYS = setOf("amenity", "shop", "craft", "office", "tourism", "leisure")

  private val json = Json { ignoreUnknownKeys = true }

  /**
   * The request for [query], ranked around the map's center and limited to [area].
   *
   * @param language the device's language code, e.g. `nl`; Photon only knows some languages
   */
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
   * The hits of a Photon response, in Photon's order, without hits outside the area of [network]
   * and with duplicates merged.
   *
   * @throws IllegalArgumentException if [response] is not a Photon response
   */
  fun parse(response: String, network: KnotLocator): List<PlaceHit> {
    val features =
      try {
        json.parseToJsonElement(response).jsonObject.getValue("features").jsonArray
      } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Not a Photon response", exception)
      }
    val hits = mutableListOf<Pair<PlaceHit, String?>>()
    for (feature in features) {
      val properties = feature.jsonObject["properties"]?.jsonObject ?: continue
      val coordinates =
        feature.jsonObject["geometry"]?.jsonObject?.get("coordinates")?.jsonArray ?: continue
      val longitude = coordinates[0].jsonPrimitive.double
      val latitude = coordinates[1].jsonPrimitive.double
      if (network.nearest(latitude, longitude, MAX_DISTANCE_TO_NETWORK_METERS) == null) continue
      val hit = hitOf(properties, latitude, longitude) ?: continue
      val city = properties.text("city")
      val isDuplicate = hits.any { (other, otherCity) ->
        other.name == hit.name &&
          otherCity == city &&
          distanceMeters(other.latitude, other.longitude, latitude, longitude) < SAME_PLACE_METERS
      }
      if (!isDuplicate) hits.add(hit to city)
    }
    return hits.map { it.first }
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
          key == "place" && properties.text("name") != null ->
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
   * class (`residential`, `tertiary`, …), and `yes` says nothing about the kind.
   */
  private fun kindOf(properties: JsonObject): String? =
    if (properties.text("osm_key") == "highway") "Street"
    else
      properties
        .text("osm_value")
        ?.takeIf { it != "yes" }
        ?.replace('_', ' ')
        ?.replaceFirstChar { it.uppercase() }

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
