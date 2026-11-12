## ADDED Requirements

### Requirement: Map buttons in groups
The buttons over the map SHALL stand in one column at the right edge, in groups that belong together: search, then position with nearest knots, then zoom in with zoom out. Each group SHALL share one background, Liquid Glass on iOS and a card on Android. In portrait the column SHALL stand on the planner panel's top edge and move with it. Where it does not fit, the zoom group SHALL be left out first, then the position group.

#### Scenario: Groups on the map
- **WHEN** a route is planned and the map shows no knots
- **THEN** the column shows search, then the position and nearest knots buttons on one background, then zoom in and zoom out on one background

#### Scenario: Moving with the panel
- **WHEN** the user pulls the panel up
- **THEN** the column moves up with it, staying on its top edge

#### Scenario: Above Navigate on Android
- **WHEN** a complete route is planned on Android in portrait
- **THEN** the column stands directly above the Navigate button and none of its buttons is covered by it

#### Scenario: Panel pulled far up
- **WHEN** the panel is pulled up so far that the column does not fit below the network switch
- **THEN** the zoom group is left out first, then the position group, and search stays

#### Scenario: Glass on iOS
- **WHEN** the map is shown on iOS 26
- **THEN** each group of buttons has the Liquid Glass background of the map's other floating controls

#### Scenario: Note on the panel's edge
- **WHEN** the note after a tap shows
- **THEN** it stands on the panel's top edge, the column stands on top of the note, on Android Navigate between the note and the column, and both drop back when the note goes

#### Scenario: Note on a small phone
- **WHEN** the note shows and the column no longer fits above it
- **THEN** the zoom group is left out while the note shows, and comes back when it goes

#### Scenario: Buttons with a screen reader
- **WHEN** a screen reader moves through the column
- **THEN** it announces each button by its name, as before, in the column's order

#### Scenario: Landscape
- **WHEN** the phone is in landscape
- **THEN** the column stands at the bottom right of the map, clear of the cutout and the navigation bar

### Requirement: Glass on the map
Everything that floats over the map SHALL share one glass look: the network switch, the info button, the button groups, the note after a tap, the card of a tapped spot or place, Navigate on Android and the notes over the map. On iOS it SHALL be Liquid Glass; on Android one slightly translucent surface with a soft border and shadow. Text and icons SHALL keep a contrast of at least 4.5:1 and 3:1.

#### Scenario: Note and card in glass
- **WHEN** the note after a tap or the card of a tapped place shows over the map
- **THEN** it has the same glass look as the map's buttons, on iOS Liquid Glass

#### Scenario: One look on Android
- **WHEN** the map shows the network switch, the info button, the button groups and a note
- **THEN** all of them have the same translucent surface, border and shadow

#### Scenario: Readable on glass
- **WHEN** the texts and icons over the map are measured against their glass background, over a light and a dark map
- **THEN** texts have a contrast of at least 4.5:1 and icons of at least 3:1

## MODIFIED Requirements

### Requirement: Zoom controls
The map SHALL offer buttons to zoom in and out, in addition to the gestures of the platform, together as one control: zoom in above zoom out, on one background, as in "Map buttons in groups".

#### Scenario: Zooming out
- **WHEN** the user taps the zoom-out button
- **THEN** the map zooms out by one level around its center

#### Scenario: Zoom buttons as one control
- **WHEN** the map is shown
- **THEN** zoom in and zoom out sit on one shared background, separated by a thin line, and a screen reader announces them as "Zoom in" and "Zoom out"

### Requirement: Show my position
While planning, the map SHALL offer a button, in one group with the nearest knots button above the zoom buttons, see "Map buttons in groups", that centers the map on the rider's current position, at least at the zoom where knots are shown, and marks it with a dot that does not follow the rider. Location access SHALL be asked for on the first tap, and on the very first start of the app, when there is neither a saved map position nor a route. The position SHALL be found once per request, without updates afterwards.

#### Scenario: Centering on my position
- **WHEN** location access is allowed and the user taps the position button
- **THEN** the map centers on the user's position with the knots visible, and a dot marks the position

#### Scenario: First tap asks
- **WHEN** location access was never asked for and the user taps the position button
- **THEN** the app asks for access while using the app, and centers on the position once it is allowed

#### Scenario: First start
- **WHEN** the app starts for the first time, with no saved map position and no route
- **THEN** it asks for location access and, if allowed, centers on the user's position

#### Scenario: Later starts
- **WHEN** the app starts with a saved map position or a route
- **THEN** it does not ask for location access and shows the saved map position or the route as before

#### Scenario: Access denied
- **WHEN** location access is denied and the user taps the position button
- **THEN** the map does not move and the app says that it needs location access to show the position, and where to allow it

#### Scenario: No position found
- **WHEN** no position is found within 15 seconds
- **THEN** the map does not move and the app says that the position could not be found

#### Scenario: No tracking while planning
- **WHEN** the map was centered on the position and the user moves on
- **THEN** the dot stays where it was, the app takes no further positions, and the system shows no location indicator after the position was found

#### Scenario: Not while navigating
- **WHEN** the rider is navigating
- **THEN** the position button is not shown, as the map follows the rider and offers its recenter button

### Requirement: Nearest knots
While planning, when the map shows no knots of the selected network, because there are none in its area or it is zoomed out too far for knots to show, the map SHALL offer a button below the position button, in the same group, that shows the nearest knots. With no waypoints set, the panel SHALL say that no knots are in view and offer the same action, instead of asking to tap a knot.

#### Scenario: No knots in the area
- **WHEN** the map shows an area without knots and no waypoints are set
- **THEN** the map shows the button, and the panel says "No knots in view" with a "Show the nearest knots" button

#### Scenario: Zoomed out too far
- **WHEN** the map is zoomed out so far that knots are not shown
- **THEN** the map shows the button

#### Scenario: Knots in view
- **WHEN** the map shows at least one knot
- **THEN** it does not show the button, and with no waypoints set, the panel says "Tap a knot to set the start"

#### Scenario: With waypoints set
- **WHEN** a start is set and the user moves the map to an area without knots
- **THEN** the map shows the button, and the panel still shows the route's state

#### Scenario: From the rider's position
- **WHEN** location access is allowed, the rider is 8 km from the nearest knot, and they tap the button
- **THEN** the map shows both their position, marked with a dot, and that knot, with the knots shown

#### Scenario: Clear of the controls
- **WHEN** the map moves to show the rider's position and the nearest knot
- **THEN** neither is hidden behind the search field, the network switch or the panel, and the button is gone, as a knot is in view

#### Scenario: Without location access
- **WHEN** location access was not allowed and the user taps the button
- **THEN** the app does not ask for it, and the map shows the knot nearest to the middle of the map

#### Scenario: No position found
- **WHEN** location access is allowed but no position is found within 15 seconds
- **THEN** the map shows the knot nearest to the middle of the map

#### Scenario: Far from the network
- **WHEN** the rider is so far from the nearest knot that the knots would not show with both in view
- **THEN** the map shows that knot, with the knots shown

#### Scenario: Other network
- **WHEN** hiking is selected and the nearest knot is sought
- **THEN** the map shows the nearest hiking knot, not a cycling one

#### Scenario: Not while navigating
- **WHEN** the rider is navigating
- **THEN** the button is not shown

#### Scenario: Nearest knots with a screen reader
- **WHEN** a screen reader reaches the button
- **THEN** it announces "Show the nearest knots" as a button

### Requirement: Clear actions in the planner panel
Navigate SHALL be the most prominent action, a filled round icon button with the navigation arrow and without a label: in the planner panel's header on iOS, and as a floating action button at the panel's top right edge on Android, moving with the panel. In landscape it SHALL be at the end of the side panel's header on both. While navigating, the header SHALL show Stop as a filled round icon button of the same size in every app language, announced as "Stop" to screen readers and showing its name on a long press on Android. Back to start, with an icon and its label, Export GPX and Clear, as icon buttons, SHALL be shown in one row in the panel below the route's knots, each only when available; there SHALL be no "More actions" menu. The header SHALL stay one row on phones 360 dp wide. Icon buttons SHALL be announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** Navigate shows as a filled round button with the navigation arrow, without a label, next to the distance on iOS and as a floating action button at the panel's top right on Android; below the knots, the panel shows Back to start with its label, if the route is not a round trip yet, then Export GPX and Clear as icon buttons, in one row, also 360 dp wide

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
- **WHEN** a screen reader (TalkBack, VoiceOver) moves through the panel of a planned route
- **THEN** it announces "Navigate", "Back to start", "Export GPX" and "Clear" as buttons

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

### Requirement: Changing what a tapped knot became
After a tapped knot, a tapped spot or a place was added to an existing route, the app SHALL show a short note: one line saying what it became, three buttons in the fixed order start, stop and end, each previewing that role with badges, and Undo. The button of the role it got SHALL show as selected and inactive. The buttons SHALL act as "Start here", "Add stop here" and "End here" do. A choice SHALL keep the note open with the chosen role selected, so a mistap can be put right, and start its 6 seconds again; Undo SHALL close it. The note SHALL go away after 6 seconds or with the next tap, but not by itself while a screen reader is on.

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
