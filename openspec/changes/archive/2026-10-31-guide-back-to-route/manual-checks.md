# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`), positions mocked with `adb emu geo fix`. The Play Store had updated the app to a release build again, so the app was uninstalled and the debug build installed; the emulator then had no internet, so the courses could not be downloaded. Without courses, off route is detected by passing a knot not on the route, which is how the guidance was checked; starting at the first knot needs the courses and is covered by `NavigationTest`, and on the iPhone.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Start away from the route, Joining the route on the way to the start | unit tests | ✅ | `NavigationTest`; needs the courses |
| Start away from the route without courses | ✅ | ✅ | Started 500 m east of 02 on 76 → 02 → 20 before the courses were there: next knot 02, not off route |
| Passing a knot that is not on the route, Off route (direction) | ✅ | ✅ | Started at 76, then at knot 06 (not on the route): "Off route, next 02, 956 m to the east" |
| Line to the knot, Both in view | ✅ | ✅ | A dashed red line from the rider to 02; the map shows both ([screenshot](screenshots/android-off-route.png)) |
| Direction in the header, Direction with a screen reader | ✅ | ✅ | "Off route, next ⓪②" above "956 m ➤ east", the arrow to the right as 02 lay right of the riding direction; `uiautomator dump`: "Off route, next 02, 956 m to the east" |
| Standing still | ✅ | ✅ | After 12 s without moving the arrow points east-north-east, as on the north-up map ([screenshot](screenshots/android-off-route-standing.png)) |
| Getting closer, Updated on the way, Lock screen off route | ✅ | ✅ | 570 m closer: "Next 02, 383 m to the east" spoken; notification "Off route, next 02" over "956 m east to 02", then "383 m east to 02" |
| Back on route, Back on route outside the app | ✅ | ✅ | At 02: "Back on route, next 20", the line gone, the notification back to "321 m to the end, around 12:45" ([screenshot](screenshots/android-back-on-route.png)) |
| Wrong turn between knots, Direction changes, Riding around without getting closer, Off route is announced once | unit tests | unit tests | `GuidanceTest`, `NavigationTest`, JVM and iOS simulator |

iOS checked 2026-10-31 by the maintainer on the iPhone with the steps below.

### iOS steps for the maintainer

1. With a planned route, start navigating somewhere more than 100 m away from it, e.g. at home: "Off route, next <first knot>, … to the …", the header with the first knot, below it the distance, a red arrow and the direction; a dashed red line on the map to that knot, both in view.
2. Walk or ride towards it: the arrow turns with your direction; standing still, it points as on the map. After 500 m closer: "Next …, … to the …".
3. Lock the phone: the Live Activity shows "… km <direction> to <knot>", changing every 100 m.
4. Reach the first knot (or join the route at a later knot): "Back on route" or the next knot; the line is gone and the strip shows your progress.
5. VoiceOver on the header while off route: "Off route, next …, … to the …".
