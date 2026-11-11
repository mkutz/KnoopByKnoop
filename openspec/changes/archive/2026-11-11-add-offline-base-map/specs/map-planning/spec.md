## MODIFIED Requirements

### Requirement: Show the network on the map
The map SHALL show the knots of the selected, installed network type with their numbers, and the connections between them along their actual course, over a base map. The user SHALL be able to switch between the cycling and the hiking network.

#### Scenario: Cycling network of the Netherlands
- **WHEN** the cycling network is installed and the user opens the map over the Netherlands with the cycling network selected
- **THEN** the map shows the Dutch cycling knots with their numbers and their connections, continuing without a gap into Belgium and Nordrhein-Westfalen

#### Scenario: Switching to hiking
- **WHEN** the user switches from cycling to hiking
- **THEN** the map shows the hiking network instead at the same place, zoomed in far enough that knots are shown, and the current route is discarded after the user confirms

#### Scenario: Opening the map without a route
- **WHEN** the map opens and there is no route to show
- **THEN** it is zoomed in far enough that knots are shown and can be tapped

#### Scenario: Without matching geometry
- **WHEN** an installed network has no geometry that belongs to its graph, e.g. because its download failed
- **THEN** connections and the route are drawn as straight lines between knots, and planning works unchanged

#### Scenario: No network access
- **WHEN** the device has no network access
- **THEN** the knots and connections are still shown and planning works; the base map may be missing, except around the planned route once its map is stored, see `network-sync` "Base map around the route"

#### Scenario: Riding without a connection
- **WHEN** the base map of the planned route is stored and the rider navigates it with no network access
- **THEN** streets, villages, rivers and woods are shown along the route, in the light and in the dark map

#### Scenario: Attribution
- **WHEN** the map is shown
- **THEN** the attribution required by OpenStreetMap and the base map provider is visible, except on iOS in landscape while the user types in the search with the on-screen keyboard, when the hits and the keyboard cover the map
