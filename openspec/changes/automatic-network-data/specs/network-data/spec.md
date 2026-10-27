# Spec Delta

## MODIFIED Requirements

### Requirement: Compact versioned graph files
Each graph file SHALL state its format version, its network type, the area it covers, the date of the OSM data and the ODbL attribution.

#### Scenario: Unsupported format version
- **WHEN** an app reads a graph file with a format version it does not support
- **THEN** it rejects the file with a clear error and keeps the previously installed data

#### Scenario: Size budget
- **WHEN** the graph for the cycling network of the whole covered area is written
- **THEN** the file is smaller than 5 MB

### Requirement: Weekly publication
The network data SHALL be rebuilt and published at least once a week, together with an index of all published files, their network types, sizes, checksums and data dates.

#### Scenario: Failed rebuild keeps old data
- **WHEN** the weekly rebuild fails, including when only one of the extracts cannot be downloaded or read
- **THEN** the previously published files and index stay available unchanged

### Requirement: Offline use after download
An app SHALL be able to plan and navigate on an installed network without network access.

#### Scenario: Airplane mode
- **WHEN** the cycling network has been installed and the device has no network access
- **THEN** cycling routes can still be planned anywhere in the covered area

### Requirement: Published geometry files
The weekly publication SHALL include one geometry file per network type, listed in the index with its size, checksum and data date like the graph files.

#### Scenario: Index lists geometry
- **WHEN** an app reads the index
- **THEN** it finds for every graph file the matching geometry file, with size and checksum to verify the download

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole covered area is written
- **THEN** the file is smaller than 10 MB

## ADDED Requirements

### Requirement: One graph per network type
The pipeline SHALL build one graph and one geometry file per network type from all extracts of the covered area together. Elements that appear in more than one extract SHALL be used once.

#### Scenario: Covered area
- **WHEN** the weekly publication is built
- **THEN** it contains one cycling and one hiking graph covering the Netherlands, Belgium and Nordrhein-Westfalen together

#### Scenario: Knot on a border
- **WHEN** a knot lies in the overlap of two extracts
- **THEN** the graph contains it once, with the connections from both sides

#### Scenario: Data date of several extracts
- **WHEN** the extracts have different data dates
- **THEN** the graph states the oldest of them

### Requirement: Routes across borders
Connections that cross the border between two extracts SHALL be part of the graph, so routes can be planned across borders like any other route.

#### Scenario: Connection cut by an extract border
- **WHEN** a node-to-node route crosses the border between the Dutch and the Belgian extract, so that neither extract alone contains all of its ways
- **THEN** the graph contains the connection with its full length and course

#### Scenario: Route from the Netherlands into Belgium
- **WHEN** the user plans a cycling route from a knot in Zeeland to a knot in Flanders
- **THEN** the route crosses the border along the signposted network
