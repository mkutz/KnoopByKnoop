# Spec Delta

## ADDED Requirements

### Requirement: Ferries on the route
The route's knot list SHALL mark every ferry between the two knots it connects, and SHALL list the ferries of the route with their name, operating times and fee as far as they are known. A ferry without known operating times SHALL be listed as such, so the user knows to check it.

#### Scenario: Route over a ferry
- **WHEN** the route crosses a river by a ferry that runs "Apr-Oct: Mo-Su 07:00-19:00" for €1.50
- **THEN** the knot list shows a ferry between the two knots on both banks, and lists the ferry with those times and that fee

#### Scenario: Ferry without known times
- **WHEN** the route uses a ferry without operating times in OpenStreetMap
- **THEN** the ferry is listed with a note that its operating times are not known

#### Scenario: Several ferries
- **WHEN** the route uses two ferries
- **THEN** both are listed in the order the route reaches them

#### Scenario: Route without ferries
- **WHEN** the route uses no ferry
- **THEN** the knot list shows no ferries and no ferry list

#### Scenario: Offline
- **WHEN** the device has no internet access
- **THEN** the ferries and their details are shown as online
