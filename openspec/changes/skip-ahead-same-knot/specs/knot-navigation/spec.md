## ADDED Requirements

### Requirement: A knot the route visits twice
After the rider passes a knot that the route visits again later, coming back within its circle SHALL count as that later visit only after the rider has been more than 200 m away from the knot since passing it.

#### Scenario: Riding around at the knot
- **WHEN** the route runs 27, 83, 15, …, 14, 83, 30, the rider passes 83 heading for 15, and comes within 30 m of 83 again before getting 200 m away from it
- **THEN** 15 stays the next knot

#### Scenario: Coming back after the loop
- **WHEN** the rider rode on from 83 towards 15, left the loop more than 200 m from 83, and returns to 83 without passing 15, 14 and the knots between
- **THEN** navigation continues from the second 83 with 30 as the next knot, as when skipping ahead
