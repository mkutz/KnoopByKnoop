# Spec Delta

## ADDED Requirements

### Requirement: Courses to each signpost
Where the ways of a connection's route reach one of its knots at several of the nodes the knot is tagged on, the published data SHALL also include a course to each of those nodes, along the route's own ways and not through another node of that knot. The connection's course and length SHALL stay as they are. Apps that do not know these courses SHALL still read the file.

#### Scenario: Two branches of a route
- **WHEN** the route 02-06 has one branch to the node of 02 by the river and one to its node near the L 16
- **THEN** the data has a course from 06 to each of the two nodes, and the connection keeps its shortest course and length

#### Scenario: Only through another node
- **WHEN** a route reaches a knot's second node only by passing its first
- **THEN** the data has no course of its own to the second node

#### Scenario: One node
- **WHEN** a route reaches a knot at only one of its nodes
- **THEN** the data has no further course for that connection

#### Scenario: Released apps
- **WHEN** an app of version 0.7.0 reads a geometry file with these courses
- **THEN** it reads the connections' courses as before, and ignores the others

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole covered area is written with these courses
- **THEN** the file is still smaller than 10 MB
