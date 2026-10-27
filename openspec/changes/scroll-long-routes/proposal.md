# Proposal

## Why

The route summary in the sheet shows every knot of the route as a badge, including the knots it only passes. On a long route these badges fill more than the sheet, and the sheet does not scroll. The waypoint list below the badges, and on iOS even the Clear and Back to start buttons next to them, can no longer be reached. The ferry list of `show-ferry-schedules` makes the summary taller still.

## What Changes

- The top of the sheet stays in place: the distance, the Clear and Back to start buttons, and the knot badges, which become one line that scrolls sideways instead of wrapping into many rows.
- Below it, the ferries of the route and the waypoint list scroll together. Moving and removing waypoints keeps working inside the scrolling content.
- This applies to the sheet in portrait and to the side panel in landscape, on Android and iOS.

Out of scope: collapsing passed knots or the ferry list, a separate screen for long routes, changes to the info panel.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: the planner panel stays usable for routes of any length

## Impact

- `apps/android`: `WaypointSheet` in `PlannerScreen.kt`: a fixed header with the distance, the buttons and a sideways scrolling row of badges; the ferries and waypoints go into one scrolling column instead of a fixed-height waypoint list; removing a waypoint must not crash there (it did on a long, scrolled list, see design)
- `apps/ios`: `WaypointSheet` in `PlannerView.swift`: the badges become a horizontal `ScrollView` in the fixed header; the ferries move into the `List` of waypoints, as its first section
- No changes to `modules/core` or `pipeline`
