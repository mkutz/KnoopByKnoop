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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
 * Stop over the lock screen: round and outlined, the size of the Stop in the app, with a ring that
 * fills around it while held, and the label as a hint on a tap, so it doesn't pass for the usual
 * Stop button, see `knot-navigation` "Navigation over the lock screen".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldAction(label: String, @DrawableRes icon: Int, onHold: () -> Unit) {
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
        Modifier.size(STOP_SIZE)
          .drawBehind {
            val outline = OUTLINE_WIDTH.toPx()
            val ring = RING_WIDTH.toPx()
            drawCircle(surface)
            drawCircle(
              ActionColors.red,
              radius = size.minDimension / 2 - outline / 2,
              style = Stroke(outline),
            )
            if (fill.value > 0f) {
              drawArc(
                ActionColors.red,
                startAngle = -90f,
                sweepAngle = 360f * fill.value,
                useCenter = false,
                topLeft = Offset(ring / 2, ring / 2),
                size = Size(size.width - ring, size.height - ring),
                style = Stroke(ring, cap = StrokeCap.Round),
              )
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

/** The size of the Stop buttons, in the app and over the lock screen. */
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
