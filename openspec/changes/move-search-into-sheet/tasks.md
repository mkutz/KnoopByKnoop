## 1. iOS layout

- [ ] 1.1 Split `PlaceSearchView` into `PlaceSearchField` (field, clear button, offline text) and `PlaceSearchResults` (searching, hits as a plain `List`, nothing found, unavailable), with the focus passed in as a `FocusState` binding
- [ ] 1.2 Remove the field from the map's top `VStack` in `PlannerView`; keep the network picker and the info button there
- [ ] 1.3 Put `PlaceSearchField` at the top of `panel` (hidden while the info is shown), and show `PlaceSearchResults` instead of `WaypointSheet` while `searchResult` is not `.none`
- [ ] 1.4 In portrait, expand the sheet to `.large` when the field gets the focus, and lower it to the smallest detent when a hit is chosen or the search is cleared
- [ ] 1.5 Raise the smallest detent to `.height(220)` and adjust the attribution's bottom padding and the notes' top padding

## 2. Checks

- [ ] 2.1 The iOS app builds for the simulator, and `./gradlew check` passes
- [ ] 2.2 CI passes
- [ ] 2.3 Manual checks in `manual-checks.md`, on the iPhone by the maintainer: "Searching from the sheet on iOS", "Back to the waypoints on iOS", "Searching in landscape on iOS", and "Station", "No hits", "Tapping a hit", "Removing the marker", "Airplane mode" again in the new place (the scenarios are UI behavior with no logic in the core, so they are covered by these checks instead of automated tests); Android rechecked to be unchanged
