# Manual checks

## How it was checked

Checked on 2026-11-08 with the published network data:

- **Android:** the emulator (`Medium_Phone_API_37.0`) with the debug build, on the route Troisdorf 25 → 32 → 29. Taps with `adb shell input tap`, the screen reader text from `uiautomator dump`. The emulator had just synced the newly published graph, so its courses were still loading and the route drew straight lines; that doesn't touch this change.
- **iOS:** the simulator (iPhone 18 Pro, iOS 27) with the debug build, loading a saved route with the café as a stop, as the simulator takes no taps here.
- **Afterwards:** both devices got their own route and camera back.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Café as a stop | ✅ | ✅ (saved route) | Food icon with the stop's corner symbol on the map and the strip, "Stop · Eiscafé Riviera" in the list ([Android map](screenshots/android-cafe-stop.png), [list](screenshots/android-list.png); [iOS map](screenshots/ios-map.png), [list](screenshots/ios-list.png)). `NetworkGeometryTest`, `RoutePointNavigationTest` |
| Place without a name as the end | ✅ | unit test | Unnamed toilets: the water icon with ■ and "End · Toilets"; the old end 29 became a stop ([Android](screenshots/android-toilets-end.png)). `NetworkGeometryTest` |
| Start that becomes a stop | unit test | unit test | `PlanningTest` |
| Kept after a restart | ✅ | ✅ | Android: icon and name after a restart ([screenshot](screenshots/android-after-restart.png)); iOS: drawn from the saved `route.json`. `PlanningTest` |
| Route saved before | unit test | unit test | `PlanningTest`: JSON without the fields |
| Spot without a place | unit test | unit test | `NetworkGeometryTest`; the flag code is unchanged |
| Places switched off | ✅ | — | The stop keeps its icon ([Android](screenshots/android-places-off.png)) |
| Screen reader in the list | ✅ | — | Android reads "Stop, Eiscafé Riviera", with "Move …" and "Remove …" by that name; the screen shows "·" |

## For the maintainer

1. **iPhone:** tap a café, "Add stop here": the stop shows the food icon on the map, the strip and in the list with the name; it stays after restarting the app.
2. **Both phones, on the map:** the stop's badge is about the size of a highlighted place icon, so the corner symbol and the route line set it apart. Say if it should be larger.
3. **On a ride:** ride to a café stop and check it reads well on the strip.
