package com.knoopbyknoop.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.knoopbyknoop.core.PlaceKind

/** The disc's diameter, a little smaller than a knot badge's plate (56). */
private const val PLACE_DISC = 42

/** The white edge around the disc, so it stands out on the light and the dark map. */
private const val PLACE_EDGE = 3f

/** The glyph's size within the disc. */
private const val PLACE_GLYPH = 28

/**
 * The glyph of each kind, Material Symbols Rounded, see `design.md` of `highlight-places-on-map`.
 */
private val GLYPHS =
  mapOf(
    PlaceKind.FOOD to R.drawable.ic_place_food,
    PlaceKind.SUPERMARKET to R.drawable.ic_place_supermarket,
    PlaceKind.BICYCLE to R.drawable.ic_place_bicycle,
    PlaceKind.RAILWAY to R.drawable.ic_place_railway,
    PlaceKind.WATER to R.drawable.ic_place_water,
  )

/**
 * The icon of a highlighted place: a white glyph on a disc in its kind's colour with a white edge,
 * see `map-planning` "Places useful on a ride".
 */
fun placeBitmap(context: Context, kind: PlaceKind): Bitmap {
  val size = PLACE_DISC + 2 * PLACE_EDGE.toInt()
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val center = size / 2f
  canvas.drawCircle(
    center,
    center,
    size / 2f,
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE },
  )
  canvas.drawCircle(
    center,
    center,
    PLACE_DISC / 2f,
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = kind.colour.toInt() },
  )
  val glyph = ContextCompat.getDrawable(context, GLYPHS.getValue(kind))!!.mutate()
  val inset = (size - PLACE_GLYPH) / 2
  glyph.setBounds(inset, inset, inset + PLACE_GLYPH, inset + PLACE_GLYPH)
  glyph.draw(canvas)
  return bitmap
}
