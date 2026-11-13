## ADDED Requirements

### Requirement: Route file
A route SHALL be written as a UTF-8 Markdown text file with the extension `.knoop`, a list of the knots in riding order as riders write them down. The first line SHALL be `# ` and the route's title. A line `Network: hiking` SHALL follow for a hiking route; without it a route SHALL be read as cycling. Each waypoint SHALL be a top-level list item and each knot passed between two waypoints an item indented below the waypoint before it. A knot SHALL be written as its number, and the first knot of the file also with its OpenStreetMap node id after the number; a route point SHALL be written as its latitude and longitude with five decimals, separated by a comma. Any text after that SHALL be the item's text.

#### Scenario: Route as a file
- **WHEN** a cycling route from Siegburg 02 through the stop 83 to the end 73, passing 28, 29 and 30 before 83 and 27, 82, 81, 80, 79 and 01 after it, is written with the title "Siegburg Bonn and back"
- **THEN** the file reads `# Siegburg Bonn and back`, then `- 02 <node id of 02> Siegburg`, the items `  - 28`, `  - 29 Troisdorf`, `  - 30`, then `- 83 Bonn`, the items `  - 27` to `  - 01`, and `- 73`

#### Scenario: Hiking route as a file
- **WHEN** a hiking route is written
- **THEN** the line after the title is `Network: hiking`

#### Scenario: Route point as a file
- **WHEN** a route starting at a flag at 50.78210, 7.21540 and then passing 02 is written
- **THEN** its first item is `- 50.78210, 7.21540` and the next item knot 02 carries its node id

#### Scenario: Route point from a place as a file
- **WHEN** a route ending at the café "Eiscafé Riviera" is written
- **THEN** its last item is the café's position followed by `Eiscafé Riviera`

#### Scenario: Round trip as a file
- **WHEN** a round trip from 02 back to 02 is written
- **THEN** the first and the last top-level item are both 02

### Requirement: Knot texts in a route file
The app SHALL write after a knot's number the town or village the knot lies in where it differs from that of the knot before in the file. Text after a waypoint's number that differs from its town SHALL be shown in the waypoint list in place of the town, kept across restarts and undo; text after a knot passed SHALL only be kept in the file.

#### Scenario: Towns in the file
- **WHEN** a route passes 02 and 28 in Siegburg, then 29 and 30 in Troisdorf, is written
- **THEN** the items read `02 <node id> Siegburg`, `28`, `29 Troisdorf` and `30`

#### Scenario: Own text for a stop
- **WHEN** a file with the item `- 83 lunch at the Fährhaus` is opened
- **THEN** the waypoint list shows "Stop · lunch at the Fährhaus" for 83, also after a restart

### Requirement: Reading a route file
When reading, the first knot SHALL be found by its node id, or where the id is missing or not in the network, by its number and its text matching the town of a knot. Every further knot, indented or not, SHALL be found among the knots connected directly to the knot before it, else as the nearest knot with its number within 5 km of the knot before. A knot after a route point SHALL be the nearest with its number within 5 km. A knot that cannot be found SHALL be left out and the next one found from the knot before it. The route SHALL be planned between the waypoints as on a restart; the knots passed SHALL only be used to find the knots after them. Blank lines and lines that are no list item SHALL be skipped.

#### Scenario: Knot found as a neighbour
- **WHEN** the file lists `- 02 <node id> Siegburg`, then `  - 28`, and a knot 28 is connected to Siegburg 02 while another 28 lies closer elsewhere
- **THEN** the route goes through the 28 connected to Siegburg 02

#### Scenario: Round trip back to the start
- **WHEN** the file ends with `- 02 Siegburg` after `  - 25`, and 25 is connected to Siegburg 02 while Bonn has a 02 of its own
- **THEN** the route ends at Siegburg 02

#### Scenario: Hand-written file without an id
- **WHEN** a file reads `# Along the Sieg`, `- 02 Siegburg`, `- 28`
- **THEN** it is read as a cycling route from Siegburg 02 to the 28 connected to it

#### Scenario: Knot skipped in a hand-written list
- **WHEN** the file lists 02 then 29, and 29 is not connected to 02 but lies 3 km from it
- **THEN** the route goes from 02 to that 29

#### Scenario: Knot not found
- **WHEN** the file lists `- 45` after 30, and no 45 is connected to 30 or lies within 5 km of it
- **THEN** the route is opened without 45, the knot after 45 is found from 30, and the user is told that 45 was left out

#### Scenario: Route different from the list
- **WHEN** the knots passed in the file between 02 and 83 differ from the route the app finds between them
- **THEN** the route found by the app is shown, and the user is told that it differs from the file's list

#### Scenario: Other lines skipped
- **WHEN** a file has a paragraph of notes below the title and a line `Date: 2027-05-01`
- **THEN** those lines are skipped and the knots are read

### Requirement: Share the route
Share in the panel SHALL offer two choices: "Route for Knoop by Knoop", which SHALL offer the route as a `.knoop` file in the system share sheet, and "GPX for other apps", the GPX export. "Route for Knoop by Knoop" SHALL be offered for a complete route whose course along the network is known, also offline, and not while navigating. The title SHALL be the start's and the end's numbers and towns, and the file SHALL be named like the GPX export with `.knoop` at the end. Route points SHALL be shared as they are, wherever they lie.

#### Scenario: Share a route
- **WHEN** the user plans a cycling route from knot 12 through stop 40 to knot 34, chooses Share and "Route for Knoop by Knoop"
- **THEN** the share sheet opens with a file `knoop-by-knoop-cycling-12-34.knoop`

#### Scenario: Share while offline
- **WHEN** the device is offline and a complete route is planned
- **THEN** "Route for Knoop by Knoop" opens the share sheet with the file

#### Scenario: Home flag shared as it is
- **WHEN** the route starts at a flag at the user's home and is shared
- **THEN** the file's first item is that flag's position

### Requirement: Open a route file
Opening a `.knoop` file from another app (Files, mail, a messenger, AirDrop, a downloads list) SHALL open the app and load the file's route in place of the one being planned, switching to the file's network where it differs, and show the whole route on the map. A route point SHALL connect to the network and find its way as a newly chosen one does. A file in which fewer than two waypoints can be found SHALL leave the route as it is, and the user SHALL be told the file holds no route the app can open. While navigating, the app SHALL say to stop navigating first and leave the route and the navigation as they are.

#### Scenario: Open a shared route
- **WHEN** the user taps a `.knoop` file of the route 02 → 83 → 73 in a messenger while the app has another route planned
- **THEN** the app opens with the route 02 → 83 → 73, shown whole on the map

#### Scenario: Opened on the other network
- **WHEN** the app shows the cycling network and the user opens a file with `Network: hiking`
- **THEN** the app switches to hiking and shows the file's route

#### Scenario: Route point opened
- **WHEN** the file's last item is a position followed by "Eiscafé Riviera"
- **THEN** the route ends at a route point at that position named "Eiscafé Riviera", joined to the nearest course

#### Scenario: Not a route file
- **WHEN** the user opens a `.knoop` file in which no two waypoints can be found
- **THEN** the route being planned stays, and the user is told the file holds no route the app can open

#### Scenario: Opened while navigating
- **WHEN** the rider is navigating and opens a route file
- **THEN** the app says to stop navigating to open it, and navigation goes on with its route

#### Scenario: Undo an opened route
- **WHEN** the route 12 → 45 is planned, the user opens a file with the route 02 → 28 on the same network and chooses Undo
- **THEN** the route is 12 → 45 again
