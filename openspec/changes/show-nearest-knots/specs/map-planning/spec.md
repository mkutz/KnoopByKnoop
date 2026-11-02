# Spec Delta

## ADDED Requirements

### Requirement: Nearest knots
While planning, when the map shows no knots of the selected network, because there are none in its area or it is zoomed out too far for knots to show, the map SHALL offer a round button below the position button that shows the nearest knots. With no waypoints set, the panel SHALL say that no knots are in view and offer the same action, instead of asking to tap a knot.

#### Scenario: No knots in the area
- **WHEN** the map shows an area without knots and no waypoints are set
- **THEN** the map shows the button, and the panel says "No knots in view" with a "Show the nearest knots" button

#### Scenario: Zoomed out too far
- **WHEN** the map is zoomed out so far that knots are not shown
- **THEN** the map shows the button

#### Scenario: Knots in view
- **WHEN** the map shows at least one knot
- **THEN** it does not show the button, and with no waypoints set, the panel says "Tap a knot to set the start"

#### Scenario: With waypoints set
- **WHEN** a start is set and the user moves the map to an area without knots
- **THEN** the map shows the button, and the panel still shows the route's state

#### Scenario: From the rider's position
- **WHEN** location access is allowed, the rider is 8 km from the nearest knot, and they tap the button
- **THEN** the map shows both their position, marked with a dot, and that knot, with the knots shown

#### Scenario: Without location access
- **WHEN** location access was not allowed and the user taps the button
- **THEN** the app does not ask for it, and the map shows the knot nearest to the middle of the map

#### Scenario: No position found
- **WHEN** location access is allowed but no position is found within 15 seconds
- **THEN** the map shows the knot nearest to the middle of the map

#### Scenario: Far from the network
- **WHEN** the rider is so far from the nearest knot that the knots would not show with both in view
- **THEN** the map shows that knot, with the knots shown

#### Scenario: Other network
- **WHEN** hiking is selected and the nearest knot is sought
- **THEN** the map shows the nearest hiking knot, not a cycling one

#### Scenario: Not while navigating
- **WHEN** the rider is navigating
- **THEN** the button is not shown

#### Scenario: Nearest knots with a screen reader
- **WHEN** a screen reader reaches the button
- **THEN** it announces "Show the nearest knots" as a button
