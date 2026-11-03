# Design

## Context

- **Where the line goes:** `Navigation.drawnCourses` holds one course per leg from `NetworkGeometry.courses(knots)`, through the signposts that fit both neighboring legs. Each ends exactly at a position of its knots. Without courses, the fallback is straight lines between main positions.
- **Where distances go:** `distanceTo`, `bearing` and `wayTo` in `Navigation` and `RouteProgress.straightTo` all use `Knot.latitude` and `Knot.longitude`, the main position.
- **What the rider sees from them:**
  - `Navigation.progress` and `straightToFirst` give the distance before the first knot and off route.
  - `guidance`, `offRouteNotice`, `guide` and `inLanguage` give the off-route way.
  - `wayToRoute` falls back to the knot before the first knot and without a course.
  - `update` uses `distanceTo` for the reminder and `precision` for the finer sampling.
- **The ride:** in the recording of 2026-11-02 (16-22-42, 6:30 to 8:00), the route 02, 28, 02 starts at 02 and the courses are loaded. The rider comes from the north-east, off route with a dashed line to the 02 by the river. From 159 m they are back on route with 02 still next: "At 02 follow 28", down to about 72 m. Riding north-west along the path, the distance grows to about 138 m, then "Next 28" at the signpost. On a leg, the panel's distance comes from the route's offsets, not from a straight line.

## Goals / Non-Goals

**Goals:**
- A rider following the line never sees the straight distance to the knot grow because of which signpost is measured.
- One rule for every straight way to a knot.

**Non-Goals:**
- Changing distances along the route, the place on the strip or how a knot is passed.

## Decisions

**The signpost comes from the drawn line.** The new `Navigation.headedFor(index): GeoPoint` is the position of the knot that `drawnCourses[index - 1]` gets to first, within 10 m as for the trimming, or for index 0 the start of `drawnCourses[0]`. These are the courses the rider sees, and they are already chosen over the whole route.
- Alternative, the nearest signpost: it would stop the distance growing, but it points away from the route when the rider happens to be nearer the signpost the route does not use, and it would jump between signposts.
- When two legs share no signpost, the line runs along the path between them, and the arriving leg can run through one signpost and end at another. The target is the one reached first: on 28, 02, 01 the north-west 02 and on 01, 02, 28 the river 02. The end of the leg would miss the reminder on 28, 02, 01, as the rider passes the north-west 02 and never comes within 200 m of the river 02 before.

**`RouteProgress` gets the positions too.** `RouteProgress.of` gets the list of these points, and `straightTo` measures to them, so the panel, the strip and the line outside the app stay consistent. `Navigation.start` computes `drawnCourses` first and passes the points on.

**`wayTo`, `bearing` and `distanceTo` take a `GeoPoint`.** Passing a point instead of a knot means no call site can fall back to the main position by accident. `distanceToNearestNode`, used for passing, stays as it is.

**Reminder and finer sampling follow.** Both use the same distance. Nodes of a knot can be 500 m apart, so measuring to the main position could give the reminder after the signpost or leave the sampling coarse when passing it. `closeToPrevious` stays on main positions: it only tells whether two knots are close, and a reminder it suppresses was already named on passing.

**A new requirement instead of modified ones.** "Number to follow" and "Way to the route on the map" are modified by `fix-double-knots`, `improve-off-route` and `follow-number-when-close`, all still open. One requirement on top avoids a fourth delta on the same text and states the rule once.

## Risks / Trade-offs

- **The distance can jump when the leg changes state.** It can change by the gap between the signposts when courses arrive after a start without them. This only happens on a new start, as the route is fixed while navigating.
- **A rider heading for the other signpost still sees the distance grow.** Near the signpost by the river on a route through the north-west one, the distance grows until they pass within 30 m, when the knot counts as passed. That is the same distance the line shows, so the two agree.
- **The dashed line back to a leg can end on the main course** beside a branch that is shown. It is out of scope, and off route within 30 m of any course counts as back on route anyway.
- **Older ride checks:** distances in `manual-checks.md` of open changes were measured to main positions. Only knots with several signposts differ.
