## ADDED Requirements

### Requirement: Courses loaded after the start
When navigation was started before the network's courses had loaded, it SHALL take them as soon as they are loaded, without stopping or restarting, and from then on SHALL navigate along them as if it had started with them. Knots already passed SHALL stay passed and SHALL NOT be announced again. The next knot SHALL stay the same unless the rider is already past it along the course. Taking the courses SHALL NOT be spoken. When no courses are installed, navigation SHALL go on without them.

#### Scenario: Courses arriving on the course
- **WHEN** the rider started navigation before the courses had loaded, rides on the course between 45 and 46, and the courses finish loading
- **THEN** the next knot is still 46, the distance to it is measured along the course to its signpost, and nothing is spoken for the change

#### Scenario: Knots passed before the courses
- **WHEN** the rider passed 45 and 46 before the courses had loaded, and they then finish loading
- **THEN** 45 and 46 stay passed, the next knot is 47, and neither is announced again

#### Scenario: Off route when the courses arrive
- **WHEN** the courses finish loading while the rider is 300 m from the course of the leg to the next knot
- **THEN** on the next position the rider is off route, with the usual notice and the line or guided way back

#### Scenario: Courses on a slow phone
- **WHEN** the rider presses Navigate right after starting the app, before the courses have loaded, and rides on
- **THEN** a few seconds after the courses have loaded, the map draws the course ahead and leaving the course is told as off route
