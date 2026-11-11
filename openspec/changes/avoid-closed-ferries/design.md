# Design

## Context

- **Ferry warning:** `FerryWatch.start(route, graph, localTime, offset, nextIndex, timeMillis, metersPerSecond, …)` estimates the arrival at each ferry along a `RouteProgress` at a pace, and gives a warning when the ferry is closed then or closes within 15 minutes after ("Ferry warning"). Its texts exist in four languages.
- **Navigate:** `AppViewModel.startNavigation` / `AppModel.startNavigation` already get one position and ask the start question (`StartChoice`) before navigating.
- **Routing:** `ShortestPathRouter` is a Dijkstra over a `NetworkGraph`. `PlanningSession.planned` builds the router from the route graph, and `RouteSnapshot` stores the waypoints.
- **Real test data:** the area `rhine-luelsdorf` holds the ferry Lülsdorf–Wesseling with its operating times, and bridges within reach.

## Goals / Non-Goals

**Goals:**
- One core function decides whether to ask, plans the detour, and gives the texts and the line, for both apps.
- The avoided ferries stay avoided for this plan.

**Non-Goals:**
- Re-routing mid-ride, asking while planning, and an undo.

## Decisions

1. **The check: `FerryDetour.of(session, geometry, position, nowMillis, localTime, language)`.**
   - **Arrivals:** a `RouteProgress` of the planned route and a `FerryWatch.start` with the rider at the route's start. The start time is moved later by the time the rider needs to reach the start: the straight distance at the network's usual pace. So the arrivals match what navigation will say.
   - **The trigger:** a ferry whose arrival state is a warning starts the question. Ferries without operating times give no warning, so they never ask.
   - **The detour:**
     - The same waypoints are routed with a router that leaves out the closed ferries' connections.
     - The new route's ferries are checked the same way; closed ones are added to the left-out set and it is routed again, at most five times.
     - If a leg can't be routed, there's no way around.
   - **Result:** a `FerryDetour` with
     - the warnings (one line per ferry)
     - the avoided connections
     - the new route's knots, its extra metres and its extra time at the usual pace
     - the detour's line: the new route's course between the first and the last knot where it differs from the planned one
     - the bounds of that line together with the ferries' courses

     It is `null` when nothing is closed.

2. **Avoiding in the plan.**
   - `ShortestPathRouter(graph, avoided: Set<Pair<Long, Long>>)` skips those knot pairs, either way round.
   - `PlanningSession` keeps `avoidedFerries`, uses them for every route it plans, and `avoidingFerries(set)` plans the waypoints anew with them. The set is cleared with the plan (`clear`, network switch).
   - `RouteSnapshot` stores them as `avoidedFerries: List<List<Long>>` (pairs of knot IDs), absent in older snapshots.

3. **Order at Navigate.** First the start question (it may cut the plan), then the ferry check on the plan as it is then, with the same position.

4. **The question.**
   - Android: a card at the bottom of the screen without dimming, so the map above stays readable (a centred dialog would hide the way around). iOS: a `confirmationDialog`, which iOS 26 shows as a card above the panel.
   - Dismissing it starts nothing, as with the start question. Without a position within the 10 seconds Navigate waits, it navigates without asking, also as with the start question.
   - **Texts:**
     - the warnings as the message
     - "Ride around it · 6.3 km longer, about 24 min"
     - "Keep the ferry"
     - without a way around, the message "There is no way around it" and only "Navigate with the ferry"

     The extra time uses `RouteProgress.formatDuration` and the length `MapFeatures.formatDistance`. Strings in four languages in the apps; the core gives the numbers and the warnings.

5. **The map.**
   - While the question is open, the app passes the detour's line and bounds to the map. The map draws the line dashed in the route's orange with a casing, apart from the solid route, and fits the bounds above the question: on Android by the card's measured height; on iOS the fit is measured on the screen and corrected, as MapLibre's own fit doesn't keep a padding that large, against the whole screen, since the map reaches behind the safe areas.
   - On answer the line is removed; riding around shows the new plan's route, as after any edit.

## Risks / Trade-offs

- **Pace estimate:** the usual pace can be off by a lot. The question shows the arrival time, so the rider can judge, and the spoken warning on the way still follows the real pace.
- **Long detours:** no limit, by decision. A detour of 40 km shows as such.
- **Avoidance surprises later:** the plan keeps avoiding a ferry that might be open on another day. That's a follow-up (an undo, or forgetting the avoidance on another day).
