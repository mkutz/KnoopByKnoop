# Design

## Context

- `Navigation.wayToRoute(fix)` returns `route.nearestAhead(fix, nextIndex, place)`: the nearest point of the course of the leg to the next knot, after the last place the rider was on the route (`place`), or the next knot without courses. The map's dashed line ends there, `WayGuide` looks for a way along paths to it, the header's direction points to it, and `MapFocus` keeps it in view.
- The maintainer's walk (2026-11-09, Siegburg): the leg ran west along the Sieg and then south to the end by the station; leaving it 100 m into the westward part and walking south-west, the nearest point was north-east on the river path, while the station part of the leg was about as close.

## Decisions

### Cost of a point: the way back plus the route after it
- For each point of the leg's course after `place`, the cost is the straight distance from the rider times 1.5, a usual detour factor on a street grid, plus the course's length from that point to the next knot. The target is the point with the least cost, searched along the course in 10 m steps.
- **Why the factor:** with straight distances alone, a point across a river or rails would look cheap; 1.5 keeps the jump modest. The guided way then finds the real way there.
- **Alternative:** route to many points along paths and compare. Exact, but a path search per candidate per fix is too slow on the phone.

### Keep the nearest point unless another saves 100 m
- The nearest point keeps its place unless the best point's cost is at least 100 m lower. Beside a straight course, cutting diagonally ahead saves only a little, so the line stays short and straight back, as in "Line to the route".

### Keep the target until another saves 100 m more
- `Navigation` keeps the last target while off route; a new best point replaces it only when it costs 100 m less than the kept one from the current position. The kept point must still lie ahead of `place`.
- **Why:** the cost changes with every fix; without a margin the line and the guided way could flip between two places.

### Only on the leg to the next knot
- Searching beyond the next knot would mean skipping it. Riding past later knots stays with "Skipping ahead".

## Risks / Trade-offs

- **[The detour factor misjudges a place]** → The guided way along paths shows the real way; with the margins the target only moves for a clear gain. The numbers are constants, easy to tune after rides.
- **[Without courses]** → Unchanged: the line goes to the next knot.
