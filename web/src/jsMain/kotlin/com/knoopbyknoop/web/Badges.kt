package com.knoopbyknoop.web

import com.knoopbyknoop.core.KnotPlate
import com.knoopbyknoop.core.PlaceKind
import kotlin.math.PI
import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

/** The map's images are drawn at twice their size, sharp on high-density screens. */
const val PIXEL_RATIO = 2

private const val GREY_RING = "#9AA19C"
private const val GREY_TEXT = "#6B736D"
private const val DARK_DISC = "#2A2C2A"
private const val DARK_RING = "#8A928C"
private const val DARK_TEXT = "#C9CEC9"
private const val INK = "#1B1F1C"

/** The glyphs of Material Symbols Rounded (Apache 2.0) in their 960 × 960 box, as the apps have. */
private val GLYPHS =
  mapOf(
    PlaceKind.FOOD to
      "M280-600v-240q0-17 11.5-28.5T320-880q17 0 28.5 11.5T360-840v240h40v-240q0-17 11.5-28.5T440-880q17 0 28.5 11.5T480-840v240q0 56-34.5 98T360-446v326q0 17-11.5 28.5T320-80q-17 0-28.5-11.5T280-120v-326q-51-14-85.5-56T160-600v-240q0-17 11.5-28.5T200-880q17 0 28.5 11.5T240-840v240h40Zm400 200h-80q-17 0-28.5-11.5T560-440v-240q0-70 51.5-135T718-880q18 0 30 14t12 33v713q0 17-11.5 28.5T720-80q-17 0-28.5-11.5T680-120v-280Z",
    PlaceKind.SUPERMARKET to
      "M280-80q-33 0-56.5-23.5T200-160q0-33 23.5-56.5T280-240q33 0 56.5 23.5T360-160q0 33-23.5 56.5T280-80Zm400 0q-33 0-56.5-23.5T600-160q0-33 23.5-56.5T680-240q33 0 56.5 23.5T760-160q0 33-23.5 56.5T680-80ZM208-800h590q23 0 35 20.5t1 41.5L692-482q-11 20-29.5 31T622-440H324l-44 80h440q17 0 28.5 11.5T760-320q0 17-11.5 28.5T720-280H280q-45 0-68-39.5t-2-78.5l54-98-144-304H80q-17 0-28.5-11.5T40-840q0-17 11.5-28.5T80-880h65q11 0 21 6t15 17l27 57Z",
    PlaceKind.BICYCLE to
      "M200-160q-85 0-142.5-57.5T0-360q0-85 58.5-142.5T200-560q77 0 129.5 46T396-400h26l-72-200h-30q-17 0-28.5-11.5T280-640q0-17 11.5-28.5T320-680h120q17 0 28.5 11.5T480-640q0 17-11.5 28.5T440-600h-4l14 40h192l-58-160h-64q-17 0-28.5-11.5T480-760q0-17 11.5-28.5T520-800h64q26 0 46.5 14t29.5 38l68 186h32q83 0 141.5 58.5T960-362q0 84-58 143t-142 59q-72 0-126.5-45T564-320H396q-14 69-68 114.5T200-160Zm112-160v-80h-72q-17 0-28.5 11.5T200-360q0 17 11.5 28.5T240-320h72Zm196-80h56q5-23 13.5-43t22.5-37H478l30 80Zm174-52 24 68q5 16 20.5 23t31.5 1q16-6 23-21t1-31l-26-68-74 28Z",
    PlaceKind.RAILWAY to
      "M160-340v-380q0-53 27.5-84.5t72.5-48q45-16.5 102.5-22T480-880q66 0 124.5 5.5t102 22q43.5 16.5 68.5 48t25 84.5v380q0 59-40.5 99.5T660-200l20 20q17 17 8 38.5T655-120q-7 0-13.5-2.5T630-130l-70-70H400l-70 70q-5 5-11.5 7.5T305-120q-23 0-32.5-21.5T280-180l20-20q-59 0-99.5-40.5T160-340Zm80-220h200v-120H240v120Zm280 0h200v-120H520v120ZM340-320q26 0 43-17t17-43q0-26-17-43t-43-17q-26 0-43 17t-17 43q0 26 17 43t43 17Zm280 0q26 0 43-17t17-43q0-26-17-43t-43-17q-26 0-43 17t-17 43q0 26 17 43t43 17Z",
    PlaceKind.WATER to
      "M480-80q-137 0-228.5-94T160-408q0-62 28-124t70-119q42-57 91-107t91-87q8-8 18.5-11.5T480-860q11 0 21.5 3.5T520-845q42 37 91 87t91 107q42 57 70 119t28 124q0 140-91.5 234T480-80Zm11-120q12-1 20.5-9.5T520-230q0-14-9-22.5t-23-7.5q-41 3-87-22.5T343-375q-2-11-10.5-18t-19.5-7q-14 0-23 10.5t-6 24.5q17 91 80 130t127 35Z",
    PlaceKind.CAMPSITE to
      "M80-120v-120q0-13 4-25t12-23l334-450-46-62q-5-7-7-14.5t-1-15q1-7.5 5-14.5t11-12q14-10 30-8t26 16l32 43 32-43q10-14 26-16t30 8q14 10 16 26t-8 30l-46 62 334 450q8 11 12 23t4 25v120q0 17-11.5 28.5T840-80H120q-17 0-28.5-11.5T80-120Zm249-40h302L480-371 329-160Z",
    PlaceKind.LODGING to
      "M80-200q-17 0-28.5-11.5T40-240v-520q0-17 11.5-28.5T80-800q17 0 28.5 11.5T120-760v360h320v-240q0-33 23.5-56.5T520-720h240q66 0 113 47t47 113v320q0 17-11.5 28.5T880-200q-17 0-28.5-11.5T840-240v-80H120v80q0 17-11.5 28.5T80-200Zm200-240q-50 0-85-35t-35-85q0-50 35-85t85-35q50 0 85 35t35 85q0 50-35 85t-85 35Z",
  )

/** [colour] (ARGB) as CSS. */
fun cssColour(colour: Long): String = "#" + (colour and 0xFFFFFF).toString(16).padStart(6, '0')

private fun canvasOf(size: Double): Pair<HTMLCanvasElement, CanvasRenderingContext2D> {
  val canvas = document.createElement("canvas") as HTMLCanvasElement
  canvas.width = (size * PIXEL_RATIO).toInt()
  canvas.height = (size * PIXEL_RATIO).toInt()
  val context = canvas.getContext("2d") as CanvasRenderingContext2D
  context.scale(PIXEL_RATIO.toDouble(), PIXEL_RATIO.toDouble())
  return canvas to context
}

private fun CanvasRenderingContext2D.disc(x: Double, y: Double, radius: Double, colour: String) {
  beginPath()
  arc(x, y, radius, 0.0, 2 * PI)
  fillStyle = colour
  fill()
}

private fun CanvasRenderingContext2D.ring(
  x: Double,
  y: Double,
  radius: Double,
  width: Double,
  colour: String,
) {
  beginPath()
  arc(x, y, radius, 0.0, 2 * PI)
  strokeStyle = colour
  lineWidth = width
  stroke()
}

/**
 * A place kind's disc with its white edge and glyph, see `map-planning` "Places useful on a ride".
 */
private fun CanvasRenderingContext2D.placeDisc(
  kind: PlaceKind,
  center: Double,
  radius: Double,
  edge: Double,
) {
  disc(center, center, radius, "#FFFFFF")
  disc(center, center, radius - edge, cssColour(kind.colour))
  val side = 2 * (radius - edge) * 0.66
  save()
  translate(center - side / 2, center - side / 2)
  scale(side / 960, side / 960)
  translate(0.0, 960.0)
  fillStyle = "#FFFFFF"
  asDynamic().fill(newPath2D(GLYPHS.getValue(kind)))
  restore()
}

private fun newPath2D(data: String): dynamic = js("new Path2D(data)")

/**
 * The corner symbol of a waypoint, white on a dark disc: ▶ start, flag stop, ■ end, ↻ round trip.
 */
private fun CanvasRenderingContext2D.cornerSymbol(
  role: String,
  x: Double,
  y: Double,
  radius: Double,
) {
  disc(x, y, radius, INK)
  fillStyle = "#FFFFFF"
  strokeStyle = "#FFFFFF"
  val r = radius * 0.5
  when (role) {
    "start" -> {
      beginPath()
      moveTo(x - r * 0.7, y - r)
      lineTo(x + r, y)
      lineTo(x - r * 0.7, y + r)
      closePath()
      fill()
    }
    "end" -> fillRect(x - r * 0.8, y - r * 0.8, r * 1.6, r * 1.6)
    "stop" -> flag(x - r * 0.8, y - r, r * 2)
    "round_trip" -> {
      beginPath()
      lineWidth = radius * 0.28
      arc(x, y, r * 0.9, -PI * 0.3, PI * 1.4)
      stroke()
      beginPath()
      moveTo(x + r * 0.95, y - r * 1.05)
      lineTo(x + r * 0.95, y - r * 0.05)
      lineTo(x - r * 0.05, y - r * 0.4)
      closePath()
      fill()
    }
  }
}

/** A flag, pole on the left, fitting a square of [size] at [x], [y]. */
private fun CanvasRenderingContext2D.flag(x: Double, y: Double, size: Double) {
  fillRect(x, y, size * 0.14, size)
  beginPath()
  moveTo(x + size * 0.14, y)
  lineTo(x + size * 0.95, y + size * 0.28)
  lineTo(x + size * 0.14, y + size * 0.56)
  closePath()
  fill()
}

/**
 * The map image called [name], as the map's symbol layers name them (see `MapFeatures`):
 * `knot-<ref>` for a knot off the route, `route-<role>-<plate>-<ref>-<place>` for one on it,
 * `place-<kind>` for a place, `offer-flag` for the clicked spot. Knots look as in the apps, see
 * `map-planning` "Route knots stand out". `null` for a name it doesn't know.
 */
fun mapImage(name: String, dark: Boolean): HTMLCanvasElement? {
  if (name.startsWith("place-")) {
    val kind = PlaceKind.ofKey(name.removePrefix("place-")) ?: return null
    val size = 24.0
    val (canvas, context) = canvasOf(size)
    context.placeDisc(kind, size / 2, size / 2, 1.5)
    return canvas
  }
  if (name == "offer-flag") return badge(null, "", KnotPlate.NONE, null, dark, offer = true)
  if (name.startsWith("knot-"))
    return badge(null, name.removePrefix("knot-"), KnotPlate.NONE, null, dark)
  if (name.startsWith("route-")) {
    val parts = name.removePrefix("route-").split("-")
    if (parts.size < 4) return null
    val role = parts[0]
    val plate = KnotPlate.ofKey(parts[1])
    val ref = parts.subList(2, parts.size - 1).joinToString("-")
    val place = parts.last().ifEmpty { null }
    return badge(role, ref, plate, place, dark)
  }
  return null
}

private fun badge(
  role: String?,
  ref: String,
  plate: KnotPlate,
  place: String?,
  dark: Boolean,
  offer: Boolean = false,
): HTMLCanvasElement {
  val large = role == "start" || role == "end" || role == "round_trip"
  val plateSize = if (large) 34.0 else 27.0
  val room = 4.5
  val size = plateSize + 2 * room
  val (canvas, context) = canvasOf(size)
  val center = size / 2
  val radius = plateSize / 2 - 1.5
  val plateColour = cssColour(plate.colour)
  val diameter = 2 * radius
  fun corner() {
    if (role != null && role != "passed") {
      context.cornerSymbol(
        role,
        center + diameter * 0.38,
        center - diameter * 0.38,
        diameter * 0.25,
      )
    }
  }
  PlaceKind.ofKey(place)?.let { kind ->
    context.placeDisc(kind, center, radius, 1.5)
    corner()
    return canvas
  }
  // fill, ring, ring width, inner ring, text
  val style =
    when {
      offer -> listOf("#FFFFFF", INK, 2.5, false, INK)
      role == null ->
        if (dark) listOf(DARK_DISC, DARK_RING, 2.0, false, DARK_TEXT)
        else listOf("#FFFFFF", GREY_RING, 2.0, false, GREY_TEXT)
      role == "passed" -> listOf("#FFFFFF", plateColour, 2.5, false, plateColour)
      else -> listOf(plateColour, null, 0.0, plate.innerRing, "#FFFFFF")
    }
  context.disc(center, center, radius, style[0] as String)
  (style[1] as String?)?.let {
    context.ring(center, center, radius - (style[2] as Double) / 2, style[2] as Double, it)
  }
  if (style[3] as Boolean) context.ring(center, center, radius * 0.8, radius * 0.11, "#FFFFFF")
  context.fillStyle = style[4] as String
  if (ref.isEmpty()) {
    context.flag(center - radius * 0.45, center - radius * 0.5, radius)
  } else {
    val fontSize = (if (ref.length > 2) 8.5 else 11.0) * (if (large) 1.25 else 1.0)
    context.font = "bold ${fontSize}px system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
    context.asDynamic().textAlign = "center"
    context.asDynamic().textBaseline = "middle"
    context.fillText(ref, center, center + 0.5)
  }
  corner()
  return canvas
}
