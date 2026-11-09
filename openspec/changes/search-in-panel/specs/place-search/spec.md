## MODIFIED Requirements

### Requirement: Search while typing
The map SHALL offer a search field. While the user types, the app SHALL list matching places, stations, addresses and businesses, each with its name and enough of its address or type to tell similar hits apart. On both apps, the search SHALL be a round magnifier button in the map's column of buttons while it isn't used, so nothing covers the map or its buttons; tapping it SHALL open the search field at the top of the waypoint sheet in portrait, or of the side panel in landscape, with the keyboard, and the hits SHALL be listed in the panel. Picking a hit, Cancel, tapping the map or, on Android, the back gesture SHALL close the search back into the button; clearing the field SHALL keep it open to type again. While navigating, there SHALL be no search.

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
- **THEN** no search field is over the map; a round magnifier button, styled like the zoom buttons, is in the map's column of buttons, and none of the map's buttons is covered

#### Scenario: Sheet pulled up on iOS
- **WHEN** the iPhone is in portrait and the user pulls the waypoint sheet up all the way
- **THEN** the sheet shows the waypoints, and the search opens only from the magnifier button

#### Scenario: Typing on iOS
- **WHEN** the iPhone is in portrait and the user taps the magnifier button and types "Utrecht Centraal"
- **THEN** the waypoint sheet rises, its top shows the search field with the keyboard open, and the hits are listed in the sheet below the field

#### Scenario: One surface on iOS
- **WHEN** the search is open in the sheet
- **THEN** the field and the hits are on the sheet's own background, with no separate glass of their own

#### Scenario: Searching with a hardware keyboard on iOS
- **WHEN** the iPhone has a hardware keyboard connected, so no on-screen keyboard comes up, and the user taps the magnifier button and types "Utrecht Centraal"
- **THEN** the field and the hits show in the sheet as with the on-screen keyboard

#### Scenario: Back above the sheet on iOS
- **WHEN** the iPhone shows hits and the user taps a hit, taps Cancel or taps the map
- **THEN** the keyboard is dismissed, the sheet shows the waypoints again at the height it had, and the search is the magnifier button again

#### Scenario: Searching in landscape on iOS
- **WHEN** the iPhone is in landscape
- **THEN** the magnifier button is in the map's column of buttons, and no search field is over the map

#### Scenario: Typing in landscape on iOS
- **WHEN** the iPhone is in landscape and the user taps the magnifier button and types "Utrecht" with the on-screen keyboard
- **THEN** the field and the hits show at the top of the side panel, and the map, the network switch and the attribution stay as they are

#### Scenario: Done typing in landscape on iOS
- **WHEN** the iPhone is in landscape, shows hits, and the user taps a hit or Cancel
- **THEN** the keyboard is dismissed, the side panel shows the waypoints again, and the search is the magnifier button again

#### Scenario: Button on the map on Android
- **WHEN** an Android phone is in portrait and the user drags the waypoint sheet between its smallest and its medium height
- **THEN** no search field is over the map; a round magnifier button, styled like the zoom buttons, is in the map's column of buttons, and none of the map's buttons is covered

#### Scenario: Typing on Android
- **WHEN** an Android phone is in portrait and the user taps the magnifier button and types "Utrecht Centraal"
- **THEN** the waypoint sheet rises, its top shows the search field with the keyboard open, and the hits are listed in the sheet below the field

#### Scenario: Back on Android
- **WHEN** an Android phone shows hits in the sheet and the user taps a hit, taps Cancel, taps the map or uses the back gesture
- **THEN** the keyboard is dismissed, the sheet shows the waypoints again at the height it had, and the search is the magnifier button again

#### Scenario: Typing in landscape on Android
- **WHEN** an Android phone is in landscape and the user taps the magnifier button and types "Utrecht" with the on-screen keyboard
- **THEN** the field and the hits show at the top of the side panel, and the map, the network switch and the attribution stay as they are

#### Scenario: No search while navigating
- **WHEN** the user navigates a route, on either app
- **THEN** there is neither a magnifier button nor a search field on the map

### Requirement: Jump to a hit
Tapping a hit SHALL move the map to it, zoomed in far enough that knots are shown and can be tapped, and SHALL mark it with a marker. A hit far from the map's view SHALL be jumped to rather than moved to, so the map draws only where it ends up. On iOS, the map SHALL show at once that it is getting there, until it has drawn the hit. The route being planned SHALL stay as it is.

#### Scenario: Tapping a hit
- **WHEN** a route is planned and the user taps the hit "Utrecht Centraal"
- **THEN** the map shows Utrecht Centraal with a marker, the knots around it can be tapped, and the route is unchanged

#### Scenario: Removing the marker
- **WHEN** a marker is shown and the user clears the search
- **THEN** the marker is removed

#### Scenario: Showing a far place on iOS
- **WHEN** the map shows Siegburg and the user taps the hit "Amsterdam Centraal"
- **THEN** the search closes at once, the map shows "Showing Amsterdam Centraal…" with a spinner, jumps to Amsterdam without moving across the way there, and the note goes when the map has drawn it
