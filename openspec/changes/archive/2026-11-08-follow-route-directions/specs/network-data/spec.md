## ADDED Requirements

### Requirement: Course in each riding direction
Where a node network route marks its ways for one direction only, the data SHALL give each connection the course of each riding direction: a way marked `forward` SHALL be used only in its own direction, a way marked `backward` only against it, and an unmarked way in both. Where the course back is not the course there reversed, the data SHALL include it.

#### Scenario: A cycle path on each side
- **WHEN** the route from 28 to 02 at the Sieg bridge in Siegburg-Zange is mapped with a `forward` way on one side of the road and a `backward` way on the other
- **THEN** the course from 28 to 02 runs on the `forward` way, and the course from 02 to 28 on the `backward` way

#### Scenario: One-way stretch
- **WHEN** a route runs through a one-way street as a `forward` member and the other direction is mapped through a parallel street as a `backward` member
- **THEN** each direction's course uses its own street

#### Scenario: No marks
- **WHEN** no way of a route is marked for one direction
- **THEN** the connection has one course, used reversed for the other direction, as before

#### Scenario: Marks that leave no way
- **WHEN** a route's ways are all marked `forward` against the direction from its first knot to its second
- **THEN** the connection still has a course in that direction, the other direction's course reversed

#### Scenario: Courses to each signpost in each direction
- **WHEN** knot 02 in Siegburg-Zange has two signposts, and the route from 06 to 02 is mapped with separate ways for each direction near 02
- **THEN** each course to a signpost of 02 also has its course back where that differs, and the route from 06 to 02 follows the ways for that direction

#### Scenario: Bridges on the course back
- **WHEN** the course back runs over a bridge the course there does not use
- **THEN** the course back has its own bridge stretch there

#### Scenario: Released apps
- **WHEN** an app of version 0.10.0 reads a geometry file with courses back
- **THEN** it reads the courses, the courses to each signpost and the bridge stretches as before, and ignores the courses back

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole covered area is written with courses back
- **THEN** the file is still smaller than 10 MB
