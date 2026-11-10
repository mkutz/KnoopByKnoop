# Spec Delta

## MODIFIED Requirements

### Requirement: Following the rider
While navigating with the app open, the map SHALL follow the rider, keeping their position in view as they ride, until the user moves or zooms the map. Then the map SHALL stay where the user put it and a recenter button SHALL appear, which brings the map back to the rider at the navigation zoom and follows them again. The map SHALL NOT return to the rider on its own. While following, on the route and off it, the base map's labels and icons SHALL stay steady.

#### Scenario: Riding with the map following
- **WHEN** the rider rides along the route and has not touched the map since navigation started
- **THEN** the map moves with them, and their position stays in view

#### Scenario: Looking around
- **WHEN** the rider drags or zooms the map while navigating
- **THEN** the map stops following and stays where they put it, and the recenter button appears

#### Scenario: Recentering
- **WHEN** the rider taps the recenter button
- **THEN** the map moves to their position at the navigation zoom, follows them again, and the button disappears

#### Scenario: No automatic return
- **WHEN** the rider moved the map and does not touch it for several minutes while riding on
- **THEN** the map stays where they put it, and the recenter button stays

#### Scenario: Off route while following
- **WHEN** the rider leaves the route while the map follows them
- **THEN** the map shows their position and the end of the line to the route together, as described in "Way to the route on the map"

#### Scenario: Steady base map while following
- **WHEN** the map follows the rider past a supermarket and a parking with their icons on the base map
- **THEN** the icons and street names stay shown while they are in view, without disappearing and reappearing with every position

#### Scenario: Steady base map off route
- **WHEN** the map follows a rider who is off route or on the way to the first knot, showing them and the end of the line
- **THEN** the icons and street names stay shown while they are in view, as while following along the route
