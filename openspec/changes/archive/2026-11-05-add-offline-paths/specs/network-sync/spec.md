# Spec Delta

## ADDED Requirements

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
