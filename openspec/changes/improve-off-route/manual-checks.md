# Manual checks

## Scenarios

Android checked 2026-11-01 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the installed cycling courses. Route 93 → 94 in Arnhem, positions mocked with `adb emu geo fix`, two fixes a second apart per point, along the course from 93, then about 180 m north, then 60 m beside the course's next stretch, then back within about 20 m of it. The emulator's own route was restored afterwards.

iOS checked the same way in the simulator (iPhone 18 Pro, iOS 27), with navigation started 30 s after launch by a temporary launch flag (removed again), the location moved along the same points at 10 m/s with `simctl location start`. The simulator's route and appearance were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Wrong turn between knots | ✅ | ✅ simulator | "Off route, next 94, 422 m to the south-east" once, about 100 m north of the course |
| Line to the route | ✅ | ✅ simulator | Beside the course, the dashed line goes to its nearest point ahead, not to 94 ([Android](screenshots/android-off-route-line.png), middle; [iOS](screenshots/ios-off-route-line.png), middle) |
| Route behind the rider | ✅ | ✅ simulator | North of where the rider left the course, the line goes back to that place, not further back towards 93 (left) |
| Parallel path | ✅ | ✅ simulator | 60 m beside the course: still off route, no notice (middle) |
| Back on route | ✅ | ✅ simulator | About 20 m from the course: "Back on route, next 94", the line is gone, the map follows as before (right) |
| Both in view | ✅ | ✅ simulator | The rider and the end of the line in view, at the navigation zoom; before the zoom cap the map zoomed in to street level with the rider under the search field ([screenshot](screenshots/android-off-route-line-too-close.png)) |
| Line to the knot | unit test | unit test | `NavigationTest` "line to the knot": without courses and before the first knot |
| Distance to the first knot | unit test | unit test | `NavigationTest` "distance to the first knot", now back within 30 m of the first leg |

### Steps for the maintainer

1. On a ride with the Pixel and the iPhone, leave the route on purpose: the dashed line points to the nearest part of the route ahead, not to the next knot, and the map shows both you and its end.
2. Ride back on a path beside the route, 30–100 m away: no "Back on route".
3. Join the route: "Back on route" and the line is gone.
