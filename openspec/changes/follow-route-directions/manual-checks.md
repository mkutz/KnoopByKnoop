# Manual checks

## Scenarios

Checked 2026-11-06 with geometry built locally from that day's extracts (not published), installed into the apps' storage. Android: emulator `Medium_Phone_API_37.0`, Android 17. iOS: iPhone 18 Pro simulator, iOS 27. At the L 16 bridge over the Sieg in Siegburg-Zange.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| A cycle path on each side, Route on its side of the road | ✅ | ✅ | 02 → 06 → 02: north on the eastern side of the bridge, back on the western side. 28–02 has no marks in the data, so it was not the place to check |
| Courses to each signpost in each direction | ✅ | ✅ | 06 → 02 ends at 02's second signpost west of the junction, where the way back leads; before, it took the main signpost's course reversed |
| Out and back | ✅ | ✅ | The same route, both lines at once |
| Geometry without courses back | ✅ core | ✅ core | `RouteDirectionsTest`, `CoursesBackFileTest` |
| Released apps | ✅ core | ✅ core | `CoursesBackFileTest` reads like 0.10.0 |
| Line ahead and off route | ✅ core | ✅ core | `RouteDirectionsTest`: navigation on the course back, never off route |

Covered by tests only: "One-way stretch", "No marks", "Marks that leave no way", "Bridges on the course back", "Geometry size budget" (3.29 MB in the local build).

## For the maintainer

| Check | Pixel | iPhone | Notes |
|---|---|---|---|
| 02 → 06 and back at Zange: each way on its side of the L 16 | ⏳ | ⏳ | Does it match what the signs and the paths on the ground say? |
| A route through a one-way street | ⏳ | ⏳ | |
