# Spec Delta

## ADDED Requirements

### Requirement: Map, knots and courses one after the other
On start, the app SHALL show the map right away, then the knots and connections of the selected knot network as soon as its graph is read, and then the courses of the connections once its geometry is read. Planning SHALL be possible as soon as the knots are shown.

#### Scenario: Normal start
- **WHEN** the knot networks and their geometry are installed and the user starts the app
- **THEN** the map is shown first, then the knots with straight connections, then the connections along their course, and no screen without a map is shown in between

#### Scenario: Planning before the courses are shown
- **WHEN** the knots are shown and the courses are not yet
- **THEN** tapping knots plans a route, which is drawn along its course as soon as the courses are shown

#### Scenario: Courses do not move the map
- **WHEN** the courses replace the straight connections
- **THEN** the map keeps its position and zoom, and the route stays as planned

### Requirement: Startup times
In a release build, the map SHALL be shown within 1 second after the app was started, and the knots of the selected knot network within 2 seconds, also on a first start without internet access. The times SHALL be measured from the tap on the app icon on the reference devices named in the design.

#### Scenario: Normal start on the reference devices
- **WHEN** the release build is started with installed knot networks and geometry
- **THEN** the map is shown within 1 second and the knots within 2 seconds

#### Scenario: First start without internet access on the reference devices
- **WHEN** the release build is installed and started for the first time in airplane mode
- **THEN** the map is shown within 1 second and the knots of the bundled graph within 2 seconds
