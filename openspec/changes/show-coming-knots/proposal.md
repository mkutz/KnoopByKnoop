# Proposal

## Why

While navigating, the panel shows the number to follow and the progress strip, and can't be dragged higher, because there was nothing more to show. Riders sometimes want to look ahead: which knots come, how far away they are, and when they will get there. Google Maps shows the next turns when its panel is swiped up, so riders are likely to try the same. For a knot network, the coming knots are the turns.

## What Changes

- While navigating, the panel can be dragged up again, in portrait. Above the number to follow and the strip, it shows the coming knots of the route in riding order: each knot's badge, as on the map, with the distance to it along the route and the arrival, the ferries between them, and the stops and the end marked by their badges. Passed knots are not shown; the next knot comes first.
- In landscape, the side panel shows the same list below the strip, as it has the room.
- Tapping a knot in the list shows it on the map. The map stops following the rider, as when the rider moves the map, and the recenter button brings it back.
- Starting navigation still brings the panel to the height of the number to follow and the strip; the list is below, out of the way until the rider drags the panel up.

## Out of Scope

- Changing the route from the list (skipping ahead happens by riding, see "Skipping ahead").
- Turn-by-turn directions between knots.
- The list outside the app (notification, Live Activity).
- Multi-day stages.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Panel height while navigating" lets the panel be dragged up again; a new requirement for the list of coming knots.

## Impact

- Core: `ProgressView` gets the coming knots with their distances and arrivals, computed as the strip's labels are.
- Android `PlannerScreen.kt` and iOS `PlannerView.swift`: the list below the strip, the sheet draggable while navigating, and tapping a knot to show it on the map.
- Android `NetworkMap.kt` and iOS `NetworkMapView.swift`: showing a given knot while navigating, which stops following.
