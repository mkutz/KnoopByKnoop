# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`) on the round trip 93 → 29 (stop) → 94 (end) in Arnhem, positions mocked with `adb emu geo fix`. The maintainer checked iOS on the iPhone on 2026-10-31.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Starting with the smallest panel | ✅ | ✅ | The number to follow and the strip with its labels; the top of the first coming knot peeks out below ([screenshot](screenshots/android-smallest.png)) |
| Starting with a high panel | unchanged | ✅ | Starting navigation brings the sheet down as before |
| Dragging the panel | ✅ | ✅ | Down: it stops at the number to follow and the strip |
| Dragging the panel up | ✅ | ✅ | The coming knots below the strip ([screenshot](screenshots/android-dragged-up.png)) |
| Ending navigation | ✅ (Stop) | ✅ | Back to the planning sheet ([screenshot](screenshots/android-after-stop.png)) |
| Navigating in landscape | ✅ | ✅ | The side panel shows the list below the strip ([screenshot](screenshots/android-landscape.png)) |
| Coming knots at the start | ✅ | ✅ | 94 "467 m 18:35", 41 "916 m 18:35", 29 stop "2.2 km 18:40", 41 "3.5 km 18:45", 94 end "4.0 km 18:50"; the exact values of the scenario in `RouteProgressTest` |
| After passing knots | unit test | ✅ | `RouteProgressTest` "after passing knots" |
| Ferry in the list | unit test | ✅ | `RouteProgressTest` "ferry in the list" (the emulator's routes have no ferry) |
| Off route | unit test | ✅ | `RouteProgressTest` "coming knots off route" |
| Long route | unit test | ✅ | `RouteProgressTest` "long list of coming knots"; the list is a `LazyColumn` / `List` and scrolls |
| Coming knots with a screen reader | ✅ | ✅ | `uiautomator dump`: "Knot 29, stop, 2.2 km, 18:40", "Knot 94, end, 4.0 km, 18:50" |
| Tapping a coming knot | ✅ | ✅ | Tapping 29: the sheet comes down, the map shows 29, the recenter button appears ([screenshot](screenshots/android-knot-shown.png)). The map extends behind the sheet, so 29 is a bit below the middle of the visible map |
| Back to the rider | ✅ | ✅ | Recenter: back to the rider, the button gone ([screenshot](screenshots/android-recentered.png)) |

### iOS steps for the maintainer

1. Navigate a route with a stop: the sheet shows the number to follow and the strip, with a grabber.
2. Drag the sheet up: below the strip, the coming knots from the next one to the end, each with its badge, distance and arrival; the stop and the end with their badges. Drag it down: it stops at the strip.
3. Ride past a knot: it drops out of the list.
4. On a route with a ferry: the ferry is listed between its knots, its arrival in red when it is closed then.
5. Off route: the list starts with the next knot at the straight distance.
6. VoiceOver on a row: e.g. "Knot 30, stop, 5.2 km, 18:40".
7. Tap a knot in the list with the sheet pulled up: the sheet comes down, the map shows the knot and stops following, the recenter button appears; recenter brings the map back to you.
8. Landscape: the list below the strip in the side panel.
9. With the sheet pulled up, the map above it can still be moved; when the sheet is at full height, the map is covered as usual.

## Found and fixed

- iOS: with the sheet draggable again, UIKit dimmed the tint of the map's buttons behind it: the recenter icon was pale green on green, the zoom icons gray ([screenshot](screenshots/ios-dimmed-buttons.png), dark mode, off route). The map view now keeps its normal tint.
- iOS: since the route summary's layout for large text sizes, "Navigate" was cut to "Navi…" at the default text size, with room to spare: inside `ViewThatFits`, SwiftUI squeezed the actions instead of the spacer. The actions now keep their full width (iPhone).
