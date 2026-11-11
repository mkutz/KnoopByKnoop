# Design

## Context

Both apps load a network in two steps: the graph with straight lines first (`hasGeometry = false`), then the courses (`addCourses` in `AppViewModel.kt`, the second `loaded =` in `AppModel.swift`). `startNavigation` passes `geometry` only when `hasGeometry` is true, so a navigation started in between gets `NavigationRoute.geometry == null`. The core builds everything course-related once, in `Navigation.start`, into the `RideSetup`:

- `courses`
- `signpostCourses`
- `drawnCourses`
- `headedFor`
- the `RouteProgress` and its offsets
- `legTurns`
- `tricky`

Nothing replaces the `RideSetup` later. `RideState` holds the ride itself:

- `nextIndex`
- `offRoute`
- the reminders
- `passedKnot`
- the precise-fix confirmation

`Pace` and `FerryWatch` work on places along the `RouteProgress`, so their meters change with the courses.

## Goals / Non-Goals

**Goals:**
- One core call that gives a running navigation its courses, used by both apps.
- After it, the navigation behaves exactly as one started with courses at the rider's place, apart from what the ride already did.

**Non-Goals:**
- Taking courses away again, or swapping one set of courses for another (a data update mid-ride keeps the running navigation as it is).
- Changing how fast courses load.

## Decisions

1. **`Navigation.withCourses(geometry, fix)` builds a new `RideSetup` and keeps the `RideState`.**
   - The course-related parts of `Navigation.start` move into one private function, which both `start` and `withCourses` use, so the two can't drift apart.
   - `nextIndex` and the passed knots stay. If the rider's leg on the course is already beyond `nextIndex`, `nextIndex` moves up to it without notices, as the rider is past those knots.
   - `offRoute` stays false; the next `update` decides it with the courses, which gives the usual off-route notice ("Off route when the courses arrive").

   *Alternative:* restarting with `Navigation.start` at the current fix. That would announce the next knot again, start the pace and ferry watch from scratch, and pick the nearest knot rules of a start, so passed knots could come back.

2. **Pace and ferries move to the new meters.**
   - The pace keeps its speed and takes the rider's place on the new `RouteProgress`.
   - The ferry watch is re-based on the new offsets and keeps which ferries it has already warned about, so no warning repeats. Its arrival times follow on the next fix.

3. **`NavigationSession.withCourses(geometry)` passes them on and returns a `SessionStep`.**
   - Before the first fix, it only replaces the route's geometry, so `Navigation.start` gets them.
   - After it, it calls `withCourses` with the last fix and returns the new state, without notices.
   - Calls after the end, or a second time, do nothing.

4. **The apps hand the courses over where they finish loading.**
   - Android: when `addCourses` finishes while navigating, `AppViewModel` puts the geometry, with the route's points, into `Navigating`. `NavigationService` collects it and calls `session.withCourses`, then acts on the step as for any other.
   - iOS: when the courses finish loading while navigating, `AppModel` calls a new `Navigator.takeCourses`, which does the same on the main actor.
   - Both use the route graph the navigation was started with (`withRoutePoints(routeGraph)`), as `startNavigation` does.

## Risks / Trade-offs

- **Place jumps:** distances along courses are longer than straight lines, so the distance left and the progress bar jump once when the courses arrive. That is the correct value replacing a wrong one; nothing is spoken for it.
- **Ferry watch re-basing:** the most delicate part. A unit test with a ferry ahead covers that no warning repeats and the arrival stays sensible.
