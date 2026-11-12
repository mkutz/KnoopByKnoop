# Proposal

## Why

Before a route exists, the planner panel holds a single line, "Tap a knot to set the start", over a mostly empty sheet. The maintainer finds it looks sad, and it says nothing about the other ways to begin (search, the nearest knots). The mockups (https://claude.ai/artifact/23wUTABxwB9fbwSpyXdN3G) were reviewed on 2026-11-12, and direction A was chosen.

## What Changes

- With fewer than two waypoints, the panel shows "Plan a ride" and two badge slots, start and end, joined by a dotted line, with their labels below. An empty slot is a dashed circle; the slot the next tap fills gently pulses, or stands out without pulsing when the phone reduces motion.
- One short hint line below the slots: "Tap a numbered knot on the map for the start.", then "Now tap a knot for the end." once the start is set, or "Now tap a knot for the start." when an end was chosen first.
- A set waypoint fills its slot with its badge as on the map and in the route row (a flag for a route point). Its label gains the place it lies in, e.g. "Start · De Rips".
- Below the hint are tonal buttons: "Search a place" (opens the search, like the map's search button) and "Nearest knots" (the existing "Show the nearest knots" action). Once a waypoint is set, "Nearest knots" gives way to Clear.
- With no knots in view and nothing planned, the hint says "No knots in view" and "Nearest knots" is filled, the prominent action, replacing today's separate prominent button.
- A complete route replaces the slots with today's route summary, badge row and action row; nothing changes there.
- Both platforms; same structure in the side panel in landscape.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement "Panel before a route"; "Nearest knots" changes how the panel offers the action with no knots in view.

## Out of scope

- A general undo for plan changes (an idea of the maintainer, its own change later; it would also sit in the panel).
- Recent or saved routes in the empty panel.
- Round trips started from the start-only panel.
- The panel with a complete route, and the navigation panel.
- The map's buttons: search and the nearest knots stay in the button column as well.

## Impact

- Core: `RouteDescription.StartOnly` carries the waypoint, so the apps can draw its badge and place.
- Android: `PlannerScreen` (`WaypointSheet` header for the non-complete states), strings in four languages.
- iOS: `PlannerView` (`WaypointSheet` summary), `Localizable.xcstrings`.
- The smallest sheet grows by about one row while nothing is planned; it is measured, as today.
