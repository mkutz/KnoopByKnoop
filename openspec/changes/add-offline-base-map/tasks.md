# Tasks

## 1. Core

- [x] 1.1 `BaseMapArea.of(course)`: the detail polygon (500 m around the course, zoom 10–14) and the overview (bounds plus 5 km, zoom 5–9), with a tile estimate per zoom. Zoom 14, then 13, are left out while the total exceeds 3,000 tiles. Tests in a new `BaseMapAreaTest`:
  - "Route planned at home": a 50 km course gives both regions and fewer than 3,000 tiles.
  - "A long route": a course of several hundred kilometres drops zoom 14 and stays within 3,000 tiles.
  - "Starting away from the route": the area depends only on the course, so the same course gives the same area.
- [x] 1.2 `BaseMapSquares.courseToLoad(course, deletedFor)`, as for the place details. Tests:
  - "A hiking route": a hiking course is loaded.
  - "Deleting the base map with a route planned": the deleted-for course gives `null`, a changed course gives it.
- [x] 1.3 The base map's state reuses `PathDataStatus` (loading, ready, unavailable). Its panel line is in the apps' strings in four languages, as the ways' line is. "Map status in all languages" is checked on the devices.

## 2. Android

- [x] 2.1 Give MapLibre an OkHttp client with the app's User-Agent and 4 requests per host, set before the first map. Set the offline tile count limit to 3,000.
- [x] 2.2 `BaseMapStore` around `OfflineManager`:
  - Create the detail and overview regions of `BaseMapArea` with the light style URL and the route's course in their metadata.
  - Report progress as `BaseMapStatus`, and stop the detail region above 50 MB.
  - Delete other courses' regions once the new one is complete. Provide the size and `clear()`.
- [x] 2.3 `AppViewModel.loadBaseMap()` in the flow of `loadPathData`: 2 seconds after the route last changed, again with network access coming back and at navigation start. The status goes to the panel line, and the size and delete action to the info sheet entry, with its explanation and the delete button's TalkBack label. Strings in four languages.
- [ ] 2.4 Check on the emulator, with screenshots in the change folder:
  - "Route planned at home", then "Riding without a connection": airplane mode, the app restarted, and the map shown along the route, light and dark.
  - "Changing the route", "A new route" and "Restarting the app".
  - "Requests name the app" and "Requests at a time": logged by an OkHttp interceptor in a debug run.
  - "Map still loading", "Map loaded" and "Map not available" (airplane mode before planning).
  - "Opening the info sheet with a base map", "No base map stored", "Deleting the base map", "Base map delete button with a screen reader" (TalkBack) and "Explaining the base map" in four languages.
  - "Without network access" and "Starting before the download is done".

  List the scenarios a unit test can't check in `openspec/checked-by-hand.md`.

## 3. iOS

- [x] 3.1 `MLNNetworkConfiguration` with a session configuration sending the app's User-Agent and allowing 4 connections per host. Set the offline tile limit to 3,000 with `MLNOfflineStorage.shared.setMaximumAllowedMapboxTiles`.
- [x] 3.2 `BaseMapStore` around `MLNOfflineStorage` with `MLNShapeOfflineRegion`, as 2.2. `AppModel` loads it as for path data. The panel line and the info sheet entry with VoiceOver label, and the strings in the String Catalog.
- [ ] 3.3 The project builds. The checks on the iPhone are the maintainer's: planning at home, then a ride in airplane mode with the map along the route, light and dark, and the info sheet entry.

## 4. Wrap-up

- [x] 4.1 A changelog line under Unreleased: "The map around a planned route is stored on the phone, so streets, villages and rivers are shown along it without a connection too; the info sheet shows its size and can delete it".
- [x] 4.2 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. Run `./gradlew checkScenarios -Pchange=add-offline-base-map`.
