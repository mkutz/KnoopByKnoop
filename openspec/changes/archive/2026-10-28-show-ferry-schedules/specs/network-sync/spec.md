## MODIFIED Requirements

### Requirement: Bundled graphs
Each app SHALL ship with the cycling and the hiking graph of the latest publication at the time it was built, without their geometry. On start, the app SHALL install the bundled graphs unless newer verified data is already installed, so planning works without any download. A bundled graph with the same data date as the installed one SHALL replace it when it is in a newer file format.

#### Scenario: Planning right after installation
- **WHEN** the user opens the app for the first time without network access
- **THEN** cycling and hiking routes can be planned right away, with connections and routes drawn as straight lines

#### Scenario: Newer data already installed
- **WHEN** an app update ships bundled graphs that are older than the installed ones
- **THEN** the installed graphs are kept

#### Scenario: Bundled graph in a newer format
- **WHEN** an app update ships a bundled graph with the same data date as the installed one, but in a newer file format, e.g. with the ferries
- **THEN** the bundled graph replaces the installed one on the next start, without network access, and a later start does not install it again

#### Scenario: Data date of a release
- **WHEN** a release of the app is built
- **THEN** its bundled graphs have the data date of the publication at build time, and the info sheet shows it until newer data is installed
