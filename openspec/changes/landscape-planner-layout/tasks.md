# Tasks

## 1. iOS

- [ ] 1.1 `PlannerView`: in compact vertical size class, show map and waypoint list in an `HStack` instead of the presented sheet; the info sheet is presented from the planner then
- [ ] 1.2 The iOS app builds (`xcodegen` and `xcodebuild` for the simulator)

## 2. Android

- [ ] 2.1 `PlannerScreen`: in landscape, a `Row` of map and waypoint list instead of `BottomSheetScaffold`; map state survives rotation
- [ ] 2.2 `./gradlew :apps:android:assembleDebug` passes

## 3. Manual checks

- [ ] 3.1 On the Android emulator and the iOS simulator, record in `manual-checks.md` in this change, with screenshots: "Planning in landscape", "Rotating with a route", "Map controls in landscape"
- [ ] 3.2 `./gradlew check` and the iOS app build pass locally and in CI
