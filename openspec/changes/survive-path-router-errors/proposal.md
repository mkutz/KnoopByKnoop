# Proposal

## Why

The code review of 2026-11-10 found that an error inside a way search crashes the iOS app: `Navigator.findWay` and `AppModel.findWays` call the core's `PathRouter` without a guard, and an exception from Kotlin that reaches Swift ends the app. Android catches it while navigating, but not in the planner's ways for route points (`AppViewModel`), where an exception in the coroutine ends the app as well. A square that passes its checksum but can't be unpacked or decoded, or a failing read on the phone, is enough. It is rare, but it would end a ride.

## What Changes

- **The router fails safe:** a way search that throws finds no way, frees the loaded squares, and the app goes on with the straight line, as when running out of memory. This holds for both apps and for both searches: the guided way while navigating and the ways of route points while planning.
- **One guard instead of several:** the guard sits in the core's router, so neither app needs its own; Android's catch of exceptions while navigating goes, its catch of running out of memory stays (the core can't catch that on all platforms).

## Out of Scope

- Reporting such errors anywhere (there is no crash reporting, see the tester feedback of 2026-11-01).
- Removing or downloading again the square that failed; the next search reads it again.
- Running out of memory on iOS: Kotlin/Native ends the app, and it can't be caught. The square limit (`MAX_KEPT_SQUARES`) keeps it unlikely.
- The other places where the apps call the core without a guard; the review's change for moving the navigation loop into the core covers them.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `path-routing`: "No way found" gets a scenario for a search that fails on broken path data, on both apps.

## Impact

- **Core:** `PathRouter.way` catches exceptions, forgets the squares and returns `null`. A test with a square source that throws.
- **Android:** the catch of `Exception` in `NavigationService.findWay` goes; the log line for it moves to the result (`way == null`), which is already logged.
- **iOS:** no code change; the guard in the core covers `Navigator.findWay` and `AppModel.findWays`.
