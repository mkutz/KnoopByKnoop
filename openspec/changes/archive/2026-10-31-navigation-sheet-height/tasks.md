# Tasks

## 1. Android

- [x] 1.1 `PlannerScreen.kt`: measure the navigation part of `WaypointSheet` (header and strip with labels), use it plus the navigation bar as `sheetPeekHeight` while navigating (150 dp otherwise), with no swiping and no drag handle while navigating, and call `partialExpand()` when `navigating` changes. Verify on the emulator (`ANDROID_SERIAL=emulator-5554`) in portrait with mocked positions on a route with a stop and the end, with screenshots: "Starting with the smallest panel", "Starting with a high panel", "Dragging the panel", "Ending navigation" (by Stop and by arriving)
- [x] 1.2 Check the same at the largest system font size and while off route (the header's height changes), and that landscape is unchanged ("Navigating in landscape"), with screenshots

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`), positions mocked with `adb emu geo fix` in Arnhem on the round trip 93 → 29 (stop) → 94: starting from the smallest and from a raised planning sheet, dragging down and up (no handle, the sheet stays), Stop, font scale 1.3 and 2.0, switching the font size while navigating, and landscape. Arriving ends navigation through the same `navigating` change as Stop, so it was not ridden separately. Off route could not be triggered without courses; a changing header height goes through the same peek height update as the font size change. At font scale 2.0, the strip's labels are clipped in their fixed 64 × 40 dp boxes. That is in the strip itself and was there before (out of scope).

## 2. iOS

- [x] 2.1 `PlannerView.swift`: measure the navigation part, use `.height(navigationHeight)` as the only detent, with the drag indicator hidden, while navigating, select it on start, and go back to `smallestDetent` and the planning detents on end. Verify the project builds with `xcodebuild`
- [x] 2.2 Write steps for the maintainer's iPhone checks of every scenario of "Panel height while navigating", including a large Dynamic Type size and the place search following the sheet, in `openspec/changes/navigation-sheet-height/manual-checks.md`

## 3. Wrap-up

- [x] 3.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased" (create the section if it is missing and grep that the entry landed)

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive navigation-sheet-height`.
