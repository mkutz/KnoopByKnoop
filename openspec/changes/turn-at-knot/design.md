# Design

## Context

- **The guided way** classifies its turns in `PathRouter.directionOf` into `TurnDirection` (straight, slight, normal, sharp, back), using its angle limits (`STRAIGHT_DEGREES`, `SLIGHT_DEGREES`, `TURN_DEGREES`, `SHARP_DEGREES`).
- **`NetworkGeometry`** gives the route's line along the courses, trimmed to the signposts it uses ("Routes at the signpost they use"), including the path between two signposts of a merged knot.
- **`sign-like-header`** adds the plate and `HeaderRow.Close`, and **`sign-like-lock-screen`** adds the outside views and `notificationTitle`.

## Goals / Non-Goals

**Goals:**
- One turn per route knot, computed once per route, not per position fix.
- The same classification as the guided way, so "left" means the same angle everywhere.

**Non-Goals:**
- Telling which arm of the real signpost carries the number. OSM has no arm data, so the turn is the route's turn, which is what the rider rides.

## Decisions

### Turn from the route line, 40 m either side

**Choice:** for each route knot after the start, find the knot's place on the route line:
- the end of the incoming leg's course
- for a knot with several signposts the route rides between, the incoming leg's end and the outgoing leg's start

Then take the bearing from the point 40 m before it to it, and from it (the outgoing start) to the point 40 m after it, along the line and across further knots. The signed difference goes through `directionOf`, which is extracted from `PathRouter` into a shared function with the same limits.

**Why 40 m:** far enough to smooth a curved approach and a junction drawn with several nodes, short enough not to reach the next bend. The guided way merges turns within 40 m for the same reason.

**Alternative:** the bearing between the neighbouring knots. That is wrong on winding legs, where the knots can lie in a different direction from the way out of the junction.

### Computed with the route geometry, cached on the route

The turns are computed when the route's line is built, the same time the strip's distances are, and kept as `List<TurnDirection?>` by route knot index. Navigation only looks them up, so battery use doesn't change. `null` means unknown:
- the start knot
- no courses
- a leg shorter than 2 m on both sides

### A start knot gets no turn

There is no incoming course. The rider's riding direction before they move is unknown, and once they ride from the start knot, its plate is already behind them. So the start knot keeps the plain arrow.

### Display

- **`HeaderRow.Close`** gets `turn: TurnDirection?`.
- **Apps:** draw the plate's arrow from it: bent arrows for left and right, diagonal arrows for slight and sharp, a U-turn for back, and the plain → for `null`.
- **Android title:** uses ↑ ↖ ↰ ↙ ↗ ↱ ↘ ↩ before the number to follow.
- **Screen readers** get "to the left", "slightly to the right" and so on.

## Risks / Trade-offs

- **[Route turn ≠ arm on the sign]** At a roundabout or an offset junction, the sign's arm can point differently from the route's overall turn. → The 40 m window follows the ridden line. A wrong arrow seen on rides goes into the ride notes, and the window can be tuned.
- **[Turning back at a merged knot]** The route may leave a knot from another signpost after riding past it. → Measured over the line between the signposts, as in the "Knot with several signposts" scenario.
- **[Geometry data older than the route-directions change]** The courses may still ignore one-way members. → The turn follows whatever line the route draws, so it is as right as the drawn route.

## Migration Plan

App-only, no data change. Without the change, the plate shows the plain → as before.
