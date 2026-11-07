## MODIFIED Requirements

### Requirement: Clear actions in the planner panel
The planner panel SHALL show Navigate as the most prominent action, with an icon and its label. While navigating, it SHALL show Stop as a filled icon button of the same size in every app language, announced as "Stop" to screen readers and showing its name on a long press on Android. Clear SHALL be shown as an icon button, and Back to start and Export GPX as entries with icon and label in a menu opened by a "More actions" icon button, shown when one of its entries is available; the header SHALL stay one row on phones 360 dp wide. Icon buttons SHALL be announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** the header shows the distance, More actions and Clear as icon buttons, and Navigate as a filled button with an icon and the label "Navigate", in one row, also 360 dp wide; More actions opens a menu with Back to start, if the route is not a round trip yet, and Export GPX

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
- **WHEN** a screen reader (TalkBack, VoiceOver) moves through the header of a planned round trip
- **THEN** it announces "More actions", "Clear" and "Navigate" as buttons, and in the opened menu "Back to start" and "Export GPX"

#### Scenario: Name of an icon button on Android
- **WHEN** the user long-presses Clear on Android
- **THEN** the name "Clear" is shown

#### Scenario: Readable actions
- **WHEN** the actions are measured against the panel's background
- **THEN** the label of Navigate has a contrast of at least 4.5:1 to its fill, the icon of Stop at least 3:1 to its fill, and the icons of the icon buttons and of the menu entries at least 3:1 to their background

#### Scenario: Readable actions in dark mode
- **WHEN** the actions are measured against the dark panel's background
- **THEN** the label of Navigate has a contrast of at least 4.5:1 to its fill, the icon of Stop at least 3:1 to its fill, and the icons of the icon buttons and of the menu entries at least 3:1 to their background
