## ADDED Requirements

### Requirement: Language of each ferry's name
Each named ferry in a graph file SHALL carry the language of its name, from the region the middle of its ferry way lies in: Dutch in a Dutch province or a Flemish province, French in a Walloon province, German in North Rhine-Westphalia. The regions SHALL be the same administrative boundaries as in "Borders between knots". A ferry in Brussels or outside these regions SHALL carry no language. Apps released before SHALL read such files as before.

#### Scenario: Ferry in Gelderland
- **WHEN** the pipeline builds the cycling graph and the ferry "Renkumse Veer" lies in Gelderland
- **THEN** the graph carries Dutch as the language of its name

#### Scenario: Ferry in Antwerp
- **WHEN** a ferry across the Scheldt lies in the province of Antwerp
- **THEN** the graph carries Dutch as the language of its name

#### Scenario: Ferry in Wallonia
- **WHEN** a ferry across the Meuse lies in the province of Liège
- **THEN** the graph carries French as the language of its name

#### Scenario: Ferry on the Rhine
- **WHEN** a ferry across the Rhine lies in North Rhine-Westphalia
- **THEN** the graph carries German as the language of its name

#### Scenario: Ferry on a border river
- **WHEN** a ferry across the Meuse runs from Dutch Limburg to Belgian Limburg
- **THEN** the graph carries the language of the region its way's middle lies in, here Dutch

#### Scenario: Ferry outside the regions
- **WHEN** a ferry lies in Lower Saxony, whose border the extracts don't hold
- **THEN** the graph carries no language for its name

#### Scenario: Ferry without a name
- **WHEN** a ferry has no name in OpenStreetMap
- **THEN** the graph carries no language for it

#### Scenario: Graph files without name languages
- **WHEN** an app reads a graph file written before ferries had name languages
- **THEN** it reads it as before, with no language for any ferry's name
