## ADDED Requirements

### Requirement: Guidance to a route point off the network
When the next route point is joined to the network by a way along paths, navigation SHALL guide along it like along the route: while the way follows a course of the network, the notices SHALL name the knot that course leads to; where the way leaves the network, the rider SHALL be told the turn with a reminder before it; from there, they SHALL be told each turn of the way to the route point, as on the guided way back to the route.

#### Scenario: Towards the next knot
- **WHEN** the route runs 28, 02 and the end flag, whose way follows the course from 02 towards 06 before leaving the network
- **THEN** after 28 the rider hears "Next 02, then towards 06", and after 02 "Towards 06, then the end"

#### Scenario: Leaving the network
- **WHEN** the way to the end flag leaves the course of 02–06 by turning right into Cecilienstraße
- **THEN** the rider hears "In 100 m, turn right into Cecilienstraße" before it, and "Turn right now into Cecilienstraße" at it

#### Scenario: Turn by turn to the flag
- **WHEN** the rider turned into Cecilienstraße and the way to the flag turns left into Kronprinzenstraße after 300 m
- **THEN** the rider hears the turn as on the guided way back to the route, "In 100 m, turn left", and the header shows it with Kronprinzenstraße on the light tile of the way to the route

#### Scenario: Missing the turn-off
- **WHEN** the rider rides on along the course past Cecilienstraße
- **THEN** they are told a way to the flag from where they are, as off route

#### Scenario: A straight line
- **WHEN** the next stretch is a straight line to a stop on a hiking route
- **THEN** the rider is told the distance and direction to the stop, as before

#### Scenario: Without path data
- **WHEN** the path data for the way's squares is not installed
- **THEN** the rider is told the turn-off with the distance and direction to the flag after it, and no turns

### Requirement: Guidance from a route point onto the network
When a route point is joined to the network by a way along paths, navigation SHALL guide from the route point along that way like the guided way back to the route, telling each turn, and SHALL tell the turn onto the course where the way joins it, naming the knot the course leads to.

#### Scenario: From the start flag onto the network
- **WHEN** the rider starts navigating at the start flag at home, whose way along paths joins the course of 02–28 after 600 m, turning left there towards 02
- **THEN** the rider hears each turn of the way, and at the join "Turn left now onto the route towards 02", and then "Next 02" as usual

## MODIFIED Requirements

### Requirement: Riding the way to a route point
On a way along paths to a route point, the rider SHALL be guided with the line on the map and turn notices, as described in "Guidance to a route point off the network" and "Guidance from a route point onto the network", and leaving the way SHALL count as off route as on a course. On a straight line, which cannot be followed, the rider SHALL NOT be told they are off route, and SHALL be told the distance and direction to where it leads.

#### Scenario: Following a way along paths
- **WHEN** the rider rides along the way to a stop
- **THEN** they stay on route, and the distance to the stop counts down along the way

#### Scenario: Leaving a way along paths
- **WHEN** the rider moves more than the off route distance away from the way
- **THEN** they are off route, as on a course

#### Scenario: A straight line
- **WHEN** the next stretch is a straight line of 800 m to a stop, and the rider rides around a building 200 m from it
- **THEN** they are not told they are off route, and see the distance and direction to the stop

#### Scenario: Turning round at a detour
- **WHEN** the rider reaches a stop at the end of a way and turns back along it
- **THEN** the turn is not taken for riding the wrong way, and the next knot is named

#### Scenario: Without internet access
- **WHEN** the rider navigates a route with a way along paths to a route point, without internet access
- **THEN** the way and its guidance work as with internet access
