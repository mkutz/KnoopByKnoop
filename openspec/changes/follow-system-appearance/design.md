# Design

## Context

See `proposal.md` for why.
- **Android** uses one `lightColorScheme` in `MainActivity.kt` and the window theme `Theme.Material.Light.NoActionBar`. The activity handles orientation and size changes itself, so the map isn't recreated on rotation. A `uiMode` change (light/dark) still recreates the activity.
- **iOS** forces the views over the map and the planner panel to light with `.environment(\.colorScheme, .light)` and `.preferredColorScheme(.light)`, because the map is always light.
- **Both** load OpenFreeMap's "liberty" style. They fall back to an inline style with only a light background (`#EEF1EA`) when the base map does not load within 3 s. The network, route and knot layers are added on top of whichever style is loaded, and both apps already re-add them when the style changes from the offline style to the base map.
- On the map, knot badges have white outlines, the route is orange, and the end badge is dark (`#263238`).

## Goals / Non-Goals

**Goals:**
- One appearance at a time: map, panel and controls are all light or all dark.
- Switching without losing the map's camera, the route or navigation.
- AA contrast for the actions in both appearances.

**Non-Goals:**
- A theme setting in the app.
- Restyling the knot badges beyond what readability on the dark map needs.

## Decisions

### OpenFreeMap "dark" as the dark base map
OpenFreeMap offers "dark" next to "liberty" (both checked: HTTP 200), from the same provider, without an API key. It is a neutral dark grey, on which the green network lines, the orange route and the white-outlined badges keep their colors.
*Alternative:* "fiord", a dark blue. Its blue water and background are close to the knot network's colors, and it looks less neutral next to the system's dark grey panels.

### Switch the style in place, keep the camera
Both apps pick the style URL from the current appearance and set it again when the appearance changes. The existing "style loaded" path then re-adds the network, route and knot layers, as when the base map replaces the offline style. The camera is part of the map view, not of the style, so it stays.
On Android, `uiMode` is added to the activity's `configChanges`, so the activity, the map view and the navigation's connection to the app's state are not recreated. Compose sees the new configuration and picks the dark color scheme. On iOS, the map view reads `traitCollection.userInterfaceStyle` and updates the style in `traitCollectionDidChange` (or `registerForTraitChanges`).
*Alternative:* letting Android recreate the activity. That costs a map reload with a visible flash, and the camera would have to be saved and restored.

### Dark colors per platform
- **Android:** `darkColorScheme` with the same primary and secondary, and dark surfaces from Material 3's defaults. The planner's action colors get dark variants:
  - Navigate: `#237A46` with white text, unchanged, since a filled button reads on both backgrounds.
  - Stop: `#B3261E` with white text, unchanged.
  - Back to start and Clear: a light green icon on a dark green container, e.g. `#8FD9AE` on `#1E3B2B`. The exact values are set during apply and checked by computation (≥ 3:1).

  The window theme gets a `values-night` variant (`Theme.Material.NoActionBar`) so the window background before Compose draws is dark too.
- **iOS:** the forcing of light is removed. `actionGreen` and `actionRed` stay for the filled buttons. The `.bordered` icon buttons use an adaptive color: `actionGreen` in light mode, a light green in dark mode, as a `Color(UIColor { traits in … })`.

### The Live Activity by appearance
The lock screen card sets its own light background (`activityBackgroundTint`) and dark text, chosen during `add-knot-navigation` because white text on the system's light card was hard to read. It now reads the appearance from the environment, like the app: a dark background with white text in dark mode, unchanged in light mode. The badges keep their colors and outlines. The Dynamic Island is always black and stays as it is. On Android, the notification is drawn by the system and already follows its appearance.
*Alternative:* leaving the background to the system. The text color would then have to follow the system's card, which isn't known in advance; that was the reason for the explicit background.

### Offline style per appearance
The inline offline style gets a dark background (`#1E2320`) in dark mode, chosen the same way as the base map.

### Badges on the dark map
The badges keep their colors and white outlines, which stand out on the dark map. The dark end badge (`#263238`) relies on its white outline there; this is checked with screenshots. If it isn't distinguishable enough, its outline becomes thicker in dark mode.

### Retrying a slow base map
Both apps fall back to the offline style when the base map isn't loaded after 3 s. Setting the fallback cancels the base map's loading, and until now the base map was only loaded again when network access came back. The dark style is not in the cache on the first start in dark mode, so this would happen to every user once. After falling back while online, the apps now load the base map again a moment later, without a timeout. MapLibre keeps showing the current style until the new one has loaded, so the base map replaces the fallback seamlessly. If loading fails, the existing failure handling shows the fallback again, and the base map is loaded once network access is back, as before. This applies to the light map on a cold cache as well.
*Alternative:* a longer timeout. It would keep the map empty longer on every slow start, and offline it would delay the knots.

## Risks / Trade-offs

- [A style switch briefly shows the map without base map] → Only at the moment of switching, which happens rarely; the camera and layers stay.
- [Navigation at sunset with automatic dark mode] → The switch is in place on Android and in the view on iOS; navigation runs in a service or session independent of the views, and the switch is checked while navigating.
- [The dark map is harder to read in bright daylight] → It follows the user's own system setting; the app doesn't force it.
- [The "dark" style can change upstream] → Same risk as "liberty" today; screenshots in the manual checks show what was checked.
