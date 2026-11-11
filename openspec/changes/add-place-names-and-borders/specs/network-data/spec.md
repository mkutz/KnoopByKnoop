## ADDED Requirements

### Requirement: Place of each knot
Each knot in a graph file SHALL carry the place it lies in: a village, town or city of OpenStreetMap, inside its mapped area or close to its point by the place's size. Its name is the place's `name`, as on its signpost, plus its names in English, German, Dutch and French where mapped. Hamlets, suburbs, quarters and districts SHALL NOT count, and a knot in no such place SHALL carry none. Apps released before SHALL read such files as before; files without places give no place for any knot.

#### Scenario: Knot in a town
- **WHEN** the pipeline builds the cycling graph and knot 06 lies at the edge of Lohmar, a town
- **THEN** knot 06 carries the place Lohmar

#### Scenario: Knot in the open
- **WHEN** a knot lies in fields 3 km from the nearest village
- **THEN** it carries no place

#### Scenario: Only a hamlet nearby
- **WHEN** a knot lies beside a hamlet, and the nearest village is far away
- **THEN** it carries no place

#### Scenario: Names in other languages
- **WHEN** a knot lies in Liège, mapped with `name` Liège, `name:de` Lüttich and `name:nl` Luik
- **THEN** the knot's place has the name Liège and those names in German and Dutch

#### Scenario: Graph files without places
- **WHEN** an app reads a graph file written before knots had places
- **THEN** it reads it as before, with no place for any knot

### Requirement: Borders between knots
Each connection in a graph file SHALL carry where along its course it crosses a border of a country, a Dutch or Belgian province, Brussels or a German state, and what lies beyond, named in English, German, Dutch and French, from OpenStreetMap's administrative boundaries. A border that cannot be read completely from the extracts SHALL fail the build rather than give wrong crossings. Apps released before SHALL read such files as before.

#### Scenario: Crossing into Belgium
- **WHEN** the connection from a knot in Dutch Limburg to one in Belgian Limburg crosses the national border 1.2 km after its start
- **THEN** it carries a crossing at 1.2 km into Belgium and the Belgian province of Limburg

#### Scenario: Crossing a province border
- **WHEN** a connection crosses from Gelderland into Overijssel
- **THEN** it carries a crossing into Overijssel only, as the country stays the same

#### Scenario: Within one province
- **WHEN** a connection runs inside North Brabant
- **THEN** it carries no crossing

#### Scenario: Beyond the covered area
- **WHEN** a connection leaves North Rhine-Westphalia into Lower Saxony, whose border the extracts don't hold
- **THEN** it carries no crossing there

#### Scenario: Incomplete border
- **WHEN** a province's boundary relation misses ways in the extracts
- **THEN** the build fails and names the province

#### Scenario: Graph files without borders
- **WHEN** an app reads a graph file written before connections had borders
- **THEN** it reads it as before, with no crossings
