# Manual checks

## How it was checked

Checked on 2026-11-07 with the locally built cycling data, on the route Troisdorf 25 → 32 → 29, which turns back at 32:

- **Android:** the emulator with the debug build.
- **iOS:** the simulator, with navigation started by a temporary launch argument, removed again before committing.
- **Afterwards:** both devices got their own data back.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Right turn at a knot | ✅ | ✅ | 04 → 06 → 02, 66 m before 06: 02's plate with →, title "06 → ↱02" ([Android](screenshots/android-06-header.png), [notification](screenshots/android-06-notification.png), [iOS](screenshots/ios-right-at-06.png), [Live Activity](screenshots/ios-live-activity-right.png)). Also: `NavigationTest`: a route turning south at 45 shows ↱ and the title "45 → ↱60" |
| Straight on | ✅ | ✅ | 06 → 02 → 01 at Siegburg 02: 01's plate with ↑, title "02 → ↑01" ([Android](screenshots/android-02-header.png), [notification](screenshots/android-02-notification.png), [iOS](screenshots/ios-straight-at-02.png), [Live Activity](screenshots/ios-live-activity-straight.png)). Also `NavigationTest` |
| Slight left | unit test | unit test | `NavigationTest`, 40° to the left |
| Turning back at an out-and-back knot | ✅ | ✅ | 150 m before 32, the plate around 25 shows the U-turn, in the header and the Pixel's notification icon; title "32 → ↩25" ([Android](screenshots/android-turn-back-at-32.png), [notification](screenshots/android-turn-back-notification.png), [iOS](screenshots/ios-turn-back-at-32.png)). `NavigationTest` |
| Knot right after the next one | unit test | unit test | `NavigationTest`, 46 10 m after 45 |
| Knot with several signposts | unit test | unit test | `NetworkGeometryTest` with a built network. With the real data, no route through Siegburg 02 changes signpost: a probe of every pair of 02's neighbours (06, 01, 28) arrives and leaves at the same one, at the river signpost or, for 06 → 02 → 28, at the other |
| Without courses | unit test | unit test | `NavigationTest`: plain → |
| Start knot | unit test | unit test | `NavigationTest` |
| Far from the knot | unit test | unit test | `NavigationTest`: no plate, no turn |
| Turn on the lock screen | ✅ | ✅ (lock screen) | Title with the turn character; the Live Activity's plate takes the same arrow (code, not seen in a real Live Activity) |
| Turn with a screen reader | unit test | unit test | "At 45 follow 60 to the right, 150 m" |

Checked on 2026-11-09 on the emulator and in the simulator with mocked rides along 04 → 06 → 02 → 01, built from the installed course data. The Dynamic Island could not be seen: the simulator's screenshots leave it empty. A turn seen today and first taken for a bug: on 06 → 02 → 06 the plate at 02 shows a sharp right, not a U-turn, because the route arrives from the north heading south and leaves on another path heading north-west.

## For the maintainer

On a ride, compare the plate's arrow with the real signpost at five knots, including a roundabout or offset junction if the ride passes one, and Siegburg 02 with its two signposts.

## Dynamic Island in the simulator (task 3.1), 2026-11-11

The iPhone 18 Pro simulator does show the island once the app is in the background: a UI test started navigating on 32 → 06 → 02 from 32, 156 m before 06, where the route turns left towards 02.

- **Expanded** (long press): 06, 156 m → the plate of 02 with ← ([screenshot](screenshots/ios-sim-island-expanded-left.png)).
- **Compact:** before this check it showed only the sign of the number to follow, without the turn ([before](screenshots/ios-sim-island-before-arrow.png)). It now shows the sign with the turn's arrow next to it, without the plate, which the narrow region would cut off ([after](screenshots/ios-sim-island-compact-left.png)). The simulator still cuts the sign's left edge, as it did the plain sign before. How it looks around the real camera is for the iPhone.
- **Minimal** (shared with another app's activity): the number only, as before ("Dynamic Island with room for one number").

