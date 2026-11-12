## MODIFIED Requirements

### Requirement: Choose a route point on the map
Where knots are shown, a tap with no knot or route point within the tolerance SHALL offer the tapped place. Without a route, it SHALL offer only "Add to route", which adds it as a tapped knot would be: the start if there is none, otherwise the end. Once a route exists, it SHALL offer "Start here", "Add stop here" and "End here" as buttons in the order start, stop, end, each previewing that role with badges as in "Changing what a tapped knot became", the place shown by its icon or the flag, and the one where it adds the least distance marked as the suggestion. The offer SHALL NOT change the route until one is chosen, and SHALL be dismissible. A place farther than 20 km on a cycling route, or 5 km on a hiking route, from every course of the network SHALL NOT be offered; the user SHALL be told that the network has no knots within that distance.

#### Scenario: Knots go first
- **WHEN** the user taps a place with a knot within the tolerance
- **THEN** the knot is treated as tapped, and nothing is offered

#### Scenario: A route point goes first
- **WHEN** the user taps a place with a route point within the tolerance
- **THEN** nothing is offered and the route does not change

#### Scenario: Offer as previews
- **WHEN** a route 25 → 32 exists and the user taps an empty spot beside its course
- **THEN** three buttons show: the flag before 25, the flag between the knots around it where it would be a stop, and 32 before the flag, without the words "Start here", "Add stop here" and "End here", which screen readers read instead

#### Scenario: Suggestion beside the route
- **WHEN** a route 25 → 32 exists and the user taps a spot beside its course
- **THEN** the stop button is marked as the suggestion

#### Scenario: Suggestion beyond the end
- **WHEN** a route 25 → 32 exists and the user taps a spot beyond 32 seen from 25
- **THEN** the end button is marked as the suggestion

#### Scenario: Offer and dismiss
- **WHEN** the user taps a place and dismisses the offer
- **THEN** the route is unchanged

#### Scenario: Offer without a route
- **WHEN** there is no route yet and the user taps a place
- **THEN** only "Add to route" is offered, and choosing it makes the place the start, or the end if a start is set

#### Scenario: Zoomed out
- **WHEN** knots are not shown and the user taps the map
- **THEN** nothing is offered

#### Scenario: Too far from the network
- **WHEN** hiking is chosen and the user taps a place 70 km from the nearest hiking course
- **THEN** nothing is offered, the route does not change, and the user is told there are no hiking knots within 5 km

### Requirement: Tapping a place on the map
A tap on a highlighted place SHALL show a card at the place with its kind's icon and its name, or the kind's name when the place has none. While planning, the card SHALL offer the route point choices of "Choose a route point on the map" below the name, and a chosen one SHALL put the route point at the place's position, not at the nearest course. A knot or a route point within the touch tolerance SHALL go first. A place beyond the distance limit of "Choose a route point on the map" (20 km cycling, 5 km hiking from every course) SHALL show its name without the choices. While navigating, the card SHALL show the name only and SHALL NOT change the route or the navigation. A tap beside the card or Back SHALL close it.

#### Scenario: Name of a café
- **WHEN** the user taps the icon of "Eiscafé Riviera" in Troisdorf at zoom 15
- **THEN** a card at the café shows the food icon and "Eiscafé Riviera", with "Add to route" below it while there is no route, and the start, stop and end buttons, previewed with the café's icon, once there is one

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

### Requirement: What a chosen route point does
"Add to route" SHALL make the place the start, or the end if a start is set. "Start here" SHALL make the place the new start, "End here" the new end, and "Add stop here" a stop in the leg where it adds the least distance. A waypoint that was the start or end before SHALL become a stop, whether it is a knot or a route point.

#### Scenario: First waypoint
- **WHEN** there are no waypoints and the user chooses "Add to route", then taps 28
- **THEN** the route goes from the point to 28

#### Scenario: End before the start
- **WHEN** 28 is the only waypoint and the user chooses "Add to route" for a place
- **THEN** 28 is the start and the place the end

#### Scenario: Before an existing start
- **WHEN** the route is 02 → 28 and the user chooses "Start here"
- **THEN** the waypoints are the point, 02 as a stop, and 28

#### Scenario: After an existing end
- **WHEN** the route is 28 → 02 and the user chooses "End here"
- **THEN** the waypoints are 28, 02 as a stop, and the point

#### Scenario: An earlier route point
- **WHEN** the start is a route point and the user chooses "Start here" somewhere else
- **THEN** the new place is the start and the earlier point a stop, which the user can remove

#### Scenario: A round trip stops being one
- **WHEN** the route is a round trip from 12 and the user chooses "End here"
- **THEN** the waypoints keep 12 as start and as a stop, and the point is the end
