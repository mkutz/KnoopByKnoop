## Context

`MapTap.Offer` is the tap on a spot or a highlighted place (`place` set). The apps keep it as `PlaceOffer` and show `PlaceOfferMenu`. The note after a tapped knot is `TapNote` with `TapOptions`, shown above the panel. `session.routePointFor(offer, geometry)` makes the route point. It is slow on long routes: about a second on the Pixel, half a minute on the emulator.

## Goals / Non-Goals

**Goals:**
- One way of adding: the same placement and the same note for knots, spots and places.
- Reading a place's details never changes the route.

**Non-Goals:**
- A faster placement.

## Decisions

### Adding a route point

`PlanningSession.placed(point)` is `tap` for a route point: the start, then the end, then where it fits (`Route.insertWhereItFits` on the graph that knows the point).

The apps share one function for spots and "Add to route". Off the main thread it makes the point and the session it gives. If the route has not changed meanwhile, it shows that session and works out `TapOptions.of(before, after, point)` for the note, exactly as for a knot.

A tap on a spot offers "Add to route" in a small card at the spot, like a place's card without a name. Adding spots at once was tried and dropped: a stray tap on the map added a flag too easily (maintainer, 2026-11-12). Adding takes about a second; a small spinner at the spot shows it, and the next tap cancels a pending add.

### The note for route points

The line names a place by its name or kind ("Eiscafé Riviera added as a stop"), and a spot without one by the role alone ("Added as the end"). The buttons are the same `KnotPreviewRow`, with the place's icon or the flag.

### The place card

Name, details, and below them a single "Add to route" while planning. "Add to route" closes the card and adds as a spot is added. Far away or while navigating: name and details only, as before.

### What goes

The menu items, the three buttons in the card, `TapOptions.forPoint`, `suggested`, the card's waiting spinner, and the "suggested" strings.

## Risks / Trade-offs

- **The second it takes to add** → A spinner at the spot.
