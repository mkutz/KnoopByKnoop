# Proposal

## Why

Outside the app, navigation still speaks in sentences ("Next 29, then 25", "Off route, next 02", "1.7 km north-east to 02"). Their length varies with the language, and they don't match the wordless header that `sign-like-header` brings into the app. The rider glances at the lock screen, the Dynamic Island or the status bar far more often than at the app, so these places need the same numbers, plates and arrows. The maintainer approved the layouts on 2026-11-07 (https://claude.ai/artifact/XVeganeauBp3Pe2WGy1qUT, board "Outside the app, every state").

## What Changes

The states are the same as in the app: close to a knot (A), between knots (B), off route (C), on the way to the route (D), plus the guided way and riding the wrong way.

**iPhone Live Activity on the lock screen and in the expanded Dynamic Island:**
- The app's header row and a short strip with the end arrival.
- Off route and riding the wrong way, the card takes the opposite of its usual appearance (light on a dark lock screen, dark on a light one), as the app's header does.

**Dynamic Island:**
- **Compact:** the row in miniature on the leading side and the distance on the trailing side.
  - A: the knot, →, and the number to follow on its plate
  - B: the next knot, › and the knot after it
  - C: the direction arrow on a light tile, and › with the next knot while on a network path
  - D: the plain direction arrow
- **Minimal** (shared island): one element: the plate (A), the next knot (B), the light tile with the arrow (C, wrong way), or the plain arrow (D).

**Android notification**, a Live Update from Android 16:
- A title made only of numbers, arrow characters and distances, e.g. "45 → 12", "83 · 1.2 km › 30", "↖ 1.7 km › 02", "→ 800 m".
- A second line with the end arrival after ■.
- A large icon that carries the picture: the plate (A), the next knot (B), the to-the-route symbol on a dark disc (C) or a light one (D).
- The status chip shows at most 7 characters: "→12", "83", "↖1.7 km", "→800 m". The chip's small icon changes off route and on the way to the route.
- The progress bar marks stops in their plate colour and a ferry you may miss as a dark point.

**Direction outside the app:** the arrow and distance point to the place where the rider gets back onto the route, as in the app. **BREAKING:** the compass word and the sentence go.

## Out of Scope

- **"Hold to stop" over the Android lock screen** keeps its label, as it is the app itself, not the notification.
- **Spoken notices.**
- **The turn direction at a knot:** `turn-at-knot`. The arrow before the plate stays a plain → until then.
- **Colouring the Android notification**, which a Live Update must not do (developer.android.com, "Live Updates").

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`:
  - "Navigation with the screen off": the wordless content per state for the Live Activity, the Dynamic Island, the notification and the chip.
  - "Direction outside the app": an arrow and the distance to the route, no compass word.
  - "Guided way in the header and outside the app": the lock screen scenario.
  - "Riding the wrong way": the outside scenario.

  The last two build on their versions in `sign-like-header`.

## Impact

- **Core:** the outside texts come from `HeaderRow` (from `sign-like-header`), as `notificationTitle`, `notificationLine` and `chipText`, the same in every language. The old sentence texts outside the app go; screen reader and speech texts stay.
- **Android:** `NavigationService.kt`, covering:
  - the title, line and chip
  - small and large icons per state
  - ProgressStyle points
- **iOS:** `apps/ios/KnoopByKnoopWidgets` and `Shared/NavigationActivity.swift`, covering:
  - the activity's content state carries the header row
  - the lock screen view and the island regions per state
  - `Shared/Badges.swift` takes the plates from `knot-colours-from-signs`
- **Depends on** `knot-colours-from-signs` and `sign-like-header`.
