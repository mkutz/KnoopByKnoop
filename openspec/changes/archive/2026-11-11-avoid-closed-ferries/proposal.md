# Proposal

## Why

A route over a ferry that is closed by the time the rider gets there only shows up as a spoken warning on the way ("The ferry Veer closes at 18:58, before you get there around 19:00"). Turning back then can cost a lot of time. The app knows the ferry's operating times and can estimate the arrival when navigation starts, so it can offer a way around before the ride begins. The maintainer asked for it on 2026-11-11, with the map showing the detour and the question naming its length.

## What Changes

- **A check on Navigate.** After the question where to start, the app estimates the arrival at each ferry on the route from the rider's position at the usual pace, as the ferry warning does.
- **The question.** If a ferry is closed then, or closes within 15 minutes after, the app asks before navigating:
  - the warning, e.g. "The ferry Lülsdorf–Wesseling closes at 19:00, before you get there around 19:40"
  - **Ride around it · 6.3 km longer, about 24 min**
  - **Keep the ferry**
- **The map shows the detour** while the question is open: it zooms to the ferry and the detour, and draws the detour as a second line next to the planned route.
- **Riding around it changes the plan for good.**
  - The same waypoints are planned again without that ferry.
  - If the new route hits another ferry closed by then, that one is left out too, until all ferries on it are open or none can be left out.
  - The plan keeps avoiding those ferries when it's edited later, and in the saved route.
- **No way around** (e.g. the ferry is the only link to an island): the question says so and offers only navigating with the ferry, and the spoken warning on the way stays.
- **Always offered,** with no limit on the detour's length; its length and time decide (the maintainer, 2026-11-11).
- **For cycling and hiking routes alike.**

Out of scope:
- **Re-routing during the ride,** when the rider is slower than estimated. The spoken warning stays as it is.
- **Offering it while planning,** before Navigate. A follow-up; the planner already shows the ferries.
- **A way to use an avoided ferry again** other than planning the route anew. A follow-up, if needed.
- **Ferries without operating times,** which can't be judged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: new requirements "Riding around a closed ferry" and "Detour on the map".

## Impact

- **`modules/core`:**
  - `ShortestPathRouter` can leave out connections.
  - `PlanningSession` keeps the avoided ferries, and `RouteSnapshot` stores them.
  - A new `FerryDetour.of(session, geometry, position, now, localTime, language)` estimates the arrivals with `FerryWatch`, plans the detour, and gives the question's texts, the extra length and time, and the detour's line and bounds.
- **`apps/android`:**
  - `AppViewModel.startNavigation` asks after the start choice.
  - The planner's dialog, and the map's detour line and zoom.
  - Strings in four languages.
- **`apps/ios`:** the same in `AppModel`, `PlannerView` and `NetworkMapView`.
