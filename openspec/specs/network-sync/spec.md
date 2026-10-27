# network-sync Specification

## Purpose
Gets the cycling and hiking networks onto the device without the user doing anything, and keeps them current, so planning works offline everywhere the published data covers.

## Requirements

### Requirement: Download the networks automatically
On first start, the app SHALL download the geometry of the cycling and the hiking network, and their graphs where the index has newer graphs than the bundled ones, without the user starting it. The cycling network SHALL be downloaded first.

#### Scenario: First start
- **WHEN** the user opens the app for the first time with network access
- **THEN** the geometry of both networks is downloaded without any further action, the cycling network first, and their graphs only if the index has newer ones than the bundled graphs

#### Scenario: Leaving the app during the download
- **WHEN** the user switches to another app while the networks are downloading
- **THEN** the download continues, and the networks are installed when the user returns after it finished

#### Scenario: Interrupted download
- **WHEN** the network connection drops during the download
- **THEN** the download continues by itself when the connection is back, and files that were completely downloaded before are not downloaded again

### Requirement: Bundled graphs
Each app SHALL ship with the cycling and the hiking graph of the latest publication at the time it was built, without their geometry. On first start, the app SHALL install the bundled graphs unless newer verified data is already installed, so planning works without any download.

#### Scenario: Planning right after installation
- **WHEN** the user opens the app for the first time without network access
- **THEN** cycling and hiking routes can be planned right away, with connections and routes drawn as straight lines

#### Scenario: Newer data already installed
- **WHEN** an app update ships bundled graphs that are older than the installed ones
- **THEN** the installed graphs are kept

#### Scenario: Data date of a release
- **WHEN** a release of the app is built
- **THEN** its bundled graphs have the data date of the publication at build time, and the info sheet shows it until newer data is installed

### Requirement: Courses still loading
While the geometry of the selected network is not installed, the map SHALL draw connections and routes as straight lines and SHALL show that the courses are still being loaded.

#### Scenario: Cycling is ready first
- **WHEN** the cycling geometry is installed and the hiking geometry is still downloading
- **THEN** cycling routes are drawn along their course, and after switching to hiking its routes are drawn as straight lines with a note that the courses are loading

#### Scenario: No network access on first start
- **WHEN** the user opens the app for the first time without network access
- **THEN** planning works on the bundled graphs, the map notes that the courses need network access once, and the download starts by itself when network access is back

### Requirement: Install only verified data
A downloaded file SHALL only be installed if its size and checksum match the index. Installed data SHALL stay usable until verified replacement data is installed.

#### Scenario: Corrupted download
- **WHEN** a downloaded file does not match the checksum in the index
- **THEN** the file is discarded, the network keeps its previous state, and the download is retried later

#### Scenario: Unsupported file version
- **WHEN** a downloaded file has a format version the app does not support
- **THEN** it is not installed, and the user is asked to update the app

### Requirement: Keep the networks current
The app SHALL check the published index for newer data when it starts, at most once a day, and SHALL install newer data in the background without the user doing anything.

#### Scenario: Newer data available
- **WHEN** the index lists a newer data date for an installed network
- **THEN** the newer files are downloaded and installed in the background, and planning on the installed data keeps working until they are installed

#### Scenario: Route across an update
- **WHEN** a route is being planned while an update of its network is installed
- **THEN** the route is kept, and a waypoint whose knot no longer exists is removed and pointed out to the user

### Requirement: Info sheet
The map SHALL offer an info sheet with the data date of each installed network, the state of any download or update, and the attribution required by OpenStreetMap.

#### Scenario: Opening the info sheet
- **WHEN** both networks are installed and the user opens the info sheet
- **THEN** it shows the data dates of the cycling and the hiking network and the OpenStreetMap attribution

#### Scenario: Failed update
- **WHEN** the last attempt to download newer data failed
- **THEN** the info sheet says so and offers to retry, while the installed data keeps working
