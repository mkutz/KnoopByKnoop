# Design

## Context

- **`RoutePoint`** (in `Knot.routePoint`) holds where the point is on the network (`fromId`, `toId`, `fraction`), the tapped `place`, and its `way`. A route point is a `Knot` with an empty `ref`.
- **Saved:** `RouteSnapshot` writes route points into `route.json` with `placeLatitude`, `placeLongitude`, `way` and `straight`; optional fields default when missing.
- **Drawn:** route knots get the image `route-<role>-<plate>-<ref>`; with an empty ref the badge code draws a flag (`KnotBadges.kt`, `KnotBadges.swift`). The same badges are on the strip (`ProgressMark`) and in the waypoint list.
- **Named:** the waypoint row names a route point by its role (`PlannerScreen.kt` `WaypointRow`, and the iOS list), also for its screen reader actions.
- **Places:** `MapPlace` (`tap-place-on-map`) has the kind, the subclass and the name; `MapPlace.kindKey` tells water from toilets. The apps draw place icons in `PlaceIcons` on both platforms.

## Goals / Non-Goals

**Goals:**
- One field set on the route point, saved and read in the core, so both apps only draw it.
- Old `route.json` files load unchanged.

**Non-Goals:**
- The name in navigation and in the GPX export; see the proposal.

## Decisions

### Kind key and name on `RoutePoint`

`RoutePoint` gets `placeKind: String?` (the `kindKey`: `food`, `supermarket`, `bicycle`, `railway`, `drinking_water` or `toilets`) and `placeName: String?`. `routePointFor(offer, …)` copies them from `offer.place`. They stay when the point changes role (start to stop), as the `Knot` is the same.

**Why the kind key, not `PlaceKind`:** it tells drinking water from toilets for the name, and it is a plain string in `route.json`.

**Saved:** `RouteSnapshot` gets two optional fields with the same names, `null` when missing, so old files load and old app versions ignore them.

### Drawing

- **`MapFeatures.routeKnotsLayer`** puts `place` (the kind key, or empty) on each route knot feature.
- **Image names** become `route-<role>-<plate>-<ref>-<place>`. For knots `place` is empty, so knots keep their badges.
- **Badge code:** with a place, the plate is the place's disc and glyph (the drawing of `PlaceIcons`, at the badge's size) instead of the white plate with the flag, and the role's corner symbol is drawn as for knots. On the strip the same through `ProgressMark.place`.

### The list

The row's text is the role and, for a route point with a place, " · " and the name, or the kind's name (the strings of `tap-place-on-map`). The screen reader actions ("Move …", "Remove …") use the same text.

### As built (2026-11-08)

- **Android glyphs as path data:** the badge code has no `Context`, so the five Material Symbols glyphs are now path data in `PlaceIcons.kt`, drawn with `PathParser`. The `ic_place_*` drawables of `highlight-places-on-map` are gone; the map's place icons use the same paths.
- **The place kept when the point is rebuilt:** `RoutePointWays.wayFor` (a way along paths found later) and `reattached` (connection gone) build new route points, so they pass the place on.
- **Screen readers read a comma:** the row shows "Stop · Eiscafé Riviera" and reads "Stop, Eiscafé Riviera", as the spec's scenario says.
- **Image names** end in `-<place>` for every route knot, empty for knots, on both platforms.

## Risks / Trade-offs

- **[Badge size]** The place disc drawn at a knot badge's size may look heavier than the small map icon. → Same glyphs, bigger disc; checked on screenshots next to knots.
- **[Names in other languages]** The name is the tile's local name, as on the map.

## Migration Plan

`route.json` gains two optional fields; nothing to migrate. Apps without this change ignore them, so a downgrade shows flags again.
