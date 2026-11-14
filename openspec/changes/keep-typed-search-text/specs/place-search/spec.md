# Spec Delta

## ADDED Requirements

### Requirement: Typed text is kept
The search field SHALL keep every character the user types, however fast they come, also while hits for an earlier part of the text arrive. The field's text SHALL only be replaced by the app when the user clears it, picks a hit, or reopens the search with text left from before.

#### Scenario: Typing fast
- **WHEN** the user types "Blue Lotus Nieuw-Lekkerland" faster than the screen updates
- **THEN** the field shows "Blue Lotus Nieuw-Lekkerland", and the search looks for that text

#### Scenario: Earlier text echoed late
- **WHEN** the field shows "Blu" and the app then reports the query "Bl" it stored for an earlier keystroke
- **THEN** the field keeps "Blu"

#### Scenario: Cleared from the app
- **WHEN** the field shows "Blue" and the user taps Clear
- **THEN** the field is empty
