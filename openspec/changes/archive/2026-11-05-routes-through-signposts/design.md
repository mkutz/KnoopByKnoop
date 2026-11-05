# Design

## Context

- **Siegburg 02** (from OpenStreetMap, 2026-11-02): node 59469043 by the river and node 81461844 near the L 16.
  - Relation 02-06 splits at node 36382152 on the Bonner Straße: 245 m to the river node, 312 m to the other. It also holds the 223 m path between them.
  - Relations 02-28 and 01-02 hold that path too. 01-02 leaves at the river node, 02-28 at the other.
- **Courses** (`NetworkExtraction.connectionOf`): the shortest path along the route's ways from any node of one knot to any node of the other, extended along the ways to each knot's position. One course per connection; the connection's length is that path's.
- **Trimming** (`NetworkGeometry.atSignposts`, from `show-knot-signposts`): a route's first and last course start and end at the signpost they pass within 10 m. Inner legs keep their whole course.
- **The file:** `NetworkGeometry.read` stops after the courses and does not look at what follows, and `FORMAT_VERSION` must match exactly. So a trailing section is skipped by released apps, and the version stays 1. `GraphFile` did the same for the merged knots' positions.
- **Off route** (`Navigation.distanceToCourse`): the distance to the courses of the current leg and its neighbors.

## Goals / Non-Goals

**Goals:**
- A line that does not run to a signpost and back when the rider need not.
- Data readable by released apps, and within the size budget.

**Non-Goals:**
- Changing lengths, routing or the rider's place on the strip.

## Decisions

**Published: one course per pair of nodes reached.** For a connection, the pipeline looks for the shortest path along the route's own ways between each node of one knot and each node of the other, without passing through another node of those knots. With two or more pairs, all are written in a section after the courses: the connection's index, then for each pair the two positions' indexes in `Knot.positions` and the course, simplified to 5 m like the others.
- Connections with one pair have no entry, as today's course and trimming already suffice.
- Alternative: only the last stretch after the branch. It would save bytes, but needs splicing on every reader. The size is measured first (task 1.1), and this is the fallback if the budget is at risk.

**Choosing over the whole route.** `NetworkGeometry` gets `courses(knots): List<List<GeoPoint>>`, one course per pair of neighboring knots, used by `courseOf(route)` and `MapFeatures.route`. It finds the shortest line through the route's knots, knot by knot, remembering the shortest way to each position of the current knot:
- Each leg offers its pairs, and its main course from and to the knot's main position and each signpost it passes within 10 m, as `atSignposts` did.
- Neighboring legs must use the same position of their knot. The main position is one of them, so legs with no signpost in common join there along the path between the signposts, as now.
- At the ends of a route, the shortest wins, so this replaces `atSignposts`, and 02 → 28 still starts at the north-west signpost.

`MapFeatures.route` draws one line per leg between waypoints, so a stop at a knot is chosen like a knot passed: it works on all the route's knots, then splits the result by leg.

**Navigation measures to every course.** `Navigation` gets each leg's main course for the place, as now, and the other courses only for `distanceToCourse`. The branch at Siegburg stays within 32 m of the main course and the path, but others may not: nodes of a knot can be 500 m apart, and the shown line must never count as off route. The line's end for the way back (`wayToRoute`) stays on the main course.

**No change to the place on the strip.** A rider on another branch is projected onto the main course as anywhere near it, and the knot counts as passed at either signpost.

## Risks / Trade-offs

- **How many connections are affected:** measured on the Köln extract, 19 of 1,222 cycling connections and none of 704 hiking ones, against 28 knots with several signposts there, and the cycling geometry file grows 3.9% (144,303 to 149,926 bytes). Small overall but common at those knots, and far from the budget, so the change went on. The full network is measured with the data run.
- **The line can differ from the shortest distance shown:** at Siegburg 67 m longer than the main course to the river 02. The route's distance stays the same, which is a stated non-goal, and small against the rounding.
- **A route relation with stray ways** could offer an odd branch. It is what is mapped, and `Course of every connection` has the same exposure.
