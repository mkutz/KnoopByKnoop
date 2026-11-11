## MODIFIED Requirements

### Requirement: Waypoint list with badges
The waypoint list SHALL show each waypoint in one line as a badge with its number and its role, followed by the place the knot lies in where it has one (see `network-data` "Place of each knot"), each line no higher than a touch target (48 dp on Android, 44 pt on iOS). The place SHALL be named as on its signpost, followed by its name in the app's language in brackets where that differs; a name too long for the line SHALL be cut short. While navigating, the waypoint list SHALL NOT be shown, as the route is fixed then and the knot sequence shows it.

#### Scenario: Listing waypoints
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** the list shows the badges 12 (start), 30 (stop) and 45 (end), styled like their markers on the map

#### Scenario: Compact waypoint list
- **WHEN** a route without ferries has four waypoints and the user expands the panel on a phone in portrait
- **THEN** the list shows all four waypoints without scrolling, each in one line

#### Scenario: Waypoint list while navigating
- **WHEN** the rider navigates a route with waypoints 12, 30 and 45
- **THEN** the panel shows the knot sequence and any ferries, but no waypoint list

#### Scenario: Place in the waypoint list
- **WHEN** the start 06 lies in Lohmar and the end 28 in Sankt Augustin
- **THEN** the list shows "Start · Lohmar" beside 06 and "End · Sankt Augustin" beside 28

#### Scenario: Waypoint in the open
- **WHEN** the stop 02 carries no place
- **THEN** its line shows its role only

#### Scenario: Place name in the app's language
- **WHEN** the app's language is German and the end lies in Liège
- **THEN** its line shows "End · Liège (Lüttich)", in German "Ende · Liège (Lüttich)"
