## Why

On Android, Navigate is a floating button on the panel's edge, but Stop sits inside the navigation header, so starting and ending a ride happen in different places. Both buttons are also round, which is the iOS look carried over: Android's own floating button (Material 3, as in Google Maps) is a rounded square. Each app should feel native to its platform, so Android gets Material's floating button for both, in one place, and iOS keeps its round buttons in the sheet.

## What Changes

- On Android in portrait, Navigate becomes a standard Material 3 floating action button: a rounded square of 56 dp (today a 64 dp circle) with the navigation arrow and no label, at the panel's top right edge as now.
- On Android in portrait, while navigating, Stop leaves the navigation header and takes Navigate's place: the same floating button in red with the stop icon. The header row gets its full width for the numbers and badges. The map buttons stand above it, as they stand above Navigate while planning.
- Over the lock screen, the hold-to-stop button takes the same place and the same rounded-square shape, outlined, with the ring filling along its edge while held. No label on the button; a tap still shows "Hold to stop".
- Mockups the maintainer chose from: https://claude.ai/artifact/2HKXUf1anr1eF7DdWHxvHE (plain, 56 dp).

Out of scope:
- iOS: Navigate and Stop stay round buttons in the sheet's header, as Apple Maps keeps Go and End in its sheet.
- Landscape on Android: Navigate and Stop stay round icon buttons at the end of the side panel's header; there is no floating button there.
- An extended floating button with a label ("Navigate", "Stop", "Hold to stop"): rejected, it covers too much map.
- The notification's and the Live Activity's Stop.
- Colours: the green and red of today stay.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: "Clear actions in the planner panel" (Navigate and Stop as Material floating buttons on Android in portrait, Stop no longer in the header there); "Map buttons in groups" (the column stands above Stop while navigating on Android, as above Navigate).
- `knot-navigation`: "Number to follow at large text sizes" (Stop is in the header only where it still is: iOS and Android in landscape); "Navigation over the lock screen" (the hold-to-stop button in the floating button's place and shape).

## Impact

- `apps/android`: `PlannerScreen.kt` (`NavigateFab`, `NavigationHeader`, the room kept for the button below the map column), `PlannerActions.kt` (`StopIconAction`, `HoldAction` drawing a rounded square and its ring).
- `openspec/checked-by-hand.md`: the changed scenarios are checked on the emulator and the Pixel.
- No change to the core, to iOS or to stored data.
