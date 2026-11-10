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

#### Scenario: Header of steady height on a guided way
- **WHEN** a way to the flag, or back to the route, is found or lost while the rider follows it, with a turn into a long street name ahead
- **THEN** the header stays one row of the same height: the turn's arrow, its distance with the street below it, cut short if long, the distance left and the next knot, and it doesn't push the map up

#### Scenario: The guided way on the map
- **WHEN** the rider follows the way to the flag on route, and the way found runs along the planned way except for a block where it takes another street
- **THEN** the map shows the guided way's line only along that other street, joined to the route at both ends, and not beside the route where they are the same; off route the whole guided way shows, as before

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

### Requirement: Way searched again from a precise position
A way to a route point or back to the route found from a position less precise than 20 m SHALL be searched again with the next position of 20 m or better, so a position that is off at the start does not decide the way.

#### Scenario: Starting beside the start flag
- **WHEN** the rider starts navigating at the start flag, whose planned way leaves southwards, the first position lies 30 m north of it with 30 m accuracy, and the way found from there runs east along another street
- **THEN** with the next position of 8 m accuracy at the flag the way is searched again, and the guided way follows the planned way south

#### Scenario: Precise from the start
- **WHEN** the first position after starting is 5 m accurate
- **THEN** the way found from it is kept, and not searched again for the next precise position

## MODIFIED Requirements

### Requirement: Turn notices on the guided way
On a guided way, the app SHALL say each turn once, about 100 m before it, e.g. "In 100 m, turn left". Turns within 40 m of each other SHALL count as one. Continuing straight on SHALL need no notice, and neither SHALL a bend where every other way at the junction turns at least 30° more than the way to take. Several turns at one junction that add up to straight on SHALL be said as straight on. When a way starts against the riding direction, the app SHALL say to turn back, and SHALL show it until the rider follows the way. When a way starts 60° or more to the side of the riding direction, with no turn of its own within 40 m, the app SHALL say that turn at once, e.g. "Turn left now", and, like turning back, not again while the rider rides on without it. The notice SHALL be spoken, also with the screen off and in the app's language.

#### Scenario: A turn ahead
- **WHEN** the rider is 100 m from a left turn on the guided way
- **THEN** the app says "In 100 m, turn left" once

#### Scenario: Turn at the start of the way
- **WHEN** the rider walks west along a street and a new way leaves it southwards right where they are
- **THEN** the app says "Turn left now", and the header shows that turn before the next one

#### Scenario: Turn close to the start of the way
- **WHEN** a new way has its first turn 40 m ahead
- **THEN** the app says "Turn left now", after the off-route notice

#### Scenario: Two turns together
- **WHEN** the way turns right and, 30 m later, left
- **THEN** the app gives one notice, using the overall direction of the two

#### Scenario: Straight on
- **WHEN** the way continues straight on over a junction, staying on the same street
- **THEN** the app says nothing there

#### Scenario: A bend without a choice
- **WHEN** the path bends right at a junction, and the only other way there turns left by more than 30° further
- **THEN** the app says nothing there

#### Scenario: A bend with a choice
- **WHEN** the way bends right at a junction, and another way there continues straighter
- **THEN** the app says the turn

#### Scenario: Crossing with an offset
- **WHEN** the way turns right and, 30 m later, left at a crossing, and continues in the same direction
- **THEN** the app says "In 100 m, go straight on"

#### Scenario: Way behind the rider
- **WHEN** a new way starts against the riding direction
- **THEN** the app says "Turn back now", and the header shows "Turn back" until the rider has followed the way for 15 m

#### Scenario: Still riding away
- **WHEN** the rider ignores it, and the next new way also starts against the riding direction
- **THEN** the header shows "Turn back" again, and the app doesn't say it again

#### Scenario: Screen off
- **WHEN** the rider reaches a turn notice with the screen off
- **THEN** it is spoken, without more location fixes than off route uses without a guided way


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
