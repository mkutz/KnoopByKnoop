# Manual checks

## How it was checked

Checked on 2026-11-10 with the published network and path data, on the route 06 → 08 near Titz (4.6 km). The mock ride (`checks/fixes`, made from the published course) goes along the course every 12 m, takes a detour 400 m north after 1.2 km and comes back the same way, and ends at 08.

- **Replay of identical fixes** (`checks/Replay.kt`): the 448 fixes, one second apart, through the app loop as it was in `NavigationService` before the change and through `NavigationSession`, each way search answered at once by the real path data. Both gave the same 462 lines: every notice, the precision at every fix, the five way searches with their lengths and turns, whether each way was shown, and the end after the arrival ([output](checks/replay.txt)).
- **Android emulator** (`Medium_Phone_API_37.0`, debug builds of `13dd4c9` and `0d9b16d`): the same ride with `adb emu geo fix` once a second. Notices and precision changes the same in both runs: Next 08, Off route, Turn back now, Back on route, 08 ahead, the end, You've arrived ([before](checks/android-ride-before.txt), [after](checks/android-ride-after.txt)). The ways differ in length because the fixes came at a different pace in the two runs (the first ran while iOS was building), so each search started at another place; the replay above rules out a difference in the logic.
- **iOS simulator** (iPhone 18 Pro, debug builds of `c777357` and `d41c927`): the same ride with `simctl location start`, Navigate tapped by a throwaway UI test in the scratchpad. Notices the same up to "Back on route" in both runs ([before](checks/ios-ride-before.txt), [after](checks/ios-ride-after.txt)); both rides were cut there by the script stopping the simulated location too early. The rest was ridden again with the session build: "08 ahead, the end", "You've arrived", then navigation ended, the planner back with the route and no location indicator ([while navigating](screenshots/ios-navigating.png), [after arriving](screenshots/ios-arrived.png)). Restarting the ride 1 km back along the course gave "Wrong direction, turn around" first, as it should.
- **Tests:** `NavigationSessionTest`, `WayFinderTest` and `BarMarksTest` on the JVM and on the iOS simulator.

## Scenarios covered by the core tests

| Scenario | Test |
|---|---|
| `knot-navigation` "Arriving" | `NavigationSessionTest`: arrival spoken, ends after speech, later fixes and ways change nothing; emulator and simulator rides |
| "Starting away from the route" | `NavigationSessionTest`: the first fix off route searches, the way found is shown |
| "Path data arrives on the way" | `NavigationSessionTest`: no way, no search before the interval, a search after it; `WayFinderTest`: squares installed after a search are read at the next one |
| A way found after it stopped mattering | `NavigationSessionTest`: back on route, the late way is not shown |
| Positions delivered again | `NavigationSessionTest`: nothing told again |
| "Ridden track" | `NavigationSessionTest`: the route's network sets the speed limit of the track |
| `app-languages` "Language setting" | `NavigationSessionTest`: a running navigation switches its texts (wired on Android only, see the proposal) |
| Lock screen bar and turn arrows | `BarMarksTest`: ranking, only ahead, the limit, every turn's angle |

## For the maintainer

A ride on each phone with a wrong turn, a way back along paths and arriving: the notices, the guided way, the lock screen (Android notification, iOS Live Activity, its bar marks and turn arrows) and the end should be as before.
