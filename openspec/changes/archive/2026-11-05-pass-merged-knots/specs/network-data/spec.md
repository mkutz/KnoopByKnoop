# Spec Delta

## MODIFIED Requirements

### Requirement: One knot per signposted junction
Nodes of the same network with the same number that belong to one junction SHALL become one knot. Knots with the same number that are separate places SHALL stay separate knots. Only a ferry between two nodes makes them separate places. The graph SHALL keep the position of every node a knot is tagged on.

#### Scenario: Junction tagged on several nodes
- **WHEN** knot 30 is tagged on two nodes within 500 m of each other, e.g. on both sides of a dual carriageway or around a roundabout, and no ferry lies between them
- **THEN** they become one knot 30, and routes arriving at either node meet there

#### Scenario: Connector inside a junction
- **WHEN** a short route of its own connects two nodes tagged 30 of the same junction, without a ferry
- **THEN** they still become one knot 30

#### Scenario: Equal numbers on both banks of a river
- **WHEN** knot 30 is tagged on both banks of a river and a route including a ferry connects the two
- **THEN** they stay two knots 30, connected by that route, so riders can choose which one to go to

#### Scenario: Positions of a merged knot
- **WHEN** knot 02 is tagged on two nodes 195 m apart and they become one knot
- **THEN** the graph file has the positions of both nodes for knot 02

### Requirement: Compact versioned graph files
Each graph file SHALL state its format version, its network type, the area it covers, the date of the OSM data and the ODbL attribution.

#### Scenario: Unsupported format version
- **WHEN** an app reads a graph file with a format version it does not support
- **THEN** it rejects the file with a clear error and keeps the previously installed data

#### Scenario: Size budget
- **WHEN** the graph for the cycling network of the whole covered area is written
- **THEN** the file is smaller than 5 MB

#### Scenario: Apps without the positions of merged knots
- **WHEN** an app released before graph files had the positions of all nodes of a merged knot reads such a file
- **THEN** it installs and reads it as before, each knot at one position

#### Scenario: Graph files without the positions of merged knots
- **WHEN** an app reads a graph file written before it had the positions of all nodes of a merged knot
- **THEN** it reads it as before, each knot with one position
