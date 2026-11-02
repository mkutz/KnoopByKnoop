# Tasks

## 1. Core

- [x] 1.1 `Texts`: the notice and display wording in English, German, Dutch and French. `NavigationTest` "in the app's language".
- [x] 1.2 `Navigation`: `furthest`, `wrongWay`, the comparison and its ending, `NavigationDisplay.wrongWay`, the red dot in `progress`, `FINE` precision, `wayToRoute` staying `null`. Tests in `NavigationTest`, one for each scenario of "Riding the wrong way":
  - "riding back along the route": the notice once, and the distance along the route growing;
  - "wrong direction in the panel": the display's text and distance, no way to the route;
  - "turning around", "turning back shortly", "riding back again later";
  - "a place that jumps": a course doubling back past itself;
  - "leaving the course while riding the wrong way", "passing the next knot while riding the wrong way";
  - "on the way back of an out-and-back route", "following the branch to another signpost";
  - "before the first knot", "without geometry".
- [x] 1.3 Replay the ride on the real course: mocked positions on the emulator along 02 → 28 with the published data, forward, then back as in the video, and note when the warning comes. Done in place of a unit test, as the core's tests have no published data; see `manual-checks.md`.

## 2. Android

- [x] 2.1 The header in `PlannerScreen`, the notification's title and look in `NavigationService`, strings in all four languages if the header needs them. On the emulator with mocked positions along the course of 02 → 28, riding forward, then back: screenshots of the panel and the notification, in light and dark, in English and German, at the largest text size ("Wrong direction in the panel", "Wrong direction outside the app"). Then turning around ("Turning around").

## 3. iOS

- [x] 3.1 The header in `PlannerView`, `ContentState.wrongWay`, the lock screen card and the Dynamic Island, the catalogs. The project builds, and the same ride in the simulator with a temporary launch flag, removed afterwards, shows the panel. The Live Activity and the island go to the maintainer's steps.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The maintainer checks it on a ride on the Pixel and the iPhone, riding back along a route on purpose and with the screen off, before `/opsx:archive warn-wrong-direction`. The 150 m may need tuning then.
