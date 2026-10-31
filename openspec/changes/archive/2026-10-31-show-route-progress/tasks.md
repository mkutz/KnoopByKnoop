# Tasks

## 1. Core: route progress

- [x] 1.1 `RouteProgress` with the knot offsets, courses per leg, stop positions and assumed pace; `FerryWatch` uses its offsets and pace; verified by the existing `FerryWarningTest`, unchanged, and new `RouteProgressTest` cases for offsets and stops
- [x] 1.2 The rider's place on the route (on the course, without courses, off route) and the pace renewed per knot; verified by `RouteProgressTest` "Moving along", "Along the way", "Off route on the strip", "Off route ahead"
- [x] 1.3 Duration and arrival rounding; verified by `RouteProgressTest` "Short time", "Very short time", "Long time rounded", "Arrival rounded"
- [x] 1.4 `Navigation.start` takes the stop positions; `Navigation.progress(fix)` with the strip data, the rows and the screen reader text; verified by `RouteProgressTest` "Strip at the start", "Ferry on the strip", "Progress with a screen reader", "Ahead at the start", "The rider's own pace", "Long time", "No stop left", "Next knot is the next stop", "Round trip"
- [x] 1.5 `Navigation.progressLine`, computed at the start and per knot passed; verified by `RouteProgressTest` "Progress outside the app" and a test that it does not change between knots
- [x] 1.6 `RouteDescription.Complete.duration`; verified by `FerryTest`/`PlanningTest` "Route summary", "Hiking time", "Long route time"
- [x] 1.7 `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` pass

## 2. Android

- [x] 2.1 `NavigationService` passes the stop positions; the notification shows `progressLine` as its content text; verified on the emulator with the screen off ("Lock screen", "Progress outside the app"), with a screenshot of the notification shade
- [x] 2.2 The progress strip in place of the badge row while navigating, with screen reader text; verified on the emulator with mocked positions along a route with a stop and a ferry (61 → 50 → 03 or 76 → 02 → 20): start, moving along, off route, with screenshots ("Long route" with a route of many knots)
- [x] 2.3 The table below the strip; verified on the emulator with the same rides ("Ahead at the start", "Along the way", "No stop left"), with screenshots
- [x] 2.4 The planning header with the duration; verified on the emulator for a cycling and a hiking route, with screenshots

## 3. iOS

- [x] 3.1 `Navigator.swift` passes the stop positions; `ContentState.progress` in the Live Activity (lock screen and expanded Dynamic Island); the strip, the table and the planning duration in `PlannerView.swift`; the project builds with `xcodebuild`
- [x] 3.2 Steps for the maintainer's iPhone checks (strip, table, Live Activity, planning time, VoiceOver) in `manual-checks.md`

## 4. Wrap-up

- [x] 4.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## 5. Revision after the iPhone checks

- [x] 5.1 Core: distance and arrival on the strip's stops, ferries and end ahead (none for the start, passed marks and plain knots), the distance to the next knot for the header, no rows; verified by `RouteProgressTest` "Ahead at the start", "Along the way", "The rider's own pace", "Ferry ahead on the strip", "Stop passed", "Round trip", "Off route on the strip ahead", "Distance off route", on the JVM and the iOS simulator
- [x] 5.2 Android: header with badges and the distance, labels below the strip's marks with the overlap rules, no table; verified on the emulator with the Renkum and the long route ("Stops close together", "Distances that would overlap", "Next knot in the middle of the route"), with screenshots
- [x] 5.3 iOS: the same, and each ferry in its own list row with a borderless website link (tapping a ferry without a website no longer opens another's); builds with `xcodebuild`; steps in `manual-checks.md`
- [x] 5.4 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG updated

## 6. Shorter long distances

- [x] 6.1 Distances from 10 km in whole kilometers ("200 km"), in the core's distance format used by the strip, the notification and the planner; verified by `RouteProgressTest` "Short distance", "Middle distance", "Long distance" and the updated strip tests; iOS and Android show "280 km" unclipped on the long route, with a screenshot

## 7. Ferries on the strip

- [x] 7.1 Core: ferry marks with their route position and arrival state; `FerryArrival.description` for screen readers; verified by `RouteProgressTest` "Closed ferry before a stop" and "Open ferry before a stop" (the core part: the states), "Open ferry ahead", and a test of the description, on the JVM and the iOS simulator
- [x] 7.2 Android: badge and label ranking with warned ferries, red ring and colored arrivals, the popover on tap, ferries as buttons for TalkBack, no ferry list while navigating; verified on the emulator with the Renkum and the long route ("Ferry popover", "Popover of a passed ferry", "No ferry list while navigating", "Ferry with a screen reader" via `uiautomator dump`), with screenshots
- [x] 7.3 iOS: the same with `.popover`; builds with `xcodebuild`; steps in `manual-checks.md`
- [x] 7.4 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG updated

## 8. No info button while navigating

- [x] 8.1 Android and iOS hide the ⓘ and its bar while navigating; verified on the emulator while planning and navigating ("Info button", "No info button while navigating"), with a screenshot; iOS builds; step in `manual-checks.md`

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive show-route-progress`.
