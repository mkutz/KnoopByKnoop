# Design

## Context

- Core: `Navigation.progress(fix)` returns a `ProgressView` with the strip's marks (`RouteProgress.view`). Distances and arrivals are computed there for the stops, ferries and the end ahead, from the rider's place along the route, or off route from the next knot plus the straight distance; arrivals at the pace of "Ferry arrival estimate", rounded as on the strip. Ferry arrival states come from `FerryWatch`.
- Android: the panel is a `BottomSheetScaffold`; since `navigation-sheet-height` the sheet is fixed while navigating (`sheetSwipeEnabled = false`, no drag handle, peek height measured from the navigation part). The waypoint list is a `LazyColumn` (`WaypointList`) that takes the rest of the sheet and is empty while navigating.
- iOS: while navigating, the sheet has the single detent `.height(navigationHeight)` and no drag indicator; the waypoint `List` is empty then.
- Both maps follow the rider until the rider moves the map; a recenter button brings them back (`knot-navigation` "Following the rider").

## Goals / Non-Goals

**Goals:**
- The coming knots computed once, in the core, with the same distances, arrivals, rounding and ferry states as the strip.
- The list where the waypoint list is while planning, so it scrolls the same way and the header stays in place.

**Non-Goals:**
- Showing the list outside the app.

## Decisions

**Core: `ProgressView.coming`.** `RouteProgress.view` also returns the coming knots: a new immutable `ComingKnot(kind, knot, label, distance, arrival, position, state, description)` per knot from the next one to the end, and per ferry ahead between its knots, in riding order. Distance and arrival use the same `distanceTo` and `arrivalText` as the strip's labels, so they always agree; plain knots get them too. `knot` is the `Knot` (for showing it on the map), `null` for ferries; `description` is the screen reader text, e.g. `Knot 30, stop, 5.2 km, 18:40`, built in the core so both apps say the same.
- Alternative: give the strip's `ProgressMark` distances for plain knots too. Would mix what the strip shows with what the list shows; the strip's labels deliberately only cover stops, ferries and the end.

**Android: the sheet draggable again.** While navigating, `sheetSwipeEnabled` is true and the drag handle is back; the peek height is the navigation part plus the handle (measured as while planning). The coming knots are rows in `WaypointList`'s `LazyColumn` while navigating, in place of the waypoints, so the sheet expands up to the list's height and the list scrolls. The peek height follows a changed navigation height only while the sheet is at its smallest (as already while planning), so a sheet the rider pulled up stays up.

**iOS: detents `[navigation, .medium, .large]`.** While navigating, the detents are the measured navigation height plus `.medium` and `.large`, with the drag indicator visible; background interaction stays enabled up through `.medium`, which is a detent again. The coming knots are rows of the existing `List` while navigating.

**Showing a knot on the map.** The view models get `showOnMap(knot)`: it sets a shown knot (with a counter, so tapping the same knot again centers it again) and brings the sheet down to the navigation height. The maps react like to a user's move: stop following, animate to the knot at the navigation zoom, show the recenter button; recentering clears the shown knot. In landscape the side panel stays as it is.

## Risks / Trade-offs

- [Computing distances and arrivals for every knot ahead on each position] → A few hundred knots at most, simple arithmetic on precomputed offsets; the strip already walks all marks per position. Only done while the panel is shown, like the strip.
- [A tap in the list moves the map away from the rider while riding] → The recenter button is there, as after moving the map; the panel comes down so the map is visible.
