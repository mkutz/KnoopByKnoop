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
The waypoint list SHALL show each waypoint in one line as a badge with its number and its role, each line no higher than a touch target (48 dp on Android, 44 pt on iOS). While navigating, the waypoint list SHALL NOT be shown, as the route is fixed then and the knot sequence shows it.

#### Scenario: Listing waypoints
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** the list shows the badges 12 (start), 30 (stop) and 45 (end), styled like their markers on the map

#### Scenario: Compact waypoint list
- **WHEN** a route without ferries has four waypoints and the user expands the panel on a phone in portrait
- **THEN** the list shows all four waypoints without scrolling, each in one line

#### Scenario: Waypoint list while navigating
- **WHEN** the rider navigates a route with waypoints 12, 30 and 45
- **THEN** the panel shows the knot sequence and any ferries, but no waypoint list

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
The route's knot list SHALL mark every ferry between the two knots it connects, and SHALL list the ferries of the route, each in one line with where it is on the route, the numbers of the knots on both sides in riding order, and its status, cut off with "…" at the end where it does not fit. Tapping a ferry SHALL expand it to its name, its full status, its operating times as tagged, its fee and its website as far as they are known, with a note that they are according to OpenStreetMap; tapping it again SHALL collapse it. A ferry without known operating times SHALL be listed as such, so the user knows to check it.

#### Scenario: Route over a ferry
- **WHEN** the route crosses a river by a ferry that runs "Apr-Oct: Mo-Su 07:00-19:00" for €1.50
- **THEN** the knot list shows a ferry between the two knots on both banks, and lists the ferry in one line; expanded, it shows those times and that fee

#### Scenario: Ferry without known times
- **WHEN** the route uses a ferry without operating times in OpenStreetMap
- **THEN** the ferry's line says "Operating times not known" in red, and expanded, it notes to check them before going

#### Scenario: Several ferries
- **WHEN** the route uses two ferries
- **THEN** both are listed in the order the route reaches them

#### Scenario: Route without ferries
- **WHEN** the route uses no ferry
- **THEN** the knot list shows no ferries and no ferry list

#### Scenario: Offline
- **WHEN** the device has no internet access
- **THEN** the ferries and their details are shown as online

#### Scenario: Expanding a ferry
- **WHEN** the route uses two ferries and the user taps the first one
- **THEN** the first one shows its name, status, times, fee and website and the note "according to OpenStreetMap", the second one stays one line, and tapping the first one again collapses it

#### Scenario: Where the ferry is
- **WHEN** the route goes from 76 across Renkumse Veer to 02 and on to 20
- **THEN** the ferry's line starts with "⛴ 76 → 02", and its name "Renkumse Veer" is shown when it is expanded

### Requirement: Ferry open now
The ferry list SHALL show for each ferry with known operating times, except for the ferries ahead while navigating (see `knot-navigation` "Ferry arrival estimate"), whether it is open now according to OpenStreetMap and, where that can be told too, when that changes next, in the local time of the ferry's region. The status SHALL be shown in the ferry's line and the operating times as tagged when it is expanded, and the status SHALL stay current while the list is shown. When the operating times do not allow a sure answer, the ferry SHALL show no status.

#### Scenario: Open now
- **WHEN** a ferry runs "Apr-Oct: Mo-Su 07:00-19:00" and it is Wednesday, 16 June 2027, 14:00
- **THEN** the ferry's line shows "Open until 19:00", and expanded, "Apr-Oct: Mo-Su 07:00-19:00" below it

#### Scenario: Opens later today
- **WHEN** the same ferry is shown on Wednesday, 16 June 2027, 06:15
- **THEN** it shows "Closed until 07:00"

#### Scenario: Opens tomorrow
- **WHEN** the same ferry is shown on Wednesday, 16 June 2027, 20:00
- **THEN** it shows "Closed until tomorrow 07:00"

#### Scenario: Closed for the season
- **WHEN** the same ferry is shown on Wednesday, 2 December 2026
- **THEN** it shows "Closed until 1 April 07:00"

#### Scenario: Open past midnight
- **WHEN** a ferry runs "Mo-Th 06:00-24:00; Fr-Su 06:00-01:00" and it is Saturday, 19 June 2027, 00:30
- **THEN** it shows "Open until 01:00"

#### Scenario: Open around the clock
- **WHEN** a ferry runs `24/7; "Not in ice conditions"`
- **THEN** it shows "Open", and expanded, the tagged times with the comment

#### Scenario: Public holiday
- **WHEN** a ferry runs "Mo-Sa 06:00-19:00; PH,Su 09:00-19:00" and it is Christmas Day, Friday, 25 December 2026, 08:00
- **THEN** it shows "Closed until 09:00"

#### Scenario: Holiday in only some countries
- **WHEN** the same ferry is shown on Good Friday, 26 March 2027, 08:00, a public holiday in Nordrhein-Westfalen but not in the Netherlands or Belgium
- **THEN** it shows no status, only the tagged times

#### Scenario: School holidays
- **WHEN** a ferry runs "Sa,Su,PH 09:30-19:40; Mar-Sep SH 09:30-19:40" and it is Wednesday, 16 June 2027, 10:00
- **THEN** it shows no status, only the tagged times

#### Scenario: Unreadable times
- **WHEN** a ferry's operating times are tagged as "April-October"
- **THEN** it shows no status, only the tagged times

#### Scenario: Phone in another time zone
- **WHEN** a ferry runs "Apr-Oct: Mo-Su 07:00-19:00" and the phone is set to London time, where it is 18:30 on Wednesday, 16 June 2027 (19:30 in the Netherlands)
- **THEN** it shows "Closed until tomorrow 07:00"

#### Scenario: Closing while shown
- **WHEN** the ferry list is shown at 18:59 for a ferry that closes at 19:00
- **THEN** its status changes to closed within a minute after 19:00, without any action

#### Scenario: Offline status
- **WHEN** the device has no internet access
- **THEN** the status, including public holidays, is shown as online

### Requirement: Clear of the system bars
The map SHALL extend behind the system's status bar and navigation bar, while the status bar's and navigation bar's icons SHALL stay readable on it in light and dark appearance. The controls over the map and the planner panel SHALL stay clear of the system bars and the display cutout, so all of their area can be tapped.

#### Scenario: Status bar on the light map
- **WHEN** the app shows the light map
- **THEN** the clock, the status icons and the notification icons are visible in dark colors above the map

#### Scenario: Status bar in dark mode
- **WHEN** the system is in dark mode, also after switching to it while the app is open
- **THEN** the status bar's and navigation bar's icons are light

#### Scenario: Tapping the top of the search field
- **WHEN** the user taps the search field near its upper edge
- **THEN** the search field gets the focus and the keyboard opens

#### Scenario: Landscape with a display cutout
- **WHEN** the phone is in landscape with the camera cutout on the left or the right
- **THEN** the search field, the network switch, the zoom buttons, the attribution and the side panel are not covered by the cutout or the navigation bar

### Requirement: Clear actions in the planner panel
The planner panel SHALL show Navigate, and Stop while navigating, as the most prominent actions, each with an icon and its label. Back to start and Clear SHALL be shown as icon buttons that are announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** the header shows the distance, Back to start and Clear as icon buttons, and Navigate as a filled button with an icon and the label "Navigate", in one row

#### Scenario: Stop stands out
- **WHEN** the rider is navigating
- **THEN** the header shows Stop as a filled red button with an icon and the label "Stop"

#### Scenario: Icon buttons with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) moves through the header of a planned round trip
- **THEN** it announces "Back to start", "Clear" and "Navigate" as buttons

#### Scenario: Name of an icon button on Android
- **WHEN** the user long-presses Clear on Android
- **THEN** the name "Clear" is shown

#### Scenario: Readable actions
- **WHEN** the actions are measured against the panel's background
- **THEN** the labels of Navigate and Stop have a contrast of at least 4.5:1 to their fill, and the icons of all four at least 3:1 to their background

#### Scenario: Readable actions in dark mode
- **WHEN** the actions are measured against the dark panel's background
- **THEN** the labels of Navigate and Stop have a contrast of at least 4.5:1 to their fill, and the icons of all four at least 3:1 to their background

### Requirement: Light and dark appearance
The app SHALL follow the system's light or dark appearance: the base map, the planner panel and the controls over the map SHALL all be light or all be dark. Switching the appearance while the app is open SHALL switch the app without moving the map or changing the route, and without interrupting navigation. The knots, the route and the highlighted knots SHALL stay distinguishable on the dark map.

#### Scenario: Dark mode
- **WHEN** the system is set to dark mode and the user opens the app
- **THEN** the base map, the planner panel, the search, the network switch and the zoom controls are dark

#### Scenario: Light mode
- **WHEN** the system is set to light mode
- **THEN** the app looks as before this change

#### Scenario: Switching while the app is open
- **WHEN** the user plans a route, zooms into it and switches the system to dark mode
- **THEN** the app turns dark, and the map shows the same area with the same route and waypoints

#### Scenario: Returning to the app
- **WHEN** the system is in dark mode and the user switches to another app and back, again and again
- **THEN** the map stays dark every time

#### Scenario: Switching while navigating
- **WHEN** the rider is navigating and the system switches to dark mode, e.g. automatically at sunset
- **THEN** navigation keeps running and announcing knots, and the app turns dark

#### Scenario: Route on the dark map
- **WHEN** a route with start, stops, end and ferries is shown on the dark map
- **THEN** start, stops, end, the passed knots, the ferries and the route line are as distinguishable as on the light map

#### Scenario: Offline in dark mode
- **WHEN** the base map cannot be loaded, e.g. offline on first start, and the system is in dark mode
- **THEN** the map shows the knot network on a dark background

#### Scenario: Lock screen in dark mode
- **WHEN** the rider navigates with the screen locked and the system is in dark mode
- **THEN** the navigation's notification or Live Activity on the lock screen is dark, with the number to follow readable

#### Scenario: Slow base map
- **WHEN** the app is online but the base map takes longer to load than the app waits for it, e.g. on the first start in dark mode
- **THEN** the map shows the knot network on the plain background first, and the base map replaces it as soon as it has loaded, without moving the map
