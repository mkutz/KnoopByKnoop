# Design

## Context

See `proposal.md` for why. Today location access is requested by the planner when navigation starts (Android `PlannerScreen.kt` with `RequestMultiplePermissions`, iOS `Navigator` with `requestWhenInUseAuthorization`). The map restores `camera.json` (both apps) or focuses the route; without either, it opens on the default view of the networks. The zoom buttons are round floating buttons on the right edge (Android `SmallFloatingActionButton` in `NetworkMap.kt`, iOS `UIButton`s in `NetworkMapView.swift`); while navigating, a green recenter button appears below them. iOS's usage text says the location is used for navigation only.

## Goals / Non-Goals

**Goals:**
- One position per tap, no continuous use while planning, so the location indicator shows only briefly.
- Ask on the first start only when the map has nothing better to show.

**Non-Goals:**
- A live position while planning.

## Decisions

### A single fix
Android: `FusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken)` with a 15 s timeout. iOS: `CLLocationManager.requestLocation()` on a manager of its own (not the `Navigator`'s), with a 15 s timeout. While waiting, the button shows a small progress indicator. The map then animates to the position at `max(current zoom, the zoom where knots appear)`.
*Alternative:* the last known location. It can be hours old or from another country; one fresh fix takes a few seconds.

### The dot
A "my-position" GeoJSON source with a circle in the rider's blue, drawn below the route knots, set once per fix and cleared when navigation starts or the app goes to the background. It is the same blue as the rider while navigating, but without the arrow and without updates.

### First start
"First start" means `camera.json` does not exist and no route was restored. Then the planner asks for access right after the map appears, and on success centers like the button. A denial is remembered by the system; the app does not ask again on later starts.

### Permission messages
Android: when the permission is denied permanently (`shouldShowRequestPermissionRationale` false after a denial), the message offers "Settings", opening the app's settings. iOS: on `.denied`, the message says to allow it in Settings, with a button opening `UIApplication.openSettingsURLString`. The iOS usage text becomes "Knoop by Knoop uses your position to show where you are on the map and, while navigating, to tell you the number to follow at the next knot."

### The button
Android: a `SmallFloatingActionButton` below the zoom buttons with Material Symbols "my_location", content description "My position", hidden while navigating (the recenter button takes its place). iOS: a `UIButton` with `location`, same place, accessibility label "My position".

## Risks / Trade-offs

- [Asking on the first start feels pushy] → Only when the map has nothing else to show; the system dialog can be declined, and the app works without it.
- [The dot is mistaken for a live position] → It does not update and goes away when the app is left; the next tap renews it.
- [No fix indoors] → After 15 s the app says so; the map stays where it was.
