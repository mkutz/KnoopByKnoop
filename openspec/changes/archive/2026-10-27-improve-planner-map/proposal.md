# Proposal

## Why

The manual checks of the Android and iOS planners showed that the map and the sheet are hard to read: there is no obvious way to zoom out, the route disappears behind knot badges in dense areas, start and end look like any other stop, the riding direction is not visible, the sheet shows the route as a long text of numbers and arrows in which the chosen waypoints do not stand out, a quick second tap on the start silently creates a round trip, and zoomed out the waypoint markers cover the map. Not being able to zoom out also made it hard to notice which region was shown.

## What Changes

- Zoom buttons on the map, in addition to the pinch and double-tap gestures
- Knots along the route are highlighted, so the route stays visible where knots are dense
- Start and end get markers that differ from stops and from each other
- Arrows along the route line show the riding direction
- The sheet shows the route's knots as badges like on the map, with the chosen waypoints clearly highlighted, instead of numbers joined by arrows
- The waypoint list shows its knots as badges too
- Tapping a knot that already is a waypoint does nothing, so an accidental double tap no longer creates a round trip and the start no longer turns into a stop; a "Back to start" action creates round trips on purpose
- Zoomed out, the route is only a line, without markers; tapping it zooms to the route

Out of scope: showing all installed regions on one map and planning across borders (comes with `seamless-regions`), turn-by-turn directions, elevation.

This change modifies `map-planning`, which `add-planning-screens` introduces: that change must be archived first.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: tapping the last waypoint again, returning to the start, zooming, how the route and its waypoints are shown on the map and in the sheet

## Impact

- `modules/core`: the route's knots with their role (start, stop, end, passed) for the map and the sheet, the route's direction in the map data
- `apps/android` and `apps/ios`: zoom buttons, map layers and styles, the badge row and waypoint list in the sheet
- No change to data, downloads or routing
