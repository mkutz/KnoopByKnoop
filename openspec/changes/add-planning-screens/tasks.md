# Tasks

## 1. Core: regions

- [ ] 1.1 `RegionCatalog`: region states from the index (or the cached index) and the installed entries, plus display names for the region IDs
- [ ] 1.2 `RegionInstaller`: check downloaded bytes against their index entry and the supported format version
- [ ] 1.3 Tests: "First start", "No network access", "Newer data available", "Corrupted download", "Unsupported file version"

## 2. Core: planning

- [ ] 2.1 `KnotLocator`: nearest knot within a radius in meters, grid index
- [ ] 2.2 `PlanningSession`: tap a knot, move, remove, clear, switch network type, on top of `Route`
- [ ] 2.3 `MapFeatures`: GeoJSON for knots, connections and the route along their course, straight lines without matching geometry, and the summary text
- [ ] 2.4 `RouteSnapshot`: route to JSON and back, reporting waypoints whose knots no longer exist
- [ ] 2.5 Tests: "Without matching geometry", "Start and end", "Adding a stop", "Tapping next to the network", "Knots with equal numbers", "Route summary", "Unreachable leg", "Reorder", "Remove", "Start over", "Switching to hiking", "Restart", "Knot gone after an update", "Remove the planned region"
- [ ] 2.6 A test that `KnotLocator` answers within 5 ms on the size of the Dutch hiking network

## 3. Android

- [ ] 3.1 Add MapLibre Android and the storage of regions, the cached index and the current route
- [ ] 3.2 Downloads with `DownloadManager`, checked by `RegionInstaller` and installed by atomic rename
- [ ] 3.3 Region screen: list, download with progress, update, remove
- [ ] 3.4 Planner screen: map with OpenFreeMap base map, network layers, network switch, taps via `KnotLocator`
- [ ] 3.5 Bottom sheet: summary, waypoint list with drag to reorder, swipe to remove, clear
- [ ] 3.6 Restore the current route on start; open the region list when no region is installed
- [ ] 3.7 Manual check on an emulator, written down in `manual-checks.md` in this change: "Leaving the app during a download", "Interrupted download", "No network access" (map), "Attribution", and a plausibility check of the Dutch hiking network's map performance

## 4. iOS

- [ ] 4.1 Add MapLibre Native via Swift Package Manager in `project.yml`, and the storage of regions, the cached index and the current route
- [ ] 4.2 Downloads with a background `URLSession`, checked by `RegionInstaller` and installed by atomic rename
- [ ] 4.3 Region screen: list, download with progress, update, remove
- [ ] 4.4 Planner screen: map with OpenFreeMap base map, network layers, network switch, taps via `KnotLocator`
- [ ] 4.5 Sheet: summary, waypoint list with drag to reorder, swipe to remove, clear
- [ ] 4.6 Restore the current route on start; open the region list when no region is installed
- [ ] 4.7 Manual check in the simulator, same list as 3.7, also in `manual-checks.md`

## 5. Wrap-up

- [ ] 5.1 `./gradlew check` and the iOS app build pass locally and in CI
- [ ] 5.2 README: what the apps can do now, and the map attribution
