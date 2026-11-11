# Proposal

## Why

On Android 12 and older, a rider who chose another language in the app than the phone's gets navigation in the phone's language. On the API 31 emulator on 2026-11-11, with the app set to German on an English phone, the planner showed German, but the navigation notification said "Stop" and "1.6 km", and the voice said "Next 2, then 28". `app-languages` already asks for the chosen language in the notices and outside the app. Before Android 13 the chosen language only reaches the app's screens. The planner works around that already, the navigation service doesn't.

## What Changes

- Android 12 and older: navigation runs in the language chosen in the app. This covers:
  - the notification's texts, including Stop
  - the channel's name and description
  - the numbers' decimal separator
  - the "starting navigation" text
  - the spoken notices, where the phone has a voice for the language
- Android 12 and older: choosing another language in the info sheet while navigating switches navigation at once, as on Android 13 and later ("Language change while navigating").
- No change on Android 13 and later or on iOS, where this already works.

Out of scope:
- The system's per-app language setting, which only exists on Android 13 and later.
- Other texts outside the app on Android 12, such as the location permission prompt, which the system writes.
- The emulator-only out-of-memory crash seen while checking, which has its own note.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `app-languages`: "Language setting" gets a scenario for navigation on Android 12 and older.

## Impact

- `apps/android`:
  - `NavigationService.kt`: texts, the session's language, the channel
  - `Speaker.kt`: the voice's locale
  - a shared helper for the chosen language, also used by `AppViewModel.text()`
  - the info sheet's language choice, which tells a running navigation about the change
- No core, iOS, pipeline or data changes.
