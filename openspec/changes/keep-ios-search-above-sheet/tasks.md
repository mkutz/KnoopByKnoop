# Tasks

## 1. iOS layout

- [x] 1.1 Derive `keyboardShown` in `PlannerView` from the bottom safe area of its `GeometryReader` (more than 100 points, see design.md); verify in the simulator that the field stays above the sheet with the Mac keyboard connected and moves above the on-screen keyboard without it (temporary logging showed 34 points without a keyboard)
- [x] 1.2 Use `keyboardShown` instead of `searchFocused` for the bottom padding and the hiding rule in `searchOverlay`; verify the iOS app builds for the simulator
- [x] 1.3 Lower the sheet to its smallest detent when `keyboardShown` turns true in portrait, instead of when the field gets the focus; verify the iOS app builds for the simulator

## 2. Checks

- [x] 2.1 `./gradlew check` passes
- [ ] 2.2 Manual checks in `manual-checks.md` in the iPhone simulator, with the Mac keyboard connected: "Searching with a hardware keyboard on iOS", "Back above the sheet on iOS", "Sheet pulled up on iOS"; with the on-screen keyboard: "Typing on iOS", "Back above the sheet on iOS" (landscape is unchanged by this change; its lack of room for the hits is a separate change, see `manual-checks.md`) (the scenarios are UI layout with no logic in the core, so they are covered by these checks instead of automated tests, as in `move-search-above-sheet`)
- [ ] 2.3 The maintainer checks "Typing on iOS" on the iPhone, to see that the on-screen keyboard behaves as before
- [ ] 2.4 CI passes
