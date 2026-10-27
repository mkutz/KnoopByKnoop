# Tasks

## 1. Core: regions

- [x] 1.1 `RegionCatalog`: region states from the index (none when offline) and the installed entries, plus display names for the region IDs
- [x] 1.2 `RegionInstaller`: check downloaded bytes against their index entry and the supported format version
- [x] 1.3 Tests: "First start", "No network access", "Newer data available", "Corrupted download", "Unsupported file version"

## 2. Core: planning

- [x] 2.1 `KnotLocator`: nearest knot within a radius in meters, grid index
- [x] 2.2 `PlanningSession`: tap a knot, move, remove, clear, switch network type, on top of `Route`
- [x] 2.3 `MapFeatures`: GeoJSON for knots, connections and the route along their course, straight lines without matching geometry, and the summary text
- [x] 2.4 `RouteSnapshot`: route to JSON and back, reporting waypoints whose knots no longer exist
- [x] 2.5 Tests: "Without matching geometry", "Start and end", "Adding a stop", "Tapping next to the network", "Knots with equal numbers", "Route summary", "Unreachable leg", "Reorder", "Remove", "Start over", "Switching to hiking", "Restart", "Knot gone after an update", "Remove the planned region"
- [x] 2.6 A test that `KnotLocator` answers within 5 ms on the size of the Dutch hiking network

## 3. Android

- [x] 3.1 Add MapLibre Android and the storage of regions and the current route
- [x] 3.2 Downloads with `DownloadManager`, checked by `RegionInstaller` and installed by atomic rename
- [x] 3.3 Region screen: list, download with progress, update, remove
- [x] 3.4 Planner screen: map with OpenFreeMap base map, network layers, network switch, taps via `KnotLocator`
- [x] 3.5 Bottom sheet: summary, waypoint list with drag to reorder, swipe to remove, clear
- [x] 3.6 Restore the current route on start; open the region list when no region is installed
- [x] 3.7 Manual check on an emulator, written down in `manual-checks.md` in this change: "Leaving the app during a download", "Interrupted download", "No network access" (map), "Attribution", and a plausibility check of the Dutch hiking network's map performance

## 4. iOS

- [x] 4.1 Add MapLibre Native via Swift Package Manager in `project.yml`, and the storage of regions and the current route
- [x] 4.2 Downloads with a background `URLSession`, checked by `RegionInstaller` and installed by atomic rename
- [x] 4.3 Region screen: list, download with progress, update, remove
- [x] 4.4 Planner screen: map with OpenFreeMap base map, network layers, network switch, taps via `KnotLocator`
- [x] 4.5 Sheet: summary, waypoint list with drag to reorder, swipe to remove, clear
- [x] 4.6 Restore the current route on start; open the region list when no region is installed
- [ ] 4.7 Manual check in the simulator, same list as 3.7, also in `manual-checks.md`

## 5. Wrap-up

- [ ] 5.1 `./gradlew check` and the iOS app build pass locally and in CI
- [x] 5.2 README: what the apps can do now, and the map attribution
