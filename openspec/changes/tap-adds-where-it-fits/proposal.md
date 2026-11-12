## Why

Two rules decide what a tap adds to the route. A tapped knot follows the "Add knots: Between | At end" switch in the panel. A tapped place offers "Start here", "Add stop here" and "End here" and ignores the switch. The maintainer wants one way (2026-11-12).

The proposal is that one tap does the likely thing at once, with a quick way to change it. This change builds that for knots first, so it can be tried on the phones ("how it feels") before deciding. Places and route points ("flags") come in a separate change, depending on the result.

## What Changes

- **BREAKING (UI):** the "Add knots: Between | At end" switch goes, and with it the remembered choice.
- Once a route exists, a tapped knot is added at once where it adds the least distance: as a stop in one of the legs, before the start, or after the end. So a knot before the start becomes the new start and the old start a stop, and a knot beyond the end becomes the new end and the old end a stop. A round trip stays one: it has no place before its start or after its end.
- After each such tap, a short note says in one line what the knot became, e.g. "14 added as a stop". It shows the buttons as start, as stop and as end in this order, each previewing that role with badges, with the current one selected, and Undo. It goes away after a few seconds, with the next tap, or when one of its choices is used.
- The first two taps stay as they are: start, then end.
- Tapped places, places from the search and route points keep their "Start here", "Add stop here" and "End here" choices.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`:
  - "Plan by tapping knots": the tapped knot goes where it adds the least distance, after the end included.
  - "What new knots become" is removed.
  - A new requirement, "Changing what a tapped knot became", covers the note.
  - "What a chosen route point does" no longer refers to the switch.

## Impact

- Core:
  - `PlanningSession.tap` loses its `NewKnots` parameter, and `NewKnots` goes.
  - `Route` gains placement after the end as a candidate.
  - `PlanningSession` gains making a waypoint the start or the end.
- Android: `NewKnotsSwitch`, `chooseNewKnots`, `KEY_NEW_KNOTS` and the strings `add_knots_*` go. A note above the panel is added.
- iOS: `newKnotsSwitch`, `NewKnotsIcon`, `newKnots` with its `UserDefaults` key and the strings go. A note above the panel is added.
- Tests:
  - `PlanningTest` and the other tests that pass `NewKnots`.
  - `openspec/checked-by-hand.md` entries of the removed requirement.

## Out of scope

- Places, places from the search and route points: they keep their choices until the next change decides.
- Moving and removing waypoints in the list. That's how a waypoint changes later, after the note has gone.
- The map control redesign from the mockups, which is its own change.
