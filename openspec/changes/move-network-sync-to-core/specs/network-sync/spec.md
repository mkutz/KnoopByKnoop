## MODIFIED Requirements

### Requirement: Install only verified data
A downloaded file SHALL only be installed if its size and checksum match the index. Installed data SHALL stay usable until verified replacement data is installed.

#### Scenario: Corrupted download
- **WHEN** a downloaded file does not match the checksum in the index
- **THEN** the file is discarded, the network keeps its previous state, and the download is retried later

#### Scenario: Unsupported file version
- **WHEN** a downloaded file has a format version the app does not support
- **THEN** it is not installed, and the user is asked to update the app

#### Scenario: Retrying a corrupted download
- **WHEN** a downloaded file was discarded as corrupted, on Android or iOS, and the app keeps running
- **THEN** the download is retried after 30 s, and after each further failure it waits twice as long, at most 5 hours

#### Scenario: Asked to update until it applies no more
- **WHEN** the user was asked to update the app because of a file's format version, and starts the app again without updating it
- **THEN** the info sheet still asks to update the app, on Android and iOS

#### Scenario: Updated app
- **WHEN** the user updated the app, and its next sync installs the files it couldn't read before
- **THEN** the info sheet no longer asks to update the app
