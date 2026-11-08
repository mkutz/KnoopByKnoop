## ADDED Requirements

### Requirement: Places useful on a ride
The map SHALL draw places from the base map that matter on a ride with their own icon on a coloured disc, one per kind:
- **food and drink:** restaurants, cafés, fast food, bars, pubs, beer gardens and ice cream
- **supermarkets**
- **bicycle shops,** including bicycle repair, and bicycle repair stations where the base map has them
- **railway stations** and halts
- **water and toilets:** drinking water and public toilets

The discs' colours SHALL differ from the knots' sign colours and from the route. The base map SHALL NOT draw these places a second time in its own style. The places SHALL be shown from zoom 14, and their names from zoom 16. The knots, the route, the waypoints and the rider's position SHALL be drawn above them, and where a place's icon would overlap a knot's badge, the badge SHALL be shown and the place left out. The places SHALL be shown on the light and the dark map, for both network types, while planning and while navigating. They come with the base map, so where the base map is missing they are missing too; nothing beyond the base map SHALL be loaded for them.

#### Scenario: Places in a town
- **WHEN** the map shows the centre of Troisdorf at zoom 15
- **THEN** cafés and restaurants, supermarkets, the bicycle shops and Troisdorf station each show their kind's icon on its coloured disc, without names

#### Scenario: Names close up
- **WHEN** the user zooms in to zoom 16 over a highlighted café
- **THEN** its name is shown next to its icon

#### Scenario: Zoomed out
- **WHEN** the map is at zoom 13
- **THEN** no highlighted places are shown

#### Scenario: Shown once
- **WHEN** a supermarket is highlighted
- **THEN** the base map's grey shop icon is not drawn at the same place

#### Scenario: Other places unchanged
- **WHEN** the map shows a pharmacy, a bank or a bike rental
- **THEN** they are drawn in the base map's own style, as before this change

#### Scenario: Knot next to a café
- **WHEN** a café is a few metres from a knot, so their icons would overlap
- **THEN** the knot's badge is shown in full and the café's icon is left out

#### Scenario: Under the route
- **WHEN** the route runs past a restaurant
- **THEN** the route line is drawn above the restaurant's icon

#### Scenario: Distinct from knots
- **WHEN** highlighted places are shown among Dutch, Belgian and German knots
- **THEN** no place disc has the colour of a knot's sign, and each kind can be told from the others by its icon and colour

#### Scenario: Places on the dark map
- **WHEN** the app is dark and the map shows highlighted places
- **THEN** their icons and names are as readable as on the light map

#### Scenario: Places while navigating
- **WHEN** the rider navigates through a town at zoom 15 or closer
- **THEN** the highlighted places are shown along the way, below the route and the rider's arrow

#### Scenario: Places on a hiking map
- **WHEN** the hiking network is selected
- **THEN** the same places are highlighted

#### Scenario: Without the base map
- **WHEN** the base map cannot be loaded, e.g. offline on first start
- **THEN** the network is shown as before and no places are shown

### Requirement: Switch for places on the map
A switch in the info sheet SHALL turn the highlighted places off and on again. It SHALL be on by default, and the choice SHALL be kept across restarts. With the switch off, the map SHALL look as before the places were highlighted.

#### Scenario: Turning the places off
- **WHEN** the user turns the places off in the info sheet
- **THEN** the map shows no highlighted places, and the base map draws all its places in its own style as before this change

#### Scenario: Choice kept for places
- **WHEN** the user turned the places off and starts the app again
- **THEN** the places are still off

#### Scenario: Switch by default
- **WHEN** the app is started for the first time after this change
- **THEN** the switch is on and the places are shown
