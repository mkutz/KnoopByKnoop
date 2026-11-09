## MODIFIED Requirements

### Requirement: Landscape layout
In landscape, the app SHALL show the map and the waypoint list side by side, so both can be seen and used at the same time. While searching, the search field and its hits SHALL be at the top of the side panel, on both apps, see `place-search`, and the map SHALL keep its width. In portrait, the waypoint list SHALL stay a sheet over the map.

#### Scenario: Planning in landscape
- **WHEN** the device is in landscape and the user taps knots 12 and 45 on the map
- **THEN** the map and the waypoint list are visible next to each other, and the list shows 12 as start and 45 as end right away

#### Scenario: Rotating with a route
- **WHEN** a route from 12 via 30 to 45 is planned and the user rotates the device between portrait and landscape
- **THEN** the route, the map's position and the selected network stay as they were

#### Scenario: Map controls in landscape
- **WHEN** the device is in landscape
- **THEN** the network switch, the zoom buttons, the magnifier button, the info button and the attribution stay visible on the map, also while the user types in the search

#### Scenario: Typing in the search in landscape
- **WHEN** an Android device is in landscape with a route planned, and the user taps the magnifier button and types "Amsterdam" with the on-screen keyboard
- **THEN** the field and the hits show at the top of the side panel, the map keeps its width with the route on it, and picking a hit or Cancel brings the waypoints back with the same route
