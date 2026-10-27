# Design

## Context

- `MapFeatures` (core) produces GeoJSON for knots, connections, the route (one line per leg) and the waypoints (points with `position` and `role`), and `RouteDescription.Complete` with the knot sequence as one string joined by arrows.
- Both apps draw knots as locally rendered badge images (white circle, green ring, green number), the route as an orange line below the badges, and waypoints as orange circles.
- See proposal.md for the feedback this change addresses.

## Goals / Non-Goals

**Goals:**

- One visual language for knots on the map and in the sheet
- The roles of knots come from the core, so both apps show the same

**Non-Goals:**

- A full visual redesign, dark mode, accessibility work beyond readable contrasts

## Decisions

### Knot roles from the core

`MapFeatures` gets a route knot list: every knot of the complete route in riding order with its role, `start`, `stop`, `end` or `passed`. A knot that is a waypoint keeps its waypoint role. It is exposed

- as GeoJSON points (`routeKnots`) with `ref` and `role`, drawn above the network's knots, and
- as a list for the sheet, replacing the arrow-joined string in `RouteDescription.Complete`.

For an incomplete route, the waypoints keep their roles, so the sheet can still show them.

*Alternative:* each app derives roles from the waypoints and the route. Rejected: both would have to agree on duplicates and round trips.

### Badge styles

| Role | Map and sheet |
|---|---|
| Network knot | white badge, green ring, green number (as now) |
| Passed | white badge, orange ring, orange number |
| Stop | orange badge, white number |
| Start | green badge, white number, slightly larger |
| End | dark badge, white number, slightly larger |
| Start and end of a round trip | split badge, left half green, right half dark, white number, slightly larger (map only; the sheet's row shows start and end at its two ends) |

The orange waypoint circles go away; the badges themselves carry the role. Badge images are rendered per role and number, on demand.

### Tapping the last waypoint and returning to the start

`PlanningSession.tap` ignores a knot that already is a waypoint. `PlanningSession.returnToStart()` appends the start as new end; it is offered in the sheet as "Back to start" once there are at least two waypoints and the route does not end at its start already.

### Zoomed out

Badges of route knots use the same minimum zoom level as the network's knots (10). Below it, only the route line is drawn. A tap below that zoom level that hits the route line (within the touch target) zooms the map to the route's bounds; other taps do nothing.

### Direction arrows

A symbol layer along the route lines (`symbol-placement: line`) with a small arrow image every 80 points, pointing in the line's direction. The legs are already written from their start towards their end, so no extra data is needed.

*Alternative:* a gradient from start to end. Rejected: harder to read and not supported equally on both SDKs.

### Zoom buttons

Two buttons (+ and −) at the right edge, above the sheet, zooming by one level around the map's center with an animation. Pinch, double tap and two-finger tap stay as the SDKs provide them.

### Sheet

- The summary shows the distance and below it the route's badges in a wrapping row, without arrows.
- The waypoint list shows the badge of each waypoint with its role as text next to it, keeping drag to reorder and swipe to remove.

## Risks / Trade-offs

- **Long routes** produce long badge rows; the row wraps and the sheet scrolls, which is acceptable for the route lengths of a day.
- **Badge images** grow to one per number and role; numbers repeat (1 to 99 mostly), so this stays at a few hundred small images.

## Open Questions

None.
