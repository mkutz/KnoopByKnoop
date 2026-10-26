# Design

## Context

- `NetworkExtraction.connectionOf` already finds, per route relation, the shortest node sequence between the nodes of its two knots (Dijkstra over the relation's ways) and keeps only its length. When several routes connect the same two knots, the shortest connection wins.
- Graph files (format version 1) store knots sorted by ID and connections in list order, referencing knots by index. They have no geometry.
- `index.json` entries describe one file each: region, network type, file name, size, SHA-256, data date. No app reads the index yet.

## Goals / Non-Goals

**Goals:**

- Routing stays independent of geometry: the graph file is unchanged, and an app without geometry plans exactly as before
- A geometry file costs about as much download as the graph file, not an order of magnitude more

**Non-Goals:**

- Geometry per riding direction, elevation, path surface

## Decisions

### Keep the node sequence, not only its length

Dijkstra in `connectionOf` additionally records predecessors, so the winning node sequence is known. The connection carries its coordinates along to the end of the extraction; when two routes connect the same knots, the geometry of the kept (shortest) one stays.

### Simplify with Douglas-Peucker at 5 m

Map display needs no point every few meters on a straight road. Douglas-Peucker with a tolerance of 5 m keeps every curve visible at the highest zoom levels the apps will use and drops most points on straight stretches. The first and last point are kept, and the line is extended to the knot's position if the path starts at another node of a merged junction.

*Alternative:* a fixed point every n meters. Rejected, because it keeps too many points on straight roads and too few in curves.

### A separate geometry file per graph file

`<region>-<network>.geometry`, built in the same run as `<region>-<network>.graph`:

| Field | Encoding |
|---|---|
| Magic `KBKO` | 4 bytes |
| Format version | 1 byte, `1` |
| SHA-256 of the graph file it belongs to | 32 bytes |
| Connection count | varint, must equal the graph's |
| Per connection, in the graph's connection order | point count, then latitude and longitude in microdegrees as zigzag deltas to the previous point, starting from the connection's `from` knot |

The graph file's checksum ties both files together: an app uses geometry only if it matches the installed graph file. Storing courses in the graph's connection order needs no IDs.

*Alternative:* geometry inside the graph file (format version 2). Rejected: the graph would outgrow its 5 MB budget, routing would need the bigger download, and geometry could not be skipped or updated separately.

### Index entries get a kind

`NetworkIndexEntry` gets `kind` (`graph` or `geometry`), defaulting to `graph` when missing. Region, network and data date stay as they are, so apps group the two files of a region and network.

### Route geometry in the core

`NetworkGeometry` maps a connection to its course. `courseOf(route)` walks the route's knot sequence and concatenates the courses of consecutive knots, reversed where the connection is stored in the other direction, dropping the duplicated point at each knot.

## Risks / Trade-offs

- **Size:** measured on the Dutch extract, simplification keeps about 20% of the points at about 3.8 bytes per point: 945 KB for cycling (247,000 points for 16,000 connections) and 1.9 MB for hiking. Belgium: 765 KB and 569 KB. Well below the estimated 2–3 MB and the 10 MB budget.
- **Merged junctions:** a course is extended to the position of its merged knot, which can be up to 500 m from the node the path starts at. The measured length of the connection does not include that stretch, so for about 1% of the Dutch cycling connections the drawn course is 25% or more longer than the measured length. Routing has had this inaccuracy since junctions are merged; the geometry only makes it visible.
- **One course for both directions:** where a route uses different ways per direction (one-way streets, dual carriageways), the drawn course may be the other direction's way. Acceptable for planning; navigation may need per-direction courses later.
- **Weekly builds change connection order:** geometry is only valid with its own graph build, which the checksum enforces. Apps must update both files together.

## Migration Plan

Nothing to migrate: graph files keep format version 1, and no released app reads the index yet. Once both are published, `add-planning-screens` downloads the geometry file together with the graph file and draws real paths.

## Open Questions

None.
