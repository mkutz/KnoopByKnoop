# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`), dark mode with `cmd uimode night`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Details on the dark map | ✅ | ⏳ | Renkum, the same view light ([screenshot](screenshots/android-renkum-light.png)) and dark ([screenshot](screenshots/android-renkum-dark.png)): the Nederrijn and the lake blue, woods and meadows dark green, roads light, dotted footpaths visible; Arnhem centre ([screenshot](screenshots/android-arnhem-dark.png)): streets, rails, footpaths, POIs, light labels |
| Dark mode | ✅ | ⏳ | Map, panel, search, network switch and buttons dark |
| Appearance in the info sheet | ✅ | ⏳ | "Appearance" with System, Light, Dark, System selected by default ([screenshot](screenshots/android-appearance-setting.png)) |
| Choosing light | ✅ | ⏳ | System dark, Light chosen: map, panel, controls and status bar light, the map in the same place |
| Choice kept | ✅ | ⏳ | Restarted with Light chosen and the system dark: light ([screenshot](screenshots/android-light-chosen-system-dark.png)) |
| Back to the system, Choosing dark | ✅ | ⏳ | System chosen: dark with the system dark, light after switching the system to light; Dark chosen with the system light: dark |
| Switching while the app is open | ✅ | ⏳ | The appearance changes without moving the map |

### Found and fixed

- The wetland areas stayed bright on the dark map: their pattern comes from the shared sprite and cannot be recolored; patterned areas are dimmed ([first version](screenshots/android-renkum-dark-first.png)).
- iPhone: MapLibre warned at startup that `UIViewController.automaticallyAdjustsScrollViewInsets` is deprecated; the map view now sets `automaticallyAdjustsContentInset` explicitly (to `true`, what the fallback did), so the warning should be gone.
- iPhone: with Light chosen and the system dark, the planner sheet (and with it the info) stayed dark: a sheet keeps the system's appearance unless it is set on the sheet itself. The chosen appearance is now applied to the root, the planner sheet and the ferry popover (`AppAppearance.swift`).
- Maintainer: street names were hard to read on the lighter streets. Liberty sets no halo color for them, so MapLibre's transparent default applied; labels along lines now get a dark halo at least 1.5 wide, and the streets are a little darker (55 % instead of 65 % lightness).

### iOS steps for the maintainer

1. In dark mode, look at a town and the countryside with rivers and woods: blue water, green woods, light streets with readable names, visible footpaths.
2. Info sheet: "Appearance" with System, Light, Dark. Choose Light while the system is dark: the whole app turns light, also the sheet and the status bar; the map stays where it was.
3. Restart the app: still light.
4. Choose Dark while the system is light: dark. Choose System: follows the system again, also when it switches.
5. In Xcode's console at startup: no MapLibre warning about `automaticallyAdjustsScrollViewInsets`.
6. Navigate a route in dark mode: knots, route, the rider and the strip stay distinguishable.
