# Tasks

## 1. Core

- [x] 1.1 `RiddenTrack`:
  - knows the route's network;
  - adds a fix only when it is more than `max(5 m, its accuracy + the last point's accuracy)` from the last point;
  - drops a fix faster than 15 m/s (cycling) or 4 m/s (hiking) from the last point;
  - takes out the last point when the track turns sharply at it far from the line between its neighbours, as in the design.
  - Tests with made-up tracks for "Standing with scattered fixes", "Scatter while walking slowly", "Impossible jump", "Spike out and back", "Riding fast with good fixes", and that "Detour" and the existing tests still pass, including the riding direction.

## 2. Apps

- [x] 2.1 Android `NavigationService` and iOS `Navigator` create the track with the route's network. Android builds, iOS builds with `xcodebuild`.
- [x] 2.2 Check on the emulator with a mocked walk that zigzags like the Siegburg screenshot: the track has no spikes. Same in the simulator.
  - Checked 2026-11-06 on the emulator: 02 → 06 in Zange, 44 fixes every 2 s, every fourth 60 m to the side; the track stays on the walked line. The newest point can be a spike until the next fix shows the track going on. The emulator's fixes report a tiny inaccuracy, so the scatter rule is covered by the core tests only. Not repeated in the simulator: the change is in the core, and iOS only passes the network like Android.

## 3. Release

- [x] 3.1 Changelog line, and a manual check in `manual-checks.md`: walk or ride through the Siegburg old town with the screen off, then look at the track.
