# Spec Delta

## Purpose

Lets the user see a knooppunt network on a map and plan a route on it by choosing knots, using the route planning of `route-planning`.

## ADDED Requirements

### Requirement: Show the network on the map
The map SHALL show the knots of the selected network type of an installed region with their numbers, and the connections between them along their actual course, over a base map. The user SHALL be able to switch between the cycling and the hiking network.

#### Scenario: Cycling network of the Netherlands
- **WHEN** the Netherlands are installed and the user opens the map with the cycling network selected
- **THEN** the map shows the Dutch cycling knots with their numbers and their connections

#### Scenario: Switching to hiking
- **WHEN** the user switches from cycling to hiking
- **THEN** the map shows the hiking network instead, and the current route is discarded after the user confirms

#### Scenario: Without matching geometry
- **WHEN** an installed network has no geometry that belongs to its graph, e.g. because its download failed
- **THEN** connections and the route are drawn as straight lines between knots, and planning works unchanged

#### Scenario: No network access
- **WHEN** the device has no network access
- **THEN** the knots and connections are still shown and planning works, only the base map may be missing

#### Scenario: Attribution
- **WHEN** the map is shown
- **THEN** the attribution required by OpenStreetMap and the base map provider is visible

### Requirement: Plan by tapping knots
Tapping a knot SHALL make it the start if there is no start yet, the end if there is no end yet, and otherwise add it as a stop into the leg where it adds the least distance. A tap SHALL select the knot nearest to the tapped position, within a tolerance that keeps knots easy to hit.

#### Scenario: Start and end
- **WHEN** the user taps knot 12 and then knot 45
- **THEN** 12 is the start, 45 the end, and the route between them is shown on the map

#### Scenario: Adding a stop
- **WHEN** a route from 12 to 45 exists and the user taps knot 30
- **THEN** 30 is added as a stop where it adds the least distance, and the route passes through it

#### Scenario: Tapping next to the network
- **WHEN** the user taps where no knot is within the tolerance
- **THEN** the route does not change

#### Scenario: Knots with equal numbers
- **WHEN** two knots 30 are close to each other, e.g. on both banks of a river, and the user taps one of them
- **THEN** exactly the tapped knot becomes part of the route

### Requirement: Show the route
The app SHALL show the planned route on the map along its actual course, together with its knot sequence and its total distance. Unreachable legs SHALL be pointed out.

#### Scenario: Route summary
- **WHEN** the route goes from 12 via 7 to 45 over 3.5 km
- **THEN** the app shows `12 → 7 → 45` and 3.5 km

#### Scenario: Unreachable leg
- **WHEN** a waypoint cannot be reached from the previous one
- **THEN** the app marks that leg as unreachable instead of showing a route summary

### Requirement: Edit the waypoint list
The app SHALL list the waypoints in order. The user SHALL be able to move any waypoint to another position and to remove waypoints as long as two remain.

#### Scenario: Reorder
- **WHEN** the waypoints are 12, 30, 7, 45 and the user moves 7 above 30
- **THEN** the waypoints are 12, 7, 30, 45 and the route on the map follows them

#### Scenario: Remove
- **WHEN** the waypoints are 12, 30, 45 and the user removes 30
- **THEN** the waypoints are 12, 45

#### Scenario: Start over
- **WHEN** the user clears the route
- **THEN** all waypoints are removed and the next tap sets a new start

### Requirement: Keep the current route
The route being planned SHALL be restored after the app is closed and opened again.

#### Scenario: Restart
- **WHEN** the user plans a route from 12 via 30 to 45 and restarts the app
- **THEN** the same route with the same knots is shown again

#### Scenario: Knot gone after an update
- **WHEN** a waypoint's knot no longer exists in updated region data
- **THEN** the route is restored without that waypoint and the user is told which one was removed
