## 1. Core: temporary waypoints

- [x] 1.1 Add `temporary: Set<Long>` to `PlanningSession`, kept by edits, and dropped by `clear()` and `start()`. `startOnRouteAt` marks a new route point as temporary. Add `through(newWaypoints, avoiding)`, which plans with temporary stops (and, for the ride only, without the avoided ferries, see design). Verify with `StartChoiceTest` `Starting nearby`, `Nearby at a knot`, `On the course past the start` and `Without courses`, updated to expect the temporary start and the plan's start unchanged in the original session.
- [x] 1.2 Remove `avoidedFerries`, `avoidingFerries` and the `avoidedFerries` parameter of `start` from `PlanningSession`, and `avoidedFerries` from `RouteSnapshot`. Keep the `avoided` parameter of `ShortestPathRouter`. Verify with the test `Routes saved with avoided ferries`: JSON with `"avoidedFerries":[[2,3]]` reads, and its route takes 2 → 3.
- [x] 1.3 `MapFeatures.describe`: temporary waypoints other than the start are ordinary route knots, not stops, and are not in `stopPositions`. Verify with the test `Temporary knots are not stops`.

## 2. Core: the way around as temporary knots

- [x] 2.1 `FerryDetour.of` builds `around` from the plan's waypoints through temporary knots: the middle inner knot of each differing stretch, falling back to all inner knots when the replanned route still takes a closed ferry or is more than 1% longer than the way around. Update `FerryDetourTest` `Ferry closed on the way`, `Riding around it` (new: waypoints kept, temporary 5, no ferry) and `Another ferry on the detour`. Add a test where a temporary knot alone would not hold the way around (`the way around holds where the way back from a temporary knot over the ferry is shorter`).
- [x] 2.2 Rewrite `Editing the plan afterwards`: after riding around, the plan as made with a knot added takes the ferry again, also through `RouteSnapshot`. Add `Started nearby and riding around` (the question for the route from the nearby start; `around` keeps that start). Update `RealFerryDetourTest` for the temporary knots; the length is unchanged.
- [x] 2.3 Update `RealRide.kt`'s start-nearby use if its API changed, and run `./gradlew :modules:core:jvmTest`. Verify that the RealRideTest texts are unchanged.

## 3. Android

- [x] 3.1 `AppViewModel`:
  - Add `plannedSession` and `showForNavigation(session)`, which shows a session without saving it.
  - `startNearby` and `rideAroundFerries` use it.
  - When `Navigating.state` goes to `null`, or a question is dismissed after starting nearby, restore `plannedSession` with its layers and description.
  - Saves during navigation write the plan as made.

  Verify on the emulator (manual check 5.1).

## 4. iOS

- [x] 4.1 `AppModel`: the same as 3.1. Drop `avoidedFerries:` from `PlanningSession.companion.start`. Restore when `Navigator.shared.state` becomes `nil`. Verify that the iOS build compiles and `./gradlew :modules:core:iosSimulatorArm64Test` passes, then check in the simulator (manual check 5.1).

## 5. Checks and docs

- [x] 5.1 Add to `openspec/checked-by-hand.md` how these are checked:
  - New: `Plan restored after stopping`, `Plan restored after arriving`, `App ended while navigating`, `Navigating the plan again` and `Map after the ride`.
  - Reworded: `The plan after the ride`, `Riding around it` and `Map after the answer`.

  Check them on the emulator and in the simulator: start nearby plus ride around on Mondorf–Graurheindorf (14 → 15) in the evening, Stop, plan back, Navigate asks again. Then do the same with the app ended while navigating. Record them in `manual-checks.md` with screenshots.
- [x] 5.2 Add a CHANGELOG Unreleased line ("Starting nearby and riding around a closed ferry count only for that ride"). Update the KDoc of `startNearby`/`rideAroundFerries`, which still say "for good".
- [x] 5.3 Run `./gradlew check`, `npx @fission-ai/openspec validate --all` and `./gradlew checkScenarios -Pchange=navigation-only-route-changes`. All pass.

## Workflow follow-up

- The maintainer checks it on the iPhone (and the Pixel once wireless debugging is on) before archiving.
