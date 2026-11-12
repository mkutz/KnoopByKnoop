package com.knoopbyknoop.android

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.KnotPlate
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.nameIn

/**
 * The panel with fewer than two waypoints: "Plan a ride", the start and the end slot, a hint and
 * the ways to begin, see `map-planning` "Panel before a route".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyRoute(
  description: RouteDescription.StartOnly?,
  noKnotsInView: Boolean,
  language: com.knoopbyknoop.core.Language,
  onSearch: (() -> Unit)?,
  onNearest: () -> Unit,
  onClear: () -> Unit,
) {
  val waypoint = description?.waypoint
  val endFirst = description?.endFirst == true
  val start = waypoint.takeUnless { endFirst }
  val end = waypoint.takeIf { endFirst }
  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
  ) {
    Text(stringResource(R.string.plan_a_ride), style = MaterialTheme.typography.titleLarge)
    val startLabel = stringResource(R.string.role_start)
    val endLabel = stringResource(R.string.role_end)
    val startPlace = start?.let { placeOf(it, language) }
    val endPlace = end?.let { placeOf(it, language) }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Slot(startLabel, start, startPlace, KnotRole.START, next = start == null)
      val dots = MaterialTheme.colorScheme.outline
      Canvas(Modifier.weight(1f).height(2.dp).padding(horizontal = 8.dp)) {
        drawLine(
          dots,
          Offset(0f, size.height / 2),
          Offset(size.width, size.height / 2),
          strokeWidth = 2.dp.toPx(),
          cap = StrokeCap.Round,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 6.dp.toPx())),
        )
      }
      Slot(endLabel, end, endPlace, KnotRole.END, next = start != null && end == null)
    }
    // Read with the slots, see `Slot`.
    Row(modifier = Modifier.clearAndSetSemantics {}) {
      SlotLabel(startLabel, startPlace, Modifier.weight(1f), TextAlign.Start)
      SlotLabel(endLabel, endPlace, Modifier.weight(1f), TextAlign.End)
    }
    Text(
      stringResource(
        when {
          waypoint == null && noKnotsInView -> R.string.no_knots_in_view
          waypoint == null -> R.string.tap_for_start
          endFirst -> R.string.now_tap_for_start
          else -> R.string.now_tap_for_end
        }
      ),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (onSearch != null)
        TonalAction(stringResource(R.string.search_a_place), R.drawable.ic_search, onSearch)
      when {
        waypoint != null ->
          TonalAction(stringResource(R.string.clear), R.drawable.ic_action_clear, onClear)
        // The prominent action with no knots in view, see `map-planning` "Nearest knots".
        noKnotsInView ->
          Button(
            onClick = onNearest,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = ActionColors.green,
                contentColor = Color.White,
              ),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
          ) {
            ActionLabel(stringResource(R.string.nearest_knots), R.drawable.ic_nearest_knots)
          }
        else ->
          TonalAction(
            stringResource(R.string.nearest_knots),
            R.drawable.ic_nearest_knots,
            onNearest,
          )
      }
    }
  }
}

@Composable
private fun TonalAction(label: String, @DrawableRes icon: Int, onClick: () -> Unit) {
  FilledTonalButton(
    onClick = onClick,
    colors =
      ButtonDefaults.filledTonalButtonColors(
        containerColor = ActionColors.greenContainer,
        contentColor = ActionColors.iconGreen,
      ),
    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
  ) {
    ActionLabel(label, icon)
  }
}

@Composable
private fun ActionLabel(label: String, @DrawableRes icon: Int) {
  Icon(
    painterResource(icon),
    contentDescription = null,
    modifier = Modifier.size(ButtonDefaults.IconSize),
  )
  Spacer(Modifier.width(ButtonDefaults.IconSpacing))
  Text(label)
}

private fun placeOf(knot: Knot, language: com.knoopbyknoop.core.Language): String? =
  knot.routePoint?.placeName ?: knot.place?.nameIn(language)

/**
 * A slot for the start or the end: the waypoint's badge when set, else a dashed circle, which
 * breathes when the next tap fills it, unless animations are off. Read with its label and place as
 * one, e.g. "Start, 48, De Rips" or "End, not set yet".
 */
@Composable
private fun Slot(label: String, knot: Knot?, place: String?, role: KnotRole, next: Boolean) {
  val notSet = stringResource(R.string.not_set_yet)
  val spoken =
    when {
      knot == null -> "$label, $notSet"
      knot.isRoutePoint -> listOfNotNull(label, place).joinToString(", ")
      else -> listOfNotNull(label, knot.ref, place).joinToString(", ")
    }
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.size(SLOT_SIZE).clearAndSetSemantics { contentDescription = spoken },
  ) {
    if (knot != null)
      KnotBadge(
        knot.ref,
        role,
        scale = 1.15f,
        plate = KnotPlate.of(knot.signRegion),
        place = knot.routePoint?.placeKind,
      )
    else EmptySlot(next)
  }
}

@Composable
private fun SlotLabel(label: String, place: String?, modifier: Modifier, align: TextAlign) {
  Text(
    if (place != null) "$label · $place" else label,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = align,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier,
  )
}

@Composable
private fun EmptySlot(next: Boolean) {
  val context = LocalContext.current
  val animationsOff = remember {
    Settings.Global.getFloat(
      context.contentResolver,
      Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    ) == 0f
  }
  val color = if (next) ActionColors.iconGreen else MaterialTheme.colorScheme.outline
  val halo =
    if (next && !animationsOff) {
      val transition = rememberInfiniteTransition(label = "next slot")
      val value by
        transition.animateFloat(
          0f,
          1f,
          infiniteRepeatable(tween(900), RepeatMode.Reverse),
          label = "halo",
        )
      value
    } else 0f
  val haloColor = ActionColors.green.copy(alpha = 0.18f)
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.size(SLOT_SIZE)
        .drawBehind {
          if (halo > 0f) drawCircle(haloColor, radius = size.minDimension / 2 + 6.dp.toPx() * halo)
        }
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape),
  ) {
    Canvas(Modifier.size(SLOT_SIZE)) {
      val stroke = 2.dp.toPx()
      drawCircle(
        color,
        radius = size.minDimension / 2 - stroke / 2,
        style =
          Stroke(
            stroke,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
          ),
      )
    }
    Text("?", color = color, fontWeight = FontWeight.Bold)
  }
}

private val SLOT_SIZE = 40.dp
