# Manual checks

## Scenarios

Android checked 2026-11-02 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the published data of that day, route 02 → 06 → 85 from Siegburg (33 km), positions mocked along the course to 06 with `adb emu geo fix`: 3 km at 58 km/h, 1 km at 18 km/h, 2 minutes standing, then riding on. No knot was passed in all of it.

iOS checked in the simulator (iPhone 18 Pro, iOS 27), the same route with a temporary launch flag (removed again) and `simctl location start`, about 3 km at 58 km/h, then 1 km at 18 km/h. The route, camera and appearance of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Between two knots | ✅ | ✅ simulator | Android, the end: 12:15 fast, 12:35 slow ([screenshot](screenshots/android-arrivals-first-cap.png)). iOS: 12:30 fast, 12:50 slow ([screenshot](screenshots/ios-arrivals-fast-slow.png)). The coming knots follow too. No app code changed |
| Slowing down | unit test | unit test | `RouteProgressTest` "slowing down between two knots": 18:50, at 12 km/h |
| A break | partly | unit test | `RouteProgressTest` "a break". On the emulator with the first cap (see the design): later while standing, as specified, but a 2-minute stop then cost too much, which led to the cap at a third of the assumed pace. The new cap is covered by the unit test, not by a run on the emulator: 19 km/h right after the break |
| A detour | unit test | unit test | `RouteProgressTest` "a detour" |
| Closed at the renewed estimate | unit test | unit test | `FerryWarningTest` "closed at the renewed estimate" |
| Existing ferry and pace scenarios | unit test | unit test | Unchanged, all pass |

Flicker near a rounding boundary: none seen on the screenshots, but these are snapshots, not a watch of each change. It goes to the steps below.

### Steps for the maintainer

1. On a ride with the Pixel and the iPhone, change your pace clearly for a few kilometres: within about 15 minutes the arrivals on the strip and in the coming knots follow, also between knots.
2. Stop for a few minutes: the arrivals move later while you stand, and after riding on they are back near your earlier pace.
3. Watch whether an arrival keeps flipping between two values five minutes apart.

### Ride of 2026-11-02

Only partly shown: the end's arrival moved from 17:35 to 17:45 while riding back the wrong way. Pace changes and flipping were not shown.
