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
- **THEN** the knots and connections are still shown and planning works; the base map may be missing, except around the planned route once its map is stored, see `network-sync` "Base map around the route"

#### Scenario: Riding without a connection
- **WHEN** the base map of the planned route is stored and the rider navigates it with no network access
- **THEN** streets, villages, rivers and woods are shown along the route, in the light and in the dark map

#### Scenario: Attribution
- **WHEN** the map is shown
- **THEN** the attribution required by OpenStreetMap and the base map provider is visible, except on iOS in landscape while the user types in the search with the on-screen keyboard, when the hits and the keyboard cover the map

### Requirement: Plan by tapping knots
Tapping a knot SHALL make it the start if there is no start yet, the end if there is no end yet, and otherwise add it where it adds the least distance: as a stop in one of the legs, before the start, making it the new start and the old start a stop, or after the end, making it the new end and the old end a stop. A round trip SHALL stay one, so its knots are only added as stops. A knot that cannot be reached from the route SHALL be added before the end, so the route reports the leg as unreachable. Tapping a knot that already is a waypoint SHALL change nothing, so an accidental double tap does not create a round trip and the start does not turn into a stop; round trips are made with the action to return to the start. A tap SHALL select the knot nearest to the tapped position, within a tolerance that keeps knots easy to hit.

#### Scenario: Start and end
- **WHEN** the user taps knot 12 and then knot 45
- **THEN** 12 is the start, 45 the end, and the route between them is shown on the map

#### Scenario: Adding a stop
- **WHEN** a route from 12 to 45 exists and the user taps knot 30, which lies beside it
- **THEN** 30 is added as a stop where it adds the least distance, and the route passes through it

#### Scenario: Beyond the end
- **WHEN** a route from 12 to 45 exists and the user taps knot 28, which lies beyond 45 seen from 12
- **THEN** the waypoints are 12, 45 and 28, with 28 as the end and 45 as a stop

#### Scenario: Before the start
- **WHEN** a route from 05 in Amsterdam to 76 in Zandvoort exists and the user taps 60, which lies on the far side of 05 seen from 76
- **THEN** the waypoints are 60, 05 and 76, with 60 as the start and 05 as a stop

#### Scenario: Extending step by step
- **WHEN** the user taps 12, 45, 28 and 7, each further along in the same direction
- **THEN** the waypoints are 12, 45, 28 and 7 in that order

#### Scenario: Knot beyond the end of a round trip
- **WHEN** the waypoints are 12, 45 and 12 and the user taps knot 28
- **THEN** 28 becomes a stop where it adds the least distance, neither the new start nor the new end, and the route stays a round trip from 12

#### Scenario: Tapping a knot that cannot be reached
- **WHEN** a route from 12 to 45 exists and the user taps a knot that cannot be reached from it
- **THEN** it is added before 45, and the route reports the leg as unreachable

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
The app SHALL show the planned route on the map along its actual course, together with its knot sequence, its total distance and the time it takes at an assumed pace of 16 km/h cycling and 4.5 km/h hiking, rounded as in `knot-navigation` "Rounded times and distances". Unreachable legs SHALL be pointed out. The knot sequence SHALL show each knot as a badge with its number, like on the map, and SHALL make the chosen waypoints clearly stand out from the knots the route only passes.

#### Scenario: Route summary
- **WHEN** the route goes from 12 via 7 to 45 over 3.5 km, with 12 and 45 as waypoints
- **THEN** the app shows 3.5 km with "about 13 min" below it, and the badges 12, 7 and 45 in this order, with 12 and 45 highlighted as start and end and 7 shown as a passed knot

#### Scenario: Unreachable leg
- **WHEN** a waypoint cannot be reached from the previous one
- **THEN** the app marks that leg as unreachable instead of showing a route summary

#### Scenario: Hiking time
- **WHEN** a hiking route is 3.5 km long
- **THEN** the app shows 3.5 km with "about 47 min" below it

#### Scenario: Long route time
- **WHEN** a cycling route is 40.0 km long
- **THEN** the app shows 40 km with "about 2 h 30 min" below it

### Requirement: Edit the waypoint list
The app SHALL list the waypoints in order. The user SHALL be able to move any waypoint to another position and to remove any waypoint; removing one of the last two SHALL leave the other as the start. Each row SHALL show a drag handle for moving and a remove button, so neither depends on a hidden gesture; screen readers SHALL offer moving up, moving down and removing as actions on the row.

#### Scenario: Reorder
- **WHEN** the waypoints are 12, 30, 7, 45 and the user moves 7 above 30
- **THEN** the waypoints are 12, 7, 30, 45 and the route on the map follows them

#### Scenario: Remove
- **WHEN** the waypoints are 12, 30, 45 and the user removes 30
- **THEN** the waypoints are 12, 45

#### Scenario: Start over
- **WHEN** the user clears the route
- **THEN** all waypoints are removed and the next tap sets a new start

#### Scenario: Visible handle
- **WHEN** the planner lists the waypoints 12, 30 and 45
- **THEN** each row shows a drag handle at its end

#### Scenario: Moving by the handle
- **WHEN** the user drags the handle of 7 above 30, without a long press first
- **THEN** the waypoints are 12, 7, 30, 45

#### Scenario: Long press still moves
- **WHEN** the user long-presses the row of 7 and drags it above 30
- **THEN** the waypoints are 12, 7, 30, 45

#### Scenario: Visible remove button
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** each row shows a remove button, and tapping the one of 30 leaves 12, 45

#### Scenario: Removing one of two waypoints
- **WHEN** only the waypoints 12 and 45 are left and the user removes 45
- **THEN** 12 is left as the start, and the next tap sets the end

#### Scenario: Reordering with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) is on the row of 30, the second of 12, 30, 45
- **THEN** it offers the actions "Move up", "Move down" and "Remove", and "Move down" makes the waypoints 12, 45, 30

### Requirement: Keep the current route
The route being planned SHALL be restored after the app is closed and opened again.

#### Scenario: Restart
- **WHEN** the user plans a route from 12 via 30 to 45 and restarts the app
- **THEN** the same route with the same knots is shown again

#### Scenario: Knot gone after an update
- **WHEN** a waypoint's knot no longer exists in updated network data
- **THEN** the route is restored without that waypoint and the user is told which one was removed

### Requirement: Zoom controls
The map SHALL offer buttons to zoom in and out, in addition to the gestures of the platform, together as one control: zoom in above zoom out, on one background, as in "Map buttons in groups".

#### Scenario: Zooming out
- **WHEN** the user taps the zoom-out button
- **THEN** the map zooms out by one level around its center

#### Scenario: Zoom buttons as one control
- **WHEN** the map is shown
- **THEN** zoom in and zoom out sit on one shared background, separated by a thin line, and a screen reader announces them as "Zoom in" and "Zoom out"

### Requirement: Route knots stand out
On the map, knots the route passes SHALL be highlighted, so the route can be followed in dense areas. Start, end and stops SHALL be distinguishable from each other and from passed knots.

A knot's badge SHALL show its plate in the colour of the knot's sign region (see `network-data` "Sign region of each knot"): red for North Rhine-Westphalia, dark green for the Netherlands, blue for Belgium, and the app's green for a knot without a region. The badge SHALL show the knot's role by its fill:
- **Waypoint** (start, stop, end or round trip): a filled plate with a white number. Knots in North Rhine-Westphalia and Belgium SHALL also have a white ring inside the plate, as on their signs. Dutch knots SHALL have none.
- **Passed knot:** white, with one ring and the number in the plate colour.
- **Knot not on the route:** a grey ring and number, whatever its region.

A small dark symbol at the corner of a waypoint's badge SHALL tell the roles apart: ▶ for the start, a flag for a stop, ■ for the end, and a circular arrow for the start and end of a round trip. The waypoint list, the progress strip and the planner panel SHALL show badges as on the map. Red SHALL NOT be used on the map or in the planner panel to mark a problem, so it never reads like a German knot.

#### Scenario: Passed knots
- **WHEN** the route from 12 to 45 passes knot 7
- **THEN** knot 7 is highlighted as part of the route, and knots the route does not pass are not

#### Scenario: Start, stop and end
- **WHEN** the route has the waypoints 12, 30 and 45
- **THEN** 12 is marked as start, 30 as stop and 45 as end, each in its own way

#### Scenario: Round trip on the map
- **WHEN** the route is a round trip with the waypoints 12, 30 and 12
- **THEN** the map marks 12 once, in a way that shows it is both start and end

#### Scenario: Route in North Rhine-Westphalia
- **WHEN** a cycling route around Troisdorf starts at 25, passes 32 and ends at 29
- **THEN** 25 and 29 are red plates with a white ring and number, 25 with ▶ and 29 with ■ at the corner, and 32 is white with a red ring and number

#### Scenario: Route in the Netherlands
- **WHEN** a cycling route in Limburg starts at 95 and passes 1
- **THEN** 95 is a dark green plate with a white number and no ring, and 1 is white with a dark green ring and number

#### Scenario: Route in Belgium
- **WHEN** a cycling route in Haspengouw starts at 150
- **THEN** 150 is a blue plate with a white ring and number

#### Scenario: Route across the border
- **WHEN** a route starts at a Dutch knot and ends at a Belgian knot
- **THEN** the start is a dark green plate and the end a blue plate

#### Scenario: Knots not on the route
- **WHEN** a route in North Rhine-Westphalia is shown and knot 77 is not on it
- **THEN** 77 has a grey ring and number, not red

#### Scenario: Knot without a region
- **WHEN** a hiking route starts at knot 12
- **THEN** 12 is a plate in the app's green

#### Scenario: Data from before sign regions
- **WHEN** the installed network data has no sign regions
- **THEN** every route knot shows the app's green, with the roles shown by fill and corner symbol as above

#### Scenario: Off route on the map
- **WHEN** the rider is off route and the map shows the line to the route or a guided way
- **THEN** the line is dark, not red

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
The waypoint list SHALL show each waypoint in one line as a badge with its number and its role, followed by the place the knot lies in where it has one (see `network-data` "Place of each knot"), each line no higher than a touch target (48 dp on Android, 44 pt on iOS). The place SHALL be named as on its signpost, followed by its name in the app's language in brackets where that differs; a name too long for the line SHALL be cut short. While navigating, the waypoint list SHALL NOT be shown, as the route is fixed then and the knot sequence shows it.

#### Scenario: Listing waypoints
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** the list shows the badges 12 (start), 30 (stop) and 45 (end), styled like their markers on the map

#### Scenario: Compact waypoint list
- **WHEN** a route without ferries has four waypoints and the user expands the panel on a phone in portrait
- **THEN** the list shows all four waypoints without scrolling, each in one line

#### Scenario: Waypoint list while navigating
- **WHEN** the rider navigates a route with waypoints 12, 30 and 45
- **THEN** the panel shows the knot sequence and any ferries, but no waypoint list

#### Scenario: Place in the waypoint list
- **WHEN** the start 06 lies in Lohmar and the end 28 in Sankt Augustin
- **THEN** the list shows "Start · Lohmar" beside 06 and "End · Sankt Augustin" beside 28

#### Scenario: Waypoint in the open
- **WHEN** the stop 02 carries no place
- **THEN** its line shows its role only

#### Scenario: Place name in the app's language
- **WHEN** the app's language is German and the end lies in Liège
- **THEN** its line shows "End · Liège (Lüttich)", in German "Ende · Liège (Lüttich)"

### Requirement: Landscape layout
In landscape, the app SHALL show the map and the waypoint list side by side, so both can be seen and used at the same time. While searching, the search field and its hits SHALL be at the top of the side panel, on both apps, see `place-search`, and the map SHALL keep its width. In portrait, the waypoint list SHALL stay a sheet over the map.

#### Scenario: Planning in landscape
- **WHEN** the device is in landscape and the user taps knots 12 and 45 on the map
- **THEN** the map and the waypoint list are visible next to each other, and the list shows 12 as start and 45 as end right away

#### Scenario: Rotating with a route
- **WHEN** a route from 12 via 30 to 45 is planned and the user rotates the device between portrait and landscape
- **THEN** the route, the map's position and the selected network stay as they were

#### Scenario: Map controls in landscape
- **WHEN** the device is in landscape
- **THEN** the network switch, the zoom buttons, the magnifier button, the info button and the attribution stay visible on the map, also while the user types in the search

#### Scenario: Typing in the search in landscape
- **WHEN** an Android device is in landscape with a route planned, and the user taps the magnifier button and types "Amsterdam" with the on-screen keyboard
- **THEN** the field and the hits show at the top of the side panel, the map keeps its width with the route on it, and picking a hit or Cancel brings the waypoints back with the same route

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
The route's knot list SHALL mark every ferry between the two knots it connects, and, while planning, SHALL list the ferries of the route, each in one line with where it is on the route, the numbers of the knots on both sides in riding order, and its status, cut off with "…" at the end where it does not fit. Tapping a ferry SHALL expand it to its name, its full status, its operating times as tagged, its fee and its website as far as they are known, with a note that they are according to OpenStreetMap; tapping it again SHALL collapse it. A ferry without known operating times SHALL be listed as such, so the user knows to check it.

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

#### Scenario: Ferries while navigating
- **WHEN** the rider starts navigating a route with ferries
- **THEN** the ferry list is replaced by the ferries on the progress strip, see `knot-navigation` "Ferries on the strip"

### Requirement: Ferry open now
The ferry list while planning, and the popover of a passed ferry while navigating, SHALL show for each ferry with known operating times whether it is open now according to OpenStreetMap and, where that can be told too, when that changes next, in the local time of the ferry's region. The status SHALL be shown in the ferry's line and the operating times as tagged when it is expanded, and the status SHALL stay current while the list is shown. When the operating times do not allow a sure answer, the ferry SHALL show no status.

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
The map SHALL extend behind the system's status bar and navigation bar, while the status bar's and navigation bar's icons SHALL stay readable on it in light and dark appearance. The controls over the map, including the map's compass, and the planner panel SHALL stay clear of the system bars and the display cutout, so all of their area can be tapped and the status icons stay visible.

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

#### Scenario: Compass below the status bar
- **WHEN** the map is rotated, so its compass button shows
- **THEN** the compass is below the search field, and the clock and the status icons, the battery among them, are not covered

### Requirement: Clear actions in the planner panel
Navigate SHALL be the most prominent action, a filled round icon button with the navigation arrow and without a label: in the planner panel's header on iOS, and as a floating action button at the panel's top right edge on Android, moving with the panel. In landscape it SHALL be at the end of the side panel's header on both. While navigating, the header SHALL show Stop as a filled round icon button of the same size in every app language, announced as "Stop" to screen readers and showing its name on a long press on Android. Back to start, with an icon and its label, Export GPX and Clear, as icon buttons, SHALL be shown in one row in the panel below the route's knots, each only when available; there SHALL be no "More actions" menu. The header SHALL stay one row on phones 360 dp wide. Icon buttons SHALL be announced by name to screen readers. In light and in dark appearance, the labels of all these actions SHALL have a contrast of at least 4.5:1, and their icons at least 3:1, against what they are shown on.

#### Scenario: Navigate stands out
- **WHEN** a complete route is planned
- **THEN** Navigate shows as a filled round button with the navigation arrow, without a label, next to the distance on iOS and as a floating action button at the panel's top right on Android; below the knots, the panel shows Back to start with its label, if the route is not a round trip yet, then Export GPX and Clear as icon buttons, in one row, also 360 dp wide

#### Scenario: Stop stands out
- **WHEN** the rider is navigating
- **THEN** the header shows Stop as a filled red icon button with the stop icon, without a label

#### Scenario: Stop in every language
- **WHEN** the rider navigates with the app in English and then in German
- **THEN** Stop has the same size and place in the header

#### Scenario: Stop with a screen reader
- **WHEN** a screen reader reaches Stop while navigating
- **THEN** it announces "Stop" as a button

#### Scenario: Icon buttons with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) moves through the panel of a planned route
- **THEN** it announces "Navigate", "Back to start", "Export GPX" and "Clear" as buttons

#### Scenario: Name of an icon button on Android
- **WHEN** the user long-presses Clear on Android
- **THEN** the name "Clear" is shown

#### Scenario: Readable actions
- **WHEN** the actions are measured against the panel's background
- **THEN** the icons of Navigate and Stop have a contrast of at least 3:1 to their fill, the label of Back to start at least 4.5:1, and the icons of the icon buttons at least 3:1 to their background

#### Scenario: Readable actions in dark mode
- **WHEN** the actions are measured against the dark panel's background
- **THEN** the icons of Navigate and Stop have a contrast of at least 3:1 to their fill, the label of Back to start at least 4.5:1, and the icons of the icon buttons at least 3:1 to their background

#### Scenario: Navigate in landscape
- **WHEN** a complete route is planned in landscape
- **THEN** Navigate is the round icon button at the end of the side panel's header, on Android and iOS

### Requirement: Light and dark appearance
The app SHALL follow the appearance chosen in the info sheet, by default the system's light or dark appearance: the base map, the planner panel and the controls over the map SHALL all be light or all be dark. The dark base map SHALL show the same details as the light one, with water in blue and paths and roads visible against the land. Switching the appearance while the app is open SHALL switch the app without moving the map or changing the route, and without interrupting navigation. On Android, an appearance chosen in the info sheet SHALL reach the base map when the sheet closes, so the sheet's own switch stays smooth. The knots, the route and the highlighted knots SHALL stay distinguishable on the dark map.

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

#### Scenario: Details on the dark map
- **WHEN** the dark map shows an area with a river, a park, footpaths and cycleways
- **THEN** the river is blue, the park is green, and the footpaths and cycleways are drawn as on the light map, in colors visible on the dark land

#### Scenario: Choosing light
- **WHEN** the system is in dark mode and the user chooses Light in the info sheet
- **THEN** the base map, the planner panel, the controls and the status bar turn light, without moving the map or changing the route

#### Scenario: Choosing dark
- **WHEN** the system is in light mode and the user chooses Dark
- **THEN** the app turns dark

#### Scenario: Back to the system
- **WHEN** the user chooses System again
- **THEN** the app follows the system's appearance, also when it switches later

#### Scenario: Choice kept
- **WHEN** the user chose Light and starts the app again
- **THEN** the app is light, whatever the system's appearance

#### Scenario: Off route in dark mode
- **WHEN** the rider is off route in dark mode
- **THEN** the panel's header is light with dark text and arrow, and the rider's dot on the strip is a ring with ✕, readable on the dark panel, and none of them is red

#### Scenario: Off route in light mode
- **WHEN** the rider is off route in light mode
- **THEN** the panel's header is dark with white text and arrow, and the knot badges in it keep their plate colours

#### Scenario: Knots on the dark map
- **WHEN** a route in North Rhine-Westphalia is shown on the dark map
- **THEN** the waypoints are red plates, the passed knots white with a red ring, and the knots not on the route a grey ring and number on a dark disc

#### Scenario: Choosing the appearance in the info sheet
- **WHEN** the user opens the info sheet on Android and switches between Light and Dark several times
- **THEN** the sheet turns light or dark at once without stalling, and the base map follows the last choice when the sheet closes, showing the same area and route

### Requirement: Show my position
While planning, the map SHALL offer a button, in one group with the nearest knots button above the zoom buttons, see "Map buttons in groups", that centers the map on the rider's current position, at least at the zoom where knots are shown, and marks it with a dot that does not follow the rider. Location access SHALL be asked for on the first tap, and on the very first start of the app, when there is neither a saved map position nor a route. The position SHALL be found once per request, without updates afterwards.

#### Scenario: Centering on my position
- **WHEN** location access is allowed and the user taps the position button
- **THEN** the map centers on the user's position with the knots visible, and a dot marks the position

#### Scenario: First tap asks
- **WHEN** location access was never asked for and the user taps the position button
- **THEN** the app asks for access while using the app, and centers on the position once it is allowed

#### Scenario: First start
- **WHEN** the app starts for the first time, with no saved map position and no route
- **THEN** it asks for location access and, if allowed, centers on the user's position

#### Scenario: Later starts
- **WHEN** the app starts with a saved map position or a route
- **THEN** it does not ask for location access and shows the saved map position or the route as before

#### Scenario: Access denied
- **WHEN** location access is denied and the user taps the position button
- **THEN** the map does not move and the app says that it needs location access to show the position, and where to allow it

#### Scenario: No position found
- **WHEN** no position is found within 15 seconds
- **THEN** the map does not move and the app says that the position could not be found

#### Scenario: No tracking while planning
- **WHEN** the map was centered on the position and the user moves on
- **THEN** the dot stays where it was, the app takes no further positions, and the system shows no location indicator after the position was found

#### Scenario: Not while navigating
- **WHEN** the rider is navigating
- **THEN** the position button is not shown, as the map follows the rider and offers its recenter button

### Requirement: Export the route as GPX
The app SHALL export a complete planned route as a GPX 1.1 file and offer it in the system share sheet. The file SHALL contain the route's line along the knot network as one track, and each knot passed, in riding order, as a waypoint named with its number and typed as start, stop, end or knot. Export SHALL be offered only for a complete route whose line is known, not while navigating, and also offline.

#### Scenario: Export a route
- **WHEN** the user plans a cycling route from knot 12 through stop 40 to knot 34 and chooses Export GPX in the header's menu
- **THEN** the share sheet opens with a file `knoop-by-knoop-cycling-12-34.gpx`

#### Scenario: Track along the network
- **WHEN** the exported file of a route is read
- **THEN** it is valid GPX 1.1 with one track whose points follow the route's line along the network from the start knot to the end knot

#### Scenario: Knots as waypoints
- **WHEN** the exported file of a route from 12 through 40 to 34 is read
- **THEN** it has a waypoint for every knot passed, in riding order, named with its number, and 12 is typed as start, 40 as stop and 34 as end

#### Scenario: Round trip
- **WHEN** a round trip from knot 12 back to 12 is exported
- **THEN** the first waypoint is 12 typed as start, the last is 12 typed as end, and the track starts and ends at knot 12

#### Scenario: No export without a complete route
- **WHEN** only a start is set, or the route's line is not loaded yet
- **THEN** Export GPX is not offered

#### Scenario: No export while navigating
- **WHEN** the rider is navigating
- **THEN** Export GPX is not shown

#### Scenario: Export while offline
- **WHEN** the device is offline and a complete route with its line is planned
- **THEN** Export GPX opens the share sheet with the file

### Requirement: Knot badges at large text sizes
The knot badges in the panel SHALL show their numbers at the same size whatever the system text size, like the badges on the map, so a number always fits within its badge.

#### Scenario: Badge at the largest text size
- **WHEN** the system text size is at its largest and the panel shows the badge of knot 94
- **THEN** the badge shows "94" completely within its circle, at the same size as at the default text size

### Requirement: Route summary at large text sizes
At large system text sizes, the planner panel SHALL show the route's distance and time each in one line and completely. Where they do not fit next to the panel's actions, the actions SHALL be shown below them.

#### Scenario: Route summary at the largest text size
- **WHEN** the system text size is at its largest and the route is 4.1 km, about 15 min
- **THEN** the panel shows "4.1 km" and "about 15 min" each in one line and completely, with the actions below them

#### Scenario: Route summary at the default text size
- **WHEN** the system text size is the default
- **THEN** the distance and time are next to the actions, as before

### Requirement: Nearest knots
While planning, when the map shows no knots of the selected network, because there are none in its area or it is zoomed out too far for knots to show, the map SHALL offer a button below the position button, in the same group, that shows the nearest knots. With no waypoints set, the panel's hint SHALL say that no knots are in view and its "Nearest knots" button SHALL be the prominent action, see "Panel before a route".

#### Scenario: No knots in the area
- **WHEN** the map shows an area without knots and no waypoints are set
- **THEN** the map shows the button, and the panel's hint says "No knots in view" with "Nearest knots" as the prominent button

#### Scenario: Zoomed out too far
- **WHEN** the map is zoomed out so far that knots are not shown
- **THEN** the map shows the button

#### Scenario: Knots in view
- **WHEN** the map shows at least one knot
- **THEN** it does not show the button, and with no waypoints set, the panel's hint says "Tap a numbered knot on the map for the start."

#### Scenario: With waypoints set
- **WHEN** a start is set and the user moves the map to an area without knots
- **THEN** the map shows the button, and the panel still shows the route's state

#### Scenario: From the rider's position
- **WHEN** location access is allowed, the rider is 8 km from the nearest knot, and they tap the button
- **THEN** the map shows both their position, marked with a dot, and that knot, with the knots shown

#### Scenario: Clear of the controls
- **WHEN** the map moves to show the rider's position and the nearest knot
- **THEN** neither is hidden behind the search field, the network switch or the panel, and the button is gone, as a knot is in view

#### Scenario: Without location access
- **WHEN** location access was not allowed and the user taps the button
- **THEN** the app does not ask for it, and the map shows the knot nearest to the middle of the map

#### Scenario: No position found
- **WHEN** location access is allowed but no position is found within 15 seconds
- **THEN** the map shows the knot nearest to the middle of the map

#### Scenario: Far from the network
- **WHEN** the rider is so far from the nearest knot that the knots would not show with both in view
- **THEN** the map shows that knot, with the knots shown

#### Scenario: Other network
- **WHEN** hiking is selected and the nearest knot is sought
- **THEN** the map shows the nearest hiking knot, not a cycling one

#### Scenario: Not while navigating
- **WHEN** the rider is navigating
- **THEN** the button is not shown

#### Scenario: Nearest knots with a screen reader
- **WHEN** a screen reader reaches the button
- **THEN** it announces "Show the nearest knots" as a button

### Requirement: Icons on the network switch
Each choice of the network switch SHALL show an icon next to its word: a bike for cycling and a hiker for hiking. The icon SHALL have the color of the word. The switch SHALL behave as without the icons.

#### Scenario: The icons
- **WHEN** the network switch is shown
- **THEN** cycling shows a bike next to its word, and hiking a hiker next to its word

#### Scenario: Selected network
- **WHEN** the user switches from cycling to hiking without a route
- **THEN** hiking shows as selected, with its icon and word, and cycling as not selected

#### Scenario: Switching with a route
- **WHEN** a route exists and the user taps the other network
- **THEN** the app asks first, and the selection only changes after the user confirms

#### Scenario: On a narrow phone
- **WHEN** the switch is shown on a phone 360 dp wide, in English, German, Dutch or French
- **THEN** both words and icons and the info button show in full, on one row

#### Scenario: Larger text
- **WHEN** the text is at 130% on a phone 360 dp wide, in German and in French
- **THEN** both words can be read in full, and the info button can be reached

#### Scenario: Very large text
- **WHEN** the text is larger than 130%, up to the largest size, on a phone 360 dp wide in German
- **THEN** both words stay on one line, and the info button can be reached, by scrolling the switch if it does not fit

#### Scenario: In landscape
- **WHEN** the app is in landscape
- **THEN** the switch shows the icons and does not cover the search field or the side panel

#### Scenario: Screen reader
- **WHEN** a screen reader reaches the switch
- **THEN** it announces the words of the two choices and which one is selected, and not the icons

#### Scenario: Readable in both appearances
- **WHEN** the switch is measured in light and in dark appearance
- **THEN** its words and icons have a contrast of at least 4.5:1 to what they are shown on

### Requirement: Knots signposted at several places
A knot signposted at several places of one junction (see `network-data` "One knot per signposted junction") SHALL have its badge on the map at every one of them. The badges SHALL act as that one knot for tapping and highlighting; everywhere else the knot SHALL appear once. A route starting or ending at such a knot SHALL be drawn and exported from or to the signpost it uses.

#### Scenario: Badges at both signposts
- **WHEN** knot 02 is signposted at two places 195 m apart and the map shows them
- **THEN** a badge 02 is shown at each of the two places

#### Scenario: Tapping the other signpost
- **WHEN** no start is set and the user taps the badge 02 that is not the knot's main position
- **THEN** 02 becomes the start, the same knot as when tapping the other badge, and the waypoint list, the strip and the coming knots show 02 once

#### Scenario: Route through a knot with two signposts
- **WHEN** the route passes 02
- **THEN** both badges 02 are highlighted as part of the route

#### Scenario: Route starting at the other signpost
- **WHEN** the route starts at 02 and leaves it at the signpost near the L 16, 195 m from the knot's main position
- **THEN** its line starts at that signpost, without the path between the two badges, as a route leaving at the main position starts there

#### Scenario: Route ending at the other signpost
- **WHEN** the route ends at 02 and arrives at the signpost near the L 16
- **THEN** its line ends at that signpost

#### Scenario: Route through a knot between its signposts
- **WHEN** the route passes 02, arriving at one signpost and leaving at the other
- **THEN** its line includes the path between the two badges

#### Scenario: Next knot with two signposts
- **WHEN** the rider is navigating with 02 as the next knot
- **THEN** both badges 02 have the next-knot ring

#### Scenario: Knots with equal numbers on both banks
- **WHEN** two knots 30 on both banks of a river are separate knots, connected by a ferry
- **THEN** each has its own badge, and tapping one selects only that knot, as before

### Requirement: Routes through a knot with several signposts
A route passing a knot signposted at several places SHALL be drawn and exported through the signpost that fits the legs before and after it, the shortest way where several fit. Where the legs share no signpost, the line SHALL join them along the path between the signposts. The route's knots, distance and time SHALL NOT change.

#### Scenario: Through the signpost of the next leg
- **WHEN** the route goes 06 → 02 → 28, 02 → 28 leaves at the signpost near the L 16, and 06 → 02 reaches both signposts
- **THEN** its line goes straight to the signpost near the L 16 and on to 28, without the 02 by the river

#### Scenario: The other direction out of the knot
- **WHEN** the route goes 06 → 02 → 01, and 02 → 01 leaves at the 02 by the river
- **THEN** its line goes to the 02 by the river and on to 01, without the signpost near the L 16

#### Scenario: A stop at the knot
- **WHEN** 02 is a stop of the route 06 → 02 → 28
- **THEN** its line is the same as when the route only passes 02

#### Scenario: Back the same way
- **WHEN** the route goes 28 → 02 → 28, and 28 reaches 02 only at the signpost near the L 16
- **THEN** its line turns at that signpost, without the path to the 02 by the river and back

#### Scenario: No shared signpost
- **WHEN** the route goes 28 → 02 → 01, arriving at one signpost and leaving at the other
- **THEN** its line joins them along the path between the two badges

#### Scenario: Same distance
- **WHEN** the line of a route goes through another signpost than before
- **THEN** the route's distance, time and knots are the same

### Requirement: Route used in both directions
The route line SHALL be drawn beside its course, to the right in riding direction, so a stretch the route uses in both directions shows as two lines next to each other, each with its own arrows.

#### Scenario: A stretch used twice
- **WHEN** a route uses the same stretch in both directions
- **THEN** the map shows two lines next to each other there, with arrows pointing the opposite ways

#### Scenario: A stretch used once
- **WHEN** a route uses a stretch in one direction
- **THEN** the map shows one line there, slightly to the right of the course

#### Scenario: Tapping the route when zoomed out
- **WHEN** the map is zoomed out and the user taps the line beside the course
- **THEN** the map zooms to show the whole route, as before

### Requirement: Choose a route point on the map
Where knots are shown, a tap with no knot, route point or highlighted place within the tolerance SHALL offer "Add to route" at the tapped spot, and SHALL NOT change the route until it is chosen. "Add to route" SHALL add a route point at the spot as a tapped knot is added: the start if there is none, the end if there is no end, and otherwise where it adds the least distance, see "Plan by tapping knots". Once a route exists, the note of "Changing what a tapped knot became" SHALL follow. The offer SHALL be dismissible. A spot farther than 20 km on a cycling route, or 5 km on a hiking route, from every course of the network SHALL NOT be added; the user SHALL be told that the network has no knots within that distance.

#### Scenario: Knots go first
- **WHEN** the user taps a place with a knot within the tolerance
- **THEN** the knot is treated as tapped

#### Scenario: A route point goes first
- **WHEN** the user taps a place with a route point within the tolerance
- **THEN** nothing is offered and the route does not change

#### Scenario: Spot beside the route
- **WHEN** a route 25 → 32 exists and the user taps an empty spot beside its course and chooses "Add to route"
- **THEN** a route point at the spot is added as a stop, and the note offers to make it the start or the end, or to undo it

#### Scenario: Spot beyond the end
- **WHEN** a route 25 → 32 exists and the user taps an empty spot beyond 32 seen from 25 and chooses "Add to route"
- **THEN** the route point is the new end, and 32 a stop

#### Scenario: Offer and dismiss
- **WHEN** the user taps an empty spot or a highlighted place and closes the offer without choosing "Add to route"
- **THEN** the route is unchanged

#### Scenario: Offer without a route
- **WHEN** there is no waypoint yet and the user adds an empty spot with "Add to route", and then taps knot 28
- **THEN** the route goes from the spot to 28, and no note is shown

#### Scenario: Zoomed out
- **WHEN** knots are not shown and the user taps the map
- **THEN** nothing is offered

#### Scenario: Too far from the network
- **WHEN** hiking is chosen and the user taps a place 70 km from the nearest hiking course
- **THEN** nothing is offered, the route does not change, and the user is told there are no hiking knots within 5 km

### Requirement: What a chosen route point does
"Start here", "Add stop here" and "End here" are the start, stop and end buttons of the note for a route point. "Start here" SHALL make the route point the new start, "End here" the new end, and "Add stop here" a stop in the leg where it adds the least distance. A waypoint that was the start or end before SHALL become a stop, whether it is a knot or a route point.

#### Scenario: First waypoint
- **WHEN** there are no waypoints and the user adds an empty spot with "Add to route", then taps 28
- **THEN** the route goes from the point to 28

#### Scenario: End before the start
- **WHEN** 28 is the only waypoint and the user adds a place with "Add to route"
- **THEN** 28 is the start and the place the end

#### Scenario: Before an existing start
- **WHEN** the route is 02 → 28, a route point was added, and the user chooses "Start here"
- **THEN** the waypoints are the point, 02 as a stop, and 28

#### Scenario: After an existing end
- **WHEN** the route is 28 → 02, a route point was added, and the user chooses "End here"
- **THEN** the waypoints are 28, 02 as a stop, and the point

#### Scenario: An earlier route point
- **WHEN** the start is a route point and the user chooses "Start here" for another one
- **THEN** the new place is the start and the earlier point a stop, which the user can remove

#### Scenario: A round trip stops being one
- **WHEN** the route is a round trip from 12 and the user chooses "End here" for a route point
- **THEN** the waypoints keep 12 as start and as a stop, and the point is the end

### Requirement: A route point on a course
A tap with a course of the network within the tolerance SHALL put the route point at the nearest place on that course. Connections with a ferry SHALL be left out, as the data does not say where on the course the ferry is.

#### Scenario: On a course
- **WHEN** the user chooses "Start here" on the course of 02-06
- **THEN** the point is on the course, and the line begins there

#### Scenario: Through the fitting signpost
- **WHEN** the route starts at a route point on 02-06 before its branches to the two signposts of 02, and goes on to 28
- **THEN** its line goes straight to the signpost that fits 02 → 28, as in "Routes through a knot with several signposts"

#### Scenario: On a ferry connection
- **WHEN** the user chooses a place on the course of a connection with a ferry
- **THEN** the point is treated as away from the network

### Requirement: A route point away from the network
On a cycling route, a route point with no course within the tolerance SHALL connect to a nearby place on a course without a ferry, by a way found on the device from the path data, see `path-routing`. Otherwise, and on a hiking route, it SHALL connect by a straight line to the nearest place on a course, which SHALL be replaced by a way once the path data allows, only while planning. Planning SHALL never wait for a way, and the point's position SHALL NOT be sent anywhere.

#### Scenario: A way is found
- **WHEN** the user chooses a place 400 m from the network on a cycling route, with the squares around it installed
- **THEN** the point connects along a way on suitable paths, and the route's length and time include it

#### Scenario: Joining a nearby course
- **WHEN** the nearest course is across a river with no bridge nearby, and another course on the same side is a little further
- **THEN** the way joins the course on the same side, if its way is shorter

#### Scenario: While the path data loads
- **WHEN** the user chooses a place away from the network and the squares around it are not installed yet
- **THEN** the route is shown at once with a straight line, and the line is replaced by a way when the squares have arrived

#### Scenario: No path data
- **WHEN** the squares around the place can't be downloaded, e.g. without internet access, or the user deleted the ways
- **THEN** the point connects by a straight line, the planner says that its way and length are only straight, and planning keeps working

#### Scenario: No way found
- **WHEN** the installed squares don't connect the place to a course, e.g. as it is in a closed area
- **THEN** the point connects by a straight line, as without path data

#### Scenario: A hiking route
- **WHEN** the user chooses a place away from the network on a hiking route
- **THEN** the point connects by a straight line, and no path data is requested

#### Scenario: Nothing leaves the device
- **WHEN** the user chooses a place away from the network
- **THEN** no request contains the place, and only the squares around the route's course are requested, as for any cycling route

#### Scenario: The line on the map
- **WHEN** a route has a way to a point away from the network
- **THEN** that way is drawn differently from the route on the network, and a straight line differently from a way along paths

### Requirement: Route points on the map and in the list
A route point SHALL be shown as a flag instead of a number badge, on the map and in the waypoint list and knot sequence, with the word for start, stop or end for screen readers.

#### Scenario: On the map
- **WHEN** a waypoint is a route point
- **THEN** a flag marks its place

#### Scenario: In the list
- **WHEN** a waypoint is a route point
- **THEN** its row shows the flag, the word for its role, a handle and a remove button

#### Scenario: Removing the route point
- **WHEN** the waypoints are a route point, 02 and 28, and the user removes the route point
- **THEN** the route is 02 → 28

### Requirement: Round trips from a route point
The action to return to the start SHALL also be available when the start is a route point, and SHALL make the same place the end.

#### Scenario: Out and back from home
- **WHEN** the waypoints are a route point and 28, and the user returns to the start
- **THEN** the route goes from the point to 28 and back to the same point

### Requirement: Keeping route points
Route points and their ways SHALL be kept with the route across restarts, also before the courses are loaded, without path data and without internet access. A route point whose connection no longer exists after a data update SHALL connect anew to the nearest course. A route stored before route points existed SHALL still be restored.

#### Scenario: After a restart
- **WHEN** a waypoint is a route point with a way along paths, and the app is restarted without internet access
- **THEN** the same place and the same way are in the route, and its length is unchanged

#### Scenario: Connection gone
- **WHEN** a data update removed the connection of a route point
- **THEN** the point stays in the route and connects to the nearest course, as a newly chosen one does

#### Scenario: Older route
- **WHEN** a route stored by an earlier version is restored
- **THEN** its knots are restored as before

### Requirement: Route points in the GPX export
The GPX export SHALL contain a route point as a waypoint named for its role, and the track SHALL run along its way.

#### Scenario: Start point in the file
- **WHEN** a route starting at a route point is exported
- **THEN** the first waypoint is named "Start", has the type start, and the track begins at its position

#### Scenario: A stop in the file
- **WHEN** a route with a route point as a stop is exported
- **THEN** that waypoint is named "Stop" and has the type stop

### Requirement: Route points from a place
A route point chosen from a tapped highlighted place SHALL keep the place's kind and name. On the map, in the waypoint list and on the strip it SHALL show the place's icon instead of the flag, with the corner symbol of its role as on knots. The waypoint list SHALL show the role followed by the place's name, or by the kind's name when the place has none, and screen readers SHALL read the same. The kind and the name SHALL be kept with the route across restarts. Route points from a spot without a place SHALL keep the flag.

#### Scenario: Café as a stop
- **WHEN** the user taps "Eiscafé Riviera" and chooses "Add stop here"
- **THEN** the stop shows the food icon with the stop's corner symbol on the map and on the strip, and the waypoint list shows "Stop · Eiscafé Riviera"

#### Scenario: Place without a name as the end
- **WHEN** the user taps a drinking water point without a name and chooses "End here"
- **THEN** the end shows the water icon with the end's corner symbol, and the list shows "End · Drinking water"

#### Scenario: Start that becomes a stop
- **WHEN** the start is a café and the user chooses "Start here" at another spot
- **THEN** the café becomes a stop and keeps its icon and name

#### Scenario: Kept after a restart
- **WHEN** the route has a supermarket as a stop and the app is started again
- **THEN** the stop still shows the supermarket icon and its name

#### Scenario: Route saved before
- **WHEN** a route with a route point was saved before this change and is loaded
- **THEN** its route point shows the flag, as before

#### Scenario: Spot without a place
- **WHEN** the user taps an empty spot and chooses "End here"
- **THEN** the end shows the flag, as before

#### Scenario: Places switched off
- **WHEN** a café is a stop and the user turns the places off in the info sheet
- **THEN** the stop keeps the food icon

#### Scenario: Screen reader in the list
- **WHEN** a screen reader reaches the café stop in the waypoint list
- **THEN** it reads "Stop, Eiscafé Riviera" and offers to move and remove it by that name

### Requirement: Places useful on a ride
The map SHALL draw places from the base map that matter on a ride with their own icon on a coloured disc, one per kind:
- **food and drink:** restaurants, cafés, fast food, bars, pubs, beer gardens and ice cream
- **supermarkets**
- **bicycle shops,** including bicycle repair, and bicycle repair stations where the base map has them
- **railway stations** and halts
- **water and toilets:** drinking water and public toilets
- **campsites,** not those for motorhomes only
- **hotels and guest houses:** hotels, motels, hostels and guest houses, not holiday homes

The discs' colours SHALL differ from the knots' sign colours and from the route. The base map SHALL NOT draw these places a second time in its own style. The places SHALL be shown from zoom 14, and their names from zoom 16. The knots, the route, the waypoints and the rider's position SHALL be drawn above them, and where a place's icon would overlap a knot's badge, the badge SHALL be shown and the place left out. The places SHALL be shown on the light and the dark map, for both network types, while planning and while navigating. They come with the base map, so where the base map is missing they are missing too; nothing beyond the base map SHALL be loaded to draw them.

#### Scenario: Places in a town
- **WHEN** the map shows the centre of Troisdorf at zoom 15
- **THEN** cafés and restaurants, supermarkets, the bicycle shops and Troisdorf station each show their kind's icon on its coloured disc, without names

#### Scenario: Names close up
- **WHEN** the user zooms in to zoom 16 over a highlighted café
- **THEN** its name is shown next to its icon

#### Scenario: Zoomed out
- **WHEN** the map is at zoom 13
- **THEN** no highlighted places are shown

#### Scenario: Shown once
- **WHEN** a supermarket is highlighted
- **THEN** the base map's grey shop icon is not drawn at the same place

#### Scenario: Other places unchanged
- **WHEN** the map shows a pharmacy, a bank or a bike rental
- **THEN** they are drawn in the base map's own style, as before this change

#### Scenario: Knot next to a café
- **WHEN** a café is a few metres from a knot, so their icons would overlap
- **THEN** the knot's badge is shown in full and the café's icon is left out

#### Scenario: Under the route
- **WHEN** the route runs past a restaurant
- **THEN** the route line is drawn above the restaurant's icon

#### Scenario: Distinct from knots
- **WHEN** highlighted places are shown among Dutch, Belgian and German knots
- **THEN** no place disc has the colour of a knot's sign, and each kind can be told from the others by its icon and colour

#### Scenario: Places on the dark map
- **WHEN** the app is dark and the map shows highlighted places
- **THEN** their icons and names are as readable as on the light map

#### Scenario: Places while navigating
- **WHEN** the rider navigates through a town at zoom 15 or closer
- **THEN** the highlighted places are shown along the way, below the route and the rider's arrow

#### Scenario: Places on a hiking map
- **WHEN** the hiking network is selected
- **THEN** the same places are highlighted

#### Scenario: Without the base map
- **WHEN** the base map cannot be loaded, e.g. offline on first start
- **THEN** the network is shown as before and no places are shown

#### Scenario: Campsites near Renesse
- **WHEN** the map shows the dunes east of Renesse at zoom 15
- **THEN** the campsites show the campsite icon on its own coloured disc, and the hotels and guest houses show the hotel icon on another

#### Scenario: Motorhome sites and holiday homes
- **WHEN** the map shows a site for motorhomes only or a holiday home in a park
- **THEN** they are drawn in the base map's own style, as before this change

### Requirement: Switch for places on the map
A switch in the info sheet SHALL turn the highlighted places off and on again. It SHALL be on by default, and the choice SHALL be kept across restarts. With the switch off, the map SHALL look as before the places were highlighted.

#### Scenario: Turning the places off
- **WHEN** the user turns the places off in the info sheet
- **THEN** the map shows no highlighted places, and the base map draws all its places in its own style as before this change

#### Scenario: Choice kept for places
- **WHEN** the user turned the places off and starts the app again
- **THEN** the places are still off

#### Scenario: Switch by default
- **WHEN** the app is started for the first time after this change
- **THEN** the switch is on and the places are shown

### Requirement: Route on the side of its direction
The route on the map, the line ahead while navigating, and the course navigation follows SHALL use, for each connection, the course of the direction the route takes it in.

#### Scenario: Route on its side of the road
- **WHEN** the route goes from 28 to 02 at the Sieg bridge in Siegburg-Zange, where each side of the road has its own way
- **THEN** the route on the map runs on the way for that direction, without crossing the road to the other side and back

#### Scenario: Out and back
- **WHEN** the route goes from 28 to 02 and back to 28
- **THEN** the map shows the way there and the way back on their own sides of the road

#### Scenario: Line ahead and off route
- **WHEN** the rider navigates from 02 to 28 on the way for that direction
- **THEN** the line ahead follows that way, and the rider is not told they are off route

#### Scenario: From a route point on the way back
- **WHEN** a stop lies on the connection between 02 and 06 and the route goes on from it to 02
- **THEN** the route from the stop to 02 follows the course from 06 to 02, not the course from 02 to 06 reversed

#### Scenario: Geometry without courses back
- **WHEN** the installed geometry file has no courses back, e.g. because it was published before this change
- **THEN** the route is drawn and followed as before

### Requirement: Route and bridges
When the geometry has bridge stretches, the map SHALL draw the base map's bridges, road, path and railway bridges with their outlines, over the route and the network's connections, with the same transparency everywhere, so the lines stay visible below them. The stretches of the route and of the connections that run on a bridge SHALL be drawn above the bridges. The route and the connections SHALL look the same below a bridge as anywhere else.

#### Scenario: Route under a bridge
- **WHEN** the planned route passes under a motorway bridge
- **THEN** the bridge is drawn semi-transparent over the route, and the route is visible below it

#### Scenario: Route over a bridge
- **WHEN** the planned route between 28 and 29 runs over a bridge
- **THEN** the route is drawn on top of that bridge

#### Scenario: Over and under the same bridge
- **WHEN** an out-and-back route runs over a bridge on the way out, and under that bridge on the way back
- **THEN** the route is drawn on top of the bridge where it runs over it, and below the semi-transparent bridge where it passes under it

#### Scenario: Connections and bridges
- **WHEN** a connection of the network passes under a bridge, and another one runs over it
- **THEN** the first is drawn below the semi-transparent bridge and the second on top of it

#### Scenario: Same transparency everywhere
- **WHEN** a bridge is near the route and another one is far from it
- **THEN** both are drawn with the same transparency, and the route keeps its colour and width below the bridge

#### Scenario: Dark map
- **WHEN** the route passes under a bridge on the dark map
- **THEN** the bridge is drawn semi-transparent in the dark map's colours over the route

#### Scenario: Offline map
- **WHEN** the base map cannot be loaded and the offline map is shown
- **THEN** no bridges are drawn, and the route is drawn as before

#### Scenario: Geometry without bridge stretches
- **WHEN** the installed geometry file has no bridge stretches, e.g. because it was published before this change
- **THEN** no bridges are drawn over the route or the connections

### Requirement: Bridge edges on the route
When the geometry has bridge stretches, the route and the line of the route ahead SHALL get the edges of a bridge on both sides where they run on a bridge, thin and partly transparent, so a stretch over a bridge can be told apart at a glance from one under a bridge. The network's connections SHALL get no edges.

#### Scenario: Under, then onto the same bridge
- **WHEN** the route passes under a bridge and then climbs onto it
- **THEN** the map shows the semi-transparent bridge across the route first, and the route with bridge edges on the bridge itself

#### Scenario: Edges on the dark map
- **WHEN** the route runs on a bridge on the dark map
- **THEN** the edges are light enough to see

#### Scenario: Connection on a bridge
- **WHEN** a connection of the network runs on a bridge
- **THEN** it is drawn on top of the bridge without edges

### Requirement: Map content above and below bridges
Knots, route knots, the next knot, the rider's position, the way to the route and the guided way SHALL stay above the bridges drawn over the route. While navigating, the line of the route ahead SHALL be drawn like the route, with the edges of a bridge where it runs on one. The ridden track SHALL stay on top of the bridges and of the route's stretches on them, so it stays visible where the rider crossed a bridge. Tunnels SHALL be drawn as the base map draws them.

#### Scenario: Knots and position stay on top
- **WHEN** a knot or the rider's position is under a bridge
- **THEN** the knot's badge and the position are drawn above the bridge

#### Scenario: Line ahead while navigating
- **WHEN** the rider navigates a route that passes under a bridge and then runs over another one
- **THEN** the line of the route ahead is drawn below the first bridge, and on top of the second with the edges of a bridge

#### Scenario: Ridden track
- **WHEN** the rider has ridden over a bridge
- **THEN** the ridden track is visible on that bridge, on top of the route

#### Scenario: Tunnel
- **WHEN** the route runs through a tunnel
- **THEN** the route is drawn as before, and the tunnel as the base map draws it

### Requirement: Tapping a place on the map
A tap on a highlighted place SHALL show a card at the place with its kind's icon and its name, or the kind's name when the place has none, and its details. While planning, the card SHALL offer "Add to route" below the name, which SHALL add a route point at the place's position, not at the nearest course, as an empty spot is added, see "Choose a route point on the map". Showing the card SHALL NOT change the route. A knot or a route point within the touch tolerance SHALL go first. A place beyond the distance limit of "Choose a route point on the map" (20 km cycling, 5 km hiking from every course) SHALL show its name without "Add to route". While navigating, the card SHALL show the name only and SHALL NOT change the route or the navigation. A tap beside the card or Back SHALL close it.

#### Scenario: Name of a café
- **WHEN** the user taps the icon of "Eiscafé Riviera" in Troisdorf at zoom 15
- **THEN** a card at the café shows the food icon and "Eiscafé Riviera", with "Add to route" below it, and the route does not change

#### Scenario: Place without a name
- **WHEN** the user taps a drinking water icon that has no name in the map data
- **THEN** the card shows the water icon and "Drinking water", in the app's language

#### Scenario: Stop at a place
- **WHEN** a route 25 → 32 exists and the user taps a supermarket 300 m off the course and chooses "Add to route"
- **THEN** the supermarket is added where it adds the least distance, here as a stop, the card closes, the note follows, and the route's way leads to it

#### Scenario: Place next to a course
- **WHEN** a café is 10 m beside a course, within the touch tolerance of both, and the user taps the café and chooses "Add to route"
- **THEN** the route point is at the café, not on the course

#### Scenario: Knot before a place
- **WHEN** a café's icon is within the touch tolerance of knot 45 and the user taps there
- **THEN** 45 is tapped as before, and no card shows

#### Scenario: Place far from the network
- **WHEN** hiking is chosen and the user taps a railway station 30 km from every hiking course
- **THEN** the card shows the station's name without "Add to route"

#### Scenario: Place while navigating
- **WHEN** the rider is navigating and taps a toilet icon
- **THEN** the card shows "Toilets" or the toilet's name without "Add to route", and the navigation and the route stay as they are

#### Scenario: Closing the card
- **WHEN** the card shows and the user taps beside it
- **THEN** the card closes, and nothing else happens with that tap

#### Scenario: Grey places unchanged
- **WHEN** the user taps a pharmacy drawn in the base map's own style
- **THEN** the tap works as on an empty spot: "Add to route" is offered without a name

### Requirement: Contacts on the place card
The card of a tapped highlighted place SHALL show the place's website and phone number from OpenStreetMap below its name, each with an action: the website opens in the browser, the phone number opens the phone's dialler with the number, without calling. A detail missing in OpenStreetMap SHALL be left out, not guessed, and a place without both SHALL show the card as before. The contacts SHALL be shown for every highlighted kind, while planning and while navigating. They come from the downloaded place details (see `network-sync` "Place details around the route"); where those are not installed, the card SHALL show what it can without them and SHALL NOT wait for them before showing the name. Showing or using the contacts SHALL NOT change the route or the navigation.

#### Scenario: Campsite with website and phone
- **WHEN** the user taps a campsite that has a website and a phone number in OpenStreetMap, with its place details installed
- **THEN** the card shows the campsite's name, its website and its phone number, each with its action, and the route point choices below

#### Scenario: Opening the website
- **WHEN** the user taps the website on the card
- **THEN** the website opens in the browser, and the route and the map stay as they are when the user comes back

#### Scenario: Calling a hotel
- **WHEN** the user taps the phone number of a hotel on the card
- **THEN** the phone's dialler opens with the number, and nothing is called until the user confirms there

#### Scenario: Website without a scheme
- **WHEN** a place's website is tagged as `www.example.nl`, without `https://`
- **THEN** the card shows it and opens `https://www.example.nl`

#### Scenario: Only a phone number
- **WHEN** a café has a phone number but no website in OpenStreetMap
- **THEN** the card shows the phone number with its action and no website row

#### Scenario: No contacts
- **WHEN** a drinking water point has neither a website nor a phone number
- **THEN** the card shows the water icon and its name as before, without empty rows

#### Scenario: Contacts while navigating
- **WHEN** the rider is navigating and taps a restaurant next to the route that has a website
- **THEN** the card shows the name and the website with its action, without the route point choices, and the navigation goes on

#### Scenario: Details not installed
- **WHEN** the user taps a place whose place details are not installed and cannot be downloaded
- **THEN** the card shows the icon and the name at once, as before this change, without contacts

#### Scenario: Details arriving after the tap
- **WHEN** the user taps a place while planning and its place details are downloaded while the card is open
- **THEN** the contacts appear on the open card without tapping again

#### Scenario: Several numbers
- **WHEN** a hotel's phone is tagged with two numbers separated by `;`
- **THEN** the card shows the first number

#### Scenario: Contacts with a screen reader
- **WHEN** a screen reader reaches the website or the phone number on the card
- **THEN** it reads the website or the number and announces that it opens the website or the dialler, as a button

### Requirement: Opening hours on the place card
The card of a tapped highlighted place with opening hours in its place details SHALL show whether the place is open now according to OpenStreetMap and, where that can be told too, when that changes next, worded as for ferries (see "Ferry open now"), in the local time of the covered area, with the opening hours as tagged below it. When the opening hours don't allow a sure answer, the card SHALL show only the tagged hours. The status SHALL stay current while the card is shown. A place without opening hours SHALL show neither.

#### Scenario: Café open now
- **WHEN** the user taps a café with "Mo-Su 08:00-18:00" on Wednesday, 16 June 2027, 14:00
- **THEN** the card shows "Open until 18:00" and "Mo-Su 08:00-18:00" below it

#### Scenario: Restaurant opening later
- **WHEN** the user taps a restaurant with "Tu-Su 17:00-22:00" on Wednesday, 16 June 2027, 14:00
- **THEN** the card shows "Closed until 17:00"

#### Scenario: Restaurant closed on its rest day
- **WHEN** the user taps the same restaurant on Monday, 14 June 2027, 14:00
- **THEN** the card shows "Closed until tomorrow 17:00"

#### Scenario: Lunch and dinner
- **WHEN** the user taps a restaurant with "Mo-Su 11:30-14:00,17:30-22:00" on Wednesday, 16 June 2027, 15:00
- **THEN** the card shows "Closed until 17:30"

#### Scenario: Hours the app can't be sure of
- **WHEN** the user taps a café with "Mo-Fr 08:00-18:00; SH off" on Wednesday, 16 June 2027, 10:00
- **THEN** the card shows no status, only "Mo-Fr 08:00-18:00; SH off"

#### Scenario: Unreadable hours
- **WHEN** a café's opening hours are tagged as "summer only, ask inside"
- **THEN** the card shows no status, only the tagged text

#### Scenario: Closing while the card is shown
- **WHEN** the card of a café that closes at 18:00 is shown at 17:59
- **THEN** its status changes to closed within a minute after 18:00, without any action

#### Scenario: Place without opening hours
- **WHEN** the user taps a campsite with a website but no opening hours
- **THEN** the card shows the website and no opening status or hours

#### Scenario: Opening hours while navigating
- **WHEN** the rider is navigating and taps a café next to the route whose place details are installed
- **THEN** the card shows its opening status and hours, and the navigation goes on

#### Scenario: Opening hours with a screen reader
- **WHEN** a screen reader reaches the opening status on the card
- **THEN** it reads the status followed by the tagged hours

### Requirement: Changing what a tapped knot became
After a tapped knot, a tapped spot or a place was added to an existing route, the app SHALL show a short note: one line saying what it became, three buttons in the fixed order start, stop and end, each previewing that role with badges, and Undo. The button of the role it got SHALL show as selected and inactive. The buttons SHALL act as "Start here", "Add stop here" and "End here" do. A choice SHALL keep the note open with the chosen role selected, so a mistap can be put right, and start its 6 seconds again; Undo SHALL close it. The note SHALL go away after 6 seconds, with the next tap, or at once when the user chooses Navigate, since planning tools are not needed while the route is ridden, but not by itself while a screen reader is on.

#### Scenario: Note after a tap
- **WHEN** a route 12 → 14 → 15 exists and the user taps 27, which becomes a stop between 14 and 15
- **THEN** the note says "27 added as a stop" and shows the buttons 27 before 12 for the start, 14, 27 and 15 for the stop, selected, and 15 before 27 for the end, then Undo

#### Scenario: Note for a new start
- **WHEN** a tapped knot 60 became the new start before 05
- **THEN** the note says "60 added as the start", and the start button shows 60 before 05 and is selected

#### Scenario: Note for a new end
- **WHEN** a tapped knot 22 became the new end after 15
- **THEN** the note says "22 added as the end", the end button shows 15 before 22 and is selected, and the stop button shows 22 with the knots around it where it would be a stop

#### Scenario: Note for a place
- **WHEN** a route 25 → 32 exists and the user adds "Eiscafé Riviera", which becomes a stop
- **THEN** the note says "Eiscafé Riviera added as a stop", and the buttons preview the café with its icon

#### Scenario: Note for a spot
- **WHEN** a route 25 → 32 exists and the user adds an empty spot beyond 32 with "Add to route"
- **THEN** the note says "Added as the end", and the buttons preview the spot with the flag

#### Scenario: Make it the start
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the start button
- **THEN** the waypoints are 30, 12 as a stop, and 45

#### Scenario: Make it the end
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the end button
- **THEN** the waypoints are 12, 45 as a stop, and 30

#### Scenario: Make it a stop
- **WHEN** 28 was added as the new end of 12 → 45 and the user chooses the stop button
- **THEN** 28 is a stop where it adds the least distance, and 45 is the end again

#### Scenario: Note stays after a choice
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses the start button
- **THEN** the note stays, says "30 added as the start" with the start button selected, and goes away 6 seconds later unless the user chooses again, chooses Undo or taps the map

#### Scenario: Undo the tap
- **WHEN** 30 was added as a stop to 12 → 45 and the user chooses Undo
- **THEN** the waypoints are 12 and 45 again

#### Scenario: Note goes away
- **WHEN** the note is shown and the user does nothing for 6 seconds, or taps another knot
- **THEN** the note is gone, and for the other knot a new note is shown

#### Scenario: Note with a screen reader
- **WHEN** a screen reader is on and 27 was added as a stop between 14 and 15
- **THEN** it reads the line "27 added as a stop", the buttons as "As the start", "As a stop, selected" and "As the end", and the note stays until Undo or the next tap

#### Scenario: No note for the first two taps
- **WHEN** the user taps the start and then the end of a new route
- **THEN** no note is shown

#### Scenario: Navigate closes the note
- **WHEN** the note says "30 added as a stop" and the user chooses Navigate
- **THEN** the note goes away at once, before the position is found

### Requirement: Map buttons in groups
The buttons over the map SHALL stand in one column at the right edge, in groups that belong together: search, then position with nearest knots, then zoom in with zoom out. Each group SHALL share one background, Liquid Glass on iOS and a card on Android. In portrait the column SHALL stand on the planner panel's top edge and move with it. Where it does not fit, the zoom group SHALL be left out first, then the position group.

#### Scenario: Groups on the map
- **WHEN** a route is planned and the map shows no knots
- **THEN** the column shows search, then the position and nearest knots buttons on one background, then zoom in and zoom out on one background

#### Scenario: Moving with the panel
- **WHEN** the user pulls the panel up
- **THEN** the column moves up with it, staying on its top edge

#### Scenario: Above Navigate on Android
- **WHEN** a complete route is planned on Android in portrait
- **THEN** the column stands directly above the Navigate button and none of its buttons is covered by it

#### Scenario: Panel pulled far up
- **WHEN** the panel is pulled up so far that the column does not fit below the network switch
- **THEN** the zoom group is left out first, then the position group, and search stays

#### Scenario: Glass on iOS
- **WHEN** the map is shown on iOS 26
- **THEN** each group of buttons has the Liquid Glass background of the map's other floating controls

#### Scenario: Note on the panel's edge
- **WHEN** the note after a tap shows
- **THEN** it stands on the panel's top edge, the column stands on top of the note, on Android Navigate between the note and the column, and both drop back when the note goes

#### Scenario: Note on a small phone
- **WHEN** the note shows and the column no longer fits above it
- **THEN** the zoom group is left out while the note shows, and comes back when it goes

#### Scenario: Buttons with a screen reader
- **WHEN** a screen reader moves through the column
- **THEN** it announces each button by its name, as before, in the column's order

#### Scenario: Landscape
- **WHEN** the phone is in landscape
- **THEN** the column stands at the bottom right of the map, clear of the cutout and the navigation bar

### Requirement: Glass on the map
Everything that floats over the map SHALL share one glass look: the network switch, the info button, the button groups, the note after a tap, the card of a tapped spot or place, Navigate on Android and the notes over the map. On iOS it SHALL be Liquid Glass; on Android one slightly translucent surface with a soft border and shadow. Text and icons SHALL keep a contrast of at least 4.5:1 and 3:1.

#### Scenario: Note and card in glass
- **WHEN** the note after a tap or the card of a tapped place shows over the map
- **THEN** it has the same glass look as the map's buttons, on iOS Liquid Glass

#### Scenario: One look on Android
- **WHEN** the map shows the network switch, the info button, the button groups and a note
- **THEN** all of them have the same translucent surface, border and shadow

#### Scenario: Readable on glass
- **WHEN** the texts and icons over the map are measured against their glass background, over a light and a dark map
- **THEN** texts have a contrast of at least 4.5:1 and icons of at least 3:1

### Requirement: Panel before a route
With fewer than two waypoints, the panel SHALL show "Plan a ride", a start and an end slot with their labels, one hint line, and buttons "Search a place" and "Nearest knots". An empty slot SHALL look empty; the slot the next tap fills SHALL stand out. A set waypoint SHALL fill its slot with its badge and its label SHALL name its place. With a waypoint set, Clear SHALL replace "Nearest knots".

#### Scenario: Nothing planned
- **WHEN** no waypoint is set and knots are in view
- **THEN** the panel shows "Plan a ride", two empty slots labelled "Start" and "End", the start slot standing out, "Tap a numbered knot on the map for the start.", and the buttons "Search a place" and "Nearest knots"

#### Scenario: Start set
- **WHEN** the user taps 48 in De Rips with nothing planned
- **THEN** the start slot shows the badge 48, labelled "Start · De Rips", the end slot stands out, the hint says "Now tap a knot for the end.", and the buttons are "Search a place" and Clear

#### Scenario: End chosen first
- **WHEN** an end was chosen before any start
- **THEN** the end slot shows its badge, the start slot stands out, and the hint says "Now tap a knot for the start."

#### Scenario: Route point in a slot
- **WHEN** the start is a route point from a tapped spot
- **THEN** the start slot shows the flag badge of route points

#### Scenario: Next slot without motion
- **WHEN** the phone is set to reduce motion or to remove animations
- **THEN** the slot that stands out does so without pulsing

#### Scenario: Search from the panel
- **WHEN** the user chooses "Search a place" in the panel
- **THEN** the place search opens as from the map's search button

#### Scenario: Nearest knots from the panel
- **WHEN** the user chooses "Nearest knots" in the panel
- **THEN** the map shows the nearest knots as the map's nearest knots button does

#### Scenario: Into the route
- **WHEN** the start is set and the user taps a knot for the end
- **THEN** the slots give way to the route's distance, time, knots and actions

#### Scenario: Slots with a screen reader
- **WHEN** a screen reader reaches the slots with only the start 48 set
- **THEN** it reads "Start, 48, De Rips" and "End, not set yet"

#### Scenario: Empty panel at large text sizes
- **WHEN** the largest text size is set and nothing is planned
- **THEN** the title, labels, hint and buttons are fully readable, and the buttons wrap to a second line rather than being cut
