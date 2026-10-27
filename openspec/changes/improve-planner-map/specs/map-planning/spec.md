# Spec Delta

## MODIFIED Requirements

### Requirement: Plan by tapping knots
Tapping a knot SHALL make it the start if there is no start yet, the end if there is no end yet, and otherwise add it as a stop into the leg where it adds the least distance. Tapping the knot that is currently the last waypoint SHALL change nothing, so an accidental double tap does not create a round trip. A tap SHALL select the knot nearest to the tapped position, within a tolerance that keeps knots easy to hit.

#### Scenario: Start and end
- **WHEN** the user taps knot 12 and then knot 45
- **THEN** 12 is the start, 45 the end, and the route between them is shown on the map

#### Scenario: Adding a stop
- **WHEN** a route from 12 to 45 exists and the user taps knot 30
- **THEN** 30 is added as a stop where it adds the least distance, and the route passes through it

#### Scenario: Tapping the last waypoint again
- **WHEN** only the start 12 is set and the user taps 12 again
- **THEN** 12 stays the only waypoint, and the next tapped knot becomes the end

#### Scenario: Tapping next to the network
- **WHEN** the user taps where no knot is within the tolerance
- **THEN** the route does not change

#### Scenario: Knots with equal numbers
- **WHEN** two knots 30 are close to each other, e.g. on both banks of a river, and the user taps one of them
- **THEN** exactly the tapped knot becomes part of the route

### Requirement: Show the route
The app SHALL show the planned route on the map along its actual course, together with its knot sequence and its total distance. Unreachable legs SHALL be pointed out. The knot sequence SHALL show each knot as a badge with its number, like on the map, and SHALL make the chosen waypoints clearly stand out from the knots the route only passes.

#### Scenario: Route summary
- **WHEN** the route goes from 12 via 7 to 45 over 3.5 km, with 12 and 45 as waypoints
- **THEN** the app shows 3.5 km and the badges 12, 7 and 45 in this order, with 12 and 45 highlighted as start and end and 7 shown as a passed knot

#### Scenario: Unreachable leg
- **WHEN** a waypoint cannot be reached from the previous one
- **THEN** the app marks that leg as unreachable instead of showing a route summary

## ADDED Requirements

### Requirement: Zoom controls
The map SHALL offer buttons to zoom in and out, in addition to the gestures of the platform.

#### Scenario: Zooming out
- **WHEN** the user taps the zoom-out button
- **THEN** the map zooms out by one level around its center

### Requirement: Route knots stand out
On the map, knots the route passes SHALL be highlighted, so the route can be followed in dense areas. Start, end and stops SHALL be distinguishable from each other and from passed knots.

#### Scenario: Passed knots
- **WHEN** the route from 12 to 45 passes knot 7
- **THEN** knot 7 is highlighted as part of the route, and knots the route does not pass are not

#### Scenario: Start, stop and end
- **WHEN** the route has the waypoints 12, 30 and 45
- **THEN** 12 is marked as start, 30 as stop and 45 as end, each in its own way

### Requirement: Return to the start
The user SHALL be able to turn a route into a round trip with one action, which adds the start as the new end.

#### Scenario: Back to start
- **WHEN** the waypoints are 12, 30 and 45 and the user chooses to return to the start
- **THEN** the waypoints are 12, 30, 45 and 12

### Requirement: Route when zoomed out
Where knots are too small to show, the route SHALL be shown as its line only, without waypoint markers. Tapping the route there SHALL zoom the map to the whole route.

#### Scenario: Zoomed out
- **WHEN** a route is planned and the map is zoomed out so far that knots are not shown
- **THEN** the route is a line without markers

#### Scenario: Tapping the route when zoomed out
- **WHEN** the map is zoomed out and the user taps the route line
- **THEN** the map zooms to show the whole route

### Requirement: Riding direction
The route line SHALL show the riding direction.

#### Scenario: Direction arrows
- **WHEN** a route is shown on the map
- **THEN** arrows along its line point from the start towards the end

### Requirement: Waypoint list with badges
The waypoint list SHALL show each waypoint as a badge with its number and its role.

#### Scenario: Listing waypoints
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** the list shows the badges 12 (start), 30 (stop) and 45 (end), styled like their markers on the map
