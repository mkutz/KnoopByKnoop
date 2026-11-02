# Manual checks

## Scenarios

Checked 2026-11-02 with the published data on the round trip 02 → 28 → 27 → 28 → 02, around the fork by 28 from the maintainer's video. Android on the emulator (`Medium_Phone_API_37.0`), positions mocked with `adb emu geo fix`; iOS in the simulator (iPhone 18 Pro, iOS 27), with navigation started by a temporary launch flag (removed again) and `simctl location start`. Route, camera and appearance of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| A stretch used twice | ✅ | ✅ simulator | Two lines with a gap, arrows opposite ways ([Android](screenshots/android-dark-and-zoomed-out.png), left, dark; [iOS](screenshots/ios-light-dark-plan-nav.png), light and dark) |
| A stretch used once | ✅ | ✅ simulator | One line, beside the road's line |
| Tapping the route when zoomed out | not run | not run | Two tries missed: the map fits the route on start, and then opened elsewhere. The tap's lookup is unchanged; to be tried by hand, see the steps |
| Which way at a fork | ✅ | ✅ simulator | The leg ahead thick with arrows, the way back thin on the other side of the road ([close-up](screenshots/android-fork-close-up.png)) |
| Passing a knot | ✅ | unit test | Past 28: "Next 27, then 28", the leg to 27 stands out ([ride](screenshots/android-ride-fork-past-off-stop.png), second) |
| Off route | ✅ | unit test | "Off route, next 27", the leg stays (third) |
| Ending navigation | ✅ | — | After Stop, the route as while planning (fourth) |
| Before the first knot | unit test | unit test | `NavigationTest` |
| Starting on a stretch used twice | ✅ | unit test | Before the fix, this route began with "02 is the end"; now "Next 28, then 27". `NavigationTest` |
| Dark mode | ✅ | ✅ simulator | Planning and at the fork |

### Steps for the maintainer

1. Plan a round trip that uses a stretch both ways: two lines side by side, arrows opposite ways. Zoom far out and tap the line: the map zooms to the route.
2. On a ride to a fork on such a route: only the leg to the next knot is thick with arrows, and the rest is thin.
3. Check sharp bends for kinks in the line, and whether the thin rest of the route is still easy enough to see.

### Ride of 2026-11-02

Step 1: two lines side by side in planning ✅ on the iPhone; zooming out and tapping stays open. Step 2 ✅ on the Pixel: at 01, the leg ahead turns thick with arrows the other way. Step 3 not looked at closely. Waits for `routes-through-signposts`.
