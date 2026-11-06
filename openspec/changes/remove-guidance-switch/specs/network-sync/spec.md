## REMOVED Requirements

### Requirement: Turning on guidance away from the route
**Reason**: Path data is chosen from the planned route alone and reveals less than the base map's tiles; a route costs about 1 to 6 MB. Guidance away from the route is now always available for cycling routes.
**Migration**: None for users, as the switch was never released. A stored setting from a test build is ignored. The explanation moves to the ways' entry in the info sheet, see "Path data in the info sheet".

## MODIFIED Requirements

### Requirement: Path data around the route
While a cycling route is complete, the app SHALL download the squares of path data within 5 km of its course without the user doing anything else. The squares requested SHALL depend only on the route, and never on the rider's position.

#### Scenario: Route planned at home
- **WHEN** the user completes a cycling route with network access and stops changing it for a moment
- **THEN** the squares around it are downloaded, and navigating it later needs no network access for them

#### Scenario: First start
- **WHEN** the user installs or updates the app and completes a cycling route, without changing any setting
- **THEN** the squares around it are downloaded

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
While planning a complete cycling route, the panel SHALL show whether the path data for it is installed, so the rider knows before leaving. On a hiking route, it SHALL show nothing about path data.

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

### Requirement: Path data in the info sheet
The info sheet SHALL show the ways as a third entry, after the cycling and the hiking network: the data date and the space the installed squares take, or that none are installed. It SHALL explain in a short text that the ways guide the rider back to the route along suitable paths, also without a connection, and that they are downloaded from GitHub around planned cycling routes, so GitHub can see the rough area of a route, but never where the rider is. While squares are installed, the entry SHALL offer a button to delete them. Deleting SHALL remove all installed squares, without asking again, and the squares SHALL be downloaded again only for a route completed or changed afterwards, or when navigation starts.

#### Scenario: Opening the info sheet with path data
- **WHEN** squares taking 4.2 MB are installed and the user opens the info sheet
- **THEN** it shows the ways after the cycling and hiking networks, with their data date, 4.2 MB, and a delete button

#### Scenario: No path data installed
- **WHEN** no squares are installed and the user opens the info sheet
- **THEN** the ways' entry says that none are downloaded, and shows no delete button

#### Scenario: Explanation
- **WHEN** the user opens the info sheet, in English, German, Dutch or French
- **THEN** the ways' entry explains what they are for and what GitHub can see, in the app's language

#### Scenario: Deleting the ways
- **WHEN** the user taps the delete button
- **THEN** all squares are removed, the entry says that none are downloaded, and the cycling and hiking networks stay installed

#### Scenario: Deleting with a route planned
- **WHEN** a cycling route is planned with its squares installed and the user deletes the ways
- **THEN** no squares are requested and the panel shows nothing about path data until the route is changed or navigation starts, and then the squares of that route are downloaded

#### Scenario: Delete button with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the delete button
- **THEN** it announces that it deletes the downloaded ways, as a button
