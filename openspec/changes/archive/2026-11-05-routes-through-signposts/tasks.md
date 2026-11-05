# Tasks

## 1. Pipeline and file

- [x] 1.1 `NetworkExtraction`: find the pairs and their courses; `Main` simplifies them and logs how many connections have them. Run on the Köln extract and check Siegburg: 02-06 has both branches, and report the share and the size. Tests in `NetworkExtractionTest`: "Two branches of a route", "Only through another node", "One node". Pause with the numbers if the share is tiny or the size is at risk.
- [x] 1.2 `NetworkGeometry`: write and read the section. Tests in `NetworkGeometryTest`: a round trip, and a file without the section. "Released apps": read a new file with the core of 0.7.0, as done for `pass-merged-knots`.

## 2. Core

- [x] 2.1 `NetworkGeometry.courses`, used by `courseOf(route)` and `MapFeatures.route`. Tests: "Through the signpost of the next leg", "The other direction out of the knot", "A stop at the knot", "Back the same way", "No shared signpost", "Same distance".
- [x] 2.2 `Navigation.distanceToCourse` also measures to the other courses. Tests in `NavigationTest`: "Following the line shown" (with a branch more than 100 m from the main course), "The other branch", "Away from all courses".

## 3. Apps

- [x] 3.1 No app code. On the emulator with Köln data built by the new pipeline: screenshots of 06 → 02 → 28, 06 → 02 → 01 and 28 → 02 → 01, and a mocked ride along the north-west branch staying on route. iOS: the project builds, and the first two in the simulator.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- After pushing, a data run publishes the new section; the file size is checked there ("Geometry size budget").
- The maintainer checks it at Siegburg 02 on the Pixel and the iPhone, before `/opsx:archive routes-through-signposts`, after `pass-merged-knots` and `show-knot-signposts`.
