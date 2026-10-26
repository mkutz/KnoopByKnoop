# Tasks

## 1. Extraction

- [ ] 1.1 Choose an OSM PBF reader for the JVM and add it to `pipeline`
- [ ] 1.2 Extract knots and node-to-node routes per network type, with route lengths
- [ ] 1.3 Tests with a small checked-in OSM extract: "Cycling and hiking are kept apart", "Route lengths"

## 2. Graph file format

- [ ] 2.1 Define the format (header with version, network, region, data date, attribution) in `modules/core`
- [ ] 2.2 Writer in `pipeline`, reader in `modules/core`
- [ ] 2.3 Tests: round trip, "Unsupported format version", "Size budget" against a real NL extract (CI only)

## 3. Publication

- [ ] 3.1 Add `.github/workflows/update-network-data.yml` (weekly cron plus manual trigger) downloading Geofabrik extracts, running the pipeline and publishing files and index
- [ ] 3.2 Publish only after all regions were built ("Failed rebuild keeps old data")

## 4. Download in the apps

- [ ] 4.1 Region list and download in the core
- [ ] 4.2 Test: "Airplane mode" with a downloaded fixture region
