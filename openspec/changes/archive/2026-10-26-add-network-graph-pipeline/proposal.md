# Proposal

## Why

The apps need the knooppunt networks offline and without a backend. OpenStreetMap holds the networks for cycling and hiking, but in a raw form that is far too big and too generic for a phone. A pipeline must turn it into small, ready-to-route graph files per region, and keep them fresh.

## What Changes

- The `pipeline` CLI reads an OpenStreetMap extract and writes one graph file per region and network type, plus an index of all files.
- A versioned, compact graph file format that the core can read on all platforms.
- A weekly GitHub Actions workflow builds the Netherlands, Belgium and Nordrhein-Westfalen and publishes the files as a rolling GitHub Release.
- The core parses the index and graph files and verifies downloads, so the apps can list regions, download one and plan routes on it offline.

Out of scope: the download UI in the apps (comes with the first planning screens), background map tiles, routing (see `route-planning`), detecting errors in OSM data (knooppuntnet already does this), regions beyond the three above.

## Capabilities

### New Capabilities
- `network-data`: extracting, publishing and loading knooppunt network graphs per region

### Modified Capabilities
- `route-planning`: waypoints refer to specific knots, since several knots can share a number

## Impact

- `pipeline`: OSM PBF parser dependency (`org.openstreetmap.pbf:osmpbf`), extraction, CLI
- `modules/core`: graph file reader and writer, index parsing (adds kotlinx-serialization)
- `.github/workflows/update-network-data.yml`: new scheduled workflow publishing to the `network-data` release
- Published files carry the ODbL attribution
