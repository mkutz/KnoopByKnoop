## ADDED Requirements

### Requirement: Route points from a place
A route point chosen from a tapped highlighted place SHALL keep the place's kind and name. On the map, in the waypoint list and on the strip it SHALL show the place's icon instead of the flag, with the corner symbol of its role as on knots. The waypoint list SHALL show the role followed by the place's name, or by the kind's name when the place has none, and screen readers SHALL read the same. The kind and the name SHALL be kept with the route across restarts. Route points from a spot without a place SHALL keep the flag.

#### Scenario: Café as a stop
- **WHEN** the user taps "Eiscafé Riviera" and chooses "Add stop here"
- **THEN** the stop shows the food icon with the stop's corner symbol on the map and on the strip, and the waypoint list shows "Stop · Eiscafé Riviera"

#### Scenario: Place without a name as the end
- **WHEN** the user taps a drinking water point without a name and chooses "End here"
- **THEN** the end shows the water icon with the end's corner symbol, and the list shows "End · Drinking water"

#### Scenario: Start that becomes a stop
- **WHEN** the start is a café and the user chooses "Start here" at another spot
- **THEN** the café becomes a stop and keeps its icon and name

#### Scenario: Kept after a restart
- **WHEN** the route has a supermarket as a stop and the app is started again
- **THEN** the stop still shows the supermarket icon and its name

#### Scenario: Route saved before
- **WHEN** a route with a route point was saved before this change and is loaded
- **THEN** its route point shows the flag, as before

#### Scenario: Spot without a place
- **WHEN** the user taps an empty spot and chooses "End here"
- **THEN** the end shows the flag, as before

#### Scenario: Places switched off
- **WHEN** a café is a stop and the user turns the places off in the info sheet
- **THEN** the stop keeps the food icon

#### Scenario: Screen reader in the list
- **WHEN** a screen reader reaches the café stop in the waypoint list
- **THEN** it reads "Stop, Eiscafé Riviera" and offers to move and remove it by that name
