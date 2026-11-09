## MODIFIED Requirements

### Requirement: Way to the route on the map
While navigating and off route, the map SHALL show a dashed straight line from the rider's position to the point of the route ahead of them where the way back plus the route from there to the next knot is shortest: on the course of the leg to the next knot, after the last place the rider was on the route. The nearest such point SHALL be the target unless another saves at least 100 m; once the line leads to such a point ahead, it SHALL keep leading ahead until that saves less than 50 m. Without the network's courses, or before the first knot, the line SHALL go to the next knot. On a guided way, the map SHALL show a solid line along that way instead of the dashed line. While the map follows the rider, it SHALL keep both the rider and the end of that line in view.

#### Scenario: Line to the route
- **WHEN** the rider heading to 02 left the course to the west and is off route 150 m from it, with 02 1.7 km ahead
- **THEN** the map shows a dashed line from their position to the nearest point of the course ahead of where they left it, about 150 m long, not to 02, as no other point saves 100 m

#### Scenario: Route behind the rider
- **WHEN** the rider is off route and the nearest point of the leg's course lies before the place where they left the route
- **THEN** the line goes to that place where they left the route, not back along the part they already rode

#### Scenario: Line to the knot
- **WHEN** the network's courses are not installed, or the rider is off route before the first knot 24
- **THEN** the map shows a dashed line from their position to the next knot

#### Scenario: Both in view
- **WHEN** the rider is off route 400 m from the course and the map follows them
- **THEN** the map shows both their position and the end of the line

#### Scenario: Back on route
- **WHEN** the rider comes back on route
- **THEN** the dashed line is gone and the map follows the rider as before

#### Scenario: Line along the guided way
- **WHEN** the rider is off route on a guided way
- **THEN** the map shows a solid line along the way and no dashed line

#### Scenario: Back to the dashed line
- **WHEN** the rider is off route and the guided way can't be replaced after leaving it
- **THEN** the map shows the dashed line again

#### Scenario: Walking on to where the route goes
- **WHEN** the leg runs 600 m west along a river and then 200 m south to the end, and the rider left it 100 m into the westward part and is now 300 m west and 150 m south of where they left it, 150 m from the river and 200 m from the southward part
- **THEN** the line leads west to the southward part of the leg, not back north to the nearer river

#### Scenario: Cutting a loop short
- **WHEN** the leg makes a 600 m loop that comes back within 80 m of where it started, and the rider leaves the route at the start of the loop and crosses towards its end, still nearer to the start
- **THEN** the line leads to the end of the loop, not back into it

#### Scenario: Steady target
- **WHEN** the line leads to a point ahead because that saves more than 100 m, and with the next position it saves only about 70 m
- **THEN** the line keeps leading ahead, not back to the nearest point
