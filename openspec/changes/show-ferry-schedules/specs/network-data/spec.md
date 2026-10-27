# Spec Delta

## ADDED Requirements

### Requirement: Ferry details
For every connection that includes a ferry, the published graph SHALL carry the details OpenStreetMap has about the ferry: its name, operator, operating times, fee and website. Details missing in OpenStreetMap SHALL be left out, not guessed.

#### Scenario: Ferry with times and fee
- **WHEN** a ferry way of a connection has `name`, `opening_hours` and `charge` in OpenStreetMap
- **THEN** the graph carries that name, those operating times and that fee for the connection

#### Scenario: Details on the ferry relation
- **WHEN** a ferry's operating times are only tagged on its route relation, not on its way
- **THEN** the graph still carries them

#### Scenario: Ferry without details
- **WHEN** a ferry has no name, times or fee in OpenStreetMap
- **THEN** the connection is still marked as a ferry, without details

#### Scenario: Older graph files
- **WHEN** an app reads a graph file written before ferry details were published
- **THEN** it reads it as before, without ferry details
