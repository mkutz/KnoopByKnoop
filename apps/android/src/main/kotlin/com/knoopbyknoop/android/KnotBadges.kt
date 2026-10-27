package com.knoopbyknoop.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoopbyknoop.core.KnotRole

/** The colors of the app icon, see `design/icon`. */
val Green = Color(0xFF2E9E5B)
val Orange = Color(0xFFF28C28)
private val Dark = Color(0xFF263238)

/**
 * How a knot is drawn, on the map and in the sheet alike, see the design of `improve-planner-map`.
 * `null` is a knot of the network that is not on the route.
 */
private class BadgeStyle(val fill: Color, val ring: Color, val text: Color, val large: Boolean)

private fun styleOf(role: KnotRole?): BadgeStyle =
  when (role) {
    null -> BadgeStyle(Color.White, Green, Green, large = false)
    KnotRole.PASSED -> BadgeStyle(Color.White, Orange, Orange, large = false)
    KnotRole.STOP -> BadgeStyle(Orange, Color.White, Color.White, large = false)
    KnotRole.START -> BadgeStyle(Green, Color.White, Color.White, large = true)
    KnotRole.END -> BadgeStyle(Dark, Color.White, Color.White, large = true)
    // Drawn as two halves, start green and end dark, see badgeBitmap and KnotBadge.
    KnotRole.ROUND_TRIP -> BadgeStyle(Green, Color.White, Color.White, large = true)
  }

/** The map image name of a badge; the map's symbol layers build the same names from GeoJSON. */
fun badgeName(role: KnotRole?, ref: String) =
  if (role == null) "knot-$ref" else "route-${role.name.lowercase()}-$ref"

/** A round badge with the knot's number, drawn locally so numbers show without network access. */
fun badgeBitmap(role: KnotRole?, ref: String): Bitmap {
  val style = styleOf(role)
  val size = if (style.large) 72 else 56
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val radius = size / 2f - 4
  canvas.drawCircle(
    size / 2f,
    size / 2f,
    radius,
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.fill.toArgb() },
  )
  if (role == KnotRole.ROUND_TRIP) {
    // The right half is the end's color: start and end of a round trip in one badge.
    val circle =
      RectF(size / 2f - radius, size / 2f - radius, size / 2f + radius, size / 2f + radius)
    canvas.drawArc(
      circle,
      -90f,
      180f,
      true,
      Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Dark.toArgb() },
    )
  }
  canvas.drawCircle(
    size / 2f,
    size / 2f,
    radius,
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = style.ring.toArgb()
      this.style = Paint.Style.STROKE
      strokeWidth = if (role == KnotRole.PASSED) 7f else 5f
    },
  )
  val text =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = style.text.toArgb()
      textAlign = Paint.Align.CENTER
      typeface = Typeface.DEFAULT_BOLD
      textSize = (if (ref.length > 2) 18f else 24f) * (if (style.large) 1.25f else 1f)
    }
  canvas.drawText(ref, size / 2f, size / 2f - (text.descent() + text.ascent()) / 2, text)
  return bitmap
}

/** An arrow pointing along the line it is placed on, for the riding direction. */
fun arrowBitmap(): Bitmap {
  val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
  val path =
    Path().apply {
      moveTo(14f, 10f)
      lineTo(34f, 24f)
      lineTo(14f, 38f)
    }
  Canvas(bitmap)
    .drawPath(
      path,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      },
    )
  return bitmap
}

/** The same badge as on the map, for the sheet. */
@Composable
fun KnotBadge(ref: String, role: KnotRole?, modifier: Modifier = Modifier) {
  val style = styleOf(role)
  val size = if (style.large) 34.dp else 28.dp
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .size(size)
        .background(
          if (role == KnotRole.ROUND_TRIP) {
            Brush.horizontalGradient(0.5f to Green, 0.5f to Dark)
          } else {
            SolidColor(style.fill)
          },
          CircleShape,
        )
        .border(if (role == KnotRole.PASSED) 3.dp else 2.dp, style.ring, CircleShape),
  ) {
    Text(
      ref,
      color = style.text,
      fontWeight = FontWeight.Bold,
      fontSize = (if (ref.length > 2) 10 else 12).sp * (if (style.large) 1.2f else 1f),
    )
  }
}
