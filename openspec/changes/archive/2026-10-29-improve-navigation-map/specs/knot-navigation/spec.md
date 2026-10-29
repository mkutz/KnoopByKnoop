# Spec Delta

## ADDED Requirements

### Requirement: Following the rider
While navigating with the app open, the map SHALL follow the rider, keeping their position in view as they ride, until the user moves or zooms the map. Then the map SHALL stay where the user put it and a recenter button SHALL appear, which brings the map back to the rider at the navigation zoom and follows them again. The map SHALL NOT return to the rider on its own.

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

### Requirement: Riding direction on the map
While navigating, the rider's position SHALL be shown as an arrow pointing in the direction they are riding, taken from their most recent movement on the ridden track, not from the compass, while the map stays north-up. When the rider has not moved recently, the position SHALL be shown as a dot without a direction.

#### Scenario: Riding
- **WHEN** the rider has ridden east for the last 20 m
- **THEN** their position is an arrow pointing east, and north is still up on the map

#### Scenario: Standing still
- **WHEN** the rider stops at a traffic light and has not moved 5 m for more than 10 s
- **THEN** their position is a dot without a direction

### Requirement: Ridden track
While navigating, the map SHALL show the track the rider has ridden since navigation started, as a line distinguishable from the planned route, made from the location fixes navigation already uses. Fixes too inaccurate to place the rider SHALL NOT be part of the track. The track SHALL exist only while navigating.

#### Scenario: Detour
- **WHEN** the rider leaves the route for a detour and comes back to it
- **THEN** the map shows the detour as part of the ridden track, next to the planned route

#### Scenario: Inaccurate fix
- **WHEN** a fix is less accurate than 50 m
- **THEN** it is not added to the ridden track

#### Scenario: Navigation ends
- **WHEN** navigation ends by Stop or by arriving
- **THEN** the ridden track is gone from the map

#### Scenario: Screen off
- **WHEN** the rider navigates with the screen off and opens the app again
- **THEN** the ridden track includes the stretch ridden with the screen off, from the fixes taken then, without the location being sampled more often than before
