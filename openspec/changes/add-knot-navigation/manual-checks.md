# Manual checks

## Android

Emulator `Medium_Phone_API_37.0`, 2026-10-28, on the route 08 → 31 → 97 at the Kaag (5.2 km, two ferries) with the published courses. Rides are simulated with `adb emu geo fix` along the route's courses, one fix per second at 6 m/s, with the screen off; the navigation's notices and precision changes are read with `adb logcat -s Navigation`. The emulator plays no sound reliably, so speech is checked by the notices in the log and the speech engine's audio streams.

| Scenario | Android | Notes |
|---|---|---|
| Start at the first knot | ✅ | "Next 31, then 97" at 08 ([screenshot](screenshots/android-navigating-map.png)) |
| Location access denied | ✅ | "Don't allow" in the system dialog: "Knoop by Knoop needs location access to navigate.", navigation does not start ([screenshot](screenshots/android-location-denied.png)) |
| Screen off | ✅ | The whole ride with the screen off, 14:33 to 14:47: "31 ahead, then 97", "Next 97", "97 ahead, the end", "You've arrived" |
| Arriving | ✅ | "You've arrived" at 97, then the service stops and the notification goes away |
| Lock screen | ✅ | The ongoing notification shows "At 31 follow 97" with Stop, among the normal notifications ([screenshot](screenshots/android-notification.png)) |
| Off route outside the app | ✅ | 2 km west of the route: "Off route, next 31" spoken, the notification shows it, the precision switches to `FINE` |
| Stop from the lock screen | ✅ | Stop in the notification: the service and the notification are gone, all location providers are off, the route is still planned |
| Looking at the map | ✅ | The map moves to the rider when navigation starts and when the app is opened while navigating; the next knot gets a ring |
| Off route on the map | ✅ | The map shows the rider ([screenshot](screenshots/android-off-route-map.png)); since then, off route it fits the rider and the next knot |
| Tapping a knot while navigating | ✅ | Tapping 08 on the map while navigating: the saved route is unchanged |
| Stopping in the app | ✅ | Stop in the header ends navigation, the route stays |
| Music playing | ⏳ | Needs a phone with music and sound |

## iOS

Simulator (iPhone 18 Pro, iOS 27.0), 2026-10-28, the same route, rides simulated with `simctl location start` at 6 m/s, the app in the background behind Settings; notices from the unified log, category `Navigation`. iPhone checks by the maintainer: ⏳.

| Scenario | Simulator | iPhone | Notes |
|---|---|---|---|
| Start at the first knot | ✅ | ⏳ | "At 31 follow 97", the map moves to the rider ([screenshot](screenshots/ios-simulator-navigating.png)) |
| Location access denied | ⏳ | ⏳ | |
| Screen off | ✅ | ⏳ | In the background 14:16 to 14:31: "31 ahead, then 97", "Next 97", "97 ahead, the end", "You've arrived", heard by the maintainer |
| Arriving | ✅ | ⏳ | "You've arrived", then the Live Activity is gone |
| Lock screen | ✅ | ⏳ | "At (31) follow (97)" with badges like on the map, 97 largest, and Stop ([screenshot](screenshots/ios-simulator-lock-screen.png)) |
| Dynamic Island | ✅ | ⏳ | Compact: "31 →" and "97"; at the end "97" and a flag; off route a warning sign and the next knot |
| Off route outside the app | ⏳ | ⏳ | |
| Stop from the lock screen | ⏳ | ⏳ | |
| Music playing | – | ⏳ | Spotify was lowered while the spike spoke, see `spike.md` |
| Looking at the map, Off route on the map | ⏳ | ⏳ | |
| Tapping a knot while navigating, Stopping in the app | ⏳ | ⏳ | |
| One hour with the screen off | – | ⏳ | Battery level at start and end of a ride of at least one hour |

## Found and fixed

- **iOS: blue location indicator after navigation** (found by the maintainer on the iPhone, after arriving, after Stop and after closing the app). The system keeps a `CLBackgroundActivitySession` beyond the app's end; one left over from a navigation the app did not end, e.g. when Xcode replaced the app or it was swiped away while navigating, kept the indicator for good. The app now ends such a session at start when it is not navigating. To check on the iPhone: start navigating, swipe the app away, start it again; the indicator goes away (✅ on the iPhone, 2026-10-29).

- **Android: no fixes with `COARSE`.** Balanced power gave no fixes at all on the emulator (no Wi-Fi or cell towers), so navigation never saw 31. A timer to fall back to `FINE` did not help either: with the screen off and no fixes, the device sleeps and so does the timer. `COARSE` now uses high accuracy every 30 s on Android (design, "Precision instead of intervals").
- **Android: notification hidden.** With low importance, the notification was filed under "Silent", collapsed with its Stop button hidden, and Android hides silent notifications on the lock screen by default. It now has default importance without sound.
- **Android: `startForeground` with a service type needs API 29** (lint); now `ServiceCompat`.
- **Both: the map stayed where it was** when navigation started or the app was opened while navigating; the rider was off screen. It now moves to the rider, and off route fits the rider and the next knot.
- **iOS: Dynamic Island "97 97".** At the last knot the compact view showed the next knot twice; "At 31" did not fit either. Now arrows, a flag and a warning sign.
- **iOS: lock screen text hard to read.** White text on the light card; now dark.
