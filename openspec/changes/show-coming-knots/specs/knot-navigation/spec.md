# Spec Delta

## MODIFIED Requirements

### Requirement: Panel height while navigating
In portrait, when navigation starts, the panel SHALL take the height of its navigation part: the number to follow and the progress strip with its distances and arrivals, whatever height it had while planning. While navigating, it SHALL not be lower than that, and the rider SHALL be able to drag it higher to see the coming knots. When navigation ends, it SHALL go back to its smallest planning height.

#### Scenario: Starting with the smallest panel
- **WHEN** the panel is at its smallest height in portrait and the rider starts navigating a route with the stop 30 at 5.2 km and the end 31 at 12.4 km
- **THEN** the panel shows the number to follow and the whole strip, with "5.2 km" and the arrival below 30 and "12 km" and the arrival below 31, without being dragged

#### Scenario: Starting with a high panel
- **WHEN** the panel is at half the screen in portrait and the rider starts navigating
- **THEN** the panel moves down to the height of the number to follow and the strip

#### Scenario: Dragging the panel
- **WHEN** the rider drags the panel down while navigating in portrait
- **THEN** the panel stops at the height of the number to follow and the strip with its distances and arrivals

#### Scenario: Dragging the panel up
- **WHEN** the rider drags the panel up while navigating in portrait
- **THEN** the panel follows and shows the coming knots below the strip

#### Scenario: Ending navigation
- **WHEN** navigation ends by Stop or by arriving
- **THEN** the panel goes back to its smallest planning height

#### Scenario: Navigating in landscape
- **WHEN** the rider navigates in landscape
- **THEN** the side panel shows the number to follow, the whole strip and the coming knots below it

## ADDED Requirements

### Requirement: Coming knots
While navigating, the panel SHALL list the coming knots of the route below the progress strip, in riding order from the next knot to the end: each with its badge as on the map, the distance to it along the route and the arrival, at the pace of "Ferry arrival estimate", following each position while shown. Ferries SHALL be listed between the knots they connect, with their arrival state as on the strip. Passed knots SHALL NOT be listed.

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

### Requirement: Showing a coming knot on the map
Tapping a knot in the list of coming knots SHALL show that knot on the map, with the panel back at the height of the number to follow and the strip in portrait, so the knot is not hidden by it. The map SHALL then stop following the rider, as when the rider moves the map, and the recenter button SHALL bring it back to the rider.

#### Scenario: Tapping a coming knot
- **WHEN** the rider has dragged the panel up in portrait and taps 12 in the list while the map follows them
- **THEN** the panel goes down to the height of the number to follow and the strip, the map shows 12 at the navigation zoom, stops following the rider, and the recenter button appears

#### Scenario: Back to the rider
- **WHEN** the rider taps the recenter button after that
- **THEN** the map moves to the rider at the navigation zoom and follows them again
