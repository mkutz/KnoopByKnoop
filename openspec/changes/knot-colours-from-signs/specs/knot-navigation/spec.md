## MODIFIED Requirements

### Requirement: Ferry arrival estimate
While navigating, the app SHALL estimate when the rider gets to each ferry still ahead on the route, from the route distance to it and the rider's pace, renewed with every precise position on the route. The pace SHALL be the one over the last 5 km of the route, without the time the rider stood still or was off route. Until the rider has covered 2 km of the route, it SHALL assume 16 km/h cycling and 4.5 km/h hiking. The popover of a ferry ahead on the progress strip SHALL show the estimate, rounded to five minutes, in one line that starts with it and tells whether the ferry is open then, in place of its status for now, as in "At 14:30: open until 19:00": in green when it is open then, and only the estimate without a color when the operating times do not tell.

#### Scenario: Arrival in the ferry list
- **WHEN** the rider navigates a route with a ferry ahead that runs "Apr-Oct: Mo-Su 07:00-19:00" and that they get to around 14:30 on Wednesday, 16 June 2027
- **THEN** tapping the ferry on the strip shows "At 14:30: open until 19:00" in green above the ferry's times, and no status for now

#### Scenario: Arrival when the times do not tell
- **WHEN** the rider navigates a route with a ferry ahead without operating times in OpenStreetMap and gets there around 14:30
- **THEN** the ferry's popover shows "At 14:30" without a color

#### Scenario: Ferry passed in the list
- **WHEN** the rider has passed the knot after a ferry
- **THEN** the ferry's popover shows its status for now, as the ferry list while planning

#### Scenario: Ferry passed
- **WHEN** the rider has passed the knot after a ferry
- **THEN** the strip shows no distance and arrival for it, and the app gives no warning about it

#### Scenario: Phone in another time zone
- **WHEN** the phone is set to London time and the rider gets to a ferry at 18:15 London time, which is 19:15 in the Netherlands
- **THEN** the ferry's popover says "At 19:15", the strip "19:15" and any warning "around 19:15", compared with the ferry's times in Dutch time

#### Scenario: Slowing down
- **WHEN** the rider rode 20 km at 20 km/h and then the last 5 km at 12 km/h, and 10 km are left to the end at 18:00
- **THEN** the strip shows "18:50" below the end, at 12 km/h, not "18:30"

#### Scenario: Between two knots
- **WHEN** the rider has covered more than 2 km, slows down, and is still before the next knot
- **THEN** the arrivals on the strip and in the coming knots follow without the rider passing a knot

#### Scenario: A break
- **WHEN** the rider rode 10 km at 20 km/h, stands still for 30 minutes, and rides on
- **THEN** the arrivals move later while they stand, and when they ride on the pace is still 20 km/h

#### Scenario: A detour
- **WHEN** the rider is off route for 10 minutes and comes back on the route
- **THEN** those 10 minutes do not count for the pace

#### Scenario: Closed at the renewed estimate
- **WHEN** the rider slows down between two knots so that a ferry ahead is closed at the estimate
- **THEN** the strip shows the ferry with its clock badge and its popover the warning at once, and the spoken warning comes at the next knot passed

### Requirement: Ferry warning
When a ferry ahead is closed at the rider's estimated arrival, or closes within 15 minutes after it, according to its operating times in OpenStreetMap in the ferry region's local time, the app SHALL warn the rider once per ferry with a spoken notice, and the progress strip SHALL show that ferry's badge with a dark clock badge at its corner and its arrival in bold, and its popover the warning in red. The strip SHALL NOT mark the warning with red, so it never reads like a German knot. When the operating times do not tell whether the ferry runs then, the app SHALL NOT warn.

#### Scenario: Closed when the rider gets there
- **WHEN** the rider starts navigating a cycling route at 18:00 on Wednesday, 16 June 2027, and the ferry Veer Broekhuizen, which runs "Apr-Oct: Mo-Su 07:00-19:00", is 20 km ahead
- **THEN** the app says "The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:15"

#### Scenario: Closes shortly after the rider gets there
- **WHEN** the same ferry is 12 km ahead at 18:05 on that day
- **THEN** the app says "The ferry Veer Broekhuizen closes at 19:00, shortly after you get there around 18:50"

#### Scenario: Not open yet when the rider gets there
- **WHEN** the same ferry is 8 km ahead at 05:45 on that day
- **THEN** the app says "The ferry Veer Broekhuizen is closed when you get there around 06:15, it opens at 07:00"

#### Scenario: Closed for the season
- **WHEN** the same ferry is 8 km ahead at 12:00 on Wednesday, 2 December 2026
- **THEN** the app says "The ferry Veer Broekhuizen is closed when you get there around 12:30, it opens on 1 April at 07:00"

#### Scenario: Open when the rider gets there
- **WHEN** the same ferry is 8 km ahead at 14:00 on Wednesday, 16 June 2027
- **THEN** the app gives no ferry warning

#### Scenario: The rider's own pace
- **WHEN** the rider has covered 10 km of the route in 50 minutes, and at 18:00 on Wednesday, 16 June 2027 the same ferry is 10 km ahead
- **THEN** the app says "The ferry Veer Broekhuizen closes at 19:00, shortly after you get there around 18:50", where the assumed 16 km/h would have given 18:40 and no warning

#### Scenario: Hiking
- **WHEN** the rider starts navigating a hiking route at 18:30 on Wednesday, 16 June 2027, with the same ferry 3 km ahead
- **THEN** the app says "The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:10"

#### Scenario: Falling behind
- **WHEN** the ferry was reachable when navigation started, and the rider is so slow that the estimate at a later knot is after its closing
- **THEN** the app warns at that knot

#### Scenario: Warned once
- **WHEN** the rider was warned about a ferry and passes further knots before it
- **THEN** the app does not warn about that ferry again

#### Scenario: Ferry without a name
- **WHEN** a ferry without a name in OpenStreetMap is closed when the rider gets there around 19:15
- **THEN** the app says "The ferry closes at 19:00, before you get there around 19:15"

#### Scenario: Times that cannot be told
- **WHEN** a ferry ahead runs "Mo-Sa 06:00-19:00; PH,Su 09:00-19:00" and the rider gets there at 08:00 on Good Friday, 26 March 2027, or a ferry ahead has no operating times in OpenStreetMap
- **THEN** the app gives no warning about that ferry

#### Scenario: Several ferries
- **WHEN** two ferries ahead are closed when the rider gets there
- **THEN** the app warns about both, the nearer one first

#### Scenario: Warning in the ferry list
- **WHEN** the rider navigates a route with a ferry ahead that they get to around 19:15 and that closes at 19:00 on that day
- **THEN** the ferry's badge on the strip has a clock badge at its corner, "19:15" below it is bold, and its popover shows "At 19:15: closed until tomorrow 07:00" in red

#### Scenario: Closed for the season in the ferry list
- **WHEN** the rider navigates a route with a ferry ahead that is closed until 1 May at 10:00 and gets there around 18:10
- **THEN** the ferry's popover shows "At 18:10: closed until 1 May 10:00" in red

#### Scenario: Screen off
- **WHEN** the screen is off when a ferry warning is due
- **THEN** the warning is spoken

#### Scenario: Offline
- **WHEN** the device has no internet access
- **THEN** the ferry warnings are given as online

### Requirement: Progress strip
While navigating, the panel SHALL show, below the number to follow, the route as a strip drawn to scale by distance along the route, in place of the knot badge row: the start, the stops, the end and the ferries marked and labeled, the start, the stops and the end with badges like on the map, the other knots as small marks, the rider's place on the route as a dot, and the part of the route already ridden filled. While the panel is shown, the strip SHALL follow each position.

#### Scenario: Strip at the start
- **WHEN** the rider starts navigating a route from 24 via the stop 30 to 31, 12.4 km long, at 24
- **THEN** the strip shows 24 at its start, 30 at the distance of 30 along the route, 31 at its end, the other knots as small marks, and the dot at 24 with nothing filled

#### Scenario: Moving along
- **WHEN** the rider has ridden 4 km of that route
- **THEN** the dot is a third of the way along the strip and the strip is filled up to it

#### Scenario: Ferry on the strip
- **WHEN** the route crosses a ferry between 12 and 30
- **THEN** the strip marks the ferry with ⛴ between 12 and 30

#### Scenario: Long route
- **WHEN** the route passes 80 knots and has two stops
- **THEN** the strip fits the width of the panel without scrolling, with the start, the stops and the end labeled and the other knots as marks

#### Scenario: Off route on the strip
- **WHEN** the rider is off route
- **THEN** the dot stays at the last place on the route and is shown as a ring with ✕, not in red

#### Scenario: Progress with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the strip after 4.0 of 12.4 km
- **THEN** it announces "4.0 of 12.4 km ridden, next knot 12"
