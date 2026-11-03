# Proposal

## Why

The maintainer and a tester sometimes plan differently from "A to B over C": they start at A and look around where to go next. Today, every tap after the second adds a stop where it adds the least distance. So a knot past the end becomes a there-and-back to it, and the route still finishes at the old end.

Planning A to B over C stays right, so both ways are needed. A long press would be hidden, and a tester already missed reordering when it was only a long press. Guessing was measured and rejected: a knot whose way runs through the end could become the new end without any control, but that catches only 53 to 57% of the knots next to the end, so neighboring knots would act differently without a visible reason.

## What Changes

- **A switch in the planner panel:** "New knots: Stop | End", once a route exists.
- **Stop:** a tapped knot is added as a stop where it adds the least distance, as today. This is the default, so nothing changes for anyone who leaves it alone.
- **End:** a tapped knot becomes the new end, and the old end becomes a stop.
- **Round trips stay closed:** in End mode, a new knot goes before the final return to the start.
- **The choice is remembered** across routes and starts of the app.
- **The first two taps stay as they are:** start, then end. The switch is not shown before there is a route.

## Out of Scope

- **A long press or a menu on the knot.**
- **Guessing from where the knot lies.**
- **Highlighting the knots next to the end,** with their distances, which could be its own change.
- **Tapping a knot already on the route,** which still changes nothing. So going back the same way to a knot other than the start is done in the waypoint list.
- **Undo.** The waypoint list's remove and drag handle fix a wrong tap.
- **Stops or ends while navigating:** the route stays fixed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`:
  - "Plan by tapping knots" follows the switch.
  - A new requirement "What new knots become" covers the switch.

## Impact

- **Core:**
  - A `NewKnots` type with `STOP` and `END`.
  - `PlanningSession.tap(knot, newKnots)`.
  - `Route` gets a way to add a new end, also before a round trip's return.
  - Tests are in `PlanningTest`.
- **Android:** a segmented control in the panel, as in the info sheet; the choice in the `settings` preferences; the sheet's smallest height; strings in four languages.
- **iOS:** a segmented picker in the panel; the choice in `AppStorage`; the sheet's smallest height; strings in four languages.
- **Data:** no data run.
- **The map:** the panel's smallest height grows by one row while a route is planned, so a little less map shows.
