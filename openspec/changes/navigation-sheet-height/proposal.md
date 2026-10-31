# Proposal

## Why

In portrait, the panel is a sheet over the map. Its smallest height (150 dp on Android, 170 pt on iOS) was chosen for planning. When navigation starts with the sheet at that height, the sheet stays there, and the progress strip's distances and arrivals below the marks are cut off. The strip is the main thing to read on the ride, so it should be readable without dragging the sheet.

## What Changes

- In portrait, starting navigation moves the sheet to the height of the navigation part of the panel: the number to follow and the progress strip with its distances and arrivals. This happens whatever height the sheet had while planning.
- While navigating, the sheet keeps that height and shows no drag handle. There is nothing more to show above the strip yet; dragging comes back with the content for it.
- When navigation ends, by Stop or by arriving, the sheet goes back to its smallest planning height.
- Landscape does not change, because the side panel already shows the whole strip.

## Out of Scope

- New content for the higher sheet while navigating, such as a list of the coming knots with distances and arrivals. That will be a separate change.
- Replacing the sheet with an overlay on the map while navigating.
- Changes to the progress strip itself, its labels, or the planning sheet's heights.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: a new requirement for the panel's height in portrait while navigating.

## Impact

- `apps/android/.../PlannerScreen.kt`: the `BottomSheetScaffold` peek height and sheet state follow navigation.
- `apps/ios/KnoopByKnoop/PlannerView.swift`: the sheet's detents and selected detent follow navigation.
- No changes to the core or the pipeline.
