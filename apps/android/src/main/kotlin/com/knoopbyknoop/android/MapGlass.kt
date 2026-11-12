package com.knoopbyknoop.android

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * The look of everything floating over the map: a slightly translucent surface with a soft border
 * and shadow, see `map-planning` "Glass on the map". The map is drawn on its own surface, so what
 * is behind it cannot be blurred as on iOS.
 */
@Composable
fun Modifier.mapGlass(shape: Shape = RoundedCornerShape(16.dp)): Modifier {
  val dark = isAppInDarkTheme()
  return shadow(6.dp, shape)
    .background(mapOverlayColor.copy(alpha = 0.88f), shape)
    .border(1.dp, Color.White.copy(alpha = if (dark) 0.12f else 0.6f), shape)
}

/** A group of map buttons on one glass background, see `map-planning` "Map buttons in groups". */
@Composable
fun MapButtonGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Column(modifier = modifier.width(MAP_BUTTON_SIZE).mapGlass(RoundedCornerShape(16.dp))) {
    content()
  }
}

/** A thin line between the buttons of a group. */
@Composable
fun MapButtonDivider() {
  HorizontalDivider(color = Color.Gray.copy(alpha = 0.25f))
}

/** One button of a group, in the app's green, announced by [label]. */
@Composable
fun MapButton(label: String, @DrawableRes icon: Int, onClick: () -> Unit) {
  IconButton(
    onClick = onClick,
    colors = IconButtonDefaults.iconButtonColors(contentColor = Green),
    modifier = Modifier.size(MAP_BUTTON_SIZE),
  ) {
    Icon(painterResource(icon), contentDescription = label)
  }
}

/** The size of a map button, and the width of the column. */
val MAP_BUTTON_SIZE = 48.dp
