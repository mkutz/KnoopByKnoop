# Tasks

## 1. Core

- [x] 1.1 `RoutePoint.placeKind` and `placeName`, set by `routePointFor` from an offer with a place, saved and read by `RouteSnapshot` as optional fields, `place` on the route knots in `MapFeatures` and on `ProgressMark`. Tests:
  - `NetworkGeometryTest` "Café as a stop" and "Place without a name as the end": the route point carries the kind and name.
  - `PlanningTest` "Start that becomes a stop": the old start keeps them.
  - `PlanningTest`, next to its `RouteSnapshot` and `routeKnotsLayer` tests: "Kept after a restart" (round trip), "Route saved before" (JSON without the fields), and the `place` property on the route knots.
  - "Spot without a place": no kind, no name.
  - `RoutePointNavigationTest` "a place's icon on the strip": the place on the strip's mark.
  `./gradlew :modules:core:jvmTest` passes.

## 2. Android

- [x] 2.1 Route point badges with the place's disc and glyph and the role's corner symbol (`KnotBadges.kt`, image names in `NetworkMap.kt`, the strip), and the role plus name in the waypoint row and its screen reader actions (`PlannerScreen.kt`). Check on the emulator with screenshots: "Café as a stop" (map, list and strip), "Place without a name as the end", "Start that becomes a stop", "Kept after a restart", "Spot without a place", "Places switched off", and the row's TalkBack text for "Screen reader in the list" from the accessibility tree.

## 3. iOS

- [ ] 3.1 The same in `KnotBadges.swift`, `NetworkMapView.swift`, the strip and the waypoint list. The project builds; the maintainer checks "Café as a stop" and "Kept after a restart" on the iPhone.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` lists every scenario with how it was checked, plus steps for the maintainer: make a café a stop and a water point the end, restart, and ride to the café. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. A CHANGELOG entry goes under Unreleased (create the section if missing).

## Workflow follow-up

- Apply after `tap-place-on-map`. Archive `add-route-points`, `highlight-places-on-map` and `tap-place-on-map` before or with this one.
- The name in navigation (header, lock screen, voice) and in the GPX export can follow as their own change.
