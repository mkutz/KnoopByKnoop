# Tasks

## 1. iOS layout

- [x] 1.1 Measure the field's height with `onGeometryChange` and give the hits the room above it, without the 80-point minimum; verify the iOS app builds and portrait still shows the hits above the field (checked on the iPhone, see `manual-checks.md`)
- [x] 1.2 In landscape while `keyboardShown`, leave out the network picker with the info button and the attribution, and use the top safe area plus 8 points as the overlay's top padding; verify the iOS app builds for the simulator

## 2. Checks

- [x] 2.1 `./gradlew check` passes
- [x] 2.2 Manual checks in `manual-checks.md`, by the maintainer on the iPhone in landscape with the on-screen keyboard: "Typing in landscape on iOS", "Done typing in landscape on iOS", "Map controls in landscape" and "Attribution" when not typing; in portrait: "Typing on iOS" and "Back above the sheet on iOS" again (the scenarios are UI layout with no logic in the core, so they are covered by these checks instead of automated tests, as in `move-search-above-sheet`)
- [ ] 2.3 CI passes
