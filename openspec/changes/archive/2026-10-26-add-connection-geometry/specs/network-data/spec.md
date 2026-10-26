# Spec Delta

## ADDED Requirements

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
