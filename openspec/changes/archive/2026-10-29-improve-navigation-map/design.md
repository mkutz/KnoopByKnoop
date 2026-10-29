# Design

## Context

See `proposal.md` for why. Navigation (`add-knot-navigation`) runs in the core's `Navigation`, which takes `Fix(latitude, longitude, accuracyMeters, timeMillis)`. The platforms feed it from `NavigationService` (Android, fused location) and `Navigator` (iOS, `CLLocationManager`). Both publish a `NavigationState(navigation, position)` that the map reads.
- **The map today:** the map holders (`MapHolder` in `NetworkMap.kt`, `Coordinator` in `NetworkMapView.swift`) draw the position as a circle. They move the camera to the rider only when navigation begins or the app is opened (`findRider`); off route they fit the rider and the next knot. Otherwise the camera never follows.
- **Sampling:** the precision (`COARSE`, `FINE`, `LIVE` while visible) decides how often fixes come. This change must not alter it.

## Goals / Non-Goals

**Goals:**
- Direction and track from the existing fixes, decided in the core and tested there.
- Following and recentering that behave the same on both platforms.

**Non-Goals:**
- Heading-up rotation, the compass, storing tracks.
- Smoothing or map-matching the track to the network.

## Decisions

### The direction from the most recent movement, decided in the core
`RiddenTrack.directionAt(fix)` gives the bearing from the last track point at least 10 m behind the newest one, and no more than 30 s older, to the newest one. It returns `null` when the newest point is more than 10 s older than the current fix, because the rider hasn't moved 5 m since ("Standing still"). Unit tests cover riding east, a curve (only the last 10 m count), standing still, and a track too short for a direction.
*Alternatives:*
- The course the platforms report (Android `Location.bearing`, iOS `CLLocation.course`). This was the first plan. The maintainer's experience is that the phone's direction is unreliable, its quality differs between phones, and the emulator always reports 0°.
- The compass. It shows where the phone points, not where the rider goes.

Movement works well where the arrow is seen: with the app open, the precision is `LIVE` and fixes come about every second. With the screen off they are sparse, but nobody sees the arrow then.

### `RiddenTrack` in the core, kept by the navigation's owner
An immutable `RiddenTrack(points)` with `add(fix)`:
- drops fixes less accurate than 50 m
- drops fixes closer than 5 m to the last point, so standing at a ferry doesn't pile up points
- otherwise appends the fix

The service (Android) and the `Navigator` (iOS) keep it next to the `Navigation` and put it into `NavigationState`. They own navigation with the screen off too, so the track includes those stretches ("Screen off"). It is dropped with the state when navigation ends. Unit tests cover accuracy, distance and order.
*Alternative:* keeping it in the map. The map isn't there with the screen off, so the track would have gaps.

### Following as a state of the map holder
The holder gets `following`, set when navigation begins.
- **While following:** each new position moves the camera to the rider and keeps the zoom. Off route, it fits the rider and the next knot, as today.
- **A gesture turns following off:** Android's `OnCameraMoveStartedListener` with `REASON_API_GESTURE`; iOS's `mapView(_:regionWillChangeWith:animated:)` with a gesture reason. The app's own camera moves don't count.
- **The recenter button** sets `following` and animates to the rider at zoom 15, the existing navigation zoom.

The button is a floating action button over the map, at the end above the attribution, shown only while navigating and not following. It uses the icon "my_location" (Material Symbols) / `location.fill` (SF Symbols) with the label "Recenter" for screen readers, in the action colors from `clearer-planner-actions`.
*Alternative:* MapLibre's own tracking modes. They need its location component, which runs its own location sampling; that would break the battery rule and duplicate the navigation's fixes.

### Arrow and track on the map
- **The position:** a symbol layer instead of the circle layer, with an arrow image when the fix has a direction and the existing dot otherwise. The arrow is rotated by `icon-rotate` from the direction, with rotation alignment `map`, so it stays correct when the map is panned.
- **The track:** a line layer from its own GeoJSON source, blue (`#1E6FD9`, 4 dp, 80 % opacity), below the planned route and the knots. Where both overlap, the route stays visible; detours show in blue. The color differs from the orange route, the green network and the red off-route text, in light and dark mode.

## Risks / Trade-offs

- [Updating the track source on each fix while visible] → One source update per fix, as the position already does. The track has at most a few thousand points for a day's ride; if that becomes slow, only the last stretch is redrawn.
- [A direction from jittery fixes] → Fixes less accurate than 50 m are not on the track, and the direction spans at least 10 m; standing still gives the dot.
- [Gesture detection differs per platform] → Both are checked manually: drag, pinch and double-tap zoom each stop following.
