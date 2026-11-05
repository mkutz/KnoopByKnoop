# Tasks

## 1. Core

- [x] 1.1 `Navigation.headedFor(index)` from `drawnCourses`; `RouteProgress.of` gets the points and `straightTo` measures to them; `distanceTo`, `bearing` and `wayTo` take a `GeoPoint`, and all their call sites for the next knot use `headedFor`, including `wayToRoute`'s fallback, `precision` and the reminder in `update`.
- [x] 1.2 Tests in `NavigationTest`, with a knot with two positions and courses from `NetworkGeometry`: "Approaching the first knot at the signpost the route leaves from", "Dashed line to the first knot", "Off route, heading for a knot on the route", "The route uses the other signpost", "Reminder at the signpost used", "Passing the other signpost", "A knot with one signpost", "Without courses". Also a test that the sampling is fine within 400 m of the signpost used and more than 400 m from the main position.
- [x] 1.3 `NetworkGeometry.firstReachedOn`: the target is the position the arriving line gets to first. Test in `NavigationTest`: "Line through both signposts", in both directions. Replay 28, 02, 01 and 01, 02, 28 through the core with the real data.

## 2. Apps

- [x] 2.1 Check that neither app measures to the knot's main position itself for the panel, the line or the camera while navigating, and raise it if one does. On the emulator with `route.json` 02, 28, 27: `geo fix` towards the north-west signpost of 02. The distance shrinks, the dashed line ends there while off route, and passing it gives "Next 28". Screenshots, then restore the route and camera. iOS: the same in the simulator; restore the flags and data, and grep that no temporary code is left.

## 3. Wrap-up

- [x] 3.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The maintainer checks it at Siegburg 02 on the Pixel and the iPhone, approaching the north-west signpost before starting at it, then `/opsx:archive aim-at-used-signpost`, after `routes-through-signposts`.
