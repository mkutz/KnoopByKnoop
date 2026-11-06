# Design

## Context

- **Today:** `InfoSheet` calls `AppCompatDelegate.setApplicationLocales`. The activity's `configChanges` lacks `locale`, so Android recreates `MainActivity`: Compose starts over, `NetworkMap` creates a new `MapView`, its style loads, the knot badges are drawn anew and all layers are handed over again. `AppViewModel` survives, so the route and the network stay loaded; the map and the screen are what is rebuilt.
- **Measured** on the Pixel 7: about 0.5–0.6 s frozen per switch, the relaunch's first frame at 560–600 ms.
- **Already in place:** `MainActivity` reads the language from `LocalConfiguration` and passes it to `AppViewModel.useLanguage`, which rewrites the core's texts. `NavigationService.onConfigurationChanged` follows a language change while navigating, see "Language change while navigating". `rememberSaveable` keeps the open info sheet, which won't be needed any more.

## Goals / Non-Goals

**Goals:** no rebuild of the activity or the map on a language change; every text follows the new language.

**Non-Goals:** right-to-left layouts, as none of the four languages needs them; the language of the base map's own labels, which come from OpenStreetMap.

## Decisions

**Handle the change in the activity.** Add `locale|layoutDirection` to `MainActivity`'s `configChanges`. AppCompat 1.7 then delivers the new locale through `onConfigurationChanged` on Android 13 and later via the platform's per-app language, and doesn't recreate the activity on older versions when the activity handles `locale`.
- Compose updates `LocalConfiguration` and recomposes; `stringResource` reads the activity's resources, which carry the new configuration.
- **Alternative:** keep the restart and make it cheaper, e.g. by keeping the `MapView` across activities. Fragile, and still a visible restart.

**Strings read outside composition.** Strings fetched once and kept, e.g. by `AppViewModel.text(...)` for messages, in remembered lambdas, or on the map (badge names are numbers, not words), would stay in the old language. Task 1.2 lists every `getString`/`stringResource` outside a composable's body and decides each.

**The map's own texts.** The attribution and the zoom buttons' screen reader words come from Compose and MapLibre; MapLibre's attribution dialog reads resources when opened, so it follows.

## Strings read outside composition (task 1.2)

Every `getString` or resource read in code outside a composable's body, as of 2026-11-06:

- **`AppViewModel.text(...)`** for snackbar messages (`needs_location_navigate`, `position_not_found`, `needs_location_position`, `waypoint_removed`): read at the moment of the event, so they use the language then. They read the application's resources, which on Android 13 and later follow the per-app language; on older versions they followed the phone before this change as well. Checked on Android 12 in task 1.4.
- **`AppViewModel.language`:** set at start, then from `MainActivity`'s `LocalConfiguration` through `useLanguage`, which recomposes on a change.
- **`MainActivity` snackbar action, `GpxExport` chooser title:** read when shown.
- **`NavigationService`:** the starting notification and the Stop action are built for each notification; `onConfigurationChanged` already switches navigation's texts and voice. The notification channel's name was set once: it is now created again on a change, which renames it.
- **`Speaker.locale`:** switched by `NavigationService.onConfigurationChanged` through `useLocale`.

## Found on Android 12 (task 1.4)

- **Before Android 13, AppCompat doesn't switch an activity that handles `locale`:** the chosen language showed in the menu, but the texts stayed. On Android 12 and older, choosing a language now rebuilds the screen as before this change; from Android 13 it switches in place.
- **Messages from `AppViewModel`** read the application's resources, which keep the phone's language before Android 13, also before this change. They are now read in the language chosen in the app.

## Risks / Trade-offs

- **[A text stays in the old language]** → Task 1.2's list, and a check of every screen in all four languages on the emulator: planner, info sheet, waypoint list, place menu, search, navigation header, notification, ferry popover.
- **[Older Android versions behave differently]** → minSdk is 26: check on an Android 12 emulator too, where AppCompat applies the locale itself.
- **[The system's per-app setting]** → changing it while the app is open must also switch in place: check it.

## Migration Plan

None.
