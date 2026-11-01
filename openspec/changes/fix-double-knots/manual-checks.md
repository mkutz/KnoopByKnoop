# Manual checks

## Scenarios

Android checked 2026-11-01 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the installed cycling courses, positions mocked with `adb emu geo fix`. The route 01 → 03 → 05 starts at the Den Helder ferry, where 01 and 03 are 9 m apart, the closest double knot in the bundled cycling graph near the coast; it was written to `route.json` and the emulator's own route restored afterwards.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Start away from the route | ✅ | ⏳ | 289 m south of 01: "Off route, next 01, 289 m north" ([screenshot](screenshots/android-off-route-before-01.png)) |
| Distance to the first knot | ✅ | ⏳ | Riding north, "Back on route, next 01", then "At 01 follow 03 89 m" with the dot at the start; before the fix this was "0 m" ([screenshot](screenshots/android-distance-to-first-knot.png)) |
| Passing a double knot | ✅ | ⏳ | One fix 9 m from 01 and 20 m from 03: logged notice "Next 05" (spoken "Next 5"), none for 03; the panel "05 is the end 7.6 km" ([screenshot](screenshots/android-after-double-knot.png)) |
| Riding on after a double knot | ✅ | ⏳ | Two fixes east along the course: "05 is the end 7.5 km", the dot moved off the start ([screenshot](screenshots/android-riding-on.png)) |
| Start at a double knot | unit test | unit test | `NavigationTest` "start at a double knot" |

### Steps for the maintainer

1. Ride through a double knot, two route knots a few metres apart (e.g. 01 and 03 at the Den Helder ferry), on the Pixel and the iPhone.
2. At the double knot: one "Next …" notice naming the knot after both, then the distance to it counts down and the dot on the strip moves on.
3. Start a route away from its start: once back near the course before the first knot, the panel and the lock screen show the distance to the first knot, not "0 m".
