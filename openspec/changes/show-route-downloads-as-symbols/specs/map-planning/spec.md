## MODIFIED Requirements

### Requirement: Clear actions in the planner panel
Navigate SHALL be the most prominent action, a filled round icon button with the navigation arrow and without a label: in the planner panel's header on iOS, and as a floating action button at the panel's top right edge on Android, moving with the panel. In landscape it SHALL be at the end of the side panel's header on both. While navigating, the header SHALL show Stop as a filled round icon button of the same size in every app language, announced as "Stop" to screen readers and showing its name on a long press on Android. Back to start, with an icon and its label, Export GPX, Clear and Undo, as icon buttons, SHALL be shown in one row in the panel below the route's knots, each only when available, with the download symbols at the row's end (see `network-sync` "Base map ready for the ride"); there SHALL be no "More actions" menu. Where the row does not fit, Back to start SHALL show its icon only, still named "Back to start" to screen readers and on a long press on Android. The header SHALL stay one row on phones 360 dp wide. Icon buttons SHALL be announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** Navigate shows as a filled round button with the navigation arrow, without a label, next to the distance on iOS and as a floating action button at the panel's top right on Android; below the knots, the panel shows Back to start with its label, if the route is not a round trip yet, then Export GPX, Clear and, after an edit, Undo as icon buttons, and the download symbols at the end, in one row, also 360 dp wide

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

#### Scenario: Back to start on a narrow phone
- **WHEN** a complete cycling route that is not a round trip is planned on a phone 360 dp wide, after an edit
- **THEN** the row shows Back to start as an icon only, Export GPX, Clear, Undo and the map and network symbols in one row, and a screen reader announces "Back to start" as a button
