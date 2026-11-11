# Proposal

## Why

The code review of 2026-11-10 found the per-fix navigation loop written twice: `NavigationService.kt` (Android) and `Navigator.swift` (iOS) each start or update the `Navigation`, run the `WayGuide`, keep the ridden track and its network, decide when arriving ends navigation, reset the path router when squares changed, and drop a found way that comes too late. Recent ride bugs cluster exactly here ("ignore fixes delivered again", the iOS audio and teardown fixes, "navigate before courses"), and each fix had to be made twice, in two languages, without tests: Android has three small unit tests, iOS none. Small differences have already crept in, e.g. the lock screen's bar shows up to 4 marks on Android and 8 on iOS, ranked by separate code.

## What Changes

- **One navigation session in the core:** a new `NavigationSession` takes each fix and returns what the app has to do: the state to show, the notices to speak, a way to search for, the precision to sample with, and whether to stop now or after the last notice is spoken. It owns the `Navigation`, the `WayGuide`, the `RiddenTrack` and the "stopping" state after arriving.
- **Found ways through the session:** the app hands a found way (or none) back to the session, which decides whether it still matters and whether to show it.
- **One way finder per app:** a core `WayFinder` keeps the path router and forgets its squares when the store's version changed, used by navigation and by the planner's ways for route points, replacing four copies of that check.
- **Lock screen bar marks and turn angles from the core:** the ranking of stops and ferries on the bar (closed ferry, stop, ferry; only ahead of the rider) and the angle of a turn arrow come from the core; each app keeps its own limit of marks (it is a limit of the platform's notification or Live Activity state).
- **The apps keep the platform:** location updates and their precision, speech and audio focus, threads, the notification and the Live Activity, and how they are drawn.
- **Tests in the core** for the existing scenarios the loop covers, so they run for both apps (see tasks).

No behavior is meant to change; the specs stay as they are.

## Out of Scope

- **Network sync** (installing downloads, failures, "needs app update", retries): its own change right after this one (`move-network-sync-to-core`), as it touches other files and has drifted in visible ways that need spec decisions.
- **iOS lock screen text from `HeaderRow.outside()`**: iOS builds its Live Activity text from the display itself; making it use the core's lock screen text changes what iOS shows and needs a look on the iPhone, so it is its own change.
- **The camera follow rules on the map** (`NetworkMap.kt` / `NetworkMapView.swift`): a later change.
- **"Navigate before courses"** (no courses for the whole ride when pressed too early): a behavior change of its own.
- **Bar mark limits:** 4 on Android and 8 on iOS stay as they are.
- **Switching the language while navigating on iOS:** found while writing this change, iOS never passes a new language to a running navigation, unlike Android and `app-languages` "Language setting". The session makes it a one-line call, but it changes what iOS speaks, so it is its own change after this one.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None: a refactoring, the behavior in the specs stays as it is (`skip_specs: true`).

## Impact

- **Core:** new `NavigationSession` and `WayFinder`, `NavigationState` moved from the apps into the core, `ProgressStrip.barMarks()` and `TurnDirection.degrees`; tests in a new `NavigationSessionTest` and `WayFinderTest`.
- **Android:** `NavigationService` feeds fixes and found ways to the session and acts on what it returns; `AppViewModel`'s route point ways use `WayFinder`; the `NavigationState` of `Navigating.kt` goes.
- **iOS:** the same in `Navigator.swift` and `AppModel.swift`; the `NavigationState` struct goes.
- **Risk:** this is the code that runs on every ride, so the change ends with a ride check on both phones.
