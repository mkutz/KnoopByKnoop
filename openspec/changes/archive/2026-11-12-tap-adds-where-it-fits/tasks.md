## 1. Core

- [x] 1.1 Add `Route.insertWhereItFits(knot)`: the legs plus the place after the end (not for a round trip), least added distance, the stop winning ties, and before the end when nothing is reachable. It reports whether the knot became a stop or the end. `PlanningSession.tap(knot)` uses it once a route exists, and `NewKnots` goes. Tests in `PlanningTest`: `Adding a stop`, `Beyond the end`, `Extending step by step`, `Knot beyond the end of a round trip`, `Tapping a knot that cannot be reached`.
- [x] 1.2 Update the other tests that pass `NewKnots` (`FerryDetourTest`, `RoutePointWaysTest`, `NetworkGeometryTest`, `FerryTest`, `NetworkSyncTest`). Remove the tests of the removed requirement's scenarios. Add tests for `Make it the start`, `Make it the end` and `Undo the tap` on the session level (`startAt`/`endAt` from the session before the tap). Run `./gradlew :modules:core:jvmTest`.

## 2. Android

- [x] 2.1 Remove `NewKnotsSwitch`, `chooseNewKnots`, `KEY_NEW_KNOTS`, `AppState.newKnots` and the `add_knots`/Between/At end strings and icons. `AppViewModel` keeps the session before the tap and the tapped knot for the note, and provides `makeTappedStart`, `makeTappedEnd` and `undoTap`.
- [x] 2.2 Add the note card above the panel: the message and the choices in values, de, nl and fr. It goes away after 6 s, on a choice or with the next tap, and stays while TalkBack is on (live region). Check on the emulator: tap beside the route, beyond the end, Make it the start/end, Undo.

## 3. iOS

- [x] 3.1 Remove `newKnotsSwitch`, `NewKnotsIcon`, `AppModel.newKnots` and its strings. `AppModel` keeps the session before the tap and offers the three choices.
- [x] 3.2 Add the glass note above the sheet, with strings in `Localizable.xcstrings` (de, nl, fr). It goes away after 6 s, on a choice or with the next tap, stays while VoiceOver is on, and is announced. Check in the simulator.

## 4. Checks

- [x] 4.1 In `openspec/checked-by-hand.md`, remove the entries that belonged only to "What new knots become". Add the note's scenarios (`Note after a tap`, `Note for a new end`, `Note goes away`, `Note with a screen reader`, `No note for the first two taps`) with how they're checked.
- [x] 4.2 Add a CHANGELOG Unreleased line. Run `./gradlew check`, `npx @fission-ai/openspec validate --all` and `./gradlew checkScenarios -Pchange=tap-adds-where-it-fits`.

## 5. The note as a preview (after the first trial)

- [x] 5.1 Core: add `PlanningSession.stopAt(knot)` and `TapOptions.of(before, after, knot)`, with a preview window of up to three route knots and "…" flags, and the choices for the other roles. Tests: `Note after a tap` and `Note for a new end` (previews and choices), and `Make it a stop`.
- [x] 5.2 Android: the note shows the preview with `KnotBadge`, the highlighted knot popping in, a button per choice with its preview, and an Undo icon. Content descriptions are in all four languages. Check on the emulator.
- [x] 5.3 iOS: the same with `KnotBadge`, and the strings in `Localizable.xcstrings`. Check in the simulator.
- [x] 5.4 Update `checked-by-hand.md` and `manual-checks.md`. Run `./gradlew check`, `openspec validate --all` and `checkScenarios -Pchange`. Install on both phones for the second trial.

## 6. One line and three buttons (after the second trial)

- [x] 6.1 Core: `TapOptions.choices` holds start, stop and end in this order, each with `current`. Start and end preview two badges (the tapped knot with the current start or end), and stop the window from its route. Update the tests `Note after a tap`, `Note for a new end` and `Make it a stop`.
- [x] 6.2 Android: one line of text, the three buttons in a row (the current one selected and inactive), and Undo. Strings in all four languages. Check on the emulator.
- [x] 6.3 iOS: the same. Check in the simulator.
- [x] 6.4 Run the checks and install on both phones for the third trial.

## 7. The start as a candidate too (after the third trial)

- [x] 7.1 Core: `Route.insertWhereItFits` also weighs the place before the start (not for a round trip), with the stop, then the end, winning ties. Tests: `Before the start` and `Note for a new start`, and the round trip neither starting nor ending.
- [x] 7.2 Apps: the line "60 added as the start" on Android (all four languages) and iOS. Run the checks and install on both phones.

## Workflow follow-up

- Install on both phones for the maintainer's "how it feels" trial. Archive only if it feels right, otherwise revert.
- On archiving, remove the `checked-by-hand.md` lines marked "replaced by `tap-adds-where-it-fits`". On reverting, put back their earlier "on the devices" entries.
