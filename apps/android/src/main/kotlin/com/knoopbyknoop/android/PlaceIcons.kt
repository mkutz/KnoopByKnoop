package com.knoopbyknoop.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import com.knoopbyknoop.core.PlaceKind

/** The disc's diameter, a little smaller than a knot badge's plate (56). */
private const val PLACE_DISC = 42

/** The white edge around the disc, so it stands out on the light and the dark map. */
private const val PLACE_EDGE = 3f

/** The glyph's size within the disc, per disc diameter. */
private const val PLACE_GLYPH = 0.66f

/**
 * The glyph of each kind: the path data of Material Symbols Rounded (Apache 2.0) in its 960 × 960
 * box, the same as the `ic_place_*` drawables, so badges can draw it without a `Context`.
 */
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

/** The glyph of [kind] as a path filling a square of [side] at the origin. */
fun placeGlyphPath(kind: PlaceKind, side: Float): Path {
  val path = PathParser.createPathFromPathData(GLYPHS.getValue(kind))
  path.transform(
    Matrix().apply {
      setTranslate(0f, 960f)
      postScale(side / 960f, side / 960f)
    }
  )
  return path
}

/** Draws [kind]'s disc with its white edge and glyph, centred at [center] with [radius]. */
fun drawPlaceDisc(canvas: Canvas, kind: PlaceKind, center: Float, radius: Float, edge: Float) {
  canvas.drawCircle(
    center,
    center,
    radius,
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE },
  )
  canvas.drawCircle(
    center,
    center,
    radius - edge,
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = kind.colour.toInt() },
  )
  val side = 2 * (radius - edge) * PLACE_GLYPH
  canvas.save()
  canvas.translate(center - side / 2, center - side / 2)
  canvas.drawPath(
    placeGlyphPath(kind, side),
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE },
  )
  canvas.restore()
}

/**
 * The icon of a highlighted place: a white glyph on a disc in its kind's colour with a white edge,
 * see `map-planning` "Places useful on a ride".
 */
fun placeBitmap(kind: PlaceKind): Bitmap {
  val size = PLACE_DISC + 2 * PLACE_EDGE.toInt()
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  drawPlaceDisc(Canvas(bitmap), kind, size / 2f, size / 2f, PLACE_EDGE)
  return bitmap
}
