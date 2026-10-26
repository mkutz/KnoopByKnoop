# Spec Delta

## Purpose

Provides the apps with up-to-date knooppunt network graphs from OpenStreetMap, small enough to download per region and route on offline.

## ADDED Requirements

### Requirement: Extract node networks from OpenStreetMap
The pipeline SHALL extract every knot (a node with `rcn_ref` or `rwn_ref` in a `network:type=node_network`) and every node-to-node route of the cycling and hiking networks from an OpenStreetMap extract.

#### Scenario: Cycling and hiking are kept apart
- **WHEN** the extract contains a cycling and a hiking network in the same area
- **THEN** the pipeline writes a separate graph for each network type

#### Scenario: Route lengths
- **WHEN** a node-to-node route is extracted
- **THEN** its length in meters is computed from the geometry of its ways

### Requirement: Compact versioned graph files
Each graph file SHALL state its format version, its network type, its region, the date of the OSM data and the ODbL attribution.

#### Scenario: Unsupported format version
- **WHEN** an app reads a graph file with a format version it does not support
- **THEN** it rejects the file with a clear error and keeps the previously installed data

#### Scenario: Size budget
- **WHEN** the graph for the cycling network of the whole Netherlands is written
- **THEN** the file is smaller than 5 MB

### Requirement: Weekly publication
The network data SHALL be rebuilt and published at least once a week, together with an index of all available regions, their sizes and data dates.

#### Scenario: Failed rebuild keeps old data
- **WHEN** the weekly rebuild fails
- **THEN** the previously published files and index stay available unchanged

### Requirement: Offline use after download
An app SHALL be able to plan and navigate within a downloaded region without network access.

#### Scenario: Airplane mode
- **WHEN** a region has been downloaded and the device has no network access
- **THEN** routes within that region can still be planned
