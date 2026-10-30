# Spec Delta

## MODIFIED Requirements

### Requirement: Show the route
The app SHALL show the planned route on the map along its actual course, together with its knot sequence, its total distance and the time it takes at an assumed pace of 16 km/h cycling and 4.5 km/h hiking, rounded as in `knot-navigation` "Rounded times". Unreachable legs SHALL be pointed out. The knot sequence SHALL show each knot as a badge with its number, like on the map, and SHALL make the chosen waypoints clearly stand out from the knots the route only passes.

#### Scenario: Route summary
- **WHEN** the route goes from 12 via 7 to 45 over 3.5 km, with 12 and 45 as waypoints
- **THEN** the app shows 3.5 km with "about 13 min" below it, and the badges 12, 7 and 45 in this order, with 12 and 45 highlighted as start and end and 7 shown as a passed knot

#### Scenario: Unreachable leg
- **WHEN** a waypoint cannot be reached from the previous one
- **THEN** the app marks that leg as unreachable instead of showing a route summary

#### Scenario: Hiking time
- **WHEN** a hiking route is 3.5 km long
- **THEN** the app shows 3.5 km with "about 47 min" below it

#### Scenario: Long route time
- **WHEN** a cycling route is 40.0 km long
- **THEN** the app shows 40.0 km with "about 2 h 30 min" below it
