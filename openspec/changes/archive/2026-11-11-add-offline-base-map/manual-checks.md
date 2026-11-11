# Manual checks

Android on the emulator (`Medium_Phone_API_37.0`), 2026-11-11; all scenarios also checked by the maintainer on the Pixel and the iPhone the same day, route 06 → 02 → 28 near Siegburg (10 km).

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Route planned at home, Map loaded | ✅ | ✅ | "Map for this route is loaded" under the ways ([screenshot](screenshots/android-map-loaded.png)); both regions complete, 16.4 MB, most of it fonts, which MapLibre stores in full |
| Riding without a connection, light | ✅ | ✅ | Airplane mode, the cache of viewed tiles cleared (temporary code), the app restarted: streets, the Agger, the A3 along the route ([screenshot](screenshots/android-offline-light.png)) |
| Riding without a connection, dark | ✅ | ✅ | The same in dark mode ([screenshot](screenshots/android-offline-dark.png)) |
| Restarting the app | ✅ | ✅ | Both shots above are after a restart in airplane mode |
| Requests name the app | ✅ | ✅ | A temporary interceptor log: all 282 requests sent `knoopbyknoop-android (https://github.com/mkutz/knoopbyknoop)` |
| Requests at a time | ✅ | ✅ | The same log counted the dispatcher's running calls: 4 at most, once 5 while one was finishing |
| Opening the info sheet with a base map | ✅ | ✅ | "Map", 16.4 MB, Delete and the explanation ([screenshot](screenshots/android-info-sheet.png)) |
| Base map delete button with a screen reader | ✅ | ✅ | Accessibility tree: "Delete the stored map" |
| Deleting the base map | ✅ | ✅ | "None stored" afterwards ([screenshot](screenshots/android-deleted.png)) |
| Map not available | ✅ | ✅ | After deleting, restarted in airplane mode: "Map for this route couldn't be loaded", knots and route shown ([screenshot](screenshots/android-map-unavailable.png)) |
| Changing the route, A new route, Starting before the download is done, Without network access, Map still loading, Map status in all languages, Explaining the base map, No base map stored | ✅ | ✅ | Checked by the maintainer on the Pixel and the iPhone, 2026-11-11 |

Found and fixed on the way:
- **Offline, the map never tried the base map.** It went straight to the plain background. Now it loads the base map first, also offline, and falls back after the 3 s timeout as before.
- **The dark map took longer than 3 s offline on the emulator.** The app now tries the base map once more after falling back, also offline. The plain background stays until it has loaded.
- **The info sheet didn't scroll.** With the new entry it no longer fit on the emulator's screen; it scrolls now.

The emulator's network dropped twice during the check (DNS failures); toggling airplane mode brought it back.
