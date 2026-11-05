# Tasks

## 1. Core

- [x] 1.1 `NavigationDisplay.close` (default `true`), set by `Navigation.display` from the reminder state. Tests in `NavigationTest`:
  - "Far from the next knot": started 800 m before 45, `display.close` is false.
  - "Coming close": riding within 200 m, the reminder notice and `display.close` are true together.
  - "Knot right after the last one": passing a knot with the next one closer than 200 m, it is true right away.
  - "Next knot in the middle of the route" and "Distance to the first knot": within 200 m, it is true.

## 2. Android

- [x] 2.1 `NavigationHeader` far layout ("Next", badge, distance; "then", small badge) when `!display.close` and `follow != null`, the close layout as now; the screen reader's description; the strings `next` and `then` in `values`, `values-de`, `values-nl` and `values-fr`. On the emulator with a mocked ride:
  - a screenshot far from a knot and one within 200 m ("Far from the next knot", "Coming close");
  - the far header in German and at font scale 2.0 ("Next knot at the largest text size");
  - TalkBack's text from `uiautomator dump` ("Far from the next knot with a screen reader").

## 3. iOS

- [x] 3.1 `NavigationHeader` the same way, with "Next" and "then" in `Localizable.xcstrings` in all four languages. The project builds, and simulator screenshots show the far and close headers at the default and the largest accessibility size. The ride goes to the maintainer's steps in `manual-checks.md`.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under a new `## Unreleased`.

## Workflow follow-up

- The maintainer checks it on a ride, on the Pixel and the iPhone, before `/opsx:archive follow-number-when-close`. Archive after `fix-double-knots`, `fix-ride-display-bugs` and `improve-off-route`, whose texts of the same requirements this delta builds on.
