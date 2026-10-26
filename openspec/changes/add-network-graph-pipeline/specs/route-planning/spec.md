# Spec Delta

## ADDED Requirements

### Requirement: Waypoints are knots, not numbers
Waypoints SHALL refer to a specific knot, not to its number, because several knots can share a number.

#### Scenario: Choose between knots with equal numbers
- **WHEN** knot 30 exists on both banks of a river, connected only by a ferry, and the user sets the one on the near bank as end
- **THEN** the route ends on the near bank and does not take the ferry
