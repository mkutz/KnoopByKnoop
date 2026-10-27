## MODIFIED Requirements

### Requirement: Search while typing
The map SHALL offer a search field. While the user types, the app SHALL list matching places, stations, addresses and businesses, each with its name and enough of its address or type to tell similar hits apart. On Android, the field SHALL be shown at the top of the map, with the hits right below it. On iOS, the field SHALL be shown at the bottom of the map, right above the waypoint sheet in portrait, with the hits above the field; while the user types, the field and the hits SHALL stay above the keyboard.

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

#### Scenario: Field above the sheet on iOS
- **WHEN** the iPhone is in portrait and the user drags the waypoint sheet between its smallest and its medium height
- **THEN** the search field stays right above the sheet's top edge, over the map

#### Scenario: Sheet pulled up on iOS
- **WHEN** the iPhone is in portrait and the user pulls the waypoint sheet up all the way
- **THEN** the search field is hidden, and it is shown again above the sheet when the sheet is lowered

#### Scenario: Typing on iOS
- **WHEN** the iPhone is in portrait and the user taps the search field and types "Utrecht Centraal"
- **THEN** the field moves above the keyboard and the hits are shown above the field, over the map

#### Scenario: Back above the sheet on iOS
- **WHEN** the iPhone shows hits and the user taps a hit or clears the search
- **THEN** the keyboard is dismissed and the field is shown right above the waypoint sheet again

#### Scenario: Searching in landscape on iOS
- **WHEN** the iPhone is in landscape
- **THEN** the search field is shown at the bottom of the map next to the side panel, and while the user types, the hits are shown above it
