# Proposal

## Why

Both apps are always light, while many people use their phone in dark mode, especially in the evening. On iOS this already showed up as a mismatch: the system sheet turned dark and the light map did not, so the panel is now kept light (`clearer-planner-actions`). The maintainer wants the apps to follow the system setting instead.

## What Changes

- In dark mode, both apps are dark: a dark base map, dark panels and sheets, search, network switch and zoom controls.
- The planner's actions keep WCAG AA contrast on the dark panel, like on the light one.
- The knots, the route and the highlighted knots stay readable on the dark map.
- Switching the system setting while the app is open switches the app, without losing the map position, the route or a running navigation.
- Offline, the fallback map without base map is dark in dark mode too.
- On iOS, the Live Activity on the lock screen is dark in dark mode too; Android's notification already follows the system.
- In light mode, nothing changes.

Out of scope: an in-app setting to choose light or dark, the Dynamic Island (always dark), the app icon, and the launch screen.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: a new requirement that the map and the planner follow the system's light or dark appearance; "Clear actions in the planner panel" (from `clearer-planner-actions`) applies in both appearances

## Impact

- Android: `MainActivity.kt` (a dark color scheme next to the light one), `NetworkMap.kt` (dark base map and offline style, switching styles in place), `PlannerActions.kt` (action colors per appearance), the manifest (`uiMode` handled without recreating the activity) and a night window theme
- iOS: `PlannerView.swift` (no more forcing light), `NavigationLiveActivity.swift` (the lock screen card by appearance), `NetworkMapView.swift` (dark base map, switching on appearance changes), `Shared/Badges.swift` (action colors per appearance)
- Base map: OpenFreeMap's "dark" style next to "liberty", no API key, same provider
- Depends on `clearer-planner-actions` being archived first, since it modifies its requirement
