# Manual checks

## Scenarios

Android checked 2026-11-02 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the published data: route 02 → 28 → 27 from Siegburg, positions mocked along the course of 02 → 28 with `adb emu geo fix` at 8 m/s, from 500 m to 1000 m, back to 700 m, forward to 780 m. iOS checked the same way in the simulator (iPhone 18 Pro, iOS 27), with navigation started by a temporary launch flag (removed again) and `simctl location start`. Route, camera, language, text size and appearance of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Riding back along the route | ✅ | ✅ simulator | Forward without a notice. The log shows "Wrong direction, turn around, next 28" once, 22 s after turning at 8 m/s, as 150 m takes 19 s. iOS: only the panel was looked at, not the log. `NavigationTest` |
| Wrong direction in the panel | ✅ | ✅ simulator | In red with the badge and "1.8 km", the dot on the strip red, no dashed line, following ([Android](screenshots/android-forward-back-shade-turned.jpg), second; [iOS](screenshots/ios-forward-back.jpg), right) |
| Wrong direction outside the app | ✅ | ⏳ | Android: "Wrong direction, next 28", "1.8 km to 28 · end around 22:30", red marker, badge 28 (third). iOS builds, a real Live Activity is for the iPhone |
| In the app's language | ✅ | unit test | German at the largest text size: complete, the badge and distance below ([screenshot](screenshots/android-dark-german-largest.jpg), right). `NavigationTest` for all languages' wording |
| Turning around | ✅ | unit test | "Back on route, next 28", then "Next 28 then 27" (Android, fourth). `NavigationTest` |
| Turning back shortly, A place that jumps, Leaving the course while riding the wrong way, Passing the next knot, Riding back again later, On the way back of an out-and-back route, Following the branch to another signpost, Before the first knot, Without geometry | unit test | unit test | `NavigationTest` |

Dark mode on Android: the panel is dark and readable ([screenshot](screenshots/android-dark-german-largest.jpg), left). The map behind it was still light 5 s after switching on the emulator, which is slow there; the appearance's behavior is unchanged by this change.

### Steps for the maintainer

1. On a ride with the Pixel and the iPhone: on a route, turn around and ride back along it. Within about 150 m: "Wrong direction, turn around, next …", spoken once, and the red header with the distance along the route.
2. Lock the phone while doing so: the notification or Live Activity shows it. iPhone: the Dynamic Island shows the warning sign and the next knot.
3. Turn around again: "Back on route, next …" after about 50 m.
4. Ride a winding stretch and a tight hairpin the right way: no warning. Say if 150 m feels too early or too late.
