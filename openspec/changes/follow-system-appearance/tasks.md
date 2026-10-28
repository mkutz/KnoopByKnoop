# Tasks

## 1. Android

- [x] 1.1 `darkColorScheme` next to the light one, chosen by `isSystemInDarkTheme()`; a `values-night` window theme; `uiMode` in the activity's `configChanges`; the app builds and turns dark on `adb shell cmd uimode night yes` ("Dark mode", "Light mode")
- [x] 1.2 Base map and offline style by appearance in `NetworkMap.kt`, switched in place on a configuration change, re-adding the layers through the existing style-loaded path; checked on the emulator by switching with a planned and zoomed route ("Switching while the app is open", "Offline in dark mode" in airplane mode on first start)
- [x] 1.3 Dark action colors in `PlannerActions.kt` for the icon buttons ("Readable actions in dark mode")
- [x] 1.4 After falling back to the offline style while online, load the base map again in the background; checked on the emulator with the base map's cache cleared (`pm clear`) in dark mode ("Slow base map")

## 2. iOS

- [x] 2.1 Remove the forced light appearance from the views over the map and the planner panel; adaptive icon-button color in `Shared/Badges.swift`; `xcodebuild` builds and the simulator shows a dark panel with `simctl ui appearance dark` ("Dark mode")
- [x] 2.2 Base map and offline style by appearance in `NetworkMapView.swift`, switched on trait changes; checked in the simulator by switching the appearance while a route is shown ("Switching while the app is open")
- [x] 2.3 The same retry of a slow base map as on Android ("Slow base map")
- [x] 2.4 The Live Activity's lock screen card dark in dark mode, light as before in light mode; both targets build ("Lock screen in dark mode", checked by the maintainer on the iPhone)

## 3. Checks

- [x] 3.1 Contrast of the actions on the dark panels of both apps computed and recorded in `manual-checks.md` ("Readable actions in dark mode")
- [x] 3.2 Manual checks in `manual-checks.md` with screenshots in light and dark mode: on the Android emulator "Dark mode", "Light mode", "Switching while the app is open", "Switching while navigating" (simulated ride, `cmd uimode night yes` during it, notices keep coming in the log), "Route on the dark map" (the 08 → 31 → 97 route with its ferries), "Offline in dark mode"; on the iPhone by the maintainer "Dark mode", "Switching while the app is open", "Switching while navigating" and "Route on the dark map"
- [x] 3.3 `./gradlew check` passes, both iOS targets build, CI passes

## Workflow follow-up

- Archive `clearer-planner-actions` before archiving this change, since this change modifies its requirement "Clear actions in the planner panel"
