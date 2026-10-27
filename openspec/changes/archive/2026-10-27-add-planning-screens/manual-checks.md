# Manual checks

Scenarios that the core's tests cannot cover, checked by hand on each platform (tasks 3.7 and 4.7).

## Android

Emulator `Medium_Phone_API_37.0` (Android 17, software rendering), 2026-10-26, debug and a temporary release-like build.

| Scenario | Result | Notes |
|---|---|---|
| First start (`region-management`) | ✅ | Lists Belgium, Netherlands, Nordrhein-Westfalen with size and data date |
| Leaving the app during a download | ✅ | Started Belgium, went to the home screen; the system showed the download notification; Belgium was installed on return. The app process stayed alive in the background; a download finishing while the process is gone is picked up on the next start (not observed) |
| Interrupted download | ✅ | Airplane mode at about 85 % of Nordrhein-Westfalen: progress stopped; after turning it off the download continued and the region was installed |
| No network access (region list) | ✅ | Only the Netherlands listed, with "Other regions need network access."; the full list returns when opening the region list online |
| No network access (map) | ✅ | Network, knot numbers and planning work offline. The base map came from MapLibre's cache; the fallback for a base map that cannot load at all was not triggered |
| Attribution | ✅ | MapLibre attribution button and "© OpenStreetMap contributors" always visible |
| Cycling network of the Netherlands, planning by tapping | ✅ | 05 → 30 gives `05 → 04 → 30`, 5.1 km along the actual paths; tapping 55 adds it as a stop |
| Reorder, remove, restart, switching to hiking | ✅ | Long-press drag, swipe to remove (disabled at two waypoints), route restored after a restart with the map zoomed to it, confirmation before switching networks |
| Map performance, Dutch hiking network | ✅ with caveat | 38,000 knots and 500,000 course points draw and pan fine. On the slow emulator, the first load after installing takes about 40 s with a debug build; the app now caches the map layers, so later cold starts show the map after about 5 s. Needs a check on a real phone |

Found and fixed during the check:

- Downloads saved under another file name by the system were not found; finished downloads are now located through the download manager
- Loading a network blocked the main thread for about 19 s; it now loads in the background, GeoJSON is written as text instead of an object tree, and the map layers are cached per installed data
- Taps while zoomed out selected knots that are not drawn yet; taps only select knots from the zoom level knots are drawn at
- The region list did not recover from being offline until a restart

## iOS

Simulator iPhone 18 Pro (iOS 27), 2026-10-26. Taps cannot be scripted in this setup, so the Dutch network and a saved route were placed into the app's storage for the map checks.

| Scenario | Result | Notes |
|---|---|---|
| First start (`region-management`) | ✅ | Lists Belgium, Netherlands, Nordrhein-Westfalen with size and data date |
| Cycling network of the Netherlands | ✅ | Network with knot badges along the actual paths over the base map, about 2 s after start |
| Restart (restoring a route) | ✅ | Route 05 → 55 → 30 restored as 14.4 km, the same route as on Android; the map shows the whole route |
| Attribution | ✅ | MapLibre attribution and "© OpenStreetMap contributors" visible |
| Planning by tapping, switching to hiking (by the maintainer) | ✅ with bug | Routes plan in cycling and hiking. The first tap seemed to do nothing: the start was set but its marker not drawn, so a second tap made the same knot the end and created a round trip. Fixed: waypoints are redrawn whenever they change |
| Opening another region (by the maintainer) | ⚠️ | The map zoomed out completely instead of to the region; left as is, since `seamless-regions` replaces opening regions |
| Leaving the app during a download, interrupted download, no network access | – | Not reported separately on iOS; downloads worked when opening regions |

The maintainer's feedback on the planner's readability (zoom buttons, route knots, markers, direction, sheet) went into the change `improve-planner-map`.

Found and fixed during the check: the map now zooms to the whole route instead of its waypoints (on Android too), and the knot sequence wraps instead of being cut off.
