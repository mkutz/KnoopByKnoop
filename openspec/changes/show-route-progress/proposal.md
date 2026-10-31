# Proposal

## Why

While navigating, the panel shows the number to follow, the knot badges and the ferries, but not where the rider is on the route: how far it is to the next knot, to the next stop or to the end, and when they get there. The badge row does not even move along as the ride progresses. Since `warn-about-closed-ferries`, the core estimates the rider's pace for the ferries; the same estimate can answer these questions for the whole route.

## What Changes

- While navigating, a progress strip takes the place of the knot badge row: the route drawn to scale, with the start, the stops, the end and the ferries labeled, the other knots as small marks, the rider as a dot moving along it and the ridden part filled.
- Below each stop, ferry and the end shown on the strip, the distance along the route to it and the arrival, e.g. "12.4 km" over "19:05"; where they would overlap, the more important mark keeps them. Distances run along the route from the rider's position; arrivals use the rider's pace, the same as for the ferries. Both update with each position while the app is on screen.
- The number to follow shows the knots as badges like on the map, with the distance to the next knot in the same line, e.g. "At (12) follow (30) 800 m".
- Ferries on the strip: every ferry keeps its badge where it fits; a ferry closed at the rider's arrival gives way only to the end, gets a red ring and a red arrival; an open one a green arrival. Tapping a ferry on the strip opens a popover with its place, name, arrival line (or status for now once passed), times, fee and website. The ferry list below the panel is no longer shown while navigating; it stays while planning.
- (After the first iPhone check, the maintainer replaced a table of next knot, next stop and end below the strip with these labels and the header distance.)
- The Android notification and the iOS Live Activity add the distance to the next knot and the arrival at the end, e.g. "800 m to 12 · end around 19:05", updated when a knot is passed, without extra location fixes.
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
- `knot-navigation`: new requirements "Progress strip", "Distances and arrivals on the strip" and "Rounded times and distances"; "Number to follow" shows badges and the distance to the next knot; new "Ferries on the strip" and "Ferry popover"; "Ferry arrival estimate" and "Ferry warning" show the arrival and the warning on the strip and in the popover instead of the list; "Navigation with the screen off" adds the distance to the next knot and the arrival at the end
- `map-planning`: "Show the route" adds the estimated time to the distance; "Ferries on the route" shows the list only while planning; "Ferry open now" also applies to the popover of a passed ferry

## Impact

- `modules/core`: a route progress model (positions along the route, pace, the texts) used by `Navigation`, `FerryWatch` and `MapFeatures.describe`; `Navigation.start` learns which knots are stops
- `apps/android` `PlannerScreen.kt`, `NavigationService.kt` and `apps/ios` `PlannerView.swift`, `Navigator.swift`, `Shared/NavigationActivity.swift` and the widget: the strip, the table, the notification and Live Activity line, the planning time
- No change to the graph format or the location sampling
