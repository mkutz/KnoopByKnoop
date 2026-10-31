# Tasks

## 1. Core: coming knots

- [x] 1.1 `ComingKnot` and `ProgressView.coming` from `RouteProgress.view`, with distances and arrivals as the strip's labels, ferries between their knots with their state, and the screen reader description. Verify with `RouteProgressTest` cases "Coming knots at the start", "After passing knots", "Ferry in the list", "Off route", "Long route" (the core part) and "Coming knots with a screen reader", on the JVM and the iOS simulator (`./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test`)

## 2. Android

- [x] 2.1 `PlannerScreen.kt`: the sheet draggable while navigating, with the handle, its smallest height the navigation part; the coming knots as rows of `WaypointList` while navigating (badge, distance, arrival, ferries with their state, TalkBack description); the list below the strip in landscape. Verify on the emulator (`ANDROID_SERIAL=emulator-5554`) with mocked positions on a route with a stop and the end, with screenshots: "Starting with the smallest panel", "Starting with a high panel", "Dragging the panel", "Dragging the panel up", "Ending navigation", "Navigating in landscape", "Coming knots at the start", "After passing knots", "Long route", "Coming knots with a screen reader" (`uiautomator dump`)
- [x] 2.2 `AppViewModel` and `NetworkMap.kt`: tapping a coming knot brings the sheet down and shows the knot on the map, stopping to follow; recentering clears it. Verify on the emulator: "Tapping a coming knot", "Back to the rider", with screenshots

## 3. iOS

- [x] 3.1 `PlannerView.swift`: detents `[navigation, .medium, .large]` with the drag indicator while navigating; the coming knots as rows of the `List`, with VoiceOver descriptions; in landscape below the strip. Verify the project builds with `xcodebuild`
- [x] 3.2 `AppModel`/`NetworkMapView.swift`: tapping a coming knot brings the sheet to the navigation detent and shows the knot on the map; recentering clears it. Verify the project builds, install it on the maintainer's iPhone, and write the steps for every scenario in `openspec/changes/show-coming-knots/manual-checks.md`

## 4. Wrap-up

- [x] 4.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive show-coming-knots`.
