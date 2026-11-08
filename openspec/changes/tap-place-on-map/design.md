# Design

## Context

- **Taps today:** the apps find the nearest knot badge within the touch tolerance and hand it to `PlanningSession.tapAt`, which returns a `MapTap`:
  - `OnKnot`: planned as before
  - `OnRoutePoint`: nothing happens
  - `Offer(position, onCourse, canAddStop)`: the choices "Start here", "Add stop here" and "End here"
  - `TooFar(limit)`: a message

  The choices show in `PlaceOfferMenu` (a `DropdownMenu` on Android, a SwiftUI menu on iOS), anchored at the offer's flag. iOS ignores taps while navigating; Android ignores offers while navigating.
- **Places:** `highlight-places-on-map` draws them in the layers `places` (z14–16) and `places-named` (from z16) on the base map's `poi` source layer, with `class`, `subclass` and `name` per feature.
- **Route points from an offer:** `routePointFor(offer, geometry)` puts the point on the course when `onCourse` is set, and otherwise connects it to the network by a way along paths (`add-route-points`).

## Goals / Non-Goals

**Goals:**
- The priority and the place's data decided in the core, tested once for both apps.
- The card reuses the existing offer menu, so planning from a place works like planning from any spot.

**Non-Goals:**
- A route point that remembers the place's name; see the proposal.

## Decisions

### The apps find the place, the core decides

On a tap, each app queries the rendered features of `places` and `places-named` in the touch target (`queryRenderedFeatures(RectF, …)` on Android, `visibleFeatures(in:styleLayerIdentifiers:)` on iOS) and takes the nearest one. It hands the core a `MapPlace(position, name, kind)`, with the kind from `PlaceKind.of(class, subclass)` and the position of the feature's point.

`PlanningSession.tapAt` gets a `place: MapPlace?` parameter:
1. a knot → `OnKnot`
2. a route point within the tolerance → `OnRoutePoint`
3. a place → `Offer(place.position, onCourse = null, canAddStop, place)`, or `PlaceOnly(place)` when it is beyond the distance limit
4. otherwise as today

**Why `onCourse = null` for a place:** the rider wants to reach the café, not the course beside it. The way from the network to the café is found as for any route point away from the network.

**Why not query the features in the core:** the rendered features live in MapLibre on each platform; the core has no tiles.

### The card

The existing menu gets a header above its items: the kind's icon (the same bitmap or image as on the map) and the name, or the kind's name. The header isn't clickable.
- **Android:** a disabled `DropdownMenuItem` with `leadingIcon`, then a divider.
- **iOS:** the same at the top of `PlaceOfferMenu`.

While navigating, the apps show the same menu with only the header (Android: `navigating` lets a `PlaceOnly`-style offer through; iOS: the `!navigating` guard lets places through). A tap beside it or Back closes it, as today.

### Kind names

Four new strings per app in four languages: "Food and drink", "Supermarket", "Bicycle shop", "Railway station", and for water and toilets the subclass decides: "Drinking water" or "Toilets". So `MapPlace` keeps the subclass for that one case, as `PlaceKind.WATER` covers both.

## Risks / Trade-offs

- **[Two places under one tap]** In a busy centre the touch target may hold several cafés. → The nearest to the tap point wins; zooming in separates them.
- **[Hidden places]** A place left out by collision (e.g. next to a knot) isn't rendered, so it can't be tapped. → As intended: what isn't shown can't be tapped.
- **[Names in another script]** The tile's `name` is the local name, as on the map. Fine for NL, BE and NRW.

## Migration Plan

App-only, no data change.
