# Tasks

## 1. Steady map while following

- [x] 1.1 On the emulator, navigate with a mocked position each second along a street with POIs and record the screen: the flicker shows as in the ride video. Record again with (a) no source updates, (b) no camera moves, and note the cause in `manual-checks.md`
- [x] 1.2 Fix the cause as the design says; a recording of the same ride shows the POIs and street names steady ("Steady base map while following"), and following, looking around and recentering still work

## 2. Compass, header, dark red

- [x] 2.1 Compass margins below the search field, from the status bar inset; with the map rotated, a screenshot shows the compass below the search field and the battery icon uncovered ("Compass below the status bar"), also in landscape
- [x] 2.2 `OffRouteHeader` as a `FlowRow`; screenshots off route in German, English, Dutch and French at the default and the largest text size show the badge ("Off route in German")
- [x] 2.3 A dark `error` red with at least 4.5:1 contrast to the dark surface (the calculation noted in `manual-checks.md`); a screenshot off route in dark mode shows the header, the arrow and the dot red ("Off route in dark mode"), and the ferry warning on the strip in dark mode

## 3. iOS and wrap-up

- [x] 3.1 iOS in the simulator: the off-route header in German at the default and the largest text size shows the badge, and off route in dark mode is red; fix the header if the badge is missing. Steps for the maintainer's iPhone in `manual-checks.md`
- [x] 3.2 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entries under Unreleased for the four fixes

## Workflow follow-up

- The maintainer checks on a ride with the Pixel (and the iPhone for the header and dark mode) before `/opsx:archive fix-ride-display-bugs`.
