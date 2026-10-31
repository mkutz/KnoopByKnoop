package com.knoopbyknoop.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The green and orange of the app icon, see `design/icon`. */
private val lightColors =
  lightColorScheme(
    primary = Color(0xFF2E9E5B),
    secondary = Color(0xFFF28C28),
    secondaryContainer = Color(0xFFD7F0E0),
    surfaceVariant = Color(0xFFEEF1EA),
  )

/** The same green and orange on dark surfaces, see `map-planning` "Light and dark appearance". */
private val darkColors =
  darkColorScheme(
    primary = Color(0xFF2E9E5B),
    onPrimary = Color.White,
    secondary = Color(0xFFF28C28),
    secondaryContainer = Color(0xFF1E3B2B),
    onSecondaryContainer = Color(0xFFD7F0E0),
    surfaceVariant = Color(0xFF2B2F2A),
  )

/** The appearance chosen in the info sheet, see `map-planning` "Light and dark appearance". */
enum class Appearance {
  SYSTEM,
  LIGHT,
  DARK,
}

/** Whether the app is dark, resolved from the [Appearance] and the system by [KnoopTheme]. */
private val LocalDark = staticCompositionLocalOf { false }

/** Whether the app is dark: the map, the panel and the controls all follow it. */
@Composable @ReadOnlyComposable fun isAppInDarkTheme(): Boolean = LocalDark.current

/**
 * Light or dark as chosen, by default like the system, see `map-planning` "Light and dark
 * appearance".
 */
@Composable
fun KnoopTheme(appearance: Appearance, content: @Composable () -> Unit) {
  val dark =
    when (appearance) {
      Appearance.SYSTEM -> isSystemInDarkTheme()
      Appearance.LIGHT -> false
      Appearance.DARK -> true
    }
  CompositionLocalProvider(LocalDark provides dark) {
    MaterialTheme(colorScheme = if (dark) darkColors else lightColors, content = content)
  }
}

/** The background of the controls floating over the map: white, or dark grey in dark mode. */
val mapOverlayColor: Color
  @Composable @ReadOnlyComposable get() = if (isAppInDarkTheme()) Color(0xFF2A2C2A) else Color.White
