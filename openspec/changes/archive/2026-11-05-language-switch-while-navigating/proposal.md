# Proposal

## Why

Seen during the checks after the test rides of 2026-11-01: when the app's language changes during navigation, the panel changes at once, but navigation's own texts stay in the old language until it is restarted: the off-route direction, the words and the line on the lock screen, the spoken notices and a ferry's popover.

`Navigation` gets its languages once, when it starts, and keeps them. On Android, the language can change at any time, in the system settings for the app, or with the phone's language, while the navigation service keeps running.

## What Changes

- **Navigation follows the language at once:** the words and the line outside the app, the off-route direction and a ferry's estimates change when the language does, without restarting navigation.
- **Spoken notices** switch too, to the new language where the phone has a voice for it, otherwise English, as at the start of navigation.
- **Nothing is spoken for the change itself**, and the route, progress and pace stay as they were.

## Out of Scope

- **iOS code:** the app's language is chosen in Info, which is not available while navigating. A language changed in the iPhone's settings restarts the app, so `Navigator` starts anew and picks it up. The maintainer's steps check that navigation's texts follow after it.
- **Offering the language while navigating:** Info stays as it is.
- **Badges like the real signs:** that idea waits for photos of signs from other areas.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `app-languages`: a new requirement "Language change while navigating".

## Impact

- **Core:** `Navigation.inLanguage` and `FerryWatch.inLanguage`. Tests are in `NavigationTest` and `FerryWarningTest`.
- **Android:** `NavigationService.onConfigurationChanged`, and `Speaker` can change its language.
- **iOS:** no code.
- **Archive order:** after `add-app-languages` and `add-language-setting`, which bring the `app-languages` capability.
