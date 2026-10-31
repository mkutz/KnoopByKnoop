# Spec Delta

## ADDED Requirements

### Requirement: Knot badges at large text sizes
The knot badges in the panel SHALL show their numbers at the same size whatever the system text size, like the badges on the map, so a number always fits within its badge.

#### Scenario: Badge at the largest text size
- **WHEN** the system text size is at its largest and the panel shows the badge of knot 94
- **THEN** the badge shows "94" completely within its circle, at the same size as at the default text size

### Requirement: Route summary at large text sizes
At large system text sizes, the planner panel SHALL show the route's distance and time each in one line and completely. Where they do not fit next to the panel's actions, the actions SHALL be shown below them.

#### Scenario: Route summary at the largest text size
- **WHEN** the system text size is at its largest and the route is 4.1 km, about 15 min
- **THEN** the panel shows "4.1 km" and "about 15 min" each in one line and completely, with the actions below them

#### Scenario: Route summary at the default text size
- **WHEN** the system text size is the default
- **THEN** the distance and time are next to the actions, as before
