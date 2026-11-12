## Context

The offer for a tapped place is `MapTap.Offer`. The apps keep it as `PlaceOffer` and show `PlaceOfferMenu`, a card with the place's name, details and three text buttons. A choice calls `session.routePointFor(offer, geometry)` and then `startAt`, `addStopAt` or `endAt`.

The note after a tapped knot uses `TapOptions` (three `TapChoice`s, each with a session and a `KnotPreview`) and the apps' `KnotPreviewRow`.

## Goals / Non-Goals

**Goals:**
- The same buttons, previews and order for knots and places.
- A suggestion from the same placement rule as for knots.

**Non-Goals:**
- Changing what a tap on a place does (show, don't add).

## Decisions

### Options for a point not yet in the route

`TapOptions.forPoint(session, point)` builds the three choices for a route point that isn't in the route yet, with `current` false for all. Its `suggested` role is where `PlanningSession.placed(point)` would put it.

`placed` is the knot placement (`Route.insertWhereItFits`) on the graph with the new route point, as `addStopAt` already builds it. The sessions are `startAt(point)`, `stopAt(point)` (`addStopAt` for a route point) and `endAt(point)`.

Without a route, there are no options: the card shows "Add to route", which calls `PlanningSession.tap(point)`, the same rule as for the first two knots (start, then end). For the first waypoint, `tap` already handles a route point: `copy` rebuilds the route graph. For the second, it plans with the current router, which doesn't know the new point yet, so `tap` has to go through `planned` for a route point there.

The apps compute it off the main thread when the card opens. The card shows the name at once and the buttons when ready, as the details already arrive later.

### The card

Under the name and details, the buttons follow the note's layout: start, stop, end. The suggested one is marked with the primary colour's ring, but stays tappable, unlike the note's selected button. The words go, and the screen reader names stay "Start here", "Add stop here" and "End here", with "suggested" for the marked one.

*Alternative:* a separate best-fit button above the three. It was rejected because that's one more thing to read, and the mark already says it.

## Risks / Trade-offs

- **Computing three routes when a card opens** → They come off the main thread, and the buttons appear when ready. That's the same cost as the knot note.
- **A far place gives long previews** → Previews are at most three badges, as for knots.
