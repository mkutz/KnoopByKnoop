## ADDED Requirements

### Requirement: Panel before a route
With fewer than two waypoints, the panel SHALL show "Plan a ride", a start and an end slot with their labels, one hint line, and buttons "Search a place" and "Nearest knots". An empty slot SHALL look empty; the slot the next tap fills SHALL stand out. A set waypoint SHALL fill its slot with its badge and its label SHALL name its place. With a waypoint set, Clear SHALL replace "Nearest knots".

#### Scenario: Nothing planned
- **WHEN** no waypoint is set and knots are in view
- **THEN** the panel shows "Plan a ride", two empty slots labelled "Start" and "End", the start slot standing out, "Tap a numbered knot on the map for the start.", and the buttons "Search a place" and "Nearest knots"

#### Scenario: Start set
- **WHEN** the user taps 48 in De Rips with nothing planned
- **THEN** the start slot shows the badge 48, labelled "Start · De Rips", the end slot stands out, the hint says "Now tap a knot for the end.", and the buttons are "Search a place" and Clear

#### Scenario: End chosen first
- **WHEN** an end was chosen before any start
- **THEN** the end slot shows its badge, the start slot stands out, and the hint says "Now tap a knot for the start."

#### Scenario: Route point in a slot
- **WHEN** the start is a route point from a tapped spot
- **THEN** the start slot shows the flag badge of route points

#### Scenario: Next slot without motion
- **WHEN** the phone is set to reduce motion or to remove animations
- **THEN** the slot that stands out does so without pulsing

#### Scenario: Search from the panel
- **WHEN** the user chooses "Search a place" in the panel
- **THEN** the place search opens as from the map's search button

#### Scenario: Nearest knots from the panel
- **WHEN** the user chooses "Nearest knots" in the panel
- **THEN** the map shows the nearest knots as the map's nearest knots button does

#### Scenario: Into the route
- **WHEN** the start is set and the user taps a knot for the end
- **THEN** the slots give way to the route's distance, time, knots and actions

#### Scenario: Slots with a screen reader
- **WHEN** a screen reader reaches the slots with only the start 48 set
- **THEN** it reads "Start, 48, De Rips" and "End, not set yet"

#### Scenario: Empty panel at large text sizes
- **WHEN** the largest text size is set and nothing is planned
- **THEN** the title, labels, hint and buttons are fully readable, and the buttons wrap to a second line rather than being cut

## MODIFIED Requirements

### Requirement: Nearest knots
While planning, when the map shows no knots of the selected network, because there are none in its area or it is zoomed out too far for knots to show, the map SHALL offer a button below the position button, in the same group, that shows the nearest knots. With no waypoints set, the panel's hint SHALL say that no knots are in view and its "Nearest knots" button SHALL be the prominent action, see "Panel before a route".

#### Scenario: No knots in the area
- **WHEN** the map shows an area without knots and no waypoints are set
- **THEN** the map shows the button, and the panel's hint says "No knots in view" with "Nearest knots" as the prominent button

#### Scenario: Zoomed out too far
- **WHEN** the map is zoomed out so far that knots are not shown
- **THEN** the map shows the button

#### Scenario: Knots in view
- **WHEN** the map shows at least one knot
- **THEN** it does not show the button, and with no waypoints set, the panel's hint says "Tap a numbered knot on the map for the start."

#### Scenario: With waypoints set
- **WHEN** a start is set and the user moves the map to an area without knots
- **THEN** the map shows the button, and the panel still shows the route's state

#### Scenario: From the rider's position
- **WHEN** location access is allowed, the rider is 8 km from the nearest knot, and they tap the button
- **THEN** the map shows both their position, marked with a dot, and that knot, with the knots shown

#### Scenario: Clear of the controls
- **WHEN** the map moves to show the rider's position and the nearest knot
- **THEN** neither is hidden behind the search field, the network switch or the panel, and the button is gone, as a knot is in view

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
