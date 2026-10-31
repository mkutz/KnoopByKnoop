# Proposal

## Why

At large system text sizes, the progress strip becomes hard to read. The distance and arrival below its marks sit in boxes of a fixed size (64 × 40 dp on Android, 70 × 40 pt on iOS), while their text grows with the text size, so "4.1 km" over "17:25" is cut to "4.1" over "17:2". On Android, the knot badges in the panel have a fixed size too, but their numbers grow with the text size, so at the largest size "94" is cut off. Riders who need large text are the ones this hurts most.

## What Changes

- The distance and arrival below the strip's marks get room that grows with the system text size, so they are never cut off. Where the larger labels would overlap, the existing ranking leaves out the less important ones (end first, then ferries with a warning, then stops, then other ferries).
- On Android, the numbers in the knot badges in the panel keep their size whatever the text size, like the badges on the map and like on iOS, so they always fit their badge.
- The panel's height while navigating already follows its content, so the larger labels stay visible.
- While navigating, the distance to the next knot moves below the number to follow where it does not fit next to it at a large text size, instead of being cut off by Stop.
- While planning, the route's distance and time stay in one line each, and the actions move below them where they do not fit next to them, instead of "4.1 km" wrapping and "about 15 min" being cut off.

## Out of Scope

- The badges on the map, which are drawn at a fixed size already.
- Larger knot badges for large text sizes (the numbers stay at the size of the map's badges).
- The list of coming knots while navigating (the next change).
- Text sizes of other parts of the panel, which already grow with the text size; the off-route header, which already uses two lines.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: new requirements for the strip's labels and the number to follow at large text sizes.
- `map-planning`: new requirements that knot badges in the panel keep their numbers' size, and for the route summary at large text sizes.

## Impact

- `apps/android/.../ProgressPanel.kt`: label room scaled by the font scale.
- `apps/android/.../KnotBadges.kt`: `KnotBadge` numbers in dp-based size, independent of the font scale.
- `apps/ios/KnoopByKnoop/ProgressPanel.swift`: label room scaled with Dynamic Type.
- `PlannerScreen.kt` and `PlannerView.swift`: the navigation header and the planning header adapt their layout when their content does not fit in one line.
- No changes to the core or the pipeline.
