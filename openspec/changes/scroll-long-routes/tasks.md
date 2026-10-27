## 1. Android

- [ ] 1.1 Split `WaypointSheet` into a fixed header (distance or hint, Clear, Back to start) and a body
- [ ] 1.2 Make `WaypointList`'s `LazyColumn` the body: badges and ferries as its first item, the waypoint rows after it, filling the rest of the sheet; keep drag and swipe working

## 2. iOS

- [ ] 2.1 Split `WaypointSheet` into a fixed header and the `List`
- [ ] 2.2 Put the badges and ferries into the `List` as a first section without move and delete; keep `onMove` and `onDelete` on the waypoints

## 3. Checks

- [ ] 3.1 Both apps build; `./gradlew check` passes
- [ ] 3.2 CI passes
- [ ] 3.3 Manual checks in `manual-checks.md`, on the Android emulator and by the maintainer on the iPhone, in portrait and landscape: "Long route", "Editing waypoints of a long route", "Short route" (UI behavior with no logic in the core, so covered by these checks instead of automated tests)
