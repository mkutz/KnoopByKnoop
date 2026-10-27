# Manual checks

Checked 2026-10-27 on the Android emulator (`Medium_Phone_API_37.0`) with a 608.9 km route from Oostende (24) via the Kaag (08) and Broekhuizen (56) to Kampen (21), restored from `route.json`. iOS is checked by the maintainer on an iPhone (⏳ until then); the simulator only showed the smallest sheet, as it cannot be dragged or scrolled in this setup ([screenshot](screenshots/ios-simulator-long-route.png)).

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Long route | ✅ | ⏳ | The smallest sheet shows the distance, Back to start, Clear and the first badges; the badges swipe sideways ([screenshot](screenshots/android-badges-swiped.png)); the expanded sheet scrolls through seven ferries to the waypoints ([screenshot](screenshots/android-long-route.png), [scrolled](screenshots/android-long-route-scrolled.png)); the same in landscape ([screenshot](screenshots/android-landscape.png)) |
| Editing waypoints of a long route | ✅ | ⏳ | At the end of the scrolled list: dragged 56 below 08 (608.9 → 550.6 km), swiped 08 away (456.3 km, no crash, [screenshot](screenshots/android-waypoint-removed.png)), dragged 56 to the start |
| Short route | ✅ | ⏳ | 08 → 31 → 97: badges in one line, two ferries and three waypoints, nothing scrolls ([screenshot](screenshots/android-short-route.png)) |

## Found and fixed

- The first layout of this change, with the badges wrapping inside the scrolling body, worked, but on long routes the badges filled the whole sheet. The maintainer suggested one sideways scrolling line; the ferries and waypoints still scroll below it (see design).
- With the badges in the header, the smallest Android sheet cut them off below the two stacked buttons; Back to start and Clear are now side by side next to the distance, on both platforms.
- Swiping a waypoint away at the end of the long, scrolled list crashed the Android app in `SwipeToDismissBox` ("The offset was read before being initialized"), also before this change. The rows are now keyed by the waypoints as well as their index; the same steps no longer crash.
