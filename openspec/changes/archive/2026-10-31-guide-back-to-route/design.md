# Design

## Context

See `proposal.md` for why. Today:
- `Navigation.start` (core) begins at a route knot within 30 m, on the course of a leg within 100 m, or else at the route knot nearest to the rider, off route when courses are installed.
- While off route, `Navigation` says "Off route, next 45" once, and says "Back on route" when the rider is within 100 m of the course again or passes a route knot; passing a later route knot counts as skipping ahead. Off route, the apps sample every 3 s (`Precision.FINE`).
- `Navigation.progress(fix)` gives the header's distance to the next knot, straight while off route (`show-route-progress`). `progressLine` is the notification's and Live Activity's second line, renewed at knots; the apps only push it when it changes.
- The apps know the riding direction from the ridden track (`RiddenTrack.directionAt`), `null` when standing.
- Both maps already get the rider's position, the next knot and whether the rider is off route.

## Goals / Non-Goals

**Goals:**
- Straight-line guidance that needs no data the app does not have, and no extra fixes.
- Texts, directions and when to speak decided in the core.

**Non-Goals:**
- Routing on roads or on the knot network.

## Decisions

### Start at the first knot when away from the route
In `Navigation.start`, with courses and neither a route knot within 30 m nor a leg within 100 m, the next knot is the first knot (position 0), off route. The existing passing logic covers joining later: passing any later route knot is skipping ahead. Without courses, the nearest route knot stays, as the app cannot tell whether the rider stands on the route between two knots.
*Alternative:* asking the rider. Decided against by the maintainer for now (out of scope).

### Bearing and compass direction
The initial great-circle bearing from the fix to the next knot, in degrees from north; the compass word is its eighth of the circle (north from 337.5° to 22.5°, …). `Navigation.guidance(fix)` returns the distance text (as in "Rounded times and distances"), the bearing and the word, or `null` when on route.

### Spoken guidance
The off-route notice gets the distance and direction: "Off route, next 45, 1.7 km to the north-east". `Navigation` remembers the distance and bearing of the last notice that told them; with each precise fix while off route, it adds a `NoticeKind.GUIDANCE` "Next 45, 1.2 km to the north" when the distance is 500 m or more below the remembered one, or the bearing differs by more than 45°. Speech uses the same text; "km" is read as kilometers by both speech engines.

### Outside the app
While off route, `progressLine` is "1.7 km north-east to 02", renewed when the distance differs by 100 m or more from the one in the line, and when going off route; back on route, it is the route progress line again. As the apps push the line only when it changes, this needs no other change to when the notification or the Live Activity are updated, and the fixes are the ones navigation already takes.

### The arrow in the header
The apps rotate an arrow by the bearing minus the riding direction, or by the bearing alone when the rider stands (as the north-up map). The header reads "Off route, next (02) 1.7 km ↗ north-east"; for screen readers "Off route, next 02, 1.7 km to the north-east".

### The line on the map
A dashed line layer from the rider's position to the next knot while off route, in the off-route red, below the rider's marker. While the map follows the rider, the camera fits both points with padding instead of centering on the rider; when the user moves the map, following stops as today.

## Risks / Trade-offs

- [The straight line crosses rivers or private land] → It is labeled as direction only; the map shows roads and the network to choose a way.
- [Guidance notices while riding around a block] → Only after 500 m closer or a turn of the direction by more than 45°; riding around does neither.
- [The first knot is far when starting next to a later part of the route] → The rider can ride to the nearer knot; passing it skips ahead, as today.
- [Live Activity updates every 100 m off route] → At cycling speed about every 20 s, within what iOS allows for a visible Live Activity.
