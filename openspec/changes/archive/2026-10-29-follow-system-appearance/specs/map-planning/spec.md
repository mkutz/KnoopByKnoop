# Spec Delta

## ADDED Requirements

### Requirement: Light and dark appearance
The app SHALL follow the system's light or dark appearance: the base map, the planner panel and the controls over the map SHALL all be light or all be dark. Switching the appearance while the app is open SHALL switch the app without moving the map or changing the route, and without interrupting navigation. The knots, the route and the highlighted knots SHALL stay distinguishable on the dark map.

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

## MODIFIED Requirements

### Requirement: Clear actions in the planner panel
The planner panel SHALL show Navigate, and Stop while navigating, as the most prominent actions, each with an icon and its label. Back to start and Clear SHALL be shown as icon buttons that are announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** the header shows the distance, Back to start and Clear as icon buttons, and Navigate as a filled button with an icon and the label "Navigate", in one row

#### Scenario: Stop stands out
- **WHEN** the rider is navigating
- **THEN** the header shows Stop as a filled red button with an icon and the label "Stop"

#### Scenario: Icon buttons with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) moves through the header of a planned round trip
- **THEN** it announces "Back to start", "Clear" and "Navigate" as buttons

#### Scenario: Name of an icon button on Android
- **WHEN** the user long-presses Clear on Android
- **THEN** the name "Clear" is shown

#### Scenario: Readable actions
- **WHEN** the actions are measured against the panel's background
- **THEN** the labels of Navigate and Stop have a contrast of at least 4.5:1 to their fill, and the icons of all four at least 3:1 to their background

#### Scenario: Readable actions in dark mode
- **WHEN** the actions are measured against the dark panel's background
- **THEN** the labels of Navigate and Stop have a contrast of at least 4.5:1 to their fill, and the icons of all four at least 3:1 to their background
