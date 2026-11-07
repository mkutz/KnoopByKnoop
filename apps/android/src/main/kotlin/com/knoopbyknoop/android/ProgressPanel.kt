package com.knoopbyknoop.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.knoopbyknoop.core.ComingKnot
import com.knoopbyknoop.core.FerryArrivalState
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.KnotPlate
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

/** How much of [LABEL_WIDTH] the longest words of a label take, e.g. `130 km`, with a gap. */
private const val LABEL_WORDS_SHARE = 0.75f

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
  val rider = RiderBlue
  // The route position of the ferry whose popover is open.
  var opened by remember { mutableStateOf<Int?>(null) }
  // The room for the labels grows with their text, see `knot-navigation` "Strip labels at large
  // text sizes".
  val fontScale = LocalDensity.current.fontScale
  val labelWidth = LABEL_WIDTH * fontScale
  val labelHeight = LABEL_HEIGHT * fontScale
  BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(STRIP_HEIGHT + labelHeight)) {
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
    val badges =
      shown(strip.marks, lineWidth, BADGE_SPACE) { it.kind != ProgressMarkKind.KNOT || it.inHeader }
    for (mark in badges) {
      val x = xOf(mark.fraction) - BADGE_SPACE / 2
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.height(STRIP_HEIGHT).width(BADGE_SPACE).offset(x = x),
      ) {
        val ferry = ferries.firstOrNull { it.fromPosition == mark.position }
        val ferryLabel = ferry?.let { stringResource(R.string.ferry_place, it.place) }
        if (mark.kind == ProgressMarkKind.FERRY && ferry != null) {
          FerryBadge(
            warning = mark.state == FerryArrivalState.WARNING,
            modifier =
              Modifier.clip(CircleShape)
                .clickable(role = Role.Button) { opened = ferry.fromPosition }
                .clearAndSetSemantics {
                  contentDescription = ferryDescription(ferryLabel.orEmpty(), ferry, navigation)
                },
          )
          if (opened == ferry.fromPosition) {
            FerryPopover(ferry, navigation) { opened = null }
          }
        } else {
          Box(modifier = Modifier.clearAndSetSemantics {}) {
            KnotBadge(mark.label, roleOf(mark.kind), plate = mark.plate)
          }
        }
      }
    }
    // Centered below the mark, but within the strip at both ends.
    fun labelStart(mark: ProgressMark): Dp =
      (xOf(mark.fraction) - labelWidth / 2).coerceIn(0.dp, maxWidth - labelWidth)
    // A label at an end moves inwards, closer to its neighbor than their marks are, so the words
    // must also stay apart where they end up, see `knot-navigation` "Strip labels at large text
    // sizes".
    val labeled =
      shown(
        badges,
        lineWidth,
        labelWidth,
        apart = { one, other ->
          abs((labelStart(one) - labelStart(other)).value) >= (labelWidth * LABEL_WORDS_SHARE).value
        },
      ) {
        it.distance != null
      }
    for (mark in labeled) {
      val x = labelStart(mark)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier.offset(x = x, y = STRIP_HEIGHT).width(labelWidth).clearAndSetSemantics {},
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
            color = arrivalColor(mark.state),
            fontWeight =
              when {
                arrivalLook(mark.state).bold -> FontWeight.Bold
                mark.state != null -> FontWeight.Medium
                else -> null
              },
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
      if (strip.offRoute) {
        // A ring with ✕, not red, see `knot-navigation` "Progress strip".
        val radius = 8.dp.toPx()
        drawCircle(Color.White, radius, center)
        drawCircle(Ink, radius - 1.25.dp.toPx(), center, style = Stroke(2.5.dp.toPx()))
        val arm = radius * 0.38f
        listOf(Offset(-arm, -arm) to Offset(arm, arm), Offset(arm, -arm) to Offset(-arm, arm))
          .forEach { (from, to) ->
            drawLine(Ink, center + from, center + to, 2.dp.toPx(), StrokeCap.Round)
          }
      } else {
        drawCircle(rider, 7.dp.toPx(), center)
        drawCircle(Color.White, 7.dp.toPx(), center, style = Stroke(2.dp.toPx()))
      }
    }
  }
}

/**
 * E.g. `Ferry 76 → 02, at 21:10 closed until 1 May 10:00` for the [label] `Ferry 76 → 02`, see
 * "Ferry popover".
 */
private fun ferryDescription(label: String, ferry: RouteFerry, navigation: Navigation): String {
  val arrival = navigation.ferryArrival(ferry.fromPosition)
  val status =
    arrival?.accessibilityText
      ?: ferry.statusAt(ferryMoment(Instant.now()))?.text?.replaceFirstChar { it.lowercase() }
  return listOfNotNull(label, status).joinToString(", ")
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

/**
 * The knots and ferries still ahead, in riding order, with their distance and arrival, see
 * `knot-navigation` "Coming knots"; tapping a knot shows it on the map.
 */
@Composable
fun ComingKnots(
  coming: List<ComingKnot>,
  onShowKnot: (Knot) -> Unit,
  modifier: Modifier = Modifier,
) {
  // The last knot clear of the navigation bar when the list is scrolled to its end.
  LazyColumn(
    modifier = modifier,
    contentPadding = WindowInsets.navigationBars.asPaddingValues(),
  ) {
    items(coming) { item ->
      val knot = item.knot
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
              if (knot != null) Modifier.clickable(role = Role.Button) { onShowKnot(knot) }
              else Modifier
            )
            .clearAndSetSemantics { contentDescription = item.description },
      ) {
        Box(modifier = Modifier.width(BADGE_SPACE + 10.dp), contentAlignment = Alignment.Center) {
          if (knot == null) FerryBadge(warning = item.state == FerryArrivalState.WARNING)
          else KnotBadge(item.label, roleOf(item.kind), plate = KnotPlate.of(knot.signRegion))
        }
        Text(
          item.distance,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        item.arrival?.let {
          Text(
            it,
            style = MaterialTheme.typography.bodyLarge,
            color = arrivalColor(item.state),
            fontWeight =
              when {
                arrivalLook(item.state).bold -> FontWeight.Bold
                item.state != null -> FontWeight.Medium
                else -> null
              },
          )
        }
      }
    }
  }
}

/**
 * How an arrival below a ferry looks, see `knot-navigation` "Ferry warning": bold where the ferry
 * is closed then, next to its clock badge, green where it runs.
 */
internal enum class ArrivalLook(val bold: Boolean, val green: Boolean) {
  PLAIN(bold = false, green = false),
  OPEN(bold = false, green = true),
  WARNING(bold = true, green = false),
}

internal fun arrivalLook(state: FerryArrivalState?): ArrivalLook =
  when (state) {
    FerryArrivalState.WARNING -> ArrivalLook.WARNING
    FerryArrivalState.OPEN -> ArrivalLook.OPEN
    else -> ArrivalLook.PLAIN
  }

/** The arrival's color: never red, so a warning never reads like a German knot. */
@Composable
private fun arrivalColor(state: FerryArrivalState?): Color =
  when (arrivalLook(state)) {
    ArrivalLook.WARNING -> MaterialTheme.colorScheme.onSurface
    ArrivalLook.OPEN -> ActionColors.iconGreen
    ArrivalLook.PLAIN -> MaterialTheme.colorScheme.onSurfaceVariant
  }

private fun roleOf(kind: ProgressMarkKind): KnotRole =
  when (kind) {
    ProgressMarkKind.START -> KnotRole.START
    ProgressMarkKind.STOP -> KnotRole.STOP
    ProgressMarkKind.END -> KnotRole.END
    else -> KnotRole.PASSED
  }

/**
 * The marks that pass [include] and fit with [space] around them and are [apart], the more
 * important ones first; in the strip's order.
 */
private fun shown(
  marks: List<ProgressMark>,
  lineWidth: Dp,
  space: Dp,
  apart: (ProgressMark, ProgressMark) -> Boolean = { _, _ -> true },
  include: (ProgressMark) -> Boolean,
): List<ProgressMark> {
  val shown = mutableListOf<ProgressMark>()
  for (mark in marks.filter(include).sortedBy { it.rank }) {
    if (
      shown.all {
        lineWidth * abs(it.fraction - mark.fraction).toFloat() >= space && apart(it, mark)
      }
    ) {
      shown += mark
    }
  }
  return shown.sortedBy { it.fraction }
}
