# Proposal

## Why

Since `tap-place-on-map`, a tapped café or toilet can become a start, stop or end, but the waypoint then looks like any tapped spot: a generic flag, and "Stop" in the list. On the iPhone on 2026-11-08 the maintainer asked for the stop to show the place's icon instead, so the route shows at a glance where the food stop is.

## What Changes

- **A route point remembers its place:** made from a tapped highlighted place, it keeps the place's kind and name.
- **The place's icon instead of the flag:** on the map, in the waypoint list and on the strip, with the start, stop or end corner symbol as on knots.
- **The name in the waypoint list:** "Stop · Eiscafé Riviera"; a place without a name shows its kind, "Stop · Drinking water". Screen readers read the same.
- **Kept with the route:** the kind and the name are saved with the route and come back after a restart. Routes saved before this change load as before.
- **Unchanged:** route points from an empty spot keep the flag, and a route point keeps its icon when the places are switched off in the info sheet.

## Out of Scope

- **The name while navigating:** the header, the notification, the Live Activity, the Dynamic Island and the spoken notices still call it the start, stop or end. A later change once the maintainer has ridden with it.
- **The name in the GPX export.**
- **Renaming a route point,** or choosing a place for an existing one.
- **Adding a place while navigating** (the maintainer's idea of 2026-11-08).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement for route points made from a place, building on "Route points on the map and in the list" and "Keeping route points" (`add-route-points`) and "Tapping a place on the map" (`tap-place-on-map`), which aren't archived yet, so this change adds a requirement rather than modifying theirs.

## Impact

- **Core:** `RoutePoint` gets the place's kind key and name, set by `routePointFor` from an offer with a place; `RouteSnapshot` saves them as optional fields; `MapFeatures` puts the kind on the route knots; `ProgressMark` carries it to the strip.
- **Apps:** badges of route points draw the place's disc and glyph instead of the flag (`KnotBadges` on both platforms, sharing the place icons of `highlight-places-on-map`), their image names include the kind, and the waypoint row shows the name.
- **Outside the code:** none.
