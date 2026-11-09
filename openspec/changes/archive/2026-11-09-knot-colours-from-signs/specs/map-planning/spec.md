## MODIFIED Requirements

### Requirement: Route knots stand out
On the map, knots the route passes SHALL be highlighted, so the route can be followed in dense areas. Start, end and stops SHALL be distinguishable from each other and from passed knots.

A knot's badge SHALL show its plate in the colour of the knot's sign region (see `network-data` "Sign region of each knot"): red for North Rhine-Westphalia, dark green for the Netherlands, blue for Belgium, and the app's green for a knot without a region. The badge SHALL show the knot's role by its fill:
- **Waypoint** (start, stop, end or round trip): a filled plate with a white number. Knots in North Rhine-Westphalia and Belgium SHALL also have a white ring inside the plate, as on their signs. Dutch knots SHALL have none.
- **Passed knot:** white, with one ring and the number in the plate colour.
- **Knot not on the route:** a grey ring and number, whatever its region.

A small dark symbol at the corner of a waypoint's badge SHALL tell the roles apart: ▶ for the start, a flag for a stop, ■ for the end, and a circular arrow for the start and end of a round trip. The waypoint list, the progress strip and the planner panel SHALL show badges as on the map. Red SHALL NOT be used on the map or in the planner panel to mark a problem, so it never reads like a German knot.

#### Scenario: Passed knots
- **WHEN** the route from 12 to 45 passes knot 7
- **THEN** knot 7 is highlighted as part of the route, and knots the route does not pass are not

#### Scenario: Start, stop and end
- **WHEN** the route has the waypoints 12, 30 and 45
- **THEN** 12 is marked as start, 30 as stop and 45 as end, each in its own way

#### Scenario: Round trip on the map
- **WHEN** the route is a round trip with the waypoints 12, 30 and 12
- **THEN** the map marks 12 once, in a way that shows it is both start and end

#### Scenario: Route in North Rhine-Westphalia
- **WHEN** a cycling route around Troisdorf starts at 25, passes 32 and ends at 29
- **THEN** 25 and 29 are red plates with a white ring and number, 25 with ▶ and 29 with ■ at the corner, and 32 is white with a red ring and number

#### Scenario: Route in the Netherlands
- **WHEN** a cycling route in Limburg starts at 95 and passes 1
- **THEN** 95 is a dark green plate with a white number and no ring, and 1 is white with a dark green ring and number

#### Scenario: Route in Belgium
- **WHEN** a cycling route in Haspengouw starts at 150
- **THEN** 150 is a blue plate with a white ring and number

#### Scenario: Route across the border
- **WHEN** a route starts at a Dutch knot and ends at a Belgian knot
- **THEN** the start is a dark green plate and the end a blue plate

#### Scenario: Knots not on the route
- **WHEN** a route in North Rhine-Westphalia is shown and knot 77 is not on it
- **THEN** 77 has a grey ring and number, not red

#### Scenario: Knot without a region
- **WHEN** a hiking route starts at knot 12
- **THEN** 12 is a plate in the app's green

#### Scenario: Data from before sign regions
- **WHEN** the installed network data has no sign regions
- **THEN** every route knot shows the app's green, with the roles shown by fill and corner symbol as above

#### Scenario: Off route on the map
- **WHEN** the rider is off route and the map shows the line to the route or a guided way
- **THEN** the line is dark, not red

### Requirement: Light and dark appearance
The app SHALL follow the appearance chosen in the info sheet, by default the system's light or dark appearance: the base map, the planner panel and the controls over the map SHALL all be light or all be dark. The dark base map SHALL show the same details as the light one, with water in blue and paths and roads visible against the land. Switching the appearance while the app is open SHALL switch the app without moving the map or changing the route, and without interrupting navigation. On Android, an appearance chosen in the info sheet SHALL reach the base map when the sheet closes, so the sheet's own switch stays smooth. The knots, the route and the highlighted knots SHALL stay distinguishable on the dark map.

#### Scenario: Dark mode
- **WHEN** the system is set to dark mode and the user opens the app
- **THEN** the base map, the planner panel, the search, the network switch and the zoom controls are dark

#### Scenario: Light mode
- **WHEN** the system is set to light mode
- **THEN** the app looks as before this change

#### Scenario: Switching while the app is open
- **WHEN** the user plans a route, zooms into it and switches the system to dark mode
- **THEN** the app turns dark, and the map shows the same area with the same route and waypoints

#### Scenario: Returning to the app
- **WHEN** the system is in dark mode and the user switches to another app and back, again and again
- **THEN** the map stays dark every time

#### Scenario: Switching while navigating
- **WHEN** the rider is navigating and the system switches to dark mode, e.g. automatically at sunset
- **THEN** navigation keeps running and announcing knots, and the app turns dark

#### Scenario: Route on the dark map
- **WHEN** a route with start, stops, end and ferries is shown on the dark map
- **THEN** start, stops, end, the passed knots, the ferries and the route line are as distinguishable as on the light map

#### Scenario: Offline in dark mode
- **WHEN** the base map cannot be loaded, e.g. offline on first start, and the system is in dark mode
- **THEN** the map shows the knot network on a dark background

#### Scenario: Lock screen in dark mode
- **WHEN** the rider navigates with the screen locked and the system is in dark mode
- **THEN** the navigation's notification or Live Activity on the lock screen is dark, with the number to follow readable

#### Scenario: Slow base map
- **WHEN** the app is online but the base map takes longer to load than the app waits for it, e.g. on the first start in dark mode
- **THEN** the map shows the knot network on the plain background first, and the base map replaces it as soon as it has loaded, without moving the map

#### Scenario: Details on the dark map
- **WHEN** the dark map shows an area with a river, a park, footpaths and cycleways
- **THEN** the river is blue, the park is green, and the footpaths and cycleways are drawn as on the light map, in colors visible on the dark land

#### Scenario: Choosing light
- **WHEN** the system is in dark mode and the user chooses Light in the info sheet
- **THEN** the base map, the planner panel, the controls and the status bar turn light, without moving the map or changing the route

#### Scenario: Choosing dark
- **WHEN** the system is in light mode and the user chooses Dark
- **THEN** the app turns dark

#### Scenario: Back to the system
- **WHEN** the user chooses System again
- **THEN** the app follows the system's appearance, also when it switches later

#### Scenario: Choice kept
- **WHEN** the user chose Light and starts the app again
- **THEN** the app is light, whatever the system's appearance

#### Scenario: Off route in dark mode
- **WHEN** the rider is off route in dark mode
- **THEN** the panel's header is light with dark text and arrow, and the rider's dot on the strip is a ring with ✕, readable on the dark panel, and none of them is red

#### Scenario: Off route in light mode
- **WHEN** the rider is off route in light mode
- **THEN** the panel's header is dark with white text and arrow, and the knot badges in it keep their plate colours

#### Scenario: Knots on the dark map
- **WHEN** a route in North Rhine-Westphalia is shown on the dark map
- **THEN** the waypoints are red plates, the passed knots white with a red ring, and the knots not on the route a grey ring and number on a dark disc

#### Scenario: Choosing the appearance in the info sheet
- **WHEN** the user opens the info sheet on Android and switches between Light and Dark several times
- **THEN** the sheet turns light or dark at once without stalling, and the base map follows the last choice when the sheet closes, showing the same area and route
