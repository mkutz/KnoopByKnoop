# Tasks

## 1. Core

- [x] 1.1 `Navigation.update`: back on route within `ON_COURSE_METERS = 30.0` of the course, the wrong-knot condition unchanged. Tests:
  - `NavigationTest` "Back on route": a fix 20 m from the course ends off route with the back-on-route notice; the existing back-on-route tests move their fixes within 30 m.
  - "Parallel path": fixes 60 m beside the course keep off route without a notice.
  - Passing a knot of the route still ends it.
- [x] 1.2 `RouteProgress.nearestAhead` and `Navigation.wayToRoute(fix)`. Tests in `NavigationTest`:
  - "Line to the route": off route 150 m west of a straight course, the point is on the course about 150 m away, not the next knot.
  - "Route behind the rider": the nearest point lies before `place`, so the point is the one at `place`.
  - "Line to the knot": without courses, and before the first knot, the point is the next knot.
  - On route, `wayToRoute` is `null`.

## 2. Android

- [x] 2.1 `NetworkMap` takes `wayToRoute` from `PlannerScreen` and draws the line to it and fits the camera to the rider and it; the ring stays on `nextKnot`. On the emulator with the installed courses, a mocked ride leaving a leg sideways:
  - a screenshot shows the line to the course, not to the next knot ("Line to the route", "Both in view");
  - riding back along a path 60 m beside the course stays off route ("Parallel path");
  - coming within 30 m says back on route and the line is gone ("Back on route").

## 3. iOS

- [x] 3.1 `NetworkMapView` the same way, from `PlannerView`. The project builds; a simulator run off route, with navigation started after the courses are loaded, shows the line to the course in a screenshot. The ride goes to the maintainer's steps in `manual-checks.md`.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The maintainer checks it on a ride, on the Pixel and the iPhone, before `/opsx:archive improve-off-route`.
