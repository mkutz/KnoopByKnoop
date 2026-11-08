## ADDED Requirements

### Requirement: Off route confirmed by a precise position
The rider SHALL be told they are off route because of their distance to the course only after a position accurate to 20 m or better confirms it, more than 100 m from the course. A position less accurate than that SHALL make the app sample precisely, without a notice. A position not newer than the last one used SHALL be ignored. Passing a knot that is not on the route SHALL still count at once.

#### Scenario: A coarse position far from the course
- **WHEN** the rider rides on the course with the screen off and a position accurate to 40 m lies 200 m from the course
- **THEN** the app samples precisely and says nothing, and when the next position accurate to 10 m is on the course, the rider stays on route

#### Scenario: Leaving the route for real
- **WHEN** the rider leaves the course and a position accurate to 10 m lies 150 m from it
- **THEN** the app says "Off route" with the next knot, as before

#### Scenario: The same position delivered again
- **WHEN** the phone delivers a position again that navigation has already used, or an older one
- **THEN** navigation ignores it, and the sampling does not change because of it

#### Scenario: Passing a knot not on the route
- **WHEN** the rider passes knot 77, which is not on the route
- **THEN** the app says "Off route" at once, as before
