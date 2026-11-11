# Design

## Context

The app sets its language with `AppCompatDelegate.setApplicationLocales` (`InfoSheet.kt`), stored by AppCompat (`autoStoreLocales`). On Android 13 and later the system applies it to the whole app, the service included, and calls `onConfigurationChanged` on the running `NavigationService`, which then switches. Before Android 13, AppCompat applies it only to its activities. The `Application` and the `Service` keep the phone's configuration:

- `AppViewModel.text()` already reads its strings through a configuration context with the chosen locales.
- `NavigationService` reads `resources.configuration.locales[0]` for the session's language, and `getString` for the channel, the Stop action and "starting navigation".
- `Speaker` takes its locale from the context it gets.
- No `onConfigurationChanged` reaches the service when the rider picks a language in the info sheet.

The app supports Android 8 (API 26) and later.

## Goals / Non-Goals

**Goals:**
- One place that answers "which locale does the rider want", used by the view model, the service and the speaker.
- A language picked while navigating reaches the running service on every Android version.

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

2. **The info sheet tells a running navigation about the language.**
   - After `setApplicationLocales`, `Navigating` gets the new locale in a `StateFlow`.
   - The service collects it and does what `onConfigurationChanged` does today: rename the channel, `speaker.useLocale`, `session.inLanguage`.
   - The existing `onConfigurationChanged` stays for Android 13 and later and for a change in the system settings. Both paths end in one function, which ignores a language that is already in use, so a change arriving twice is harmless.

   *Alternative:* restarting the service. That would drop the precise fix sampling and could speak again, which "Nothing else changes" forbids.

3. **The decimal separator follows from the texts' language.** The core formats numbers by its `Language`, so the session's language from `AppLocale` is enough for "1,6 km".

## Risks / Trade-offs

- **AppCompat's stored locales are read before an activity ran:** `getApplicationLocales()` returns empty in a fresh process before AppCompat restored them. Navigation starts from the activity, so they are restored by then. The helper falls back to the phone's language otherwise, which is today's behaviour.
- **Two paths on Android 13 and later:** both a configuration change and the flow arrive. The language check makes the second a no-op, covered by a unit test of the shared function where it can be one, and by the emulator check.
