## Context

How it works today:
- Starting nearby calls `PlanningSession.startOnRouteAt`, and the apps take the result as the plan: `update` on Android, `change` on iOS. Both write `route.json`.
- Riding around takes `FerryDetour.around`, a session replanned with `avoidingFerries`. It carries `avoidedFerries` into every later edit and into `route.json`.
- The apps follow the navigation through `Navigating.state` on Android and `Navigator.shared.state` on iOS. While it is set, `update`/`change` do nothing ("Route is fixed while navigating").
- Navigation itself gets a `NavigationRoute` built from the session's knots, its `routeGraph`, the geometry and the description's `stopPositions`.

## Goals / Non-Goals

**Goals:**
- One place holds the plan as made, and one holds the route being navigated. Only the plan as made is ever stored.
- The way around is expressed as waypoints, so navigation, the map and the description need nothing ferry-specific.

**Non-Goals:**
- Keeping a navigation's route across an app restart. Navigation does not survive a restart today, and the plan as made is what comes back.

## Decisions

### Temporary waypoints in the session, marked by ID

`PlanningSession` gets `temporary: Set<Long>`: the IDs of the waypoints added only for this navigation. Two new functions create sessions with it:
- `startOnRouteAt(nearby)` stays and marks a new route point as temporary. A knot of the network that was already a waypoint is not marked.
- `throughDetour(knots)` inserts the given knots as stops into the legs they belong to, marked as temporary.

`MapFeatures.describe` gives temporary waypoints other than the start the role of an ordinary route knot, not `STOP`. Then `stopPositions`, the panel and the map treat them like any knot on the way. The temporary start keeps the start role and its flag.

*Alternative:* a separate "navigation plan" type wrapping two sessions. It was rejected because the description, map layers and `NavigationRoute` all take a `PlanningSession`, so the wrapper would need to be unpacked everywhere.

*Alternative:* keeping only avoided ferries in a navigation-only session, without knots. It was rejected because the maintainer asked for temporary knots: they make the ride's route look and behave like an altered plan. The avoided set stays in the router as well, because knots alone do not pin the legs.

### Choosing the temporary knots of the way around

`FerryDetour` still finds the way around with `ShortestPathRouter(graph, avoided)`, as now. That avoided set lives only inside this computation.

For each differing stretch, it takes the stretch's inner knots (those not on the planned route) and adds the middle one as a temporary stop. A temporary knot alone does not hold the route on the way around: between two knots, the shortest way can still go back over the ferry. A test with a long connection on the far side showed this. So the navigation-only session is routed with the same avoided set. It is the session's router, which is never stored, so the plan as made and `route.json` stay free of it. If the route still takes a closed ferry, or is more than 1% longer than the way around, all inner knots of the stretches are used instead.

`FerryDetour.around` becomes this session with temporary stops. `extraDistance` and `extraTime` are measured on it.

### The apps keep the plan as made

- Android: `AppState.session` stays the shown session. A new `private var plannedSession` holds the plan as made while a navigation-only session is shown.
- iOS: the same on `AppModel`.

`startNearby` and `rideAroundFerries` set the shown session without saving it. A new `showForNavigation(session)` does what `update` does without `storage.saveRoute`. The first such call remembers the current session as `plannedSession`.

When the navigation state goes from set to `null`, a pending `plannedSession` replaces the shown session, and the layers and description are made again from it. The same happens when a question is dismissed after starting nearby: there, `checkFerries` follows `startNearby`, so dismissing the ferry question must restore too.

Every place that saves the route while navigating (e.g. iOS's reattach after new data) saves `plannedSession ?: session`.

### Stored routes

`RouteSnapshot.avoidedFerries` is removed, and the parser already ignores unknown keys. `PlanningSession.avoidedFerries`, `avoidingFerries` and the `start(..., avoidedFerries)` parameter go. `ShortestPathRouter` keeps its `avoided` parameter for `FerryDetour`.

## Risks / Trade-offs

- **Middle knot not enough:** the replanned leg might go back over the ferry or take a shorter variant. → Check it and fall back to all inner knots. A test covers a detour where one middle knot is not enough.
- **Temporary stop at a route point of the plan:** a stretch's inner knot could be a route point that is already a waypoint. → Only knots not already among the waypoints are added.
- **Restore missed:** navigation could end while the view model or model is not observing, e.g. after process death. → Nothing to restore then, because `route.json` was never overwritten.
- **Navigation ended by the system:** arrival or a crash of the service still sets the state to `null`, which restores.

## Migration Plan

Routes saved with `avoidedFerries` read without them. The ferry comes back into those routes, which is the wanted behaviour. No rollback concern: an older app reading a new `route.json` misses only the field it would default anyway.
