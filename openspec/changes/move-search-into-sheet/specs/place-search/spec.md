## MODIFIED Requirements

### Requirement: Search while typing
The app SHALL offer a search field. While the user types, the app SHALL list matching places, stations, addresses and businesses, each with its name and enough of its address or type to tell similar hits apart. On Android, the field SHALL be shown over the map, with the hits right below it. On iOS, the field SHALL be shown at the top of the waypoint panel, and while the user searches, the panel SHALL show the hits in place of the waypoints; in portrait, the sheet SHALL expand so the hits are not hidden by the keyboard.

#### Scenario: Station
- **WHEN** the user types "Utrecht Centraal"
- **THEN** the list shows the station Utrecht Centraal in Utrecht as one hit

#### Scenario: Address
- **WHEN** the user types a street and house number
- **THEN** the list shows the matching address with its town

#### Scenario: Business
- **WHEN** the user types the name of a café or shop
- **THEN** the list shows matching businesses with their street and town

#### Scenario: No hits
- **WHEN** nothing matches what the user typed
- **THEN** the list says that nothing was found

#### Scenario: Searching from the sheet on iOS
- **WHEN** the iPhone is in portrait, the sheet is at its smallest height and the user taps the search field and types "Utrecht Centraal"
- **THEN** the sheet expands and shows the hits in place of the waypoints, above the keyboard

#### Scenario: Back to the waypoints on iOS
- **WHEN** the iPhone is in portrait, the sheet shows hits and the user taps a hit or clears the search
- **THEN** the sheet shows the waypoints again and goes back to its smallest height, so the map can be seen

#### Scenario: Searching in landscape on iOS
- **WHEN** the iPhone is in landscape and the user types in the search field at the top of the side panel
- **THEN** the side panel shows the hits in place of the waypoints, and the map stays visible next to it
