## 1. Android

- [x] 1.1 `startNavigation` clears `offer`, `addingAt` and `marker`, and a pending "Show my position" or "Nearest knots" does not move the map once Navigate was chosen (check before the move; `locating`/`findingNearest` cleared).
- [x] 1.2 `PlannerScreen` closes the search when Navigate is chosen, not only once navigation runs.

## 2. iOS

- [x] 2.1 The same in `AppModel.startNavigation`: `offer`, `addingAt`, `marker`, and the pending moves of `locate` and `showNearest`.
- [x] 2.2 `PlannerView` closes the search when Navigate is chosen.

## 3. Checks

- [x] 3.1 Entries in `openspec/checked-by-hand.md` for the four scenarios, CHANGELOG line; `./gradlew check`, validate, `checkScenarios -Pchange=close-planning-tools-on-navigate`.
- [x] 3.2 On the emulator or simulator: place card, spot being added, search hit, nearest knots, each then Navigate. Install on both phones for the maintainer.
