# Tasks

## 1. Tricky stretches on the route

- [x] 1.1 Core: find tricky stretches on each leg's course once per route, measuring turns between the 10 m before and after a point and grouping turns of 60° or more within 100 m. Tests: "Zigzag", "One sharp turn", "Gentle bends", "Without courses", and a course with many short OSM vertices that has no tricky stretch.
- [x] 1.2 Measure how often tricky stretches occur per 10 km on the eight public trips of `add-offline-paths`, cycling and hiking, with the real course data, and report it. **The maintainer decides whether to change 60° and 100 m before any app work.**

## 2. What the map shows

- [x] 2.1 Core: `MapFocus` for a fix, with its kind, the points to show and the limits in meters, in the order tricky spot, knot, after knot, base. Tests: "Riding between knots", "Tricky spot at a knot", "Off route".
- [x] 2.2 Core: the knot focus. Tests: "Approaching a knot" (rider, knot and 100 m after it), "Coming closer" (the focus stays the same kind and its points stay within the radius set at the start), "Knot at the end of the route".
- [x] 2.3 Core: the focus after passing a knot, timed by fix times. Tests: "Next knot 800 m ahead", "Next knot far ahead" (capped at 3 km), "Next knot close", "Tricky spot right after the knot", "Last knot passed".
- [x] 2.4 Core: the tricky spot focus on the route. Tests: "Approaching a zigzag", "Past the spot".

## 3. Android

- [x] 3.1 `NetworkMap`: turn a `MapFocus` into a zoom (circle around the rider, within the limits), set it in one 1 s ease when the kind changes, and pan only while it stays the same. Recenter uses the current focus. Keep `following = false` on hand moves.
- [x] 3.2 A mocked ride on the emulator through a knot and a zigzag, with screenshots before, at and after each, and a screen recording for "Smooth zoom changes" and "Labels while zoomed in". Covers "Approaching a knot", "Next knot 800 m ahead", "Approaching a zigzag", "Past the spot".
- [ ] 3.3 On the emulator: "Looking around", "No automatic zoom after looking around", "Recentering near a knot", "Off route", "Both in view".

## 4. iOS

- [x] 4.1 `NetworkMapView`: the same as 3.1.
- [ ] 4.2 A throwaway XCUITest ride in the simulator through a knot and a zigzag, with screenshots, for the same scenarios as 3.2 and 3.3.

## 5. Doubtful junctions (after `add-junction-pictures`)

- [x] 5.1 Core: doubtful junctions on the guided way count as tricky spots, and off route they win over keeping the end of the way in view. Tests: "Doubtful junction off route", "Doubtful junction on the way back".
- [ ] 5.2 Check both apps with a mocked ride off route past a doubtful junction.

## 6. Wrap-up

- [x] 6.1 `manual-checks.md` with the scenarios and the maintainer's steps, including a real ride on each phone that notes whether the zoom helped at knots and tricky spots, whether 10 s after a knot is right, and whether the map moved too much.
- [ ] 6.2 Battery: check with the maintainer whether "Battery use while navigating" needs a new ride. The screen-off path doesn't change, so it shouldn't.
- [x] 6.3 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- Groups 1 to 4 can start on their own. Group 5 waits for `add-junction-pictures`.
- Start after `add-route-points`, which is changing `NetworkMap` and the navigation core right now.
