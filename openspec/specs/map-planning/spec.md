# map-planning Specification

## Purpose
Lets the user see a knooppunt network on a map and plan a route on it by choosing knots, using the route planning of `route-planning`.

## Requirements

### Requirement: Show the network on the map
The map SHALL show the knots of the selected, installed network type with their numbers, and the connections between them along their actual course, over a base map. The user SHALL be able to switch between the cycling and the hiking network.

#### Scenario: Cycling network of the Netherlands
- **WHEN** the cycling network is installed and the user opens the map over the Netherlands with the cycling network selected
- **THEN** the map shows the Dutch cycling knots with their numbers and their connections, continuing without a gap into Belgium and Nordrhein-Westfalen

#### Scenario: Switching to hiking
- **WHEN** the user switches from cycling to hiking
- **THEN** the map shows the hiking network instead at the same place, zoomed in far enough that knots are shown, and the current route is discarded after the user confirms

#### Scenario: Opening the map without a route
- **WHEN** the map opens and there is no route to show
- **THEN** it is zoomed in far enough that knots are shown and can be tapped

#### Scenario: Without matching geometry
- **WHEN** an installed network has no geometry that belongs to its graph, e.g. because its download failed
- **THEN** connections and the route are drawn as straight lines between knots, and planning works unchanged

#### Scenario: No network access
- **WHEN** the device has no network access
- **THEN** the knots and connections are still shown and planning works, only the base map may be missing

#### Scenario: Attribution
- **WHEN** the map is shown
- **THEN** the attribution required by OpenStreetMap and the base map provider is visible, except on iOS in landscape while the user types in the search with the on-screen keyboard, when the hits and the keyboard cover the map

### Requirement: Plan by tapping knots
Tapping a knot SHALL make it the start if there is no start yet, the end if there is no end yet, and otherwise add it as a stop into the leg where it adds the least distance. Tapping a knot that already is a waypoint SHALL change nothing, so an accidental double tap does not create a round trip and the start does not turn into a stop; round trips are made with the action to return to the start. A tap SHALL select the knot nearest to the tapped position, within a tolerance that keeps knots easy to hit.

#### Scenario: Start and end
- **WHEN** the user taps knot 12 and then knot 45
- **THEN** 12 is the start, 45 the end, and the route between them is shown on the map

#### Scenario: Adding a stop
- **WHEN** a route from 12 to 45 exists and the user taps knot 30
- **THEN** 30 is added as a stop where it adds the least distance, and the route passes through it

#### Scenario: Tapping the last waypoint again
- **WHEN** only the start 12 is set and the user taps 12 again
- **THEN** 12 stays the only waypoint, and the next tapped knot becomes the end

#### Scenario: Tapping the start of a route again
- **WHEN** a route from 12 to 45 exists and the user taps 12 again
- **THEN** the waypoints stay 12 and 45

#### Scenario: Tapping next to the network
- **WHEN** the user taps where no knot is within the tolerance
- **THEN** the route does not change

#### Scenario: Knots with equal numbers
- **WHEN** two knots 30 are close to each other, e.g. on both banks of a river, and the user taps one of them
- **THEN** exactly the tapped knot becomes part of the route

### Requirement: Show the route
The app SHALL show the planned route on the map along its actual course, together with its knot sequence and its total distance. Unreachable legs SHALL be pointed out. The knot sequence SHALL show each knot as a badge with its number, like on the map, and SHALL make the chosen waypoints clearly stand out from the knots the route only passes.

#### Scenario: Route summary
- **WHEN** the route goes from 12 via 7 to 45 over 3.5 km, with 12 and 45 as waypoints
- **THEN** the app shows 3.5 km and the badges 12, 7 and 45 in this order, with 12 and 45 highlighted as start and end and 7 shown as a passed knot

#### Scenario: Unreachable leg
- **WHEN** a waypoint cannot be reached from the previous one
- **THEN** the app marks that leg as unreachable instead of showing a route summary

### Requirement: Edit the waypoint list
The app SHALL list the waypoints in order. The user SHALL be able to move any waypoint to another position and to remove waypoints as long as two remain.

#### Scenario: Reorder
- **WHEN** the waypoints are 12, 30, 7, 45 and the user moves 7 above 30
- **THEN** the waypoints are 12, 7, 30, 45 and the route on the map follows them

#### Scenario: Remove
- **WHEN** the waypoints are 12, 30, 45 and the user removes 30
- **THEN** the waypoints are 12, 45

#### Scenario: Start over
- **WHEN** the user clears the route
- **THEN** all waypoints are removed and the next tap sets a new start

### Requirement: Keep the current route
The route being planned SHALL be restored after the app is closed and opened again.

#### Scenario: Restart
- **WHEN** the user plans a route from 12 via 30 to 45 and restarts the app
- **THEN** the same route with the same knots is shown again

#### Scenario: Knot gone after an update
- **WHEN** a waypoint's knot no longer exists in updated network data
- **THEN** the route is restored without that waypoint and the user is told which one was removed

### Requirement: Zoom controls
The map SHALL offer buttons to zoom in and out, in addition to the gestures of the platform.

#### Scenario: Zooming out
- **WHEN** the user taps the zoom-out button
- **THEN** the map zooms out by one level around its center

### Requirement: Route knots stand out
On the map, knots the route passes SHALL be highlighted, so the route can be followed in dense areas. Start, end and stops SHALL be distinguishable from each other and from passed knots.

#### Scenario: Passed knots
- **WHEN** the route from 12 to 45 passes knot 7
- **THEN** knot 7 is highlighted as part of the route, and knots the route does not pass are not

#### Scenario: Start, stop and end
- **WHEN** the route has the waypoints 12, 30 and 45
- **THEN** 12 is marked as start, 30 as stop and 45 as end, each in its own way

#### Scenario: Round trip on the map
- **WHEN** the route is a round trip with the waypoints 12, 30 and 12
- **THEN** the map marks 12 once, in a way that shows it is both start and end

### Requirement: Return to the start
The user SHALL be able to turn a route into a round trip with one action, which adds the start as the new end.

#### Scenario: Back to start
- **WHEN** the waypoints are 12, 30 and 45 and the user chooses to return to the start
- **THEN** the waypoints are 12, 30, 45 and 12

### Requirement: Route when zoomed out
Where knots are too small to show, the route SHALL be shown as its line only, without waypoint markers. Tapping the route there SHALL zoom the map to the whole route.

#### Scenario: Zoomed out
- **WHEN** a route is planned and the map is zoomed out so far that knots are not shown
- **THEN** the route is a line without markers

#### Scenario: Tapping the route when zoomed out
- **WHEN** the map is zoomed out and the user taps the route line
- **THEN** the map zooms to show the whole route

### Requirement: Riding direction
The route line SHALL show the riding direction.

#### Scenario: Direction arrows
- **WHEN** a route is shown on the map
- **THEN** arrows along its line point from the start towards the end

### Requirement: Waypoint list with badges
The waypoint list SHALL show each waypoint as a badge with its number and its role.

#### Scenario: Listing waypoints
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** the list shows the badges 12 (start), 30 (stop) and 45 (end), styled like their markers on the map

### Requirement: Landscape layout
In landscape, the app SHALL show the map and the waypoint list side by side, so both can be seen and used at the same time. In portrait, the waypoint list SHALL stay a sheet over the map.

#### Scenario: Planning in landscape
- **WHEN** the device is in landscape and the user taps knots 12 and 45 on the map
- **THEN** the map and the waypoint list are visible next to each other, and the list shows 12 as start and 45 as end right away

#### Scenario: Rotating with a route
- **WHEN** a route from 12 via 30 to 45 is planned and the user rotates the device between portrait and landscape
- **THEN** the route, the map's position and the selected network stay as they were

#### Scenario: Map controls in landscape
- **WHEN** the device is in landscape
- **THEN** the network switch, the zoom buttons, the info button and the attribution stay visible on the map, except on iOS while the user types in the search with the on-screen keyboard, see `place-search`

### Requirement: Map, knots and courses one after the other
On start, the app SHALL show the map right away, then the knots and connections of the selected knot network as soon as its graph is read, and then the courses of the connections once its geometry is read. Planning SHALL be possible as soon as the knots are shown.

#### Scenario: Normal start
- **WHEN** the knot networks and their geometry are installed and the user starts the app
- **THEN** the map is shown first, then the knots with straight connections, then the connections along their course, and no screen without a map is shown in between

#### Scenario: Planning before the courses are shown
- **WHEN** the knots are shown and the courses are not yet
- **THEN** tapping knots plans a route, which is drawn along its course as soon as the courses are shown

#### Scenario: Courses do not move the map
- **WHEN** the courses replace the straight connections
- **THEN** the map keeps its position and zoom, and the route stays as planned

### Requirement: Startup times
In a release build, the map SHALL be shown within 1 second after the app was started, and the knots of the selected knot network within 2 seconds, also on a first start without internet access. The times SHALL be measured from the tap on the app icon on the reference devices named in the design.

#### Scenario: Normal start on the reference devices
- **WHEN** the release build is started with installed knot networks and geometry
- **THEN** the map is shown within 1 second and the knots within 2 seconds

#### Scenario: First start without internet access on the reference devices
- **WHEN** the release build is installed and started for the first time in airplane mode
- **THEN** the map is shown within 1 second and the knots of the bundled graph within 2 seconds

### Requirement: Long routes in the panel
The planner panel (the sheet in portrait, the side panel in landscape) SHALL stay usable for routes of any length. The route's distance, the actions to clear the route and to return to the start, and the knot sequence SHALL stay at the top of the panel; the knot sequence SHALL be one line that scrolls sideways where it does not fit. The ferries of the route and the waypoint list SHALL scroll together below them.

#### Scenario: Long route
- **WHEN** the route passes so many knots that their badges do not fit into the panel
- **THEN** the distance, Clear, Back to start and the first badges are shown at the top, the user can swipe the badges sideways to see the rest of the route, and can scroll down through the ferries to the waypoint list

#### Scenario: Editing waypoints of a long route
- **WHEN** the route is long and the user scrolls to the waypoint list
- **THEN** the user can move and remove waypoints there as on a short route

#### Scenario: Short route
- **WHEN** the badges fit into one line and the ferries and waypoints fit into the panel
- **THEN** nothing scrolls

### Requirement: Ferries on the route
The route's knot list SHALL mark every ferry between the two knots it connects, and SHALL list the ferries of the route with their name, operating times and fee as far as they are known. A ferry without known operating times SHALL be listed as such, so the user knows to check it.

#### Scenario: Route over a ferry
- **WHEN** the route crosses a river by a ferry that runs "Apr-Oct: Mo-Su 07:00-19:00" for €1.50
- **THEN** the knot list shows a ferry between the two knots on both banks, and lists the ferry with those times and that fee

#### Scenario: Ferry without known times
- **WHEN** the route uses a ferry without operating times in OpenStreetMap
- **THEN** the ferry is listed with a note that its operating times are not known

#### Scenario: Several ferries
- **WHEN** the route uses two ferries
- **THEN** both are listed in the order the route reaches them

#### Scenario: Route without ferries
- **WHEN** the route uses no ferry
- **THEN** the knot list shows no ferries and no ferry list

#### Scenario: Offline
- **WHEN** the device has no internet access
- **THEN** the ferries and their details are shown as online
