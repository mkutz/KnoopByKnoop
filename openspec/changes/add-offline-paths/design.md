# Design

## Context

See `proposal.md` for the motivation. The constraints are: offline after download, no app backend, data only from OpenStreetMap, shared logic in the core, and low battery use. `Navigation.precision` already samples `FINE` (3 s on Android, 10 m on iOS) whenever the rider is off route, so guidance needs no more location fixes.

### The Köln test of 2026-11-04

A throwaway test, not app code, on the Geofabrik extract of the Köln region.

| Question | Result |
|---|---|
| Rideable ways | 72,809 km |
| Whole region | 6.9 MB compressed, 10.5 MB uncompressed, with road class, surface, access tags and names |
| Squares of 1/16° | 312 squares, median 17 KB, largest 136 KB |
| Squares of 1/8° | 92 squares, median 62 KB, largest 485 KB |
| Siegburg to Beuel, 5 km either side | 812 KB in 15 squares of 1/16°, 1,085 KB in 6 of 1/8° |
| Routing, Python on a Mac, whole region in memory | 1 to 32 ms, 498 to 24,287 nodes visited |
| Cycle infrastructure | 3,611 km as its own line, 943 km only as a tag on a road |
| Decision points on nine trips, 44.6 km | 94, about 2 per km |

Compared with BRouter's trekking profile on its public server, on eight trips from public places:

| Trip | Test router km | BRouter km | Same way | On a main road without a lane |
|---|---|---|---|---|
| Siegburg Markt to 28 | 4.14 | 4.32 | 88 % | 0 % |
| Troisdorf Bf to 28 | 3.22 | 3.41 | 70 % | 9 % |
| Sankt Augustin Zentrum to 27 | 7.19 | 7.24 | 7 % | 1 % |
| Bonn Hbf to 81 | 2.41 | 2.71 | 76 % | 0 % |
| Beuel Bf to 80 | 1.24 | 1.19 | 58 % | 1 % |
| Hennef Bf to 02 south-east | 6.64 | 6.64 | 100 % | 3 % |
| Lohmar Markt to 02 north-west | 7.08 | 7.20 | 73 % | 0 % |
| Niederkassel Rathaus to 28 | 11.45 | 12.12 | 76 % | 5 % |

BRouter is a benchmark, not the truth. Nobody has ridden these ways, only the Köln region was measured, and the test router had no turn costs and turned back needlessly at least once.

## Goals / Non-Goals

**Goals:**
- A file and router small and simple enough to live in this project.
- Off route guidance that works with no signal once the squares are in.

**Non-Goals:**
- Matching BRouter's quality. The way only has to be sensible for a short stretch back to the route.
- A general routing engine: no elevation, profiles or alternatives.

## Decisions

### One file, read by range requests
All squares go in one `cycling.paths` release asset, with a directory listing each square's offset, size and SHA-256. The app reads only the byte ranges it needs. A range request for `cycling.graph` on the release returned HTTP 206 with the requested 16 bytes on 2026-11-04, through GitHub's redirect.

- **Alternative: one asset per square.** It would reuse the existing download code, but means thousands of assets, an index many times its size, and a publication that isn't replaced in one step.
- **Alternative: one file per region to download whole.** It would reveal less to GitHub, but an estimated 90 MB is too much for a rider who only wants one route.

A replaced file changes the data under a running download. The per-square checksum catches this, and the app then rereads the directory.

### Off until the user turns it on
The maintainer's decision: privacy as promised today holds unless the user accepts the change by turning the feature on. It's a switch in the info sheet, next to the appearance choice, and its explanation says what GitHub can see. It's stored like the other settings. The info sheet is only offered while planning, so it can't change during navigation.

Turning it off removes the squares, so nothing suggests the feature is still active. The publication and the index don't depend on the switch.

- **Alternative: always on, with a changed privacy policy.** Simpler, and more riders would benefit, but it would weaken the promise for everyone.
- **Alternative: asking the first time the rider is off route.** More riders would find it, but the question would come while riding, and the squares wouldn't be there yet.

### Squares of 1/16°
They needed less around the test route than 1/8° and cap a square at 136 KB in Köln, which also keeps memory down. The directory for an estimated few thousand squares is small enough to fetch once per data date. Each square is compressed on its own with deflate; the core has no decompression today, so it gets an `expect` function, like `sha256Hex`.

### An optional top-level `paths` entry in `index.json`
`NetworkIndex` ignores unknown keys, but `IndexedFileKind` is an enum, so a new `kind` in `files` would make released apps fail on the whole index. A new top-level key is ignored by them. It holds the file name, the data date, the format version, the directory's range, and the directory's SHA-256.

### What a square holds
Each way has its geometry simplified to 5 m (junctions always kept), its class, cost factor, access and one-way flags, and name. Rideable and not rideable ways are told apart, as `add-junction-pictures` draws motorways and trunk roads too. Node ids at shared points connect ways, also across squares. The size test left out ways that can't be ridden, so the file will be somewhat larger than the estimate.

### The cost table starts from the test
Cost is length times a factor, computed in the pipeline, so changing it needs no app release.

| Way | Factor |
|---|---|
| Cycleway, bicycle road | 1.0 |
| Path, footway, track or pedestrian way open to bicycles | at most 1.05 |
| Living street | 1.15 |
| Residential | 1.2 |
| Unclassified, track | 1.3 |
| Path | 1.4 |
| Service | 1.5 |
| Tertiary | 1.7 |
| Bridleway | 2.0 |
| Pedestrian | 2.2 |
| Secondary | 2.5 |
| Footway | 3.0 |
| Primary | 4.0 |
| Trunk, if open to bicycles | 5.0 |

A cycle lane reduces a road's excess over 1.0 to a quarter. Track grades 3 to 5 multiply by 1.4. Surfaces multiply: fine gravel and compacted 1.05, gravel 1.15, unpaved 1.3, sett 1.4, ground and dirt 1.6, cobblestone 1.7, woodchips 1.8, grass 2.0, mud and sand 2.5.

**New since the test:** a cost for turning back at a junction, because the test router did this needlessly at Siegstraße.

### A* over squares loaded on demand
The core gets a `PathRouter` over parsed squares, loaded when the search reaches them and not at all outside the area between rider and target. It's only called off route, on a fix when there's no way yet, or when leaving the way, so it doesn't run on every fix. It never runs on the main thread.

### Target and fallback in `Navigation`
`Navigation.wayToRoute` from `improve-off-route` already decides where the line ends. The guided way is a separate value next to it, so a failed route or missing data simply leaves today's behavior. A way that targets the course ends on the nearest path node within 30 m of the course ahead, which is also what brings the rider back on route.

### Fetching
Requests start 2 seconds after the route last changed. Long routes fetch more: the size grows with length, roughly 1 MB per 15 km around Bonn. It may be more in denser cities, and it isn't measured elsewhere.

## Risks / Trade-offs

- **[Risk] The phone is too slow or runs out of memory.** Python on a Mac says little, and `hiking-oom` shows memory is a real limit. → Task group 2 ends with a measurement on real phones, before any app work: at most 500 ms and 50 MB extra for a 12 km way. If this fails, the change stops and goes back to the maintainer.
- **[Risk] The whole area is bigger or takes the runner too long.** The 90 MB is scaled by land area from Köln and leaves out roads that can't be ridden. → Task 1.4 builds the real file before anything depends on it, with a 150 MB budget.
- **[Risk] Bad ways are drawn just as clearly as good ones.** → The manual checks need real rides, and the costs are in the pipeline, so they can change weekly without a release.
- **[Risk] OSM's cycle infrastructure is only a tag on a road in places.** About 21 % of it in Köln. → Those ways are still routed; they only look like a road.
- **[Trade-off] GitHub can see the rough area for those who turn the feature on.** Squares are requested around a route and the rider. → Off by default, and explained at the switch and in the privacy policy. The only way to avoid it would be downloading everything.
- **[Trade-off] Few riders may find a switch in the info sheet.** → Accepted for now. A hint can be added later without changing the data or the router.
- **[Risk] Turn notices come late at 3 s or 10 m between fixes.** → The notice comes at 100 m, about 20 s ahead at riding speed, so this should leave margin. To be checked on the ride.
- **[Risk] Network changes while the squares are a week old.** → Accepted. It's the same delay as the network data.

## Migration Plan

Publish the file first: older apps ignore the new index entry. The switch is off after updating, so nothing changes for existing users until they turn it on. Rollback is removing the entry, which leaves today's off route behavior. Apps without path data never lose anything.
