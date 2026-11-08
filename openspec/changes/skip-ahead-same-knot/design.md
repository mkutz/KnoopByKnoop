# Design

## Context

- `passedRouteIndex` returns the first route knot from `nextIndex` on that the fix entered, and the knots right after it within their circles. A knot the route visits again appears twice in `knots`; entering it again right after passing matches the later index.
- The ride's log (2026-11-08): "Next 15" at 14:41:18 after 83, "Next 30" at 14:41:56.

## Decisions

### Remember the knot passed and whether the rider got away from it
- Navigation keeps the id of the knot passed last and whether a fix since was more than 200 m (`REMINDER_METERS`) from it.
- In `passedRouteIndex`, an index whose knot is that knot is skipped while the rider has not been away.
- **Why 200 m:** clearly beyond the 30 m circle and the jitter at a junction, and short enough that a real loop back counts.
- **Alternative:** never counting a later visit of the knot just passed. That would break "Coming back after the loop".

## Risks / Trade-offs

- **[A tiny loop shorter than 200 m away]** → Its second visit does not count by entering the circle; the rider passes the knots of the loop anyway, which count as usual.

## Migration Plan

App update only.
