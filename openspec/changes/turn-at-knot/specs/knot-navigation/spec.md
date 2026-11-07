## ADDED Requirements

### Requirement: Turn at the next knot
While navigating close to the next knot, the sign plate around the number to follow SHALL show, as its arrow, the turn the route makes at that knot. The turn compares the route's direction over the 40 m before the knot with its direction over the 40 m after it, along the route's courses, also across knots within those 40 m. It SHALL be classified like the turns on the guided way:
- straight on (↑)
- slight left (↖) or slight right (↗)
- left or right
- sharp left (↙) or sharp right (↘)
- back (a U-turn)

The same arrow SHALL be shown on the plate in the Live Activity and the Dynamic Island. The Android notification's title SHALL have the turn's character before the number to follow. Where the turn cannot be told, the plate SHALL show the plain → instead.

#### Scenario: Right turn at a knot
- **WHEN** the rider is 150 m before 45, and the route comes into 45 heading north and leaves it heading east towards 12
- **THEN** the plate around 12 shows a right-turn arrow

#### Scenario: Straight on
- **WHEN** the route passes 45 with less than the guided way's straight-on angle between its direction before and after the knot
- **THEN** the plate shows ↑

#### Scenario: Slight left
- **WHEN** the route bends 40° to the left at 45
- **THEN** the plate shows ↖

#### Scenario: Turning back at an out-and-back knot
- **WHEN** the route goes out to 45 and comes back the same way towards 12
- **THEN** the plate shows a U-turn arrow

#### Scenario: Knot right after the next one
- **WHEN** 46 is 10 m after 45, and the route turns right between them
- **THEN** close to 45, the plate shows the right turn, measured over the 40 m after 45, across 46

#### Scenario: Knot with several signposts
- **WHEN** the route enters 02 at its signpost by the river and leaves it from its signpost near the L 16, running along the path between them
- **THEN** the turn is measured from the route's direction into the first signpost to its direction out of the second

#### Scenario: Without courses
- **WHEN** the network's courses are not installed
- **THEN** the plate shows the plain →

#### Scenario: Start knot
- **WHEN** navigation begins at the start knot 24 and the rider has not moved yet
- **THEN** the plate around the number to follow shows the plain →

#### Scenario: Far from the knot
- **WHEN** the rider is 1.2 km before 83
- **THEN** no turn is shown, as there is no plate between knots

#### Scenario: Turn on the lock screen
- **WHEN** the rider is close to 45, turns right there towards 12, and looks at the lock screen
- **THEN** the Live Activity shows the plate around 12 with the right-turn arrow, and the Android title reads "45 → ↱12"

#### Scenario: Turn with a screen reader
- **WHEN** a screen reader reaches the header close to 45 with a right turn to 12
- **THEN** it announces "At 45 follow 12 to the right, 150 m"
