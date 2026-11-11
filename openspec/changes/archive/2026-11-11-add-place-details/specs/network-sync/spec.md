## ADDED Requirements

### Requirement: Place details around the route
While a route is complete, for cycling and for hiking, the app SHALL download the squares of place details within 5 km of its course without the user doing anything else. The squares requested SHALL depend only on the route, and never on the rider's position. Without network access, planning and navigating SHALL work as before, and the download SHALL be tried again when network access is back.

#### Scenario: Cycling route planned at home
- **WHEN** the user completes a cycling route with network access and stops changing it for a moment
- **THEN** the place details around it are downloaded, and place cards along it show contacts later without network access

#### Scenario: Hiking route
- **WHEN** the user completes a hiking route with network access
- **THEN** the place details around it are downloaded, while no path data is

#### Scenario: Changing the route
- **WHEN** the user keeps adding knots to a route
- **THEN** squares are only requested after the route has stopped changing, and squares already installed are not requested again

#### Scenario: Navigating away from the route
- **WHEN** the rider is navigating 8 km off the route with network access and taps a café
- **THEN** no square is requested for the café or around the rider, and the card shows the café without contacts unless its square is installed

#### Scenario: Without network access
- **WHEN** the squares around a route can't be downloaded
- **THEN** planning and navigating work as before, place cards show no contacts where squares are missing, and the download is tried again when network access is back

### Requirement: Place details for a tapped place
While planning, not navigating, a tap on a highlighted place whose square of place details is not installed SHALL download that square, with network access, so the card can show the contacts. Only the square of the tapped place SHALL be requested.

#### Scenario: Café far from the route
- **WHEN** the user taps a café 30 km from the planned route while planning, with network access
- **THEN** the café's square is downloaded and the card shows its contacts once it is there

#### Scenario: Square already installed
- **WHEN** the user taps a place whose square is installed
- **THEN** nothing is requested, and the card shows the contacts at once

#### Scenario: Tap while navigating
- **WHEN** the rider taps a place while navigating and its square is not installed
- **THEN** nothing is requested

### Requirement: Keeping place details
A downloaded square of place details SHALL only be used if its checksum matches the published file's list of squares; a failed square SHALL NOT affect the squares already installed. Installed squares SHALL stay usable until a newer version of them is downloaded, and a square SHALL be updated only when it is needed again. The app SHALL keep at most 10 MB of place details, removing those unused for the longest first, but never those of the current route.

#### Scenario: Corrupted square of place details
- **WHEN** a downloaded square of place details doesn't match its checksum
- **THEN** it is discarded and requested again later, and the rest keep working

#### Scenario: Newer place details published
- **WHEN** newer place details are published and the user plans a new route in an area with installed squares
- **THEN** those squares are replaced, and squares of other areas are not downloaded

#### Scenario: Storage limit for place details
- **WHEN** new squares would take the installed place details above 10 MB
- **THEN** the squares unused for the longest are removed, and those of the current route stay

### Requirement: Place details in the info sheet
The info sheet SHALL show the place details as an entry after the ways: the data date and the space the installed squares take, or that none are installed. It SHALL explain in a short text that they hold the websites, phone numbers and opening hours of the places on the map, and that they are downloaded from GitHub around planned routes and for places tapped while planning, so GitHub can see the rough area of a route or of a place, but never where the rider is. While squares are installed, the entry SHALL offer a button to delete them. Deleting SHALL remove all installed squares of place details, without asking again, and leave the networks and the ways installed; squares SHALL be downloaded again only for a route completed or changed afterwards, for a place tapped while planning, or when navigation starts.

#### Scenario: Opening the info sheet with place details
- **WHEN** squares of place details taking 1.3 MB are installed and the user opens the info sheet
- **THEN** it shows the place details after the ways, with their data date, 1.3 MB, and a delete button

#### Scenario: No place details installed
- **WHEN** no squares of place details are installed and the user opens the info sheet
- **THEN** the entry says that none are downloaded, and shows no delete button

#### Scenario: Explaining place details
- **WHEN** the user opens the info sheet, in English, German, Dutch or French
- **THEN** the entry explains what the place details are and what GitHub can see, in the app's language

#### Scenario: Deleting the place details
- **WHEN** the user taps the entry's delete button
- **THEN** all squares of place details are removed, the entry says that none are downloaded, and the networks and the ways stay installed

#### Scenario: Deleting place details with a route planned
- **WHEN** a route is planned with its place details installed and the user deletes them
- **THEN** no squares are requested until the route is changed, a place is tapped while planning, or navigation starts

#### Scenario: Place details delete button with a screen reader
- **WHEN** a screen reader reaches the entry's delete button
- **THEN** it announces that it deletes the downloaded place details, as a button
