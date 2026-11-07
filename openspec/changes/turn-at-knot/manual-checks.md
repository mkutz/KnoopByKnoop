# Manual checks

## How it was checked

Checked on 2026-11-07 with the locally built cycling data, on the route Troisdorf 25 → 32 → 29, which turns back at 32:

- **Android:** the emulator with the debug build.
- **iOS:** the simulator, with navigation started by a temporary launch argument, removed again before committing.
- **Afterwards:** both devices got their own data back.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Right turn at a knot | unit test | unit test | `NavigationTest`: a route turning south at 45 shows ↱ and the title "45 → ↱60" |
| Straight on | unit test | unit test | `NavigationTest` |
| Slight left | unit test | unit test | `NavigationTest`, 40° to the left |
| Turning back at an out-and-back knot | ✅ | ✅ | 150 m before 32, the plate around 25 shows the U-turn, in the header and the Pixel's notification icon; title "32 → ↩25" ([Android](screenshots/android-turn-back-at-32.png), [notification](screenshots/android-turn-back-notification.png), [iOS](screenshots/ios-turn-back-at-32.png)). `NavigationTest` |
| Knot right after the next one | unit test | unit test | `NavigationTest`, 46 10 m after 45 |
| Knot with several signposts | — | — | Measured from where the route arrives at the knot to where it leaves it, so between two signposts by construction; no test with real signpost courses yet |
| Without courses | unit test | unit test | `NavigationTest`: plain → |
| Start knot | unit test | unit test | `NavigationTest` |
| Far from the knot | unit test | unit test | `NavigationTest`: no plate, no turn |
| Turn on the lock screen | unit test | — | Title with the turn character; the Live Activity's plate takes the same arrow (code, not seen in a real Live Activity) |
| Turn with a screen reader | unit test | unit test | "At 45 follow 60 to the right, 150 m" |

## For the maintainer

On a ride, compare the plate's arrow with the real signpost at five knots, including a roundabout or offset junction if the ride passes one, and Siegburg 02 with its two signposts.
