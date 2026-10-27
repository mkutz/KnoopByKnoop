# Proposal

## Why

The route summary in the sheet shows every knot of the route as a badge, including the knots it only passes. On a long route these badges fill more than the sheet, and the sheet does not scroll. The waypoint list below the badges, and on iOS even the Clear and Back to start buttons next to them, can no longer be reached. The ferry list of `show-ferry-schedules` makes the summary taller still.

## What Changes

- The top row of the sheet, with the distance and the Clear and Back to start buttons, stays in place.
- Everything below it scrolls together: the knot badges, the ferries of the route and the waypoint list. Moving and removing waypoints keeps working inside the scrolling content.
- This applies to the sheet in portrait and to the side panel in landscape, on Android and iOS.

Out of scope: shortening the knot sequence (e.g. collapsing passed knots), a separate screen for long routes, changes to the info panel.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: the planner panel stays usable for routes of any length

## Impact

- `apps/android`: `WaypointSheet` in `PlannerScreen.kt`: the header row stays fixed; the badges, ferries and waypoints go into one scrolling column instead of a fixed-height waypoint list
- `apps/ios`: `WaypointSheet` in `PlannerView.swift`: the badges and ferries move into the `List` of waypoints, as its first section, below a fixed header with the distance and the buttons
- No changes to `modules/core` or `pipeline`
