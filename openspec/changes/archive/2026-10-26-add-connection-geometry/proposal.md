# Proposal

## Why

The graph files hold the length of each connection, measured along the actual paths, but not their course. Apps can therefore only draw straight lines between knots, which looks wrong for winding paths and hides where a route actually goes. The pipeline already computes the path of every connection to measure it, so it can publish that path too. Doing this before the apps use the files means there is nothing to stay compatible with.

## What Changes

- The pipeline writes a geometry file per region and network type next to the graph file: for every connection the course of its path, simplified to what a map needs.
- `index.json` lists the geometry files like the graph files, with size, checksum and data date, so apps download and verify them the same way.
- A geometry file belongs to exactly one graph file. Apps detect a mismatching pair and fall back to straight lines instead of drawing wrong paths.
- The core reads geometry files and returns the course of a connection and of a whole route in riding direction.
- The weekly workflow publishes the geometry files and checks a size budget for them.

Out of scope:

- Separate geometries per riding direction where a route uses different ways in each direction; the first version keeps the one the length is measured on
- Drawing in the apps; `add-planning-screens` uses this instead of straight lines
- Elevation

## Capabilities

### New Capabilities

### Modified Capabilities
- `network-data`: adds geometry files to the published data, their pairing with graph files and their size budget

## Impact

- `pipeline`: keeps the node sequence of each connection, simplifies it and writes the geometry file
- `modules/core`: geometry file reader and writer, index entries with a file kind, route geometry
- `.github/workflows/update-network-data.yml`: publishes geometry files, checks their size budget
- `index.json` gets a new field per entry; no app reads the index yet, so nothing breaks
