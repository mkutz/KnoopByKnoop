# Spec Delta

## ADDED Requirements

### Requirement: Guided way while off route
While navigating a cycling route and off route, with guidance away from the route turned on (see `network-sync`), the app SHALL find a way along suitable paths from the rider to the end of the line in "Way to the route on the map", as soon as the path data allows. It SHALL NOT change the planned route and SHALL NOT need network access at that moment.

#### Scenario: Starting away from the route
- **WHEN** the rider starts navigating at home, off route, with the squares installed and 24 as the first knot
- **THEN** the app finds a way to 24 along suitable paths and guides the rider along it

#### Scenario: Wrong turn
- **WHEN** the rider left the course of the leg to 45 and is off route with the squares installed
- **THEN** the app finds a way to the course ahead of where they left it

#### Scenario: Without courses
- **WHEN** the network's courses aren't installed and the rider is off route with the squares installed
- **THEN** the app finds a way to the next knot

#### Scenario: Path data arrives on the way
- **WHEN** the rider is off route without a way, and the squares between them and the target finish downloading
- **THEN** the app finds a way at the next location fix and guides the rider along it

#### Scenario: No way, no path data, far away or hiking
- **WHEN** no way can be found, the squares aren't installed, the target is more than 20 km away, or the route is a hiking route
- **THEN** off route works without a guided way, with the straight line, distance and compass direction

#### Scenario: Guidance turned off
- **WHEN** guidance away from the route is off and the rider is off route
- **THEN** off route works without a guided way, with the straight line, distance and compass direction

#### Scenario: Back on route with a guided way
- **WHEN** the rider following a guided way comes within 30 m of the course
- **THEN** the guided way ends, and the app says they are back on route, as in "Off route"

#### Scenario: Skipping ahead on the guided way
- **WHEN** the rider following a guided way to 24 passes 12, a later knot of the route
- **THEN** the guided way ends and navigation continues from 12, as when skipping ahead

### Requirement: Leaving the guided way
When the rider is more than 30 m from the guided way on two location fixes in a row, the app SHALL find a new way from their position and riding direction, at most once every 10 seconds. It SHALL keep the last way until it has a new one. While the rider is more than 15 m from the way, the distances shown SHALL include how far they are from it, and the line on the map SHALL start at the rider.

#### Scenario: Missing a turn
- **WHEN** the rider rides past a turn and is more than 30 m from the way on two fixes
- **THEN** the app finds a new way, and the next turn notice is about the new way

#### Scenario: A single inaccurate fix
- **WHEN** one fix is more than 30 m from the way and the next is on it again
- **THEN** the way stays as it is

#### Scenario: No new way
- **WHEN** no new way can be found
- **THEN** off route continues without a guided way

#### Scenario: Riding away from the way
- **WHEN** the rider is 40 m from a way with 320 m left, and hasn't been more than 30 m away on two fixes yet
- **THEN** the distance left shown is "360 m", and the line on the map runs from the rider to the way

### Requirement: Turn notices on the guided way
On a guided way, the app SHALL say each turn once, about 100 m before it, e.g. "In 100 m, turn left". Turns within 40 m of each other SHALL count as one. Continuing straight on SHALL need no notice, and neither SHALL a bend where every other way at the junction turns at least 30° more than the way to take. Several turns at one junction that add up to straight on SHALL be said as straight on. When a way starts against the riding direction, the app SHALL say to turn back, and SHALL show it until the rider follows the way. The notice SHALL be spoken, also with the screen off and in the app's language.

#### Scenario: A turn ahead
- **WHEN** the rider is 100 m from a left turn on the guided way
- **THEN** the app says "In 100 m, turn left" once

#### Scenario: Turn close to the start of the way
- **WHEN** a new way has its first turn 40 m ahead
- **THEN** the app says "Turn left now", after the off-route notice

#### Scenario: Two turns together
- **WHEN** the way turns right and, 30 m later, left
- **THEN** the app gives one notice, using the overall direction of the two

#### Scenario: Straight on
- **WHEN** the way continues straight on over a junction
- **THEN** the app says nothing there

#### Scenario: A bend without a choice
- **WHEN** the path bends right at a junction, and the only other way there turns left by more than 30° further
- **THEN** the app says nothing there

#### Scenario: A bend with a choice
- **WHEN** the way bends right at a junction, and another way there continues straighter
- **THEN** the app says the turn

#### Scenario: Crossing with an offset
- **WHEN** the way turns right and, 30 m later, left at a crossing, and continues in the same direction
- **THEN** the app says "In 100 m, go straight on"

#### Scenario: Way behind the rider
- **WHEN** a new way starts against the riding direction
- **THEN** the app says "Turn back now", and the header shows "Turn back" until the rider has followed the way for 15 m

#### Scenario: Still riding away
- **WHEN** the rider ignores it, and the next new way also starts against the riding direction
- **THEN** the header shows "Turn back" again, and the app doesn't say it again

#### Scenario: Screen off
- **WHEN** the rider reaches a turn notice with the screen off
- **THEN** it is spoken, without more location fixes than off route uses without a guided way

### Requirement: Guided way in the header and outside the app
On a guided way, the header, the Android notification and the iOS Live Activity SHALL show the next turn with its distance, and the distance left along the way, in place of the straight distance. The header SHALL also show the street name of the turn when known.

#### Scenario: In the header
- **WHEN** the rider is off route with next knot 02, a left turn into Weierstraße 120 m ahead and 1.9 km left along the way
- **THEN** the header shows "Off route, next 02", "Left in 120 m, Weierstraße" and "1.9 km"

#### Scenario: On the lock screen
- **WHEN** the same rider looks at the lock screen
- **THEN** it shows "Off route, next 02" and "Left in 120 m · 1.9 km"

#### Scenario: Distance to the course
- **WHEN** the guided way ends on the course of the route, 300 m ahead
- **THEN** the distance left shown is "300 m", to the route and not to the next knot

#### Scenario: Distances ahead on a guided way
- **WHEN** the rider is on a guided way with 140 m left, ending on the course 1.1 km before 82
- **THEN** the progress panel shows 1.2 km to 82, and the knots after it and the arrival times count from there

#### Scenario: Header with a screen reader
- **WHEN** a screen reader reaches the header on a guided way
- **THEN** it announces "Off route, next 02, left in 120 m, Weierstraße, 1.9 km"

## MODIFIED Requirements

### Requirement: Off route
The rider SHALL be told they are off route when they pass a knot that is not on the route, or are more than 100 m away from the course of the route. They SHALL be back on route only when they pass a knot of the route or come within 30 m of the course, so a path running alongside the route does not count as the route. The notice SHALL name the next knot and the straight distance and compass direction to it, e.g. "Off route, next 45, 1.7 km to the north-east". While off route without a guided way, a guidance notice SHALL repeat the distance and direction when the rider got 500 m closer to the knot or its direction changed by more than 45° since the last notice. On a guided way, the turn notices SHALL replace these guidance notices. The planned route SHALL NOT change.

#### Scenario: Passing a knot that is not on the route
- **WHEN** the rider heading to 45 passes knot 77, which is not on the route
- **THEN** the app says "Off route, next 45" with the distance and direction to 45

#### Scenario: Wrong turn between knots
- **WHEN** the rider heading to 45 leaves the course of the route and is more than 100 m away from it, with 45 1.7 km to the north-east
- **THEN** the app says "Off route, next 45, 1.7 km to the north-east"

#### Scenario: Back on route
- **WHEN** the rider who is off route comes back within 30 m of the course, or passes a knot of the route
- **THEN** the app says they are back on route and names the next knot

#### Scenario: Parallel path
- **WHEN** the rider who is off route rides on a path 60 m beside the course
- **THEN** they are still off route, and the app does not say they are back on route

#### Scenario: Without geometry
- **WHEN** the network's courses are not installed
- **THEN** off route is only detected by passing a knot that is not on the route

#### Scenario: Off route is announced once
- **WHEN** the rider stays off route for several minutes
- **THEN** the app says "Off route" once, not repeatedly, and only the guidance or turn notices follow

#### Scenario: Getting closer
- **WHEN** the rider is off route without a guided way and got 500 m closer to 45 since the last notice, now 1.2 km to the north
- **THEN** the app says "Next 45, 1.2 km to the north"

#### Scenario: Direction changes
- **WHEN** the rider is off route without a guided way and the direction to 45 changed from north-east to south-east
- **THEN** the app says the distance and the new direction, e.g. "Next 45, 1.4 km to the south-east"

#### Scenario: Riding around without getting closer
- **WHEN** the rider is off route without a guided way, neither 500 m closer nor in a direction more than 45° off since the last notice
- **THEN** the app says nothing

#### Scenario: No guidance notices on a guided way
- **WHEN** the rider is on a guided way and got 500 m closer to 45
- **THEN** the app gives no distance and direction notice, only the turn notices

### Requirement: Way to the route on the map
While navigating and off route, the map SHALL show a dashed straight line from the rider's position to the nearest point of the route ahead of them: on the course of the leg to the next knot, after the last place the rider was on the route. Without the network's courses, or before the first knot, the line SHALL go to the next knot. On a guided way, the map SHALL show a solid line along that way instead of the dashed line. While the map follows the rider, it SHALL keep both the rider and the end of that line in view.

#### Scenario: Line to the route
- **WHEN** the rider heading to 02 left the course to the west and is off route 150 m from it, with 02 1.7 km ahead
- **THEN** the map shows a dashed line from their position to the nearest point of the course ahead of where they left it, about 150 m long, not to 02

#### Scenario: Route behind the rider
- **WHEN** the rider is off route and the nearest point of the leg's course lies before the place where they left the route
- **THEN** the line goes to that place where they left the route, not back along the part they already rode

#### Scenario: Line to the knot
- **WHEN** the network's courses are not installed, or the rider is off route before the first knot 24
- **THEN** the map shows a dashed line from their position to the next knot

#### Scenario: Both in view
- **WHEN** the rider is off route 400 m from the course and the map follows them
- **THEN** the map shows both their position and the end of the line

#### Scenario: Back on route
- **WHEN** the rider comes back on route
- **THEN** the dashed line is gone and the map follows the rider as before

#### Scenario: Line along the guided way
- **WHEN** the rider is off route on a guided way
- **THEN** the map shows a solid line along the way and no dashed line

#### Scenario: Back to the dashed line
- **WHEN** the rider is off route and the guided way can't be replaced after leaving it
- **THEN** the map shows the dashed line again

### Requirement: Direction to the route
While navigating and off route without a guided way, the header SHALL show, next to the distance to the next knot, an arrow pointing towards it relative to the rider's riding direction, and the compass direction in words (north, north-east, east, south-east, south, south-west, west, north-west). When the rider has not moved recently, the arrow SHALL point relative to north, as the map.

#### Scenario: Direction in the header
- **WHEN** the rider is off route without a guided way, rides north and 02 is 1.7 km to the north-east
- **THEN** the header shows "Off route, next 02 1.7 km", an arrow pointing to the front right and "north-east"

#### Scenario: Standing still
- **WHEN** the rider has not moved recently, has no guided way and 02 is to the east
- **THEN** the arrow points to the right, as east on the map

#### Scenario: Direction with a screen reader
- **WHEN** a screen reader reaches the header while off route without a guided way
- **THEN** it announces "Off route, next 02, 1.7 km to the north-east"

### Requirement: Direction outside the app
While navigating and off route without a guided way, the Android notification and the iOS Live Activity SHALL show the distance and compass direction to the next knot, e.g. "1.7 km north-east to 02", updated when the distance changed by 100 m or more, without more location fixes than navigating does.

#### Scenario: Lock screen off route
- **WHEN** the rider is off route without a guided way and with the screen off, 1.7 km south-west of 02
- **THEN** the lock screen shows "Off route, next 02" and "1.7 km north-east to 02"

#### Scenario: Updated on the way
- **WHEN** the rider got 100 m closer to 02
- **THEN** the lock screen shows the new distance

#### Scenario: Back on route outside the app
- **WHEN** the rider comes back on route
- **THEN** the lock screen shows the number to follow and the progress line as before
