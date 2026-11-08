package com.knoopbyknoop.core

/**
 * A kind of place useful on a ride, drawn from the base map's `poi` layer with its own icon, see
 * `map-planning` "Places useful on a ride".
 *
 * @property key the kind's name in the map image name, see [iconName]
 * @property colour the icon disc's colour as ARGB, the same on the light and the dark map
 * @property matches the OpenMapTiles `class` and the `subclass` values it covers per class
 */
enum class PlaceKind(val key: String, val colour: Long, val matches: Map<String, Set<String>>) {
  FOOD(
    "food",
    0xFFB0268C,
    mapOf(
      "restaurant" to setOf("restaurant"),
      "cafe" to setOf("cafe"),
      "fast_food" to setOf("fast_food"),
      "bar" to setOf("bar"),
      "beer" to setOf("pub", "biergarten"),
      "ice_cream" to setOf("ice_cream"),
    ),
  ),
  SUPERMARKET("supermarket", 0xFF7A3E9D, mapOf("grocery" to setOf("supermarket"))),
  BICYCLE("bicycle", 0xFF8A5A2B, mapOf("bicycle" to setOf("bicycle"))),
  RAILWAY("railway", 0xFF3D4A5C, mapOf("railway" to setOf("station", "halt"))),
  WATER(
    "water",
    0xFF00897B,
    mapOf("drinking_water" to setOf("drinking_water"), "toilets" to setOf("toilets")),
  );

  /** The name of this kind's icon in the map style. */
  val iconName: String
    get() = "place-$key"

  companion object {
    /** The first zoom with places, where the tiles have all of them. */
    const val MIN_ZOOM = 14.0

    /** The first zoom with the places' names. */
    const val NAME_ZOOM = 16.0

    /** The id of the map layer with the places' icons, from [MIN_ZOOM] to [NAME_ZOOM]. */
    const val LAYER_ID = "places"

    /**
     * The id of the map layer with the places' icons and names, from [NAME_ZOOM]. A layer of its
     * own, as the map's zoom decides a layer's zoom range, while a zoom step in the text is
     * evaluated at the tile's zoom, a level late with the base map's 512-pixel tiles.
     */
    const val NAMED_LAYER_ID = "places-named"

    /** The source layer of the base map's places. */
    const val SOURCE_LAYER = "poi"

    /** The base map's layers that draw places in its own style. */
    val BASE_MAP_LAYERS = listOf("poi_r1", "poi_r7", "poi_r20", "poi_transit")

    /** The kind of a place with the tiles' [className] and [subclass], `null` for other places. */
    fun of(className: String?, subclass: String?): PlaceKind? = entries.firstOrNull {
      subclass in it.matches[className].orEmpty()
    }

    private fun quoted(value: String) = "\"$value\""

    private fun list(values: Iterable<String>) = values.joinToString(", ", "[", "]") { quoted(it) }

    /** True for a place of any kind. */
    private val anyKind: String
      get() {
        val conditions =
          entries
            .flatMap { it.matches.entries }
            .groupBy({ it.key }, { it.value })
            .map { (className, subclasses) ->
              """["all", ["==", ["get", "class"], ${quoted(className)}], """ +
                """["match", ["get", "subclass"], ${list(subclasses.flatten().sorted())}, true, false]]"""
            }
        return """["any", ${conditions.joinToString(", ")}]"""
      }

    /** The filter of the places layer: points of any kind. */
    val placesFilter: String
      get() =
        """["all", ["match", ["geometry-type"], ["MultiPoint", "Point"], true, false], $anyKind]"""

    /**
     * The filter to combine with each of the [BASE_MAP_LAYERS]' own filters, so they leave our
     * kinds out.
     */
    val notPlacesFilter: String
      get() = """["!", $anyKind]"""

    /** The icon image per place, by class. */
    val placesIcon: String
      get() {
        val cases = entries.flatMap { kind ->
          kind.matches.keys.map { "${quoted(it)}, ${quoted(kind.iconName)}" }
        }
        return """["match", ["get", "class"], ${cases.joinToString(", ")}, ""]"""
      }

    /** The text per place in the [NAMED_LAYER_ID]: its local name. */
    val placesText: String
      get() = """["coalesce", ["get", "name"], ""]"""
  }
}
