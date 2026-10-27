# Tasks

## 1. iOS

- [x] 1.1 `PlannerView`: in landscape (wider than high), show map and waypoint panel in an `HStack` instead of the presented sheet; the info is shown inside the panel, as in portrait
- [x] 1.2 The iOS app builds (`xcodegen` and `xcodebuild` for the simulator)

## 2. Android

- [x] 2.1 `PlannerScreen`: in landscape, a `Row` of map and waypoint list instead of `BottomSheetScaffold`; map state survives rotation
- [x] 2.2 `./gradlew :apps:android:assembleDebug` passes

## 3. Manual checks

- [x] 3.1 On the Android emulator and the iOS simulator, record in `manual-checks.md` in this change, with screenshots: "Planning in landscape", "Rotating with a route", "Map controls in landscape"
- [x] 3.2 `./gradlew check` and the iOS app build pass locally and in CI
