package com.knoopbyknoop.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.GuidedHeader
import com.knoopbyknoop.core.HeaderBadge
import com.knoopbyknoop.core.HeaderRow
import com.knoopbyknoop.core.TurnDirection

/** The orange of the route line, for the to-the-route symbol. */
private val RouteOrange = Color(0xFFF28C28)

/** The rider's blue, as their position on the map. */
private val RiderArrowBlue = Color(0xFF1A73E8)

/**
 * The header's row without words: badges, arrows and distances, see `knot-navigation` "Number to
 * follow", "Direction to the route" and "Header on the way to the route". Screen readers get
 * [HeaderRow.spoken].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeaderRowView(row: HeaderRow, modifier: Modifier = Modifier) {
  val guided = (row as? HeaderRow.ToRoute)?.guided
  if (row is HeaderRow.ToRoute && guided != null) {
    // On a guided way always one row, so the header keeps its height as the way is found or lost,
    // see `knot-navigation` "Header of steady height on a guided way".
    GuidedRow(row, guided, modifier.clearAndSetSemantics { contentDescription = row.spoken })
    return
  }
  // The distance moves below the badges where it does not fit next to them, see `knot-navigation`
  // "Number to follow at large text sizes".
  FlowRow(
    itemVerticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.clearAndSetSemantics { contentDescription = row.spoken },
  ) {
    val prominent = MaterialTheme.typography.headlineMedium
    val secondary = MaterialTheme.typography.titleMedium
    when (row) {
      is HeaderRow.Close -> {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(contentAlignment = Alignment.Center) { HeaderKnot(row.next, scale = 1.4f) }
        }
        Text(row.distance, style = secondary, maxLines = 1)
        Arrow(28.dp)
        SignPlate(row.follow, row.turn)
      }
      is HeaderRow.Far -> {
        HeaderKnot(row.next, scale = 2f)
        Text(row.distance, style = prominent, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Chevron(18.dp)
          HeaderKnot(row.follow, scale = 1.2f)
        }
      }
      is HeaderRow.End -> {
        HeaderKnot(row.next, scale = 2f)
        Text(row.distance, style = prominent, maxLines = 1)
      }
      is HeaderRow.WrongWay -> {
        UTurn(36.dp)
        HeaderKnot(row.next, scale = 1.6f)
        Text(row.distance, style = prominent, maxLines = 1)
      }
      is HeaderRow.ToRoute -> {
        ToRouteSymbol(awayFromRoute = row.offRoute, size = 40.dp, tile = !row.offRoute)
        Icon(
          painterResource(R.drawable.ic_action_navigate),
          contentDescription = null,
          modifier = Modifier.size(40.dp).rotate(row.arrowDegrees.toFloat()),
        )
        Text(row.distance, style = prominent, maxLines = 1)
        row.next?.let {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Chevron(18.dp)
            HeaderKnot(it, scale = 1.2f)
          }
        }
      }
    }
  }
}

/**
 * The row on a guided way: the tile, the turn with its distance and street, the distance left and
 * the next knot, in one row whose street is cut short rather than wrapped.
 */
@Composable
private fun GuidedRow(row: HeaderRow.ToRoute, guided: GuidedHeader, modifier: Modifier) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier,
  ) {
    ToRouteSymbol(awayFromRoute = row.offRoute, size = 40.dp, tile = !row.offRoute)
    Icon(
      painterResource(R.drawable.ic_action_navigate),
      contentDescription = null,
      modifier = Modifier.size(32.dp).rotate(turnDegrees(guided.turn)),
    )
    Column(Modifier.weight(1f, fill = false)) {
      Text(
        guided.turnDistance ?: guided.remaining,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
      guided.street?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (guided.turnDistance != null) {
      Text(guided.remaining, style = MaterialTheme.typography.titleLarge, maxLines = 1)
    }
    row.next?.let {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Chevron(18.dp)
        HeaderKnot(it, scale = 1.2f)
      }
    }
  }
}

/** A knot of the header, filled or white as on its signs, with a picked knot's corner symbol. */
@Composable
private fun HeaderKnot(badge: HeaderBadge, scale: Float) {
  KnotBadge(
    badge.ref,
    badge.role,
    scale = scale,
    plate = badge.plate,
    filled = badge.filled,
  )
}

/**
 * The number to follow on a sign plate, with the arrow to the right of the number, like the knot's
 * arrow signs, see `knot-navigation` "Number to follow".
 */
@Composable
private fun SignPlate(badge: HeaderBadge, turn: TurnDirection?) {
  val plate = badge.plate.color
  val dutch = !badge.filled
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    modifier =
      Modifier.background(if (dutch) Color.White else plate, RoundedCornerShape(8.dp))
        .then(if (dutch) Modifier.border(3.dp, plate, RoundedCornerShape(8.dp)) else Modifier)
        .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
  ) {
    HeaderKnot(badge, scale = 2f)
    // The turn the route makes at the knot, or a plain → where it cannot be told, see
    // `knot-navigation` "Turn at the next knot".
    val arrowColor = if (dutch) plate else Color.White
    if (turn == TurnDirection.BACK) UTurn(26.dp, arrowColor)
    else Arrow(26.dp, color = arrowColor, degrees = turn?.let { turnDegrees(it) })
  }
}

/** The angle a turn on the guided way turns the arrow, from straight ahead. */
internal fun turnDegrees(turn: TurnDirection?): Float =
  when (turn) {
    null,
    TurnDirection.STRAIGHT -> 0f
    TurnDirection.SLIGHT_LEFT -> -45f
    TurnDirection.LEFT -> -90f
    TurnDirection.SHARP_LEFT -> -135f
    TurnDirection.SLIGHT_RIGHT -> 45f
    TurnDirection.RIGHT -> 90f
    TurnDirection.SHARP_RIGHT -> 135f
    TurnDirection.BACK -> 180f
  }

/** A plain → between the knot and the number to follow, until it shows the turn. */
@Composable
private fun Arrow(size: Dp, color: Color = LocalContentColor.current, degrees: Float? = null) {
  // Drawn pointing right; a turn points it up first, then turns it by [degrees].
  Canvas(Modifier.size(size).rotate(degrees?.let { it - 90f } ?: 0f)) {
    val width = this.size.width
    val stroke = Stroke(width * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawLine(
      color,
      Offset(width * 0.18f, width / 2),
      Offset(width * 0.78f, width / 2),
      stroke.width,
      StrokeCap.Round,
    )
    drawPath(
      Path().apply {
        moveTo(width * 0.55f, width * 0.27f)
        lineTo(width * 0.8f, width / 2)
        lineTo(width * 0.55f, width * 0.73f)
      },
      color,
      style = stroke,
    )
  }
}

/** A small › before the knot after the next one, or the next knot while off route. */
@Composable
private fun Chevron(size: Dp) {
  val color = LocalContentColor.current.copy(alpha = 0.6f)
  Canvas(Modifier.size(size)) {
    val width = this.size.width
    drawPath(
      Path().apply {
        moveTo(width * 0.38f, width * 0.22f)
        lineTo(width * 0.66f, width / 2)
        lineTo(width * 0.38f, width * 0.78f)
      },
      color,
      style = Stroke(width * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }
}

/** The U-turn of riding the wrong way, see `knot-navigation` "Riding the wrong way". */
@Composable
private fun UTurn(size: Dp, color: Color = LocalContentColor.current) {
  Canvas(Modifier.size(size)) {
    val width = this.size.width
    val stroke = Stroke(width * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
      Path().apply {
        moveTo(width * 0.68f, width * 0.85f)
        lineTo(width * 0.68f, width * 0.42f)
        arcTo(
          androidx.compose.ui.geometry.Rect(
            width * 0.3f,
            width * 0.2f,
            width * 0.68f,
            width * 0.6f,
          ),
          0f,
          -180f,
          false,
        )
        lineTo(width * 0.3f, width * 0.62f)
      },
      color,
      style = stroke,
    )
    drawPath(
      Path().apply {
        moveTo(width * 0.16f, width * 0.5f)
        lineTo(width * 0.3f, width * 0.68f)
        lineTo(width * 0.44f, width * 0.5f)
      },
      color,
      style = stroke,
    )
  }
}

/**
 * The to-the-route symbol: a small rider arrow joined by a dashed line to the orange route line, as
 * on the map; the arrow points away from the route off route, towards it on the way to it, see
 * `knot-navigation` "Direction to the route".
 */
@Composable
fun ToRouteSymbol(awayFromRoute: Boolean, size: Dp, tile: Boolean) {
  val content = LocalContentColor.current
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      if (tile)
        Modifier.size(size + 12.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
      else Modifier.size(size),
  ) {
    Canvas(Modifier.size(size)) { drawToRouteSymbol(content, awayFromRoute) }
  }
}

private fun DrawScope.drawToRouteSymbol(dashes: Color, awayFromRoute: Boolean) {
  val unit = size.width / 24
  drawPath(
    Path().apply {
      moveTo(9 * unit, 2.5f * unit)
      cubicTo(12 * unit, 7 * unit, 15 * unit, 11 * unit, 21.5f * unit, 13 * unit)
    },
    RouteOrange,
    style = Stroke(3.2f * unit, cap = StrokeCap.Round),
  )
  drawLine(
    dashes,
    Offset(6 * unit, 18.5f * unit),
    Offset(13 * unit, 9.5f * unit),
    2 * unit,
    StrokeCap.Round,
    PathEffect.dashPathEffect(floatArrayOf(2 * unit, 2.6f * unit)),
  )
  val arrow =
    Path().apply {
      moveTo(0f, -4.6f * unit)
      lineTo(3.6f * unit, 4f * unit)
      lineTo(0f, 2f * unit)
      lineTo(-3.6f * unit, 4f * unit)
      close()
    }
  val center = Offset(5.5f * unit, 19 * unit)
  rotate(if (awayFromRoute) 218f else 38f, center) {
    translate(center.x, center.y) {
      drawPath(arrow, RiderArrowBlue)
      drawPath(arrow, Color.White, style = Stroke(1.2f * unit, join = StrokeJoin.Round))
    }
  }
}

private inline fun DrawScope.translate(left: Float, top: Float, block: DrawScope.() -> Unit) {
  drawContext.transform.translate(left, top)
  block()
  drawContext.transform.translate(-left, -top)
}
