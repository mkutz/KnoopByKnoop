# Spec Delta

## ADDED Requirements

### Requirement: Leg ahead on the map
While navigating, the map SHALL show the course to the next knot thick and with arrows, and the rest of the route thin and without arrows. When a knot is passed, the course to the new next knot SHALL take its place. The ridden track SHALL stay on top of the route. Starting on a stretch the route uses in both directions, navigation SHALL begin with its first use.

#### Scenario: Which way at a fork
- **WHEN** the rider nears a fork where the route also comes back on the other branch, with 28 as the next knot
- **THEN** only the course to 28 is thick and has arrows, so the map shows which way to go now

#### Scenario: Passing a knot
- **WHEN** the rider passes 28 with 27 as the new next knot
- **THEN** the course to 27 is thick and has arrows, and the course to 28 is thin

#### Scenario: Before the first knot
- **WHEN** the rider is off route before the first knot of the route
- **THEN** no course stands out, the route is thin, and the dashed line leads to the first knot

#### Scenario: Off route
- **WHEN** the rider is off route with 28 as the next knot
- **THEN** the course to 28 stays thick with its arrows

#### Scenario: Starting on a stretch used twice
- **WHEN** the rider starts navigating the round trip 02, 28, 27, 28, 02 on the course between 02 and 28, not at a knot
- **THEN** 28 is the next knot, not 02 on the way back, even if the way back's course is a few metres nearer

#### Scenario: Ending navigation
- **WHEN** navigation ends
- **THEN** the route is drawn as while planning, with arrows along all of it
