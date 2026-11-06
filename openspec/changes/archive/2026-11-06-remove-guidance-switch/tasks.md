# Tasks

## 1. Core

- [x] 1.1 `PathSquares.courseToLoad` without `enabled`, with the course the ways were deleted for.
  - `PathStoreTest`: "First start" (a cycling course is loaded with no setting), "A hiking route", and "Deleting with a route planned" (the deleted-for course gives `null`, another course or none deleted-for gives the course).
- [x] 1.2 `PathStore.installedBytes()`: the squares' sizes plus the cached directory, 0 after `clear()`.
  - `PathStoreTest`: "Opening the info sheet with path data" (the sum of two installed squares and the directory), "No path data installed" (0), "Deleting the ways" (0 after `clear()`, and the graph files in the same folder untouched).

## 2. Android

- [x] 2.1 `AppViewModel`: remove `guidance`, `chooseGuidance` and `KEY_GUIDANCE`. Add the installed size, `deletePathData()` with the deleted-for course, and clear it in `startNavigation`.
- [x] 2.2 `InfoSheet`: the section "Downloaded data" with the ways' row, size, delete button with its screen reader label, and the explanation. Remove `GuidanceSwitch` and the `guidance` string. Texts in English, German, Dutch and French.
- [x] 2.3 Check on the emulator with a fresh install of this build, with screenshots in each language:
  - "First start": plan 02 → 80 from Siegburg, and the panel says the ways are loading, then loaded;
  - "Opening the info sheet with path data", "Explanation", "Deleting the ways", "No path data installed";
  - "Deleting with a route planned": no path request in logcat until a knot is added, then the squares come back;
  - "Delete button with a screen reader" with TalkBack;
  - "Status with a screen reader" again, as the panel's status now shows without a switch.

## 3. iOS

- [x] 3.1 `AppModel`: the same as 2.1, with `guidance` and its `UserDefaults` key removed.
- [x] 3.2 `InfoSheet.swift`: the same as 2.2, with the explanation as the section's footer, and the strings in all four languages.
- [x] 3.3 The project builds with `xcodebuild`. Check the same scenarios as 2.3 in the simulator, with VoiceOver's label read from the accessibility tree.

## 4. Specs in the code and the rest

- [x] 4.1 Change the comments citing "Guided way while off route" to "Guided way back to the route" (`NetworkMapView.swift`, `Navigator.swift`, `NavigationService.kt`, `NetworkMap.kt`, `Navigating.kt`, and the core). Also the comments citing "Turning on guidance away from the route" or "the switch".
  - Checked by a grep finding none left outside `openspec/changes/archive`.
- [x] 4.2 Existing tests stay green for "Route planned at home", "Changing the route", "Starting away from the route", "Far off route", "Starting before the download is done", "Without network access", "Still downloading", "Ready", "Not available", and the scenarios of "Guided way back to the route" (`PathStoreTest`, `PathDownloaderTest`, `WayGuideTest`, `NavigationTest`).
  - Remove or rewrite tests that only cover the switch.
- [x] 4.3 `PRIVACY.md`: the "In short" line, the GitHub row and the paragraph "Guidance away from the route" without "off until you turn it on"; the deleting goes into the paragraph. Update "Last updated".
- [x] 4.4 `CHANGELOG.md`: rewrite the Unreleased entry for guidance without the switch, and mention the ways' entry with the delete button.
- [x] 4.5 `manual-checks.md` for this change with the results of 2.3 and 3.3, and a row for the maintainer: the panel and the info sheet on the Pixel and the iPhone after installing this build over the one with the switch.
- [x] 4.6 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.

## Workflow follow-up

- Before the release, the maintainer looks at the Play data safety form and the App Store privacy labels for the GitHub downloads.
- Then `add-route-points` is reworked to find the way to a place away from the network with the path data.
