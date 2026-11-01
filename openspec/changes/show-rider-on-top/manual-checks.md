# Manual checks

## Scenarios

Android checked 2026-11-01 on the emulator (`Medium_Phone_API_37.0`, 420 dpi) with the debug build, navigating 93 → 29 → 94 in Arnhem, positions mocked once a second at 6 m/s along the course with `adb emu geo fix`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Riding along the route | ✅ | ✅ simulator | After 250 m: from 93 the route is blue with orange edges, ahead to 94 orange with its arrows ([screenshot](screenshots/android-track-and-arrow.png)); the same in dark mode ([light and dark](screenshots/android-light-and-dark.png)) |
| Seeing the position at a glance | ✅ | ✅ simulator | The arrow measures about 105 px from tip to base, 40 dp at 2.625, larger than a knot's badge ([close-up](screenshots/android-arrow-close-up.png)); standing still, the dot is about 77 px across with its outline, 29 dp ([screenshot](screenshots/android-dot-dark.png)) |

iOS checked in the simulator (iPhone 18 Pro, iOS 27), navigation started by a temporary launch flag (removed again), the location moved along the course at 6 m/s with `simctl location start`: the ridden stretch from 93 is blue on the route with its orange edges, the part ahead orange with arrows, and the arrow 44 pt long ([screenshot](screenshots/ios-track-and-arrow.png)). The simulator's route, location and dark appearance were restored.

### Steps for the maintainer

1. On a ride with the Pixel and the iPhone: the part of the route already ridden shows the blue track on top with orange edges; the part ahead stays orange with arrows.
2. The rider's arrow is easy to find at a glance; standing still at a traffic light, the dot is too.
