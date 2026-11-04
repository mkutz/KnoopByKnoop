# Spec Delta

## ADDED Requirements

### Requirement: Published path data
The weekly publication SHALL include one file with the ways needed for routing bicycles and for drawing what surrounds them, for the whole covered area, built from the same extracts as the networks and split into squares that can be downloaded on their own.

#### Scenario: Ways across borders
- **WHEN** a cycle path crosses from Germany into the Netherlands
- **THEN** the squares on both sides connect, and a way can follow it across the border

#### Scenario: Roads that can't be ridden
- **WHEN** a motorway passes through a square
- **THEN** it is in the file marked as not rideable, so it can be drawn but not routed on

#### Scenario: Failed path build
- **WHEN** building the path data fails
- **THEN** the previously published files and index stay available unchanged

#### Scenario: Path data size budget
- **WHEN** the path file of the whole covered area is written
- **THEN** it is smaller than 150 MB, or the publication fails

### Requirement: Path data in the index
The index SHALL describe the path file in an optional entry of its own, with the data date and what is needed to verify it. Apps that don't know this entry SHALL be able to read the index as before.

#### Scenario: An earlier app reads the new index
- **WHEN** a released app that doesn't know path data reads an index with this entry
- **THEN** it installs the graphs and geometry as before

#### Scenario: An index without path data
- **WHEN** an app reads an index without this entry
- **THEN** it works as without path data, and off route works as it does without a guided way

### Requirement: Versioned path format
The path file SHALL state its format version, so an app can tell that it can't read it.

#### Scenario: Unsupported path format
- **WHEN** the path file has a format version the app doesn't support
- **THEN** the app doesn't use it, navigation works without a guided way, and the graphs and geometry are not affected
