## 1. iOS layout

- [x] 1.1 Report the sheet's height from the sheet's content with `onGeometryChange` into a `sheetHeight` state of `PlannerView`
- [x] 1.2 Move `PlaceSearchView` from the top `VStack` of the map into a bottom-aligned overlay of the map, right above the sheet in portrait and at the bottom of the map in landscape; keep the network picker and the info button at the top
- [x] 1.3 Show the hits above the field instead of below it, with their height limited to the room up to the network picker
- [x] 1.4 While the field has the focus, place it above the keyboard instead of above the sheet; choosing a hit or clearing the search removes the focus
- [x] 1.5 Hide the field while the sheet is higher than its medium height, keeping the query and the hits
- [x] 1.6 Move the attribution above the field, and the loading and courses notes up to just below the network picker

## 2. Liquid Glass

- [x] 2.1 Add a `floatingBackground(in:)` view modifier: `glassEffect(.regular, in:)` on iOS 26 and later, `.regularMaterial` before
- [x] 2.2 Use it for the search field, the hits, the network picker with the info button, `LoadingNote` and the attribution, instead of their current backgrounds
- [x] 2.3 Make sure the portrait sheet sets no background of its own, so iOS draws it as glass

## 3. Checks

- [x] 3.1 The iOS app builds for the simulator, and `./gradlew check` passes
- [x] 3.2 CI passes
- [ ] 3.3 Manual checks in `manual-checks.md`, on the iPhone by the maintainer: "Field above the sheet on iOS", "Sheet pulled up on iOS", "Typing on iOS", "Back above the sheet on iOS", "Searching in landscape on iOS", and "Station", "No hits", "Tapping a hit", "Removing the marker", "Airplane mode" again in the new place, and Liquid Glass on the map's floating views, readable over a busy map (the scenarios are UI behavior with no logic in the core, so they are covered by these checks instead of automated tests); Android rechecked to be unchanged
