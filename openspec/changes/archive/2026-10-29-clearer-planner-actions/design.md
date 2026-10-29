# Design

## Context

See `proposal.md` for why. Both apps use one green, `#2E9E5B` (Android `colorScheme.primary`, iOS `Color.knoopGreen` and the app's tint), for the knot badges, the map and the actions. The actions are Material `TextButton`s on Android and plain SwiftUI `Button`s on iOS. On the panel's background (`#F3EDF7` on Android) that green reaches only 2.97:1, and white on it only 3.41:1. Neither app has UI tests; behavior on the platforms is checked manually, see `manual-checks.md` in earlier changes.

## Goals / Non-Goals

**Goals:**
- One obvious main action per state: Navigate while planning, Stop while navigating.
- WCAG AA contrast for the actions, without changing the green of the badges and the map.
- The same look on both platforms, each with its platform's own components and icons.

**Non-Goals:**
- A new color scheme or theme for the whole app.
- Restyling the other controls.

## Decisions

### A darker action green, only for the actions
A second green, `#237A46`, is used for the actions only. It gives 5.32:1 for white labels on it and 4.63:1 against the panel. The badges and the route on the map keep `#2E9E5B`, which testers already know and which the badges' white numbers are designed around.
*Alternative:* darkening `primary` everywhere. That would change badges, markers and the network switch, which is outside this change.

### Navigate: a filled button with icon and label
Android uses a Material 3 `Button` (filled, pill-shaped) with `containerColor` set to the action green and a leading 18 dp icon. iOS uses a `Button` with a `Label`, `.buttonStyle(.borderedProminent)`, `.buttonBorderShape(.capsule)` and the action green as tint. Icons: Material Symbols `navigation` and SF Symbol `location.north.fill`, both the familiar "go" arrow of navigation apps.

### Back to start and Clear: round tonal icon buttons
Android: `FilledTonalIconButton` with the icon in the action green on a light green container, wrapped in a `TooltipBox` with a `PlainTooltip` for the name on long press, and `contentDescription` for TalkBack. iOS: `Button` with `Label(..., systemImage:)`, `.labelStyle(.iconOnly)`, `.buttonStyle(.bordered)`, `.buttonBorderShape(.circle)`; VoiceOver reads the label's title. iOS has no long-press tooltip for buttons, so the name on long press is Android only ("Name of an icon button on Android").
Icons: Back to start: Material Symbols `u_turn_left` / SF Symbol `arrow.uturn.backward` (turning around to where you came from). Clear: `delete` / `trash`, the usual "throw this away".
*Alternative:* labeled tonal buttons for all three. They don't fit in one row next to "123.4 km" on a 360 dp wide phone, which `map-planning` "Long routes in the panel" needs.

### Stop: filled red
The same component as Navigate, with Material 3's error color `#B3261E` (6.54:1 with white) on both platforms. iOS's `systemRed` gives only 3.55:1 with white, so iOS uses the same red explicitly. Icons: Material Symbols `stop` / SF Symbol `stop.fill`.

### Icons on Android as vector drawables
The few icons are copied from Material Symbols (Apache 2.0, rounded, weight 400, 24 dp) into `res/drawable` as `ic_action_*.xml`. The `material-icons-extended` library would add several megabytes for four icons.

### Contrast checked by computing it
The contrast ratios are computed with the WCAG formula from the colors in the code, and recorded in `manual-checks.md` together with screenshots. With no UI tests in either app, the scenarios are checked manually like in earlier changes: Android on the emulator (TalkBack, long press), iOS by the maintainer on the iPhone (VoiceOver).

## Risks / Trade-offs

- [Icon-only buttons can be unclear] → Familiar icons, the names on long press on Android and for screen readers, and Navigate keeps its label.
- [Two greens in the app] → The darker green is used only for actions, so the rule is simple: badges and map light, actions dark.
- [Bigger buttons make the header taller] → Material's 40 dp buttons replace 40 dp text buttons, so the header keeps its height; checked on the smallest sheet.
