# Manual checks

## How it was checked

Checked on 2026-11-07 with the cycling data built locally with sign regions (see `knot-colours-from-signs`), on the route Troisdorf 25 → 32 → 29:

- **Android:** the emulator (`Medium_Phone_API_37.0`) with the debug build. Positions were set with `adb emu geo fix`.
- **iOS:** the simulator (iPhone 18 Pro, iOS 27). Navigation was started by a temporary `-startNavigation` launch argument, removed again before committing, and positions were set with `xcrun simctl location`.
- **Afterwards:** both devices got their own data, route and camera back.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Next knot in the middle of the route | ✅ | ✅ | 32 with "172 m" / "185 m", →, 25 on the red sign plate ([Android](screenshots/android-close.png), [iOS](screenshots/ios-close.png)). `NavigationTest` |
| Far from the next knot | ✅ | ✅ | 32 large with "4.4 km", › 25 small, no plate ([Android](screenshots/android-far.png), [iOS](screenshots/ios-far.png)). `NavigationTest` |
| Coming close | unit test | unit test | `NavigationTest` "coming close in the header" |
| Knot right after the last one | unit test | unit test | `NavigationTest` |
| Next knot is the end | unit test | unit test | `NavigationTest` |
| Distance off route | ✅ | ✅ | |
| Distance to the first knot | unit test | unit test | `NavigationTest` |
| Dutch knots in the header | unit test | unit test | `NavigationTest`. Not seen on a device yet |
| German knots in the header | ✅ | ✅ | Troisdorf |
| Picked knot in the header | ✅ | ✅ | 32 with the stop's flag |
| Same layout in every language | ✅ | — | English and German look the same, apart from the decimal comma ([German](screenshots/android-on-the-way-german.png)) |
| Far from the next knot with a screen reader | unit test | unit test | Text from `NavigationTest`; TalkBack and VoiceOver not run |
| Close to the next knot with a screen reader | unit test | unit test | As above |
| Direction in the header | ✅ | ✅ | Dark header, to-the-route symbol, arrow, "420 m" ([Android](screenshots/android-off-route.png), [iOS](screenshots/ios-off-route.png)) |
| Back to the route, not to the knot | unit test | unit test | `NavigationTest` |
| Off the network | ✅ | ✅ | No badge east of the Agger |
| Wrong turn within the network | unit test | unit test | `NavigationTest` |
| Standing still | unit test | unit test | `NavigationTest` |
| Without courses | unit test | unit test | `NavigationTest` |
| Direction with a screen reader | unit test | unit test | As above |
| In the header (guided way) | ✅ | ✅ | Turn arrow, "340 m", distance left ([Android](screenshots/android-on-the-way.png), [iOS](screenshots/ios-on-the-way.png)). `GuidedWayTest` for the street |
| Header with a screen reader (guided way) | unit test | unit test | `GuidedWayTest` |
| Number to follow at the largest text size | open | open | |
| Next knot at the largest text size | open | open | |
| Number to follow at the default text size | ✅ | ✅ | |
| Off route in German | open | — | The on-the-way header at the largest font fits one row ([Android](screenshots/android-largest-font.png)) |
| Wrong direction in the panel | unit test | unit test | `NavigationTest` "wrong direction in the header". Not seen on a device yet |
| In the app's language (wrong way) | unit test | unit test | |
| Knots of the header on the strip | ✅ | ✅ | 25 after 32 has its badge on the strip. `RouteProgressTest` |
| Passed knot leaves the strip's labels | unit test | unit test | `RouteProgressTest` |
| Next knot close to a stop | unit test | unit test | `RouteProgressTest`, via the shared `ProgressMark.rank` |
| Starting at home | ✅ | ✅ | Neutral tile, symbol pointing to the route |
| Reaching the network on the way | unit test | unit test | `NavigationTest` |
| On the route | unit test | unit test | `NavigationTest` |
| Leaving the route later | unit test | unit test | `NavigationTest` |
| On the way with a screen reader | unit test | unit test | |
| Stop stands out | ✅ | ✅ | A round red icon button |
| Stop in every language | ✅ | — | Same size in German |
| Stop with a screen reader | open | open | Named "Stop" in code, not yet heard |

## For the maintainer

1. **A German ride:** watch the header far from a knot, close to one (the sign plate), at the end, after a wrong turn within the network (the small badge), and after leaving the network (no badge).
2. **A start from home:** the neutral tile with the arrow pointing to the route, until you reach it.
3. **Ride back along the route for 150 m:** the dark header with the U-turn.
4. **If path data is installed, leave the route:** the turn arrow with its distance, the street and the distance left.
5. **TalkBack or VoiceOver once:** the header reads e.g. "Next 32, 4.4 km, then 25", and Stop is read as "Stop".
