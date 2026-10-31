# Tasks

## 1. Android

- [x] 1.1 `ProgressPanel.kt`: label width and height scaled by the font scale, also in the overlap ranking and the strip's height. Verify on the emulator (`ANDROID_SERIAL=emulator-5554`) with mocked positions on a route with a stop and the end, with screenshots at font scale 1.0, 1.3 and 2.0: "Largest text size", "Labels that would overlap at a large text size", "Default text size"
- [x] 1.2 `KnotBadges.kt`: `KnotBadge` numbers independent of the font scale. Verify on the emulator at font scale 2.0 in planning (badge row, waypoint list) and navigating (header, strip), with screenshots: "Badge at the largest text size"

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`) on the round trip 93 → 29 (stop) → 94 in Arnhem, positions mocked with `adb emu geo fix`. At font scale 2.0, the labels "2.2 km 17:55" and "4.0 km 18:00" are complete, and the badges in the header, the strip, the badge row and the waypoint list show their numbers whole. At first the badge numbers sat low and were clipped, because the line height still grew with the font scale; it is now in dp as well. On this route, the labels did not overlap even at 2.0, so "Labels that would overlap at a large text size" was not seen on the emulator; the ranking is unchanged and now works with the scaled width.

## 2. iOS

- [x] 2.1 `ProgressPanel.swift`: `labelWidth` and `labelHeight` as `@ScaledMetric(relativeTo: .callout)`. Verify the project builds with `xcodebuild`
- [x] 2.2 Steps for the maintainer's iPhone checks of every scenario, at the default and the largest Dynamic Type size (including Larger Accessibility Sizes), in `openspec/changes/strip-at-large-text/manual-checks.md`

## 3. Headers at large text sizes

- [x] 3.1 Android `PlannerScreen.kt`: the distance to the next knot wraps below the number to follow; the planning summary stays one line each with the actions below it where it does not fit. Verify on the emulator at font scale 1.0 and 2.0, planning and navigating, with screenshots: "Number to follow at the largest text size", "Number to follow at the default text size", "Route summary at the largest text size", "Route summary at the default text size"
- [x] 3.2 iOS `PlannerView.swift`: the same with `ViewThatFits`. Verify the project builds with `xcodebuild`, install it on the maintainer's iPhone, and add the steps to `manual-checks.md`

## 4. Wrap-up

- [x] 4.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive strip-at-large-text`.
