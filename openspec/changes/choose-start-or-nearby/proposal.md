# Proposal

## Why

Started away from the route, navigation leads to the first knot, but it also joins the route on the way when the rider follows a leg's course for 50 m ("Joining the course on the way to the start"). It guesses from the riding whether the rider wants the whole route or just wants to get onto it, and the app's own guided way can make it guess wrong.

A mocked ride on 2026-11-11 showed it: route 06 → 02 → 28, started in Siegburg-Dreesch about 250 m from the course of 06 → 02 and 4.4 km from 06.
- The guided way to 06 runs north on Hauptstraße, the street the course of 06 → 02 takes southwards.
- After 50 m on it the rider heard "Back on route, next 02", then "Wrong direction", then "Off route, next 02, 4.5 km to the south".
- About ten "Turn back now" followed, all the way to 06.

Both wishes are valid: riding the route in full from its start, or getting onto it nearby to reach its end. The maintainer decided (2026-11-11) that the app asks when both are reasonable, instead of guessing.

## What Changes

- **Navigate asks when the course is much closer than the start.** This applies when the rider is not at the start and a place on the course before the first stop is much closer than the start. The question offers "Ride to 06" and "Start on the route nearby", each with its straight distance.
- **"Start on the route nearby" changes the plan for good**, before navigation starts.
  - The start becomes a route point at the nearest place on the course, as "Start here" on a course makes one, or the route's knot there when the place is at a knot.
  - The waypoints before it are removed, and the stops and the end stay.
  - The plan keeps this start after the ride.
- **Navigation always begins at the first waypoint.**
  - Away from it, navigation leads there and nothing on the way joins the route: not the course of a leg, not a later knot of the route.
  - **BREAKING** for the spec: "Joining the course on the way to the start" is removed.
  - Also removed: skipping ahead before the start, and the route point scenario "Joining further along".
- **Starting on the course or at a knot of the route past the start is asked too.** It no longer silently starts there.
- **The way to the start is told as "To the start", not "Off route".** For example: "To the start, 06, 4.4 km to the north".
  - Reaching the start is told as passing a knot, not as "Back on route".
  - The last 200 m keep the first leg's course as the way to the start, as today.
- **Without courses**, the question compares the start with the nearest knot of the route before the first stop, which then becomes the start.
- **The mocked ride becomes a regression ride** (`dreesch-to-06`) on a new cut-out of the real data, `lohmar-siegburg`. A second ride covers "Start on the route nearby".

Out of scope:
- **Joining the route on the way after choosing "Ride to 06".** A rider who changes their mind stops and presses Navigate again, which asks again.
- **Asking later in the ride.** Leaving the route mid-ride keeps today's off-route handling, the guided way back to the course ahead.
- **Keeping the original plan.** The maintainer chose to change the plan for good; there is no undo beyond planning the start again.
- **Places on the course after the first stop.** Starting there would drop a stop, so they are not offered.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`:
  - Modified: "Start navigation", "Skipping ahead", "Number to follow" (wording on the way to the start) and "Navigating with route points".
  - New: "Start on the route nearby".
  - Removed: "Joining the course on the way to the start".

## Impact

- **`modules/core`:**
  - New: a start choice for a session, a geometry and a position (whether to ask, the nearby place and the distances), and a planning step that makes the nearby place the start.
  - `Navigation.start` and `update` no longer join or skip ahead before the first waypoint.
  - `Texts` gets the "To the start" notices in all four languages.
  - Tests: `NavigationTest`, `RealRideTest` (new area and two rides) and a test for the start choice.
- **`apps/android`:** `AppViewModel.startNavigation` gets one position first, asks with a dialog when the core says so, and saves the changed plan. New strings in four languages.
- **`apps/ios`:** `AppModel.startNavigation` does the same, with a confirmation dialog.
- **Test data:** `real-areas/lohmar-siegburg`, cut from the published data of 2026-11-11, so the area check in `RealRideTest` accepts each area's own date.
- No pipeline or data format changes.
