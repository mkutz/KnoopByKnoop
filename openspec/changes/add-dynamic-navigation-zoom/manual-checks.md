# Manual checks

## Scenarios

Android checked 2026-11-08 on the emulator (`Medium_Phone_API_37.0`), debug build, app on screen, route 06 → 02 → Alt Mülldorf near Siegburg, positions from the test GPS provider every second, 10 m apart, along the real course. Its course has two tricky stretches: right after 06, the jog under the A 3 (26–103 m), and at Mühlentor in Siegburg (6517–6541 m). The debug log names each change of the map's focus. The shell's mock location permission and the test provider were removed again afterwards.

iOS checked in the simulator (iPhone 18 Pro, iOS 27), debug build, round trip 02 → 06 → 02, started by a throwaway XCUITest runner outside the repository, positions set with `simctl location` along the same course from 02.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Riding between knots | ✅ emulator | ✅ simulator | Navigation zoom between the spots |
| Tricky spot at a knot | unit test | unit test | |
| Off route | ✅ emulator | ✅ iPhone | The off-route camera is unchanged; the mocked departure stayed within 100 m of the bending course, so it was not shown |
| Approaching a knot | ✅ emulator | unit test | KNOT, 342 m across, with 02 and the route after it ([frames](screenshots/android-knot-02-frames.png)) |
| Coming closer | ✅ emulator | unit test | One zoom from the reminder to passing |
| Knot at the end of the route | unit test | unit test | |
| Next knot 800 m ahead | unit test | unit test | The route had a flag after 02, which gets the navigation zoom (route points are out of scope) |
| Next knot far ahead | unit test | unit test | |
| Next knot close | unit test | unit test | |
| Tricky spot right after the knot | ✅ emulator | unit test | Starting at 06, the jog right after it ([screenshot](screenshots/android-tricky-after-06.png)), then back ([screenshot](screenshots/android-past-tricky.png)) |
| Last knot passed | unit test | unit test | |
| Zigzag, One sharp turn, Gentle bends, Without courses | unit test | unit test | `TrickyStretchesTest` |
| Approaching a zigzag | ✅ emulator | ✅ simulator | TRICKY_SPOT, 200 m across ([Android](screenshots/android-zigzag-siegburg.png), [iOS](screenshots/ios-zigzag.png)) |
| Past the spot | ✅ emulator | ✅ simulator | Back to the navigation zoom |
| Smooth zoom changes, Labels while zoomed in | ✅ emulator | — | Screen recordings of both rides: one even move per change, labels steady while following; not kept in the repository |
| Looking around, No automatic zoom after looking around | ✅ emulator | ✅ simulator | Dragged before 02: no zoom at 02, the recenter button stays ([screenshot](screenshots/android-looking-around.png)) |
| Recentering near a knot | ✅ emulator | ✅ simulator | 2026-11-09 on 04 → 06 → 02: dragged away, 156 m before 06 no zoom, then Recenter zoomed in on 06 and the route after it ([looking around](screenshots/android-looking-around-near-06.png), [recentered](screenshots/android-recentered-near-06.png)); earlier also at the zigzag |
| Both in view | ✅ emulator | ✅ iPhone | Unchanged; on the walk of 2026-11-09, off route south of the Sieg, the map kept the rider and the end of the line in view; on the emulator 400 m east of the leg 06 → 02 on a guided way, the rider and where the way meets the route both in view ([screenshot](screenshots/android-off-route-400m.png)) |

### Steps for the maintainer

1. Ride a route with the app open on each phone, past several knots and through the jog under the A 3 after 06 at Lohmarer Brücke.
2. From 200 m before a knot the map zooms in on it and the route after it; after passing it zooms out for about 10 s to show the next knot, then back.
3. Close before a tricky spot the map zooms in on it, and back after it.
4. Note whether the zoom helped at knots and tricky spots, whether 10 s after a knot is right, and whether the map moved too much.
5. Drag the map, ride to a knot: no zoom. Tap recenter within 200 m of a knot: zoomed in on it.

## Doubtful junction off route (5.2)

Checked on 2026-11-11 on the emulator (API 37) with the debug build, replaying the recorded real ride `siegburg-going-on` from `modules/core/src/jvmTest/resources/real-rides/` with `adb emu geo fix`, app on screen: off route south-west of 02, the guided way back turns back and keeps left at a fork.

- **Found and fixed:** both apps' maps ignored the junction off route. The core chose the junction as the focus (5.1), but the map's off-route camera, which keeps the rider and the end of the way back in view, ran whenever there was a way back, so the focus never applied. Before the fix the log showed no focus change at the fork and the view stayed the same ([before, after](screenshots/android-doubtful-junction-off-route.png)). Now the off-route camera gives way to a tricky spot: at the fork the log shows "map focus: TRICKY_SPOT", and back on route "BASE". The same change in `NetworkMapView.swift`; iOS builds, not replayed.
- In this ride the rider rides away from the fork, as the way back begins with "Turn back", so the view still holds the rider and the fork and doesn't come much closer. A ride towards a fork on a way back is the maintainer's check below.
- Pressing Navigate before the courses had loaded (19 s on the emulator) gave a ride without courses and without the off-route state. That is the known "Navigate before courses" bug, its own change.
