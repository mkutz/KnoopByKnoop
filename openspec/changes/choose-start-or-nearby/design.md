# Design

## Context

**How a ride starts today.** `Navigation.start` (in `Navigation.kt`) picks the start from the first fix:
- at a knot of the route, after it;
- on a leg's course within `OFF_COURSE_METERS` (100 m), on that leg;
- otherwise at the first knot, off route.

Without courses, it begins at the nearest route knot.

**What `update` does before the first knot:**
- The `Joining` state, around `Navigation.kt:994`, joins a leg after 50 m within 30 m of it, in either direction.
- The skip-ahead check (`passedRouteIndex`) lets a later route knot take over.

**The notices.** The way to the first knot is handled as off route, with the guided way and its notices. `Texts.offRoute` / `offRoutePoint` give the words, and reaching the course or knot gives `backOnRoute`.

**What planning already has.**
- A route point on a course: `NetworkGeometry.nearestOnCourses` gives a `CoursePlace`, and `routePointKnot` makes the point.
- `PlanningSession.startAt` puts a point before the waypoints.
- The stops are the waypoints between the first and the last; `RouteDescription.stopPositions` gives their positions in the route's knots.

**When the apps start navigating.** Both apps start at once on Navigate:
- Android: `AppViewModel.startNavigation` → `NavigationService`.
- iOS: `AppModel.startNavigation` → `Navigator.start`.

The position only arrives with the service's first fix.

## Goals / Non-Goals

**Goals:**
- The choice between the whole route and the route nearby is made by the rider, once, before navigation starts.
- Navigation itself only knows one way to start: at the first waypoint.
- One core function decides whether to ask and what "nearby" is, used by both apps.

**Non-Goals:**
- Asking again mid-ride, or offering to join after "Ride to 06".
- Keeping the original plan next to the cut one.
- Changing the guided way's search.

## Decisions

1. **`StartChoice.of(session, geometry, position)` in the core.** It returns `null` (don't ask) or the nearby start with both straight distances.
   - **Candidates:** the route's knots from position 0 to the first stop position, or to the end without stops. With courses, it takes the nearest place on their legs' courses, cut from the route's geometry so only these legs count.
   - **Ask** when the rider is more than 30 m from the start, the place is more than 200 m along the course from the start, and the place is less than half as far as the start. The 200 m reuse the first knot's existing radius. The half keeps the question to cases where riding to the start clearly is a detour.
   - **A place within 30 m of a knot of the route** gives that knot instead of a route point.
   - **Without courses**, the candidates are the knots themselves (the nearest one, excluding the start).

   *Alternative:* deciding in `Navigation.start` on the first fix. Rejected: the plan can't change once navigation runs ("Route is fixed while navigating"), and the dialog belongs before it.

2. **`PlanningSession.startOnRouteAt(start)` cuts the plan.**
   - The new waypoints are the nearby start, then the waypoints from the first stop on (or just the end).
   - A route point is placed like "Start here" on a course does (`routePointKnot` with an empty way), then the route is planned anew.
   - From a place on the planned course, the shortest way to the first stop is the rest of that course (a tail of a shortest path is a shortest path), so the route ahead doesn't change; ties could pick an equal-length alternative, which is acceptable.
   - The apps save the plan as after any edit.

3. **The apps get one position first.**
   - On Navigate, after the location permission is granted, the app asks for a single current position with up to 10 seconds, the same way "Show my position" does.
   - It calls `StartChoice.of`. With a choice it shows a dialog: Android an `AlertDialog`, iOS a `confirmationDialog`.
   - "Start on the route nearby" calls `startOnRouteAt`, saves, then starts navigating. "Ride to …" starts at once. Dismissing does neither.
   - Without a position in time, it starts navigating without asking.
   - The fix is not handed to the service: the service gets its own fixes as today.

4. **`Navigation` starts only at the first waypoint.**
   - `start`: at the first waypoint, or among the consecutive knots within 30 m at the start, it begins after the last of them. Otherwise `nextIndex = 0` and the state is "to the start". The leg-on-course start and the nearest-knot start without courses go.
   - The `Joining` state and its constants are removed. While `nextIndex == 0`, the skip-ahead check is skipped.
   - The last 200 m keep counting the first leg's course as the way to the start, as "Distance to the first knot" does today.
   - "To the start" is the off-route state with `nextIndex == 0`, so the guided way, its search and the guidance notices stay as they are. Only the words change: new `Texts.toStart` / `toStartPoint` replace `offRoute` / `offRoutePoint` there, in EN/DE/NL/FR, for both the notice and the header.
   - Reaching the start gives the pass notice of the first knot instead of `backOnRoute`.
   - Wrong-way watching starts only once the first waypoint is passed.

5. **Tests.**
   - `NavigationTest`: the start scenarios.
   - `StartChoiceTest` (new): asking, not asking, the knot case, only before the first stop, without courses.
   - `PlanningSessionTest`: `startOnRouteAt`.
   - `RealRideTest` gets two rides on `real-areas/lohmar-siegburg`:
     - `dreesch-to-06`, already scripted: B, with 06 next all the way.
     - `dreesch-nearby`: the plan cut by `startOnRouteAt` and ridden from Dreesch to the point and on to 02.
   - The area check reads each area's data date from `NOTICE` instead of one fixed date.

## Risks / Trade-offs

- **The rider loses the plan's original start.** The maintainer's choice; the question names what it does ("Start on the route nearby"). Re-planning 06 is a tap on the map.
- **The question delays the start by up to 10 s** without a position. Mitigation: it uses the same single position as "Show my position", which is usually there in 1–2 s, and starts without asking when it's not.
- **Recorded rides change.** `NavigationReplayTest` and `RealRideTest` rides that start mid-route or join before the first knot will record differently. Each changed text is reviewed. Rides meant to start mid-route are re-scripted to start at their first knot, or use a cut plan.
- **Thresholds (half the distance, 200 m)** are guesses. The emulator check and the first rides will tell; they are constants in one place.
