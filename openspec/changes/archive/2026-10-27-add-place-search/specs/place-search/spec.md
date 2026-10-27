# Spec Delta

## Purpose

Lets users find a place by name or address and jump there on the map, so they can start planning around it.

## ADDED Requirements

### Requirement: Search while typing
The map SHALL offer a search field. While the user types, the app SHALL list matching places, stations, addresses and businesses, each with its name and enough of its address or type to tell similar hits apart.

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
Tapping a hit SHALL move the map to it, zoomed in far enough that knots are shown and can be tapped, and SHALL mark it with a marker. The route being planned SHALL stay as it is.

#### Scenario: Tapping a hit
- **WHEN** a route is planned and the user taps the hit "Utrecht Centraal"
- **THEN** the map shows Utrecht Centraal with a marker, the knots around it can be tapped, and the route is unchanged

#### Scenario: Removing the marker
- **WHEN** a marker is shown and the user clears the search
- **THEN** the marker is removed

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
