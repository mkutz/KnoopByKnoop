# Spec Delta

## MODIFIED Requirements

### Requirement: Keep the networks current
The app SHALL check the published index for newer data every time it starts, and SHALL install newer data in the background without the user doing anything. Coming back to the app from the background SHALL NOT count as a start.

#### Scenario: Newer data available
- **WHEN** the index lists a newer data date for an installed network
- **THEN** the newer files are downloaded and installed in the background, and planning on the installed data keeps working until they are installed

#### Scenario: Route across an update
- **WHEN** a route is being planned while an update of its network is installed
- **THEN** the route is kept, and a waypoint whose knot no longer exists is removed and pointed out to the user

#### Scenario: Data published since the last start
- **WHEN** newer data was published an hour after the app last checked the index, and the user starts the app again
- **THEN** the app checks the index at this start and installs the newer data, without waiting a day

#### Scenario: Nothing new
- **WHEN** the app starts and the index lists the installed data dates
- **THEN** only the index is downloaded, and no network file
