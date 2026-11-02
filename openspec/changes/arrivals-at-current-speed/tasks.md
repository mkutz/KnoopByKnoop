# Tasks

## 1. Core

- [x] 1.1 `Navigation.Pace`: segments of at least 100 m, the 5 km window, the cap at a third of the assumed pace, renewed on each precise fix on the route, and reset when back on route. Tests in `RouteProgressTest`:
  - "Slowing down": 20 km at 20 km/h, then 5 km at 12 km/h, with 10 km left at 18:00 gives "18:50".
  - "Between two knots": the labels change with no knot passed.
  - "A break": 30 minutes standing, then the pace is 20 km/h again, and the arrivals moved later while standing.
  - "A detour": 10 minutes off route do not count.
  - The existing tests "the rider's own pace" and the assumed pace until 2 km still hold.
- [x] 1.2 `FerryWatch.renewed` without notices, called per fix. Tests in `FerryWarningTest`:
  - "Closed at the renewed estimate": `ferryArrival` shows the warning state between knots with no notice, and the notice comes at the next knot.
  - "Warned once", "falling behind" and the existing ferry scenarios still hold.

## 2. Apps

- [x] 2.1 No app code. On the emulator, a mocked ride along a route at two speeds: a screenshot before and after shows the arrivals on the strip and in the coming knots moving between two knots. A stop of a few minutes moves them later, and riding on brings the old pace back. Watch for flicker near a rounding boundary. iOS: the project builds with the new core; the same in the simulator with `simctl location start` at two speeds.

## 3. Wrap-up

- [x] 3.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The maintainer checks it on a ride, on the Pixel and the iPhone, before `/opsx:archive arrivals-at-current-speed`.
