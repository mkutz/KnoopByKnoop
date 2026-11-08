## ADDED Requirements

### Requirement: Route on the side of its direction
The route on the map, the line ahead while navigating, and the course navigation follows SHALL use, for each connection, the course of the direction the route takes it in.

#### Scenario: Route on its side of the road
- **WHEN** the route goes from 28 to 02 at the Sieg bridge in Siegburg-Zange, where each side of the road has its own way
- **THEN** the route on the map runs on the way for that direction, without crossing the road to the other side and back

#### Scenario: Out and back
- **WHEN** the route goes from 28 to 02 and back to 28
- **THEN** the map shows the way there and the way back on their own sides of the road

#### Scenario: Line ahead and off route
- **WHEN** the rider navigates from 02 to 28 on the way for that direction
- **THEN** the line ahead follows that way, and the rider is not told they are off route

#### Scenario: From a route point on the way back
- **WHEN** a stop lies on the connection between 02 and 06 and the route goes on from it to 02
- **THEN** the route from the stop to 02 follows the course from 06 to 02, not the course from 02 to 06 reversed

#### Scenario: Geometry without courses back
- **WHEN** the installed geometry file has no courses back, e.g. because it was published before this change
- **THEN** the route is drawn and followed as before
