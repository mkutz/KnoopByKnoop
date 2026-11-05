package com.knoopbyknoop.android

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
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
 * Stop over the lock screen: outlined, and filling up while held, so it doesn't pass for the usual
 * Stop button, see `knot-navigation` "Navigation over the lock screen".
 */
@Composable
fun HoldAction(label: String, @DrawableRes icon: Int, onHold: () -> Unit) {
  val currentOnHold by rememberUpdatedState(onHold)
  val fill = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val shape = ButtonDefaults.shape
  val content: @Composable () -> Unit = {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(ButtonDefaults.ButtonWithIconContentPadding),
    ) {
      Icon(
        painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
  }
  Box(
    modifier =
      Modifier.clip(shape)
        .border(2.dp, ActionColors.red, shape)
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
              scope.launch {
                if (fill.value < TAP_FILL) fill.animateTo(TAP_FILL, tween(TAP_FILL_MILLIS))
                fill.animateTo(0f, tween(EMPTY_MILLIS))
              }
            } else {
              scope.launch { fill.snapTo(1f) }
              currentOnHold()
            }
          }
        }
  ) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
      content()
    }
    Box(
      modifier =
        Modifier.matchParentSize().drawWithContent {
          clipRect(right = size.width * fill.value) { this@drawWithContent.drawContent() }
        }
    ) {
      Box(Modifier.matchParentSize().background(ActionColors.red))
      CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
  }
}

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

/** An entry of [MoreActions]: its label, icon and action. */
data class MenuAction(val label: String, @DrawableRes val icon: Int, val onClick: () -> Unit)

/**
 * The "More actions" icon button with a menu of [actions], each with icon and label, so the header
 * stays one row on narrow phones, see `map-planning` "Clear actions in the planner panel".
 */
@Composable
fun MoreActions(actions: List<MenuAction>) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconAction(stringResource(R.string.more_actions), R.drawable.ic_action_more) { expanded = true }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      actions.forEach { action ->
        DropdownMenuItem(
          text = { Text(action.label) },
          leadingIcon = {
            Icon(
              painterResource(action.icon),
              contentDescription = null,
              tint = ActionColors.iconGreen,
              modifier = Modifier.size(20.dp),
            )
          },
          onClick = {
            expanded = false
            action.onClick()
          },
        )
      }
    }
  }
}

/**
 * The round button that opens the info sheet, styled like the zoom buttons, see `network-sync`
 * "Info sheet". The label is read by TalkBack and shown on long press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
    tooltip = { PlainTooltip { Text(stringResource(R.string.info)) } },
    state = rememberTooltipState(),
    modifier = modifier,
  ) {
    SmallFloatingActionButton(
      onClick = onClick,
      containerColor = mapOverlayColor,
      contentColor = Green,
    ) {
      Icon(
        painterResource(R.drawable.ic_action_info),
        contentDescription = stringResource(R.string.info),
      )
    }
  }
}
