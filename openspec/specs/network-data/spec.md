# network-data Specification

## Purpose
Provides the apps with up-to-date knooppunt network graphs from OpenStreetMap, small enough to download per region and route on offline.

## Requirements

### Requirement: Extract node networks from OpenStreetMap
The pipeline SHALL extract every knot (a node with `rcn_ref` or `rwn_ref` in a `network:type=node_network`) and every node-to-node route of the cycling and hiking networks from an OpenStreetMap extract.

#### Scenario: Cycling and hiking are kept apart
- **WHEN** the extract contains a cycling and a hiking network in the same area
- **THEN** the pipeline writes a separate graph for each network type

#### Scenario: Route lengths
- **WHEN** a node-to-node route is extracted
- **THEN** its length in meters is computed from the geometry of its ways

### Requirement: One knot per signposted junction
Nodes of the same network with the same number that belong to one junction SHALL become one knot. Knots with the same number that are separate places SHALL stay separate knots. Only a ferry between two nodes makes them separate places.

#### Scenario: Junction tagged on several nodes
- **WHEN** knot 30 is tagged on two nodes within 500 m of each other, e.g. on both sides of a dual carriageway or around a roundabout, and no ferry lies between them
- **THEN** they become one knot 30, and routes arriving at either node meet there

#### Scenario: Connector inside a junction
- **WHEN** a short route of its own connects two nodes tagged 30 of the same junction, without a ferry
- **THEN** they still become one knot 30

#### Scenario: Equal numbers on both banks of a river
- **WHEN** knot 30 is tagged on both banks of a river and a route including a ferry connects the two
- **THEN** they stay two knots 30, connected by that route, so riders can choose which one to go to

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

### Requirement: Course of every connection
For every connection, the published data SHALL include the course of the path its length is measured on, accurate to within 5 m of the OpenStreetMap ways.

#### Scenario: Winding path
- **WHEN** a connection follows a winding forest path
- **THEN** its course follows the path's curves within 5 m, not a straight line between the knots

#### Scenario: Route in riding direction
- **WHEN** a route goes from knot 12 via 7 to 45
- **THEN** its course starts at 12, passes 7 and ends at 45 as one continuous line, regardless of the direction in which each connection was stored

#### Scenario: Junction tagged on several nodes
- **WHEN** a connection ends at a knot that was merged from several nodes
- **THEN** its course still ends at that knot's position

### Requirement: Geometry belongs to one graph
A geometry file SHALL belong to exactly one graph file. Geometry SHALL only be used together with the graph file it was built with.

#### Scenario: Matching files
- **WHEN** an app has the graph file and the geometry file of the same build
- **THEN** connections and routes are drawn along their actual course

#### Scenario: Mismatching or missing geometry
- **WHEN** an app has a graph file but no geometry file of the same build, e.g. because only the graph was updated
- **THEN** routing works unchanged, and connections are drawn as straight lines until a matching geometry file is installed

### Requirement: Published geometry files
The weekly publication SHALL include one geometry file per region and network type, listed in the index with its size, checksum and data date like the graph files.

#### Scenario: Index lists geometry
- **WHEN** an app reads the index
- **THEN** it finds for every graph file the matching geometry file, with size and checksum to verify the download

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole Netherlands is written
- **THEN** the file is smaller than 10 MB
