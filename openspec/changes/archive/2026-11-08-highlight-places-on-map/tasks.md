# Tasks

## 1. Groundwork

- [x] 1.1 Read a real OpenFreeMap tile at zoom 14 over Troisdorf and one over Utrecht, and list the `class`/`subclass` pairs of the `poi` layer for the five kinds, including whether `bicycle_repair_station`, `halt` and `biergarten` appear. Record the list in `design.md` under "As built" and correct the kind table there if needed.
- [x] 1.2 A mockup artifact with the five icons on the light and the dark map, next to NL, BE and NRW knots, a route and a busy town centre at z14, z15 and z16. The maintainer picks the colours before code; the chosen colours go into `design.md`.

## 2. Core

- [x] 2.1 `PlaceKind` with the subclass matches, colours and keys, and the JSON for `placesFilter`, `placesIcon` and `baseMapPoiFilter`. Tests in `PlaceKindTest`:
  - "Places in a town": a feature of each kind matches its kind (restaurant, supermarket, bicycle, station, drinking water, toilets).
  - "Other places unchanged": pharmacy, bank, bicycle rental and a bus station match no kind.
  - "Shown once": `baseMapPoiFilter` keeps the original condition and excludes every kind's subclasses.
  - "Distinct from knots": no kind's colour equals or is near a `KnotPlate` colour or the app green (a colour distance check), and the white glyph's contrast is at least 3:1 on each.
- [x] 2.2 A test reads `map-styles/liberty-dark.json` and checks that `poi_r1`, `poi_r7`, `poi_r20` and `poi_transit` exist and use the `openmaptiles` source's `poi` layer ("Shown once" on the dark map). `./gradlew :modules:core:jvmTest` passes.

## 3. Android

- [x] 3.1 The place icons drawn by the badge code from Material Symbols drawables, the `places` layer from z14 with names from z16 below `connections`, and the base map's POI filters changed and restored, in `NetworkMap.kt`. Check on the emulator with screenshots for: "Places in a town", "Names close up", "Zoomed out", "Shown once", "Other places unchanged", "Knot next to a café", "Under the route", "Places on the dark map", "Places while navigating", "Places on a hiking map" and "Without the base map" (offline on a fresh install).
- [x] 3.2 The "Places on the map" switch in `InfoSheet.kt`, stored in the settings. Check on the emulator: "Turning the places off" (screenshot before and after), "Choice kept for places" (restart), "Switch by default" (fresh install). A `AppViewModel` unit test for the stored default and choice, if the existing tests reach the settings.

## 4. iOS

- [x] 4.1 The same in `NetworkMapView.swift` with SF Symbols, and the switch in `InfoSheet.swift` with `@AppStorage("places")`. The project builds, and simulator screenshots cover the same scenarios as 3.1 and 3.2.

## 5. Wrap-up

- [x] 5.1 `manual-checks.md` lists every scenario with how it was checked, plus ride steps for the maintainer: a town centre while planning and navigating, light and dark, the switch off and on, and whether the icons crowd out street names in a busy centre. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. A CHANGELOG entry goes under Unreleased (create the section if missing).

## Workflow follow-up

- Independent of the sign changes; can be applied before or after their archive.
- If riders use the places, our own place data along the route (opening hours, "next supermarket in 14 km", tapping a place to make it a route point) can be its own change.
