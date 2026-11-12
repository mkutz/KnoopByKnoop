## ADDED Requirements

### Requirement: Undo route edits
Undo in the panel SHALL take back the last edit of the route being planned, and each further Undo the edit before, back to the oldest edit kept; there SHALL be no redo. An edit SHALL be every change the user makes to the waypoints while planning: a tapped knot, spot or place added, with the note's choices for it counting as part of that tap, "Start here", "Add stop here" and "End here", a waypoint removed or moved, Back to start, and Clear. Changes the app makes by itself, such as the courses coming in or a waypoint dropped after a data update, SHALL NOT be edits, and Undo SHALL keep what they brought. At least the last 50 edits SHALL be kept, also when the app is closed and opened again. Undo SHALL be shown only when there is an edit to take back and not while navigating. Switching the network SHALL discard the edits with the route. A knot that no longer exists SHALL be left out when an edit is taken back, as on a restart.

#### Scenario: Undo a tap
- **WHEN** the route is 12 → 45 and the user taps 30, which becomes a stop, and then chooses Undo
- **THEN** the route is 12 → 45 again

#### Scenario: Several steps back
- **WHEN** the user planned 12, then 45, then tapped 30, then removed 12, and chooses Undo three times
- **THEN** the waypoints are 12, 30, 45 after the first Undo, 12, 45 after the second, and only the start 12 after the third

#### Scenario: Undo a removal
- **WHEN** the waypoints are 12, 30, 45 and the user removes 30, then chooses Undo
- **THEN** the waypoints are 12, 30, 45

#### Scenario: Undo a move
- **WHEN** the waypoints are 12, 30, 7, 45 and the user moves 7 above 30, then chooses Undo
- **THEN** the waypoints are 12, 30, 7, 45

#### Scenario: Undo Back to start
- **WHEN** the route 12 → 45 became a round trip with Back to start and the user chooses Undo
- **THEN** the route is 12 → 45 again

#### Scenario: Undo Clear
- **WHEN** the user clears the route 12 → 30 → 45 and chooses Undo
- **THEN** the route 12 → 30 → 45 is back

#### Scenario: Nothing to undo
- **WHEN** the app was installed fresh and nothing was planned yet, or every kept edit was taken back
- **THEN** no Undo is shown

#### Scenario: Undo after a restart
- **WHEN** the user planned 12 → 45, tapped 30, closed the app, opened it again and chooses Undo
- **THEN** the route is 12 → 45

#### Scenario: Courses are not an edit
- **WHEN** the user tapped 45 after 12 and the courses came in afterwards, changing the route's line, and the user chooses Undo
- **THEN** only 12 is left, and the line of what is left keeps the courses

#### Scenario: No undo while navigating
- **WHEN** the rider is navigating
- **THEN** no Undo is shown

#### Scenario: Network switch discards the edits
- **WHEN** the user switches from cycling to hiking with a route and confirms
- **THEN** no Undo is shown on hiking, and none for the cycling route when switching back

#### Scenario: Knot gone when undoing
- **WHEN** an edit kept from before a data update brings back a waypoint whose knot no longer exists
- **THEN** that waypoint is left out and the user is told which one, as on a restart

#### Scenario: Undo with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches Undo
- **THEN** it announces "Undo" as a button

## MODIFIED Requirements

### Requirement: Changing what a tapped knot became
After a tapped knot, a tapped spot or a place was added to an existing route, the app SHALL show a short note: one line saying what it became, three buttons in the fixed order start, stop and end, each previewing that role with badges; it SHALL have no Undo of its own, as Undo in the panel takes the tap back (see "Undo route edits"). The button of the role it got SHALL show as selected and inactive. The buttons SHALL act as "Start here", "Add stop here" and "End here" do. A choice SHALL keep the note open with the chosen role selected, so a mistap can be put right, and start its 6 seconds again; Undo in the panel SHALL close it. The note SHALL go away after 6 seconds, with the next tap, or at once when the user chooses Navigate, since planning tools are not needed while the route is ridden, but not by itself while a screen reader is on.

#### Scenario: Note after a tap
- **WHEN** a route 12 → 14 → 15 exists and the user taps 27, which becomes a stop between 14 and 15
- **THEN** the note says "27 added as a stop" and shows the buttons 27 before 12 for the start, 14, 27 and 15 for the stop, selected, and 15 before 27 for the end, and no Undo of its own

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
- **THEN** the note stays, says "30 added as the start" with the start button selected, and goes away 6 seconds later unless the user chooses again, chooses Undo in the panel or taps the map

#### Scenario: Undo the tap
- **WHEN** 30 was added as a stop to 12 → 45, made the start in the note, and the user chooses Undo in the panel
- **THEN** the waypoints are 12 and 45 again and the note is gone, since the note's choices belong to the tap

#### Scenario: Note goes away
- **WHEN** the note is shown and the user does nothing for 6 seconds, or taps another knot
- **THEN** the note is gone, and for the other knot a new note is shown

#### Scenario: Note with a screen reader
- **WHEN** a screen reader is on and 27 was added as a stop between 14 and 15
- **THEN** it reads the line "27 added as a stop", the buttons as "As the start", "As a stop, selected" and "As the end", and the note stays until Undo in the panel or the next tap

#### Scenario: No note for the first two taps
- **WHEN** the user taps the start and then the end of a new route
- **THEN** no note is shown

#### Scenario: Navigate closes the note
- **WHEN** the note says "30 added as a stop" and the user chooses Navigate
- **THEN** the note goes away at once, before the position is found

### Requirement: Clear actions in the planner panel
Navigate SHALL be the most prominent action, a filled round icon button with the navigation arrow and without a label: in the planner panel's header on iOS, and as a floating action button at the panel's top right edge on Android, moving with the panel. In landscape it SHALL be at the end of the side panel's header on both. While navigating, the header SHALL show Stop as a filled round icon button of the same size in every app language, announced as "Stop" to screen readers and showing its name on a long press on Android. Back to start, with an icon and its label, Export GPX, Clear and Undo, as icon buttons, SHALL be shown in one row in the panel below the route's knots, each only when available; there SHALL be no "More actions" menu. The header SHALL stay one row on phones 360 dp wide. Icon buttons SHALL be announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** Navigate shows as a filled round button with the navigation arrow, without a label, next to the distance on iOS and as a floating action button at the panel's top right on Android; below the knots, the panel shows Back to start with its label, if the route is not a round trip yet, then Export GPX, Clear and, after an edit, Undo as icon buttons, in one row, also 360 dp wide

#### Scenario: Stop stands out
- **WHEN** the rider is navigating
- **THEN** the header shows Stop as a filled red icon button with the stop icon, without a label

#### Scenario: Stop in every language
- **WHEN** the rider navigates with the app in English and then in German
- **THEN** Stop has the same size and place in the header

#### Scenario: Stop with a screen reader
- **WHEN** a screen reader reaches Stop while navigating
- **THEN** it announces "Stop" as a button

#### Scenario: Icon buttons with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) moves through the panel of a route planned by tapping
- **THEN** it announces "Navigate", "Back to start", "Export GPX", "Clear" and "Undo" as buttons

#### Scenario: Name of an icon button on Android
- **WHEN** the user long-presses Clear on Android
- **THEN** the name "Clear" is shown

#### Scenario: Readable actions
- **WHEN** the actions are measured against the panel's background
- **THEN** the icons of Navigate and Stop have a contrast of at least 3:1 to their fill, the label of Back to start at least 4.5:1, and the icons of the icon buttons at least 3:1 to their background

#### Scenario: Readable actions in dark mode
- **WHEN** the actions are measured against the dark panel's background
- **THEN** the icons of Navigate and Stop have a contrast of at least 3:1 to their fill, the label of Back to start at least 4.5:1, and the icons of the icon buttons at least 3:1 to their background

#### Scenario: Navigate in landscape
- **WHEN** a complete route is planned in landscape
- **THEN** Navigate is the round icon button at the end of the side panel's header, on Android and iOS

### Requirement: Panel before a route
With fewer than two waypoints, the panel SHALL show "Plan a ride", a start and an end slot with their labels, one hint line, and buttons "Search a place" and "Nearest knots". An empty slot SHALL look empty; the slot the next tap fills SHALL stand out. A set waypoint SHALL fill its slot with its badge and its label SHALL name its place. With a waypoint set, Clear SHALL replace "Nearest knots". After an edit, Undo SHALL follow them, also with no waypoint left, so a cleared route can be brought back.

#### Scenario: Nothing planned
- **WHEN** no waypoint is set and knots are in view
- **THEN** the panel shows "Plan a ride", two empty slots labelled "Start" and "End", the start slot standing out, "Tap a numbered knot on the map for the start.", and the buttons "Search a place" and "Nearest knots"

#### Scenario: Start set
- **WHEN** the user taps 48 in De Rips with nothing planned
- **THEN** the start slot shows the badge 48, labelled "Start · De Rips", the end slot stands out, the hint says "Now tap a knot for the end.", and the buttons are "Search a place" and Clear

#### Scenario: End chosen first
- **WHEN** an end was chosen before any start
- **THEN** the end slot shows its badge, the start slot stands out, and the hint says "Now tap a knot for the start."

#### Scenario: Route point in a slot
- **WHEN** the start is a route point from a tapped spot
- **THEN** the start slot shows the flag badge of route points

#### Scenario: Next slot without motion
- **WHEN** the phone is set to reduce motion or to remove animations
- **THEN** the slot that stands out does so without pulsing

#### Scenario: Search from the panel
- **WHEN** the user chooses "Search a place" in the panel
- **THEN** the place search opens as from the map's search button

#### Scenario: Nearest knots from the panel
- **WHEN** the user chooses "Nearest knots" in the panel
- **THEN** the map shows the nearest knots as the map's nearest knots button does

#### Scenario: Into the route
- **WHEN** the start is set and the user taps a knot for the end
- **THEN** the slots give way to the route's distance, time, knots and actions

#### Scenario: Slots with a screen reader
- **WHEN** a screen reader reaches the slots with only the start 48 set
- **THEN** it reads "Start, 48, De Rips" and "End, not set yet"

#### Scenario: Empty panel at large text sizes
- **WHEN** the largest text size is set and nothing is planned
- **THEN** the title, labels, hint and buttons are fully readable, and the buttons wrap to a second line rather than being cut

#### Scenario: Undo in the empty panel
- **WHEN** the user cleared the route 12 → 45
- **THEN** the panel shows "Plan a ride" with the buttons "Search a place", "Nearest knots" and Undo, and Undo brings back 12 → 45
