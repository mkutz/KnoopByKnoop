# Design

## Context

- **OSM roles:** in route relations, `forward` means the route uses the member only in the way's own direction, `backward` only against it; an empty role means both. Node network routes use them for one-way stretches and for a separate way on each side of a road.
- **Pipeline today:** `PbfExtract.toOsmRelation` keeps only the member way IDs. `NetworkExtraction.connectionOf` builds an undirected neighbor map of the route's ways, takes the shortest path between the two knots, and stores it as the course from the connection's `from` knot to its `to` knot. Where several routes connect the same knots, the shorter is kept.
- **Core today:** `NetworkGeometry.courseOf`, `courses(knots)` and the route point overlay reverse a course when it is used from `to` to `from`. Navigation follows `courseOf(route)`. Lines for the map are split at bridges by segment lookup (`show-route-on-bridges`), so they need no direction handling.
- **Geometry file:** courses, then the signpost courses, then the two bridge sections. Readers since 0.7.0 stop after the sections they know.

## Goals / Non-Goals

**Goals:**
- Each direction of a connection follows the ways the route marks for it.
- Nothing changes where a route has no marks, and nothing changes in apps without the new section.
- The core picks the right course everywhere a course is used in a direction.

**Non-Goals:**
- Lengths per direction (see below).
- Guessing sides where the route is not marked, e.g. from `cycleway:right` tags.

## Decisions

### Directed paths from the roles
- **Pipeline:** `OsmRelation` keeps each member's role. The route's neighbor map becomes directed: a `forward` member adds edges in node order only, a `backward` member against it, others both.
- **Courses:** the course there is the shortest directed path from the `from` knot to the `to` knot; the course back the shortest directed path from `to` to `from`. If a direction finds no path, it uses the other one reversed ("Marks that leave no way"). If neither does, the undirected path of today is used.
- **Course back stored only when different:** compared after simplification, so a course back that is the same ways reversed is not stored.
- **Merging routes:** where several routes connect the same knots, the route kept is chosen by the length there as today, and brings its course back with it.
- **Alternative:** a course per member role split at the point the two sides join. Needs the same directed search and more bookkeeping.

### Courses to each signpost in each direction
- **Why:** the first local build showed the route from 06 to 02 in Zange taking an undirected signpost course instead of the course back: 02 has two signposts, and `courses(knots)` picks the shortest of the course and the signpost courses. 1,961 of the 6,475 cycling connections with a course back also have signpost courses. Added to the change on 2026-11-06 after asking the maintainer.
- **Pipeline:** each signpost course is searched on the directed ways too, there and back, with the same fallbacks as the course; a `SignpostCourse` gets its course back where it differs.
- **File:** one more section after the courses back: the connection index, the signpost course's position in its list, its points from the `to` knot's signpost, and its bridge stretches.
- **Core:** the options for the way back use a signpost course's course back where it has one, otherwise the course reversed, as today.
- **One-way signpost courses:** unlike a connection's course, a signpost course with no way in one direction is not reversed for it; it is left out in that direction, as the knot's other signposts are offered. The section marks it "there only" or "back only". Found at Zange, where the way back reaches 02's second signpost first.

### One length per connection
- The graph file keeps one length per connection, the length of the course there. A course back is usually a few meters longer or shorter, a side of the same road.
- The data build reports how much the two directions differ, the median and the largest. If many differ by more than 100 m, lengths per direction become their own change; the graph file format would need a version.

### The courses back in a section after the bridges
- **Format:** the number of courses back, then for each the delta-coded connection index, its points coded like the courses (from its own start, the `to` knot), and its bridge stretches as in the bridge section.
- **Compatibility:** after all known sections, so 0.7.0 to 0.10.0 read the file as before.
- **Size:** one more set of points for the connections that differ; the data build reports the size against the 10 MB budget.

### The core picks the course of the direction
- `NetworkGeometry` gets `coursesBack: Map<Int, List<GeoPoint>>`. One private function gives the course of a connection in a direction: the course there, or the course back if there is one, otherwise the course there reversed. `courseOf`, `courses(knots)` and the route point overlay use it instead of reversing themselves.
- **Route points** on a connection cut the course there; one on the way back is placed on that course reversed, as today. A route point beside the course back is rare and would only move the cut by the road's width.
- Bridge stretches of the courses back join the segment set the map lines are split with, so bridges work on both.

## Risks / Trade-offs

- **[Routes mapped with wrong roles]** → A direction that finds no way falls back to the other reversed, so no connection is lost. A wrongly marked one-way stretch can make a direction's course a detour; the data build lists courses back more than twice as long as the course there, to look at.
- **[Lengths]** → see "One length per connection".
- **[Old apps]** → They keep showing one course reversed, as today.

## Migration Plan

1. Ship pipeline and core; published geometry gains the section, released apps ignore it.
2. Apps of the next release use the courses back once the weekly data has them.
3. **Rollback:** geometry without the section, or an app without this change, behaves as today.
