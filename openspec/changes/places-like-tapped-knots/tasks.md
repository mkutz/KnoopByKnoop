## 1. Core

- [x] 1.1 Let `PlanningSession.tap` take a route point as the second waypoint (through `planned`). Add `PlanningSession.placed(point)`.
- [x] 1.2 Remove `TapOptions.forPoint` and `suggested`; make sure `TapOptions.of` works for route points. Tests in `PlanningTest`: `Spot beside the route`, `Spot beyond the end`, `Offer without a route`, `First waypoint`, `End before the start`, and the note's options for a spot. Run `./gradlew :modules:core:jvmTest`.

## 2. Android

- [x] 2.1 A tap on an empty spot adds it at once off the main thread, with a spinner at the spot, then the note. The place card shows "Add to route" (or nothing more, far away or while navigating), which adds the same way. The note's line names a place, or says "Added as …" for a spot, in all four languages. Remove the card's buttons and spinner. Check on the emulator.

## 3. iOS

- [x] 3.1 The same in `AppModel` and `PlannerView`. Check in the simulator.

## 4. Checks

- [x] 4.1 Update `openspec/checked-by-hand.md` (card and note scenarios) and the CHANGELOG line. Run `./gradlew check`, `npx @fission-ai/openspec validate --all` and `./gradlew checkScenarios -Pchange=places-like-tapped-knots`. Install on both phones.

## Workflow follow-up

- The maintainer tries it on the phones, then archive.
