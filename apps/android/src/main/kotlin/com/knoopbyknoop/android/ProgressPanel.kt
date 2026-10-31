package com.knoopbyknoop.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.ProgressMark
import com.knoopbyknoop.core.ProgressMarkKind
import com.knoopbyknoop.core.ProgressStrip
import com.knoopbyknoop.core.ProgressView
import kotlin.math.abs

/**
 * Where the rider is on the route while navigating: the progress strip with the distances and
 * arrivals below its marks, see `knot-navigation` "Progress strip" and "Distances and arrivals on
 * the strip".
 */
@Composable
fun ProgressPanel(progress: ProgressView) {
  Box(modifier = Modifier.padding(vertical = 4.dp)) { ProgressStripView(progress.strip) }
}

/** The rider's color, as their position on the map. */
private val RiderBlue = Color(0xFF1A73E8)

private val BADGE_SPACE = 30.dp
private val STRIP_HEIGHT = 44.dp

/** The room for a mark's distance and arrival, e.g. `279.6 km` over `04:30`. */
private val LABEL_WIDTH = 64.dp
private val LABEL_HEIGHT = 40.dp

/** The distances and arrivals that give way to a stronger one where they would overlap. */
private val distancePriority =
  listOf(ProgressMarkKind.END, ProgressMarkKind.STOP, ProgressMarkKind.FERRY)

/** The labels that give way to a stronger one where they would overlap. */
private val labelPriority =
  listOf(
    ProgressMarkKind.END,
    ProgressMarkKind.STOP,
    ProgressMarkKind.START,
    ProgressMarkKind.FERRY,
  )

/**
 * The route drawn to scale with the rider as a dot and the ridden part filled; labels that would
 * overlap a stronger one are left out, the marks of the other knots always drawn.
 */
@Composable
private fun ProgressStripView(strip: ProgressStrip) {
  val ridden = ActionColors.green
  val ahead = MaterialTheme.colorScheme.outlineVariant
  val tick = MaterialTheme.colorScheme.onSurfaceVariant
  val rider = if (strip.offRoute) MaterialTheme.colorScheme.error else RiderBlue
  BoxWithConstraints(
    modifier =
      Modifier.fillMaxWidth().height(STRIP_HEIGHT + LABEL_HEIGHT).clearAndSetSemantics {
        contentDescription = strip.description
      }
  ) {
    // The line keeps half a badge free at both ends, so the start and end badges fit.
    val inset = BADGE_SPACE / 2
    val lineWidth = maxWidth - BADGE_SPACE
    fun xOf(fraction: Double): Dp = inset + lineWidth * fraction.toFloat()
    Canvas(modifier = Modifier.fillMaxWidth().height(STRIP_HEIGHT)) {
      val y = size.height / 2
      val start = inset.toPx()
      val end = size.width - inset.toPx()
      val riderX = start + (end - start) * strip.rider.toFloat()
      drawLine(ahead, Offset(start, y), Offset(end, y), 4.dp.toPx(), StrokeCap.Round)
      drawLine(ridden, Offset(start, y), Offset(riderX, y), 4.dp.toPx(), StrokeCap.Round)
      for (mark in strip.marks.filter { it.kind == ProgressMarkKind.KNOT }) {
        val x = start + (end - start) * mark.fraction.toFloat()
        drawLine(tick, Offset(x, y - 5.dp.toPx()), Offset(x, y + 5.dp.toPx()), 1.dp.toPx())
      }
    }
    for (mark in shownLabels(strip.marks, lineWidth)) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.height(STRIP_HEIGHT).offset(x = xOf(mark.fraction) - BADGE_SPACE / 2),
      ) {
        when (mark.kind) {
          ProgressMarkKind.FERRY -> FerryBadge()
          else ->
            Box(modifier = Modifier.height(BADGE_SPACE), contentAlignment = Alignment.Center) {
              KnotBadge(mark.label, roleOf(mark.kind))
            }
        }
      }
    }
    val badges = shownLabels(strip.marks, lineWidth)
    for (mark in shownDistances(badges, lineWidth)) {
      // Centered below the mark, but within the strip at both ends.
      val x = (xOf(mark.fraction) - LABEL_WIDTH / 2).coerceIn(0.dp, maxWidth - LABEL_WIDTH)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(x = x, y = STRIP_HEIGHT).width(LABEL_WIDTH),
      ) {
        Text(
          mark.distance.orEmpty(),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
        mark.arrival?.let {
          Text(
            it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }
    }
    // Above the badges, so the rider stays visible at a labeled knot.
    Canvas(modifier = Modifier.fillMaxWidth().height(STRIP_HEIGHT)) {
      val start = inset.toPx()
      val riderX = start + (size.width - 2 * start) * strip.rider.toFloat()
      val center = Offset(riderX, size.height / 2)
      drawCircle(rider, 7.dp.toPx(), center)
      drawCircle(Color.White, 7.dp.toPx(), center, style = Stroke(2.dp.toPx()))
    }
  }
}

private fun roleOf(kind: ProgressMarkKind): KnotRole =
  when (kind) {
    ProgressMarkKind.START -> KnotRole.START
    ProgressMarkKind.STOP -> KnotRole.STOP
    ProgressMarkKind.END -> KnotRole.END
    else -> KnotRole.PASSED
  }

/** The labeled marks that fit, the stronger ones first. */
private fun shownLabels(marks: List<ProgressMark>, lineWidth: Dp): List<ProgressMark> {
  val shown = mutableListOf<ProgressMark>()
  for (kind in labelPriority) {
    for (mark in marks.filter { it.kind == kind }) {
      val fits = shown.none { lineWidth * abs(it.fraction - mark.fraction).toFloat() < BADGE_SPACE }
      if (fits) shown += mark
    }
  }
  return shown
}

/**
 * The shown stops, ferries and end ahead whose distance and arrival fit, the stronger ones first.
 */
private fun shownDistances(badges: List<ProgressMark>, lineWidth: Dp): List<ProgressMark> {
  val shown = mutableListOf<ProgressMark>()
  for (kind in distancePriority) {
    for (mark in badges.filter { it.kind == kind && it.distance != null }) {
      val fits = shown.none { lineWidth * abs(it.fraction - mark.fraction).toFloat() < LABEL_WIDTH }
      if (fits) shown += mark
    }
  }
  return shown
}
