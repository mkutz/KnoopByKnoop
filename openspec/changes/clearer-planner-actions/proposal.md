# Proposal

## Why

The planner's actions Back to start, Clear and Navigate, and Stop while navigating, are plain text links in the app's green on the light panel. At about 3:1 they are hard to read outdoors, and Navigate, the action the planner leads to, looks no different from Clear. Testers on Play's internal track will see this first.

## What Changes

- Navigate becomes a filled button with an icon and its label, the one prominent action in the planner's header.
- Back to start and Clear become round icon buttons; screen readers still announce them by name, and on Android a long press shows the name.
- While navigating, Stop becomes a filled red button with an icon and its label.
- All these actions meet WCAG AA contrast on the panel, outdoors in daylight included.
- The header stays one row, as `map-planning` "Long routes in the panel" requires.

Out of scope: other buttons and controls (search, network switch, Info, zoom, waypoint list), dark mode, the notification and the Live Activity, and changes to what the actions do.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: a new requirement for how the panel's actions are shown: Navigate and Stop stand out, Back to start and Clear are named icon buttons, and all are readable

## Impact

- Android: `PlannerScreen.kt` (header and `NavigationHeader`), new vector drawables from Material Symbols in `res/drawable`
- iOS: `PlannerView.swift` (header and `NavigationHeader`), SF Symbols, a darker green next to `knoopGreen` in `Shared/Badges.swift`
- No core changes, no new dependencies
