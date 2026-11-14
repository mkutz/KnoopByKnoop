# Spec Delta

## ADDED Requirements

### Requirement: Map buttons for screen readers
A screen reader (TalkBack, VoiceOver) SHALL reach every button on the map while it shows: search, My position, the nearest knots, zoom in, zoom out and Recenter. Each SHALL be read with its label in the app's language.

#### Scenario: Map buttons with VoiceOver
- **WHEN** VoiceOver moves through the planner of a planned route on the iPhone
- **THEN** it reaches "Search places", "My position", "Zoom in" and "Zoom out", each read with that label

#### Scenario: Map buttons with TalkBack
- **WHEN** TalkBack moves through the planner of a planned route on Android
- **THEN** it reaches "Search places", "My position", "Zoom in" and "Zoom out", each read with that label
