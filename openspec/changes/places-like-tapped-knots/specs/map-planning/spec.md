## MODIFIED Requirements

### Requirement: Choose a route point on the map
Where knots are shown, a tap with no knot, route point or highlighted place within the tolerance SHALL offer "Add to route" at the tapped spot, and SHALL NOT change the route until it is chosen. "Add to route" SHALL add a route point at the spot as a tapped knot is added: the start if there is none, the end if there is no end, and otherwise where it adds the least distance, see "Plan by tapping knots". Once a route exists, the note of "Changing what a tapped knot became" SHALL follow. The offer SHALL be dismissible. A spot farther than 20 km on a cycling route, or 5 km on a hiking route, from every course of the network SHALL NOT be added; the user SHALL be told that the network has no knots within that distance.

#### Scenario: Knots go first
- **WHEN** the user taps a place with a knot within the tolerance
- **THEN** the knot is treated as tapped

#### Scenario: A route point goes first
- **WHEN** the user taps a place with a route point within the tolerance
- **THEN** nothing is offered and the route does not change

#### Scenario: Spot beside the route
- **WHEN** a route 25 → 32 exists and the user taps an empty spot beside its course and chooses "Add to route"
- **THEN** a route point at the spot is added as a stop, and the note offers to make it the start or the end, or to undo it

#### Scenario: Spot beyond the end
- **WHEN** a route 25 → 32 exists and the user taps an empty spot beyond 32 seen from 25 and chooses "Add to route"
- **THEN** the route point is the new end, and 32 a stop

#### Scenario: Offer and dismiss
- **WHEN** the user taps an empty spot or a highlighted place and closes the offer without choosing "Add to route"
- **THEN** the route is unchanged

#### Scenario: Offer without a route
- **WHEN** there is no waypoint yet and the user adds an empty spot with "Add to route", and then taps knot 28
- **THEN** the route goes from the spot to 28, and no note is shown

#### Scenario: Zoomed out
- **WHEN** knots are not shown and the user taps the map
- **THEN** nothing is offered

#### Scenario: Too far from the network
- **WHEN** hiking is chosen and the user taps a place 70 km from the nearest hiking course
- **THEN** nothing is offered, the route does not change, and the user is told there are no hiking knots within 5 km

### Requirement: What a chosen route point does
"Start here", "Add stop here" and "End here" are the start, stop and end buttons of the note for a route point. "Start here" SHALL make the route point the new start, "End here" the new end, and "Add stop here" a stop in the leg where it adds the least distance. A waypoint that was the start or end before SHALL become a stop, whether it is a knot or a route point.

#### Scenario: First waypoint
- **WHEN** there are no waypoints and the user adds an empty spot with "Add to route", then taps 28
- **THEN** the route goes from the point to 28

#### Scenario: End before the start
- **WHEN** 28 is the only waypoint and the user adds a place with "Add to route"
- **THEN** 28 is the start and the place the end

#### Scenario: Before an existing start
- **WHEN** the route is 02 → 28, a route point was added, and the user chooses "Start here"
- **THEN** the waypoints are the point, 02 as a stop, and 28

#### Scenario: After an existing end
- **WHEN** the route is 28 → 02, a route point was added, and the user chooses "End here"
- **THEN** the waypoints are 28, 02 as a stop, and the point

#### Scenario: An earlier route point
- **WHEN** the start is a route point and the user chooses "Start here" for another one
- **THEN** the new place is the start and the earlier point a stop, which the user can remove

#### Scenario: A round trip stops being one
- **WHEN** the route is a round trip from 12 and the user chooses "End here" for a route point
- **THEN** the waypoints keep 12 as start and as a stop, and the point is the end

### Requirement: Tapping a place on the map
A tap on a highlighted place SHALL show a card at the place with its kind's icon and its name, or the kind's name when the place has none, and its details. While planning, the card SHALL offer "Add to route" below the name, which SHALL add a route point at the place's position, not at the nearest course, as an empty spot is added, see "Choose a route point on the map". Showing the card SHALL NOT change the route. A knot or a route point within the touch tolerance SHALL go first. A place beyond the distance limit of "Choose a route point on the map" (20 km cycling, 5 km hiking from every course) SHALL show its name without "Add to route". While navigating, the card SHALL show the name only and SHALL NOT change the route or the navigation. A tap beside the card or Back SHALL close it.

#### Scenario: Name of a café
- **WHEN** the user taps the icon of "Eiscafé Riviera" in Troisdorf at zoom 15
- **THEN** a card at the café shows the food icon and "Eiscafé Riviera", with "Add to route" below it, and the route does not change

#### Scenario: Place without a name
- **WHEN** the user taps a drinking water icon that has no name in the map data
- **THEN** the card shows the water icon and "Drinking water", in the app's language

#### Scenario: Stop at a place
- **WHEN** a route 25 → 32 exists and the user taps a supermarket 300 m off the course and chooses "Add to route"
- **THEN** the supermarket is added where it adds the least distance, here as a stop, the card closes, the note follows, and the route's way leads to it

#### Scenario: Place next to a course
- **WHEN** a café is 10 m beside a course, within the touch tolerance of both, and the user taps the café and chooses "Add to route"
- **THEN** the route point is at the café, not on the course

#### Scenario: Knot before a place
- **WHEN** a café's icon is within the touch tolerance of knot 45 and the user taps there
- **THEN** 45 is tapped as before, and no card shows

#### Scenario: Place far from the network
- **WHEN** hiking is chosen and the user taps a railway station 30 km from every hiking course
- **THEN** the card shows the station's name without "Add to route"

#### Scenario: Place while navigating
- **WHEN** the rider is navigating and taps a toilet icon
- **THEN** the card shows "Toilets" or the toilet's name without "Add to route", and the navigation and the route stay as they are

#### Scenario: Closing the card
- **WHEN** the card shows and the user taps beside it
- **THEN** the card closes, and nothing else happens with that tap

#### Scenario: Grey places unchanged
- **WHEN** the user taps a pharmacy drawn in the base map's own style
- **THEN** the tap works as on an empty spot: "Add to route" is offered without a name

### Requirement: Changing what a tapped knot became
After a tapped knot, a tapped spot or a place was added to an existing route, the app SHALL show a short note: one line saying what it became, three buttons in the fixed order start, stop and end, each previewing that role with badges, and Undo. The button of the role it got SHALL show as selected and inactive. The buttons SHALL act as "Start here", "Add stop here" and "End here" do. The note SHALL go away after 6 seconds, on a choice or with the next tap, but not by itself while a screen reader is on.

#### Scenario: Note after a tap
- **WHEN** a route 12 → 14 → 15 exists and the user taps 27, which becomes a stop between 14 and 15
- **THEN** the note says "27 added as a stop" and shows the buttons 27 before 12 for the start, 14, 27 and 15 for the stop, selected, and 15 before 27 for the end, then Undo

#### Scenario: Note for a new start
- **WHEN** a tapped knot 60 became the new start before 05
- **THEN** the note says "60 added as the start", and the start button shows 60 before 05 and is selected

#### Scenario: Note for a new end
- **WHEN** a tapped knot 22 became the new end after 15
- **THEN** the note says "22 added as the end", the end button shows 15 before 22 and is selected, and the stop button shows 22 with the knots around it where it would be a stop

#### Scenario: Note for a place
- **WHEN** a route 25 → 32 exists and the user adds "Eiscafé Riviera", which becomes a stop
- **THEN** the note says "Eiscafé Riviera added as a stop", and the buttons preview the café with its icon

#### Scenario: Note for a spot
- **WHEN** a route 25 → 32 exists and the user adds an empty spot beyond 32 with "Add to route"
- **THEN** the note says "Added as the end", and the buttons preview the spot with the flag

#### Scenario: Make it the start
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the start button
- **THEN** the waypoints are 30, 12 as a stop, and 45

#### Scenario: Make it the end
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the end button
- **THEN** the waypoints are 12, 45 as a stop, and 30

#### Scenario: Make it a stop
- **WHEN** 28 was added as the new end of 12 → 45 and the user chooses the stop button
- **THEN** 28 is a stop where it adds the least distance, and 45 is the end again

#### Scenario: Undo the tap
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses Undo
- **THEN** the waypoints are 12 and 45 again

#### Scenario: Note goes away
- **WHEN** the note is shown and the user does nothing for 6 seconds, or taps another knot
- **THEN** the note is gone, and for the other knot a new note is shown

#### Scenario: Note with a screen reader
- **WHEN** a screen reader is on and 27 was added as a stop between 14 and 15
- **THEN** it reads the line "27 added as a stop", the buttons as "As the start", "As a stop, selected" and "As the end", and the note stays until a choice is used or the next tap

#### Scenario: No note for the first two taps
- **WHEN** the user taps the start and then the end of a new route
- **THEN** no note is shown
