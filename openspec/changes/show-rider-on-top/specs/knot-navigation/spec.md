# Spec Delta

## MODIFIED Requirements

### Requirement: Riding direction on the map
While navigating, the rider's position SHALL be shown as an arrow pointing in the direction they are riding, taken from their most recent movement on the ridden track, not from the compass, while the map stays north-up. When the rider has not moved recently, the position SHALL be shown as a dot without a direction. The arrow SHALL be about 44 points long and the dot about 30 points across with its outline, so the position stands out at a glance.

#### Scenario: Riding
- **WHEN** the rider has ridden east for the last 20 m
- **THEN** their position is an arrow pointing east, and north is still up on the map

#### Scenario: Standing still
- **WHEN** the rider stops at a traffic light and has not moved 5 m for more than 10 s
- **THEN** their position is a dot without a direction

#### Scenario: Seeing the position at a glance
- **WHEN** the rider glances at the phone on the handlebar while navigating
- **THEN** their arrow is about 44 points long, larger than a knot's badge, and stands out from the route and the knots

### Requirement: Ridden track
While navigating, the map SHALL show the track the rider has ridden since navigation started, as a line distinguishable from the planned route, drawn on top of it and narrower than it, so the ridden part of the route shows and the route stays visible at its edges, made from the location fixes navigation already uses. Fixes too inaccurate to place the rider SHALL NOT be part of the track. The track SHALL exist only while navigating.

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

#### Scenario: Riding along the route
- **WHEN** the rider has ridden the first 2 km of the route as planned
- **THEN** those 2 km show the blue track on top of the route with the route's orange at both edges, and the rest of the route is orange with its direction arrows
