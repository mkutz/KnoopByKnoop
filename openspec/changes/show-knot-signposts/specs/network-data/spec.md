# Spec Delta

## MODIFIED Requirements

### Requirement: Course of every connection
For every connection, the published data SHALL include the course of the path its length is measured on, accurate to within 5 m of the OpenStreetMap ways. Where a connection reaches a merged knot at another node than the knot's position, its course SHALL continue to the knot's position along the network's ways between the two, and only where no such way exists in a straight line.

#### Scenario: Winding path
- **WHEN** a connection follows a winding forest path
- **THEN** its course follows the path's curves within 5 m, not a straight line between the knots

#### Scenario: Route in riding direction
- **WHEN** a route goes from knot 12 via 7 to 45
- **THEN** its course starts at 12, passes 7 and ends at 45 as one continuous line, regardless of the direction in which each connection was stored

#### Scenario: Junction tagged on several nodes
- **WHEN** a connection ends at a knot that was merged from several nodes
- **THEN** its course still ends at that knot's position

#### Scenario: Along the ways to the knot's position
- **WHEN** a connection reaches knot 02 at its node near the L 16, 195 m from the knot's position, and a way of the network leads from there to the knot's position
- **THEN** its course continues along that way to the knot's position, not straight across the road

#### Scenario: No way between the nodes
- **WHEN** no way of the network joins the node a connection reaches and the knot's position
- **THEN** its course continues in a straight line to the knot's position, as before
