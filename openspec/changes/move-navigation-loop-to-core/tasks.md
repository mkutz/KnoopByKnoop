# Tasks

## 1. Core

- [ ] 1.1 `NavigationRoute` and `NavigationState` in the core; `NavigationSession` with `onFix`, `onWay`, `inLanguage` and `SessionStep` as in the design. Tests in a new `NavigationSessionTest` on the straight test network, covering the existing scenarios:
  - `knot-navigation` "Arriving": the step after passing the end has "You've arrived" and `AFTER_SPEECH`; later fixes and ways give empty steps.
  - "Starting away from the route": the first fix off route gives a `WaySearch`; `onWay` with a way gives a state with the guided way.
  - "Path data arrives on the way": `onWay(null)`, then a fix after the store's squares changed gives a new search.
  - A way arriving after the rider is back on route, or after arriving, gives no state (the stale-result guard of both apps).
  - A fix not newer than the last gives no notices again ("ignore fixes delivered again").
  - "Ridden track": the track takes the route's network and the fix.
  - A first fix where `Navigation.start` gives `null` gives `NOW`.
  `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` passes.
- [ ] 1.2 `WayFinder` with the version check, and `RoutePointWays` through it. Tests in `WayFinderTest`: a store whose version changes makes the finder read squares again; the same version keeps them. Tests pass on JVM and iOS simulator.
- [ ] 1.3 `ProgressStrip.barMarks(limit)` and `TurnDirection.degrees`. Tests: ranking closed ferry, stop, ferry; marks behind the rider left out; the limit; every turn's angle as the apps have them today.

## 2. Android

- [ ] 2.1 `NavigationService` on `NavigationSession` and `WayFinder`; `AppViewModel`'s route point ways on `WayFinder`; bar marks and turn angles from the core; `NavigationState` of `Navigating.kt` removed. `./gradlew check` and `:apps:android:assembleDebug` pass. Emulator: a mocked ride along route 06 → 08 with a detour off route and arriving, notices and lock screen compared with the build before (logcat of notices, screenshots).

## 3. iOS

- [ ] 3.1 `Navigator.swift` on `NavigationSession` and `WayFinder`; `AppModel.findWays` on `WayFinder`; bar marks and turn angles from the core; the `NavigationState` struct removed. The app builds (`xcodebuild` as in the Build workflow). Simulator: the same mocked ride with `simctl location` and a throwaway UI test to start navigation, notices from the log compared with the build before.

## 4. Wrap-up

- [ ] 4.1 `manual-checks.md` with every check above and the ride steps for the maintainer: a ride on each phone with a wrong turn, a way back, and arriving. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. No CHANGELOG entry unless the ride finds a difference, as nothing is meant to change for riders.

## Workflow follow-up

- Archive after the maintainer's ride on both phones.
- Then `move-network-sync-to-core`.
