# Spec Delta

## MODIFIED Requirements

### Requirement: Start navigation
A complete route SHALL be navigable. Navigation SHALL be started by the user while the app is open, and SHALL begin where the rider is: at a knot of the route, within 30 m of any of the nodes it is tagged on, or on its course; more than 100 m from the course, it SHALL lead to the first knot of the route. Standing within 30 m of several consecutive knots of the route, it SHALL begin after the last of them. Without courses, it SHALL begin at the route knot nearest to the rider. It SHALL need location access only while the app is in use, not always.

#### Scenario: Start at the first knot
- **WHEN** the route runs 24, 45, 12, 31 and the rider starts navigation standing at knot 24
- **THEN** navigation begins with 45 as the next knot

#### Scenario: Start at a knot in the middle of the route
- **WHEN** the rider starts navigation standing at knot 12 of the route 24, 45, 12, 31
- **THEN** navigation begins with 31 as the next knot

#### Scenario: Start between two knots
- **WHEN** the rider starts navigation on the course of the route between 45 and 12
- **THEN** navigation begins with 12 as the next knot

#### Scenario: Start at a double knot
- **WHEN** the route runs 24, 45, 46, 12, 46 is 10 m after 45, and the rider starts navigation standing between 45 and 46
- **THEN** navigation begins with 12 as the next knot

#### Scenario: Start at another node of a merged knot
- **WHEN** knot 24 of the route 24, 45, 12, 31 is tagged on two nodes 195 m apart, and the rider starts navigation standing at the node that is not the knot's position on the map
- **THEN** navigation begins with 45 as the next knot

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

### Requirement: Passing a knot
A knot SHALL count as passed when the rider comes within 30 m of it, of any of the nodes it is tagged on. Passing the next knot SHALL make the knot after it the next knot and SHALL be announced with the next two knots, e.g. "Next 12, then 31". When the rider comes within 30 m of several consecutive knots of the route at once, all of them SHALL count as passed, and one announcement SHALL name the knot after the last of them.

#### Scenario: Passing the next knot
- **WHEN** the rider heading to 45 on the route 24, 45, 12, 31 comes within 30 m of 45
- **THEN** 12 becomes the next knot and the app says "Next 12, then 31"

#### Scenario: Riding close to a knot without passing it
- **WHEN** the rider passes 80 m away from the next knot on a parallel road
- **THEN** the knot does not count as passed

#### Scenario: Round trip
- **WHEN** the route is a round trip 24, 45, 12, 24 and the rider starts navigation at 24
- **THEN** 45 is the next knot, and reaching 24 again only counts as arriving after 45 and 12 were passed

#### Scenario: Passing a double knot
- **WHEN** the rider heading to 45 on the route 24, 45, 46, 12, with 46 10 m after 45, comes within 30 m of both 45 and 46 at once
- **THEN** both count as passed, 12 becomes the next knot, and the app says "Next 12" once, without a notice for 46

#### Scenario: Riding on after a double knot
- **WHEN** the rider passed the double knot 45, 46 and rides on towards 12
- **THEN** the distance to 12 counts down and the rider's place on the progress strip moves on

#### Scenario: Passing a merged knot at its other node
- **WHEN** knot 45 of the route 24, 45, 12, 31 is tagged on two nodes 195 m apart, and the rider heading to 45 comes within 30 m of the node that is not the knot's position on the map
- **THEN** 12 becomes the next knot and the app says "Next 12, then 31"

#### Scenario: Passing a merged knot that is not on the route
- **WHEN** knot 77, not on the route, is tagged on two nodes, and the rider heading to 45 comes within 30 m of the node that is not 77's position on the map
- **THEN** the app says "Off route, next 45" with the distance and direction to 45
