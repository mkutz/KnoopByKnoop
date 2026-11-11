# Tasks

## 1. Test data

- [ ] 1.1 Keep the cut-out `real-areas/lohmar-siegburg` (box 50.765, 7.145, 50.86, 7.26, published data of 2026-11-11). Add it to `real-areas/NOTICE` with its date.
  - The area check in `RealRideTest` reads each area's date from `NOTICE`.
  - Replace the temporary `scratch Dreesch to 06` test with a named one: `Lohmar-Siegburg to the start from Dreesch`, for `dreesch-to-06`.
  - The ride's current recording shows the bug; it is recorded again in 2.3.

## 2. Core: navigation starts at the first waypoint

- [ ] 2.1 `Navigation.start` begins only at the first waypoint. At it, or at consecutive knots within 30 m at the start, it begins after the last of them. Everywhere else it heads for the start, also without courses. Tests in `NavigationTest`:
  - "Start at the first knot"
  - "Start at a double knot" (route 24, 25, 45)
  - "Start at another node of a merged knot"
  - "Start away from the route"
  - "Start away from the route without courses"
  - "Start at a knot in the middle of the route" and "Start between two knots", each on a plan cut by `startOnRouteAt` (from 3.2)
- [ ] 2.2 Remove `Joining` and its constants. No skipping ahead while `nextIndex == 0`, and no wrong-way watch before the start is passed. The first leg's course counts as the way to the start within 200 m of it. Tests:
  - "Joining the route on the way to the start": the course of 45 → 12 against the route
  - "Following a course on the way to the start"
  - "Passing a later knot on the way to the start"
  - "On the course close to the first knot"
  - "Joining further along": route point start
  - "Shortcut" still passes.
- [ ] 2.3 "To the start" instead of "Off route" while heading for the start, and the pass notice instead of "Back on route" on reaching it:
  - New texts `toStart` / `toStartPoint` in EN, DE, NL and FR, for the notice and the header.
  - Tests:
    - "Start away from the route"
    - "Reaching the start"
    - "Distance on the way to the start"
    - "Starting away from the point"
    - "All languages" (route points), extended by "To the start"
  - Record `dreesch-to-06` again. 06 stays next all the way, without "Back on route", "Wrong direction" or "Turn back now", and the ride ends at 06 with "Next …".
- [ ] 2.4 Run the recorded rides and review every changed text. Rides that relied on starting mid-route or joining before the first knot are re-scripted to start at their first knot, or ride a cut plan. `NavigationReplayTest` and `RealRideTest` pass.

## 3. Core: the start choice

- [ ] 3.1 `StartChoice.of(session, geometry, position)`. It returns the nearby start (a place on the course or a route knot) and both straight distances, or `null`. Tests in a new `StartChoiceTest`:
  - "Asked near the course"
  - "Nearby at a knot"
  - "On the course past the start"
  - "At the start"
  - "Close to the start along the course"
  - "Course not much closer"
  - "Only before the first stop"
  - "Without courses"
- [ ] 3.2 `PlanningSession.startOnRouteAt(start)`: the nearby start, then the waypoints from the first stop on. Tests in `PlanningSessionTest`:
  - "Starting nearby": the route point on the course of 06 → 02; 06 gone; 02 and 28 kept; the route ahead unchanged
  - "Riding to the start": the plan unchanged when not cut
- [ ] 3.3 A second ride `dreesch-nearby` in `RealRideTest` on `lohmar-siegburg`. The plan is cut by `startOnRouteAt` at the Dreesch position, ridden to the point and on along the course past the next knot. Review the recording: "To the start" at the beginning, then "Next …" at the point, then on route.

## 4. Android

- [ ] 4.1 `AppViewModel.startNavigation`:
  - After the permission, take one current position, up to 10 s, as "Show my position" does, and call `StartChoice.of`.
  - With a choice, show a dialog: "Ride to 06 · 4.4 km" and "Start on the route nearby · 250 m".
    - "Start on the route nearby" calls `startOnRouteAt`, saves the plan and starts navigating.
    - "Ride to …" starts navigating at once.
    - Dismissing starts nothing and stops using the location.
  - Without a position, start without asking.
  - Strings in EN, DE, NL and FR.
- [ ] 4.2 Check on the emulator with route 06 → 02 → 28. Started at the Dreesch position (50.80134, 7.21105):
  - The dialog shows.
  - "Ride to 06", replaying `dreesch-to-06`, keeps 06 next all the way.
  - "Start on the route nearby" shows the cut plan and leads to the point. After Stop, the planner keeps the new start.
  - Dismissing starts nothing.

  Screenshots go in the change folder. List in `openspec/checked-by-hand.md`:
  - "Asked near the course"
  - "Riding to the start"
  - "Starting nearby"
  - "Question dismissed"
  - "Position unknown"
  - "The plan after the ride"

## 5. iOS

- [ ] 5.1 `AppModel.startNavigation` does the same as 4.1 with a `confirmationDialog`. Strings in the String Catalog in all four languages. The project builds.
- [ ] 5.2 Check in the simulator with the simulated location at Dreesch: the dialog, both choices and the cut plan in the planner. The ride on the iPhone is the maintainer's.

## 6. Wrap-up

- [ ] 6.1 A changelog line under Unreleased: "Started away from the route's start, Navigate asks whether to ride to the start or to start on the route nearby, and no longer joins the route on the way to the start by itself".
- [ ] 6.2 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. Then run `./gradlew checkScenarios -Pchange=choose-start-or-nearby`.
