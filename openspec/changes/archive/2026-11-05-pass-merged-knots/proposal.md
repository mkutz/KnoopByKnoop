# Proposal

## Why

On the test ride on 2026-11-01 (Pixel 7), the first knot of the route, cycling knot 02 in Siegburg, never counted as passed, and navigation waited for it until a later knot was passed. In OpenStreetMap, 02 is tagged on two nodes 195 m apart: 59469043 and 81461844 to the north-west. The pipeline merges them into one knot, as `network-data` "One knot per signposted junction" asks, but keeps only the position of the node with the lowest ID. Navigation counts a knot as passed within 30 m of that position, so a rider passing the junction at its other node never passes the knot.

## What Changes

- The graph files keep the positions of all nodes a merged knot is tagged on, in a new section after the ferries. Apps released before this change read the files as before and ignore it, so the format version stays 2.
- A knot counts as passed within 30 m of any of its nodes: the next knot, a later knot of the route (skipping ahead), and a knot not on the route (off route).
- Navigation started within 30 m of any node of a route knot begins there.

## Out of Scope

- Where the map shows a merged knot: its badge stays at one node.
- The course drawn between a merged knot's nodes: it still runs to the knot's position, as now.
- The reminder 200 m before a knot, and the straight distance and direction off route: they keep counting to the knot's position.
- Changing which nodes are merged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-data`: "One knot per signposted junction" keeps the positions of all nodes of a merged knot; "Compact versioned graph files" gets a scenario for apps that do not know them yet.
- `knot-navigation`: "Passing a knot" and "Start navigation" count any node of a knot. Both requirements are also modified by `fix-double-knots`; this delta builds on that one, so `fix-double-knots` is archived first.

## Impact

- Pipeline: `NetworkExtraction.mergeSameKnots` keeps the other members' positions.
- Core: `Knot` gets the positions of its other nodes; `GraphFile` writes and reads the new section; `KnotLocator` and `Navigation` (`entered`, `start`, the run of passed knots from `fix-double-knots`) use all positions.
- Data: the next weekly publication of `update-network-data` writes the section; the bundled graphs get it with the next build after that.
- Apps: no code changes expected; Swift does not construct `Knot`.
