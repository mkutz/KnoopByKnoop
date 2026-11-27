# Spec Delta

## Purpose

The route planner in the browser at knoopbyknoop.com: planning a route on the knot networks on a big screen with the apps' rules, and handing it to the phone by link, QR code or file.

## ADDED Requirements

### Requirement: Planner page in every language
The website SHALL offer the planner at `/plan/` in English and at `/de/plan/`, `/nl/plan/` and `/fr/plan/`, with every text, number and distance in the page's language. The language switch SHALL keep the route being planned.

#### Scenario: Planner in Dutch
- **WHEN** a visitor opens `/nl/plan/` and plans a route of 3.4 km
- **THEN** the planner's texts are Dutch and the distance reads "3,4 km"

#### Scenario: Language switch keeps the route
- **WHEN** a visitor plans a route on `/plan/` and chooses Deutsch
- **THEN** `/de/plan/` opens with the same route

### Requirement: Without JavaScript
A browser without JavaScript, or one too old for the planner, SHALL see a page that says the planner needs a current browser with JavaScript and links to the apps in the stores, instead of an empty map.

#### Scenario: JavaScript turned off
- **WHEN** a visitor with JavaScript turned off opens `/de/plan/`
- **THEN** the page says in German that the planner needs JavaScript and shows the App Store and Google Play badges

### Requirement: Networks on the map
The planner SHALL show the knots of the cycling or the hiking network with their numbers, and the courses between them, over a base map, as the apps draw them, with a switch between the two networks. Switching SHALL start a new route, as in the apps.

#### Scenario: Cycling network around Siegburg
- **WHEN** a visitor opens the planner and moves the map to Siegburg
- **THEN** the cycling knots, among them 02, show with their numbers, joined by their courses

#### Scenario: Switch to hiking
- **WHEN** the visitor switches to hiking
- **THEN** the hiking knots and courses show instead, and the route is empty

### Requirement: Loading shown
While the network or its courses load, the planner SHALL say so over the map, with what is loading, until it is there. A failure to show one part of the planner SHALL NOT keep the network or its courses from loading.

#### Scenario: Network still loading
- **WHEN** a visitor opens the planner for the first time and the network has not arrived yet
- **THEN** the map says that the network is loading

#### Scenario: Courses still loading
- **WHEN** the knots are in but the courses are not
- **THEN** the map says that the courses are loading, and the notice goes once they are in

#### Scenario: Kept route with a ferry
- **WHEN** the planner is opened again with a kept route that crosses the Rhine by ferry
- **THEN** the route shows in the panel with its ferry, and the courses load

### Requirement: Network data from the website
The planner SHALL read the network data only from knoopbyknoop.com: the graphs, the courses, the path data and the place details of the latest weekly publication, each file checked against the published index as the apps check it. Path data and place details SHALL be read in the squares needed, not as whole files.

#### Scenario: Data of the latest publication
- **WHEN** the network data was published on Monday and a visitor opens the planner on Tuesday
- **THEN** the planner uses Monday's data, read from knoopbyknoop.com

#### Scenario: File not matching the index
- **WHEN** a data file read by the planner does not match the index's checksum
- **THEN** the planner does not use it and says that the network data could not be loaded

### Requirement: Data updated with the weekly publication
After each weekly publication of the network data, the website SHALL serve the new data within two hours, all files from the same publication at once.

#### Scenario: New publication
- **WHEN** the weekly workflow publishes new network data
- **THEN** within two hours, knoopbyknoop.com serves the new index and its files, and no page mixes files of two publications

### Requirement: First view
Without a kept route, the planner SHALL show the area of all networks. While the map is zoomed out too far to draw the knots, it SHALL say over the map to zoom in or search a place, and a click on the map SHALL zoom in on the clicked spot until the knots show.

#### Scenario: Click while zoomed out
- **WHEN** a visitor opens the planner for the first time and clicks near Siegburg
- **THEN** the map zooms in on Siegburg so its knots show, and the route stays empty

#### Scenario: Hint to zoom in
- **WHEN** the map is zoomed out too far to draw the knots
- **THEN** the map says to zoom in or search a place to see the knots

### Requirement: Panel keeps the focus
When the route changes, the panel SHALL keep the keyboard focus on the control that was used, or on the same control of the next waypoint where its own went away, and SHALL keep its scroll position, opened lists and the text typed into the search field.

#### Scenario: Moving a stop with the keyboard
- **WHEN** a keyboard user moves the stop 83 down with its button
- **THEN** the focus stays on a move button in the list, so the next key press goes on from there

### Requirement: Plan by clicking knots
Clicking a knot SHALL change the route as tapping it does in the apps (see `map-planning` "Plan by tapping knots"), with the route computed by the same rules as in the apps, so the same waypoints on the same data give the same route in the planner and in the apps.

#### Scenario: Start, end and a stop
- **WHEN** the visitor clicks 02 in Siegburg, then 73, then 83, which lies between them
- **THEN** the route goes 02 → 83 → 73, along the same knots the apps find for these waypoints

#### Scenario: Double click on a waypoint
- **WHEN** the visitor clicks the start knot twice
- **THEN** the route stays as it was

### Requirement: Edit the waypoints
A panel beside the map SHALL list the waypoints in riding order with their numbers and towns. The visitor SHALL be able to remove a waypoint, move it to another place in the list, return to the start for a round trip, and clear the route.

#### Scenario: Reorder by dragging
- **WHEN** the route is 02 → 83 → 73 and the visitor drags 73 above 83
- **THEN** the route becomes 02 → 73 → 83

#### Scenario: Round trip in the planner
- **WHEN** the visitor chooses to return to the start on the route 02 → 83
- **THEN** the route becomes the round trip 02 → 83 → 02

### Requirement: Route summary in the planner
The panel SHALL show the route's total distance and the knots it passes, in riding order, as soon as the route is complete.

#### Scenario: Distance and knots
- **WHEN** the route 02 → 83 is complete
- **THEN** the panel shows its distance in kilometres and the knots from 02 to 83 in riding order

### Requirement: Route points in the planner
Clicking the map away from any knot SHALL offer to add a route point there. A route point SHALL join the network and find its way as in the apps (see `map-planning` "What a chosen route point does").

#### Scenario: Route point at a café
- **WHEN** the visitor clicks a spot 300 m off the network and chooses to add it
- **THEN** the route goes to that spot along paths from the nearest course, as the apps plan it

### Requirement: Undo in the planner
Undo SHALL take back the last edit of the route, and each further Undo the edit before, for at least the last 50 edits, by the same rules as in the apps (see `map-planning` "Undo route edits"). Undo SHALL be shown only when there is an edit to take back.

#### Scenario: Undo a removed stop
- **WHEN** the visitor removes the stop 83 from 02 → 83 → 73 and chooses Undo
- **THEN** the route is 02 → 83 → 73 again

### Requirement: Route kept in the browser
The route being planned and its undo history SHALL be kept in the visitor's browser and restored when the planner is opened again in that browser. Nothing of the route SHALL be sent to knoopbyknoop.com.

#### Scenario: Reload
- **WHEN** the visitor plans 02 → 83 → 73 and reloads the page
- **THEN** the route is 02 → 83 → 73 again

#### Scenario: Browser storage unavailable
- **WHEN** the browser blocks site storage
- **THEN** the planner works, and the route is only lost on reload

### Requirement: Place search in the planner
The planner SHALL offer a search field listing matching places, stations, addresses and businesses while the visitor types, in the page's language, within the area the networks cover, as the apps' search does (see `place-search` "Search while typing"). Choosing a hit SHALL move the map there.

#### Scenario: Station found in the planner
- **WHEN** the visitor types "Utrecht Centraal" and chooses the hit
- **THEN** the map moves to the station Utrecht Centraal

#### Scenario: Search unavailable in the planner
- **WHEN** the search service does not answer
- **THEN** the field says that search is not available right now, and planning works as before

### Requirement: Places in the planner
The map SHALL show the places useful on a ride as the apps do (see `map-planning` "Places useful on a ride"), with the same switch to hide them. Clicking a place SHALL open a card with its name, its kind and, as far as known, its opening hours, website and phone number, and the choice to add it to the route as a route point named after it.

#### Scenario: Café on the card
- **WHEN** the visitor clicks a café with tagged opening hours
- **THEN** a card shows its name, that it is a café, and its opening hours

#### Scenario: Place added to the route
- **WHEN** the visitor chooses to add the café "Eiscafé Riviera" to the route
- **THEN** the route goes to a route point at the café, named "Eiscafé Riviera"

### Requirement: Ferries in the planner
The panel SHALL list the ferries on the route with where they are on it, the knots on both sides, and, where the operating times allow, whether the ferry is open now and when that changes, in the ferry's local time, as the apps do (see `map-planning` "Ferries on the route" and "Ferry open now").

#### Scenario: Ferry on the route
- **WHEN** the route crosses a river by a ferry with tagged operating times, at a time it runs
- **THEN** the panel lists the ferry between its two knots and says it is open now, and until when

#### Scenario: Ferry listed without operating times
- **WHEN** the route uses a ferry without tagged operating times
- **THEN** the panel lists it as having no known operating times

### Requirement: Route link from the planner
For a complete route, the planner SHALL offer to copy the route's link (see `route-sharing` "Route link") and SHALL show it as a QR code that a phone camera can read from the screen.

#### Scenario: Scan to the phone
- **WHEN** the visitor plans 02 → 83 → 73 and scans the QR code with a phone that has the app
- **THEN** the app opens with the route 02 → 83 → 73

#### Scenario: Copy the link
- **WHEN** the visitor copies the link of the route 02 → 83 → 73
- **THEN** the clipboard holds a link starting with `https://knoopbyknoop.com/route/#` that opens the same route

### Requirement: Downloads from the planner
For a complete route, the planner SHALL offer to download it as a `.knoop` file and as GPX, with the same names and contents the apps write for the same route (see `route-sharing` "Share the route" and `map-planning` "Export the route as GPX").

#### Scenario: Same file as the app
- **WHEN** the visitor downloads the cycling route 12 → 40 → 34 as a route file
- **THEN** the file is named `knoop-by-knoop-cycling-12-34.knoop` and reads as the app writes this route

#### Scenario: GPX
- **WHEN** the visitor downloads the same route as GPX
- **THEN** the GPX file holds the route's line as one track and its knots as waypoints, as the apps export it

### Requirement: Open a route in the planner
The planner SHALL open a `.knoop` file chosen by the visitor or dropped on the map, and a route link opened in the browser, reading it as the apps do (see `route-sharing` "Reading a route file"), loading its route in place of the one being planned and showing it whole. Opening a route SHALL be an edit Undo can take back.

#### Scenario: Dropped file
- **WHEN** the visitor drops a `.knoop` file of the hiking route 12 → 45 on the map while planning a cycling route
- **THEN** the planner switches to hiking and shows the route 12 → 45 whole

#### Scenario: Link without the app
- **WHEN** a route link is opened on a computer
- **THEN** the planner opens with the link's route, in the browser's language where it is one of the four, else in English

#### Scenario: Not a route
- **WHEN** the visitor opens a file in which no two waypoints can be found
- **THEN** the route stays as it was, and the planner says the file holds no route it can open

### Requirement: Light and dark planner
The planner SHALL follow the browser's light or dark setting: the base map, the panel and the controls SHALL all be light or all be dark, with the dark base map as in the apps.

#### Scenario: Dark setting
- **WHEN** the visitor's system is set to dark
- **THEN** the planner shows the dark base map and a dark panel

### Requirement: No position in the planner
The planner SHALL NOT ask for the visitor's location, and SHALL send other sites only what the apps send them for the same use: the shown map area to the base map service and the typed text with the map's centre to the place search.

#### Scenario: Opening the planner
- **WHEN** a visitor opens the planner
- **THEN** the browser does not ask for permission to use the location

### Requirement: Planner start time
On a broadband connection, the planner SHALL show the base map at once and the network's knots within five seconds of opening it.

#### Scenario: First visit
- **WHEN** a visitor opens the planner for the first time on a 50 Mbit/s connection
- **THEN** the knots show within five seconds
