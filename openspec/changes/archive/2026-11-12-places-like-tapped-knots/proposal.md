## Why

A tapped knot is added where it fits, with a one-line note and the buttons [as start] [as stop] [as end] previewed with badges. A tapped place or empty spot ("flag") still offered a menu, "Start here", "Add stop here" and "End here". The maintainer finds the two too different (2026-11-12).

The first version of this change gave the place card the same buttons. On the phones, the maintainer still saw two ways: the knot is added at once and the note sits above the panel, while a place is added only after a choice, in a card at the spot. Only one difference has a reason: tapping a highlighted place also shows its opening hours and contacts, so adding it on every tap would change the route whenever the user just looks. An empty spot has nothing to show.

So there is one way of adding: everything ends in the same note above the panel. Spots and places take one more step, "Add to route" at the spot. A version that added spots at once, like knots, made adding a flag by accident too easy (maintainer, 2026-11-12).

## What Changes

- A tap on an empty spot offers "Add to route" there. Choosing it adds a route point as a tapped knot is added: the start if there is none, the end if there is no end, otherwise where it adds the least distance. The note with [as start] [as stop] [as end] and Undo follows.
- A tap on a highlighted place (restaurant, campsite, …) shows its card with name, opening hours and contacts, and "Add to route". That button adds the place as a spot is added, the card closes, and the note follows.
- The note says "Eiscafé Riviera added as a stop" for a place, and "Added as the end" for a spot. Its previews show the place's icon or the flag.
- The "Start here", "Add stop here" and "End here" menu goes. Those names now mean the note's buttons for a route point.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`:
  - "Choose a route point on the map": a tapped spot offers "Add to route", which adds it where it fits.
  - "Tapping a place on the map": the card offers "Add to route".
  - "What a chosen route point does": the choices are the note's buttons.
  - "Changing what a tapped knot became": the note follows spots and places too.

## Impact

- Core: `PlanningSession.placed(point)` adds a route point as a tapped knot is added. `TapOptions.of` works for route points; `TapOptions.forPoint` and `suggested` go again.
- Android and iOS: the tap on a spot adds at once (off the main thread) and shows the note. The place card shows "Add to route" instead of the menu or the buttons. The note's line names a place, or says "Added as …" for a spot.
- Tests: `PlanningTest`, and `openspec/checked-by-hand.md` for the cards and the note.

## Out of scope

- Places from the search, which only move the map ("Jump to a hit").
- Making the previews faster on long routes.
- The map control redesign.
