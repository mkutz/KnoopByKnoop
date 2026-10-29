# Spec Delta

## MODIFIED Requirements

### Requirement: Arriving and stopping
Passing the last knot of the route SHALL be announced with "You've arrived" and SHALL end navigation. The user SHALL be able to stop navigation at any time in the app. The app SHALL use the location only while navigating, so the system's location indicator shows only then; ending navigation SHALL stop using it. On iOS, closing the app while navigating SHALL end navigation.

#### Scenario: Arriving
- **WHEN** the rider passes 31, the end of the route
- **THEN** the app says "You've arrived", navigation ends, and the notification or Live Activity goes away

#### Scenario: Stopping in the app
- **WHEN** the rider taps Stop in the app while navigating
- **THEN** navigation ends and the planned route is still there

#### Scenario: No location use after navigation
- **WHEN** navigation ended by arriving, by Stop, or because the app was closed or ended while navigating, and the app is started again or left in the background
- **THEN** the app does not use the location, and the system shows no location indicator for it

#### Scenario: Closing the app while navigating
- **WHEN** the rider closes the app on iOS while navigating, by swiping it away in the app switcher
- **THEN** navigation ends, the location indicator and the Live Activity go away right then, and the planned route is still there at the next start
