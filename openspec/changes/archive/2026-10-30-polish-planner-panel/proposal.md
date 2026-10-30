# Proposal

## Why

A few things make the planner busier than it needs to be:
- The green "Info" text button looks like a primary action next to the network switch, although it is rarely used.
- The expanded panel shows the waypoints twice: as badges in the knot sequence and as a list of tall rows that only add the words Start, Stop and End. While navigating, the list cannot even be edited.
- Each ferry takes four to eight lines: title, status, the tagged times (often three lines), fee and website, more than the rest of the panel.
- While navigating, a ferry ahead shows two lines of similar red text one above the other, "You get there around 18:10" and "Closed, opens on 1 May at 10:00", which the rider has to sort out at a glance.

## What Changes

- The "Info" text button becomes a round ⓘ icon button, styled like the zoom buttons so it is not taken for a map symbol (the base map uses a bare "i" for tourist information). Screen readers announce it as "Info".
- The waypoint list rows become one compact line each, no higher than a touch target. While navigating, the waypoint list is not shown; the knot sequence still shows the route.
- Each ferry is listed in one line with where it is on the route and its status, e.g. "⛴ 76 → 02 · Closed until 1 May 10:00"; its name, often long and hardly readable in one line, moves into the expanded view. Tapping it expands it to the name, the full status, the tagged times, fee, website and the note that they are according to OpenStreetMap. The "Ferries on this route" heading goes; the ⛴ marks the rows.
- The status texts get shorter: "Closed until 07:00", "Closed until tomorrow 07:00", "Closed until 1 April 07:00", "Open until 31 October 19:00" instead of "Closed, opens tomorrow at 07:00" and the like.
- While navigating, each ferry ahead shows one line about the arrival instead of its status for now and the arrival line: the arrival first, then the ferry's state then, e.g. "At 18:10: closed until 1 May 10:00" in red, or "At 14:30: open until 19:00" in green. The spoken warnings do not change.

Out of scope:
- A duration estimate next to the route's distance (its own change, if wanted).
- Smaller or hidden zoom buttons.
- Other ways to move or remove waypoints than today's.
- Hiding ferries already passed from the ferry list.

## Capabilities

### New Capabilities

### Modified Capabilities
- `network-sync`: "Info sheet" says how the sheet is opened, as an icon button
- `map-planning`: "Ferries on the route" lists each ferry in one line with its place on the route that expands; "Ferry open now" gets shorter status texts; "Waypoint list with badges" gets compact rows and is hidden while navigating; "Ferry open now" leaves the ferries ahead to the arrival line while navigating
- `knot-navigation`: "Ferry arrival estimate" and "Ferry warning" show the arrival and the warning in one line per ferry

## Impact

- `modules/core`: the one-line arrival text and its state in `FerryArrival` (`FerryWatch.kt`)
- `apps/android` `PlannerScreen.kt`, `RouteFerries.kt` and `apps/ios` `PlannerView.swift`, `RouteFerries.swift`: the info button, the waypoint rows, the ferry lines
- No change to the graph, navigation logic or spoken notices
