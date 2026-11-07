# Tasks

## 1. Core

- [ ] 1.1 Add `SignRegion` and `Knot.signRegion`, and append the region section to `GraphFile` after the merged knots, still at version 2. Tests in `GraphFileTest`:
  - "Graph files without sign regions": a file written without the section reads every knot with `null`.
  - A round trip keeps the regions of NL, BE and NRW knots, and `null` for the others.
  - "Apps without sign regions": the bytes up to the end of the ferries section are unchanged, so an older reader reads the file as before.
- [ ] 1.2 Add `KnotPlate.of(region)` with the four plates. Tests in a new `KnotPlateTest`: NRW red with inner ring, NL dark green without, BE blue with, `null` app green with.
- [ ] 1.3 `MapFeatures`: a `plate` property (`nl`, `be`, `nrw`, `none`) on the route marker points, and none on the network knots. Tests in `PlanningTest`:
  - "Route across the border": a route from a NL knot to a BE knot gives `nl` on the start and `be` on the end.
  - "Data from before sign regions": knots without a region give `none`.
  - Round trip: the single marker keeps its plate.

## 2. Pipeline

- [ ] 2.1 `NetworkExtraction`: collect the NL, BE and DE-NW boundary relations with their outer member ways and nodes, build closed rings, and fail with the boundary's name when a ring does not close. Test in `NetworkExtractionTest` "Incomplete border": a boundary with a missing way fails the build.
- [ ] 2.2 Assign each cycling knot's region by point in polygon, and none for hiking knots. Tests in `NetworkExtractionTest` with small square boundaries built with `PbfWriter`:
  - "Knot in Limburg", "Knot in Flanders" and "Knot in North Rhine-Westphalia": one knot inside each.
  - "Knot in another German state": a knot outside all three.
  - "Knot near a border": a knot in both extracts, on the BE side.
  - "Hiking knots": no region.

  Then run `pipeline build` on the three real extracts and check: Roermond 95 is NL, Haspengouw 150 is BE, Lünen 60 and Troisdorf 25 are NRW. Also record the build time and the cycling graph size against before, still under 5 MB.

## 3. Android

- [ ] 3.1 `KnotBadges.kt`: draw the plate, the fill by role, the inner ring, the grey knots not on the route (light and dark), and the corner symbols. Use the new image names, and bump the layer version of the cached knots file. Check on the emulator with the newly built data, with screenshots in `screenshots/`:
  - "Route in North Rhine-Westphalia" (Troisdorf 25 → 32 → 29)
  - "Route in the Netherlands" (Roermond)
  - "Route in Belgium" (Haspengouw)
  - "Route across the border"
  - "Knots not on the route"
  - "Knot without a region" (hiking)
  - "Start, stop and end"
  - "Round trip on the map"
  - "Knots on the dark map"
  - the waypoint list showing the same badges
- [ ] 3.2 `NetworkMap.kt`: the way-to-route line, the guided way, the unreachable leg and the search-hit marker change from red to dark, in light and dark mode. Screenshot check of "Off route on the map" with a simulated off-route position on the emulator.
- [ ] 3.3 Panel: off route, the header area turns dark (light in dark mode) and the strip dot becomes a ring with ✕ (`ProgressPanel.kt`). A ferry warning gets the clock badge and a bold arrival (`ProgressPanel.kt`, `RouteFerries.kt`). Extend `FerryArrivalClockTest` for the arrival's style per state. Check on the emulator: "Off route in light mode", "Off route in dark mode", "Off route on the strip", "Warning in the ferry list" and "Closed at the renewed estimate", with a ferry route and a time set past its closing.

## 4. iOS

- [ ] 4.1 `KnotBadges.swift`: a new `PlateBadge` with the same plates, fills, corner symbols and image names. `Shared/Badges.swift` stays as it is for the widget. The project builds, and simulator screenshots show the same scenarios as 3.1.
- [ ] 4.2 `NetworkMapView.swift`: the same line and marker colours as 3.2. Checked by a simulator screenshot while off route.
- [ ] 4.3 `PlannerView.swift`, `ProgressPanel.swift`, `RouteFerries.swift`: the same panel changes as 3.3. Simulator screenshots of the same scenarios, and the Live Activity unchanged.

## 5. Wrap-up

- [ ] 5.1 `manual-checks.md` lists every scenario of the deltas with how it was checked, plus the maintainer's ride steps: badges match the signs at Troisdorf, Siegburg and a Dutch knot, and an off-route moment shows the dark header. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. A CHANGELOG entry goes under Unreleased (create the section if missing), saying that route roles are now shown by fill and corner symbol.

## Workflow follow-up

- The regions reach the phones with the next data publication after the commit, or a manual run of Update Network Data.
- The maintainer checks it on the Pixel and the iPhone before `/opsx:archive knot-colours-from-signs`.
- Then `sign-like-header` builds on the dark off-route header and the clock badge.
