# Tasks

## 1. Core

- [x] 1.1 `NewKnots`, `Route.withNewEnd`, and `PlanningSession.tap(knot, newKnots)`, with its KDoc; existing callers pass `STOP`.
- [x] 1.2 Tests in `PlanningTest`: "Adding a stop", "Extending the route", "Looking around", "A knot beside the route", "Round trip on End", "A knot that cannot be reached", "First two taps" on both modes, and tapping a waypoint on End changes nothing. The existing scenarios of "Plan by tapping knots" keep passing.

## 2. Android

- [x] 2.1 The segmented control below the knot sequence in `WaypointSheet`, in the sheet's smallest height (`peekHeight`) and at the top of the landscape side panel, not while navigating; the choice in the `settings` preferences, passed to `tap`. The string "New knots" in `values`, `values-de`, `values-nl` and `values-fr`.
- [x] 2.2 Check on the emulator: "Extending the route", "Round trip on End", "Default", "Remembered" after a restart, "Switching changes nothing yet", "In the smallest panel" at 360 dp, "In landscape", "While navigating", "Larger text" in German at 130%, "Screen reader" with the labels in the UI tree, "Readable in both appearances". Screenshots; restore the route, camera, font scale and the choice.

## 3. iOS

- [x] 3.1 The segmented `Picker` in `WaypointSheet`, in the smallest detent and at the top of the landscape panel, not while navigating; the choice in `AppStorage`, passed to `tap`. The string in `Localizable.xcstrings`, inserted without reformatting the catalog.
- [x] 3.2 Check in the simulator the same scenarios, with temporary launch flags that are removed afterwards; grep that none is left; screenshots; restore the route, camera and the choice.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check`, the iOS core tests and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased, a section that has to be created after 0.8.0.

## 5. Clearer words and icons

- [x] 5.1 Spec, proposal and design: "Add knots", "Between" and "At end", with an arrow icon on each choice.
- [x] 5.2 Android: the two icons as vector drawables in the segmented buttons' icon slot, and the strings in four languages.
- [x] 5.3 iOS: the two icons and words in a small control of its own, as the segmented `Picker` shows only the words, and the strings in `Localizable.xcstrings`, inserted without reformatting the catalog.
- [x] 5.4 Check again on the emulator and in the simulator: the words and icons in all languages at 360 dp, "The icons", "Larger text", "In landscape", "Readable in both appearances" with measured contrast; new screenshots; restore everything.
- [x] 5.5 `manual-checks.md` and the CHANGELOG entry with the new words; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.

## Workflow follow-up

- The maintainer tries it on both phones, looking around from a start and planning A to B over C, and judges the remembered choice and the map lost to the row, before `/opsx:archive add-new-knots-switch`.
