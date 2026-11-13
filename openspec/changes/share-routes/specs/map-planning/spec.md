## MODIFIED Requirements

### Requirement: Clear actions in the planner panel
Navigate SHALL be the most prominent action, a filled round icon button with the navigation arrow and without a label: in the planner panel's header on iOS, and as a floating action button at the panel's top right edge on Android, moving with the panel. In landscape it SHALL be at the end of the side panel's header on both. While navigating, the header SHALL show Stop as a filled round icon button of the same size in every app language, announced as "Stop" to screen readers and showing its name on a long press on Android. Back to start, with an icon and its label, Share, Clear and Undo, as icon buttons, SHALL be shown in one row in the panel below the route's knots, each only when available, with the download symbols at the row's end (see `network-sync` "Base map ready for the ride"); there SHALL be no "More actions" menu. Where the row does not fit, Back to start SHALL show its icon only, still named "Back to start" to screen readers and on a long press on Android. The header SHALL stay one row on phones 360 dp wide. Icon buttons SHALL be announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** Navigate shows as a filled round button with the navigation arrow, without a label, next to the distance on iOS and as a floating action button at the panel's top right on Android; below the knots, the panel shows Back to start with its label, if the route is not a round trip yet, then Share, Clear and, after an edit, Undo as icon buttons, and the download symbols at the end, in one row, also 360 dp wide

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
- **THEN** it announces "Navigate", "Back to start", "Share", "Clear" and "Undo" as buttons

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

#### Scenario: Back to start on a narrow phone
- **WHEN** a complete cycling route that is not a round trip is planned on a phone 360 dp wide, after an edit
- **THEN** the row shows Back to start as an icon only, Share, Clear, Undo and the map and network symbols in one row, and a screen reader announces "Back to start" as a button

### Requirement: Export the route as GPX
The app SHALL export a complete planned route as a GPX 1.1 file and offer it in the system share sheet, through the choice "GPX for other apps" of Share in the panel (see `route-sharing` "Share the route"). The file SHALL contain the route's line along the knot network as one track, and each knot passed, in riding order, as a waypoint named with its number and typed as start, stop, end or knot. Export SHALL be offered only for a complete route whose line is known, not while navigating, and also offline.

#### Scenario: Export a route
- **WHEN** the user plans a cycling route from knot 12 through stop 40 to knot 34 and chooses Share and "GPX for other apps"
- **THEN** the share sheet opens with a file `knoop-by-knoop-cycling-12-34.gpx`

#### Scenario: Track along the network
- **WHEN** the exported file of a route is read
- **THEN** it is valid GPX 1.1 with one track whose points follow the route's line along the network from the start knot to the end knot

#### Scenario: Knots as waypoints
- **WHEN** the exported file of a route from 12 through 40 to 34 is read
- **THEN** it has a waypoint for every knot passed, in riding order, named with its number, and 12 is typed as start, 40 as stop and 34 as end

#### Scenario: Round trip
- **WHEN** a round trip from knot 12 back to 12 is exported
- **THEN** the first waypoint is 12 typed as start, the last is 12 typed as end, and the track starts and ends at knot 12

#### Scenario: No export without a complete route
- **WHEN** only a start is set, or the route's line is not loaded yet
- **THEN** "GPX for other apps" is not offered

#### Scenario: No export while navigating
- **WHEN** the rider is navigating
- **THEN** neither Share nor "GPX for other apps" is shown

#### Scenario: Export while offline
- **WHEN** the device is offline and a complete route with its line is planned
- **THEN** "GPX for other apps" opens the share sheet with the file

### Requirement: Undo route edits
Undo in the panel SHALL take back the last edit of the route being planned, and each further Undo the edit before, back to the oldest edit kept; there SHALL be no redo. An edit SHALL be every change the user makes to the waypoints while planning: a tapped knot, spot or place added, with the note's choices for it counting as part of that tap, "Start here", "Add stop here" and "End here", a waypoint removed or moved, Back to start, Clear, and a route file opened on the same network (see `route-sharing` "Open a route file"). Changes the app makes by itself, such as the courses coming in or a waypoint dropped after a data update, SHALL NOT be edits, and Undo SHALL keep what they brought. At least the last 50 edits SHALL be kept, also when the app is closed and opened again. Undo SHALL be shown only when there is an edit to take back and not while navigating. Switching the network, also by opening a route file of the other network, SHALL discard the edits with the route. A knot that no longer exists SHALL be left out when an edit is taken back, as on a restart.

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

#### Scenario: Opened route on the other network
- **WHEN** the user planned a cycling route by tapping and then opens a hiking route file
- **THEN** the hiking route is shown and no Undo is shown
