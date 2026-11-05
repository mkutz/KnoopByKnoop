# Spec Delta

## ADDED Requirements

### Requirement: Route used in both directions
The route line SHALL be drawn beside its course, to the right in riding direction, so a stretch the route uses in both directions shows as two lines next to each other, each with its own arrows.

#### Scenario: A stretch used twice
- **WHEN** a route uses the same stretch in both directions
- **THEN** the map shows two lines next to each other there, with arrows pointing the opposite ways

#### Scenario: A stretch used once
- **WHEN** a route uses a stretch in one direction
- **THEN** the map shows one line there, slightly to the right of the course

#### Scenario: Tapping the route when zoomed out
- **WHEN** the map is zoomed out and the user taps the line beside the course
- **THEN** the map zooms to show the whole route, as before
