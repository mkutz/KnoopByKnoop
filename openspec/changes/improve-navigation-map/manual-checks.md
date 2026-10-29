# Manual checks

## Android

Emulator `Medium_Phone_API_37.0`, 2026-10-29, on the route 08 → 31 → 97 at the Kaag. Rides simulated with `adb emu geo fix`, one fix per second, each a few metres from the last: repeated identical fixes produce no location updates on the emulator. The emulator always reports course 0°, which no longer matters, since the direction comes from the movement.

| Scenario | Android | Notes |
|---|---|---|
| Riding with the map following | ✅ | The map moves with the rider, south and then east ([screenshots](screenshots/android-following.png), 1 and 5) |
| Looking around | ✅ | Dragging the map, and zooming by double tap, stop following; the map stays and "Recenter" appears below the zoom buttons ([screenshots](screenshots/android-following.png), 2). Pinch needs two fingers and can't be scripted: ⏳ on a phone |
| Recentering | ✅ | "Recenter" moves the map to the rider at zoom 15, follows again, and disappears ([screenshots](screenshots/android-following.png), 4 and 5) |
| No automatic return | ✅ | Riding on for 10 s after dragging: the map stays where it was, the button stays ([screenshots](screenshots/android-following.png), 3) |
| Off route while following | ✅ | From 08, 2 km west: "Off route, next 31", the map shows the rider and the highlighted 31 together ([screenshot](screenshots/android-off-route.png)) |
| Riding | ✅ | Arrow south while riding south, east after turning east ([screenshots](screenshots/android-direction.png)) |
| Standing still | ✅ | 15 s of fixes jittering by 1 m: the dot ([screenshots](screenshots/android-direction.png), right) |
| Detour | ✅ | The detour west is drawn in blue next to the orange route ([screenshot](screenshots/android-off-route.png), [first screenshot](screenshots/android-detour-screen-off-stop.png)) |
| Inaccurate fix | ✅ | Covered by `RiddenTrackTest` |
| Navigation ends | ✅ | After Stop, the track is gone and the service has ended ([screenshots](screenshots/android-detour-screen-off-stop.png), right) |
| Screen off | ✅ | 60 s ridden with the screen off: the track includes that stretch after waking up ([screenshots](screenshots/android-detour-screen-off-stop.png), middle); the precision changed to `COARSE` and back to `LIVE` as before |

On a real Android phone: ⏳ pinch stops following; the arrow on a real ride.

## iOS

Built for the simulator and in Release for a device; the simulator can't be tapped by script, so navigation can't be started there. iPhone checks by the maintainer on a real ride: ⏳.

| Scenario | iPhone | Notes |
|---|---|---|
| Riding | ⏳ | |
| Standing still | ⏳ | |
| Detour | ⏳ | |
| Looking around | ⏳ | Drag, pinch and double tap each stop following |
| Recentering | ⏳ | |

## Found and fixed

- **Riding direction from the phone's course replaced by the most recent movement** (maintainer's experience that the phone's direction is unreliable; also the emulator always reports 0°). Proposal, spec, design and tasks changed accordingly before the platforms were finished.
