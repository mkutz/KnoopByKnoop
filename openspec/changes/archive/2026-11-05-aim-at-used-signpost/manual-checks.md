# Manual checks

## Scenarios

Checked 2026-11-03 at Siegburg 02 with the route 02, 28, 27 and the installed cycling data. Android on the emulator, iOS in the simulator (iPhone 18 Pro), with mocked positions. The route and camera of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Approaching the first knot at the signpost the route leaves from | ✅ off route, unit test on route | ✅ off route, unit test on route | 400 m north: 392 m, where the river 02 would give 551 m. On the path from the river 02: 117, 87 and 56 m, shrinking. The state "At 02 follow 28" in `NavigationTest` |
| Dashed line to the first knot | ✅ | ✅ | Ends at the north-west 02 ([Android](screenshots/android-aim-at-used-signpost.png), [iOS](screenshots/ios-aim-at-used-signpost.png)) |
| Off route, heading for a knot on the route | unit test | unit test | `NavigationTest` |
| The route uses the other signpost | unit test | unit test | `NavigationTest` |
| Line through both signposts | replay, unit test | unit test | `NavigationTest` in both directions, and 28, 02, 01 and 01, 02, 28 replayed through the core on the JVM with the real data |
| Reminder at the signpost used | unit test | unit test | `NavigationTest`. Sampling near the signpost too |
| Passing the other signpost | unit test | unit test | `NavigationTest` |
| A knot with one signpost | unit test | unit test | `NavigationTest` |
| Without courses | ✅ | unit test | Navigation started before the courses had loaded: 551 m to the river 02, and 78, 109, 139 m growing on the path. See below |

Passing the north-west signpost gave "Next 28" on both.

The tests of the new behavior fail without the fix. The others guard behavior that must not change.

### Found on the way

On the emulator, the courses took more than 25 s to load after the start of the app. Pressing Navigate before that starts navigation without courses for the whole ride: the panel never says off route, and the distances go to main positions. iOS passes the courses the same way, but this was not tried there. Not part of this change, and not what happened on the ride of 2026-11-02: that recording shows the courses loaded.

### Steps for the maintainer

These also cover riding through both signposts: 28, 02, 01 gives the reminder before the north-west 02, and 01, 02, 28 gives it before the river 02 and "Next 28" there. When 02 counts as passed at the north-west signpost, the panel's distance still jumps from about 245 m to the next knot. That comes from distances along the route, which are out of scope.

1. On a ride at Siegburg 02 on the Pixel and the iPhone, with 02, 28 as the start of the route: start navigating away from the route, and ride to the north-west 02. The distance shrinks all the way, and the dashed line ends there.
2. Ride on past it: "Next 28" at the signpost.
