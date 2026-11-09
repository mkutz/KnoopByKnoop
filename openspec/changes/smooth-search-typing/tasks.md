# Tasks

## 1. iOS

- [ ] 1.1 `PlaceSearchModel` with the query, the result, the request, `search`, `searchAgain` and `clear`, taking the locator, area and map centre from `AppModel` when a request starts; `AppModel` owns it without publishing it and keeps `choose`, the marker and `showingHit`. `PlaceSearchView` observes it. Builds.
- [ ] 1.2 `DebugTiming` notes stalls over 50 ms. In the simulator, a search typed and picked as before ("Typing on iOS", "Back above the sheet on iOS"), and a breakpoint-free check that `PlannerView`'s body is not evaluated per character ("The map while typing"), e.g. with `Self._printChanges()` in a debug build.

## 2. Release

- [ ] 2.1 Changelog line, and a check on the iPhone: type "Amsterdam" letter by letter, then read the timing log for "Typing a place's name": no stall over 50 ms between the keystrokes.
