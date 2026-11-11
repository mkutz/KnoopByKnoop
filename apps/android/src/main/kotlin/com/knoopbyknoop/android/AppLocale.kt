package com.knoopbyknoop.android

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

/**
 * The language the rider chose in the app, see `app-languages` "Language setting". Before Android
 * 13 the chosen language reaches only the app's activities: the application and the navigation
 * service keep the phone's, so they read it here.
 */
object AppLocale {

  /** The chosen locale, or [context]'s own when the app follows the phone. */
  fun of(context: Context): Locale =
    AppCompatDelegate.getApplicationLocales()[0] ?: context.resources.configuration.locales[0]

  /** [context] with its resources in the chosen language, or [context] when none is chosen. */
  fun context(context: Context): Context {
    val chosen = AppCompatDelegate.getApplicationLocales()
    if (chosen.isEmpty) return context
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(chosen.toLanguageTags()))
    return context.createConfigurationContext(configuration)
  }
}
