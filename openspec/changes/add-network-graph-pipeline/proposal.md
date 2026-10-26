# Proposal

## Why

The apps need the knooppunt networks offline and without a backend. OpenStreetMap holds the networks for cycling and hiking, but in a raw form that is far too big and too generic for a phone. A pipeline must turn it into small, ready-to-route graph files per region, and keep them fresh.

## What Changes

- The `pipeline` CLI reads an OpenStreetMap extract and writes one graph file per region and network type.
- A versioned, compact graph file format that the core can load on all platforms.
- A weekly GitHub Actions workflow runs the pipeline and publishes the files plus an index as static files.
- The apps can list available regions and download one.

Out of scope: background map tiles, routing (see `add-waypoint-route-planning`), detecting errors in OSM data (knooppuntnet already does this).

## Capabilities

### New Capabilities
- `network-data`: extracting, publishing and loading knooppunt network graphs per region

### Modified Capabilities

## Impact

- `pipeline`: OSM PBF parsing dependency, graph builder, file writer
- `modules/core`: graph file reader
- `.github/workflows/update-network-data.yml`: new scheduled workflow
- Published files must carry the ODbL attribution
