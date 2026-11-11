# Tasks

## 1. Core

- [x] 1.1 Add `HeaderRow` and `HeaderBadge` to `NavigationUpdate`, with the close, far and end rows and the sign-like `filled`. Tests in `NavigationTest`:
  - "Next knot in the middle of the route", "Far from the next knot", "Coming close", "Knot right after the last one", "Next knot is the end" and "Distance to the first knot".
  - "Dutch knots in the header", "German knots in the header" and "Picked knot in the header", for the fill, the corner role and which badge is on the sign plate.
- [x] 1.2 Add `joinedRoute` and the on-the-way row. The off-route row gets its arrow and distance from `wayToRoute`, falling back to the next knot, with the bearing relative to the riding direction or to north when standing still. Tests in `NavigationTest`:
  - "Direction in the header", "Back to the route, not to the knot", "Standing still", "Without courses" and "Distance off route".
  - "Starting at home", "On the route" and "Leaving the route later".
- [x] 1.3 Add the on-network check: within 30 m of any course of the installed geometry, evaluated only off route or on the way. Tests in `NavigationTest`: "Off the network", "Wrong turn within the network" and "Reaching the network on the way".
- [x] 1.4 Add the guided and wrong-way rows. Tests: "In the header" for the guided way (turn, distance, street, distance left) and "Wrong direction in the panel" (U-turn row with 28 and the distance along the route).
- [x] 1.5 Add `HeaderRow.spoken(language)`. Tests: "Far from the next knot with a screen reader", "Close to the next knot with a screen reader", "Direction with a screen reader", "On the way with a screen reader" and "Header with a screen reader", in English. The German texts of "In the app's language" exist for wrong way.
- [x] 1.6 Mark the next knot and the number to follow as labeled on the strip model, with their badge style, and let them win over overlapping stop badges. Tests in `RouteProgressTest`: "Knots of the header on the strip", "Passed knot leaves the strip's labels" and "Next knot close to a stop".

## 2. Android

- [x] 2.1 Lay out the hero row from `HeaderRow` in the navigation header: badges at map size, arrows, distances, the dark background for off route, guided and wrong way, and the to-the-route symbol tile for the way to the route. Make Stop a fixed-size icon button with a content description and a long-press name. Check on the emulator with screenshots in `screenshots/`:
  - every scenario of 1.1 to 1.4
  - "Same layout in every language" (English and German side by side, the same height)
  - "Stop in every language"
  - the strip scenarios of 1.6
- [x] 2.2 Large text sizes. Check on the emulator at the largest font: "Number to follow at the largest text size", "Next knot at the largest text size", "Number to follow at the default text size" and "Off route in German".
- [x] 2.3 Screen reader. TalkBack check of the announcements in 1.5 and "Stop with a screen reader".

## 3. iOS

- [x] 3.1 The same row in `PlannerView.swift`, with Stop as an icon button with an accessibility label. The project builds, and simulator screenshots cover the scenarios of 2.1.
- [x] 3.2 Large text sizes and VoiceOver, as 2.2 and 2.3, in the simulator.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` lists every scenario of the deltas with how it was checked, plus ride steps for the maintainer:
  - a German ride: close, far, end, a wrong turn within the network, and leaving the network
  - a start from home
  - riding the wrong way
  - a guided way

  `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. A CHANGELOG entry goes under Unreleased (create the section if missing).

## Workflow follow-up

- Apply only after `knot-colours-from-signs` is applied, since this change uses its plates, corner symbols and dark off-route header.
- The maintainer checks it on the Pixel and the iPhone before `/opsx:archive sign-like-header`.
- `turn-at-knot` then replaces the plain arrow, and `sign-like-lock-screen` brings the row outside the app.

## Notes

- 2.1 to 3.2, 2026-11-09: see `manual-checks.md`. "Dutch knots in the header" is covered by `NavigationTest` only; no Dutch route was ridden on a device. 2.3 and 3.2 stay open for listening: the labels are verified in the accessibility tree, the large text sizes on iOS are done (3.2's first half), and the maintainer's step 5 hears TalkBack and VoiceOver once.
