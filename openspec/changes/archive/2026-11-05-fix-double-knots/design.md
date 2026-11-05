# Design

## Context

- `Navigation.passedRouteIndex` returns the first route knot from the next one whose circle the fix entered. `entered` deliberately ignores a knot whose circle the previous precise fix was already in, so standing at a knot does not pass it again and again. Together they make a double knot unpassable: the fix that passes the first knot is already inside the second one's circle.
- `Navigation.start` picks the first knot within 30 m (`startIndex`), so starting at a double knot has the same effect: the start fix becomes `lastPreciseFix`, inside the next knot's circle.
- `RouteProgress.placeOf` returns 0 for `nextIndex == 0`, and `view` and `line` measure the distance to the next knot as `offsets[nextIndex] - place`, which is 0 for the first knot. Off route, `view` gets the straight distance instead; on route it never does.

## Goals / Non-Goals

**Goals:**
- A double knot (or a run of knots within one circle) passes on the one fix that reaches them.
- Distances to the first knot that mean something.

**Non-Goals:**
- Changing `entered`: the "already inside" rule stays for single knots.

## Decisions

**Extend the passed knot through the run of knots the fix is within.** After `passedRouteIndex` found index `i`, move on to `i + 1`, `i + 2`, … while the fix is within 30 m of that knot, keeping the round-trip rule that the end only counts once it is the next knot. The rest of `update` stays: one `nextNotice` (or `backOnRouteNotice`, or `arrivedNotice` when the run reaches the end) for the knot after the last one, the place and pace at that knot, the ferry watch at that knot.
- Alternative: drop the "already inside" check for the knot right after the one just passed. Would also need the next fix to come, and gives two notices in quick succession, the first naming a knot the rider already stands at.
- Alternative: merge double knots in the pipeline. They are different signposts with different numbers, and the route lists both.

**Same at the start.** `startIndex` becomes the last knot of the run of consecutive route knots within 30 m, starting at the first one within 30 m, without the end of the route (as `dropLast(1)` does now).

**Straight distance to the first knot.** `Navigation.progress` passes the straight distance to the first knot to `RouteProgress.view` as it passes it off route, while `nextIndex == 0`, but without marking the strip off route. `line` gets the same: an optional straight distance used for the next knot and added to the route length for the end. The rider's dot stays at the start.
- Alternative: let `placeOf` return a negative place before the first knot. Would leak into the strip, the pace and the ferry estimate.

## Risks / Trade-offs

- [A fix within 30 m of the next two knots that the rider does not really pass, e.g. standing at the first and turning back] → Both knots are at the same junction, so being there is passing both; the rider is led on to the knot after them, as by skipping ahead.
- [Without precise fixes nothing changes] → Coarse fixes still never pass knots, as before.
