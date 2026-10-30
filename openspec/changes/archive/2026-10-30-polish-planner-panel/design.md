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
- "At " and the rounded arrival, then the ferry's status at the arrival in the ferry list's words, starting in lowercase: "At 14:30: open until 19:00", "At 19:15: closed until tomorrow 07:00", "At 18:10: closed until 1 May 10:00"; `WARNING` when the spoken warning applies (closed then, or closing within 15 minutes, e.g. "At 18:50: open until 19:00" in red), otherwise `OPEN`
- no status at the arrival: "At 14:30"
"tomorrow" and the dates count from the arrival day. A first version reused the spoken text ("Closed when you get there around 18:10, opens on 1 May at 10:00"); it did not fit into one line, so the maintainer chose the arrival-first form.
The apps show this line in place of the status for now while `ferryArrival` returns one; passed ferries get `null` and show their status for now again.
*Alternative:* keep `warning: Boolean` and compose the line in each app. That would duplicate the wording on two platforms.

### Shorter status texts
`RouteFerry.statusText` (core) says "Closed until 07:00", "Closed until tomorrow 07:00", "Closed until 1 April 07:00" and "Open until 31 October 19:00" instead of "Closed, opens …" and "… at …". It is shared by the status for now and the arrival line. The spoken warnings keep their full sentences, which are heard, not glanced at.

### Ferries in one line, expanded on tap
The collapsed row is "⛴ 76 → 02 · status" in one line: the knots on both sides of the ferry in riding order (`RouteFerry.place`, built in the core) instead of the name, which is often long ("Driehoeksveer Ridderkerk - Krimpen aan de Lek - Kinderdijk" pushed the status out of the line on the emulator) and says less about the route than where the ferry is. The name opens the expanded view. The status is cut off with "…" at the end where it does not fit. The status part in its color (green open, red closed or warned; gray tagged times when there is no status; red "Operating times not known" without times) and a chevron at the end. While navigating, the arrival line replaces the status, as above. Tapping the row toggles it; expanded, it shows the full status or arrival line, the tagged times, fee, website and "Times according to OpenStreetMap". Which ferries are expanded is kept in the panel's state only, by route position, and starts collapsed for a new route. The "Ferries on this route" heading is dropped; the ⛴ badge in the knot sequence and the ⛴ in each row mark them. Android: a `Row` of the place and the status (weighted, ellipsis), clickable with the role of a button and the state announced ("expanded"/"collapsed"). iOS: a `Button` row with the place (higher layout priority) and the status (`lineLimit(1)`), and `.accessibilityValue` for the state.
*Alternative:* only name and status, without details. That would hide the tagged times, which stay the reference when the status cannot be told.

## Risks / Trade-offs

- [A rider does not find the times behind the tap] → The chevron shows the row expands; the status, which answers the usual question, stays visible.
- [The ⓘ is overlooked by first-time users] → It is in the same place as before and readable for screen readers; the info sheet holds nothing needed for planning.
- [48 dp rows are harder to drag precisely] → Still the minimum touch target size; drag uses the same row height for the swap threshold.
- [Hiding the list while navigating removes the only place the roles are spelled out] → The badges keep the role colors and the start/end styling, as on the map.
