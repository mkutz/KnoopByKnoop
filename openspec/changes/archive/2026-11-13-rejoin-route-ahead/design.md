# Design

## Context

- `Navigation.wayToRoute(fix)` returns `route.nearestAhead(fix, nextIndex, place)`: the nearest point of the course of the leg to the next knot, after the last place the rider was on the route (`place`), or the next knot without courses. The map's dashed line ends there, `WayGuide` looks for a way along paths to it, the header's direction points to it, and `MapFocus` keeps it in view.
- The maintainer's walk (2026-11-09, Siegburg): the leg ran west along the Sieg and then south to the end by the station; leaving it 100 m into the westward part and walking south-west, the nearest point was north-east on the river path, while the station part of the leg was about as close.

## Decisions

### Cost of a point: the way back plus the route after it
- For each point of the leg's course after `place`, the cost is the straight distance from the rider times 3 plus the course's length from that point to the next knot. The target is the point with the least cost, searched along the course in 10 m steps.
- **Why 3 (changed during apply, first 1.5):** with 1.5, a rider 300 m beside a straight course was led diagonally to the knot ahead, which saves 114 m on paper, against "Direction to the route" "Back to the route, not to the knot", and four of its tests failed. What the walk and the loop save is route skipped where it bends or loops, not cutting across open ground. With 3, beside a straight course the nearest point stays the target up to about 550 m off it; the walk still saves over 400 m, the loop 580 m. It also keeps points across a river or rails from looking cheap; the guided way finds the real way there.
- **Alternative:** route to many points along paths and compare. Exact, but a path search per candidate per fix is too slow on the phone.

### Keep the nearest point unless another saves 100 m
- The nearest point keeps its place unless the best point's cost is at least 100 m lower. Beside a straight course, cutting diagonally ahead saves only a little, so the line stays short and straight back, as in "Line to the route".

### Two thresholds against flipping
- `Navigation` keeps the last target while off route. Leading to a point ahead starts when it saves 100 m over the nearest point and stops when it saves less than 50 m.
- **Changed during apply:** the first rule kept the target until another saved 100 m more. That also kept a point the rider had walked away from, against "Route behind the rider". Flipping only matters between the nearest point and one ahead; the nearest point itself moves smoothly.

### Only on the leg to the next knot
- Searching beyond the next knot would mean skipping it. Riding past later knots stays with "Skipping ahead".

## Risks / Trade-offs

- **[The detour factor misjudges a place]** → The guided way along paths shows the real way; with the margins the target only moves for a clear gain. The numbers are constants, easy to tune after rides.
- **[Without courses]** → Unchanged: the line goes to the next knot.
