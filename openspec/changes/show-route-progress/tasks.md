# Tasks

## 1. Core: route progress

- [ ] 1.1 `RouteProgress` with the knot offsets, courses per leg, stop positions and assumed pace; `FerryWatch` uses its offsets and pace; verified by the existing `FerryWarningTest`, unchanged, and new `RouteProgressTest` cases for offsets and stops
- [ ] 1.2 The rider's place on the route (on the course, without courses, off route) and the pace renewed per knot; verified by `RouteProgressTest` "Moving along", "Along the way", "Off route on the strip", "Off route ahead"
- [ ] 1.3 Duration and arrival rounding; verified by `RouteProgressTest` "Short time", "Very short time", "Long time rounded", "Arrival rounded"
- [ ] 1.4 `Navigation.start` takes the stop positions; `Navigation.progress(fix)` with the strip data, the rows and the screen reader text; verified by `RouteProgressTest` "Strip at the start", "Ferry on the strip", "Progress with a screen reader", "Ahead at the start", "The rider's own pace", "Long time", "No stop left", "Next knot is the next stop", "Round trip"
- [ ] 1.5 `Navigation.progressLine`, computed at the start and per knot passed; verified by `RouteProgressTest` "Progress outside the app" and a test that it does not change between knots
- [ ] 1.6 `RouteDescription.Complete.duration`; verified by `FerryTest`/`PlanningTest` "Route summary", "Hiking time", "Long route time"
- [ ] 1.7 `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` pass

## 2. Android

- [ ] 2.1 `NavigationService` passes the stop positions; the notification shows `progressLine` as its content text; verified on the emulator with the screen off ("Lock screen", "Progress outside the app"), with a screenshot of the notification shade
- [ ] 2.2 The progress strip in place of the badge row while navigating, with screen reader text; verified on the emulator with mocked positions along a route with a stop and a ferry (61 → 50 → 03 or 76 → 02 → 20): start, moving along, off route, with screenshots ("Long route" with a route of many knots)
- [ ] 2.3 The table below the strip; verified on the emulator with the same rides ("Ahead at the start", "Along the way", "No stop left"), with screenshots
- [ ] 2.4 The planning header with the duration; verified on the emulator for a cycling and a hiking route, with screenshots

## 3. iOS

- [ ] 3.1 `Navigator.swift` passes the stop positions; `ContentState.progress` in the Live Activity (lock screen and expanded Dynamic Island); the strip, the table and the planning duration in `PlannerView.swift`; the project builds with `xcodebuild`
- [ ] 3.2 Steps for the maintainer's iPhone checks (strip, table, Live Activity, planning time, VoiceOver) in `manual-checks.md`

## 4. Wrap-up

- [ ] 4.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive show-route-progress`.
