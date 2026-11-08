# route-planning Specification

## Purpose
Lets a user plan a route over a knooppunt network by choosing waypoints and changing them freely, while the knots in between are computed.

## Requirements

### Requirement: Route from waypoints
A route SHALL be defined by an ordered list of at least two waypoints of the same network, each a knot or a route point, a chosen place that is not a knot. The route SHALL consist of the shortest path over the network between each pair of consecutive waypoints.

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

#### Scenario: Starting from a route point
- **WHEN** the start is a route point on the connection 02-06, 300 m from 02 and 1,700 m from 06, and the end is 28
- **THEN** the route is the shorter way from the point over 02 or 06 to 28, and its length counts only the part of 02-06 that is ridden

#### Scenario: Ending at a route point
- **WHEN** the start is 28 and the end is a route point on the connection 02-06
- **THEN** the route ends at the point, without riding on to the next knot

#### Scenario: Two route points on one connection
- **WHEN** two consecutive waypoints are route points on the same connection
- **THEN** that leg is the stretch of the connection between them, without either of its knots

#### Scenario: Passing a route point that is not on this leg
- **WHEN** the start is a route point on 02-06 and a later leg rides from 02 to 06
- **THEN** that leg is the whole connection, and the point is not among the knots it passes

#### Scenario: Length with a way to the network
- **WHEN** a route point away from the network is reached by a way of 320 m
- **THEN** the route's length includes those 320 m, each time the way is ridden

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

### Requirement: Waypoints are knots, not numbers
A waypoint that is a knot SHALL refer to a specific knot, not to its number, because several knots can share a number.

#### Scenario: Choose between knots with equal numbers
- **WHEN** knot 30 exists on both banks of a river, connected only by a ferry, and the user sets the one on the near bank as end
- **THEN** the route ends on the near bank and does not take the ferry

### Requirement: Route points as waypoints
A route point SHALL be movable and removable like a knot, and its role SHALL follow from its position. A route point added as a stop without a position SHALL be inserted into the leg where it adds the least distance.

#### Scenario: Moving a route point
- **WHEN** the waypoints are a route point, 30 and 45, and the user moves the point to position 1
- **THEN** the waypoints are 30, the point and 45, with 30 the start and the point a stop

#### Scenario: A stop on the route's own course
- **WHEN** the route rides 02 → 06 and the user adds a stop on that connection
- **THEN** the route's knots and length do not change, and the point is a stop between them

#### Scenario: A stop as a detour
- **WHEN** the route rides 02 → 06 and the user adds a stop away from the network, 320 m by its way from the course
- **THEN** the route rides to the point and back, and is 640 m longer

#### Scenario: Several route points
- **WHEN** the waypoints are a route point, 02, a route point, a route point and 28
- **THEN** the route passes them in that order
