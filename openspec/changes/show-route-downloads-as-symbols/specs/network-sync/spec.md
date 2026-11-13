## MODIFIED Requirements

### Requirement: Path data ready for the ride
While planning a complete cycling route, the panel SHALL show whether the path data for it is installed, so the rider knows before leaving: as a network symbol at the end of the action row, with a spinner while loading, a checkmark when loaded and a warning mark when it couldn't be loaded, without a line of text and without changing the panel's height. A screen reader SHALL read the status as a sentence, and a tap on the symbol SHALL show that sentence. On a hiking route, it SHALL show nothing about path data.

#### Scenario: Still downloading
- **WHEN** squares of the route are still being downloaded
- **THEN** the network symbol shows a spinner, and its sentence says that ways for this route are loading

#### Scenario: Ready
- **WHEN** all squares of the route are installed
- **THEN** the network symbol shows a checkmark, and its sentence says that ways for this route are loaded

#### Scenario: Not available
- **WHEN** squares of the route are missing and can't be downloaded
- **THEN** the network symbol shows a warning mark, its sentence says that ways for this route couldn't be loaded, and planning and navigating work as before

#### Scenario: Status with a screen reader
- **WHEN** a screen reader reaches the panel
- **THEN** it announces the same status as a sentence, e.g. "Ways for this route are loaded"

#### Scenario: Status sentence on a tap
- **WHEN** the user taps the network symbol while it shows a spinner
- **THEN** "Loading ways for this route…" is shown for a moment

#### Scenario: Panel keeps its height
- **WHEN** a cycling route becomes complete and its ways and map load and finish
- **THEN** the panel's height does not change as the symbols appear and change

#### Scenario: No ways symbol on hiking
- **WHEN** a complete hiking route is planned
- **THEN** the action row shows the map symbol but no network symbol

### Requirement: Base map ready for the ride
While planning a complete route, the panel SHALL show whether the route's base map is stored, as a map symbol at the end of the action row before the network symbol, with a spinner while loading, a checkmark when loaded and a warning mark when it couldn't be loaded, without a line of text. A screen reader SHALL read the status as a sentence, and a tap on the symbol SHALL show that sentence.

#### Scenario: Map still loading
- **WHEN** parts of the route's base map are still being downloaded
- **THEN** the map symbol shows a spinner, and its sentence says that the map for this route is loading

#### Scenario: Map loaded
- **WHEN** the route's base map is stored
- **THEN** the map symbol shows a checkmark, and its sentence says that the map for this route is loaded

#### Scenario: Map not available
- **WHEN** the route's base map can't be downloaded
- **THEN** the map symbol shows a warning mark, its sentence says that the map for this route couldn't be loaded, and planning and navigating work as before

#### Scenario: Map status in all languages
- **WHEN** the app's language is German, Dutch or French
- **THEN** the map symbol's sentence is in that language, also for a screen reader

#### Scenario: Status marks without motion
- **WHEN** the phone is set to reduce motion or to remove animations and the map finishes loading
- **THEN** the checkmark replaces the spinner without a fade
