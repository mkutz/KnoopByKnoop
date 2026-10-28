# Proposal

## Why

On iOS in portrait, tapping the search field moves it down to where the on-screen keyboard is expected to lift it. With a hardware keyboard, no on-screen keyboard comes up, so the field and the attribution end up behind the waypoint sheet, while the hits are still listed above it. The user types into a field they cannot see. This happens with a Bluetooth keyboard on an iPhone, and always in the simulator with the Mac keyboard connected, which is where it was found during the manual checks of `show-ferry-schedules`. It is not a regression of that change: the search overlay has not changed since `move-search-above-sheet`.

## What Changes

- On iOS in portrait, the search field moves down above the keyboard only when an on-screen keyboard actually covers the bottom of the screen. Without one, the field stays right above the waypoint sheet while the user types, with the hits above it.
- The sheet is lowered to its smallest height only when an on-screen keyboard comes up, not on every tap into the field.
- Whether the field is hidden because the sheet is pulled up also depends on the keyboard, not on the field having the focus.

Out of scope: Android, whose field sits at the top of the map and is not affected; landscape on iOS, where the field already stays at the bottom of the map either way; hiding or moving the sheet while searching; any change to the search itself, the hits or their ranking.

## Capabilities

### New Capabilities

### Modified Capabilities
- `place-search`: on iOS, where the field stays while the user types without an on-screen keyboard

## Impact

- `apps/ios`: `PlannerView` tracks whether an on-screen keyboard covers the screen and uses that instead of the field's focus to place the search overlay, to hide it and to lower the sheet
- No changes to `modules/core` or `apps/android`
