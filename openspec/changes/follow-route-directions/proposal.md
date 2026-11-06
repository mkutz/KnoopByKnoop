# Proposal

## Why

On a walk on 2026-11-06, the planned route led the maintainer across the street at a pedestrian light by the Sieg bridge in Siegburg-Zange, which felt wrong on the ground. There, the node network route (relation 8445495) is mapped with separate ways for each direction: members with the role `forward` or `backward`, used in one direction only.

The pipeline ignores member roles. It finds one course per connection on the route's ways as if they could be used both ways, and the apps use that course reversed for the other direction. So in one direction a course can run on the side meant for the other direction, with a crossing to get there and another to get back, or against a one-way stretch.

Measured on the extracts of 2026-11-06 (Netherlands, Belgium, North Rhine-Westphalia):
- **Cycling:** 8,740 of 33,115 routes have such members, 3,303 of them both `forward` and `backward`; 5,626 of 82,109 km of members carry a role, about 7 %.
- **Hiking:** 75 of 69,795 km, negligible, but handled the same way.

## What Changes

- **Pipeline:** keeps the roles of route members. A `forward` member is used only in the way's own direction, a `backward` member only against it, members without a role both ways. Each connection gets its course in each direction, and so does each course to a signpost; where a course back is not the course there reversed, it is published as well.
- **Geometry file:** sections with the courses back and the signpost courses back, after the bridge stretches, with their own bridge stretches. Released apps ignore them.
- **Core:** a route, the line ahead and navigation use the course of the riding direction. Without a course back, the course there is reversed, as today.

## Out of Scope

- **Lengths per direction:** a connection keeps one length, see the design.
- **Choosing a knot's signpost on one's side** and **avoiding crossings on the ways to and from the route**: own changes, if the rides show they are needed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-data`: a new requirement "Course in each riding direction".
- `map-planning`: a new requirement "Route on the side of its direction".

## Impact

- **Pipeline:** `PbfExtract` (member roles), `OsmRelation`, `NetworkExtraction` (directed neighbors, a course per direction), `Main` (simplifying the course back with its bridge stretches).
- **Core:** `NetworkGeometry` (reading and writing the section, the course of a direction in `courseOf`, `courses` and the route point overlay), `MapFeatures` through those.
- **Apps:** none expected; both draw what the core hands out.
- **Data:** the geometry files grow by the courses back, checked against the 10 MB budget.
