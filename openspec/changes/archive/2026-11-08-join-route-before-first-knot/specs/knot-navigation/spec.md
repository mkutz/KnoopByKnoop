## ADDED Requirements

### Requirement: Joining the course on the way to the start
While navigation leads to the first knot of the route, a rider who follows the course of a leg of the route for at least 50 m, within 30 m of it, SHALL join the route there: the next knot SHALL be the end of that leg, as when starting on the course. Crossing a course SHALL NOT join it. Until joined, the rider SHALL NOT be told they are back on route, except within 200 m of the first knot, where its course counts as the way there, as in "Distance to the first knot".

#### Scenario: Reaching the course and following it
- **WHEN** the rider started navigating the route 06 → 02 250 m from its course, heads for 06, reaches the course 4 km before 06 and rides 100 m along it towards 02
- **THEN** navigation continues with 02 as the next knot, the line ahead is drawn, and the rider is told they are back on route

#### Scenario: Crossing the course
- **WHEN** the rider heads for the first knot 24 of the route 24, 45, 12, 31 and crosses the course of the leg from 45 to 12 at a right angle
- **THEN** navigation keeps 24 as the next knot and the rider stays off route

#### Scenario: Reaching the course of the first leg near the start
- **WHEN** the rider heads for 24 and follows the course of the leg from 24 to 45 from 300 m after 24 on
- **THEN** navigation continues with 45 as the next knot

#### Scenario: On the course close to the first knot
- **WHEN** the rider heads for 24 and comes within 30 m of the course of the leg from 24 to 45, 80 m from 24
- **THEN** the rider is back on route with 24 still the next knot, as before

#### Scenario: Following the course against the route
- **WHEN** the rider reaches the course of the leg from 45 to 12 and follows it towards 45
- **THEN** navigation continues with 12 as the next knot and, after riding on, tells the rider they ride the wrong way, as on the route

#### Scenario: Without courses
- **WHEN** the network's courses are not installed
- **THEN** navigation behaves as before, as it began at the nearest knot
