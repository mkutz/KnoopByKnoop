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

#### Scenario: Retrying a corrupted download
- **WHEN** a downloaded file was discarded as corrupted, on Android or iOS, and the app keeps running
- **THEN** the download is retried no sooner than 30 s later, and after each further failure it waits at least twice as long, up to 5 hours; Android's job scheduler may start a retry later than that

#### Scenario: Asked to update until it applies no more
- **WHEN** the user was asked to update the app because of a file's format version, and starts the app again without updating it
- **THEN** the info sheet still asks to update the app, on Android and iOS

#### Scenario: Updated app
- **WHEN** the user updated the app, and its next sync installs the files it couldn't read before
- **THEN** the info sheet no longer asks to update the app

#### Scenario: Published data readable again
- **WHEN** the user was asked to update the app, and the published index lists again the files installed on the device
- **THEN** the info sheet no longer asks to update the app, and the network no longer shows a failed update

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
While planning, the map SHALL offer an info sheet, opened with the info button, a round button with a gear at the top right of the map, apart from the network switch and announced as "Settings", with the data date of each installed network, the state of any download or update, the attribution required by OpenStreetMap, and the choice of the appearance (System, Light, Dark), see `map-planning` "Light and dark appearance".

#### Scenario: Opening the info sheet
- **WHEN** both networks are installed and the user opens the info sheet
- **THEN** it shows the data dates of the cycling and the hiking network and the OpenStreetMap attribution

#### Scenario: Failed update
- **WHEN** the last attempt to download newer data failed
- **THEN** the info sheet says so and offers to retry, while the installed data keeps working

#### Scenario: Info button
- **WHEN** the map is shown while planning
- **THEN** the info sheet is opened with a round gear button at the top right, on its own background like the map's button groups, not with a text button and not inside the network switch

#### Scenario: Info button with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the ⓘ button
- **THEN** it announces "Settings" as a button

#### Scenario: Readable info button
- **WHEN** the gear button is measured against its background, in light and in dark appearance
- **THEN** its icon has a contrast of at least 3:1

#### Scenario: No info button while navigating
- **WHEN** the rider is navigating
- **THEN** the map shows neither the gear button nor the network switch, and the info sheet is available again after navigation ends

#### Scenario: Appearance in the info sheet
- **WHEN** the user opens the info sheet
- **THEN** it shows the appearance with System, Light and Dark, the current choice selected, System by default

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
While planning a complete cycling route, the panel SHALL show whether the path data for it is installed, so the rider knows before leaving: as a network symbol at the end of the action row for the paths and streets around the route, with a spinner while loading, a checkmark when loaded and a warning mark when it couldn't be loaded, without a line of text and without changing the panel's height. A screen reader SHALL read the status as a sentence that says what the rider can do with it, and a tap on the symbol SHALL show that sentence; the sentences SHALL speak of paths and streets, not of ways or path data. On a hiking route, it SHALL show nothing about path data.

#### Scenario: Still downloading
- **WHEN** squares of the route are still being downloaded
- **THEN** the network symbol shows a spinner, and its sentence says "Saving paths and streets around the route…"

#### Scenario: Ready
- **WHEN** all squares of the route are installed
- **THEN** the network symbol shows a checkmark, and its sentence says "Paths and streets around the route saved: directions back to it work offline"

#### Scenario: Not available
- **WHEN** squares of the route are missing and can't be downloaded
- **THEN** the network symbol shows a warning mark, its sentence says "Paths and streets around the route not saved: off the route, only the knots guide you", and planning and navigating work as before

#### Scenario: Status with a screen reader
- **WHEN** a screen reader reaches the panel
- **THEN** it announces the same status as a sentence, e.g. "Paths and streets around the route saved: directions back to it work offline"

#### Scenario: Status sentence on a tap
- **WHEN** the user taps the network symbol while it shows a spinner
- **THEN** "Saving paths and streets around the route…" is shown for a moment

#### Scenario: Panel keeps its height
- **WHEN** a cycling route becomes complete and its ways and map load and finish
- **THEN** the panel's height does not change as the symbols appear and change

#### Scenario: No ways symbol on hiking
- **WHEN** a complete hiking route is planned
- **THEN** the action row shows the map symbol but no network symbol

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
The info sheet SHALL show the paths and streets as a third entry, named "Paths and streets", after the cycling and the hiking network: the data date and the space the installed squares take, or that none are installed. It SHALL explain in a short text that the paths and streets guide the rider back to the route off the knot network, also without a connection, and that they are downloaded from GitHub around planned cycling routes, so GitHub can see the rough area of a route, but never where the rider is. While squares are installed, the entry SHALL offer a button to delete them. Deleting SHALL remove all installed squares, without asking again, and the squares SHALL be downloaded again only for a route completed or changed afterwards, or when navigation starts.

#### Scenario: Opening the info sheet with path data
- **WHEN** squares taking 4.2 MB are installed and the user opens the info sheet
- **THEN** it shows "Paths and streets" after the cycling and hiking networks, with their data date, 4.2 MB, and a delete button

#### Scenario: No path data installed
- **WHEN** no squares are installed and the user opens the info sheet
- **THEN** the "Paths and streets" entry says that none are downloaded, and shows no delete button

#### Scenario: Explanation
- **WHEN** the user opens the info sheet, in English, German, Dutch or French
- **THEN** the "Paths and streets" entry explains what they are for and what GitHub can see, in the app's language

#### Scenario: Deleting the ways
- **WHEN** the user taps the delete button
- **THEN** all squares are removed, the entry says that none are downloaded, and the cycling and hiking networks stay installed

#### Scenario: Deleting with a route planned
- **WHEN** a cycling route is planned with its squares installed and the user deletes the ways
- **THEN** no squares are requested and the panel shows nothing about path data until the route is changed or navigation starts, and then the squares of that route are downloaded

#### Scenario: Delete button with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the delete button
- **THEN** it announces "Delete downloaded paths and streets" as a button

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

### Requirement: Base map around the route
While a cycling or hiking route is complete, the app SHALL store the base map around its course without the user doing anything else: in detail close to the course and coarser farther away, so the map can be read along the route while riding and around it while looking ahead. The tiles requested SHALL depend only on the route, never on the rider's position.

#### Scenario: Route planned at home
- **WHEN** the user completes a route with network access and stops changing it for a moment
- **THEN** the base map around it is stored, and the map shows streets, villages, rivers and woods along it later without network access

#### Scenario: A hiking route
- **WHEN** the user completes a hiking route
- **THEN** its base map is stored as for a cycling route

#### Scenario: Changing the route
- **WHEN** the user keeps adding knots to a route
- **THEN** the base map is only requested after the route has stopped changing, and tiles already stored are not requested again

#### Scenario: Starting before the download is done
- **WHEN** the rider starts navigating while parts of the route's base map are still missing, with network access
- **THEN** the missing parts of the route's base map are downloaded, and no others

#### Scenario: Starting away from the route
- **WHEN** the rider starts navigating 6 km from the route, with network access
- **THEN** no base map is requested for storing around the rider or on the way to the route

#### Scenario: A long route
- **WHEN** the route's base map would need more than 3,000 tiles or 50 MB
- **THEN** it is stored without the most detailed level where that doesn't fit, and the map can still be read zoomed in

#### Scenario: Without network access
- **WHEN** the route's base map can't be downloaded
- **THEN** planning and navigating work as before, and the download is tried again when network access is back

### Requirement: Base map requests
Every request for the base map SHALL name the app in its User-Agent. Storing a route's base map SHALL send at most 4 requests at a time and request at most 3,000 tiles and 50 MB per route, as OpenFreeMap's terms for offline downloads ask.

#### Scenario: Requests name the app
- **WHEN** the map loads tiles, while planning or while storing a route's base map
- **THEN** each request's User-Agent names Knoop by Knoop

#### Scenario: Requests at a time
- **WHEN** a route's base map is being stored
- **THEN** no more than 4 of its requests are open at a time

### Requirement: Base map ready for the ride
While planning a complete route, the panel SHALL show whether the route's base map is stored, as a map symbol at the end of the action row before the network symbol, with a spinner while loading, a checkmark when loaded and a warning mark when it couldn't be loaded, without a line of text. A screen reader SHALL read the status as a sentence that says what the rider can do with it, and a tap on the symbol SHALL show that sentence.

#### Scenario: Map still loading
- **WHEN** parts of the route's base map are still being downloaded
- **THEN** the map symbol shows a spinner, and its sentence says "Saving the map along the route…"

#### Scenario: Map loaded
- **WHEN** the route's base map is stored
- **THEN** the map symbol shows a checkmark, and its sentence says "Map along the route saved: it shows offline too"

#### Scenario: Map not available
- **WHEN** the route's base map can't be downloaded
- **THEN** the map symbol shows a warning mark, its sentence says "Map along the route not saved: offline, only the knots and the route show", and planning and navigating work as before

#### Scenario: Map status in all languages
- **WHEN** the app's language is German, Dutch or French
- **THEN** the map symbol's sentence is in that language, also for a screen reader

#### Scenario: Status marks without motion
- **WHEN** the phone is set to reduce motion or to remove animations and the map finishes loading
- **THEN** the checkmark replaces the spinner without a fade

### Requirement: Keeping the base map
The stored base map SHALL stay usable without network access until the route changes. The app SHALL keep the base map of the current route only: a new route's map SHALL replace it once that is downloaded. Tiles the map caches while viewing it SHALL be handled as before.

#### Scenario: A new route
- **WHEN** the user plans a new route in another area and its base map is stored
- **THEN** the base map of the previous route is removed

#### Scenario: Restarting the app
- **WHEN** the app is closed and started again without network access, with the same route planned
- **THEN** the route's base map is shown

### Requirement: Base map in the info sheet
The info sheet SHALL show the base map after the place details: the space it takes, or that none is stored, with a short text that the map around the planned route is stored for use without a connection and comes from OpenFreeMap, which can see the rough area of a route but never where the rider is. While one is stored, the entry SHALL offer a button to delete it.

#### Scenario: Opening the info sheet with a base map
- **WHEN** the base map of the route, taking 18 MB, is stored and the user opens the info sheet
- **THEN** it shows the base map after the place details, with 18 MB and a delete button

#### Scenario: No base map stored
- **WHEN** no base map is stored and the user opens the info sheet
- **THEN** the entry says that none is stored, and shows no delete button

#### Scenario: Explaining the base map
- **WHEN** the user opens the info sheet, in English, German, Dutch or French
- **THEN** the entry explains what the stored base map is for and what OpenFreeMap can see, in the app's language

#### Scenario: Deleting the base map
- **WHEN** the user taps the entry's delete button
- **THEN** the stored base map is removed, the entry says that none is stored, and the networks, ways and place details stay installed

#### Scenario: Deleting the base map with a route planned
- **WHEN** a route is planned with its base map stored and the user deletes it
- **THEN** no base map is requested until the route is changed or navigation starts, and then the route's base map is downloaded

#### Scenario: Base map delete button with a screen reader
- **WHEN** a screen reader reaches the entry's delete button
- **THEN** it announces that it deletes the stored base map, as a button
