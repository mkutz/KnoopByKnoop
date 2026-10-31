# Spec Delta

## ADDED Requirements

### Requirement: Show my position
While planning, the map SHALL offer a round button below the zoom buttons that centers the map on the rider's current position, at least at the zoom where knots are shown, and marks it with a dot that does not follow the rider. Location access SHALL be asked for on the first tap, and on the very first start of the app, when there is neither a saved map position nor a route. The position SHALL be found once per request, without updates afterwards.

#### Scenario: Centering on my position
- **WHEN** location access is allowed and the user taps the position button
- **THEN** the map centers on the user's position with the knots visible, and a dot marks the position

#### Scenario: First tap asks
- **WHEN** location access was never asked for and the user taps the position button
- **THEN** the app asks for access while using the app, and centers on the position once it is allowed

#### Scenario: First start
- **WHEN** the app starts for the first time, with no saved map position and no route
- **THEN** it asks for location access and, if allowed, centers on the user's position

#### Scenario: Later starts
- **WHEN** the app starts with a saved map position or a route
- **THEN** it does not ask for location access and shows the saved map position or the route as before

#### Scenario: Access denied
- **WHEN** location access is denied and the user taps the position button
- **THEN** the map does not move and the app says that it needs location access to show the position, and where to allow it

#### Scenario: No position found
- **WHEN** no position is found within 15 seconds
- **THEN** the map does not move and the app says that the position could not be found

#### Scenario: No tracking while planning
- **WHEN** the map was centered on the position and the user moves on
- **THEN** the dot stays where it was, the app takes no further positions, and the system shows no location indicator after the position was found

#### Scenario: Not while navigating
- **WHEN** the rider is navigating
- **THEN** the position button is not shown, as the map follows the rider and offers its recenter button
