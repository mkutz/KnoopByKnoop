# Spec Delta

## ADDED Requirements

### Requirement: Way to the route on the map
While navigating and off route, the map SHALL show a dashed straight line from the rider's position to the knot to head for. While the map follows the rider, it SHALL keep both the rider and that knot in view.

#### Scenario: Line to the knot
- **WHEN** the rider is off route with 02 as the next knot
- **THEN** the map shows a dashed line from their position to 02

#### Scenario: Both in view
- **WHEN** the rider is off route 1.7 km from 02 and the map follows them
- **THEN** the map shows both their position and 02

#### Scenario: Back on route
- **WHEN** the rider comes back on route
- **THEN** the dashed line is gone and the map follows the rider as before

### Requirement: Direction to the route
While navigating and off route, the header SHALL show, next to the distance to the next knot, an arrow pointing towards it relative to the rider's riding direction, and the compass direction in words (north, north-east, east, south-east, south, south-west, west, north-west). When the rider has not moved recently, the arrow SHALL point relative to north, as the map.

#### Scenario: Direction in the header
- **WHEN** the rider is off route, rides north and 02 is 1.7 km to the north-east
- **THEN** the header shows "Off route, next 02 1.7 km", an arrow pointing to the front right and "north-east"

#### Scenario: Standing still
- **WHEN** the rider has not moved recently and 02 is to the east
- **THEN** the arrow points to the right, as east on the map

#### Scenario: Direction with a screen reader
- **WHEN** a screen reader reaches the header while off route
- **THEN** it announces "Off route, next 02, 1.7 km to the north-east"

### Requirement: Direction outside the app
While navigating and off route, the Android notification and the iOS Live Activity SHALL show the distance and compass direction to the next knot, e.g. "1.7 km north-east to 02", updated when the distance changed by 100 m or more, without more location fixes than navigating does.

#### Scenario: Lock screen off route
- **WHEN** the rider is off route with the screen off, 1.7 km south-west of 02
- **THEN** the lock screen shows "Off route, next 02" and "1.7 km north-east to 02"

#### Scenario: Updated on the way
- **WHEN** the rider got 100 m closer to 02
- **THEN** the lock screen shows the new distance

#### Scenario: Back on route outside the app
- **WHEN** the rider comes back on route
- **THEN** the lock screen shows the number to follow and the progress line as before

## MODIFIED Requirements

### Requirement: Start navigation
A complete route SHALL be navigable. Navigation SHALL be started by the user while the app is open, and SHALL begin where the rider is: at a knot of the route or on its course; more than 100 m from the course, it SHALL lead to the first knot of the route. Without courses, it SHALL begin at the route knot nearest to the rider. It SHALL need location access only while the app is in use, not always.

#### Scenario: Start at the first knot
- **WHEN** the route runs 24, 45, 12, 31 and the rider starts navigation standing at knot 24
- **THEN** navigation begins with 45 as the next knot

#### Scenario: Start at a knot in the middle of the route
- **WHEN** the rider starts navigation standing at knot 12 of the route 24, 45, 12, 31
- **THEN** navigation begins with 31 as the next knot

#### Scenario: Start between two knots
- **WHEN** the rider starts navigation on the course of the route between 45 and 12
- **THEN** navigation begins with 12 as the next knot

#### Scenario: Start away from the route
- **WHEN** the rider starts navigation 2 km away from the course of the route 24, 45, 12, 31, nearest to route knot 45
- **THEN** navigation begins with 24, the first knot, as the next knot and says the rider is off route

#### Scenario: Incomplete route
- **WHEN** the route has no start, no end, or an unreachable leg
- **THEN** navigation cannot be started

#### Scenario: Location access denied
- **WHEN** the user starts navigation and denies location access
- **THEN** navigation does not start, and the app says that it needs location access to navigate

#### Scenario: Joining the route on the way to the start
- **WHEN** the rider started away from the route, heads for 24 and on the way passes 12 of the route
- **THEN** navigation continues from 12 with 31 as the next knot, as when skipping ahead

#### Scenario: Start away from the route without courses
- **WHEN** the network's courses are not installed and the rider starts navigation nearest to route knot 45
- **THEN** navigation begins with 45 as the next knot

### Requirement: Off route
The rider SHALL be told they are off route when they pass a knot that is not on the route, or are more than 100 m away from the course of the route. The notice SHALL name the next knot and the straight distance and compass direction to it, e.g. "Off route, next 45, 1.7 km to the north-east". While off route, a guidance notice SHALL repeat the distance and direction when the rider got 500 m closer to the knot or its direction changed by more than 45° since the last notice. The app SHALL NOT plan a new route back.

#### Scenario: Passing a knot that is not on the route
- **WHEN** the rider heading to 45 passes knot 77, which is not on the route
- **THEN** the app says "Off route, next 45" with the distance and direction to 45

#### Scenario: Wrong turn between knots
- **WHEN** the rider heading to 45 leaves the course of the route and is more than 100 m away from it, with 45 1.7 km to the north-east
- **THEN** the app says "Off route, next 45, 1.7 km to the north-east"

#### Scenario: Back on route
- **WHEN** the rider who is off route comes back within 100 m of the course, or passes a knot of the route
- **THEN** the app says they are back on route and names the next knot

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
