# Tasks

## 1. Core

- [x] 1.1 `Navigation.courseAhead`, kept from `geometry.courses(knots)`, and `MapFeatures.legAhead`. Tests in `NavigationTest`: "Which way at a fork" (the course to the next knot, in riding direction, also when the route comes back on it), "Passing a knot", "Before the first knot" (`null`), "Off route" (still the course to the next knot), and without courses (the straight line).

## 2. Android

- [x] 2.1 Offsets for `route`, its arrows and the track; the widths; a tap on the zoomed-out line still zooms to the route. On the emulator, a planned round trip using a stretch twice (the video's 02 → 28 → 27 → … → 02): a screenshot of two lines side by side with arrows both ways ("A stretch used twice", "A stretch used once"), and at Siegburg 02 for sharp bends.
- [x] 2.2 `route-ahead` and its arrows, and the thin route without arrows while navigating. With a mocked ride to 28: a screenshot at the fork ("Which way at a fork"), then past 28 ("Passing a knot"), off route ("Off route"), before the first knot ("Before the first knot"), and after Stop ("Ending navigation"). The track stays on top. Light and dark.

## 3. iOS

- [x] 3.1 The same in `NetworkMapView`. The project builds, and simulator screenshots of the planned round trip and of a mocked ride at the fork, in light and dark.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The maintainer checks it on a ride, on the Pixel and the iPhone, before `/opsx:archive out-and-back-legs`, after `routes-through-signposts` and `show-rider-on-top`.
