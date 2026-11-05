# Proposal

## Why

Planning 04 → 06 → 02 → 28 over Siegburg 02, the maintainer found that the line goes down to the 02 by the river and back along the path between the two signposts, about 468 m from the junction at the bridge. They would ride straight to the north-west 02, 312 m. OpenStreetMap agrees: the route relation 02-06 has a branch to each of the two signposts.

The data keeps one course per connection, the shortest to any signpost of the knot. So the course cannot fit the leg after it.

## What Changes

- **A course to each signpost:** where a connection reaches a knot at several of its signposts along its own ways, the published data also has a course for each.
- **Routes through such a knot:** the line uses the signpost that fits both legs. 06 → 02 → 28 goes through the north-west 02 and 06 → 02 → 01 through the one by the river, each without the path between them.
- **Also without alternatives:** when two legs meet at one signpost anyway, e.g. 28 → 02 → 28, the line no longer runs on to the main position and back.
- **When the legs share no signpost,** the path between the signposts stays, as now.
- **Navigation:** riding the line that is shown never counts as off route, even where it runs more than 100 m from the knot's main course.
- **The file:** the courses are in an optional section at the end of the geometry file, which the released apps skip.

## Out of Scope

- **Distances and times:** a connection's length stays the one of its shortest course. Routes keep their length, and the 240 m or so of a path between signposts stay uncounted.
- **Routing:** the knots of a route do not change.
- **Telling the rider which signpost to take,** in notices or lists.
- **Choosing a signpost by hand.**

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-data`: a new requirement "Courses to each signpost".
- `map-planning`: a new requirement "Routes through a knot with several signposts".
- `knot-navigation`: a new requirement "Riding the line through a signpost".

## Impact

- **Pipeline:** `NetworkExtraction.connectionOf` finds the courses to each signpost; `Main` simplifies and writes them and logs how many there are.
- **Core:**
  - `NetworkGeometry` reads and writes the section and chooses the courses of a route.
  - `MapFeatures.route` and the GPX export use them.
  - `Navigation` also measures to them for off route.
- **Apps:** no code. A data run is needed after this is pushed.
- **Archive order:** after `pass-merged-knots` and `show-knot-signposts`, whose positions and trimming this builds on.
