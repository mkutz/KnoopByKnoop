# Tasks

## 1. Core

- [x] 1.1 `MapPlace(position, name, kind, subclass)`, `MapTap.Offer.place` and `MapTap.PlaceOnly`, and `PlanningSession.tapAt(…, place)` with the priority of the design; a route point from an offer with a place uses the place's position. Tests in `NetworkGeometryTest`, next to the other tap tests and their network:
  - "Name of a café" and "Place without a name": the offer carries the place.
  - "Stop at a place" and "Place next to a course": the route point is at the place's position, also when a course is within the tolerance.
  - "Knot before a place": `OnKnot`.
  - "Place far from the network": `PlaceOnly`.
  - "Grey places unchanged": without a place, the same `Offer` as before.
  `./gradlew :modules:core:jvmTest` passes.

## 2. Android

- [x] 2.1 The place under the tap from `places` and `places-named` in `NetworkMap.kt`, passed through `onTap` to `AppViewModel.tap`; the header with icon and name in `PlaceOfferMenu`; a name-only card while navigating; the kind names in four languages. Check on the emulator with screenshots: "Name of a café", "Place without a name", "Stop at a place", "Place next to a course", "Knot before a place", "Place while navigating", "Closing the card" and "Grey places unchanged".

## 3. iOS

- [x] 3.1 The same in `NetworkMapView.swift`, `AppModel.swift` and `PlaceOfferMenu`, with the strings in `Localizable.xcstrings`. The project builds; simulator screenshots of "Name of a café" and "Place without a name" (taps through a throwaway UI test, or by the maintainer on the iPhone).

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` lists every scenario with how it was checked, plus ride steps for the maintainer: tap a café and a toilet while planning and while navigating, make one a stop. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. A CHANGELOG entry goes under Unreleased (create the section if missing).

## Workflow follow-up

- Apply after `highlight-places-on-map`; archive `add-route-points` and `highlight-places-on-map` first or together, as this change builds on their requirements.
- Adding a place to the route while navigating, and naming route points after their place, can follow as their own changes.
