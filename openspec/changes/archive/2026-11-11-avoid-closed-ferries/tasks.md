# Tasks

## 1. Core

- [x] 1.1 `ShortestPathRouter` with avoided connections, `PlanningSession.avoidedFerries` and `avoidingFerries`, and `RouteSnapshot.avoidedFerries` (absent in older snapshots). Tests:
  - "Editing the plan afterwards" (a knot added still avoids the ferry; a snapshot round trip keeps it)
  - an older snapshot without the field reads as before
- [x] 1.2 `FerryDetour.of`: arrivals from the rider's position with `FerryWatch`, the detour with repeated leaving out, the extra length and time, the line and bounds. Tests on a synthetic network with a ferry and a bridge:
  - "Ferry closed on the way"
  - "Ferry open when the rider gets there"
  - "Another ferry on the detour"
  - "No way around"
  - "Ferry without operating times"
- [x] 1.3 A test on the real cut-out `rhine-luelsdorf` with the ferry Lülsdorf–Wesseling: a start in the evening gives the question with a detour over a bridge, and its length is plausible.
  - Done 2026-11-11 (`RealFerryDetourTest`). Within the cut-out there is no bridge; the way around crosses on the ferry Mondorf–Graurheindorf (open until 21:00 in summer): 21 km and 1 h 15 min longer on a June Wednesday at 18:50. At 18:50 on Wed 2026-11-11 the operating times of Lülsdorf–Wesseling (with Easter-based ranges) gave no status, so neither the ferry warning nor the question would come then. At 21:55 the same evening they did; not investigated further.

## 2. Android

- [x] 2.1 `AppViewModel`: the check after the start choice, and the state for the question and the map. "Ride around it" sets the plan with `avoidingFerries`, saves it and navigates; "Keep the ferry" navigates. Strings in four languages.
- [x] 2.2 The dialog, and the map's detour line and fit to the bounds.
- [x] 2.3 Check on the emulator with the ferry Lülsdorf–Wesseling at a time when it's closed (the emulator's time zone and clock set accordingly), with screenshots:
  - "Ferry closed on the way", "Map while asking"
  - "Riding around it", "Map after the answer"
  - "Keeping the ferry"
  - "Question in all languages" (German)
  - Done 2026-11-11 with the ferry Mondorf–Graurheindorf instead, closed at that hour, without changing the clock; Lülsdorf–Wesseling gave no status at 18:50 (see 1.3). See `manual-checks.md`.

## 3. iOS

- [x] 3.1 The same in `AppModel`, `PlannerView` (a `confirmationDialog`) and `NetworkMapView` (the line and the bounds). Strings in the String Catalog. The project builds; a simulator check of the question and the map.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the device scenarios and the maintainer's steps (a ride planned over a ferry in the evening). A CHANGELOG line under Unreleased. List the scenarios a unit test can't check in `openspec/checked-by-hand.md`. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; then run `./gradlew checkScenarios -Pchange=avoid-closed-ferries`.
