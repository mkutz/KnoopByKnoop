## 1. Core

- [x] 1.1 Let `PlanningSession.tap` take a route point as the second waypoint (through `planned`). Add `PlanningSession.placed(point)` (knot placement with the route point's graph) and `TapOptions.forPoint(session, point)` with `suggested`. Tests in `PlanningTest`: `Suggestion beside the route`, `Suggestion beyond the end`, `Offer as previews` (the previews and the sessions), `Offer without a route`, `First waypoint` and `End before the start` ("Add to route" via `tap`: start, then end). Run `./gradlew :modules:core:jvmTest`.

## 2. Android

- [x] 2.1 `AppViewModel` works out the options off the main thread when an offer opens. `PlaceOfferMenu` shows the three buttons with `KnotPreviewRow`, the suggestion ringed, and keeps the name, details and dismissal. Screen reader names stay "Start here", "Add stop here" and "End here", plus "suggested". Without a route, the card shows "Add to route" (all four languages). Check on the emulator: a spot beside the route, a spot beyond the end, and a café without a route, added as the start, then another as the end.

## 3. iOS

- [x] 3.1 The same in `AppModel` and `PlaceOfferMenu`. Check in the simulator.

## 4. Checks

- [x] 4.1 Add the card scenarios to `openspec/checked-by-hand.md` where no unit test can check them (`Name of a café`). Add a CHANGELOG Unreleased line. Run `./gradlew check`, `npx @fission-ai/openspec validate --all` and `./gradlew checkScenarios -Pchange=places-like-tapped-knots`.

## Workflow follow-up

- Install on both phones for the maintainer's check, then archive.
