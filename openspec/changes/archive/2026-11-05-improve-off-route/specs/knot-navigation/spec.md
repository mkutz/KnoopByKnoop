# Spec Delta

## MODIFIED Requirements

### Requirement: Number to follow
While navigating, the app SHALL show the number to follow at the next knot as its most prominent information, e.g. "At 45 follow 12", with the knots as badges like on the map, and in the same line the distance to the next knot along the route, or straight to it while off route or while the next knot is the first knot of the route. When the next knot is the end of the route, it SHALL say so instead.

#### Scenario: Next knot in the middle of the route
- **WHEN** the next knot is 45 and the route continues to 12
- **THEN** the app shows "At 45 follow 12" with 45 and 12 as badges, 12 as the most prominent element, and the distance to 45, e.g. "800 m"

#### Scenario: Next knot is the end
- **WHEN** the next knot is 31, the end of the route
- **THEN** the app shows that 31 is the end of the route

#### Scenario: Distance off route
- **WHEN** the rider is off route, 1.7 km straight from the next knot 02
- **THEN** the app shows "Off route, next 02" with 02 as a badge and "1.7 km"

#### Scenario: Distance to the first knot
- **WHEN** the rider started away from the route 24, 45, 12, 31, came back within 30 m of its course 80 m straight from 24, and 24 is still the next knot
- **THEN** the app shows "At 24 follow 45" and "80 m", and outside the app "80 m to 24", not "0 m"

### Requirement: Off route
The rider SHALL be told they are off route when they pass a knot that is not on the route, or are more than 100 m away from the course of the route. They SHALL be back on route only when they pass a knot of the route or come within 30 m of the course, so a path running alongside the route does not count as the route. The notice SHALL name the next knot and the straight distance and compass direction to it, e.g. "Off route, next 45, 1.7 km to the north-east". While off route, a guidance notice SHALL repeat the distance and direction when the rider got 500 m closer to the knot or its direction changed by more than 45° since the last notice. The app SHALL NOT plan a new route back.

#### Scenario: Passing a knot that is not on the route
- **WHEN** the rider heading to 45 passes knot 77, which is not on the route
- **THEN** the app says "Off route, next 45" with the distance and direction to 45

#### Scenario: Wrong turn between knots
- **WHEN** the rider heading to 45 leaves the course of the route and is more than 100 m away from it, with 45 1.7 km to the north-east
- **THEN** the app says "Off route, next 45, 1.7 km to the north-east"

#### Scenario: Back on route
- **WHEN** the rider who is off route comes back within 30 m of the course, or passes a knot of the route
- **THEN** the app says they are back on route and names the next knot

#### Scenario: Parallel path
- **WHEN** the rider who is off route rides on a path 60 m beside the course
- **THEN** they are still off route, and the app does not say they are back on route

#### Scenario: Without geometry
- **WHEN** the network's courses are not installed
- **THEN** off route is only detected by passing a knot that is not on the route

#### Scenario: Off route is announced once
- **WHEN** the rider stays off route for several minutes
- **THEN** the app says "Off route" once, not repeatedly, and only the guidance notices follow

#### Scenario: Getting closer
- **WHEN** the rider is off route and got 500 m closer to 45 since the last notice, now 1.2 km to the north
- **THEN** the app says "Next 45, 1.2 km to the north"

#### Scenario: Direction changes
- **WHEN** the rider is off route and the direction to 45 changed from north-east to south-east
- **THEN** the app says the distance and the new direction, e.g. "Next 45, 1.4 km to the south-east"

#### Scenario: Riding around without getting closer
- **WHEN** the rider is off route, neither 500 m closer nor in a direction more than 45° off since the last notice
- **THEN** the app says nothing

### Requirement: Way to the route on the map
While navigating and off route, the map SHALL show a dashed straight line from the rider's position to the nearest point of the route ahead of them: on the course of the leg to the next knot, after the last place the rider was on the route. Without the network's courses, or before the first knot, the line SHALL go to the next knot. While the map follows the rider, it SHALL keep both the rider and the end of that line in view.

#### Scenario: Line to the route
- **WHEN** the rider heading to 02 left the course to the west and is off route 150 m from it, with 02 1.7 km ahead
- **THEN** the map shows a dashed line from their position to the nearest point of the course ahead of where they left it, about 150 m long, not to 02

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
