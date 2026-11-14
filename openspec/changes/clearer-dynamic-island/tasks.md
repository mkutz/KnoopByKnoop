# Tasks

## 1. Badges without corner symbols in the island

- [x] 1.1 Give `SignKnot` in `apps/ios/Shared/SignViews.swift` a parameter to leave out the corner symbol, on by default so every other use stays the same. Pass it from `CompactLeading` and `Minimal` in `apps/ios/KnoopByKnoopWidgets/NavigationLiveActivity.swift`, and not from the expanded regions or the lock-screen card. Verify: `xcodegen` and `xcodebuild build` for the simulator pass.
- [x] 1.2 Add "Stop next in the Dynamic Island" and "Corner symbols in the expanded island" to `openspec/checked-by-hand.md` under `knot-navigation`, checked in the simulator by previewing the Live Activity's compact, minimal and expanded views with the end next, and on the iPhone on the next ride or walk. Verify: in the simulator, the compact and minimal island show 71 without ■ and nothing cut off, and the expanded island and lock screen show ■.

## 2. The distance in the compact island

- [x] 2.1 In `compactTrailing`, show the distance larger and bold (about the badge's height, e.g. `.title3`) with `.monospacedDigit()`, keeping `lineLimit(1)` and a minimum scale factor as the last resort. Verify: in the simulator, "4.0 km", "1.7 km" and "150 m" show in full and the island's width stays the same between "180 m" and "110 m".
- [x] 2.2 Add "Distance counting down in the Dynamic Island" to `openspec/checked-by-hand.md` under `knot-navigation`, checked in the simulator and on the iPhone. Verify: `./gradlew checkScenarios -Pchange=clearer-dynamic-island` lists no missing scenario.

## 3. Release notes and checks

- [x] 3.1 Add a line under "## Unreleased" in `CHANGELOG.md` (create the section, it went with 1.0.0): the Dynamic Island no longer shows the cut-off corner symbol of the start, a stop or the end, and its distance is larger and keeps its width. Verify: `grep -n "Dynamic Island" CHANGELOG.md` finds it under Unreleased.
- [x] 3.2 Run `./gradlew checkScenarios -Pchange=clearer-dynamic-island`. Verify: it passes.
