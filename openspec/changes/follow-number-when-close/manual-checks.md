# Manual checks

## Scenarios

Android checked 2026-11-01 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the installed cycling courses. Route 93 → 94 → 29 in Arnhem, so after 94 comes 41. Positions were mocked with `adb emu geo fix` along the course from 93 to about 150 m before 94. The emulator's route, app language and font scale were restored afterwards.

iOS checked the same way in the simulator (iPhone 18 Pro, iOS 27), with navigation started 30 s after launch by a temporary launch flag (removed again), and the location moved along the same points with `simctl location start`. The simulator's route, appearance and text size were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Far from the next knot | ✅ | ✅ simulator | 418 m before 94: "Next 94 418 m", "then 41" smaller below ([Android](screenshots/android-far-and-close.png), left; [iOS](screenshots/ios-far-largest-and-close.png), left) |
| Coming close | ✅ | ✅ simulator | The reminder "94 ahead, then 41", and "At 94 follow 41" with 41 biggest at 156 m (right). The panel keeps its height |
| Knot right after the last one | unit test | unit test | `NavigationTest` "knot right after the last one" |
| Next knot in the middle of the route | ✅ | ✅ simulator | As "Coming close" |
| Next knot is the end | unit test | unit test | Unchanged, `follow` is `null` |
| Distance off route | — | — | Unchanged, checked in `improve-off-route` |
| Distance to the first knot | unit test | unit test | `NavigationTest` "distance to the first knot": close, so "At 24 follow 45" |
| Far from the next knot with a screen reader | ✅ | — | `uiautomator dump`: "Next 94, 418 m, then 41"; in German "Nächster 94, 418 m, dann 41". iOS builds the same label; VoiceOver on the iPhone |
| Next knot at the largest text size | ✅ | ✅ simulator | Android in German at font scale 2.0: "Nächster 94", "418 m" below, "dann 41", "Beenden" complete ([screenshot](screenshots/android-far-german-largest.png)). iOS at the largest accessibility size: Stop above, "Next 94 418 m", "then 41" complete (second from left); before, Stop was cut to "St…" ([screenshot](screenshots/ios-stop-cut-off.png)) |
| Number to follow at the largest text size | — | — | Unchanged layout |
| Number to follow at the default text size | ✅ | ✅ simulator | The distance stays in the line |
| Off route in German | — | — | Unchanged, checked in `fix-ride-display-bugs` |

### Steps for the maintainer

1. On a ride with the Pixel and the iPhone: far from the next knot, the header shows "Next" with the knot and its distance big, and "then" with the number to follow small.
2. About 200 m before the knot, with the spoken reminder, it switches to "At … follow …" with the number to follow big. The panel does not jump.
3. With VoiceOver on the iPhone, the far header reads "Next …, … m, then …".
