# Tasks

## 1. Core

- [x] 1.1 Start at the first knot when more than 100 m from the course; nearest knot without courses; verified by `NavigationTest` "Start away from the route", "Joining the route on the way to the start", "Start away from the route without courses", and the other start tests unchanged
- [x] 1.2 `Navigation.guidance(fix)` with distance, bearing and compass word; verified by `GuidanceTest` for the eight directions and "Direction in the header" (the core part)
- [x] 1.3 Off-route notice with distance and direction, guidance notices after 500 m closer or a turn of more than 45°; verified by `GuidanceTest` "Passing a knot that is not on the route", "Wrong turn between knots", "Getting closer", "Direction changes", "Riding around without getting closer", "Off route is announced once"
- [x] 1.4 `progressLine` off route ("1.7 km north-east to 02"), renewed per 100 m, back to the route line on route; verified by `GuidanceTest` "Lock screen off route", "Updated on the way", "Back on route outside the app"
- [x] 1.5 `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` pass

## 2. Android

- [x] 2.1 Dashed line to the next knot off route, both in view while following; verified on the emulator with mocked positions ("Line to the knot", "Both in view", "Back on route"), with screenshots
- [x] 2.2 Arrow and compass word in the header, relative to the riding direction; screen reader text; verified on the emulator ("Direction in the header", "Standing still", "Direction with a screen reader" via `uiautomator dump`), with screenshots
- [x] 2.3 Guidance notices spoken and the notification line off route; verified on the emulator via the notice log and `dumpsys notification` ("Getting closer", "Lock screen off route", "Updated on the way")

## 3. iOS

- [x] 3.1 The line and camera in `NetworkMapView.swift`, the arrow in `PlannerView.swift`, the Live Activity line; builds with `xcodebuild`
- [x] 3.2 Steps for the maintainer's iPhone checks in `manual-checks.md`, a real ride or walk away from a route included

## 4. Wrap-up

- [x] 4.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive guide-back-to-route`.
