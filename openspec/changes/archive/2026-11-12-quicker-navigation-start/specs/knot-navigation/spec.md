## MODIFIED Requirements

### Requirement: Start on the route nearby
When navigation is started more than 30 m from the start and a place on the course before the first stop, more than 200 m along it from the start, is less than half as far as the start, the app SHALL ask once whether to ride to the start or to start on the route nearby, each with its straight distance. Starting nearby SHALL make that place the start of the route for this navigation only, as if the plan started there, see "The plan after navigation". The position SHALL be one at most 60 s old and accurate to 100 m, else a new one of that accuracy; while it is sought, Navigate SHALL show that it is working.

#### Scenario: Asked near the course
- **WHEN** the route runs 06 → 02 → 28 with 02 a stop, and the rider starts navigation 250 m from the course of 06 → 02 and 4.4 km from 06
- **THEN** the app asks "Ride to 06 · 4.4 km" or "Start on the route nearby · 250 m"

#### Scenario: Riding to the start
- **WHEN** the rider is asked and chooses to ride to 06
- **THEN** the plan stays as it was, and navigation leads to 06

#### Scenario: Starting nearby
- **WHEN** the rider is asked and chooses to start on the route nearby
- **THEN** the navigated route starts at a temporary start at the nearest place on the course of 06 → 02, as "Start here" on a course places it, without 06, with 02 and 28. Navigation leads to that point, and the plan still starts at 06

#### Scenario: Nearby at a knot
- **WHEN** the nearest place on the course is within 30 m of 05, a knot of the route between 06 and 02, and the rider starts nearby
- **THEN** the navigated route starts at 05, without a route point

#### Scenario: On the course past the start
- **WHEN** the rider starts navigation on the course 2 km after 06
- **THEN** the app asks. Starting nearby starts the navigated route where the rider is, and navigation begins with the next knot after that place

#### Scenario: At the start
- **WHEN** the rider starts navigation within 30 m of 06
- **THEN** the app does not ask and navigation begins at 06

#### Scenario: Rough position at the start
- **WHEN** the position is 70 m from 06 and accurate to 80 m
- **THEN** the app does not ask and navigation begins at 06

#### Scenario: Close to the start along the course
- **WHEN** the nearest place on the course is 150 m along it from 06
- **THEN** the app does not ask and navigation leads to 06

#### Scenario: Course not much closer
- **WHEN** the rider starts navigation 1.5 km from the course of 06 → 02 and 2 km from 06
- **THEN** the app does not ask and navigation leads to 06

#### Scenario: Only before the first stop
- **WHEN** the rider starts navigation 100 m from the course of 02 → 28 and 5 km from the course of 06 → 02 and from 06
- **THEN** the app does not ask and navigation leads to 06, so no stop is dropped

#### Scenario: Without courses
- **WHEN** the network's courses are not installed, and the rider starts navigation 300 m from 05, a knot of the route before 02, and 4 km from 06
- **THEN** the app asks, and starting nearby makes 05 the start of the navigated route

#### Scenario: Recent position
- **WHEN** the phone found a position accurate to 40 m 20 seconds before Navigate is pressed
- **THEN** the app decides with that position without waiting for a new one

#### Scenario: Position too old or too rough
- **WHEN** the phone's last position is 5 minutes old, or accurate only to 500 m
- **THEN** the app asks the phone for a new position accurate to about 100 m

#### Scenario: Waiting for the position
- **WHEN** the rider presses Navigate and the position is not known yet
- **THEN** Navigate at once shows a spinner instead of its arrow, keeps its size, ignores further taps, and a screen reader says "Finding your position"

#### Scenario: Position unknown
- **WHEN** the rider starts navigation and no position is known within 10 seconds
- **THEN** the app does not ask and navigation leads to the start

#### Scenario: Question dismissed
- **WHEN** the rider dismisses the question without choosing
- **THEN** navigation does not start, the plan stays as it was, and the location is no longer used

#### Scenario: The plan after the ride
- **WHEN** the rider started nearby, and then stops navigation or arrives
- **THEN** the planner shows the plan from 06 again, as it was before Navigate
