# Tasks

## 1. Extraction

- [x] 1.1 Add `osmpbf` to `pipeline` and read extracts in three passes (relations, ways, nodes)
- [x] 1.2 Extract knots and connections per network type from plain OSM elements, with lengths along the relation's ways
- [x] 1.3 Tests with small in-memory OSM networks: "Cycling and hiking are kept apart", "Route lengths", relations with a wrong number of knots are skipped
- [x] 1.5 Merge nodes of one junction into one knot, keep knots with equal numbers apart when a route of their own connects them, with tests for both scenarios of "One knot per signposted junction"
- [x] 1.6 Test "Choose between knots with equal numbers" in `modules/core`
- [x] 1.4 Smoke run on a real extract (Geofabrik `netherlands/utrecht`) and check the result for plausibility

## 2. Graph file format

- [x] 2.1 Writer and reader of the format from `design.md` in `modules/core`
- [x] 2.2 Tests: round trip, "Unsupported format version"
- [x] 2.3 "Size budget": the workflow fails if the Dutch cycling graph is 5 MB or larger

## 3. Publication

- [x] 3.1 `pipeline` commands: `build` (extract to graph files) and `index` (write `index.json` with sizes, SHA-256 and data dates)
- [x] 3.2 Index parsing and SHA-256 verification of downloaded files in `modules/core`, with tests
- [x] 3.3 `.github/workflows/update-network-data.yml`: weekly cron plus manual trigger, one job per region, publish job replacing the `network-data` release assets
- [x] 3.4 Publish only after all regions were built ("Failed rebuild keeps old data")

## 4. Offline use

- [x] 4.1 Test "Airplane mode": load a graph from bytes and plan a route on it, with no network access in the core
