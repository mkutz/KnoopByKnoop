# Design

## Context

See `proposal.md` for why. Today:
- Android: `PlannerScreen.kt` puts a `TextButton("Info")` at the end of the row with the network chips; while navigating, the chips are hidden and the button stands alone. The panel already has `IconAction` (icon button with a tooltip on long-press and a content description) for Back to start and Clear, and the zoom buttons are round floating buttons. iOS: `PlannerView.infoButton` is a `Button("Info")` next to the network picker.
- The waypoint rows are 56 dp high on Android (`WAYPOINT_ROW_HEIGHT`, also used for dragging); on iOS they are plain `List` rows with a 40 pt badge and the role in `.body`. Both lists stay in the panel while navigating, with editing disabled.
- `FerryWatch` (core) gives `FerryArrival(text, warning)` per ferry ahead; the apps show it above the status for now, both red when warned and closed.

## Goals / Non-Goals

**Goals:**
- The same look and wording on both platforms, the texts built in the core as before.

**Non-Goals:**
- New gestures or controls for the waypoint list.
- Changing the spoken warnings.

## Decisions

### Info as a round icon button
Android: the Material "info outline" icon in a 40 dp round button styled like the zoom buttons, inside a 48 dp touch target, `contentDescription = "Info"`, with the same long-press tooltip as `IconAction`. iOS: `Image(systemName: "info.circle")` in a button with `.accessibilityLabel("Info")` and the zoom buttons' floating background. Both keep their place at the end of the network switch row; while navigating, the row holds only this button.
*Alternative:* a bare "i" glyph. The base map draws tourist information with a bare "i", so a control could be taken for a map symbol.

### Compact waypoint rows, hidden while navigating
Android: `WAYPOINT_ROW_HEIGHT` becomes 48 dp; the role text stays in `bodyMedium` next to the badge. iOS: the badge shrinks to the knot sequence's badge size and the list rows get a minimum height of 44 pt via `.environment(\.defaultMinListRowHeight, 44)` with tighter insets. While navigating, neither app shows the waypoint rows; the ferries stay in the scrolling part of the panel.
*Alternative:* drop the list and edit via the badges. That needs a new way to move and remove waypoints, which is out of scope.

### One arrival line, built in the core
`FerryArrival` gets the one-line text and a state instead of `warning`: `OPEN` (green), `WARNING` (red) or `UNKNOWN` (no color). The texts derive from the same evaluation as the spoken warnings:
- warning: the spoken text without "The ferry … ", capitalized, and "is closed when" shortened to "Closed when": "Closes at 19:00, before you get there around 19:15", "Closed when you get there around 18:10, opens on 1 May at 10:00"
- open, no warning: "Open when you get there around 14:30"
- no status at the arrival: "You get there around 14:30"
The apps show this line in place of the status for now while `ferryArrival` returns one; passed ferries get `null` and show their status for now again.
*Alternative:* keep `warning: Boolean` and compose the line in each app. That would duplicate the wording on two platforms.

## Risks / Trade-offs

- [The ⓘ is overlooked by first-time users] → It is in the same place as before and readable for screen readers; the info sheet holds nothing needed for planning.
- [48 dp rows are harder to drag precisely] → Still the minimum touch target size; drag uses the same row height for the swap threshold.
- [Hiding the list while navigating removes the only place the roles are spelled out] → The badges keep the role colors and the start/end styling, as on the map.
