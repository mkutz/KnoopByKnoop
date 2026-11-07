package com.knoopbyknoop.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.KnotPlate
import com.knoopbyknoop.core.KnotRole

/** The colors of the app icon, see `design/icon`. */
val Green = Color(0xFF2E9E5B)
val Orange = Color(0xFFF28C28)

/**
 * The dark of corner symbols and of marks for problems, see `map-planning` "Route knots stand out".
 */
val Ink = Color(0xFF1B1F1C)

/**
 * A knot that is not on the route: a grey ring and number, see `map-planning` "Route knots stand
 * out".
 */
private val GreyRing = Color(0xFF9AA19C)
private val GreyText = Color(0xFF6B736D)
private val DarkDisc = Color(0xFF2A2C2A)
private val DarkRing = Color(0xFF8A928C)
private val DarkText = Color(0xFFC9CEC9)

/** The colour of [plate] for drawing. */
val KnotPlate.color: Color
  get() = Color(colour.toInt())

/**
 * How a knot is drawn, on the map and in the sheet alike, see `map-planning` "Route knots stand
 * out": a waypoint as a filled plate in its sign region's colour, a passed knot white with a ring
 * in that colour, and a knot not on the route ([role] `null`) with a grey ring.
 */
private class BadgeStyle(
  val fill: Color,
  val ring: Color?,
  val ringWidth: Float,
  val innerRing: Boolean,
  val text: Color,
  val large: Boolean,
)

private fun styleOf(
  role: KnotRole?,
  plate: KnotPlate,
  dark: Boolean,
  filled: Boolean? = null,
): BadgeStyle =
  when {
    // In the navigation header, filled or white as on the knot's signs, see `knot-navigation`
    // "Number to follow".
    filled == true -> BadgeStyle(plate.color, null, 0f, plate.innerRing, Color.White, large = false)
    filled == false -> BadgeStyle(Color.White, plate.color, 5f, false, plate.color, large = false)
    else -> styleOf(role, plate, dark)
  }

private fun styleOf(role: KnotRole?, plate: KnotPlate, dark: Boolean): BadgeStyle =
  when (role) {
    null ->
      if (dark) BadgeStyle(DarkDisc, DarkRing, 4f, false, DarkText, large = false)
      else BadgeStyle(Color.White, GreyRing, 4f, false, GreyText, large = false)
    KnotRole.PASSED -> BadgeStyle(Color.White, plate.color, 5f, false, plate.color, large = false)
    KnotRole.STOP -> BadgeStyle(plate.color, null, 0f, plate.innerRing, Color.White, large = false)
    KnotRole.START,
    KnotRole.END,
    KnotRole.ROUND_TRIP ->
      BadgeStyle(plate.color, null, 0f, plate.innerRing, Color.White, large = true)
  }

/**
 * The map image name of a badge; the map's symbol layers build the same names from GeoJSON. Knots
 * not on the route are grey everywhere, so only route knots carry their plate.
 */
fun badgeName(role: KnotRole?, ref: String, plate: KnotPlate = KnotPlate.NONE) =
  if (role == null) "knot-$ref" else "route-${role.name.lowercase()}-${plate.key}-$ref"

/** Room around the plate for the corner symbol, on every side so the plate stays centred. */
private const val CORNER_ROOM = 9

/**
 * The corner symbol's diameter, and how far its centre sits from the badge's, per badge diameter.
 */
private const val CORNER_SIZE = 0.5f
private const val CORNER_OFFSET = 0.38f

/**
 * A round badge with the knot's number, drawn locally so numbers show without network access, with
 * a waypoint's corner symbol.
 */
fun badgeBitmap(
  role: KnotRole?,
  ref: String,
  plate: KnotPlate = KnotPlate.NONE,
  dark: Boolean = false,
  filled: Boolean? = null,
): Bitmap {
  val style = styleOf(role, plate, dark, filled)
  val plateSize = if (style.large) 72 else 56
  val size = plateSize + 2 * CORNER_ROOM
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val center = size / 2f
  val radius = plateSize / 2f - 3
  canvas.drawCircle(
    center,
    center,
    radius,
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.fill.toArgb() },
  )
  style.ring?.let { ring ->
    canvas.drawCircle(
      center,
      center,
      radius - style.ringWidth / 2,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ring.toArgb()
        this.style = Paint.Style.STROKE
        strokeWidth = style.ringWidth
      },
    )
  }
  if (style.innerRing) {
    canvas.drawCircle(
      center,
      center,
      radius * 0.8f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        this.style = Paint.Style.STROKE
        strokeWidth = radius * 0.11f
      },
    )
  }
  val text =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = style.text.toArgb()
      textAlign = Paint.Align.CENTER
      typeface = Typeface.DEFAULT_BOLD
      textSize = (if (ref.length > 2) 17f else 22f) * (if (style.large) 1.25f else 1f)
    }
  if (ref.isEmpty()) {
    // A route point has no number: a flag, see `map-planning` "Route points on the map and in the
    // list".
    canvas.save()
    canvas.translate(CORNER_ROOM.toFloat(), CORNER_ROOM.toFloat())
    canvas.drawPath(
      flagPath(plateSize.toFloat()),
      Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.text.toArgb() },
    )
    canvas.restore()
  } else {
    canvas.drawText(ref, center, center - (text.descent() + text.ascent()) / 2, text)
  }
  role?.let {
    val diameter = 2 * radius
    drawCornerSymbol(
      canvas,
      it,
      center + diameter * CORNER_OFFSET,
      center - diameter * CORNER_OFFSET,
      diameter * CORNER_SIZE / 2,
    )
  }
  return bitmap
}

/**
 * A waypoint's corner symbol: ▶ for the start, a flag for a stop, ■ for the end and a circular
 * arrow for a round trip, white on a dark disc, see `map-planning` "Route knots stand out".
 */
private fun drawCornerSymbol(canvas: Canvas, role: KnotRole, x: Float, y: Float, radius: Float) {
  val path = cornerSymbolPath(role, radius * 2) ?: return
  canvas.drawCircle(x, y, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ink.toArgb() })
  canvas.drawCircle(
    x,
    y,
    radius,
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = radius * 0.12f
    },
  )
  canvas.save()
  canvas.translate(x - radius, y - radius)
  val roundTrip = role == KnotRole.ROUND_TRIP
  canvas.drawPath(
    path,
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.WHITE
      if (roundTrip) {
        style = Paint.Style.STROKE
        strokeWidth = radius * 0.26f
        strokeCap = Paint.Cap.ROUND
      }
    },
  )
  if (roundTrip) {
    canvas.drawPath(
      roundTripHeadPath(radius * 2),
      Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE },
    )
  }
  canvas.restore()
}

/** The glyph of a waypoint's corner symbol in a square of [size], `null` for passed knots. */
fun cornerSymbolPath(role: KnotRole, size: Float): Path? =
  when (role) {
    KnotRole.PASSED -> null
    KnotRole.START ->
      Path().apply {
        moveTo(size * 0.34f, size * 0.22f)
        lineTo(size * 0.8f, size * 0.5f)
        lineTo(size * 0.34f, size * 0.78f)
        close()
      }
    KnotRole.END ->
      Path().apply {
        addRect(size * 0.27f, size * 0.27f, size * 0.73f, size * 0.73f, Path.Direction.CW)
      }
    KnotRole.STOP ->
      Path().apply {
        // A bolder flag than a route point's: a pole and a wide pennant.
        addRect(size * 0.27f, size * 0.2f, size * 0.37f, size * 0.82f, Path.Direction.CW)
        moveTo(size * 0.37f, size * 0.2f)
        lineTo(size * 0.8f, size * 0.36f)
        lineTo(size * 0.37f, size * 0.54f)
        close()
      }
    KnotRole.ROUND_TRIP ->
      Path().apply {
        addArc(RectF(size * 0.26f, size * 0.26f, size * 0.74f, size * 0.74f), -60f, 280f)
      }
  }

/** The arrowhead at the end of the round trip's circular arrow, in a square of [size]. */
fun roundTripHeadPath(size: Float): Path =
  Path().apply {
    // The arc ends at 220°, just above the left; the head points on clockwise, upwards.
    moveTo(size * 0.12f, size * 0.44f)
    lineTo(size * 0.44f, size * 0.44f)
    lineTo(size * 0.26f, size * 0.2f)
    close()
  }

/** A flag for a route point, filling a badge of [size]: a pole and a pennant. */
fun flagPath(size: Float): Path =
  Path().apply {
    val pole = size * 0.06f
    val left = size * 0.34f
    addRect(left, size * 0.24f, left + pole, size * 0.78f, Path.Direction.CW)
    moveTo(left + pole, size * 0.24f)
    lineTo(size * 0.72f, size * 0.37f)
    lineTo(left + pole, size * 0.52f)
    close()
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

/**
 * The rider's position with a riding direction: a navigation arrow pointing north, rotated by the
 * map, see `knot-navigation` "Riding direction on the map".
 */
fun riderArrowBitmap(color: Int): Bitmap {
  val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
  val path =
    Path().apply {
      moveTo(32f, 6f)
      lineTo(54f, 56f)
      lineTo(32f, 45f)
      lineTo(10f, 56f)
      close()
    }
  val canvas = Canvas(bitmap)
  canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
  canvas.drawPath(
    path,
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.color = android.graphics.Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = 5f
      strokeJoin = Paint.Join.ROUND
    },
  )
  return bitmap
}

/** The same badge as on the map, for the sheet; [scale] makes it larger, e.g. to stand out. */
@Composable
fun KnotBadge(
  ref: String,
  role: KnotRole?,
  modifier: Modifier = Modifier,
  scale: Float = 1f,
  plate: KnotPlate = KnotPlate.NONE,
  filled: Boolean? = null,
) {
  val dark = isAppInDarkTheme()
  val style = styleOf(role, plate, dark, filled)
  val size = (if (style.large) 34.dp else 28.dp) * scale
  // Required, so a slot narrower than the badge, e.g. on the strip, can't squeeze it into an oval.
  Box(contentAlignment = Alignment.Center, modifier = modifier.requiredSize(size)) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.requiredSize(size)
          .background(style.fill, CircleShape)
          .then(style.ring?.let { Modifier.border(2.dp * scale, it, CircleShape) } ?: Modifier)
          .then(
            if (style.innerRing)
              Modifier.padding(size * 0.12f).border(size * 0.055f, Color.White, CircleShape)
            else Modifier
          ),
    ) {
      // In dp, so the number fits the badge at any text size, like on the map, see `map-planning`
      // "Knot badges at large text sizes"; the line height too, which would push the number down.
      val textSize =
        with(LocalDensity.current) {
          ((if (ref.length > 2) 10 else 12).dp * (if (style.large) 1.2f else 1f) * scale).toSp()
        }
      if (ref.isEmpty()) {
        // A route point's flag, as on the map.
        Canvas(Modifier.size(size)) {
          drawPath(flagPath(this.size.width).asComposePath(), style.text)
        }
      } else {
        Text(
          ref,
          color = style.text,
          fontWeight = FontWeight.Bold,
          fontSize = textSize,
          lineHeight = textSize,
          maxLines = 1,
        )
      }
    }
    role?.let {
      CornerSymbol(
        it,
        size * CORNER_SIZE,
        Modifier.align(Alignment.Center)
          .offset(x = size * CORNER_OFFSET, y = -size * CORNER_OFFSET),
      )
    }
  }
}

/** A waypoint's corner symbol in the sheet, as on the map's badges. */
@Composable
private fun CornerSymbol(role: KnotRole, size: androidx.compose.ui.unit.Dp, modifier: Modifier) {
  if (role == KnotRole.PASSED) return
  Canvas(
    modifier
      .requiredSize(size)
      .background(Ink, CircleShape)
      .border(size * 0.06f, Color.White, CircleShape)
  ) {
    val side = this.size.width
    val path = cornerSymbolPath(role, side) ?: return@Canvas
    if (role == KnotRole.ROUND_TRIP) {
      drawPath(
        path.asComposePath(),
        Color.White,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = side * 0.12f),
      )
      drawPath(roundTripHeadPath(side).asComposePath(), Color.White)
    } else {
      drawPath(path.asComposePath(), Color.White)
    }
  }
}

/**
 * The number to follow on its sign plate, for the notification's large icon, see `knot-navigation`
 * "Navigation with the screen off".
 */
fun signPlateBitmap(
  badge: com.knoopbyknoop.core.HeaderBadge,
  turnDegrees: Float? = null,
  uTurn: android.graphics.drawable.Drawable? = null,
): Bitmap {
  val badgeImage = badgeBitmap(badge.role, badge.ref, badge.plate, filled = badge.filled)
  val width = badgeImage.width + 40
  val height = badgeImage.height
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val plate = badge.plate.color.toArgb()
  val dutch = !badge.filled
  val rect = RectF(4f, 8f, width - 4f, height - 8f)
  canvas.drawRoundRect(
    rect,
    10f,
    10f,
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (dutch) android.graphics.Color.WHITE else plate
    },
  )
  if (dutch) {
    canvas.drawRoundRect(
      rect,
      10f,
      10f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = plate
        style = Paint.Style.STROKE
        strokeWidth = 5f
      },
    )
  }
  canvas.drawBitmap(badgeImage, 0f, 0f, null)
  val arrow =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (dutch) plate else android.graphics.Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = 6f
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
    }
  val y = height / 2f
  val left = badgeImage.width - 2f
  if (uTurn != null) {
    // Turning back at the knot: the U-turn, see `knot-navigation` "Turn at the next knot".
    uTurn.setTint(arrow.color)
    uTurn.setBounds(left.toInt() - 4, (y - 18).toInt(), left.toInt() + 32, (y + 18).toInt())
    uTurn.draw(canvas)
    return bitmap
  }
  // The turn the route makes at the knot, or a plain → where it cannot be told.
  canvas.save()
  canvas.rotate(turnDegrees?.let { it - 90f } ?: 0f, left + 14f, y)
  canvas.drawLine(left, y, left + 28f, y, arrow)
  canvas.drawPath(
    Path().apply {
      moveTo(left + 18f, y - 10f)
      lineTo(left + 28f, y)
      lineTo(left + 18f, y + 10f)
    },
    arrow,
  )
  canvas.restore()
  return bitmap
}

/**
 * A symbol on a disc for the notification's large icon: the to-the-route symbol, dark off route and
 * light on the way to the route, or the U-turn of riding the wrong way.
 */
fun symbolDiscBitmap(context: android.content.Context, icon: Int, dark: Boolean): Bitmap {
  val size = 96
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  canvas.drawCircle(
    size / 2f,
    size / 2f,
    size / 2f - 2,
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (dark) Ink.toArgb() else 0xFFE3E7E3.toInt()
    },
  )
  context.getDrawable(icon)?.let { drawable ->
    drawable.setTint(if (dark) android.graphics.Color.WHITE else Ink.toArgb())
    drawable.setBounds(20, 20, size - 20, size - 20)
    drawable.draw(canvas)
  }
  return bitmap
}
