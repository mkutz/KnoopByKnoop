# place-search Specification

## Purpose

Lets users find a place by name or address and jump there on the map, so they can start planning around it.

## Requirements

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
- **WHEN** an Android phone shows hits in the sheet and the user taps a hit, taps Cancel, taps the map or uses the back gesture, a first one closing only the keyboard if it is open
- **THEN** the keyboard is dismissed, the sheet shows the waypoints again at the height it had, and the search is the magnifier button again

#### Scenario: Typing in landscape on Android
- **WHEN** an Android phone is in landscape and the user taps the magnifier button and types "Utrecht" with the on-screen keyboard
- **THEN** the field and the hits show at the top of the side panel, and the map, the network switch and the attribution stay as they are

#### Scenario: No search while navigating
- **WHEN** the user navigates a route, on either app
- **THEN** there is neither a magnifier button nor a search field on the map

### Requirement: Hits in the covered area
Search SHALL only show hits inside the area covered by the knot networks. Towns and stations of that name SHALL be listed first, wherever they are in the covered area; other hits SHALL be ranked near the part of the map being shown first.

#### Scenario: Same name in several towns
- **WHEN** the map shows Zeeland and the user types "Stationsstraat"
- **THEN** streets of that name in Zeeland are listed before those farther away

#### Scenario: Town far away
- **WHEN** the map shows Helmond and the user types "Sluis"
- **THEN** the town of Sluis in Zeeland is listed first, before locks named "Sluis" near Helmond

#### Scenario: Place outside the covered area
- **WHEN** the user types the name of a place that only exists outside the covered area, e.g. "Paris"
- **THEN** it is not listed

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

### Requirement: Search needs internet access
Without internet access, the search field SHALL be disabled and SHALL say that search needs an internet connection. Planning SHALL work as without search.

#### Scenario: Airplane mode
- **WHEN** the device has no internet access
- **THEN** the search field is disabled and says that search needs an internet connection

#### Scenario: Internet access is back
- **WHEN** internet access is back
- **THEN** the search field can be used again without restarting the app

#### Scenario: Search service unavailable
- **WHEN** the device is online but the search service does not answer
- **THEN** the list says that search is not available right now, and planning keeps working

### Requirement: Typing without delay
Each character typed into the place search SHALL show at once. On iOS, typing a character SHALL NOT hold up the app noticeably, that is for no more than 100 ms on an iPhone 18 Pro, and SHALL NOT draw the map or the waypoints again.

#### Scenario: Typing a place's name
- **WHEN** the user types "Amsterdam" into the search on the iPhone, one character after the other
- **THEN** each character shows as it is typed, and the app's timing log shows no stall over 100 ms between the keystrokes

#### Scenario: The map while typing
- **WHEN** the user types into the search
- **THEN** the map and the waypoint list below the search are not drawn again for the characters

### Requirement: Hits in the app's language
The places found SHALL be named in the app's language, the one its own texts are in, whatever language the search was typed in. Where the search service has no names in that language, as for Dutch, the places SHALL be named as on the spot.

#### Scenario: German app
- **WHEN** the app's language is German, the map shows Siegburg, and the user searches "Cologne"
- **THEN** the hit reads "Köln"

#### Scenario: English app
- **WHEN** the app's language is English and the user searches "Köln"
- **THEN** the hit reads "Cologne"

#### Scenario: Dutch app
- **WHEN** the app's language is Dutch and the user searches "Netherlands" or "Köln"
- **THEN** the hits read "Nederland" and "Köln", as named on the spot

#### Scenario: App language differs from the phone's
- **WHEN** the phone's language is English, German is chosen as the app's language, and the user searches "Nederland"
- **THEN** the hit reads "Niederlande"

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
