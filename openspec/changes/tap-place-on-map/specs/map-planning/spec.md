## ADDED Requirements

### Requirement: Tapping a place on the map
A tap on a highlighted place SHALL show a card at the place with its kind's icon and its name, or the kind's name when the place has none. While planning, the card SHALL offer the route point choices of "Choose a route point on the map" below the name, and a chosen one SHALL put the route point at the place's position, not at the nearest course. A knot or a route point within the touch tolerance SHALL go first. A place beyond the distance limit of "Choose a route point on the map" (20 km cycling, 5 km hiking from every course) SHALL show its name without the choices. While navigating, the card SHALL show the name only and SHALL NOT change the route or the navigation. A tap beside the card or Back SHALL close it.

#### Scenario: Name of a café
- **WHEN** the user taps the icon of "Eiscafé Riviera" in Troisdorf at zoom 15
- **THEN** a card at the café shows the food icon and "Eiscafé Riviera", with "Start here" and "End here" below it

#### Scenario: Place without a name
- **WHEN** the user taps a drinking water icon that has no name in the map data
- **THEN** the card shows the water icon and "Drinking water", in the app's language

#### Scenario: Stop at a place
- **WHEN** a route 25 → 32 exists and the user taps a supermarket 300 m off the course and chooses "Add stop here"
- **THEN** the stop's flag is at the supermarket, and the route's way leads to it

#### Scenario: Place next to a course
- **WHEN** a café is 10 m beside a course, within the touch tolerance of both, and the user taps the café and chooses "End here"
- **THEN** the end is at the café, not on the course

#### Scenario: Knot before a place
- **WHEN** a café's icon is within the touch tolerance of knot 45 and the user taps there
- **THEN** 45 is tapped as before, and no card shows

#### Scenario: Place far from the network
- **WHEN** hiking is chosen and the user taps a railway station 30 km from every hiking course
- **THEN** the card shows the station's name without choices

#### Scenario: Place while navigating
- **WHEN** the rider is navigating and taps a toilet icon
- **THEN** the card shows "Toilets" or the toilet's name without choices, and the navigation and the route stay as they are

#### Scenario: Closing the card
- **WHEN** the card shows and the user taps beside it
- **THEN** the card closes, and nothing else happens with that tap

#### Scenario: Grey places unchanged
- **WHEN** the user taps a pharmacy drawn in the base map's own style
- **THEN** the tap works as on an empty spot: the route point choices without a name
