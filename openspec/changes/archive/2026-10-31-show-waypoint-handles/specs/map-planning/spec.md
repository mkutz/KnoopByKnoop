# Spec Delta

## MODIFIED Requirements

### Requirement: Edit the waypoint list
The app SHALL list the waypoints in order. The user SHALL be able to move any waypoint to another position and to remove any waypoint; removing one of the last two SHALL leave the other as the start. Each row SHALL show a drag handle for moving and a remove button, so neither depends on a hidden gesture; screen readers SHALL offer moving up, moving down and removing as actions on the row.

#### Scenario: Reorder
- **WHEN** the waypoints are 12, 30, 7, 45 and the user moves 7 above 30
- **THEN** the waypoints are 12, 7, 30, 45 and the route on the map follows them

#### Scenario: Remove
- **WHEN** the waypoints are 12, 30, 45 and the user removes 30
- **THEN** the waypoints are 12, 45

#### Scenario: Start over
- **WHEN** the user clears the route
- **THEN** all waypoints are removed and the next tap sets a new start

#### Scenario: Visible handle
- **WHEN** the planner lists the waypoints 12, 30 and 45
- **THEN** each row shows a drag handle at its end

#### Scenario: Moving by the handle
- **WHEN** the user drags the handle of 7 above 30, without a long press first
- **THEN** the waypoints are 12, 7, 30, 45

#### Scenario: Long press still moves
- **WHEN** the user long-presses the row of 7 and drags it above 30
- **THEN** the waypoints are 12, 7, 30, 45

#### Scenario: Visible remove button
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** each row shows a remove button, and tapping the one of 30 leaves 12, 45

#### Scenario: Removing one of two waypoints
- **WHEN** only the waypoints 12 and 45 are left and the user removes 45
- **THEN** 12 is left as the start, and the next tap sets the end

#### Scenario: Reordering with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) is on the row of 30, the second of 12, 30, 45
- **THEN** it offers the actions "Move up", "Move down" and "Remove", and "Move down" makes the waypoints 12, 45, 30
