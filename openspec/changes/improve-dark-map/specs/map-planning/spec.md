# Spec Delta

## MODIFIED Requirements

### Requirement: Light and dark appearance
The app SHALL follow the appearance chosen in the info sheet, by default the system's light or dark appearance: the base map, the planner panel and the controls over the map SHALL all be light or all be dark. The dark base map SHALL show the same details as the light one, with water in blue and paths and roads visible against the land. Switching the appearance while the app is open SHALL switch the app without moving the map or changing the route, and without interrupting navigation. The knots, the route and the highlighted knots SHALL stay distinguishable on the dark map.

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
