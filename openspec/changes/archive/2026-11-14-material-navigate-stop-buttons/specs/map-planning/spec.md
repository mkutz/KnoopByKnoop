## MODIFIED Requirements

### Requirement: Clear actions in the planner panel
Navigate SHALL be the most prominent action, filled, with the navigation arrow and without a label: a round icon button in the planner panel's header on iOS, and on Android in portrait a Material floating action button, a rounded square of 56 dp, at the panel's top right edge, moving with the panel. In landscape it SHALL be a round icon button at the end of the side panel's header on both. While navigating, Stop SHALL take Navigate's place, shape and size, filled red with the stop icon: in the header on iOS and in landscape, and as the floating action button on Android in portrait, where the header then shows no Stop. Stop SHALL have the same size in every app language, and SHALL be announced as "Stop" to screen readers and show its name on a long press on Android. Back to start, with an icon and its label, Share, Clear and Undo, as icon buttons, SHALL be shown in one row in the panel below the route's knots, each only when available, with the download symbols at the row's end (see `network-sync` "Base map ready for the ride"); there SHALL be no "More actions" menu. Where the row does not fit, Back to start SHALL show its icon only, still named "Back to start" to screen readers and on a long press on Android. The header SHALL stay one row on phones 360 dp wide. Icon buttons SHALL be announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** Navigate shows as a filled button with the navigation arrow, without a label: round next to the distance on iOS, and on Android a floating action button, a rounded square, at the panel's top right; below the knots, the panel shows Back to start with its label, if the route is not a round trip yet, then Share, Clear and, after an edit, Undo as icon buttons, and the download symbols at the end, in one row, also 360 dp wide

#### Scenario: Stop stands out
- **WHEN** the rider is navigating
- **THEN** Stop shows as a filled red button with the stop icon, without a label, in Navigate's place: round at the end of the header on iOS, and on Android the floating action button at the panel's top right edge, in Navigate's shape and size, while the header row shows no Stop

#### Scenario: Stop in every language
- **WHEN** the rider navigates with the app in English and then in German
- **THEN** Stop has the same size and place

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

#### Scenario: Stop in landscape on Android
- **WHEN** the rider is navigating on Android in landscape
- **THEN** Stop is the round red icon button at the end of the side panel's header, as on iOS

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

#### Scenario: Above Stop on Android
- **WHEN** the rider is navigating on Android in portrait
- **THEN** the column stands directly above the Stop button, as above Navigate while planning, and none of its buttons is covered by it

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
