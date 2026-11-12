## ADDED Requirements

### Requirement: Changing what a tapped knot became
After a tapped knot was added to an existing route, the app SHALL show a short note: one line saying what it became, three buttons in the fixed order start, stop and end, each previewing that role with badges, and Undo. The button of the role it got SHALL show as selected and inactive. The buttons SHALL act as "Start here", "Add stop here" and "End here" do. The note SHALL go away after 6 seconds, on a choice or with the next tap, but not by itself while a screen reader is on.

#### Scenario: Note after a tap
- **WHEN** a route 12 → 14 → 15 exists and the user taps 27, which becomes a stop between 14 and 15
- **THEN** the note says "27 added as a stop" and shows the buttons 27 before 12 for the start, 14, 27 and 15 for the stop, selected, and 15 before 27 for the end, then Undo

#### Scenario: Note for a new start
- **WHEN** a tapped knot 60 became the new start before 05
- **THEN** the note says "60 added as the start", and the start button shows 60 before 05 and is selected

#### Scenario: Note for a new end
- **WHEN** a tapped knot 22 became the new end after 15
- **THEN** the note says "22 added as the end", the end button shows 15 before 22 and is selected, and the stop button shows 22 with the knots around it where it would be a stop

#### Scenario: Make it the start
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the start button
- **THEN** the waypoints are 30, 12 as a stop, and 45

#### Scenario: Make it the end
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the end button
- **THEN** the waypoints are 12, 45 as a stop, and 30

#### Scenario: Make it a stop
- **WHEN** 28 was added as the new end of 12 → 45 and the user chooses the stop button
- **THEN** 28 is a stop where it adds the least distance, and 45 is the end again

#### Scenario: Undo the tap
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses Undo
- **THEN** the waypoints are 12 and 45 again

#### Scenario: Note goes away
- **WHEN** the note is shown and the user does nothing for 6 seconds, or taps another knot
- **THEN** the note is gone, and for the other knot a new note is shown

#### Scenario: Note with a screen reader
- **WHEN** a screen reader is on and 27 was added as a stop between 14 and 15
- **THEN** it reads the line "27 added as a stop", the buttons as "As the start", "As a stop, selected" and "As the end", and the note stays until a choice is used or the next tap

#### Scenario: No note for the first two taps
- **WHEN** the user taps the start and then the end of a new route
- **THEN** no note is shown

## MODIFIED Requirements

### Requirement: Plan by tapping knots
Tapping a knot SHALL make it the start if there is no start yet, the end if there is no end yet, and otherwise add it where it adds the least distance: as a stop in one of the legs, before the start, making it the new start and the old start a stop, or after the end, making it the new end and the old end a stop. A round trip SHALL stay one, so its knots are only added as stops. A knot that cannot be reached from the route SHALL be added before the end, so the route reports the leg as unreachable. Tapping a knot that already is a waypoint SHALL change nothing, so an accidental double tap does not create a round trip and the start does not turn into a stop; round trips are made with the action to return to the start. A tap SHALL select the knot nearest to the tapped position, within a tolerance that keeps knots easy to hit.

#### Scenario: Start and end
- **WHEN** the user taps knot 12 and then knot 45
- **THEN** 12 is the start, 45 the end, and the route between them is shown on the map

#### Scenario: Adding a stop
- **WHEN** a route from 12 to 45 exists and the user taps knot 30, which lies beside it
- **THEN** 30 is added as a stop where it adds the least distance, and the route passes through it

#### Scenario: Beyond the end
- **WHEN** a route from 12 to 45 exists and the user taps knot 28, which lies beyond 45 seen from 12
- **THEN** the waypoints are 12, 45 and 28, with 28 as the end and 45 as a stop

#### Scenario: Before the start
- **WHEN** a route from 05 in Amsterdam to 76 in Zandvoort exists and the user taps 60, which lies on the far side of 05 seen from 76
- **THEN** the waypoints are 60, 05 and 76, with 60 as the start and 05 as a stop

#### Scenario: Extending step by step
- **WHEN** the user taps 12, 45, 28 and 7, each further along in the same direction
- **THEN** the waypoints are 12, 45, 28 and 7 in that order

#### Scenario: Knot beyond the end of a round trip
- **WHEN** the waypoints are 12, 45 and 12 and the user taps knot 28
- **THEN** 28 becomes a stop where it adds the least distance, neither the new start nor the new end, and the route stays a round trip from 12

#### Scenario: Tapping a knot that cannot be reached
- **WHEN** a route from 12 to 45 exists and the user taps a knot that cannot be reached from it
- **THEN** it is added before 45, and the route reports the leg as unreachable

#### Scenario: Tapping the last waypoint again
- **WHEN** only the start 12 is set and the user taps 12 again
- **THEN** 12 stays the only waypoint, and the next tapped knot becomes the end

#### Scenario: Tapping the start of a route again
- **WHEN** a route from 12 to 45 exists and the user taps 12 again
- **THEN** the waypoints stay 12 and 45

#### Scenario: Tapping next to the network
- **WHEN** the user taps where no knot is within the tolerance
- **THEN** the route does not change

#### Scenario: Knots with equal numbers
- **WHEN** two knots 30 are close to each other, e.g. on both banks of a river, and the user taps one of them
- **THEN** exactly the tapped knot becomes part of the route

### Requirement: What a chosen route point does
"Start here" SHALL make the place the new start, "End here" the new end, and "Add stop here" a stop in the leg where it adds the least distance. A waypoint that was the start or end before SHALL become a stop, whether it is a knot or a route point.

#### Scenario: First waypoint
- **WHEN** there are no waypoints and the user chooses "Start here", then taps 28
- **THEN** the route goes from the point to 28

#### Scenario: End before the start
- **WHEN** there are no waypoints and the user chooses "End here", then taps 28
- **THEN** 28 is the start and the point the end

#### Scenario: Before an existing start
- **WHEN** the route is 02 → 28 and the user chooses "Start here"
- **THEN** the waypoints are the point, 02 as a stop, and 28

#### Scenario: After an existing end
- **WHEN** the route is 28 → 02 and the user chooses "End here"
- **THEN** the waypoints are 28, 02 as a stop, and the point

#### Scenario: An earlier route point
- **WHEN** the start is a route point and the user chooses "Start here" somewhere else
- **THEN** the new place is the start and the earlier point a stop, which the user can remove

#### Scenario: A round trip stops being one
- **WHEN** the route is a round trip from 12 and the user chooses "End here"
- **THEN** the waypoints keep 12 as start and as a stop, and the point is the end

## REMOVED Requirements

### Requirement: What new knots become
**Reason**: A tapped knot now goes where it adds the least distance, the place after the end included, and a note right after the tap changes it, see "Changing what a tapped knot became". The maintainer wants one way to add to the route instead of a switch next to the place choices.
**Migration**: A remembered "At end" choice is ignored. A knot beyond the end becomes the new end by itself; for others, use "Make it the end" in the note.
