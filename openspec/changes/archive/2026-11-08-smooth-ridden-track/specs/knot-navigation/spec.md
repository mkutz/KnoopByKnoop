# Spec Delta

## MODIFIED Requirements

### Requirement: Ridden track
While navigating, the map SHALL show the track the rider has ridden since navigation started, as a line distinguishable from the planned route, drawn on top of it and narrower than it, so the ridden part of the route shows and the route stays visible at its edges, made from the location fixes navigation already uses. Fixes too inaccurate to place the rider SHALL NOT be part of the track. A fix SHALL be part of the track only when it is farther from the track's last point than the inaccuracies of both together, and at least 5 m. A fix that would mean riding faster than 15 m/s on a cycling route, or 4 m/s on a hiking route, since the track's last point SHALL NOT be part of the track, and a point the next fix comes straight back from SHALL be taken out of it. The track SHALL exist only while navigating.

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

#### Scenario: Standing with scattered fixes
- **WHEN** a hiker stands still for two minutes, with fixes accurate to 20 m that scatter up to 20 m around them
- **THEN** the track gets no new point

#### Scenario: Scatter while walking slowly
- **WHEN** a hiker walks 200 m straight east at 1.3 m/s, with fixes every 10 s accurate to 30 m that scatter up to 30 m to either side
- **THEN** the track's points are at least 60 m apart, and none of them lies west of the one before

#### Scenario: Impossible jump
- **WHEN** on a hiking route a fix lies 120 m from the track's last point, 10 s after it
- **THEN** it is not added to the ridden track

#### Scenario: Spike out and back
- **WHEN** on a cycling route a fix lands 80 m north of the track while riding east, and the next fix is back on the line further east
- **THEN** the track has no point 80 m north

#### Scenario: Out and back on the same path
- **WHEN** a cyclist rides 100 m off the route and back on the same path at 5 m/s, with a fix every 10 s
- **THEN** the ridden track shows the way out to its far end and back

#### Scenario: Riding fast with good fixes
- **WHEN** a cyclist rides at 8 m/s with fixes every second accurate to 5 m
- **THEN** the track has a point at least every 16 m along the way
