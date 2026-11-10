## MODIFIED Requirements

### Requirement: No way found
When the installed squares don't connect two places, the router SHALL report that there is no way, instead of a partial way.

#### Scenario: Squares missing in between
- **WHEN** the squares between the rider and the target aren't installed
- **THEN** no way is found

#### Scenario: Not enough memory
- **WHEN** a search runs out of memory on Android
- **THEN** no way is found, the loaded squares are freed, and the app keeps running with the straight line

#### Scenario: Broken path data
- **WHEN** a square read during a search can't be unpacked, while navigating or while finding the way of a route point, on Android or iOS
- **THEN** no way is found, the loaded squares are freed, and the app keeps running with the straight line
