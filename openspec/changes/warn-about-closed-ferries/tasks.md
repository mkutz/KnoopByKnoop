# Tasks

## 1. Core: arrival estimate

- [ ] 1.1 `Navigation.start` takes the local-time conversion and keeps the ferries ahead with their route positions, the distance from each knot to them (leg lengths from the graph, half the ferry's leg) and the network's assumed pace; verified by unit tests for routes with no, one and two ferries
- [ ] 1.2 Pace from route progress (assumed until 2 km covered, then own pace including stops) and the estimate from the rider's position, renewed at start and on every knot passed, not while off route; verified by `FerryWarningTest` "The rider's own pace", "Hiking", "Falling behind"

## 2. Core: warnings

- [ ] 2.1 `NoticeKind.FERRY` with the warning rules and texts of the design, once per ferry, nearer ferry first, none without a certain status or times; verified by `FerryWarningTest` "Closed when the rider gets there", "Closes shortly after the rider gets there", "Not open yet when the rider gets there", "Closed for the season", "Open when the rider gets there", "Warned once", "Ferry without a name", "Times that cannot be told", "Several ferries", "Ferry passed"
- [ ] 2.2 The rounded arrival and warned state per ferry ahead, for the ferry list; verified by `FerryWarningTest` "Arrival in the ferry list" and "Warning in the ferry list" at core level
- [ ] 2.3 `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` pass

## 3. Android

- [ ] 3.1 `NavigationService` passes the `Europe/Amsterdam` conversion from `FerryClock.kt` to `Navigation.start`; ferry notices are spoken like the others; verified by `FerryClockTest` "Phone in another time zone" for an arrival instant
- [ ] 3.2 `RouteFerries.kt` shows "You get there around HH:MM" while navigating, in the error color when warned; verified on the emulator with a route over a ferry and mock locations (`adb emu geo fix`), with a screenshot
- [ ] 3.3 Manual checks on the emulator: "Closed when the rider gets there" or "Closes shortly after" with the current time and a suitable ferry, "Screen off" (notice spoken with the screen off), "Offline" (airplane mode), recorded in `manual-checks.md`

## 4. iOS

- [ ] 4.1 `Navigator.swift` passes the `Europe/Amsterdam` conversion to `Navigation.start`; `RouteFerries.swift` shows the arrival while navigating, in red when warned; the project builds with `xcodebuild`
- [ ] 4.2 Steps for the maintainer's iPhone checks ("Arrival in the ferry list", "Screen off", "Offline", "Phone in another time zone") in `manual-checks.md`

## 5. Wrap-up

- [ ] 5.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive warn-about-closed-ferries`.
