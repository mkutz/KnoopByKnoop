# Proposal

## Why

While the map follows the rider, it stays at one zoom: 15, set when navigation starts and on recenter. That is too far out at a knot or a tricky bit of way to see where the signpost is and where the route goes on, and too close right after a knot to see where the next one is. Today the rider has to zoom by hand, which stops following. The map should pick the zoom that fits what is ahead.

## What Changes

- **Zoom in at tricky spots:** when the rider approaches a tricky spot, the map zooms in on it until the rider is past it. A tricky spot is a doubtful junction from `add-junction-pictures` on a guided way, or a stretch of the route where several sharp turns follow closely, e.g. a zigzag within 100 m. The second rule needs only the network's courses, not path data.
- **Zoom in at knots:** from the reminder 200 m before a knot, the map zooms in so the rider, the knot and the first part of the route after it fit in the view. This shows where to expect the signpost and which way the route goes on.
- **Zoom out after a knot:** after passing a knot, the map zooms out for about 10 s, just far enough to show the rider and the next knot together, but not farther out than a set limit. Then it goes back to the base zoom of 15.
- **Order:** a tricky spot wins over the knot rules. The zoom-out after a knot is skipped or cut short when a tricky spot is close ahead.
- **Unchanged:** moving or zooming the map by hand still stops following, and the automatic zoom with it. Recenter brings both back. The map stays north-up. Off route the map still keeps the rider and the end of the way back in view, unless a doubtful junction on the guided way is close ahead.
- Zoom changes are smooth, one even move, and do not make the base map's labels flicker.

## Out of Scope

- **Turning the map** in the riding direction (heading-up). The map stays north-up.
- **Tilting the map** or a 3D view.
- **Changing the zoom with speed.**
- **A setting** to turn the automatic zoom off or choose its levels. Hand zoom and recenter stay as they are; a setting can follow if riders ask for one.
- **Keeping a zoom the rider chose by hand while following.**
- **Route points from `add-route-points`.** They get the base zoom, as they have no signpost. They can be added once that change is done.
- **Hiking-specific levels.** Hiking uses the same rules.
- **The lock screen and notifications.** This only changes the map in the app.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Following the rider" changes from one fixed zoom to a zoom that follows what is ahead. New requirements describe the zoom at tricky spots, at knots, and after passing a knot.

## Impact

- **Depends on:** `add-junction-pictures` for doubtful junctions. The rest of the change doesn't depend on it. Until it is done, only the route's own sharp turns count as tricky spots.
- **Core:** finding tricky stretches on a leg's course once per route. A new "what to show" result from the navigation state: the points to fit and the zoom limits, so both platforms behave the same and it can be tested without a screen.
- **Android:** `NetworkMap` turns that result into the camera instead of the fixed `NAVIGATION_ZOOM` while following.
- **iOS:** `NetworkMapView` does the same.
- **Battery:** the camera still moves only once per position fix, so there's no extra work while the screen is off.
