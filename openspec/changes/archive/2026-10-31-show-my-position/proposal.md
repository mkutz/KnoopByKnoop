# Proposal

## Why

Two Android testers asked for the map to show where they are: "wäre geil, wenn man direkt zu Beginn nach GPS gefragt wird und die Karte an die aktuelle Position geht. Bzw. wenn es einen 'hier' Knopf gibt". Today the app asks for location access only when navigation starts, so while planning, finding one's own surroundings means searching or panning.

## What Changes

- A round "my position" button below the zoom buttons centers the map on the rider's current position, once per tap, at a zoom where the knots are shown. The first tap asks for location access "while using the app".
- On the very first start, when there is no saved map position and no route, the app asks for location access right away and, if allowed, shows the rider's position.
- The found position is marked on the map as a dot, which does not follow the rider; it stays until the next tap, navigation, or leaving the app.
- Location is used once per request: a single fix, no updates while planning. The system's location indicator shows briefly while the position is found.
- Without access, or when no position is found in time, the app says so instead of moving the map.

Out of scope:
- Showing and following the rider's position continuously while planning.
- Planning from the current position, e.g. "start at the nearest knot".
- The dark map from the same round of feedback (its own change).

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: new requirement "Show my position"
- `knot-navigation`: "Arriving and stopping" allows the location also once on request while planning

## Impact

- `apps/android` `NetworkMap.kt`, `PlannerScreen.kt`, `AppViewModel.kt`: the button, a single fix with `FusedLocationProviderClient.getCurrentLocation`, the permission request, the first-start check
- `apps/ios` `NetworkMapView.swift`, `PlannerView.swift`, `AppModel.swift`: the button, `CLLocationManager.requestLocation`, the first-start check, the usage text in `project.yml`
- No change to the core or to navigation
