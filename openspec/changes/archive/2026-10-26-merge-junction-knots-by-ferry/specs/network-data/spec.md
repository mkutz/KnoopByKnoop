# Spec Delta

## MODIFIED Requirements

### Requirement: One knot per signposted junction
Nodes of the same network with the same number that belong to one junction SHALL become one knot. Knots with the same number that are separate places SHALL stay separate knots. Only a ferry between two nodes makes them separate places.

#### Scenario: Junction tagged on several nodes
- **WHEN** knot 30 is tagged on two nodes within 500 m of each other, e.g. on both sides of a dual carriageway or around a roundabout, and no ferry lies between them
- **THEN** they become one knot 30, and routes arriving at either node meet there

#### Scenario: Connector inside a junction
- **WHEN** a short route of its own connects two nodes tagged 30 of the same junction, without a ferry
- **THEN** they still become one knot 30

#### Scenario: Equal numbers on both banks of a river
- **WHEN** knot 30 is tagged on both banks of a river and a route including a ferry connects the two
- **THEN** they stay two knots 30, connected by that route, so riders can choose which one to go to
