## 1. Core

- [x] 1.1 `RouteHistory` in `modules/core`: snapshots before each edit, at most 50, `record`, `canUndo`, `undo(graph)`, JSON. Tests named after the scenarios Undo a tap, Several steps back, Undo a removal, Undo a move, Undo Back to start, Undo Clear, Nothing to undo, Undo after a restart (JSON round trip), Knot gone when undoing, and the limit of 50.

## 2. Android

- [x] 2.1 `NetworkStorage`: `route-history.json` read and written with the route; deleted on a network switch.
- [x] 2.2 `AppViewModel`: `edit(session)` records and updates, used by taps, Start/Stop/End here, remove, move, Back to start and Clear; the note's choices and the courses do not record; `undo()` restores, closes the note, tells about gone knots; `AppState.canUndo`.
- [x] 2.3 `PlannerScreen` action row and `EmptyRoute` show Undo (`ic_undo`, "Undo") when `canUndo` and not navigating; the note's Undo is removed.

## 3. iOS

- [x] 3.1 The same in `NetworkStorage`, `AppModel` (`edit`, `undo`, `canUndo`), `PlannerView` and `EmptyRoute`; the note's Undo is removed.

## 4. Checks

- [x] 4.1 `openspec/checked-by-hand.md` entries for the scenarios checked on devices, CHANGELOG line; `./gradlew check`, validate, `checkScenarios -Pchange=add-undo-button`.
- [x] 4.2 On the emulator and in the simulator: undo a tap, a removal, a Clear, after a restart; Undo in the empty panel; screen reader; 360 dp row. Install on both phones.
