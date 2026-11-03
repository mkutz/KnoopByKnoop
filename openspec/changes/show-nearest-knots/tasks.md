# Tasks

## 1. Core

- [x] 1.1 `KnotLocator.noKnotsIn` and `nearestPosition`. Tests in `PlanningTest`: a knot inside and outside the bounds, also at another position of a merged knot; the nearest knot far away, and in the selected network only ("Other network").

## 2. Android

- [x] 2.1 `mapMoved` passes the bounds; `noKnotsInView`; the button below the to-me button; `showNearestKnots` with the position finder and the middle of the map as fallback; the panel; the strings in four languages. On the emulator:
  - the map over an area without knots, e.g. Berlin: a screenshot of the button and the panel ("No knots in the area");
  - zoomed out ("Zoomed out too far"), and with knots in view ("Knots in view");
  - a start set and the map moved away ("With waypoints set");
  - a tap with a mocked position 8 km off ("From the rider's position"), with access denied ("Without location access"), and from Berlin ("Far from the network");
  - hiking selected ("Other network");
  - TalkBack's text from `uiautomator dump` ("Nearest knots with a screen reader"), and German at the largest font scale;
  - not shown while navigating ("Not while navigating").
  - Light and dark.

## 3. iOS

- [x] 3.1 The same in `NetworkMapView`, `AppModel` and `PlannerView`, with `Localizable.xcstrings`. The project builds; simulator screenshots of the button and the panel over Berlin, and after a tap, in light and dark. "No position found" goes to a unit test of the fallback if possible, otherwise to the maintainer's steps.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Found on the iPhone

- [x] Knot behind the search field, button staying ("Clear of the controls"): the camera centered on the padded area, Android's top padding below the controls. Simulator before and after, emulator with a knot to the north-east.

## Workflow follow-up

- The maintainer checks it on the Pixel and the iPhone before `/opsx:archive show-nearest-knots`.
