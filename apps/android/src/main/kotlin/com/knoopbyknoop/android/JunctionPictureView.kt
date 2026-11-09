package com.knoopbyknoop.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.JunctionPicture
import com.knoopbyknoop.core.PictureLine
import com.knoopbyknoop.core.PictureLineKind
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A drawing of a doubtful junction, see `knot-navigation` "Junction picture": the riding direction
 * up, the way bold in green with an arrow at its end, main roads wide, other streets and cycle
 * paths as short exits, on a dark tile in light and dark appearance alike, as style B of the
 * sketches.
 */
@Composable
fun JunctionPictureView(picture: JunctionPicture, modifier: Modifier = Modifier) {
  Canvas(
    modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(10.dp))
      .background(Tile, RoundedCornerShape(10.dp))
  ) {
    val side = min(size.width, size.height)
    val scale = side / JunctionPicture.ACROSS_METERS.toFloat()
    val width = side / 160f
    fun point(x: Int, y: Int) =
      Offset(
        (x + JunctionPicture.ACROSS_METERS.toFloat() / 2) * scale,
        (JunctionPicture.TOP_METERS.toFloat() - y) * scale,
      )
    fun pathOf(line: PictureLine) =
      Path().apply {
        line.points.forEachIndexed { index, it ->
          val at = point(it.x, it.y)
          if (index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
        }
      }
    fun stroke(line: PictureLine, color: Color, lineWidth: Float) =
      drawPath(
        pathOf(line),
        color,
        style = Stroke(lineWidth * width, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    val lines = picture.lines.filter { it.points.size >= 2 }
    // Wide roads first, the way on top, as in the sketches.
    lines.filter { it.kind == PictureLineKind.MAIN_ROAD }.forEach { stroke(it, MainRoad, 10f) }
    lines.filter { it.kind == PictureLineKind.STREET }.forEach { stroke(it, Street, 3.2f) }
    lines.filter { it.kind == PictureLineKind.CYCLE_PATH }.forEach { stroke(it, CyclePath, 3.2f) }
    lines
      .filter { it.kind == PictureLineKind.WAY_BEFORE }
      .forEach { line ->
        stroke(line, Way.copy(alpha = 0.55f), 5f)
        val start = point(line.points[0].x, line.points[0].y)
        drawCircle(Color.White, 5.5f * width, start)
        drawCircle(Way, 5.5f * width, start, style = Stroke(2.5f * width))
      }
    lines
      .filter { it.kind == PictureLineKind.WAY_AFTER }
      .forEach { line ->
        stroke(line, Way, 5.5f)
        // The arrow at its end, along its last stretch.
        val end = line.points.last().let { point(it.x, it.y) }
        val before = line.points[line.points.size - 2].let { point(it.x, it.y) }
        val angle = atan2(end.y - before.y, end.x - before.x)
        val length = 13f * width
        fun at(distance: Float, turn: Double) =
          Offset(
            end.x + (cos(angle + turn) * distance).toFloat(),
            end.y + (sin(angle + turn) * distance).toFloat(),
          )
        val head =
          Path().apply {
            at(length * 0.45f, 0.0).let { moveTo(it.x, it.y) }
            at(length * 0.75f, PI * 0.78).let { lineTo(it.x, it.y) }
            at(length * 0.75f, -PI * 0.78).let { lineTo(it.x, it.y) }
            close()
          }
        drawPath(head, Way)
      }
  }
}

private val Tile = Color(0xFF141A1F)
private val MainRoad = Color(0xFF6F7B86)
private val Street = Color(0xFF7D8A95)
private val CyclePath = Color(0xFF5FB8D6)
private val Way = Color(0xFF2FD17A)
