# Spec Delta

## ADDED Requirements

### Requirement: Clear of the system bars
The map SHALL extend behind the system's status bar and navigation bar, while the status bar's and navigation bar's icons SHALL stay readable on it in light and dark appearance. The controls over the map and the planner panel SHALL stay clear of the system bars and the display cutout, so all of their area can be tapped.

#### Scenario: Status bar on the light map
- **WHEN** the app shows the light map
- **THEN** the clock, the status icons and the notification icons are visible in dark colors above the map

#### Scenario: Status bar in dark mode
- **WHEN** the system is in dark mode, also after switching to it while the app is open
- **THEN** the status bar's and navigation bar's icons are light

#### Scenario: Tapping the top of the search field
- **WHEN** the user taps the search field near its upper edge
- **THEN** the search field gets the focus and the keyboard opens

#### Scenario: Landscape with a display cutout
- **WHEN** the phone is in landscape with the camera cutout on the left or the right
- **THEN** the search field, the network switch, the zoom buttons, the attribution and the side panel are not covered by the cutout or the navigation bar
