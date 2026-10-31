## ADDED Requirements

### Requirement: Export the route as GPX
The app SHALL export a complete planned route as a GPX 1.1 file and offer it in the system share sheet. The file SHALL contain the route's line along the knot network as one track, and each knot passed, in riding order, as a waypoint named with its number and typed as start, stop, end or knot. Export SHALL be offered only for a complete route whose line is known, not while navigating, and also offline.

#### Scenario: Export a route
- **WHEN** the user plans a cycling route from knot 12 through stop 40 to knot 34 and taps Export GPX
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
- **THEN** Export GPX is not offered

#### Scenario: No export while navigating
- **WHEN** the rider is navigating
- **THEN** Export GPX is not shown

#### Scenario: Export while offline
- **WHEN** the device is offline and a complete route with its line is planned
- **THEN** Export GPX opens the share sheet with the file

## MODIFIED Requirements

### Requirement: Clear actions in the planner panel
The planner panel SHALL show Navigate, and Stop while navigating, as the most prominent actions, each with an icon and its label. Back to start, Clear and Export GPX SHALL be shown as icon buttons that are announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** the header shows the distance, Back to start, Export GPX and Clear as icon buttons, and Navigate as a filled button with an icon and the label "Navigate", in one row

#### Scenario: Stop stands out
- **WHEN** the rider is navigating
- **THEN** the header shows Stop as a filled red button with an icon and the label "Stop"

#### Scenario: Icon buttons with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) moves through the header of a planned round trip
- **THEN** it announces "Back to start", "Export GPX", "Clear" and "Navigate" as buttons

#### Scenario: Name of an icon button on Android
- **WHEN** the user long-presses Clear on Android
- **THEN** the name "Clear" is shown

#### Scenario: Readable actions
- **WHEN** the actions are measured against the panel's background
- **THEN** the labels of Navigate and Stop have a contrast of at least 4.5:1 to their fill, and the icons of all five at least 3:1 to their background

#### Scenario: Readable actions in dark mode
- **WHEN** the actions are measured against the dark panel's background
- **THEN** the labels of Navigate and Stop have a contrast of at least 4.5:1 to their fill, and the icons of all five at least 3:1 to their background
