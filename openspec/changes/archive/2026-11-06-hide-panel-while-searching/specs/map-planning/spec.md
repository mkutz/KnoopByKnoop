## MODIFIED Requirements

### Requirement: Landscape layout
In landscape, the app SHALL show the map and the waypoint list side by side, so both can be seen and used at the same time. On Android, while the on-screen keyboard is open for the search, the waypoint list SHALL slide aside, so the map, the search field and its hits get the whole width, and a chip with the route's start, end and distance SHALL show on the map; tapping it SHALL close the keyboard and bring the list back. In portrait, the waypoint list SHALL stay a sheet over the map.

#### Scenario: Planning in landscape
- **WHEN** the device is in landscape and the user taps knots 12 and 45 on the map
- **THEN** the map and the waypoint list are visible next to each other, and the list shows 12 as start and 45 as end right away

#### Scenario: Rotating with a route
- **WHEN** a route from 12 via 30 to 45 is planned and the user rotates the device between portrait and landscape
- **THEN** the route, the map's position and the selected network stay as they were

#### Scenario: Map controls in landscape
- **WHEN** the device is in landscape
- **THEN** the network switch, the zoom buttons, the info button and the attribution stay visible on the map, except on iOS while the user types in the search with the on-screen keyboard, see `place-search`

#### Scenario: Typing in the search in landscape
- **WHEN** an Android device is in landscape with a route planned, and the user types "Amsterdam" in the search with the on-screen keyboard
- **THEN** the waypoint list's panel slides aside, the map, the field and the hits use the whole width, and a chip shows 02 → 80 and the distance; tapping the chip, or closing the keyboard, brings the panel back with the same route
