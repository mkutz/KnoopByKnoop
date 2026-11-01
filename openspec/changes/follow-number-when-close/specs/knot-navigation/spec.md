# Spec Delta

## MODIFIED Requirements

### Requirement: Number to follow
While navigating and close to the next knot, from when the rider is reminded of it (see "Reminder before a knot"), the app SHALL show the number to follow there as its most prominent information, e.g. "At 45 follow 12", with the knots as badges like on the map. Farther away, it SHALL show the next knot and the distance to it as its most prominent information, e.g. "Next 45", with the number to follow after it smaller, e.g. "then 12". In the same line as the next knot, it SHALL show the distance to the next knot along the route, or straight to it while off route or while the next knot is the first knot of the route. When the next knot is the end of the route, it SHALL say so instead.

#### Scenario: Next knot in the middle of the route
- **WHEN** the next knot is 45, 150 m ahead, and the route continues to 12
- **THEN** the app shows "At 45 follow 12" with 45 and 12 as badges, 12 as the most prominent element, and the distance to 45, "150 m"

#### Scenario: Far from the next knot
- **WHEN** the next knot is 83, 1.2 km ahead, and the route continues to 30
- **THEN** the app shows "Next 83" and "1.2 km" with 83 as a badge, 83 and the distance as the most prominent elements, and "then 30" smaller below them, with 30 as a badge

#### Scenario: Coming close
- **WHEN** the rider heading to 83 comes within 200 m of it and is reminded "83 ahead, then 30"
- **THEN** the app shows "At 83 follow 30" with 30 as the most prominent element

#### Scenario: Knot right after the last one
- **WHEN** the rider passes 45 and the next knot 12 is 150 m after it
- **THEN** the app shows "At 12 follow 31" right away

#### Scenario: Next knot is the end
- **WHEN** the next knot is 31, the end of the route
- **THEN** the app shows that 31 is the end of the route

#### Scenario: Distance off route
- **WHEN** the rider is off route, 1.7 km straight from the next knot 02
- **THEN** the app shows "Off route, next 02" with 02 as a badge and "1.7 km"

#### Scenario: Distance to the first knot
- **WHEN** the rider started away from the route 24, 45, 12, 31, came back within 30 m of its course 80 m straight from 24, and 24 is still the next knot
- **THEN** the app shows "At 24 follow 45" and "80 m", and outside the app "80 m to 24", not "0 m"

#### Scenario: Far from the next knot with a screen reader
- **WHEN** a screen reader reaches the header while the next knot 83 is 1.2 km ahead and 30 follows
- **THEN** it announces "Next 83, 1.2 km, then 30"

### Requirement: Number to follow at large text sizes
In every app language and at large system text sizes, the number to follow, the next knot, the distance to it and Stop SHALL be shown completely, and off route the next knot's badge. Where the distance does not fit in the line with the number to follow or the next knot, it SHALL be shown in a line below it; where the badge does not fit in the line with the off-route text, it SHALL follow the text in the next line.

#### Scenario: Number to follow at the largest text size
- **WHEN** the system text size is at its largest and the rider is 150 m before 94 with 41 to follow
- **THEN** the panel shows "At 94 follow 41" and Stop completely, and "150 m" completely below them

#### Scenario: Next knot at the largest text size
- **WHEN** the system text size is at its largest and the rider is 467 m before 94 with 41 to follow
- **THEN** the panel shows "Next 94", "467 m", "then 41" and Stop completely

#### Scenario: Number to follow at the default text size
- **WHEN** the system text size is the default
- **THEN** the distance stays in the line with the number to follow, as before

#### Scenario: Off route in German
- **WHEN** the app is in German and the rider is off route with 29 as the next knot
- **THEN** the panel shows "Abseits der Route, nächster" and the badge 29 completely, with Stop
