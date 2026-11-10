## ADDED Requirements

### Requirement: Checked before publication
Before the weekly network data is published, every file SHALL be read back as the apps read it, each network SHALL have at least a fixed number of knots and connections, no network SHALL lose more than 5 % of its knots or connections against the published data unless the maintainer allows it for that run, and fixed knot pairs SHALL still be routable. If any check fails, nothing SHALL be published.

#### Scenario: File the apps would reject
- **WHEN** a geometry file of the rebuild does not belong to its graph file, or a square of the path data does not match its checksum
- **THEN** the publication fails, and the previously published files and index stay available unchanged

#### Scenario: Too few knots
- **WHEN** a rebuild from a truncated extract has 12,000 cycling knots, below the floor of 18,000
- **THEN** the publication fails, and the previously published data stays

#### Scenario: Sudden drop
- **WHEN** the published hiking network has 50,000 knots and the rebuild has 46,000, a loss of 8 %
- **THEN** the publication fails, and the previously published data stays

#### Scenario: Small change
- **WHEN** the published cycling network has 20,000 knots and the rebuild has 19,500, a loss of 2.5 %, and every other check passes
- **THEN** the rebuild is published

#### Scenario: Deliberate drop
- **WHEN** the maintainer starts a rebuild by hand allowing a drop, and the rebuild loses 8 % of the hiking knots but passes the floors, the read-back and the known routes
- **THEN** the rebuild is published

#### Scenario: Known route lost
- **WHEN** a rebuild has no cycling route between the fixed knot pair from the Netherlands to Belgium, or only one more than twice as long as the straight line between them
- **THEN** the publication fails, and the previously published data stays

#### Scenario: Nothing published yet
- **WHEN** the published data cannot be fetched or read, e.g. on the first publication or after a format version change
- **THEN** the drop is not checked, the other checks still apply, and the run says so in its log
