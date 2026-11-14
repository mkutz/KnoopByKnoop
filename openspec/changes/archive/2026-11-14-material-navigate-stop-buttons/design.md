## Context

On Android in portrait, `NavigateFab` (`PlannerScreen.kt`) is a 64 dp `FloatingActionButton` with an explicit `CircleShape` and a half-transparent white border, placed at the panel's top right edge by an offset from the sheet's top (`noteTop() - fabRoom`). The map's button column learns its bottom through `buttonsBottom`, which keeps `fabRoom` free while `showNavigate`. While navigating, `NavigationHeader` ends its row with `StopIconAction` (a 56 dp `FilledIconButton`, `STOP_SIZE`), or with `HoldAction` over the lock screen (an outlined circle with a ring drawn by `drawArc`). Landscape shows Navigate as a round icon button in the side panel's header, and Stop in the navigation header.

Mockups: https://claude.ai/artifact/2HKXUf1anr1eF7DdWHxvHE. The maintainer chose plain 56 dp buttons and rejected the extended version with a label.

## Goals / Non-Goals

**Goals:**
- One floating button slot on Android in portrait: Navigate while planning, Stop while navigating, hold-to-stop over the lock screen.
- Material 3's own floating button shape and size (rounded square, 56 dp).
- The header row without Stop in that layout, so the numbers and badges get the room.

**Non-Goals:**
- iOS, landscape, the notification and the Live Activity stay as they are.
- No extended button with a label.

## Decisions

- **Material's defaults rather than our own shape.** `FloatingActionButton` with its default `shape` (`FloatingActionButtonDefaults.shape`) and default 56 dp size; drop the explicit `CircleShape`, the `size(NAVIGATE_FAB_SIZE)` and the white border. Colours stay `ActionColors.green` and `ActionColors.red` with white icons; their contrast is measured already ("Readable actions"). Alternative: 64 dp in the Material shape, as easier to hit; the maintainer chose the standard size, as in Google Maps.
- **One composable for the slot.** A `ActionFab` placed where `NavigateFab` is now, showing Navigate, Stop or the hold button depending on `state.navigating` and `Navigating.overLockScreen`. The slot is shown while `showNavigate || state.navigating` (portrait only), and `buttonsBottom` keeps `fabRoom` free in both cases, so the map column stands above Stop as above Navigate (scenario "Above Stop on Android"). `fabRoom` becomes 56 dp plus the gap.
- **Stop's semantics as today.** The red button keeps the name "Stop" for TalkBack and its tooltip on a long press, as `StopIconAction` does; only its container changes to the floating button.
- **Header without Stop in portrait.** `NavigationHeader` gets a parameter whether it shows Stop: false in portrait, true in landscape. The row's weight then gives the badges and distances the freed width.
- **The hold button as a rounded square.** `HoldAction` draws a rounded rectangle with the floating button's corner radius (read from the shape's `createOutline`, so it follows Material's value) instead of a circle: the surface fill, the red outline, and the ring as a `Path` along the outline, drawn partly with `PathMeasure.getSegment` from the top centre clockwise for the fill fraction. Its size and place are the floating button's. The tooltip "Hold to stop" and the timing stay. In landscape it stays round in the header.
- **Elevation.** The floating button keeps Material's default elevation; the hold button gets the same shadow so it stands on the map like the red one.

## Risks / Trade-offs

- [A floating Stop on the panel's edge is easier to hit by accident when taking the phone from the mount] → over the lock screen it stays a hold; in the app a tap stops as today. The maintainer judges it on a ride.
- [The button covers a little more map in the lower right while navigating] → 56 dp is smaller than today's 64 dp Navigate; the map's follow position is not changed.
- [The ring along a rounded square fills unevenly in time at the corners if measured by angle] → measured along the path's length, so it fills at an even speed.

## Migration Plan

None; a UI change shipped with the next release.

## Open Questions

None.
