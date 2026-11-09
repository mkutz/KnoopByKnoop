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
| Same layout in every language | ✅ | ✅ | English and German look the same, apart from the decimal comma ([German](screenshots/android-on-the-way-german.png)) |
| Far from the next knot with a screen reader | label | label | The header is one element labelled "Next 06, 5.5 km, then 02" (iOS) and "Next 02, 5.6 km, then the end" (Android), read from the accessibility tree; TalkBack ran on the emulator but logs no spoken text, so listening is the maintainer's step 5 |
| Close to the next knot with a screen reader | label | label | "At 06 follow 02 back, 205 m", "At 02 follow 06 sharply to the right, 390 m"; German "Bei 06 folge 02 zurück, 205 m" |
| Direction in the header | ✅ | ✅ | Dark header, to-the-route symbol, arrow, "420 m" ([Android](screenshots/android-off-route.png), [iOS](screenshots/ios-off-route.png)) |
| Back to the route, not to the knot | unit test | unit test | `NavigationTest` |
| Off the network | ✅ | ✅ | No badge east of the Agger |
| Wrong turn within the network | unit test | unit test | `NavigationTest` |
| Standing still | unit test | unit test | `NavigationTest` |
| Without courses | unit test | unit test | `NavigationTest` |
| Direction with a screen reader | unit test | unit test | As above |
| In the header (guided way) | ✅ | ✅ | Turn arrow, "340 m", distance left ([Android](screenshots/android-on-the-way.png), [iOS](screenshots/ios-on-the-way.png)). `GuidedWayTest` for the street |
| Header with a screen reader (guided way) | unit test | unit test | `GuidedWayTest` |
| Number to follow at the largest text size | ✅ | ✅ | iOS (Accessibility XXXL): 06, →, 02 on its plate and Stop in a row, "205 m" below, after a fix ([iOS](screenshots/ios-largest-close.png)). Android at font size 2.0, its largest: all in one row, in English and German ([Android](screenshots/android-largest-close.png), [German](screenshots/android-largest-close-german.png)) |
| Next knot at the largest text size | ✅ | ✅ | 06 and › 02 in a row, "5.5 km" below on iOS ([iOS](screenshots/ios-largest-far.png)); one row on Android |
| Number to follow at the default text size | ✅ | ✅ | |
| Off route in German | ✅ | ✅ | 1.7 km east of the route, with path data so on a guided way: Android shows "290 m", "Talweg" and "2,2 km" at its largest font ([Android](screenshots/android-largest-off-route-german.png)); iOS at Accessibility XXXL showed "…" for the turn's distance, fixed: "360 m" complete, the street cut short, the distance left left out at the accessibility sizes ([iOS](screenshots/ios-largest-off-route-german.png)). At the default size German and English have the same height |
| Wrong direction in the panel | ✅ | ✅ | Riding 250 to 400 m back along the route: the dark header with the U-turn, the next knot and the distance ([iOS](screenshots/ios-largest-wrong-way.png), [Android German](screenshots/android-largest-wrong-way-german.png)). `NavigationTest` |
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
| Stop in every language | ✅ | ✅ | Same size in German, named "Beenden" ([iOS German](screenshots/ios-close-german.png), [English](screenshots/ios-close-english.png)) |
| Stop with a screen reader | label | label | "Stop", in German "Beenden", in the accessibility tree on both; not yet heard |

Checked again on 2026-11-09 on the simulator and the emulator, with mocked rides along 02–06 near Siegburg built from the installed course data, at the largest text sizes and in German and English. Both found layout problems at the largest sizes on iOS, now fixed: the distance sat between the knot and the arrow instead of below the row, and on a guided way the turn's distance was cut to "…". Android's header had the same row order problem in principle (a `FlowRow` wrapping in order); it now takes the same layout, though at Android's largest font the rows still fit in one line.

The maintainer on 2026-11-09, on the way from a start at home: on the Pixel, the guided row with "160 m", "Kronprinzenstra…", "340 m" and › 02 was too tight, the 02 running under Stop; on the iPhone the same header looked good. Android's guided row now leaves out the distance left where the row does not fit with it. The header also keeps a 12 dp gap before Stop, as the badge's corner symbol touched it even where the row fit; the maintainer: "Looks good now".

## For the maintainer

1. **A German ride:** watch the header far from a knot, close to one (the sign plate), at the end, after a wrong turn within the network (the small badge), and after leaving the network (no badge).
2. **A start from home:** the neutral tile with the arrow pointing to the route, until you reach it.
3. **Ride back along the route for 150 m:** the dark header with the U-turn.
4. **If path data is installed, leave the route:** the turn arrow with its distance, the street and the distance left.
5. **TalkBack or VoiceOver once:** the header reads e.g. "Next 32, 4.4 km, then 25", and Stop is read as "Stop".
