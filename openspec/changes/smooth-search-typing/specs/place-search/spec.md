## ADDED Requirements

### Requirement: Typing without delay
Each character typed into the place search SHALL show at once. On iOS, typing a character SHALL NOT hold up the app for more than 50 ms on an iPhone 18 Pro, and SHALL NOT draw the map or the waypoints again.

#### Scenario: Typing a place's name
- **WHEN** the user types "Amsterdam" into the search on the iPhone, one character after the other
- **THEN** each character shows as it is typed, and the app's timing log shows no stall over 50 ms between the keystrokes

#### Scenario: The map while typing
- **WHEN** the user types into the search
- **THEN** the map and the waypoint list below the search are not drawn again for the characters
