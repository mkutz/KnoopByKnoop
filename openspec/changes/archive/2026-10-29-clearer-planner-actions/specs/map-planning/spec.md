# Spec Delta

## ADDED Requirements

### Requirement: Clear actions in the planner panel
The planner panel SHALL show Navigate, and Stop while navigating, as the most prominent actions, each with an icon and its label. Back to start and Clear SHALL be shown as icon buttons that are announced by name to screen readers. The labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** the header shows the distance, Back to start and Clear as icon buttons, and Navigate as a filled button with an icon and the label "Navigate", in one row

#### Scenario: Stop stands out
- **WHEN** the rider is navigating
- **THEN** the header shows Stop as a filled red button with an icon and the label "Stop"

#### Scenario: Icon buttons with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) moves through the header of a planned round trip
- **THEN** it announces "Back to start", "Clear" and "Navigate" as buttons

#### Scenario: Name of an icon button on Android
- **WHEN** the user long-presses Clear on Android
- **THEN** the name "Clear" is shown

#### Scenario: Readable actions
- **WHEN** the actions are measured against the panel's background
- **THEN** the labels of Navigate and Stop have a contrast of at least 4.5:1 to their fill, and the icons of all four at least 3:1 to their background
