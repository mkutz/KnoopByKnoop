# Tasks

## 0. Prerequisite

- [ ] 0.1 Archive `add-planning-screens`, which introduces `map-planning`

## 1. Core

- [ ] 1.1 Route knots with their role (`start`, `stop`, `end`, `passed`) in `MapFeatures`, as GeoJSON and as a list for the sheet; `RouteDescription.Complete` carries the list instead of the arrow-joined string
- [ ] 1.2 `PlanningSession`: ignore a tap on the last waypoint, `returnToStart()`
- [ ] 1.3 Tests: "Tapping the last waypoint again", "Back to start", "Route summary", "Passed knots", "Start, stop and end", "Listing waypoints", roles of a round trip and of an incomplete route

## 2. Android

- [ ] 2.1 Zoom buttons ("Zooming out")
- [ ] 2.2 Badge images per role, route knot layer above the network, no more waypoint circles
- [ ] 2.3 Direction arrows along the route ("Direction arrows")
- [ ] 2.4 Sheet: badge row in the summary, badges in the waypoint list, "Back to start"
- [ ] 2.5 Zoomed out: route line only, tapping it zooms to the route

## 3. iOS

- [ ] 3.1 Zoom buttons ("Zooming out")
- [ ] 3.2 Badge images per role, route knot layer above the network, no more waypoint circles
- [ ] 3.3 Direction arrows along the route ("Direction arrows")
- [ ] 3.4 Sheet: badge row in the summary, badges in the waypoint list, "Back to start"
- [ ] 3.5 Zoomed out: route line only, tapping it zooms to the route

## 4. Verification

- [ ] 4.1 Screenshots of a route with start, stop, end and passed knots on both platforms, in a dense area, added to `manual-checks.md` in this change
- [ ] 4.2 `./gradlew check` and the iOS app build pass locally and in CI
