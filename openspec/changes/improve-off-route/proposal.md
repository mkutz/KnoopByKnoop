# Proposal

## Why

The maintainer's notes on the test ride of 2026-11-01 (Bonn towards Troisdorf, Pixel 7), both while off route on purpose:

- **12:42:** the dashed line led straight to the next knot, but the way back to the route is to the route, which can be much closer.
- **17:57:** the app said "Back on route" while the rider was still on a parallel path beside the route. Leaving and coming back use the same 100 m from the course, so any path within 100 m of the route counts as being on it.

## What Changes

- **Line to the route:** off route, the dashed line on the map goes from the rider to the nearest point of the route ahead of them: on the course of the current leg, from where they left the route to the next knot. When following, the map keeps the rider and that point in view.
  - Without installed courses, or before the first knot, there is no course to aim at, so the line still goes to the next knot.
- **Back on route:** the rider is back on route only within 30 m of the course, the distance that counts as passing a knot. Leaving the route stays at more than 100 m, and passing a knot of the route still brings the rider back on route.
- The header, the spoken notices and the lock screen keep naming the next knot with the straight distance and direction to it, as now.

## Out of Scope

- **Guidance to the route:** the header arrow and distance, the notices and the lock screen pointing to the nearest route point instead of the next knot. The maintainer chose the line only.
- **Parallel paths closer than 30 m:** GPS cannot tell them from the route reliably, so they still count as on the route.
- **Leaving the route:** when the rider counts as off route is unchanged.
- **The other ride notes:** out-and-back legs with a fork, which number is most prominent, arrivals at the current speed, the lock-screen notification and the language switch during navigation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`:
  - "Off route" sets back on route at 30 m from the course.
  - "Way to the route on the map" draws the line to the nearest point of the route ahead and keeps that point in view.

## Impact

- **Core:**
  - `Navigation.kt` gets the 30 m back-on-route distance and the point to head for while off route.
  - `RouteProgress.kt` or the geometry helpers get the nearest point on a course after a place.
  - Tests are in `NavigationTest`.
- **Android:** `NetworkMap.kt` (the line and the camera bounds) and the call in `PlannerScreen.kt`.
- **iOS:** `NetworkMapView.swift` (the line and the camera bounds) and the call in `PlannerView.swift`.
