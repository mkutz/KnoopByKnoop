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
The app SHALL check the published index for newer data every time it starts, and SHALL install newer data in the background without the user doing anything. Coming back to the app from the background SHALL NOT count as a start.

#### Scenario: Newer data available
- **WHEN** the index lists a newer data date for an installed network
- **THEN** the newer files are downloaded and installed in the background, and planning on the installed data keeps working until they are installed

#### Scenario: Route across an update
- **WHEN** a route is being planned while an update of its network is installed
- **THEN** the route is kept, and a waypoint whose knot no longer exists is removed and pointed out to the user

#### Scenario: Data published since the last start
- **WHEN** newer data was published an hour after the app last checked the index, and the user starts the app again
- **THEN** the app checks the index at this start and installs the newer data, without waiting a day

#### Scenario: Nothing new
- **WHEN** the app starts and the index lists the installed data dates
- **THEN** only the index is downloaded, and no network file

### Requirement: Info sheet
While planning, the map SHALL offer an info sheet, opened with a round info button (ⓘ) at the end of the network switch, with the data date of each installed network, the state of any download or update, the attribution required by OpenStreetMap, and the choice of the appearance (System, Light, Dark), see `map-planning` "Light and dark appearance".

#### Scenario: Opening the info sheet
- **WHEN** both networks are installed and the user opens the info sheet
- **THEN** it shows the data dates of the cycling and the hiking network and the OpenStreetMap attribution

#### Scenario: Failed update
- **WHEN** the last attempt to download newer data failed
- **THEN** the info sheet says so and offers to retry, while the installed data keeps working

#### Scenario: Info button
- **WHEN** the map is shown while planning
- **THEN** the info sheet is opened with a round ⓘ button, styled like the zoom buttons, not with a text button

#### Scenario: Info button with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the ⓘ button
- **THEN** it announces "Info" as a button

#### Scenario: Readable info button
- **WHEN** the ⓘ button is measured against its background, in light and in dark appearance
- **THEN** its icon has a contrast of at least 3:1

#### Scenario: No info button while navigating
- **WHEN** the rider is navigating
- **THEN** the map shows neither the ⓘ button nor the network switch, and the info sheet is available again after navigation ends

#### Scenario: Appearance in the info sheet
- **WHEN** the user opens the info sheet
- **THEN** it shows the appearance with System, Light and Dark, the current choice selected, System by default

### Requirement: Turning on guidance away from the route
Guidance away from the route SHALL be off until the user turns it on with a switch in the info sheet. The switch SHALL explain that the app then requests path data from GitHub by area, so GitHub can see the rough area of a planned route, and never where the rider is. While it's off, the app SHALL NOT request any path data.

#### Scenario: Off by default
- **WHEN** the user installs or updates the app and hasn't turned the switch on
- **THEN** the app requests no path data, also when an index lists it, and off route works without a guided way

#### Scenario: Reading the explanation
- **WHEN** the user opens the info sheet
- **THEN** the switch and its explanation are shown, in the app's language

#### Scenario: Turning it on with a route
- **WHEN** the user turns the switch on while a complete cycling route is planned, with network access
- **THEN** the squares around that route are downloaded

#### Scenario: Turning it off
- **WHEN** the user turns the switch off
- **THEN** running downloads stop, the installed squares are removed, and the app requests no more path data

#### Scenario: Kept across starts
- **WHEN** the user turned the switch on and restarts the app
- **THEN** it's still on

### Requirement: Path data around the route
While guidance away from the route is turned on and a cycling route is complete, the app SHALL download the squares of path data within 5 km of its course without the user doing anything else. The squares requested SHALL depend only on the route, and never on the rider's position.

#### Scenario: Route planned at home
- **WHEN** guidance away from the route is on, and the user completes a cycling route with network access and stops changing it for a moment
- **THEN** the squares around it are downloaded, and navigating it later needs no network access for them

#### Scenario: Changing the route
- **WHEN** the user keeps adding knots to a route
- **THEN** squares are only requested after the route has stopped changing, and squares already installed are not requested again

#### Scenario: Starting away from the route
- **WHEN** the rider starts navigating 6 km from the route, with network access
- **THEN** no squares are requested around the rider or on the way to the route

#### Scenario: Far off route
- **WHEN** the rider is off route with network access and gets near the edge of the downloaded squares
- **THEN** no more squares are requested

#### Scenario: Starting before the download is done
- **WHEN** the rider starts navigating while squares of the route are still missing, with network access
- **THEN** the missing squares of the route are downloaded, and no others

#### Scenario: A hiking route
- **WHEN** the user completes a hiking route
- **THEN** no path data is downloaded

#### Scenario: Without network access
- **WHEN** the squares around a route can't be downloaded
- **THEN** planning and navigating work as before, and the download is tried again when network access is back

### Requirement: Path data ready for the ride
While planning a complete cycling route with guidance away from the route turned on, the panel SHALL show whether the path data for it is installed, so the rider knows before leaving. With the switch off, or on a hiking route, it SHALL show nothing about path data.

#### Scenario: Still downloading
- **WHEN** squares of the route are still being downloaded
- **THEN** the panel says that ways for this route are loading

#### Scenario: Ready
- **WHEN** all squares of the route are installed
- **THEN** the panel says that ways for this route are loaded

#### Scenario: Not available
- **WHEN** squares of the route are missing and can't be downloaded
- **THEN** the panel says that ways for this route couldn't be loaded, and planning and navigating work as before

#### Scenario: Status with a screen reader
- **WHEN** a screen reader reaches the panel
- **THEN** it announces the same status

### Requirement: Verified squares
A downloaded square SHALL only be used if its checksum matches the published file's list of squares. A failed square SHALL NOT affect the squares already installed.

#### Scenario: Corrupted square
- **WHEN** a downloaded square doesn't match its checksum
- **THEN** it is discarded and requested again later, and the rest keep working

#### Scenario: File replaced during a download
- **WHEN** the weekly publication replaces the path file while squares are being downloaded
- **THEN** squares that no longer match are discarded, and the app reads the new list of squares

### Requirement: Keeping path data
Installed squares SHALL stay usable until a newer version of them is downloaded. A square SHALL be updated only when it is needed for a route again. The app SHALL keep at most 50 MB of squares, removing those unused for the longest first, but never those of the current route.

#### Scenario: Newer path data published
- **WHEN** a newer path file is published and the user plans a new route in an area with installed squares
- **THEN** those squares are replaced, and squares of other areas are not downloaded

#### Scenario: Storage limit
- **WHEN** a new route would take the installed squares above 50 MB
- **THEN** the squares unused for the longest are removed, and those of the current route stay

### Requirement: Path data in the info sheet
The info sheet SHALL show the data date of the installed path data.

#### Scenario: Opening the info sheet with path data
- **WHEN** squares are installed and the user opens the info sheet
- **THEN** it shows their data date
