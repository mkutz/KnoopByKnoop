# Manual checks

## Contrast in dark mode ("Readable actions in dark mode")

Computed with the WCAG 2.x formula from the colors in the code. Labels need at least 4.5:1, icons at least 3:1. Light mode is unchanged, see `clearer-planner-actions/manual-checks.md`.

| What | Colors | Ratio |
|---|---|---|
| Navigate: label and icon | white on `#237A46` | 5.32 ✅ |
| Stop: label and icon | white on `#B3261E` | 6.54 ✅ |
| Android: Back to start and Clear icons | `#8FD9AE` on `#1E3B2B` | 7.40 ✅ |
| iOS: Back to start and Clear icons, `.bordered` (tint at about 15 %) on the dark glass panel | `#8FD9AE` over `#1C1C1E` / `#2C2C2E` / `#3A3A3C` | 7.36 / 5.94 / 4.92 ✅ |

## Android

Emulator `Medium_Phone_API_37.0`, 2026-10-28, on the route 08 → 31 → 97 at the Kaag (5.2 km, two ferries). Dark mode switched with `adb shell cmd uimode night yes|no`.

| Scenario | Android | Notes |
|---|---|---|
| Dark mode | ✅ | Started in dark mode: dark map, panel, search, network switch and zoom buttons ([screenshot](screenshots/android-dark-start.png), [header](screenshots/android-dark-header.png)) |
| Light mode | ✅ | Looks as before, compare `clearer-planner-actions/screenshots/android-planning.png` |
| Switching while the app is open | ✅ | Light → dark → light while the route is shown: same area, same route; the activity is not recreated ([screenshots](screenshots/android-switching-to-dark.png): 3 s, 10 s, 25 s after switching); dark → light → dark within half a second ends on the dark map |
| Switching while navigating | ✅ | Switched to dark while navigating: navigation keeps running, the service stays and the dark header shows "At 08 follow 31" with Stop ([screenshot](screenshots/android-navigating-dark.png)). On the emulator, notices after the switch could not be checked (the simulated fixes gave none even without switching); checked by the maintainer on their Android phone |
| Route on the dark map | ✅ | Start, stop, end (white outline), the route with its arrows and the network are distinguishable ([screenshot](screenshots/android-route-dark-map.png)) |
| Offline in dark mode | ✅ | First start in airplane mode with the map cache deleted: the network on a dark background; with network access back, the dark base map ([screenshots](screenshots/android-offline-dark.png)) |
| Slow base map | ✅ | Dark mode with the map cache deleted: dark from the first frame, the fallback after 3 s, the dark base map loaded in the background ([screenshots](screenshots/android-slow-base-map.png): 3 s, 12 s, 25 s) |

## iOS

Simulator (iPhone 18 Pro), 2026-10-28, switched with `xcrun simctl ui … appearance dark|light`. iPhone checks by the maintainer, 2026-10-28.

| Scenario | Simulator | iPhone | Notes |
|---|---|---|---|
| Dark mode | ✅ | ✅ | |
| Switching while the app is open | ✅ | ✅ | Light → dark → light: map, network switch, zoom buttons, search and sheet switch, same area and route ([screenshots](screenshots/ios-simulator-switching.png)) |
| Switching while navigating | – | ✅ | Checked by the maintainer on the iPhone |
| Returning to the app | ✅ | ✅ | Dark mode, three times to Settings and back: the map stays dark. On the iPhone it alternated between dark and light before the fix, see below |
| Lock screen in dark mode | – | ✅ | Was light in dark mode (found by the maintainer), fixed, see below |
| Route on the dark map | ✅ | ✅ | |
| Slow base map | ✅ | – | The simulator loads the base map within the 3 s, even with the map cache deleted. With a temporary 0.05 s timeout, the app ends on the dark base map; the fallback in between was too short to capture, as `updateUIView` already loaded the base map again on the next update |

## Found and fixed

- **Android: crash when a style loads while the map changes.** MapLibre invalidates a style as soon as the next one starts loading; `MapHolder` kept the old one, and the next update, here the knot network arriving, crashed with "Calling getImage when a newer style is loading/has loaded". Found with the retry of a slow base map; it could also happen on an appearance switch or when network access comes back. The old style is now forgotten when the next one starts loading, on iOS too.
- **iOS: Live Activity light in dark mode.** The lock screen card had its own light background and dark text from `add-knot-navigation`, so it stayed light in dark mode (found by the maintainer on the iPhone; the proposal had wrongly assumed the system styles it). It now has a dark background and white text in dark mode; white on the card is about 17:1, the system red of "Off route" about 5:1.
- **iOS: the map alternated between dark and light when returning to the app** (found by the maintainer on the iPhone). For the app switcher's snapshots, iOS switches a backgrounded app's appearance to the other one and back. The map started loading the other style, ignored the switch back while that was loading, and ended up in the wrong appearance. Now a style that finishes loading is checked against the current appearance and replaced if it doesn't match (both platforms, also for fast switching on Android), and iOS ignores appearance changes in the background and checks again when the app is active.
- **Android: beige map before the first style.** MapLibre's own background showed in the first seconds; it is now the offline style's background, dark in dark mode.
