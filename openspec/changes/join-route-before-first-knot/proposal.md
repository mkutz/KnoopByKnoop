# Proposal

## Why

On a ride on 2026-11-08 the maintainer started navigating the route 06 → 02 about 250 m from its course near Siegburg-Dreesch. As specified, navigation led to the first knot, 06, 5 km north. The maintainer rode to the course and along it towards 02, but navigation kept heading for 06:

- it said "back on route", while the header still showed "06 4.4 km";
- no line ahead was drawn, as there is none before the first knot;
- riding away from 06 along the course was not noticed;
- only after leaving the course again it said "off route" and guided 5.3 km back to 06.

The cause: before the first knot, being within 30 m of the first leg's course already ends "off route", but the next knot stays the first one. The route is only joined on the way to the start at a knot of the route.

## What Changes

- Before the first knot, a rider who follows the course of a leg of the route joins it there: the next knot becomes the end of that leg, the line ahead is drawn and the header counts down to it, as when starting on the course ("Start between two knots").
- Crossing the course, e.g. at a road the route passes, does not join it.
- Starting more than 100 m from the course still leads to the first knot.

## Out of Scope

- **The position hidden under the search field** while the header shows a turn, seen on the same ride. It belongs to the header's own change (`sign-like-header`) or the map's follow logic, and is raised there.
- **Riding the wrong way before the first knot**: once joined, the existing wrong-way watch applies.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: a new requirement "Joining the course on the way to the start".

## Impact

- **Core:** `Navigation.update` (the off-route branch before the first knot), sharing the leg choice with `Navigation.start`.
- **Apps:** none; both show what the core reports.
