# Tasks

## 1. Core

- [x] 1.1 `RouteDescription.StartOnly` gets `waypoint: Knot`; `MapFeatures.describe` fills it (the start, or the end when `endFirst`). Tests in `PlanningTest`/`MapFeaturesTest`: `Start set` (the waypoint is 48, not end-first), `End chosen first` (the waypoint is the end, `endFirst`), `Route point in a slot` (a route point waypoint with `routePoint`).

## 2. Android

- [x] 2.1 `EmptyRoute` composable in `PlannerScreen.kt`: title, slot row (dashed empty, pulsing next unless animations are off, badge when set), labels with the place, hint, and a `FlowRow` with "Search a place" and "Nearest knots", or Clear once a waypoint is set. It replaces the non-complete `Text` and the nearest knots `ProminentAction` in `WaypointSheet`. Hide `RouteActions` while there is no complete route. Same in landscape.
- [x] 2.2 Strings in values, de, nl, fr; remove the four old hint strings. Check on the emulator: nothing planned, start set, end first, a spot as the start, no knots in view, TalkBack, largest font, animations off, landscape, dark.

## 3. iOS

- [x] 3.1 `EmptyRoute` view in `PlannerView.swift` with the same parts; the halo off with `accessibilityReduceMotion`; one accessibility element per slot. It replaces the non-complete `summary` cases and the nearest knots `ProminentAction` in `mainAction`. `routeActions` only with a complete route.
- [x] 3.2 `Localizable.xcstrings` entries in de, nl, fr; remove the old hint keys. Check in the simulator: the same states as on Android, VoiceOver, the largest text size, Reduce Motion, landscape, dark.

## 4. Checks

- [x] 4.1 Add the scenarios no unit test checks to `openspec/checked-by-hand.md`: `Nothing planned`, `Next slot without motion`, `Search from the panel`, `Nearest knots from the panel`, `Into the route`, `Slots with a screen reader`, `Empty panel at large text sizes`. Update the entries for `No knots in the area` and `Knots in view` if listed. Add a CHANGELOG Unreleased line.
- [x] 4.2 Run `./gradlew check`, `npx @fission-ai/openspec validate --all` and `./gradlew checkScenarios -Pchange=redesign-empty-planner-panel`. Install on both phones for the maintainer's look.
