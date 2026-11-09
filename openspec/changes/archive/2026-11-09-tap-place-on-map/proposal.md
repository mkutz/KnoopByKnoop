# Proposal

## Why

Since `highlight-places-on-map`, places useful on a ride stand out on the map, but their names only show from zoom 16, and a tap on one does nothing special. On the maintainer's first look at the phones (2026-11-08), the first wish was to see at least the name when tapping a place. It is also the first step towards adding a toilet or food stop to the route quickly.

## What Changes

- **Tap a highlighted place:** a small card at the place shows the kind's icon and the place's name, or the kind ("Drinking water", "Supermarket") when it has no name. This works at any zoom where the places are shown.
- **While planning, the route point choices come with it:** "Start here", "Add stop here" (once a route exists) and "End here" show below the name, as for any tapped spot. Chosen, they put the route point at the place itself, not at the nearest course, so the way leads to the café and not past it.
- **Priority:** a knot or a route point within the touch tolerance still goes first. Places come before a plain spot on the map.
- **Far from the network:** a place more than 20 km (cycling) or 5 km (hiking) from every course shows its name without the choices.
- **While navigating:** the card shows the name only, without choices. A tap elsewhere closes it, and navigation goes on unchanged.
- **Data:** only what the base map's tiles have, the name and the kind. Nothing is looked up or sent.

## Out of Scope

- **Adding a place to the route while navigating.** A later change (the maintainer's idea of 2026-11-08), as it needs changing the route during navigation.
- **Naming the route point after the place** in the waypoint list, on the strip, in the voice and in the GPX export. The route point stays an unnamed flag, as from any tapped spot.
- **Details:** opening hours, address, phone, website, photos, ratings.
- **The base map's grey places** (pharmacies, shops): tapping them stays like tapping an empty spot.
- **Searching places by kind,** and a list of places along the route.
- **Tapping a place while the places are switched off** in the info sheet: then there are none to tap.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement for tapping a highlighted place, building on "Places useful on a ride" (`highlight-places-on-map`) and "Choose a route point on the map" (`add-route-points`), both not archived yet, so this change adds a requirement rather than modifying theirs.

## Impact

- **Core:** `MapTap` gets the tapped place (name, kind, position); `PlanningSession.tapAt` takes it, with the priority above; `PlaceKind` gets the kind from the tile's class; a route point from a place uses the place's position without snapping to a course. Names of the kinds in `Texts`-style app strings in four languages.
- **Apps:** finding the place under the tap in the `places` and `places-named` layers (`queryRenderedFeatures` on Android, `visibleFeatures` on iOS); the card above the existing choices (`PlaceOfferMenu` on Android and iOS), and a name-only card while navigating.
- **Outside the code:** none.
