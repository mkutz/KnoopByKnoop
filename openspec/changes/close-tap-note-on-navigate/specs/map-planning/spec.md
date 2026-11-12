## MODIFIED Requirements

### Requirement: Changing what a tapped knot became
After a tapped knot, a tapped spot or a place was added to an existing route, the app SHALL show a short note: one line saying what it became, three buttons in the fixed order start, stop and end, each previewing that role with badges, and Undo. The button of the role it got SHALL show as selected and inactive. The buttons SHALL act as "Start here", "Add stop here" and "End here" do. A choice SHALL keep the note open with the chosen role selected, so a mistap can be put right, and start its 6 seconds again; Undo SHALL close it. The note SHALL go away after 6 seconds, with the next tap, or at once when the user chooses Navigate, since planning tools are not needed while the route is ridden, but not by itself while a screen reader is on.

#### Scenario: Note after a tap
- **WHEN** a route 12 → 14 → 15 exists and the user taps 27, which becomes a stop between 14 and 15
- **THEN** the note says "27 added as a stop" and shows the buttons 27 before 12 for the start, 14, 27 and 15 for the stop, selected, and 15 before 27 for the end, then Undo

#### Scenario: Note for a new start
- **WHEN** a tapped knot 60 became the new start before 05
- **THEN** the note says "60 added as the start", and the start button shows 60 before 05 and is selected

#### Scenario: Note for a new end
- **WHEN** a tapped knot 22 became the new end after 15
- **THEN** the note says "22 added as the end", the end button shows 15 before 22 and is selected, and the stop button shows 22 with the knots around it where it would be a stop

#### Scenario: Note for a place
- **WHEN** a route 25 → 32 exists and the user adds "Eiscafé Riviera", which becomes a stop
- **THEN** the note says "Eiscafé Riviera added as a stop", and the buttons preview the café with its icon

#### Scenario: Note for a spot
- **WHEN** a route 25 → 32 exists and the user adds an empty spot beyond 32 with "Add to route"
- **THEN** the note says "Added as the end", and the buttons preview the spot with the flag

#### Scenario: Make it the start
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the start button
- **THEN** the waypoints are 30, 12 as a stop, and 45

#### Scenario: Make it the end
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the end button
- **THEN** the waypoints are 12, 45 as a stop, and 30

#### Scenario: Make it a stop
- **WHEN** 28 was added as the new end of 12 → 45 and the user chooses the stop button
- **THEN** 28 is a stop where it adds the least distance, and 45 is the end again

#### Scenario: Note stays after a choice
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the start button
- **THEN** the note stays, says "30 added as the start" with the start button selected, and goes away 6 seconds later unless the user chooses again, chooses Undo or taps the map

#### Scenario: Undo the tap
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses Undo
- **THEN** the waypoints are 12 and 45 again

#### Scenario: Note goes away
- **WHEN** the note is shown and the user does nothing for 6 seconds, or taps another knot
- **THEN** the note is gone, and for the other knot a new note is shown

#### Scenario: Note with a screen reader
- **WHEN** a screen reader is on and 27 was added as a stop between 14 and 15
- **THEN** it reads the line "27 added as a stop", the buttons as "As the start", "As a stop, selected" and "As the end", and the note stays until Undo or the next tap

#### Scenario: No note for the first two taps
- **WHEN** the user taps the start and then the end of a new route
- **THEN** no note is shown

#### Scenario: Navigate closes the note
- **WHEN** the note says "30 added as a stop" and the user chooses Navigate
- **THEN** the note goes away at once, before the position is found
