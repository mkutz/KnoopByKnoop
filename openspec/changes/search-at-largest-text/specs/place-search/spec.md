## ADDED Requirements

### Requirement: Search at the largest text size
At any system text size, the first tap on the magnifier button SHALL open the search, the search field SHALL show completely below the sheet's grabber, and closing the search SHALL bring back the waypoint sheet with the route's distance and time complete, see `map-planning` "Route summary at large text sizes".

#### Scenario: Opening at the largest text size on iOS
- **WHEN** the iPhone's text size is at its largest, the app was just started with a route of 15 km planned, and the user taps the magnifier button once
- **THEN** the search opens with the keyboard, and the field, its text and Cancel show completely below the sheet's grabber

#### Scenario: Closing at the largest text size on iOS
- **WHEN** the iPhone's text size is at its largest, the search is open, and the user taps Cancel
- **THEN** the sheet shows the waypoints again with "15 km" and "about 58 min" complete at its top

#### Scenario: Opening at the largest text size on Android
- **WHEN** an Android phone's font size is at its largest and the user taps the magnifier button once
- **THEN** the search opens with the keyboard, and the field and Cancel show completely
