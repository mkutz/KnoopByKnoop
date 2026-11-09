# Tasks

## 1. iOS

- [x] 1.1 `NetworkMapView`: the magnifier button below my position and the nearest knots, styled like them, hidden while navigating, with the label "Search places" in `Localizable.xcstrings` in all four languages. The column of buttons ends just above the sheet's top edge in portrait and moves with it. Builds.
- [x] 1.2 `PlannerView` and `PlaceSearchView`: a `searching` state opened by the button; the sheet goes to large with the field focused at its top and the hits below it, on the sheet's background, and in landscape the same in the side panel; picking a hit, clearing or tapping the map closes it and restores the sheet's height. The search overlay over the map goes; the attribution stays above the sheet. Builds.
- [ ] 1.3 Simulator screenshots, light and dark: "Field above the sheet on iOS" at the sheet's smallest and medium height (no button covered), "Sheet pulled up on iOS", "Typing on iOS", "One surface on iOS", "Back above the sheet on iOS", "Searching in landscape on iOS", "Typing in landscape on iOS", "Done typing in landscape on iOS", the offline state of "Search needs internet access", and the largest text size.

## 2. Android

- [x] 2.1 `NetworkMap`: the magnifier button at the top of the column, like the zoom buttons, only while planning, labelled "Search places"; `strings.xml` gets it and Cancel in all four languages. Builds.
- [x] 2.2 `PlannerScreen` and `PlaceSearchField`: a `searching` state opened by the button; the sheet expands with the field focused at its top and the hits below, on the sheet's surface, and in landscape the same in the side panel; picking a hit, Cancel, tapping the map or back closes it and restores the sheet's height. The field leaves the map's top; the sliding landscape panel and `RouteChip` go. The column of buttons stays clear of the sheet. Builds, `./gradlew check`.
- [x] 2.3 Emulator screenshots, light and dark: "Button on the map on Android" at the sheet's smallest and medium height, "Typing on Android", "Back on Android", "Typing in landscape on Android", "No search while navigating", map-planning "Typing in the search in landscape" and "Map controls in landscape", and the offline state of "Search needs internet access".

## 3. Release

- [ ] 3.1 Changelog line, and a manual check on the iPhone: search a place, pick it, and check the map's buttons with the sheet at different heights.
- [ ] 3.2 Changelog line for Android, and a check on the Pixel: open the search from the button, pick a place, close it with back, and see there is no search while navigating.

## Notes

- 2.3, emulator, 2026-11-09: the magnifier at the top of the column, no field on the map, at the sheet's smallest and medium height no button covered; typing "Utrecht" raises the sheet with the field, keyboard and hits; picking "Utrecht Centraal" jumps there with the marker and brings the sheet back to its height; reopening searches the text again; back closes the keyboard, then the search. Landscape: the field and hits at the top of the side panel, the map, the switch and the attribution stay, a tap on the map closes the search without adding a knot. Navigating: no magnifier, no field. Dark mode fine; offline the field is disabled with "Search needs an internet connection". The emulator ran out of memory once, reloading the network after stopping navigation, as noted before in the memory of "Hiking out of memory"; not touched by this change.
