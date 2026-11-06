## REMOVED Requirements

### Requirement: Guided way while off route
**Reason**: It depended on a switch that is removed, see `network-sync`. Replaced by "Guided way back to the route", the same requirement without the switch and its scenario "Guidance turned off".
**Migration**: None. Code comments that cite this requirement cite "Guided way back to the route" instead.

## ADDED Requirements

### Requirement: Guided way back to the route
While navigating a cycling route and off route, the app SHALL find a way along suitable paths from the rider to the end of the line in "Way to the route on the map", as soon as the path data allows. It SHALL NOT change the planned route and SHALL NOT need network access at that moment.

#### Scenario: Starting away from the route
- **WHEN** the rider starts navigating at home, off route, with the squares installed and 24 as the first knot
- **THEN** the app finds a way to 24 along suitable paths and guides the rider along it

#### Scenario: Wrong turn
- **WHEN** the rider left the course of the leg to 45 and is off route with the squares installed
- **THEN** the app finds a way to the course ahead of where they left it

#### Scenario: Without courses
- **WHEN** the network's courses aren't installed and the rider is off route with the squares installed
- **THEN** the app finds a way to the next knot

#### Scenario: Path data arrives on the way
- **WHEN** the rider is off route without a way, and the squares between them and the target finish downloading
- **THEN** the app finds a way at the next location fix and guides the rider along it

#### Scenario: No way, no path data, far away or hiking
- **WHEN** no way can be found, the squares aren't installed, the target is more than 20 km away, or the route is a hiking route
- **THEN** off route works without a guided way, with the straight line, distance and compass direction

#### Scenario: Beyond the path data
- **WHEN** the rider is off route where no squares are installed, e.g. after starting 6 km from the route
- **THEN** off route works without a guided way, with the straight line, distance and compass direction

#### Scenario: Reaching the path data
- **WHEN** the same rider gets to where the squares are installed
- **THEN** the app finds a way and guides the rider along it, within 10 seconds

#### Scenario: Back on route with a guided way
- **WHEN** the rider following a guided way comes within 30 m of the course
- **THEN** the guided way ends, and the app says they are back on route, as in "Off route"

#### Scenario: Skipping ahead on the guided way
- **WHEN** the rider following a guided way to 24 passes 12, a later knot of the route
- **THEN** the guided way ends and navigation continues from 12, as when skipping ahead
