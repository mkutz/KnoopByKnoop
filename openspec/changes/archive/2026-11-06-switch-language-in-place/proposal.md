# Proposal

## Why

An Android tester found the language switch in the info sheet slow; their screenshot shows the language menu frozen mid-animation. Measured on the maintainer's Pixel 7 on 2026-11-06, over eight switches: each one rebuilds the whole screen, map included. The log says "Skipped 30–38 frames" each time, about half a second frozen, the first new frame takes 560–600 ms, and the roughly 440 knot badges (about 7 MB) are drawn again every time. 20% of all frames were slow, the slowest 1% at 500 ms. The app's spec already asks for the switch to happen "right away without restarting the app"; on Android the activity is restarted, as it doesn't declare that it handles a locale change itself.

## What Changes

- **Android:** choosing a language in the info sheet switches the texts in place. The map, the route, its map layers and a running navigation stay as they are, and nothing is loaded again. The same holds when the language is changed in the system's per-app language setting while the app is open.

## Out of Scope

- **iOS,** where the language already switches in place.
- **The appearance switch,** done in `defer-map-appearance`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `app-languages`: "Language setting" says the switch keeps the map and the route as they are, with a scenario for it.

## Impact

- **Android:** `AndroidManifest.xml` (`locale|layoutDirection` in the activity's `configChanges`); every place that reads a string from resources outside Compose's recomposition, so it follows the new locale; `NavigationService` already follows a configuration change.
