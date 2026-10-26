# Design

## Context

The apps route offline over knooppunt networks (see `openspec/specs/route-planning`). They need the networks as small graph files per region. The only data source is OpenStreetMap, via the Geofabrik extracts for the Netherlands, Belgium and Nordrhein-Westfalen.

## Decisions

### Reading OpenStreetMap data

- The pipeline reads `.osm.pbf` extracts with `org.openstreetmap.pbf:osmpbf`, the maintained low-level PBF parser.
- PBF files store nodes, then ways, then relations. The pipeline reads each extract in three passes, so it only keeps what it needs in memory:
  1. **Relations:** node network routes (`type=route`, `network:type=node_network`, `network=rcn` for cycling or `network=rwn` for hiking) and the IDs of their member ways
  2. **Ways:** the node sequences of those member ways
  3. **Nodes:** knots (`rcn_ref`/`rwn_ref` with `network:type=node_network`) and the coordinates of all nodes on member ways
- The extraction logic works on plain OSM elements, separate from the PBF reader, so tests can use small in-memory networks.

### Connections and their lengths

- A route relation becomes a connection between the two knots of its network that lie on its ways.
- Its length is the shortest distance between those two knots along the relation's ways. Many relations hold separate ways for both riding directions, so the sum of all way lengths would overcount.
- Relations with fewer or more than two knots of their network on them are skipped and counted by reason in the pipeline's log.

### Knots tagged on several nodes

- Mappers often tag a junction's number on several nodes: both sides of a dual carriageway, around a roundabout. In the Utrecht extract, 204 of 246 pairs of nodes sharing a number are less than 50 m apart.
- Without merging, routes arriving at one node and leaving from the other would not connect. So nodes of the same network with the same number within 100 m become one knot, represented by the node with the lowest ID.
- Knots with the same number can also be separate places, e.g. on both banks of a river, connected by a ferry. When a route of their own connects two nodes with the same number, they always stay separate knots, connected by that route, so riders can choose which one to go to.

### Graph file format

Binary, little effort to read on every platform, versioned:

| Field | Encoding |
|---|---|
| Magic `KBKG` | 4 bytes |
| Format version | 1 byte, currently `1` |
| Network type | 1 byte (0 cycling, 1 hiking) |
| Region ID, data date (ISO), attribution | length-prefixed UTF-8 strings |
| Knots | count, then per knot: ID (delta to previous, sorted ascending), ref string, latitude and longitude in microdegrees (zigzag delta to previous) |
| Connections | count, then per connection: from and to as knot indices, length in meters |

All integers are unsigned LEB128 varints, which keeps the Dutch cycling network well below the 5 MB budget. Reader and writer both live in `modules/core`, so round trips are tested in one place. The pipeline uses the writer.

### Publication

- A rolling GitHub Release with the tag `network-data`. Each weekly run replaces its assets: one file per region and network type (e.g. `netherlands-cycling.graph`) plus `index.json`.
- `index.json` lists the regions with network type, file name, size, SHA-256 and data date. The core parses it with kotlinx-serialization.
- The workflow builds every region in its own job. The publish job only runs after all regions were built and the size budget check passed, so a failed run leaves the last publication untouched.

### Downloading in the apps

- Downloading uses the native background download APIs (Android `DownloadManager`/WorkManager, iOS background `URLSession`): they are resumable and battery-friendly.
- The core stays free of networking. It parses the index and graph files from bytes, and verifies a file against the index's SHA-256 before an app replaces installed data.
- The download UI in the apps comes with its own change, together with the first planning screens. This change delivers everything the apps need for it.

## Risks

- **OSM data quality:** broken or incomplete route relations lead to missing connections. knooppuntnet already reports such errors to mappers; the pipeline only logs counts.
- **Rate limits and size:** the NRW extract is about 750 MB and the Netherlands about 1.3 GB. Both fit into a standard GitHub runner's disk and a weekly schedule.
