# Design

## Context

The app sets its language with `AppCompatDelegate.setApplicationLocales` (`InfoSheet.kt`), stored by AppCompat (`autoStoreLocales`). On Android 13 and later the system applies it to the whole app, the service included, and calls `onConfigurationChanged` on the running `NavigationService`, which then switches. Before Android 13, AppCompat applies it only to its activities. The `Application` and the `Service` keep the phone's configuration:

- `AppViewModel.text()` already reads its strings through a configuration context with the chosen locales.
- `NavigationService` reads `resources.configuration.locales[0]` for the session's language, and `getString` for the channel, the Stop action and "starting navigation".
- `Speaker` takes its locale from the context it gets.
- On a change of the phone's language, `onConfigurationChanged` switches the service to the new configuration's locale, before Android 13 the phone's, even when the rider chose another in the app. The info sheet isn't there while navigating (`network-sync` "Info sheet"), so the app's own choice can't change during a ride.

The app supports Android 8 (API 26) and later.

## Goals / Non-Goals

**Goals:**
- One place that answers "which locale does the rider want", used by the view model, the service and the speaker.
- A change of the phone's language while navigating keeps the language chosen in the app.

**Non-Goals:**
- Changing how Android 13 and later works; the same path just keeps working there.
- Changing the core: it already takes the language as a parameter.

## Decisions

1. **A small `AppLocale` helper in the app.**
   - `AppLocale.of(context)` returns the first of `AppCompatDelegate.getApplicationLocales()`, or the context's own locale when none is chosen.
   - `AppLocale.context(context)` returns a configuration context with that locale, or the context itself when none is chosen.
   - `AppViewModel.text()` moves onto it.
   - `NavigationService` uses it for the session's language, every `getString` and the `Speaker`'s locale.

   *Alternative:* overriding `attachBaseContext` in the service with the chosen locale. That covers the strings but not a change while the service runs, and it hides the logic away from the view model's copy.

2. **A configuration change switches to the chosen language, not the new configuration's.** `onConfigurationChanged` hands `AppLocale.of` to the one function that switches language, which does nothing for the language already in use. With System chosen, that is the phone's new language as today; with a language chosen, navigation keeps it.

   *Alternative, first planned and dropped while applying:* a signal from the info sheet's language choice to the running service. The info sheet is hidden while navigating, so it could never fire.

3. **The decimal separator follows from the texts' language.** The core formats numbers by its `Language`, so the session's language from `AppLocale` is enough for "1,6 km".

## Risks / Trade-offs

- **AppCompat's stored locales are read before an activity ran:** `getApplicationLocales()` returns empty in a fresh process before AppCompat restored them. Navigation starts from the activity, so they are restored by then. The helper falls back to the phone's language otherwise, which is today's behaviour.
