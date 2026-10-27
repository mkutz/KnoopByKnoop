## ADDED Requirements

### Requirement: Long routes in the panel
The planner panel (the sheet in portrait, the side panel in landscape) SHALL stay usable for routes of any length. The route's distance and the actions to clear the route and to return to the start SHALL stay visible at the top of the panel. The knot sequence, the ferries of the route and the waypoint list SHALL scroll together below them.

#### Scenario: Long route
- **WHEN** the route passes so many knots that their badges do not fit into the panel
- **THEN** the distance, Clear and Back to start are still shown at the top, and the user can scroll down through the badges to the waypoint list

#### Scenario: Editing waypoints of a long route
- **WHEN** the route is long and the user scrolls to the waypoint list
- **THEN** the user can move and remove waypoints there as on a short route

#### Scenario: Short route
- **WHEN** the badges and the waypoint list fit into the panel
- **THEN** the panel looks as before, without scrolling
