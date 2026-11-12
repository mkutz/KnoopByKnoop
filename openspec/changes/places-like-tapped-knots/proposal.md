## Why

A tapped knot is now added where it fits, with a one-line note and the buttons [as start] [as stop] [as end] previewed with badges. A tapped place or empty spot ("flag") still offers a plain menu, "Start here", "Add stop here" and "End here". The maintainer finds the two too different (2026-11-12).

Places can't simply be added on a tap, though. Tapping a place is also how its card with the opening hours and contacts is read. The maintainer chose to keep "tap shows, choose adds" for places, with the same buttons as the note.

## What Changes

- The choices of a tapped place or empty spot become three buttons in the order start, stop and end, like the note after a tapped knot. Each previews its role with badges: the place's icon or the flag, next to the start or end it would take over, or between the knots around it where it would be a stop.
- The button where the place adds the least distance, as for a tapped knot, is marked as the suggestion. Nothing changes until one is chosen.
- Without a route, the card offers just "Add to route", as knots ask nothing then either: the first place becomes the start, the second the end (maintainer, 2026-11-12). "Start here" and "End here" without a route go.
- The words "Start here", "Add stop here" and "End here" are no longer shown, but remain the buttons' names for screen readers.
- The place card keeps its name, opening hours and contacts above the buttons.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`:
  - "Choose a route point on the map": the choices become badge-preview buttons with a suggestion.
  - "Tapping a place on the map": the café card shows "Add to route" or the buttons.
  - "What a chosen route point does": "Add to route" adds as the first two knot taps do.

## Impact

- Core: `TapOptions` gains the options for a point not yet in the route (`TapOptions.forPoint`), with the suggested role. `PlanningSession` gains placing a route point where it fits, which is today's knot placement with the route point's graph.
- Android: `PlaceOfferMenu` shows the buttons, reusing the note's `KnotPreviewRow`.
- iOS: `PlaceOfferMenu` likewise.
- Tests: `PlanningTest` (suggestion, previews), and `openspec/checked-by-hand.md` for the cards.

## Out of scope

- Adding a place at once on a tap, or showing a note after a place was chosen.
- Places from the search, which only move the map ("Jump to a hit").
- The map control redesign.
