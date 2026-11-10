package com.knoopbyknoop.core

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

class PlaceKindTest {

  /** A place in the base map's tiles. */
  private data class Place(
    val className: String,
    val subclass: String,
    val geometry: String = "Point",
    val name: String? = null,
  )

  /** Evaluates the few MapLibre expressions the place filters use, for [place] at [zoom]. */
  private fun evaluate(expression: JsonElement, place: Place, zoom: Double = 15.0): Any? {
    if (expression is JsonPrimitive) {
      return expression.booleanOrNull ?: expression.content.toDoubleOrNull() ?: expression.content
    }
    val array = expression as JsonArray
    val operator = array[0].jsonPrimitive.content
    fun argument(index: Int) = evaluate(array[index], place, zoom)
    return when (operator) {
      "all" -> array.drop(1).all { evaluate(it, place, zoom) == true }
      "any" -> array.drop(1).any { evaluate(it, place, zoom) == true }
      "!" -> argument(1) != true
      "==" -> argument(1) == argument(2)
      "get" ->
        when (array[1].jsonPrimitive.content) {
          "class" -> place.className
          "subclass" -> place.subclass
          "name" -> place.name
          else -> null
        }
      "geometry-type" -> place.geometry
      "zoom" -> zoom
      "coalesce" -> array.drop(1).firstNotNullOfOrNull { evaluate(it, place, zoom) }
      "step" -> {
        val input = argument(1) as Double
        var result = argument(2)
        for (index in 3 until array.size step 2) {
          if (input >= (argument(index) as Double)) result = argument(index + 1)
        }
        result
      }
      "match" -> {
        val input = argument(1)
        for (index in 2 until array.size - 1 step 2) {
          val label = array[index]
          val labels =
            if (label is JsonArray) label.map { it.jsonPrimitive.content }
            else listOf(label.jsonPrimitive.content)
          if (input in labels) return argument(index + 1)
        }
        argument(array.size - 1)
      }
      else -> error("Unsupported operator $operator")
    }
  }

  private fun shown(place: Place) =
    evaluate(Json.parseToJsonElement(PlaceKind.placesFilter), place) == true

  private fun icon(place: Place) = evaluate(Json.parseToJsonElement(PlaceKind.placesIcon), place)

  private val town =
    mapOf(
      Place("restaurant", "restaurant") to PlaceKind.FOOD,
      Place("cafe", "cafe") to PlaceKind.FOOD,
      Place("fast_food", "fast_food") to PlaceKind.FOOD,
      Place("bar", "bar") to PlaceKind.FOOD,
      Place("beer", "pub") to PlaceKind.FOOD,
      Place("beer", "biergarten") to PlaceKind.FOOD,
      Place("ice_cream", "ice_cream") to PlaceKind.FOOD,
      Place("grocery", "supermarket") to PlaceKind.SUPERMARKET,
      Place("bicycle", "bicycle") to PlaceKind.BICYCLE,
      Place("railway", "station") to PlaceKind.RAILWAY,
      Place("drinking_water", "drinking_water") to PlaceKind.WATER,
      Place("toilets", "toilets") to PlaceKind.WATER,
    )

  private val others =
    listOf(
      Place("pharmacy", "pharmacy"),
      Place("bank", "bank"),
      Place("bicycle_rental", "bicycle_rental"),
      Place("bicycle_parking", "bicycle_parking"),
      Place("bus", "bus_station"),
      Place("railway", "tram_stop"),
      Place("grocery", "deli"),
      Place("bar", "nightclub"),
    )

  @Test
  fun `places in a town are shown with their kind's icon`() {
    for ((place, kind) in town) {
      assertEquals(kind, PlaceKind.of(place.className, place.subclass), "$place")
      assertTrue(shown(place), "$place")
      assertEquals(kind.iconName, icon(place), "$place")
    }
  }

  @Test
  fun `other places are left to the base map`() {
    for (place in others) {
      assertNull(PlaceKind.of(place.className, place.subclass), "$place")
      assertFalse(shown(place), "$place")
    }
  }

  @Test
  fun `places drawn as areas are not shown twice`() {
    assertFalse(shown(Place("grocery", "supermarket", geometry = "Polygon")))
  }

  // Scenario: Other places unchanged
  // Scenario: Shown once
  @Test
  fun `the base map leaves our kinds out and keeps the others`() {
    val notPlaces = Json.parseToJsonElement(PlaceKind.notPlacesFilter)
    for (place in town.keys) assertEquals(false, evaluate(notPlaces, place), "$place")
    for (place in others) assertEquals(true, evaluate(notPlaces, place), "$place")
  }

  // Scenario: Names close up
  // Scenario: Places useful on a ride / Zoomed out
  @Test
  fun `names only close up`() {
    val text = Json.parseToJsonElement(PlaceKind.placesText)
    val cafe = Place("cafe", "cafe", name = "Café am Markt")
    assertEquals("Café am Markt", evaluate(text, cafe))
    assertEquals("", evaluate(text, cafe.copy(name = null)))
    assertEquals(14.0, PlaceKind.MIN_ZOOM)
    assertEquals(16.0, PlaceKind.NAME_ZOOM)
  }

  private fun channels(colour: Long) =
    listOf(16, 8, 0).map { ((colour shr it) and 0xFF).toDouble() / 255 }

  private fun luminance(colour: Long) =
    channels(colour)
      .map { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
      .let { 0.2126 * it[0] + 0.7152 * it[1] + 0.0722 * it[2] }

  private fun distance(first: Long, second: Long) =
    sqrt(channels(first).zip(channels(second)).sumOf { (a, b) -> (a - b).pow(2) })

  /** The colour's hue in degrees. */
  private fun hue(colour: Long): Double {
    val (red, green, blue) = channels(colour)
    val max = maxOf(red, green, blue)
    val range = max - minOf(red, green, blue)
    if (range == 0.0) return 0.0
    val sector =
      when (max) {
        red -> ((green - blue) / range).mod(6.0)
        green -> (blue - red) / range + 2
        else -> (red - green) / range + 4
      }
    return sector * 60
  }

  private fun hueDifference(first: Long, second: Long): Double {
    val difference = kotlin.math.abs(hue(first) - hue(second))
    return minOf(difference, 360 - difference)
  }

  /** Whether two colours can be told apart: far apart as colours, or of clearly different hues. */
  private fun distinct(first: Long, second: Long) =
    distance(first, second) > 0.25 || hueDifference(first, second) > 25

  // Scenario: Distinct from knots
  @Test
  fun `place discs stay clear of the knots' sign colours and the route`() {
    // The route line's orange, as in the apps' map code.
    val route = 0xFFF28C28
    val knotColours =
      SignRegion.entries.map { KnotPlate.of(it).colour } + KnotPlate.NONE.colour + route
    for (kind in PlaceKind.entries) {
      for (knot in knotColours) {
        assertTrue(distinct(kind.colour, knot), "$kind is close to ${knot.toString(16)}")
      }
      val contrast = 1.05 / (luminance(kind.colour) + 0.05)
      assertTrue(contrast >= 3.0, "$kind: white glyph contrast $contrast")
    }
  }

  @Test
  fun `each kind has its own colour and icon`() {
    for (first in PlaceKind.entries) {
      for (second in PlaceKind.entries - first) {
        assertTrue(distinct(first.colour, second.colour), "$first is close to $second")
      }
    }
    assertEquals(PlaceKind.entries.size, PlaceKind.entries.map { it.iconName }.toSet().size)
  }
}
