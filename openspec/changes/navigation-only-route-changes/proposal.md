## Why

"Start on the route nearby" and "Ride around it" both change the plan for good. A plan made at home loses its start after one ride from somewhere else. A ferry closed one evening stays out of the plan on later days when it is open, and only clearing the plan brings it back. The maintainer wants both answers to count only for the ride they were given for (2026-11-12).

## What Changes

- Starting nearby and riding around a closed ferry change only the route of the coming navigation. During navigation it works as if the plan had been altered:
  - Starting nearby puts a temporary start (the start flag) at the place nearby, in place of the plan's start.
  - Riding around adds temporary knots on the way around, so the route goes around the ferry. The ride's route also leaves the ferry out, but nothing of that is stored.
- While navigating, the planner shows this altered route, with the temporary start as the start. The knots added to ride around show as ordinary knots of the route, not as stops.
- When navigation ends, by Stop or by arriving, the plan comes back exactly as it was before Navigate. This also holds when the app was closed or killed while navigating.
- Navigating the plan again asks both questions again when they apply. A ferry closed last time is in the route again, and is asked about again only if it is closed by then.
- With several closed ferries, Navigate asks one question per ferry, in riding order ("Closed ferry 1 of 2"). Each question zooms to that ferry and its own detour, with that detour's extra length and time, and each ferry can be ridden around or kept on its own. This is the maintainer's feedback from a long route to Den Helder (2026-11-12): one question named all the ferries but showed only the first detour.
- A ferry with several crossings in the network counts as one ferry: it is named once and left out with all its crossings. On that route Horsterveer was named twice.
- **BREAKING (stored routes):** the plan no longer stores avoided ferries. Routes saved with them read without them, so those ferries are back in the route.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`:
  - "Start on the route nearby": the new start counts only for the ride.
  - "Riding around a closed ferry": the way around counts only for the ride, through temporary knots, and is no longer avoided afterwards.
  - "Detour on the map": the map shows each ferry's own detour, and goes back to the plan after the ride.
  - "Riding around a closed ferry" also asks per ferry and counts a ferry with several crossings once.
  - A new requirement, "The plan after navigation", restores the plan when navigation ends.

## Impact

- Core:
  - `PlanningSession` loses `avoidedFerries` and `avoidingFerries`.
  - `ShortestPathRouter` keeps its avoided pairs, used only to find the way around.
  - `RouteSnapshot` drops `avoidedFerries`; old files still read.
  - `FerryDetour` returns the plan with the temporary knots. `MapFeatures.describe` learns which waypoints are temporary, so they are not stops.
- Android `AppViewModel` and iOS `AppModel`: they keep the plan as made beside the session being navigated, save only the plan as made, and restore it when navigation ends.
- Tests: `FerryDetourTest`, `StartChoiceTest`, `RealRide` and `openspec/checked-by-hand.md`.
- The ferry question asks per ferry, with a numbered title in all four languages. The map while asking shows that ferry's own detour.

## Out of scope

- Changing the route while navigating, or rerouting around a ferry mid-ride.
- Keeping a ride's changes in the plan on request, e.g. a "keep this start" choice.
- Offering the way around while planning.
- A "ride around all the rest" shortcut for routes with many closed ferries.
- The Lülsdorf–Wesseling operating times that sometimes give no status.
