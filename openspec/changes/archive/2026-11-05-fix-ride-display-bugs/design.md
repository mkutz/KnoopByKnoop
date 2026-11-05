# Design

## Context

- **Following** (`NetworkMap.kt`): `showNavigation` runs from a `LaunchedEffect` on every new position and calls `animateCamera(newLatLng(rider))`, a fresh camera animation per fix (once a second while the app is visible, `Precision.LIVE`). The rider's position and the ridden track are GeoJSON sources set again with each fix. MapLibre places the base map's symbols (POI icons, street names) again after camera moves and source changes; symbols that lose a collision fade out and back in.
- **Compass**: the Android map leaves MapLibre's compass at its defaults, top right with a small margin, which ignores the status bar inset; it shows once the map's bearing is not north. iOS hides `compassView`.
- **Off-route header** (`OffRouteHeader` in `PlannerScreen.kt`): a `Row` of the text and the `KnotBadge`. The text is measured first and takes the whole width when it wraps, so the badge gets none. The English "Off route, next" fits in one line, the German "Abseits der Route, nächster" does not.
- **Dark red**: the header text, the arrow and the strip's dot use `MaterialTheme.colorScheme.error`. `darkColors` does not set `error`, so Material's default dark error `#FFB4AB`, a pale pink, applies. The map's off-route line is `#D32F2F` in both appearances.

## Goals / Non-Goals

**Goals:**
- Fix the four findings on Android with the smallest changes, and check iOS for the same situations.

**Non-Goals:**
- Changing which controls the map has, or the off-route header's content.

## Decisions

**Find the jitter's cause before fixing it.** On the emulator, navigating with a mocked position each second along a street with POIs, a screen recording shows the flicker as in the ride video. Then, one at a time: (a) following without updating the position and track sources; (b) updating the sources without moving the camera. The fix follows the cause:
- Camera: move with one `easeCamera` lasting about the fix interval, not cancelled by the next fix, or `moveCamera` when the rider is close to the center, so placement is not redone mid-animation every second.
- Sources: give the rider's and the track's layers `iconIgnorePlacement`/`iconAllowOverlap` (and `textIgnorePlacement`), so they take no part in collisions and do not push base map symbols out.
- Both, if both contribute. If neither is the cause, the task pauses with the recordings rather than guessing.
- Alternative: turning off symbol fading. Hides the symptom, labels would still jump.
- Found: the camera. With the camera still and the sources updated, the labels stay; the fix is one linear `easeCamera` of 1 s per fix (see `manual-checks.md`).

**Compass below the search field.** `uiSettings.setCompassMargins` with the top margin where the controls at the top end (measured, so it includes the status bar inset, the search field and, while planning, the network switch), updated when they change. The compass stays, as on Android a rotated map otherwise has no way back to north.
- Found while implementing: at the end, below the controls, the compass sits behind the zoom buttons in landscape, where the map is low and the centered zoom buttons reach up to it. So it goes to the start, aligned with the controls above it, clear of a cutout there.
- Alternative: hide it as on iOS. A map rotated by a two-finger gesture would then stay rotated without a hint.

**Badge after the text, wrapping as a whole.** `OffRouteHeader` puts the text and the badge in a `FlowRow` (as the number to follow does since `strip-at-large-text`), and the text in one piece, so the badge follows it in the same line when it fits and in the next when not.
- Alternative: `Modifier.weight(1f, fill = false)` on the text. Keeps the badge in the line but squeezes the text further, wrapping it more often.

**A dark red for dark mode.** `darkColors` sets `error` to a clear red with at least 4.5:1 contrast to the dark panel's surface, checked with a contrast calculation, close to the map line's `#D32F2F` in hue. `onError` and the containers stay as Material derives them.
- Alternative: a separate off-route color instead of `error`. Three places already use `error` for exactly this, and the ferry warning uses it too, where pink is just as wrong.

## Risks / Trade-offs

- [The jitter has a cause outside both hypotheses, e.g. the panel's height changing the map's padding] → The recordings in task 1.1 tell; a third cause pauses the task with the evidence.
- [The new red changes the ferry warning in dark mode] → Intended; checked in the same pass.
- [iOS, found in the simulator check: the German text was cut off rather than the badge, and at accessibility sizes Stop and the distance were too] → `offRouteHeader` uses `ViewThatFits` (badge below the wrapping text), the distance row moves the direction below where needed, and at accessibility sizes Stop goes above the off-route header. The number-to-follow header is unchanged.

