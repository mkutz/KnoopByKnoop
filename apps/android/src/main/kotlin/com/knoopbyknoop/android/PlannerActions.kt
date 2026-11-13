package com.knoopbyknoop.android

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The actions' colors, see `map-planning` "Clear actions in the planner panel": darker than the
 * badges' green, so labels and icons stay readable on the panel.
 */
object ActionColors {
  val green = Color(0xFF237A46)
  val red = Color(0xFFB3261E)

  /** The icon of a round action: dark green, or light green on the dark panel. */
  val iconGreen: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF8FD9AE) else green

  /** The background of a round action: light green, or dark green on the dark panel. */
  val greenContainer: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF1E3B2B) else Color(0xFFD7EEDF)

  /** A download that couldn't be loaded, see `network-sync`: amber, as red means Stop. */
  val warning: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFFFFB84D) else Color(0xFFB26A00)
}

/** A filled action with icon and label: Navigate, or Stop in red. */
@Composable
fun ProminentAction(
  label: String,
  @DrawableRes icon: Int,
  onClick: () -> Unit,
  color: Color = ActionColors.green,
) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
  ) {
    Icon(
      painterResource(icon),
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(label)
  }
}

/**
 * Stop over the lock screen: outlined, in the [shape] and [size] of the Stop in the app, with a
 * ring that fills along its edge while held, and the label as a hint on a tap, so it doesn't pass
 * for the usual Stop button, see `knot-navigation` "Navigation over the lock screen".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldAction(
  label: String,
  @DrawableRes icon: Int,
  onHold: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = STOP_SIZE,
  shape: Shape = CircleShape,
) {
  val currentOnHold by rememberUpdatedState(onHold)
  val fill = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val hint = rememberTooltipState()
  val surface = MaterialTheme.colorScheme.surface
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(label) } },
    state = hint,
    // Shown on a tap below, not on the long press that stops.
    enableUserInput = false,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        modifier
          .size(size)
          .drawBehind {
            val outline = OUTLINE_WIDTH.toPx()
            val ring = RING_WIDTH.toPx()
            val corner = cornerRadiusOf(shape, this.size, layoutDirection, this)
            drawRoundRect(surface, cornerRadius = CornerRadius(corner))
            drawRoundRect(
              ActionColors.red,
              topLeft = Offset(outline / 2, outline / 2),
              size = Size(this.size.width - outline, this.size.height - outline),
              cornerRadius = CornerRadius(corner - outline / 2),
              style = Stroke(outline),
            )
            if (fill.value > 0f) {
              val edge =
                ringPath(
                  Rect(ring / 2, ring / 2, this.size.width - ring / 2, this.size.height - ring / 2),
                  corner - ring / 2,
                )
              // Measured along the edge, so the ring fills at an even pace past the corners.
              val measure = PathMeasure().apply { setPath(edge, forceClosed = false) }
              val filled = Path()
              measure.getSegment(0f, measure.length * fill.value, filled, startWithMoveTo = true)
              drawPath(filled, ActionColors.red, style = Stroke(ring, cap = StrokeCap.Round))
            }
          }
          .clearAndSetSemantics {
            contentDescription = label
            role = Role.Button
            onClick {
              currentOnHold()
              true
            }
          }
          .pointerInput(Unit) {
            awaitEachGesture {
              awaitFirstDown()
              val filling = scope.launch {
                fill.animateTo(1f, tween(HOLD_MILLIS.toInt(), easing = LinearEasing))
              }
              // Timed here and not by the animation, which ends at once with animations turned off.
              val released =
                withTimeoutOrNull(HOLD_MILLIS) {
                  waitForUpOrCancellation()
                  true
                } ?: false
              filling.cancel()
              if (released) {
                scope.launch { hint.show() }
                scope.launch {
                  if (fill.value < TAP_FILL) fill.animateTo(TAP_FILL, tween(TAP_FILL_MILLIS))
                  fill.animateTo(0f, tween(EMPTY_MILLIS))
                }
              } else {
                scope.launch { fill.snapTo(1f) }
                currentOnHold()
              }
            }
          },
    ) {
      Icon(
        painterResource(icon),
        contentDescription = null,
        tint = ActionColors.red,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

/** The corner radius of [shape] at [size]: half the size for a circle. */
private fun cornerRadiusOf(
  shape: Shape,
  size: Size,
  layoutDirection: LayoutDirection,
  density: Density,
): Float =
  when (val outline = shape.createOutline(size, layoutDirection, density)) {
    is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
    else -> 0f
  }

/** The edge of a rounded rectangle, clockwise from the middle of its top. */
private fun ringPath(rect: Rect, radius: Float): Path {
  val r = radius.coerceIn(0f, rect.minDimension / 2)
  return Path().apply {
    moveTo(rect.center.x, rect.top)
    lineTo(rect.right - r, rect.top)
    arcTo(Rect(rect.right - 2 * r, rect.top, rect.right, rect.top + 2 * r), -90f, 90f, false)
    lineTo(rect.right, rect.bottom - r)
    arcTo(Rect(rect.right - 2 * r, rect.bottom - 2 * r, rect.right, rect.bottom), 0f, 90f, false)
    lineTo(rect.left + r, rect.bottom)
    arcTo(Rect(rect.left, rect.bottom - 2 * r, rect.left + 2 * r, rect.bottom), 90f, 90f, false)
    lineTo(rect.left, rect.top + r)
    arcTo(Rect(rect.left, rect.top, rect.left + 2 * r, rect.top + 2 * r), 180f, 90f, false)
    close()
  }
}

/** The size of the Stop buttons in the header, in the app and over the lock screen. */
private val STOP_SIZE = 52.dp
private val OUTLINE_WIDTH = 2.dp
private val RING_WIDTH = 5.dp

private const val HOLD_MILLIS = 1000L
private const val TAP_FILL = 0.3f
private const val TAP_FILL_MILLIS = 150
private const val EMPTY_MILLIS = 250

/**
 * A round action with only an icon: Back to start or Clear. The label is read by TalkBack and shown
 * on long press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconAction(label: String, @DrawableRes icon: Int, onClick: () -> Unit) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(label) } },
    state = rememberTooltipState(),
  ) {
    FilledTonalIconButton(
      onClick = onClick,
      colors =
        IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = ActionColors.greenContainer,
          contentColor = ActionColors.iconGreen,
        ),
    ) {
      Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(20.dp))
    }
  }
}

/**
 * Share in the panel: an icon button opening its choices, the route file for Knoop by Knoop and,
 * where it can be made, the GPX file for other apps, see `route-sharing` "Share the route".
 */
@Composable
fun ShareAction(onRouteFile: () -> Unit, onGpx: (() -> Unit)?) {
  var open by remember { mutableStateOf(false) }
  Box {
    IconAction(stringResource(R.string.share), R.drawable.ic_action_export) { open = true }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.share_route_file)) },
        onClick = {
          open = false
          onRouteFile()
        },
      )
      if (onGpx != null)
        DropdownMenuItem(
          text = { Text(stringResource(R.string.share_gpx)) },
          onClick = {
            open = false
            onGpx()
          },
        )
    }
  }
}

/**
 * Stop while navigating: a filled icon button of the same size in every language, named to screen
 * readers and on a long press, see `map-planning` "Clear actions in the planner panel".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopIconAction(label: String, onClick: () -> Unit) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(label) } },
    state = rememberTooltipState(),
  ) {
    FilledIconButton(
      onClick = onClick,
      modifier = Modifier.size(STOP_SIZE),
      colors =
        IconButtonDefaults.filledIconButtonColors(
          containerColor = ActionColors.red,
          contentColor = Color.White,
        ),
    ) {
      Icon(
        painterResource(R.drawable.ic_action_stop),
        contentDescription = label,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

/**
 * The round gear button that opens the info sheet, on its own glass at the top right, see
 * `network-sync` "Info sheet". "Settings" is read by TalkBack and shown on long press.
 */
@Composable
fun InfoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  NamedAction(stringResource(R.string.settings), modifier) {
    IconButton(
      onClick = onClick,
      colors =
        IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
      modifier = Modifier.size(MAP_BUTTON_SIZE).mapGlass(CircleShape),
    ) {
      Icon(
        painterResource(R.drawable.ic_settings),
        contentDescription = stringResource(R.string.settings),
      )
    }
  }
}

/** [content] with its [label] shown on a long press, for icon-only buttons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamedAction(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  // In a box of its own: the tooltip's wrapper drops alignment set on its modifier.
  Box(modifier = modifier) {
    TooltipBox(
      positionProvider =
        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
      tooltip = { PlainTooltip { Text(label) } },
      state = rememberTooltipState(),
    ) {
      content()
    }
  }
}
