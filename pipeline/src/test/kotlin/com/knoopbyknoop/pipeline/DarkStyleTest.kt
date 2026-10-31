package com.knoopbyknoop.pipeline

import com.knoopbyknoop.pipeline.DarkStyle.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Covers the dark map of `map-planning` "Light and dark appearance" ("Details on the dark map"), on
 * a copy of OpenFreeMap's liberty style of 2026-10-31.
 */
class DarkStyleTest {

  private val liberty =
    Json.parseToJsonElement(javaClass.getResource("/liberty.json")!!.readText()).jsonObject

  private val dark = DarkStyle.darken(liberty)

  private fun layers(style: JsonObject) = (style["layers"] as JsonArray).map { it.jsonObject }

  private fun layer(id: String) = layers(dark).single { it["id"]!!.jsonPrimitive.content == id }

  private fun color(id: String, property: String): Color =
    assertNotNull(Color.parse(layer(id)["paint"]!!.jsonObject[property]!!.jsonPrimitive.content))

  @Test
  fun `all layers are kept`() {
    assertEquals(
      layers(liberty).map { it["id"] to it["type"] },
      layers(dark).map { it["id"] to it["type"] },
    )
    assertEquals(liberty["sources"], dark["sources"])
    assertEquals(liberty["sprite"], dark["sprite"])
  }

  @Test
  fun `the land is dark`() {
    assertTrue(color("background", "background-color").lightness < 0.15)
  }

  @Test
  fun `water is blue`() {
    val water = color("water", "fill-color")

    assertTrue(water.hue in 190.0..240.0, "hue ${water.hue}")
    assertTrue(water.saturation > 0.3, "saturation ${water.saturation}")
    assertTrue(water.lightness in 0.2..0.35, "lightness ${water.lightness}")
    assertTrue(color("waterway_river", "line-color").hue in 190.0..240.0)
  }

  @Test
  fun `parks and woods are green`() {
    for (id in listOf("park", "landcover_wood", "landcover_grass")) {
      val green = color(id, "fill-color")
      assertTrue(green.hue in 60.0..170.0, "$id hue ${green.hue}")
      assertTrue(green.lightness < 0.25, "$id lightness ${green.lightness}")
    }
  }

  @Test
  fun `roads and paths stand out from the land and their casings`() {
    val land = color("background", "background-color").lightness
    val road = color("road_minor", "line-color").lightness
    val path = color("road_path_pedestrian", "line-color").lightness
    val casing = color("road_minor_casing", "line-color").lightness

    assertTrue(road > land + 0.4, "road $road, land $land")
    assertTrue(path > land + 0.4, "path $path, land $land")
    assertTrue(casing < road - 0.3, "casing $casing, road $road")
  }

  @Test
  fun `labels are light on dark halos`() {
    assertTrue(color("poi_r1", "text-color").lightness > 0.65)
    assertTrue(color("poi_r1", "text-halo-color").lightness < 0.15)
  }

  @Test
  fun `the relief and the icons are dimmed`() {
    assertEquals(
      JsonPrimitive(0.35),
      layer("natural_earth")["paint"]!!.jsonObject["raster-brightness-max"],
    )
    assertEquals(JsonPrimitive(0.8), layer("poi_r1")["paint"]!!.jsonObject["icon-opacity"])
  }

  @Test
  fun `street names have a dark halo`() {
    for (id in listOf("highway-name-minor", "highway-name-major", "highway-name-path")) {
      val paint = layer(id)["paint"]!!.jsonObject
      val halo = assertNotNull(Color.parse(paint["text-halo-color"]!!.jsonPrimitive.content))
      assertTrue(halo.lightness < 0.15, "$id halo $halo")
      assertTrue(paint["text-halo-width"]!!.jsonPrimitive.content.toDouble() >= 1.5, id)
      assertTrue(color(id, "text-color").lightness > halo.lightness + 0.5, id)
    }
  }

  @Test
  fun `patterns are dimmed`() {
    val patterned =
      layers(dark).filter { it["paint"]?.jsonObject?.containsKey("fill-pattern") == true }

    assertTrue(patterned.isNotEmpty())
    assertTrue(patterned.all { it["paint"]!!.jsonObject["fill-opacity"] == JsonPrimitive(0.25) })
  }

  @Test
  fun `colors inside expressions are mapped`() {
    val outline = layer("building")["paint"]!!.jsonObject["fill-outline-color"] as JsonArray
    val colors = outline.mapNotNull {
      (it as? JsonPrimitive)?.takeIf { it.isString }?.content?.let(Color::parse)
    }

    assertEquals(2, colors.size)
    assertTrue(colors.all { it.lightness < 0.3 }, colors.toString())
  }

  @Test
  fun `colors are read in all forms`() {
    assertEquals(Color(0.0, 0.0, 1.0, 1.0), Color.parse("#fff"))
    assertEquals(Color(0.0, 0.0, 1.0, 1.0), Color.parse("#ffffff"))
    assertEquals(Color(0.0, 1.0, 0.5, 0.5), Color.parse("rgba(255, 0, 0, 0.5)"))
    assertEquals(Color(98.0, 0.61, 0.72, 0.7), Color.parse("hsla(98,61%,72%,0.7)"))
    assertEquals(null, Color.parse("zoom"))
  }
}
