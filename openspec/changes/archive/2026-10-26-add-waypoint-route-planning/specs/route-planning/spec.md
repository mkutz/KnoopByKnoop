# Spec Delta

## Purpose

Lets a user plan a route over a knooppunt network by choosing waypoints and changing them freely, while the knots in between are computed.

## ADDED Requirements

### Requirement: Route from waypoints
A route SHALL be defined by an ordered list of at least two waypoints, each a knot of the same network. The route SHALL consist of the shortest path over the network between each pair of consecutive waypoints.

#### Scenario: Start and end only
- **WHEN** the user sets knot 12 as start and knot 45 as end
- **THEN** the route is the shortest knot sequence from 12 to 45, beginning with 12 and ending with 45

#### Scenario: Route through a stop
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** the route is the shortest path from 12 to 30 followed by the shortest path from 30 to 45, and knot 30 appears in it

#### Scenario: Unreachable waypoint
- **WHEN** a waypoint cannot be reached from the previous waypoint over the network
- **THEN** the route reports which leg is unreachable instead of returning a partial route

#### Scenario: Waypoints of different networks
- **WHEN** the waypoints include a cycling knot and a hiking knot
- **THEN** the route is rejected

### Requirement: Insert a stop between start and end
The user SHALL be able to insert a stop at any position between the first and the last waypoint without changing the other waypoints.

#### Scenario: Insert a stop at a given position
- **WHEN** the waypoints are 12 and 45 and the user inserts knot 30 at position 1
- **THEN** the waypoints are 12, 30, 45

#### Scenario: Insert a stop into the best leg
- **WHEN** the user adds a stop without choosing a position
- **THEN** the stop is inserted into the leg where it adds the least distance to the route

#### Scenario: Insert an unreachable stop without a position
- **WHEN** the user adds a stop without choosing a position and the stop cannot be reached from any leg
- **THEN** the stop is inserted right before the end and the route reports the unreachable leg

### Requirement: Reorder and remove stops
The user SHALL be able to move any waypoint to another position and to remove any stop, as long as at least two waypoints remain.

#### Scenario: Move a stop
- **WHEN** the waypoints are 12, 30, 7, 45 and the user moves 7 to position 1
- **THEN** the waypoints are 12, 7, 30, 45 and the route is recomputed accordingly

#### Scenario: Remove a stop
- **WHEN** the waypoints are 12, 30, 45 and the user removes 30
- **THEN** the waypoints are 12, 45

#### Scenario: Refuse to drop below two waypoints
- **WHEN** the route has exactly two waypoints and the user removes one
- **THEN** the removal is rejected and the route is unchanged

### Requirement: Round trips
A route SHALL allow the first and the last waypoint to be the same knot.

#### Scenario: Round trip through a stop
- **WHEN** the waypoints are 12, 30, 12
- **THEN** the route starts and ends at knot 12 and passes knot 30

### Requirement: Route summary
A route SHALL expose its full knot sequence and its total distance in meters.

#### Scenario: Consecutive legs share their waypoint once
- **WHEN** the waypoints are 12, 30, 45
- **THEN** knot 30 appears exactly once at the junction of both legs in the knot sequence
