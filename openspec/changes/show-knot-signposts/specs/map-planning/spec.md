# Spec Delta

## ADDED Requirements

### Requirement: Knots signposted at several places
A knot signposted at several places of one junction (see `network-data` "One knot per signposted junction") SHALL have its badge on the map at every one of them. The badges SHALL act as that one knot for tapping and highlighting; everywhere else the knot SHALL appear once.

#### Scenario: Badges at both signposts
- **WHEN** knot 02 is signposted at two places 195 m apart and the map shows them
- **THEN** a badge 02 is shown at each of the two places

#### Scenario: Tapping the other signpost
- **WHEN** no start is set and the user taps the badge 02 that is not the knot's main position
- **THEN** 02 becomes the start, the same knot as when tapping the other badge, and the waypoint list, the strip and the coming knots show 02 once

#### Scenario: Route through a knot with two signposts
- **WHEN** the route passes 02
- **THEN** both badges 02 are highlighted as part of the route

#### Scenario: Next knot with two signposts
- **WHEN** the rider is navigating with 02 as the next knot
- **THEN** both badges 02 have the next-knot ring

#### Scenario: Knots with equal numbers on both banks
- **WHEN** two knots 30 on both banks of a river are separate knots, connected by a ferry
- **THEN** each has its own badge, and tapping one selects only that knot, as before
