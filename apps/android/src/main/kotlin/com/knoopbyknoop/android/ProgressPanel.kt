package com.knoopbyknoop.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.knoopbyknoop.core.FerryArrivalState
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.Navigation
import com.knoopbyknoop.core.ProgressMark
import com.knoopbyknoop.core.ProgressMarkKind
import com.knoopbyknoop.core.ProgressStrip
import com.knoopbyknoop.core.ProgressView
import com.knoopbyknoop.core.RouteFerry
import java.time.Instant
import kotlin.math.abs

/**
 * Where the rider is on the route while navigating: the progress strip with the distances and
 * arrivals below its marks and the ferries, which open a popover, see `knot-navigation` "Progress
 * strip", "Distances and arrivals on the strip", "Ferries on the strip" and "Ferry popover".
 */
@Composable
fun ProgressPanel(progress: ProgressView, ferries: List<RouteFerry>, navigation: Navigation) {
  Box(modifier = Modifier.padding(vertical = 4.dp)) {
    ProgressStripView(progress.strip, ferries, navigation)
  }
}

/** The rider's color, as their position on the map. */
private val RiderBlue = Color(0xFF1A73E8)

private val BADGE_SPACE = 30.dp
private val STRIP_HEIGHT = 44.dp

/** The room for a mark's distance and arrival, e.g. `280 km` over `04:30`. */
private val LABEL_WIDTH = 64.dp
private val LABEL_HEIGHT = 40.dp

/** How important a mark is where its badge or its distance would overlap another's: lower first. */
private fun rank(mark: ProgressMark): Int =
  when {
    mark.kind == ProgressMarkKind.END -> 0
    mark.kind == ProgressMarkKind.FERRY && mark.state == FerryArrivalState.WARNING -> 1
    mark.kind == ProgressMarkKind.STOP -> 2
    mark.kind == ProgressMarkKind.START -> 3
    mark.kind == ProgressMarkKind.FERRY -> 4
    else -> Int.MAX_VALUE
  }

/**
 * The route drawn to scale with the rider as a dot and the ridden part filled; labels that would
 * overlap a stronger one are left out, the marks of the other knots always drawn.
 */
@Composable
private fun ProgressStripView(
  strip: ProgressStrip,
  ferries: List<RouteFerry>,
  navigation: Navigation,
) {
  val ridden = ActionColors.green
  val ahead = MaterialTheme.colorScheme.outlineVariant
  val tick = MaterialTheme.colorScheme.onSurfaceVariant
  val rider = if (strip.offRoute) MaterialTheme.colorScheme.error else RiderBlue
  // The route position of the ferry whose popover is open.
  var opened by remember { mutableStateOf<Int?>(null) }
  BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(STRIP_HEIGHT + LABEL_HEIGHT)) {
    // The line keeps half a badge free at both ends, so the start and end badges fit.
    val inset = BADGE_SPACE / 2
    val lineWidth = maxWidth - BADGE_SPACE
    fun xOf(fraction: Double): Dp = inset + lineWidth * fraction.toFloat()
    // For screen readers, the strip is one element, except the ferries, which are buttons.
    Canvas(
      modifier =
        Modifier.fillMaxWidth().height(STRIP_HEIGHT).clearAndSetSemantics {
          contentDescription = strip.description
        }
    ) {
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
    val badges = shown(strip.marks, lineWidth, BADGE_SPACE) { it.kind != ProgressMarkKind.KNOT }
    for (mark in badges) {
      val x = xOf(mark.fraction) - BADGE_SPACE / 2
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.height(STRIP_HEIGHT).width(BADGE_SPACE).offset(x = x),
      ) {
        val ferry = ferries.firstOrNull { it.fromPosition == mark.position }
        if (mark.kind == ProgressMarkKind.FERRY && ferry != null) {
          FerryBadge(
            warning = mark.state == FerryArrivalState.WARNING,
            modifier =
              Modifier.clip(CircleShape)
                .clickable(role = Role.Button) { opened = ferry.fromPosition }
                .clearAndSetSemantics {
                  contentDescription = ferryDescription(ferry, navigation)
                },
          )
          if (opened == ferry.fromPosition) {
            FerryPopover(ferry, navigation) { opened = null }
          }
        } else {
          Box(modifier = Modifier.clearAndSetSemantics {}) {
            KnotBadge(mark.label, roleOf(mark.kind))
          }
        }
      }
    }
    val labeled = shown(badges, lineWidth, LABEL_WIDTH) { it.distance != null }
    for (mark in labeled) {
      // Centered below the mark, but within the strip at both ends.
      val x = (xOf(mark.fraction) - LABEL_WIDTH / 2).coerceIn(0.dp, maxWidth - LABEL_WIDTH)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier.offset(x = x, y = STRIP_HEIGHT).width(LABEL_WIDTH).clearAndSetSemantics {},
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
            color =
              when (mark.state) {
                FerryArrivalState.WARNING -> MaterialTheme.colorScheme.error
                FerryArrivalState.OPEN -> ActionColors.iconGreen
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              },
            fontWeight = if (mark.state != null) FontWeight.Medium else null,
            maxLines = 1,
          )
        }
      }
    }
    // Above the badges, so the rider stays visible at a labeled knot.
    Canvas(modifier = Modifier.fillMaxWidth().height(STRIP_HEIGHT).clearAndSetSemantics {}) {
      val start = inset.toPx()
      val riderX = start + (size.width - 2 * start) * strip.rider.toFloat()
      val center = Offset(riderX, size.height / 2)
      drawCircle(rider, 7.dp.toPx(), center)
      drawCircle(Color.White, 7.dp.toPx(), center, style = Stroke(2.dp.toPx()))
    }
  }
}

/** E.g. `Ferry 76 → 02, at 21:10 closed until 1 May 10:00`, see "Ferry popover". */
private fun ferryDescription(ferry: RouteFerry, navigation: Navigation): String {
  val arrival = navigation.ferryArrival(ferry.fromPosition)
  val status =
    arrival?.accessibilityText
      ?: ferry.statusAt(ferryMoment(Instant.now()))?.text?.replaceFirstChar { it.lowercase() }
  return listOfNotNull("Ferry ${ferry.place}", status).joinToString(", ")
}

/** The ferry's details above the strip, closed by tapping outside, see "Ferry popover". */
@Composable
private fun FerryPopover(ferry: RouteFerry, navigation: Navigation, onDismiss: () -> Unit) {
  val line = status(ferry, ferryMoment(Instant.now()), navigation)
  Popup(
    alignment = Alignment.BottomCenter,
    offset = IntOffset(0, -120),
    onDismissRequest = onDismiss,
    properties = PopupProperties(focusable = true),
  ) {
    Card(
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
      modifier = Modifier.widthIn(max = 300.dp),
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text("⛴ ${ferry.place}", style = MaterialTheme.typography.titleSmall)
        FerryDetails(ferry, line)
      }
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

/**
 * The marks that pass [include] and fit with [space] around them, the more important ones first; in
 * the strip's order.
 */
private fun shown(
  marks: List<ProgressMark>,
  lineWidth: Dp,
  space: Dp,
  include: (ProgressMark) -> Boolean,
): List<ProgressMark> {
  val shown = mutableListOf<ProgressMark>()
  for (mark in marks.filter(include).sortedBy(::rank)) {
    if (shown.none { lineWidth * abs(it.fraction - mark.fraction).toFloat() < space }) shown += mark
  }
  return shown.sortedBy { it.fraction }
}
