# Proposal

## Why

While navigating, the panel shows the number to follow, the knot badges and the ferries, but not where the rider is on the route: how far it is to the next knot, to the next stop or to the end, and when they get there. The badge row does not even move along as the ride progresses. Since `warn-about-closed-ferries`, the core estimates the rider's pace for the ferries; the same estimate can answer these questions for the whole route.

## What Changes

- While navigating, a progress strip takes the place of the knot badge row: the route drawn to scale, with the start, the stops, the end and the ferries labeled, the other knots as small marks, the rider as a dot moving along it and the ridden part filled.
- Below it, a table with distance and time to the next knot, and distance, time and arrival to the next stop (if one is left before the end) and to the end, e.g. "End 31 · 12.4 km · 47 min · 19:07". Distances run along the route from the rider's position; times use the rider's pace, the same as for the ferries. Both update with each position while the app is on screen.
- The Android notification and the iOS Live Activity add the distance to the next knot and the arrival at the end, e.g. "0.8 km to 12 · end around 19:05", updated when a knot is passed, without extra location fixes.
- While planning, the route's header shows the estimated time next to the distance at the assumed pace, e.g. "12.4 km · about 47 min".
- The pace and the distances along the route move from the ferry estimate into one route progress model in the core, which the ferries, the table, the strip and the notification share.

Out of scope:
- Spoken progress, e.g. the remaining distance at stops.
- More frequent location fixes with the screen off; the notification's distance is as of the last knot passed.
- A departure time while planning, and paces by terrain, wind or elevation.
- Progress on the map itself, and a strip while planning.
- Changing the ferry warnings or their texts.

## Capabilities

### New Capabilities

### Modified Capabilities
- `knot-navigation`: new requirements "Progress strip" and "Distances and times ahead"; "Navigation with the screen off" adds the distance to the next knot and the arrival at the end
- `map-planning`: "Show the route" adds the estimated time to the distance

## Impact

- `modules/core`: a route progress model (positions along the route, pace, the texts) used by `Navigation`, `FerryWatch` and `MapFeatures.describe`; `Navigation.start` learns which knots are stops
- `apps/android` `PlannerScreen.kt`, `NavigationService.kt` and `apps/ios` `PlannerView.swift`, `Navigator.swift`, `Shared/NavigationActivity.swift` and the widget: the strip, the table, the notification and Live Activity line, the planning time
- No change to the graph format or the location sampling
