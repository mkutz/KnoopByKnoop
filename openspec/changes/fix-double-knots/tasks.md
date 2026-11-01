# Tasks

## 1. Core

- [x] 1.1 Tests in `NavigationTest` for "Passing a double knot" (route 24, 45, 46, 12 with 46 10 m after 45: one "Next 12", no notice for 46) and "Riding on after a double knot" (the distance to 12 and the strip's rider move on); they fail before the fix, as in the throwaway test from the ride
- [x] 1.2 `Navigation.update`: count the run of consecutive route knots within 30 m of the fix as passed, keeping the round-trip rule; the tests from 1.1 and all existing `NavigationTest` scenarios pass
- [x] 1.3 Test for "Start at a double knot" (start between 45 and 46 of the route 24, 45, 46, 12: 12 is the next knot), then make `Navigation.start` begin after the run; the test passes
- [x] 1.4 Test for "Distance to the first knot" (started away, back within 100 m of the course 80 m from 24: panel "80 m", line "80 m to 24"), then pass the straight distance to the first knot to `RouteProgress.view` and `line`; the test and `RouteProgressTest` pass

## 2. Check

- [x] 2.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased says that navigation no longer gets stuck at double knots and that the distance to the first knot is shown
- [x] 2.2 On the emulator, a mock route through a double knot passes both and moves on to the next knot (screenshot of the panel after the double knot), written up in `openspec/changes/fix-double-knots/manual-checks.md`

## Workflow follow-up

- The maintainer rides through a double knot (e.g. 01 and 03 at the Den Helder ferry) and starts a route away from its start, on the Pixel and the iPhone, before `/opsx:archive fix-double-knots`.
