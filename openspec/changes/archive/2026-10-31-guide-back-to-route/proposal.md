# Proposal

## Why

A rider who starts navigating away from the route, or leaves it, is told "Off route, next 02" and sees the distance to that knot, but not where it is. Finding it means reading the map, which is hard on a bike and impossible with the screen off. Starting away from the route also skips everything before the nearest route knot, although riders usually want to ride the whole route from its start.

## What Changes

- Navigation started more than 100 m from the course of the route leads to its first knot instead of the nearest one; joining the route at a later knot on the way still counts, as skipping ahead does today.
- While off route, the app guides the rider to the knot to head for, in a straight line, as it has no road data:
  - the map shows a dashed line from the rider to that knot and, while it follows the rider, keeps both in view
  - the header shows an arrow pointing towards the knot relative to the riding direction, with the distance and the compass direction, e.g. "Off route, next (02) 1.7 km ↗ north-east"
  - the off-route notice says the direction, e.g. "Off route, next 2, 1.7 km to the north-east"; a guidance notice repeats distance and direction when the rider got 500 m closer or the direction changed by more than 45°
  - the notification and the Live Activity show the distance and direction, updated when the distance changed by 100 m or more, without extra location fixes

Out of scope:
- Routing along roads or paths to the route; there is no road data offline.
- Routing along the knot network to the route.
- A choice between going to the start and joining the route nearer.
- Turn-by-turn guidance on the route itself.

## Capabilities

### New Capabilities

### Modified Capabilities
- `knot-navigation`: "Start navigation" leads to the first knot when starting away from the route; "Off route" gives the direction and repeats guidance; new requirements "Way to the route on the map", "Direction to the route" and "Direction outside the app"

## Impact

- `modules/core`: the start knot when away from the route, bearing and compass direction to the next knot, the guidance notices and the outside-app line while off route (`Navigation.kt`)
- `apps/android` `NetworkMap.kt`, `PlannerScreen.kt`, `NavigationService.kt` and `apps/ios` `NetworkMapView.swift`, `PlannerView.swift`, `Navigator.swift`, the Live Activity: the line on the map, the arrow in the header, the notification and Live Activity text
- No change to the location sampling: off route, it already samples every 3 s
