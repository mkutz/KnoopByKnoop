## ADDED Requirements

### Requirement: Sign region of each knot
Each cycling knot in a graph file SHALL carry the region whose signposts it follows, so the apps can show its badge like its signs:
- the Netherlands for a knot inside the Dutch national border
- Belgium for a knot inside the Belgian national border
- North Rhine-Westphalia for a knot inside that state's border

The borders SHALL come from OpenStreetMap's administrative boundaries in the extracts, at build time. Other cycling knots and all hiking knots SHALL carry no region. If a border cannot be read completely from the extracts, the build SHALL fail rather than write wrong regions. Apps released before this requirement SHALL read such graph files as before, and apps SHALL read graph files without regions with no region for any knot.

#### Scenario: Knot in Limburg
- **WHEN** the pipeline builds the cycling graph and knot 95 lies in Roermond
- **THEN** knot 95 carries the region Netherlands

#### Scenario: Knot in Flanders
- **WHEN** knot 150 lies in Haspengouw
- **THEN** it carries the region Belgium

#### Scenario: Knot in North Rhine-Westphalia
- **WHEN** knot 60 lies in Lünen
- **THEN** it carries the region North Rhine-Westphalia

#### Scenario: Knot in another German state
- **WHEN** the North Rhine-Westphalia extract contains a knot just across the border in Rhineland-Palatinate
- **THEN** that knot carries no region

#### Scenario: Knot near a border
- **WHEN** a knot lies 50 m on the Belgian side of the Dutch-Belgian border and is contained in both the Dutch and the Belgian extract
- **THEN** it carries the region Belgium

#### Scenario: Hiking knots
- **WHEN** the pipeline builds the hiking graph
- **THEN** no hiking knot carries a region

#### Scenario: Incomplete border
- **WHEN** the Belgian national border cannot be closed from the ways in the extracts
- **THEN** the build fails and names the incomplete border

#### Scenario: Apps without sign regions
- **WHEN** an app released before graph files had sign regions reads such a file
- **THEN** it installs and reads it as before

#### Scenario: Graph files without sign regions
- **WHEN** an app reads a graph file written before it had sign regions
- **THEN** it reads every knot without a region
