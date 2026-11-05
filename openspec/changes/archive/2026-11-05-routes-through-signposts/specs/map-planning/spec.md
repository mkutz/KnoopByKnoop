# Spec Delta

## ADDED Requirements

### Requirement: Routes through a knot with several signposts
A route passing a knot signposted at several places SHALL be drawn and exported through the signpost that fits the legs before and after it, the shortest way where several fit. Where the legs share no signpost, the line SHALL join them along the path between the signposts. The route's knots, distance and time SHALL NOT change.

#### Scenario: Through the signpost of the next leg
- **WHEN** the route goes 06 → 02 → 28, 02 → 28 leaves at the signpost near the L 16, and 06 → 02 reaches both signposts
- **THEN** its line goes straight to the signpost near the L 16 and on to 28, without the 02 by the river

#### Scenario: The other direction out of the knot
- **WHEN** the route goes 06 → 02 → 01, and 02 → 01 leaves at the 02 by the river
- **THEN** its line goes to the 02 by the river and on to 01, without the signpost near the L 16

#### Scenario: A stop at the knot
- **WHEN** 02 is a stop of the route 06 → 02 → 28
- **THEN** its line is the same as when the route only passes 02

#### Scenario: Back the same way
- **WHEN** the route goes 28 → 02 → 28, and 28 reaches 02 only at the signpost near the L 16
- **THEN** its line turns at that signpost, without the path to the 02 by the river and back

#### Scenario: No shared signpost
- **WHEN** the route goes 28 → 02 → 01, arriving at one signpost and leaving at the other
- **THEN** its line joins them along the path between the two badges

#### Scenario: Same distance
- **WHEN** the line of a route goes through another signpost than before
- **THEN** the route's distance, time and knots are the same
