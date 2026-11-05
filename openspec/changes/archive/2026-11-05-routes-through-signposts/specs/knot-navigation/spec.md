# Spec Delta

## ADDED Requirements

### Requirement: Riding the line through a signpost
While navigating, a rider following any course of a connection to a signpost of its knot SHALL NOT be told they are off route for it, and passing that signpost SHALL count as passing the knot.

#### Scenario: Following the line shown
- **WHEN** the rider on 06 → 02 → 28 follows the branch to the signpost near the L 16, which at some point is more than 100 m from the connection's main course
- **THEN** they stay on route, and passing that signpost makes 28 the next knot

#### Scenario: The other branch
- **WHEN** the rider on the same route takes the branch to the 02 by the river instead
- **THEN** they stay on route, as this is a course of the connection too

#### Scenario: Away from all courses
- **WHEN** the rider is more than 100 m from every course of the current leg and its neighbors
- **THEN** they are off route, as before
