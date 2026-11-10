## Purpose
Keeps the specs valid and every scenario tied to a test, or to a stated way it is checked otherwise, so a change cannot quietly leave specified behavior unverified.

## ADDED Requirements

### Requirement: Tests tied to scenarios by name
A test SHALL count for a scenario when its name starts with the scenario's name, ignoring case and punctuation, or when it is marked with the scenario's name. For a scenario name of fewer than three words, only a test of exactly that name or a marked test SHALL count.

#### Scenario: Test named after the scenario
- **WHEN** the scenario "Cycle path beside a main road" has the test `Cycle path beside a main road is preferred when 20 percent longer`
- **THEN** the scenario counts as tested

#### Scenario: Test marked with the scenario
- **WHEN** the scenario "Retrying a corrupted download" has no test of that name, but a test is marked `// Scenario: Retrying a corrupted download`
- **THEN** the scenario counts as tested

#### Scenario: Short scenario name
- **WHEN** the scenario "Ready" has no test of exactly that name and no marked test, and a test is named `ready to route after the download`
- **THEN** the scenario does not count as tested

### Requirement: Every scenario has a test or a stated check
The build SHALL fail when a scenario of the specs has neither a test nor an entry in the list of scenarios checked otherwise, naming the capability, requirement and scenario of each one. Every entry of the list SHALL say how the scenario is checked.

#### Scenario: Scenario without a test
- **WHEN** a commit adds the scenario "Knot in Flanders" to `network-data` without a test and without an entry in the list
- **THEN** the build fails, naming `network-data`, the requirement "Sign region of each knot" and the scenario

#### Scenario: Scenario checked on the devices
- **WHEN** the scenario "Readable info button" has no test and the list has it under `network-sync` as checked on the devices
- **THEN** the build passes

#### Scenario: Entry without a way of checking
- **WHEN** the list has an entry with a scenario name but nothing on how it is checked
- **THEN** the build fails, naming the entry

### Requirement: The list of scenarios checked otherwise stays true
The build SHALL fail on an entry of the list whose scenario no longer exists in the capability it is listed under, and on an entry whose scenario has a test, naming the entry.

#### Scenario: Scenario renamed
- **WHEN** a change renames a scenario listed as checked on the devices, and the list still has the old name
- **THEN** the build fails, naming the entry with the old name

#### Scenario: Scenario tested later
- **WHEN** a test is added for a scenario the list has as checked on the devices
- **THEN** the build fails until the entry is removed

### Requirement: Scenarios of a change checked before archiving
The scenario check SHALL be able to check the scenarios a change adds or modifies, with the tests and the list as they are, so a change's scenarios without a test show before it is archived.

#### Scenario: Change with a scenario still untested
- **WHEN** the check is run for a change whose spec delta adds two scenarios, one of them with a test
- **THEN** it fails, naming the other scenario

### Requirement: Specs checked on every push
Every push to main and every pull request SHALL validate the specs and the active changes with OpenSpec and run the scenario check, also when the push changes only Markdown.

#### Scenario: Commit changing only a spec
- **WHEN** a commit changes only `openspec/specs/knot-navigation/spec.md` and leaves a requirement without a scenario
- **THEN** the Specs workflow fails on it, although Build skips the commit

#### Scenario: Valid specs
- **WHEN** a commit changes a spec, the specs are valid and every scenario has a test or an entry in the list
- **THEN** the Specs workflow passes
