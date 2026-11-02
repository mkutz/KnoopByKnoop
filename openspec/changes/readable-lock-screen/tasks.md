# Tasks

## 1. Core

- [x] 1.1 `NavigationDisplay.text` with the far wording; `progressLine` renewed per 100 m on the route. Tests in `RouteProgressTest` and `NavigationTest`: "Far from the next knot outside the app", "Lock screen", "Distance on the way" (no change under 100 m, new distance and arrival after it), "Progress outside the app" at a knot, and off route as before.

## 2. Android

- [x] 2.1 `NavigationService`: the large badge on all versions, and only notifying on a change. "Older Android" on an API 26–35 emulator if one is at hand, otherwise by reading the version guard, and said so.
- [x] 2.2 From API 36: `ProgressStyle`, promotion, chip, and the permission in the manifest. On the API 37 emulator with a mocked ride:
  - screenshots of the lock screen and the shade far from a knot and close to it, in English and German ("Live Update", "Far from the next knot outside the app", "Lock screen");
  - the number of points the bar shows, with a route with many stops, then choose by the strip's ranking;
  - the chip while another app is open;
  - off route ("Off route outside the app");
  - Stop in the notification ("Stop from the lock screen");
  - Live Updates turned off in the system settings ("Live Update turned off");
  - the distance changing without a knot passed ("Distance on the way").

## 3. iOS

- [x] 3.1 `ContentState.close`, `riderFraction` and `marks`, `NextKnot` far and close, and the `ProgressBar` on the lock screen and in the expanded Dynamic Island ("Progress bar in the Live Activity"). The project builds. Simulator screenshots of the lock screen far and close, and of the Dynamic Island. The ride and the update rate go to the maintainer's steps in `manual-checks.md`.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The maintainer checks it on a ride, on the Pixel and the iPhone, before `/opsx:archive readable-lock-screen`, after `follow-number-when-close`.
