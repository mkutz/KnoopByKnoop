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
- **THEN** the route is drawn on top of the bridge where it runs over it, and below the semi-transparent bridge where it passes under it

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

### Requirement: Bridge edges on the route
When the geometry has bridge stretches, the route and the line of the route ahead SHALL get the edges of a bridge on both sides where they run on a bridge, thin and partly transparent, so a stretch over a bridge can be told apart at a glance from one under a bridge. The network's connections SHALL get no edges.

#### Scenario: Under, then onto the same bridge
- **WHEN** the route passes under a bridge and then climbs onto it
- **THEN** the map shows the semi-transparent bridge across the route first, and the route with bridge edges on the bridge itself

#### Scenario: Edges on the dark map
- **WHEN** the route runs on a bridge on the dark map
- **THEN** the edges are light enough to see

#### Scenario: Connection on a bridge
- **WHEN** a connection of the network runs on a bridge
- **THEN** it is drawn on top of the bridge without edges

### Requirement: Map content above and below bridges
Knots, route knots, the next knot, the rider's position, the way to the route and the guided way SHALL stay above the bridges drawn over the route. While navigating, the line of the route ahead SHALL be drawn like the route, with the edges of a bridge where it runs on one. The ridden track SHALL stay on top of the bridges and of the route's stretches on them, so it stays visible where the rider crossed a bridge. Tunnels SHALL be drawn as the base map draws them.

#### Scenario: Knots and position stay on top
- **WHEN** a knot or the rider's position is under a bridge
- **THEN** the knot's badge and the position are drawn above the bridge

#### Scenario: Line ahead while navigating
- **WHEN** the rider navigates a route that passes under a bridge and then runs over another one
- **THEN** the line of the route ahead is drawn below the first bridge, and on top of the second with the edges of a bridge

#### Scenario: Ridden track
- **WHEN** the rider has ridden over a bridge
- **THEN** the ridden track is visible on that bridge, on top of the route

#### Scenario: Tunnel
- **WHEN** the route runs through a tunnel
- **THEN** the route is drawn as before, and the tunnel as the base map draws it
