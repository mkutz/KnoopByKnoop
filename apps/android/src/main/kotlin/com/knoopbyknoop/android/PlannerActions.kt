package com.knoopbyknoop.android

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * The actions' colors, see `map-planning` "Clear actions in the planner panel": darker than the
 * badges' green, so labels and icons stay readable on the panel.
 */
object ActionColors {
  val green = Color(0xFF237A46)
  val greenContainer = Color(0xFFD7EEDF)
  val red = Color(0xFFB3261E)
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
          contentColor = ActionColors.green,
        ),
    ) {
      Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(20.dp))
    }
  }
}
