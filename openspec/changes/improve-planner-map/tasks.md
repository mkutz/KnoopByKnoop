# Tasks

## 0. Prerequisite

- [x] 0.1 Archive `add-planning-screens`, which introduces `map-planning`

## 1. Core

- [x] 1.1 Route knots with their role (`start`, `stop`, `end`, `passed`) in `MapFeatures`, as GeoJSON and as a list for the sheet; `RouteDescription.Complete` carries the list instead of the arrow-joined string
- [x] 1.2 `PlanningSession`: ignore a tap on the last waypoint, `returnToStart()`
- [x] 1.6 `PlanningSession`: ignore a tap on any waypoint, with a test for "Tapping the start of a route again"
- [x] 1.4 Map markers of the route merge the start and end of a round trip into one marker with the role `ROUND_TRIP`, with a test for "Round trip on the map"
- [x] 1.5 Split badge for `ROUND_TRIP` on Android and iOS
- [x] 1.3 Tests: "Tapping the last waypoint again", "Back to start", "Route summary", "Passed knots", "Start, stop and end", "Listing waypoints", roles of a round trip and of an incomplete route

## 2. Android

- [x] 2.1 Zoom buttons ("Zooming out")
- [x] 2.2 Badge images per role, route knot layer above the network, no more waypoint circles
- [x] 2.3 Direction arrows along the route ("Direction arrows")
- [x] 2.4 Sheet: badge row in the summary, badges in the waypoint list, "Back to start"
- [x] 2.5 Zoomed out: route line only, tapping it zooms to the route
- [x] 2.6 Waypoint list: while dragging, the other waypoints make room and the dragged one is drawn on top

## 3. iOS

- [x] 3.1 Zoom buttons ("Zooming out")
- [x] 3.2 Badge images per role, route knot layer above the network, no more waypoint circles
- [x] 3.3 Direction arrows along the route ("Direction arrows")
- [x] 3.4 Sheet: badge row in the summary, badges in the waypoint list, "Back to start"
- [x] 3.5 Zoomed out: route line only, tapping it zooms to the route
- [x] 3.6 Waypoint list: stable row identities and the new order shown at once on drop; planning updates computed off the main thread

## 4. Verification

- [x] 4.1 Screenshots of a route with start, stop, end and passed knots on both platforms, in a dense area, added to `manual-checks.md` in this change
- [ ] 4.2 `./gradlew check` and the iOS app build pass locally and in CI
