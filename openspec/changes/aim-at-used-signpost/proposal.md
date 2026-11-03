# Proposal

## Why

On the ride of 2026-11-02 towards Siegburg 02, the panel said "At 02 follow 28" with a distance growing from 77 to 145 m, and the dashed line pointed at the 02 by the river, although the route leaves from the north-west 02. At that signpost the app then said "Next 28", which was correct.

`routes-through-signposts` made the line on the map use the right signpost, and `pass-merged-knots` counts either signpost as passing the knot. Every straight distance and direction to a knot still measures to the knot's main position. At Siegburg that is the river, about 195 m from the signpost the route uses, so a rider doing the right thing sees the distance grow and a line pointing away from their way.

## What Changes

- **The signpost the line uses:** the straight distance and direction to the next knot go to the signpost where the route's line first reaches it, or for the first knot, where the line leaves it. A knot with one position behaves as before.
- **Where it applies:**
  - the distance in the panel, on the strip and outside the app, before the first knot and off route
  - the distance, compass direction and arrow in the off-route notices, header and line outside the app
  - the dashed line where it goes to the knot: before the first knot, or without courses
  - the 200 m reminder, and the finer location sampling within 400 m of the next knot
- **Passing stays as it is:** either signpost still counts as passing the knot.

## Out of Scope

- **Distances along the route:** on a leg, the distance to the next knot and all later ones still come from the connection's length, as `routes-through-signposts` decided.
- **The dashed line back to a leg:** off route after the first knot, it still ends on the leg's main course. On a branch to another signpost it can end beside the line shown. This has not been seen on a ride, and can be its own change if it is.
- **The rider's place on the strip.**
- **Saying which signpost to head for,** in notices or lists.
- **Rings and badges:** the next-knot ring stays at all signposts.
- **New data:** this uses the courses the apps already have.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: a new requirement "Straight ways to a knot with several signposts".

## Impact

- **Core:**
  - `Navigation` measures distances and directions to the signpost taken from `drawnCourses`.
  - `RouteProgress.straightTo` does the same.
  - Tests are in `NavigationTest`.
- **Apps:** no code expected. Both show what the core gives them through `guidance`, `wayToRoute` and `progress`.
- **Data:** no data run.
- **Archive order:** after `routes-through-signposts`, whose chosen courses this uses.
