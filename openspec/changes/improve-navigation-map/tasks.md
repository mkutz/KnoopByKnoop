# Tasks

## 1. Core

- [x] 1.1 `RiddenTrack.directionAt(fix)`: the bearing over the most recent movement of at least 10 m within 30 s, `null` after more than 10 s without moving 5 m; tests in `RiddenTrackTest`: "Riding" (east for 20 m gives 90°), a curve, "Standing still", a track too short
- [x] 1.2 `RiddenTrack` with `add(fix)` dropping fixes less accurate than 50 m and closer than 5 m to the last point; tests in `RiddenTrackTest`: "Inaccurate fix", points kept in order, standing still adds nothing
- [x] 1.3 `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` passes

## 2. Android

- [x] 2.1 `NavigationService` keeps a `RiddenTrack` and publishes it in `NavigationState`; dropped when navigation ends ("Navigation ends")
- [x] 2.2 Map: the position as an arrow rotated by the direction, or the dot; the track as a blue line below the route ("Riding", "Standing still", "Detour")
- [x] 2.3 Following in `MapHolder`, turned off by gestures, and the recenter button in `PlannerScreen.kt` ("Riding with the map following", "Looking around", "Recentering", "No automatic return", "Off route while following")

## 3. iOS

- [x] 3.1 `Navigator` keeps a `RiddenTrack` and publishes it; dropped when navigation ends
- [x] 3.2 Map: arrow or dot, and the track line, in `NetworkMapView.swift`
- [x] 3.3 Following in the coordinator, turned off by gestures, and the recenter button in `PlannerView.swift`; `xcodebuild` builds both targets

## 4. Checks

- [ ] 4.1 Manual checks in `manual-checks.md` with screenshots on the Android emulator, with a simulated ride by `adb emu geo fix` along the route and on a detour, giving positions that change from fix to fix (repeated identical fixes produce no updates): every scenario of the three requirements, including drag, pinch and double-tap each stopping following, and "Screen off" with the screen off for part of the ride; on the iPhone by the maintainer on a real ride: "Riding", "Standing still", "Detour", "Looking around", "Recentering"
- [x] 4.2 `./gradlew check` passes, both iOS targets build, CI passes

## Workflow follow-up

- Archive `add-knot-navigation` before this change: the requirements here are added to `knot-navigation`, whose main spec that change creates
