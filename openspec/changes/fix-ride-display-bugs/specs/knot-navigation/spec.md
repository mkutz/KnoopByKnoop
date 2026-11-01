# Spec Delta

## MODIFIED Requirements

### Requirement: Following the rider
While navigating with the app open, the map SHALL follow the rider, keeping their position in view as they ride, until the user moves or zooms the map. Then the map SHALL stay where the user put it and a recenter button SHALL appear, which brings the map back to the rider at the navigation zoom and follows them again. The map SHALL NOT return to the rider on its own. While following, the base map's labels and icons SHALL stay steady.

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
- **THEN** the map shows their position and the next knot together

#### Scenario: Steady base map while following
- **WHEN** the map follows the rider past a supermarket and a parking with their icons on the base map
- **THEN** the icons and street names stay shown while they are in view, without disappearing and reappearing with every position

### Requirement: Number to follow at large text sizes
In every app language and at large system text sizes, the number to follow, the distance to the next knot and Stop SHALL be shown completely, and off route the next knot's badge. Where the distance does not fit in the line with the number to follow, it SHALL be shown in a line below it; where the badge does not fit in the line with the off-route text, it SHALL follow the text in the next line.

#### Scenario: Number to follow at the largest text size
- **WHEN** the system text size is at its largest and the rider is at 94 with 41 to follow, 467 m ahead
- **THEN** the panel shows "At 94 follow 41" and Stop completely, and "467 m" completely below them

#### Scenario: Number to follow at the default text size
- **WHEN** the system text size is the default
- **THEN** the distance stays in the line with the number to follow, as before

#### Scenario: Off route in German
- **WHEN** the app is in German and the rider is off route with 29 as the next knot
- **THEN** the panel shows "Abseits der Route, nächster" and the badge 29 completely, with Stop
