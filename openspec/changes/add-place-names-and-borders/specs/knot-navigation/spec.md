## MODIFIED Requirements

### Requirement: Coming knots
While navigating, the panel SHALL list the coming knots of the route below the progress strip, in riding order from the next knot to the end: each with its badge as on the map, the place it lies in, named as in the waypoint list, the distance to it along the route and the arrival, at the pace of "Ferry arrival estimate", following each position while shown. Ferries SHALL be listed between the knots they connect, with their arrival state as on the strip, and so SHALL the borders the route crosses, each naming the country and the province or state beyond it, in the app's language. Passed knots SHALL NOT be listed.

#### Scenario: Coming knots at the start
- **WHEN** the rider starts navigating the route 24, 45, 30 (stop), 12, 31 at 18:20 at 24, with 45 at 1.1 km, 30 at 5.2 km, 12 at 8.0 km and 31 at 12.4 km
- **THEN** the list shows 45 "1.1 km", 30 "5.2 km 18:40", 12 "8.0 km", 31 "12 km 19:05", each with its arrival, 30 and 31 with their stop and end badges

#### Scenario: After passing knots
- **WHEN** the rider has passed 45 and 30 of that route
- **THEN** the list starts with 12 and no longer shows 45 and 30

#### Scenario: Ferry in the list
- **WHEN** the route crosses a ferry between 12 and 31 that is closed at the rider's arrival
- **THEN** the list shows the ferry between 12 and 31, with its arrival in red as on the strip

#### Scenario: Off route
- **WHEN** the rider is off route 1.7 km from the next knot 12
- **THEN** the list starts with 12 at "1.7 km", and the distances to the knots after it count from 12 plus 1.7 km

#### Scenario: Long route
- **WHEN** the route has 80 knots ahead
- **THEN** the list shows them all, and scrolls

#### Scenario: Coming knots with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches 30 in the list
- **THEN** it announces "Knot 30, stop, 5.2 km, 18:40"

#### Scenario: Places in the list
- **WHEN** the rider navigates 06 → 02 → 28, with 06 in Lohmar, 02 in the open and 28 in Sankt Augustin
- **THEN** the list shows Lohmar beside 06, nothing beside 02, and Sankt Augustin beside 28

#### Scenario: Crossing into another country
- **WHEN** the route crosses from Dutch Limburg into Belgian Limburg between 95 and 66
- **THEN** a line between 95 and 66 says "Belgium, Limburg", in German "Belgien, Limburg"

#### Scenario: Crossing a province border
- **WHEN** the route crosses from Gelderland into Overijssel between 12 and 31
- **THEN** a line between 12 and 31 says "Overijssel"

#### Scenario: Place and border with a screen reader
- **WHEN** a screen reader reaches 28 in Sankt Augustin, 5.2 km ahead at 18:40, and then the border line
- **THEN** it announces "Knot 28, Sankt Augustin, end, 5.2 km, 18:40", and for the border "Border, Belgium, Limburg"
