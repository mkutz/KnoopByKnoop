## 1. Android

- [x] 1.1 Split `WaypointSheet` into a fixed header (distance or hint, Clear, Back to start, one sideways scrolling line of badges) and a body
- [x] 1.2 Make `WaypointList`'s `LazyColumn` the body: the ferries as its first item, the waypoint rows after it, filling the rest of the sheet; keep drag and swipe working
- [x] 1.3 Key the waypoint rows so that removing a waypoint does not reuse another row's swipe state, and check that removing at the end of a long, scrolled list no longer crashes

## 2. iOS

- [x] 2.1 Split `WaypointSheet` into a fixed header with a horizontal `ScrollView` of badges, and the `List`
- [x] 2.2 Put the ferries into the `List` as a first section without move and delete; keep `onMove` and `onDelete` on the waypoints

## 3. Checks

- [x] 3.1 Both apps build; `./gradlew check` passes
- [x] 3.2 CI passes
- [x] 3.3 Manual checks in `manual-checks.md`, on the Android emulator and by the maintainer on the iPhone, in portrait and landscape: "Long route", "Editing waypoints of a long route", "Short route" (UI behavior with no logic in the core, so covered by these checks instead of automated tests)
