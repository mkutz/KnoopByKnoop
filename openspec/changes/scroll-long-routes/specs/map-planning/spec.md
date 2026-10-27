## ADDED Requirements

### Requirement: Long routes in the panel
The planner panel (the sheet in portrait, the side panel in landscape) SHALL stay usable for routes of any length. The route's distance, the actions to clear the route and to return to the start, and the knot sequence SHALL stay at the top of the panel; the knot sequence SHALL be one line that scrolls sideways where it does not fit. The ferries of the route and the waypoint list SHALL scroll together below them.

#### Scenario: Long route
- **WHEN** the route passes so many knots that their badges do not fit into the panel
- **THEN** the distance, Clear, Back to start and the first badges are shown at the top, the user can swipe the badges sideways to see the rest of the route, and can scroll down through the ferries to the waypoint list

#### Scenario: Editing waypoints of a long route
- **WHEN** the route is long and the user scrolls to the waypoint list
- **THEN** the user can move and remove waypoints there as on a short route

#### Scenario: Short route
- **WHEN** the badges fit into one line and the ferries and waypoints fit into the panel
- **THEN** nothing scrolls
