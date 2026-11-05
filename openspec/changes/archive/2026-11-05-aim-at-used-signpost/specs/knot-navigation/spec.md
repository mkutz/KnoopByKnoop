## ADDED Requirements

### Requirement: Straight ways to a knot with several signposts
Wherever navigation gives a straight distance or direction to the next knot, or draws a dashed line to it, and wherever it decides by the straight distance to that knot whether the rider is near it, it SHALL measure to the signpost the route's line uses: where the line first reaches the knot, or for the first knot of the route, where the line leaves it. Passing any signpost of the knot SHALL still count as passing it. Without the network's courses, it SHALL measure to the knot's main position.

#### Scenario: Approaching the first knot at the signpost the route leaves from
- **WHEN** the route 02, 28 leaves 02 at its north-west signpost, its main position is by the river, and the rider rides towards the north-west signpost, now 80 m from it
- **THEN** the app shows "At 02 follow 28" and "80 m", and the distance shrinks as they get closer

#### Scenario: Dashed line to the first knot
- **WHEN** the rider is off route before the first knot 02 of the route 02, 28, which leaves 02 at its north-west signpost
- **THEN** the dashed line ends at the north-west signpost, not at the one by the river

#### Scenario: Off route, heading for a knot on the route
- **WHEN** the rider on the route 06, 02, 28 is off route with 02 next, and the line reaches 02 at its north-west signpost, 1.2 km to the north
- **THEN** the app says "Off route, next 02, 1.2 km to the north", measured to that signpost, in the notice, the header and outside the app

#### Scenario: The route uses the other signpost
- **WHEN** the rider on the route 06, 02, 01 is off route with 02 next, and the line reaches 02 at its signpost by the river
- **THEN** the distance and direction go to the signpost by the river

#### Scenario: Line through both signposts
- **WHEN** the rider on the route 28, 02, 01 heads for 02, and the line reaches its north-west signpost first and runs on to the one by the river
- **THEN** the reminder, and the distance and direction while off route, go to the north-west signpost, and on the route 01, 02, 28 to the one by the river

#### Scenario: Reminder at the signpost used
- **WHEN** the rider on the route 06, 02, 28 comes within 200 m of the north-west signpost of 02, more than 200 m from its main position
- **THEN** the app says "02 ahead, then 28" once

#### Scenario: Passing the other signpost
- **WHEN** the rider heading for the north-west signpost of 02 passes its signpost by the river instead
- **THEN** 02 counts as passed and the app names the next knot

#### Scenario: A knot with one signpost
- **WHEN** the next knot has one position
- **THEN** the distances and directions are the same as before this change

#### Scenario: Without courses
- **WHEN** the network's courses are not installed and the rider is off route with 02 next
- **THEN** the distance, the direction and the dashed line go to the main position of 02
