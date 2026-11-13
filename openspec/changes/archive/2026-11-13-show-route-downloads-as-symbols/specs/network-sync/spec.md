## MODIFIED Requirements

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
