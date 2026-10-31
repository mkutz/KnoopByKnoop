package com.knoopbyknoop.pipeline

import java.net.URI
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The dark map: a dark variant of OpenFreeMap's light "liberty" style with all its layers, see
 * `map-planning` "Light and dark appearance" and the design of `improve-dark-map`.
 */
object DarkStyle {

  /** OpenFreeMap's light style, which the light map loads and the dark one is made from. */
  const val LIBERTY_URL = "https://tiles.openfreemap.org/styles/liberty"

  private val json = Json { prettyPrint = true }

  private const val PATTERN_OPACITY = 0.25

  private const val LINE_LABEL_HALO = "hsla(30, 8%, 9%, 0.95)"
  private const val LINE_LABEL_HALO_WIDTH = 1.5

  /** Reads liberty from [source], a URL or a file, and writes its dark variant to [target]. */
  fun generate(source: String, target: Path) {
    val text =
      if (source.startsWith("http")) URI(source).toURL().readText() else Path.of(source).readText()
    target.writeText(
      json.encodeToString(
        JsonElement.serializer(),
        darken(Json.parseToJsonElement(text).jsonObject),
      )
    )
  }

  /** The dark variant of [style]: same sources, layers, filters and layout, dark colors. */
  fun darken(style: JsonObject): JsonObject {
    val layers = (style["layers"] as JsonArray).map { darkenLayer(it.jsonObject) }
    return JsonObject(
      style +
        mapOf(
          "name" to JsonPrimitive("Knoop by Knoop dark, generated from OpenFreeMap Liberty"),
          "layers" to JsonArray(layers),
        )
    )
  }

  /** How a layer's colors are mapped. */
  internal enum class Kind {
    BACKGROUND,
    AREA,
    WATER,
    LINE,
    CASING,
    WATER_LINE,
    LABEL,
  }

  private fun darkenLayer(layer: JsonObject): JsonObject {
    val id = layer["id"]?.jsonPrimitive?.content.orEmpty()
    val type = layer["type"]?.jsonPrimitive?.content.orEmpty()
    val sourceLayer = layer["source-layer"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val paint = layer["paint"] as? JsonObject ?: JsonObject(mapOf())
    val darkPaint =
      paint
        .mapValues { (property, value) ->
          if (!property.endsWith("-color")) value
          else darkenValue(value, kindOf(id, type, sourceLayer, property))
        }
        .toMutableMap()
    // Patterns from the shared sprite, e.g. wetland, are made for light land and cannot be
    // recolored: dimmed.
    if (paint.containsKey("fill-pattern"))
      darkPaint["fill-opacity"] = JsonPrimitive(PATTERN_OPACITY)
    when (type) {
      // The shaded relief, made for light land, dimmed.
      "raster" -> darkPaint["raster-brightness-max"] = JsonPrimitive(0.35)
      // The shared sprite's icons are made for light land: a little less bright.
      "symbol" -> {
        val layout = layer["layout"]?.jsonObject
        if (layout?.containsKey("icon-image") == true)
          darkPaint.putIfAbsent("icon-opacity", JsonPrimitive(0.8))
        // Street names lie on the light street lines: a clear dark halo, which liberty does not
        // always set (MapLibre's default halo is transparent).
        if (
          layout?.containsKey("text-field") == true &&
            (layout["symbol-placement"] as? JsonPrimitive)?.contentOrNull == "line"
        ) {
          darkPaint["text-halo-color"] = JsonPrimitive(LINE_LABEL_HALO)
          val width =
            (paint["text-halo-width"] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() ?: 0.0
          darkPaint["text-halo-width"] = JsonPrimitive(max(width, LINE_LABEL_HALO_WIDTH))
        }
      }
    }
    return JsonObject(layer + ("paint" to JsonObject(darkPaint)))
  }

  private fun kindOf(id: String, type: String, sourceLayer: String, property: String): Kind {
    val water = sourceLayer == "water" || "water" in id
    return when {
      property == "text-color" -> Kind.LABEL
      property == "text-halo-color" || property == "icon-halo-color" -> Kind.BACKGROUND
      type == "background" -> Kind.BACKGROUND
      type == "line" && (sourceLayer == "waterway" || water) -> Kind.WATER_LINE
      type == "line" && "casing" in id -> Kind.CASING
      type == "line" -> Kind.LINE
      water -> Kind.WATER
      else -> Kind.AREA
    }
  }

  /** Maps every color in [value], a color, a `stops` object or an expression. */
  private fun darkenValue(value: JsonElement, kind: Kind): JsonElement =
    when (value) {
      is JsonPrimitive ->
        if (value.isString)
          Color.parse(value.content)?.let { JsonPrimitive(dark(it, kind).css()) } ?: value
        else value
      is JsonArray -> JsonArray(value.map { darkenValue(it, kind) })
      is JsonObject -> JsonObject(value.mapValues { (_, it) -> darkenValue(it, kind) })
    }

  /** The dark counterpart of [color] in a layer of [kind], see the design. */
  internal fun dark(color: Color, kind: Kind): Color {
    val green = color.saturation > 0.12 && color.hue in 60.0..170.0
    return when (kind) {
      Kind.BACKGROUND -> color.copy(saturation = color.saturation * 0.3, lightness = 0.11)
      Kind.WATER -> color.copy(saturation = min(color.saturation, 0.55), lightness = 0.27)
      Kind.WATER_LINE -> color.copy(saturation = min(color.saturation, 0.55), lightness = 0.36)
      Kind.AREA ->
        if (green) color.copy(saturation = color.saturation * 0.45, lightness = 0.17)
        else
          color.copy(
            saturation = color.saturation * 0.4,
            lightness = 0.09 + (1 - color.lightness) * 0.45,
          )
      Kind.LINE ->
        color.copy(saturation = color.saturation * 0.6, lightness = 0.28 + color.lightness * 0.27)
      Kind.CASING -> color.copy(saturation = color.saturation * 0.4, lightness = 0.16)
      Kind.LABEL ->
        color.copy(saturation = color.saturation * 0.6, lightness = 0.92 - color.lightness * 0.3)
    }
  }

  /** A color in HSL with alpha, each from 0 to 1, the hue in degrees. */
  data class Color(
    val hue: Double,
    val saturation: Double,
    val lightness: Double,
    val alpha: Double,
  ) {

    /** As CSS, e.g. `hsla(221, 55%, 27%, 1)`. */
    fun css(): String =
      "hsla(${hue.roundToInt()}, ${(saturation * 100).roundToInt()}%, ${(lightness.coerceIn(0.0, 1.0) * 100).roundToInt()}%, ${trim(alpha)})"

    private fun trim(value: Double) =
      if (value == value.roundToInt().toDouble()) "${value.roundToInt()}" else "$value"

    companion object {
      private val rgb =
        Regex("""rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+)\s*)?\)""")
      private val hsl =
        Regex("""hsla?\(\s*([\d.]+)\s*,\s*([\d.]+)%\s*,\s*([\d.]+)%\s*(?:,\s*([\d.]+)\s*)?\)""")

      /** `#rgb`, `#rrggbb`, `rgb()`, `rgba()`, `hsl()` or `hsla()`; `null` for anything else. */
      fun parse(text: String): Color? {
        val value = text.trim()
        rgb.matchEntire(value)?.destructured?.let { (r, g, b, a) ->
          return fromRgb(r.toDouble(), g.toDouble(), b.toDouble(), a.toDoubleOrNull() ?: 1.0)
        }
        hsl.matchEntire(value)?.destructured?.let { (h, s, l, a) ->
          return Color(
            h.toDouble(),
            s.toDouble() / 100,
            l.toDouble() / 100,
            a.toDoubleOrNull() ?: 1.0,
          )
        }
        if (value.startsWith("#")) {
          val hex =
            when (value.length) {
              4 -> value.drop(1).map { "$it$it" }.joinToString("")
              7 -> value.drop(1)
              else -> return null
            }
          val channels = hex.chunked(2).map { it.toIntOrNull(16) ?: return null }
          return fromRgb(
            channels[0].toDouble(),
            channels[1].toDouble(),
            channels[2].toDouble(),
            1.0,
          )
        }
        return null
      }

      private fun fromRgb(red: Double, green: Double, blue: Double, alpha: Double): Color {
        val r = red / 255
        val g = green / 255
        val b = blue / 255
        val maximum = max(r, max(g, b))
        val minimum = min(r, min(g, b))
        val lightness = (maximum + minimum) / 2
        val delta = maximum - minimum
        if (delta == 0.0) return Color(0.0, 0.0, lightness, alpha)
        val saturation = delta / (1 - abs(2 * lightness - 1))
        val hue =
          when (maximum) {
            r -> 60 * (((g - b) / delta).mod(6.0))
            g -> 60 * ((b - r) / delta + 2)
            else -> 60 * ((r - g) / delta + 4)
          }
        return Color(hue, saturation, lightness, alpha)
      }
    }
  }
}
