## ADDED Requirements

### Requirement: Route and bridges
When the geometry has bridge stretches, the map SHALL draw the base map's bridges, road, path and railway bridges with their outlines, over the route and the network's connections, with the same transparency everywhere, so the lines stay visible below them. The stretches of the route and of the connections that run on a bridge SHALL be drawn above the bridges. The route and the connections SHALL look the same below a bridge as anywhere else.

#### Scenario: Route under a bridge
- **WHEN** the planned route passes under a motorway bridge
- **THEN** the bridge is drawn semi-transparent over the route, and the route is visible below it

#### Scenario: Route over a bridge
- **WHEN** the planned route between 28 and 29 runs over a bridge
- **THEN** the route is drawn on top of that bridge

#### Scenario: Over and under the same bridge
- **WHEN** an out-and-back route runs over a bridge on the way out, and under that bridge on the way back
- **THEN** the route is drawn on top of the bridge where it runs over it, and below it where it passes under it

#### Scenario: Connections and bridges
- **WHEN** a connection of the network passes under a bridge, and another one runs over it
- **THEN** the first is drawn below the semi-transparent bridge and the second on top of it

#### Scenario: Same transparency everywhere
- **WHEN** a bridge is near the route and another one is far from it
- **THEN** both are drawn with the same transparency, and the route keeps its colour and width below the bridge

#### Scenario: Dark map
- **WHEN** the route passes under a bridge on the dark map
- **THEN** the bridge is drawn semi-transparent in the dark map's colours over the route

#### Scenario: Offline map
- **WHEN** the base map cannot be loaded and the offline map is shown
- **THEN** no bridges are drawn, and the route is drawn as before

#### Scenario: Geometry without bridge stretches
- **WHEN** the installed geometry file has no bridge stretches, e.g. because it was published before this change
- **THEN** no bridges are drawn over the route or the connections

### Requirement: Map content above and below bridges
Knots, route knots, the next knot, the rider's position, the way to the route and the guided way SHALL stay above the bridges drawn over the route. While navigating, the line of the route ahead SHALL be drawn like the route. The ridden track SHALL stay below the bridges. Tunnels SHALL be drawn as the base map draws them.

#### Scenario: Knots and position stay on top
- **WHEN** a knot or the rider's position is under a bridge
- **THEN** the knot's badge and the position are drawn above the bridge

#### Scenario: Line ahead while navigating
- **WHEN** the rider navigates a route that passes under a bridge and then runs over another one
- **THEN** the line of the route ahead is drawn below the first bridge and on top of the second

#### Scenario: Ridden track
- **WHEN** the ridden track runs over a bridge
- **THEN** it is drawn below that bridge

#### Scenario: Tunnel
- **WHEN** the route runs through a tunnel
- **THEN** the route is drawn as before, and the tunnel as the base map draws it
