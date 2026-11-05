# Design

## Context

- `NetworkExtraction.mergeSameKnots` groups nodes with the same ref within 500 m (unless a ferry separates them) and maps every member to the first, the node with the lowest ID. The other members' positions are dropped; their node IDs still count as the knot when routes are connected, and each course is extended to the representative's position.
- `Knot` has one position. `GraphFile` format version 2 writes the knots, the connections and the ferries; `GraphFile.read` stops after the ferry section and ignores anything after it.
- Released apps (0.5.0, 0.6.0) install a downloaded graph only if they support its format version; otherwise they ask for an app update (`network-sync` "Unsupported file version").
- `Navigation.entered` checks a knot's circle on the line from the previous precise fix; `start` and `lastWithin` (from `fix-double-knots`) measure straight to the knot; off-route knots are found with `KnotLocator.nearest`.

## Goals / Non-Goals

**Goals:**
- Passing any node of a merged knot passes the knot.
- Released apps keep installing the weekly data.

**Non-Goals:**
- Drawing merged knots or their courses differently.

## Decisions

**A trailing section in format version 2, no version 3.** After the ferries, the writer adds the number of knots with other nodes, then for each its knot index, the number of its other nodes, and each position as zigzag microdegree deltas from the knot's own position. `read` reads the section only when bytes are left after the ferries (a new `ByteReader.hasMore`), so files written before have no other nodes. Released readers stop after the ferries and never see it.
- Alternative: format version 3. Released apps would refuse every weekly update from the first publication on and ask riders to update, for a section they can ignore.
- Alternative: a separate file. Another download and index entry for a few kilobytes.
- Consequence: a later addition to version 2 must come after this section, and a reader must not treat trailing bytes as an error. Both go in the format comment in `GraphFile`.

**Other node positions on `Knot`.** `Knot` gets `otherNodes: List<GeoPoint> = listOf()` and `positions` (its own position first, then the others). Navigation and the locator already hold `Knot`s, so nothing needs the graph to look them up.
- Alternative: a map from knot ID to positions in `NetworkGraph`. `Navigation` would need it passed through `start` and the locator separately.

**All positions in navigation.** `entered(fix, knot)` is true when the previous precise fix was within 30 m of none of the knot's positions and the line from it, or the fix alone, comes within 30 m of any of them. `start`'s first knot within 30 m, `lastWithin` and being back on route 100 m from an off-route knot measure to the nearest position. `KnotLocator` puts a knot in the cell of every position, and its new `nearestAtAnyNode` returns the knot with the nearest position, for knots not on the route.
- Found while implementing: `KnotLocator.nearest` also finds the knot for a tap on the map, so it keeps measuring to the knot's own position; otherwise a tap near another node would select the knot, which `map-planning` does not ask for.

**Pipeline keeps the members.** `mergeSameKnots` builds the representative with the other members' positions, in node ID order. Everything else in the extraction stays.

**Rollout.** The core, the pipeline and the tests change in one commit. The next Monday publication writes the section; apps with the new core read it. A bundled graph with the section only replaces an installed one of the same data date when its format version is higher, which it is not, so riders get the section with the next weekly data, at most a week later.

## Risks / Trade-offs

- [The section breaks a released reader] → Checked before rollout: the core of 0.6.0 (`a493d81`) reads a graph with the section, in a throwaway test.
- [Graph size] → One position per extra node, a few bytes each; the size budget test runs on the real extract in `update-network-data`, and the pipeline log prints the file size.
- [A merged knot's nodes up to 500 m apart pass the knot at a node far from where it is drawn] → That is the junction riders see signposted; the next knot follows from there, and the reminder and the map stay at the drawn position.
