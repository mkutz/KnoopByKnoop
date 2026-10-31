# Manual checks

## Scenarios

The maintainer checked the first version on the iPhone on 2026-10-31 and asked for a revision: the badges and the distance to the next knot in the header, and distances and arrivals on the strip instead of the table. Rows marked ✅ for iOS were checked in the first round and are unchanged by the revision.

Android checked 2026-10-30 (first version) and 2026-10-31 (revision) on the emulator (`Medium_Phone_API_37.0`) with the bundled graphs, positions mocked with `adb emu geo fix`. Mocked positions jump from knot to knot within seconds, so the own pace after 2 km is unrealistically fast there; the times at a real pace are covered by `RouteProgressTest`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Route summary (planning time) | ✅ | ✅ | 76 → 02 → 20 at Renkum: "2.3 km" with "about 9 min" below ([screenshot](screenshots/android-planning-cycling.png)) |
| Hiking time | ✅ | ✅ | O68 → O39 near Heteren: "2.6 km", "about 35 min" ([screenshot](screenshots/android-planning-hiking.png)) |
| Strip at the start, Ferry on the strip | ✅ | ✅ | First iPhone check: strip with the start, ⛴, the stops and the end, the dot at the start |
| Next knot in the middle of the route, Ahead at the start, Ferry ahead on the strip, Stops close together | ✅ | ⏳ | Revision, 76 → 02 → 19 (109.6 km) at 11:09: header "At ⓪② follow ②⓪ 2.0 km" with badges; below the strip "2.0 km 11:15" at the stop 02, "33.2 km 13:15" at a ferry, "109.6 km 18:00" at the end; the ferry right before 02 gives way to the stop; no table ([screenshot](screenshots/android-navigating-long-route.png)) |
| Distance off route, Off route on the strip ahead, Off route on the strip | ✅ | ⏳ | Revision, off route near Renkum: "Off route, next ⓪② 1.8 km", the dot red, the distances counted from 02 plus the straight distance ([screenshot](screenshots/android-navigating-off-route.png)) |
| Moving along, Stop passed (first version) | ✅ | ✅ | After the stop 02 on 76 → 02 → 20: filled up to the dot; the stop no longer ahead ([screenshot](screenshots/android-navigating-after-stop.png), with the first version's table) |
| Progress with a screen reader | ✅ | ⏳ | `uiautomator dump`: content description "0.0 of 2.3 km ridden, next knot 02" at the start, "2.1 of 2.3 km ridden, next knot 20" after the stop |
| Lock screen, Progress outside the app | ✅ | ✅ | Notification "Off route, next 20" over "321 m to the end, around 21:05", as of passing 02 ([screenshot](screenshots/android-notification.png)) |
| Along the way, The rider's own pace, Distances that would overlap (core part), Round trip, Rounded times, Hiking time, Long route time | unit tests | unit tests | `RouteProgressTest`, JVM and iOS simulator |

### iOS steps for the maintainer (revision)

1. Navigate a route with stops and ferries (e.g. the one through Utrecht): the header shows "At ⓪② follow ②⓪" with badges like on the map, the one to follow largest, and the distance to the next knot in the same line. No table below the strip.
2. Below the strip, the stops, ferries and the end that have a badge show their distance and arrival; close stops before the end (96, 37, 62) show none, and labels never overlap.
3. Off route: "Off route, next ⓪②" with the straight distance; the labels count from the next knot.
4. A route with two ferries, the first with a website, the second without (e.g. Wesseling): tapping the second one expands it and opens no website; "Website" of the first still opens its site.
5. VoiceOver on the strip: "… of … km ridden, next knot …".

### Found and fixed

- iOS, from `polish-planner-panel`: tapping a ferry without a website (Wesseling) opened the website of the ferry before it, as all ferries were one list row and a row sends taps to its first link. Each ferry is its own row now, with a borderless link.
- At a labeled knot, the badge covered the rider's dot (at the start, at 76): the dot is drawn above the badges now.
- The planning time wrapped next to the actions ("about 35" / "min"): it is on its own line below the distance now.
