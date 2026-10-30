# Manual checks

## Scenarios

Android checked 2026-10-30 on the emulator (`Medium_Phone_API_37.0`) with the bundled graphs, positions mocked with `adb emu geo fix`. Mocked positions jump from knot to knot within seconds, so the own pace after 2 km is unrealistically fast there; the times at a real pace are covered by `RouteProgressTest`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Route summary (planning time) | ✅ | ⏳ | 76 → 02 → 20 at Renkum: "2.3 km" with "about 9 min" below ([screenshot](screenshots/android-planning-cycling.png)) |
| Hiking time | ✅ | ⏳ | O68 → O39 near Heteren: "2.6 km", "about 35 min" ([screenshot](screenshots/android-planning-hiking.png)) |
| Strip at the start, Ferry on the strip, Ahead at the start | ✅ | ⏳ | Started at 76 at 21:04: strip 76 · ⛴ · 02 · 20 with the dot at 76; "Next knot 02 2.0 km 8 min", "Next stop 02 2.0 km 8 min 21:15", "End 20 2.3 km 9 min 21:15" ([screenshot](screenshots/android-navigating-start.png)) |
| Moving along, No stop left | ✅ | ⏳ | After the stop 02, between 02 and 20: filled up to the dot, "Next knot 20 237 m", "End 20 237 m", no next stop ([screenshot](screenshots/android-navigating-after-stop.png)) |
| Progress with a screen reader | ✅ | ⏳ | `uiautomator dump`: content description "0.0 of 2.3 km ridden, next knot 02" at the start, "2.1 of 2.3 km ridden, next knot 20" after the stop |
| Off route on the strip, Off route ahead | ✅ | ⏳ | 300 m north of the route: "Off route, next 20", the dot red at the last place on the route, "Next knot 20 443 m" without a time ([screenshot](screenshots/android-navigating-off-route.png)) |
| Long route | ✅ | ⏳ | 76 → 02 → 19 (Rotterdam), 109.6 km: the strip fits without scrolling, ticks for the knots, ⛴ for four ferries, 19 at the end; the start gives way to the stop 02 next to it; "End 19 109.6 km 6 h 50 min 04:00" ([screenshot](screenshots/android-navigating-long-route.png)) |
| Lock screen, Progress outside the app | ✅ | ⏳ | Notification "Off route, next 20" over "321 m to the end, around 21:05", as of passing 02 ([screenshot](screenshots/android-notification.png)) |
| Along the way, The rider's own pace, Long time, Next knot is the next stop, Round trip, Rounded times, Long route time | unit tests | unit tests | `RouteProgressTest`, JVM and iOS simulator |

### Found and fixed

- At a labeled knot, the badge covered the rider's dot (at the start, at 76): the dot is drawn above the badges now.
- The planning time wrapped next to the actions ("about 35" / "min"): it is on its own line below the distance now.

### iOS steps for the maintainer

1. Plan 76 → 02 → 20 at Renkum: below "2.3 km", "about 9 min". A hiking route shows its time at 4.5 km/h.
2. Start navigating near 76: in place of the badges a strip with 76, ⛴, 02 and 20, the blue dot at the start; below it "Next knot", "Next stop" and "End" with distance, time and arrival.
3. Ride or walk a bit: the dot moves, the line fills up to it, the distances shrink. After the stop, the "Next stop" row goes away.
4. Off route: the dot turns red and stays; the next knot shows the straight distance without a time.
5. Lock the phone: the Live Activity shows the progress line, e.g. "2.0 km to 02 · end around …", below the number to follow; long-press the Dynamic Island: the same line at the bottom. It changes when a knot is passed.
6. VoiceOver on the strip: "… of … km ridden, next knot …".
