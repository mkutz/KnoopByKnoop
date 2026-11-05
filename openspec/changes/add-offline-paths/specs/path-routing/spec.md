# Spec Delta

## Purpose

Finds a way for a bicycle between two places on the device, from the published path data, preferring ways suited to cycling and without any network request.

## ADDED Requirements

### Requirement: Routing on the device
The app SHALL find a way between two places from the installed path data alone, without any network request.

#### Scenario: Without network access
- **WHEN** the squares between two places are installed and the device has no network access
- **THEN** the app finds a way between them

### Requirement: Ways suited to cycling
The router SHALL prefer, in this order, cycle paths and bicycle roads, quiet streets, and roads with a cycle lane, over main roads without one. It SHALL accept a longer way for this.

#### Scenario: Cycle path beside a main road
- **WHEN** a primary road without a cycle lane and a cycle path 20 % longer both connect two places
- **THEN** the way uses the cycle path

#### Scenario: Main road with a cycle lane
- **WHEN** a secondary road with a cycle lane and a residential street of the same length both connect two places, and the street is not cheaper by the cost table
- **THEN** the way may use the road with the lane, and does not count it as a main road without a lane

#### Scenario: A much longer detour
- **WHEN** the only way avoiding a tertiary road is three times as long
- **THEN** the way uses the tertiary road

#### Scenario: Poor surface
- **WHEN** a paved path and a muddy path of about the same length both connect two places
- **THEN** the way uses the paved path

### Requirement: Ways that are not used
The router SHALL NOT use motorways, trunk roads or steps unless they are tagged as open to bicycles, ways closed to bicycles, ways with a mandatory separate cycle path, private ways, parking aisles, driveways or ferries. It SHALL respect one-way streets, unless they are open to bicycles in both directions.

#### Scenario: Closed to bicycles
- **WHEN** the shortest connection is a way tagged `bicycle=no`
- **THEN** the way avoids it

#### Scenario: Mandatory separate cycle path
- **WHEN** a road is tagged `bicycle=use_sidepath`
- **THEN** the way doesn't use the road

#### Scenario: One-way street
- **WHEN** a street is one-way against the riding direction
- **THEN** the way doesn't use it against that direction

#### Scenario: One-way street open to bicycles
- **WHEN** a street is one-way with `oneway:bicycle=no`
- **THEN** the way may use it in both directions

#### Scenario: Ferry
- **WHEN** two places are only connected by a ferry
- **THEN** no way is found

### Requirement: No needless turning back
The router SHALL only reverse direction at a junction when no other way of reasonable length continues.

#### Scenario: Turning back to change sides
- **WHEN** two roads meet, and the cheapest way without a turn cost would go back along the arriving path to reach the other side
- **THEN** the way continues without turning back, if that adds less than 200 m

### Requirement: Starting in the riding direction
When given the rider's riding direction, the router SHALL count starting against it like turning back at a junction, and SHALL report whether the way it found starts against it.

#### Scenario: A way ahead about as short as the way back
- **WHEN** the rider rides along a path, and the way back to the target is less than 200 m shorter than the way ahead
- **THEN** the way goes ahead

#### Scenario: The way back much shorter
- **WHEN** the way back is more than 200 m shorter than the way ahead
- **THEN** the way goes back and is reported as starting against the riding direction

#### Scenario: Standing still
- **WHEN** the riding direction isn't known
- **THEN** the cheapest way is found, and it isn't reported as starting against the riding direction

### Requirement: Connecting to the paths
A way SHALL start and end on the nearest usable path within 200 m of the given place. A nearby path that isn't connected to the other place SHALL be skipped in favor of the next nearest.

#### Scenario: A stray piece of path
- **WHEN** the nearest path to the rider is a short piece with no connection to the target, and a connected path is 60 m away
- **THEN** the way starts on the connected path

#### Scenario: Far from any path
- **WHEN** the nearest path is more than 200 m from the rider
- **THEN** no way is found

#### Scenario: Riding on a path
- **WHEN** the rider is on a path, and another path 120 m away would give a cheaper way
- **THEN** the way starts on the path they are on

### Requirement: No way found
When the installed squares don't connect two places, the router SHALL report that there is no way, instead of a partial way.

#### Scenario: Squares missing in between
- **WHEN** the squares between the rider and the target aren't installed
- **THEN** no way is found
