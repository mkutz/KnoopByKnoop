# Tasks

## 1. Spike: background location with "while in use"

- [x] 1.1 Android: a throwaway foreground service of type `location`, started from the visible app with only `ACCESS_FINE_LOCATION` granted, keeps receiving fixes and can speak with `TextToSpeech` for 10 minutes with the screen off (emulator with `adb emu geo fix` updates; log the fixes)
- [x] 1.2 iOS: a throwaway background location session with "when in use" permission keeps receiving fixes and can speak with `AVSpeechSynthesizer` for 10 minutes with the screen locked; find out whether the `audio` background mode is needed (simulator with `simctl location`, then the maintainer's iPhone)
- [x] 1.3 Record the results in `spike.md` in this change; if "while in use" is not enough on a platform, stop and revise proposal and design before going on

## 2. Core: navigation state machine

- [x] 2.1 `Navigation` with `Fix`, `NavigationUpdate`, `Notice` and `Precision`, created from the route's knots, the courses per leg (none without geometry) and the first fix; covered by `NavigationTest`: "Start at the first knot", "Start at a knot in the middle of the route", "Start between two knots", "Start away from the route", "Incomplete route"
- [x] 2.2 Passing knots by entering their 30 m circle, checked on the line between fixes; tests: "Passing the next knot", "Riding close to a knot without passing it", "Round trip", and a knot passed between two fixes 40 m apart
- [x] 2.3 Reminder 200 m before the next knot, once per knot; tests: "Approaching the next knot", "Knots close together"
- [x] 2.4 Skipping ahead to the first later occurrence; tests: "Shortcut", and a route that passes a knot twice
- [x] 2.5 Off route and back on route, with the accuracy rule for coarse fixes; tests: "Passing a knot that is not on the route", "Wrong turn between knots", "Back on route", "Without geometry", "Off route is announced once", and a coarse fix far from the course that only switches to `FINE`
- [x] 2.6 Arrival; tests: "Arriving" (the core part: the "You've arrived" notice and the arrived state)
- [x] 2.7 Texts to show and to speak, with numbers spelled without leading zeros; tests: "Next knot in the middle of the route", "Next knot is the end", "Number with a leading zero"
- [x] 2.8 Precision from the state (`COARSE`, `FINE`, `LIVE` while visible); tests for each row of the design's precision table
- [x] 2.9 `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` passes

## 3. Android

- [ ] 3.1 Permissions in the manifest, the runtime request on Navigate, and the message when location access is denied ("Location access denied")
- [ ] 3.2 `NavigationService` (foreground, type `location`) with the fused location provider, mapping the core's precision, and a process-wide holder shared with `AppViewModel`; check on the emulator with simulated fixes that navigation keeps running with the screen off
- [ ] 3.3 Ongoing notification with "At 45 follow 12", off route, and a Stop action ("Lock screen", "Off route outside the app", "Stop from the lock screen")
- [ ] 3.4 `TextToSpeech` with navigation guidance usage and ducking audio focus ("Screen off", "Music playing")
- [ ] 3.5 Planner: Navigate button for a complete route, the header with "At 45 follow 12" and Stop while navigating, the waypoint list and knot taps locked ("Tapping a knot while navigating", "Stopping in the app")
- [ ] 3.6 Map: the rider's position as a point layer from the navigation's fixes and the next knot highlighted ("Looking at the map", "Off route on the map")

## 4. iOS

- [ ] 4.1 Location usage description, the "when in use" request on Navigate, and the message when access is denied ("Location access denied")
- [ ] 4.2 Background location with `CLBackgroundActivitySession`, mapping the core's precision; check with `simctl location` that navigation keeps running in the background
- [ ] 4.3 Widget extension target in `project.yml` with the Live Activity for the lock screen and the Dynamic Island (compact, minimal, expanded) and Stop as a `LiveActivityIntent` ("Lock screen", "Dynamic Island", "Off route outside the app", "Stop from the lock screen")
- [ ] 4.4 `AVSpeechSynthesizer` with a `.voicePrompt` audio session that ducks other audio ("Screen off", "Music playing")
- [ ] 4.5 Planner: Navigate button, the header while navigating, editing and knot taps locked ("Tapping a knot while navigating", "Stopping in the app")
- [ ] 4.6 Map: the rider's position and the next knot highlighted ("Looking at the map", "Off route on the map")

## 5. Checks and docs

- [ ] 5.1 Both apps build; `./gradlew check` passes; `xcodebuild` builds both iOS targets
- [ ] 5.2 CI passes
- [ ] 5.3 `README.md` "What the apps can do" lists navigation
- [ ] 5.4 Manual checks in `manual-checks.md` for the scenarios that depend on the platforms (permissions, screen off, notification, Live Activity, Dynamic Island, speech and ducking, map, Stop, "Arriving" end to end): on the Android emulator with a simulated ride, and by the maintainer on the iPhone on a real ride
- [ ] 5.5 Battery: the maintainer navigates for at least one hour with the screen off on the iPhone and records the battery drop in `manual-checks.md` ("One hour with the screen off", at most 5 percentage points); tune the core's thresholds if needed

## Workflow follow-up

- Implement after `show-ferry-schedules` and `scroll-long-routes` are archived
- Store submissions need the background location justification and Google Play's foreground service declaration (see design)
