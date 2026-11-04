# Spec Delta

## ADDED Requirements

### Requirement: The routing service
The published index SHALL say which routing service the apps use for points away from the network, so it can be replaced or turned off without a release. Indexes without it SHALL mean that there is no service. The info sheet SHALL name the service, what it receives, and its data's attribution.

#### Scenario: Another service address
- **WHEN** a new index gives another address for the service
- **THEN** the apps use it after they have read that index, without an update

#### Scenario: An index without a service
- **WHEN** the installed index, or one written by an earlier pipeline, names no service
- **THEN** the apps work as with the service turned off, and the networks install as before

#### Scenario: In the info sheet
- **WHEN** the user opens the info sheet
- **THEN** it names the routing service, says that a chosen point's position is sent to it, and shows its attribution
