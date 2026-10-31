# Manual checks

## Scenarios

The maintainer checked the first version, the revision and revision 2 on the iPhone on 2026-10-31 and asked for a revision: the badges and the distance to the next knot in the header, and distances and arrivals on the strip instead of the table. Rows marked ✅ for iOS were checked in the first round and are unchanged by the revision.

Android checked 2026-10-30 (first version) and 2026-10-31 (revision) on the emulator (`Medium_Phone_API_37.0`) with the bundled graphs, positions mocked with `adb emu geo fix`. Mocked positions jump from knot to knot within seconds, so the own pace after 2 km is unrealistically fast there; the times at a real pace are covered by `RouteProgressTest`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Route summary (planning time) | ✅ | ✅ | 76 → 02 → 20 at Renkum: "2.3 km" with "about 9 min" below ([screenshot](screenshots/android-planning-cycling.png)) |
| Hiking time | ✅ | ✅ | O68 → O39 near Heteren: "2.6 km", "about 35 min" ([screenshot](screenshots/android-planning-hiking.png)) |
| Strip at the start, Ferry on the strip | ✅ | ✅ | First iPhone check: strip with the start, ⛴, the stops and the end, the dot at the start |
| Next knot in the middle of the route, Ahead at the start, Ferry ahead on the strip, Stops close together | ✅ | ✅ | Revision, 76 → 02 → 19 (109.6 km) at 11:09: header "At ⓪② follow ②⓪ 2.0 km" with badges; below the strip "2.0 km 11:30" at the stop 02, "33 km 13:25" at a ferry, "110 km 18:10" at the end (whole kilometers from 10 km); the ferry right before 02 gives way to the stop; no table ([screenshot](screenshots/android-navigating-long-route.png)) |
| Distance off route, Off route on the strip ahead, Off route on the strip | ✅ | ✅ | Revision, off route near Renkum: "Off route, next ⓪② 1.8 km", the dot red, the distances counted from 02 plus the straight distance ([screenshot](screenshots/android-navigating-off-route.png)) |
| Moving along, Stop passed (first version) | ✅ | ✅ | After the stop 02 on 76 → 02 → 20: filled up to the dot; the stop no longer ahead ([screenshot](screenshots/android-navigating-after-stop.png), with the first version's table) |
| Progress with a screen reader | ✅ | ✅ | `uiautomator dump`: content description "0.0 of 2.3 km ridden, next knot 02" at the start, "2.1 of 2.3 km ridden, next knot 20" after the stop |
| Closed ferry before a stop, Ferry popover, Popover of a passed ferry, No ferry list while navigating, Ferry with a screen reader, Warning in the ferry list (strip), Closed for the season in the ferry list (popover) | ✅ | ✅ | Revision 2. Renkum 76 → 02 → 20 at 11:31: the ferry with a red ring and "1.0 km 11:35" in red ([screenshot](screenshots/android-ferry-on-strip.png)); tapping it: "⛴ 76 → 02", "Renkumse Veer", "At 11:35: closed until 1 May 10:00" in red, times, the OpenStreetMap note, Website ([screenshot](screenshots/android-ferry-popover.png)); after 02, the popover shows "Closed until 1 May 10:00" and the ring is gone ([screenshot](screenshots/android-ferry-popover-passed.png)); no ferry list while navigating; TalkBack (`uiautomator dump`): "Ferry 76 → 02, at 11:35 closed until 1 May 10:00". Long route: the closed ferry before the stop 02 is shown instead of 02, the open ferry "33 km 13:35" in green ([screenshot](screenshots/android-navigating-long-route.png)) |
| Open ferry before a stop, Open ferry ahead | unit tests and ranking | ✅ | `RouteProgressTest` "Open ferry ahead" (state and arrival); the ranking puts open ferries after stops |
| Info button, No info button while navigating | ✅ | ⏳ | Revision 3. While planning the ⓘ is there (`uiautomator dump`: one "Info" button); while navigating neither the ⓘ nor its bar ([screenshot](screenshots/android-navigating-without-info.png)); after Stop it is back |
| Lock screen, Progress outside the app | ✅ | ✅ | Notification "Off route, next 20" over "321 m to the end, around 21:05", as of passing 02 ([screenshot](screenshots/android-notification.png)) |
| Along the way, The rider's own pace, Distances that would overlap (core part), Round trip, Rounded times, Hiking time, Long route time | unit tests | unit tests | `RouteProgressTest`, JVM and iOS simulator |

### iOS steps for the maintainer (revision)

1. Navigate a route with stops and ferries (e.g. the one through Utrecht): the header shows "At ⓪② follow ②⓪" with badges like on the map, the one to follow largest, and the distance to the next knot in the same line. No table below the strip.
2. Below the strip, the stops, ferries and the end that have a badge show their distance and arrival; close stops before the end (96, 37, 62) show none, and labels never overlap.
3. Off route: "Off route, next ⓪②" with the straight distance; the labels count from the next knot.
4. A route with two ferries, the first with a website, the second without (e.g. Wesseling): tapping the second one expands it and opens no website; "Website" of the first still opens its site.
5. Long distances: from 10 km in whole kilometers ("200 km", "280 km"), not clipped below the strip.
6. VoiceOver on the strip: "… of … km ridden, next knot …".

### iOS steps for the maintainer (revision 2)

1. Navigate a route with ferries: no ferry list below the strip. Ferries ahead keep their badge; one closed when you get there has a red ring and a red arrival below it, an open one a green arrival.
2. A closed ferry close to a stop shows instead of the stop; an open one gives way to the stop.
3. Tap a ferry on the strip: a popover with "⛴ 76 → 02", its name, "At …: …" in its color, the times, fee, website and "Times according to OpenStreetMap"; tapping outside closes it. After passing the ferry, its popover shows the status for now.
4. VoiceOver: each ferry on the strip is a button, e.g. "Ferry 76 → 02, at 21:10 closed until 1 May 10:00".

### iOS steps for the maintainer (revision 3)

1. While planning, the bar with Cycling/Hiking and ⓘ is at the top; start navigating: the bar is gone; stop: it is back.

### Found and fixed

- iPhone, revision 2: the ferry badges were semi-transparent over the map; they have an opaque background under their tint now, like the knots.
- iPhone, revision: distances over 100 km were cut off below the strip ("200.2…", "279.6 k…"): from 10 km they are whole kilometers now ("200 km").
- iOS, from `polish-planner-panel`: tapping a ferry without a website (Wesseling) opened the website of the ferry before it, as all ferries were one list row and a row sends taps to its first link. Each ferry is its own row now, with a borderless link.
- At a labeled knot, the badge covered the rider's dot (at the start, at 76): the dot is drawn above the badges now.
- The planning time wrapped next to the actions ("about 35" / "min"): it is on its own line below the distance now.
