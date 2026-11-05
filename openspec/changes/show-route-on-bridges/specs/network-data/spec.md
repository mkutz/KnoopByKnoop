## ADDED Requirements

### Requirement: Bridge stretches of courses
For every course in the published data, including the courses to each signpost, the data SHALL include the stretches of the course that run on a bridge. A bridge is an OpenStreetMap way with a `bridge` tag other than `no`. Each stretch SHALL start and end within 5 m of where the bridge starts and ends along the course. Apps that do not know these stretches SHALL still read the file.

#### Scenario: Course over a bridge
- **WHEN** the connection 28-29 crosses a road on a bridge
- **THEN** its course has one bridge stretch, from where the bridge starts to where it ends

#### Scenario: Course under a bridge
- **WHEN** a connection passes under a railway bridge on an untagged path
- **THEN** its course has no bridge stretch there

#### Scenario: Several ways of one bridge
- **WHEN** a bridge is mapped as two consecutive ways with a `bridge` tag
- **THEN** the course has one bridge stretch covering both

#### Scenario: Bridge tagged no
- **WHEN** a way of a course has `bridge=no`
- **THEN** the course has no bridge stretch there

#### Scenario: Stretch kept through simplification
- **WHEN** the course of a connection is simplified to within 5 m and a bridge starts at a point the simplification would drop
- **THEN** the bridge stretch still starts within 5 m of where the bridge starts

#### Scenario: Signpost course over a bridge
- **WHEN** the course to one of the signposts of a knot runs over a bridge
- **THEN** that course has its own bridge stretch

#### Scenario: Released apps
- **WHEN** an app of version 0.9.0 reads a geometry file with bridge stretches
- **THEN** it reads the courses and the courses to each signpost as before, and ignores the bridge stretches

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole covered area is written with bridge stretches
- **THEN** the file is still smaller than 10 MB
