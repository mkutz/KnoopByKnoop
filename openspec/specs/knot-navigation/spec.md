# knot-navigation Specification

## Purpose
Guides the rider along a planned route knot by knot: it tells which number to follow at the next knot, by voice and on the lock screen, so the screen can stay off and the battery lasts.

## Requirements

### Requirement: Start navigation
A complete route SHALL be navigable. Navigation SHALL be started by the user while the app is open, and SHALL begin where the rider is: at a knot of the route or on its course; more than 100 m from the course, it SHALL lead to the first knot of the route. Without courses, it SHALL begin at the route knot nearest to the rider. It SHALL need location access only while the app is in use, not always.

#### Scenario: Start at the first knot
- **WHEN** the route runs 24, 45, 12, 31 and the rider starts navigation standing at knot 24
- **THEN** navigation begins with 45 as the next knot

#### Scenario: Start at a knot in the middle of the route
- **WHEN** the rider starts navigation standing at knot 12 of the route 24, 45, 12, 31
- **THEN** navigation begins with 31 as the next knot

#### Scenario: Start between two knots
- **WHEN** the rider starts navigation on the course of the route between 45 and 12
- **THEN** navigation begins with 12 as the next knot

#### Scenario: Start away from the route
- **WHEN** the rider starts navigation 2 km away from the course of the route 24, 45, 12, 31, nearest to route knot 45
- **THEN** navigation begins with 24, the first knot, as the next knot and says the rider is off route

#### Scenario: Incomplete route
- **WHEN** the route has no start, no end, or an unreachable leg
- **THEN** navigation cannot be started

#### Scenario: Location access denied
- **WHEN** the user starts navigation and denies location access
- **THEN** navigation does not start, and the app says that it needs location access to navigate

#### Scenario: Joining the route on the way to the start
- **WHEN** the rider started away from the route, heads for 24 and on the way passes 12 of the route
- **THEN** navigation continues from 12 with 31 as the next knot, as when skipping ahead

#### Scenario: Start away from the route without courses
- **WHEN** the network's courses are not installed and the rider starts navigation nearest to route knot 45
- **THEN** navigation begins with 45 as the next knot

### Requirement: Number to follow
While navigating, the app SHALL show the number to follow at the next knot as its most prominent information, e.g. "At 45 follow 12", with the knots as badges like on the map, and in the same line the distance to the next knot along the route, or straight to it while off route. When the next knot is the end of the route, it SHALL say so instead.

#### Scenario: Next knot in the middle of the route
- **WHEN** the next knot is 45 and the route continues to 12
- **THEN** the app shows "At 45 follow 12" with 45 and 12 as badges, 12 as the most prominent element, and the distance to 45, e.g. "800 m"

#### Scenario: Next knot is the end
- **WHEN** the next knot is 31, the end of the route
- **THEN** the app shows that 31 is the end of the route

#### Scenario: Distance off route
- **WHEN** the rider is off route, 1.7 km straight from the next knot 02
- **THEN** the app shows "Off route, next 02" with 02 as a badge and "1.7 km"

### Requirement: Passing a knot
A knot SHALL count as passed when the rider comes within 30 m of it. Passing the next knot SHALL make the knot after it the next knot and SHALL be announced with the next two knots, e.g. "Next 12, then 31".

#### Scenario: Passing the next knot
- **WHEN** the rider heading to 45 on the route 24, 45, 12, 31 comes within 30 m of 45
- **THEN** 12 becomes the next knot and the app says "Next 12, then 31"

#### Scenario: Riding close to a knot without passing it
- **WHEN** the rider passes 80 m away from the next knot on a parallel road
- **THEN** the knot does not count as passed

#### Scenario: Round trip
- **WHEN** the route is a round trip 24, 45, 12, 24 and the rider starts navigation at 24
- **THEN** 45 is the next knot, and reaching 24 again only counts as arriving after 45 and 12 were passed

### Requirement: Reminder before a knot
When the rider comes within 200 m of the next knot, the app SHALL remind them of the number to follow there, e.g. "45 ahead, then 12". The reminder SHALL be given once per knot.

#### Scenario: Approaching the next knot
- **WHEN** the rider heading to 45, with 12 after it, comes within 200 m of 45
- **THEN** the app says "45 ahead, then 12" once

#### Scenario: Knots close together
- **WHEN** the next knot is less than 200 m after the knot just passed
- **THEN** the app does not give a separate reminder for it, because the announcement on passing already named it

### Requirement: Skipping ahead
When the rider passes a later knot of the route than the next knot, navigation SHALL continue from that knot, as if the knots before it were passed.

#### Scenario: Shortcut
- **WHEN** the rider heading to 45 on the route 24, 45, 12, 31 passes 12 without passing 45
- **THEN** 31 becomes the next knot and the app says "Next 31"

### Requirement: Off route
The rider SHALL be told they are off route when they pass a knot that is not on the route, or are more than 100 m away from the course of the route. The notice SHALL name the next knot and the straight distance and compass direction to it, e.g. "Off route, next 45, 1.7 km to the north-east". While off route, a guidance notice SHALL repeat the distance and direction when the rider got 500 m closer to the knot or its direction changed by more than 45° since the last notice. The app SHALL NOT plan a new route back.

#### Scenario: Passing a knot that is not on the route
- **WHEN** the rider heading to 45 passes knot 77, which is not on the route
- **THEN** the app says "Off route, next 45" with the distance and direction to 45

#### Scenario: Wrong turn between knots
- **WHEN** the rider heading to 45 leaves the course of the route and is more than 100 m away from it, with 45 1.7 km to the north-east
- **THEN** the app says "Off route, next 45, 1.7 km to the north-east"

#### Scenario: Back on route
- **WHEN** the rider who is off route comes back within 100 m of the course, or passes a knot of the route
- **THEN** the app says they are back on route and names the next knot

#### Scenario: Without geometry
- **WHEN** the network's courses are not installed
- **THEN** off route is only detected by passing a knot that is not on the route

#### Scenario: Off route is announced once
- **WHEN** the rider stays off route for several minutes
- **THEN** the app says "Off route" once, not repeatedly, and only the guidance notices follow

#### Scenario: Getting closer
- **WHEN** the rider is off route and got 500 m closer to 45 since the last notice, now 1.2 km to the north
- **THEN** the app says "Next 45, 1.2 km to the north"

#### Scenario: Direction changes
- **WHEN** the rider is off route and the direction to 45 changed from north-east to south-east
- **THEN** the app says the distance and the new direction, e.g. "Next 45, 1.4 km to the south-east"

#### Scenario: Riding around without getting closer
- **WHEN** the rider is off route, neither 500 m closer nor in a direction more than 45° off since the last notice
- **THEN** the app says nothing

### Requirement: Spoken notices
Every notice SHALL be spoken in English, also with the screen off and while other apps are in use. Knot numbers SHALL be spoken as numbers, so "08" is spoken as "eight". Other audio SHALL be lowered while a notice is spoken and restored after it.

#### Scenario: Screen off
- **WHEN** the rider passes a knot while the screen is off
- **THEN** the notice is spoken

#### Scenario: Number with a leading zero
- **WHEN** the next knot is 08
- **THEN** it is spoken as "eight"

#### Scenario: Music playing
- **WHEN** a notice is spoken while music plays
- **THEN** the music is lowered while the notice is spoken and plays on at its volume afterwards

### Requirement: Navigation with the screen off
Navigation SHALL keep running with the screen off and while other apps are in use. While navigating, the app SHALL show the number to follow at the next knot outside the app, together with the distance to the next knot and the arrival at the end as of the last knot passed, with a button to stop: on Android in an ongoing notification, on iOS in a Live Activity on the lock screen and in the Dynamic Island. Showing them SHALL NOT need more location fixes than navigating does.

#### Scenario: Lock screen
- **WHEN** the rider is navigating and looks at the lock screen
- **THEN** it shows "At 45 follow 12" and, e.g., "800 m to 45 · end around 19:05", updated whenever the next knot changes

#### Scenario: Dynamic Island
- **WHEN** the rider is navigating on an iPhone with a Dynamic Island and uses another app
- **THEN** the Dynamic Island shows the next knot and the number to follow there

#### Scenario: Off route outside the app
- **WHEN** the rider is off route and looks at the lock screen
- **THEN** it shows that they are off route, and the next knot

#### Scenario: Stop from the lock screen
- **WHEN** the rider taps Stop in the notification or the Live Activity
- **THEN** navigation stops, and the notification or Live Activity goes away

#### Scenario: Progress outside the app
- **WHEN** the rider passes a knot with the screen off, and the next knot is 800 m ahead and the end is reached around 19:05 at their pace
- **THEN** the notification or Live Activity shows "800 m to 45 · end around 19:05" until the next knot is passed

### Requirement: Position on the map
While navigating with the app open, the map SHALL show the rider's position, the route and the next knot, so the rider can decide how to get back to the route.

#### Scenario: Looking at the map
- **WHEN** the rider opens the app while navigating
- **THEN** the map shows their position, the route and the next knot highlighted

#### Scenario: Off route on the map
- **WHEN** the rider is off route and opens the app
- **THEN** the map shows their position together with the route, so they see where it is

### Requirement: Route is fixed while navigating
While navigating, the route SHALL NOT be changed. Changing it SHALL require stopping navigation first.

#### Scenario: Tapping a knot while navigating
- **WHEN** the rider taps a knot on the map while navigating
- **THEN** the route stays unchanged

#### Scenario: Switching the network while navigating
- **WHEN** the rider is navigating
- **THEN** the switch between cycling and hiking is not shown, and it is back after navigation ends

### Requirement: Arriving and stopping
Passing the last knot of the route SHALL be announced with "You've arrived" and SHALL end navigation. The user SHALL be able to stop navigation at any time in the app. The app SHALL use the location only while navigating, and once when the user asks for their position on the map, see `map-planning` "Show my position", so the system's location indicator shows only then; ending navigation SHALL stop using it. On iOS, closing the app while navigating SHALL end navigation.

#### Scenario: Arriving
- **WHEN** the rider passes 31, the end of the route
- **THEN** the app says "You've arrived", navigation ends, and the notification or Live Activity goes away

#### Scenario: Stopping in the app
- **WHEN** the rider taps Stop in the app while navigating
- **THEN** navigation ends and the planned route is still there

#### Scenario: No location use after navigation
- **WHEN** navigation ended by arriving, by Stop, or because the app was closed or ended while navigating, and the app is started again or left in the background
- **THEN** the app does not use the location until the user asks for their position, and the system shows no location indicator for it

#### Scenario: Closing the app while navigating
- **WHEN** the rider closes the app on iOS while navigating, by swiping it away in the app switcher
- **THEN** navigation ends, the location indicator and the Live Activity go away right then, and the planned route is still there at the next start

### Requirement: Battery use while navigating
Navigating with the screen off SHALL use at most 5 % of the battery per hour, measured on a real ride on the reference devices named in the design.

#### Scenario: One hour with the screen off
- **WHEN** the rider navigates a route for one hour with the screen off
- **THEN** the battery level drops by at most 5 percentage points

### Requirement: Following the rider
While navigating with the app open, the map SHALL follow the rider, keeping their position in view as they ride, until the user moves or zooms the map. Then the map SHALL stay where the user put it and a recenter button SHALL appear, which brings the map back to the rider at the navigation zoom and follows them again. The map SHALL NOT return to the rider on its own.

#### Scenario: Riding with the map following
- **WHEN** the rider rides along the route and has not touched the map since navigation started
- **THEN** the map moves with them, and their position stays in view

#### Scenario: Looking around
- **WHEN** the rider drags or zooms the map while navigating
- **THEN** the map stops following and stays where they put it, and the recenter button appears

#### Scenario: Recentering
- **WHEN** the rider taps the recenter button
- **THEN** the map moves to their position at the navigation zoom, follows them again, and the button disappears

#### Scenario: No automatic return
- **WHEN** the rider moved the map and does not touch it for several minutes while riding on
- **THEN** the map stays where they put it, and the recenter button stays

#### Scenario: Off route while following
- **WHEN** the rider leaves the route while the map follows them
- **THEN** the map shows their position and the next knot together

### Requirement: Riding direction on the map
While navigating, the rider's position SHALL be shown as an arrow pointing in the direction they are riding, taken from their most recent movement on the ridden track, not from the compass, while the map stays north-up. When the rider has not moved recently, the position SHALL be shown as a dot without a direction.

#### Scenario: Riding
- **WHEN** the rider has ridden east for the last 20 m
- **THEN** their position is an arrow pointing east, and north is still up on the map

#### Scenario: Standing still
- **WHEN** the rider stops at a traffic light and has not moved 5 m for more than 10 s
- **THEN** their position is a dot without a direction

### Requirement: Ridden track
While navigating, the map SHALL show the track the rider has ridden since navigation started, as a line distinguishable from the planned route, made from the location fixes navigation already uses. Fixes too inaccurate to place the rider SHALL NOT be part of the track. The track SHALL exist only while navigating.

#### Scenario: Detour
- **WHEN** the rider leaves the route for a detour and comes back to it
- **THEN** the map shows the detour as part of the ridden track, next to the planned route

#### Scenario: Inaccurate fix
- **WHEN** a fix is less accurate than 50 m
- **THEN** it is not added to the ridden track

#### Scenario: Navigation ends
- **WHEN** navigation ends by Stop or by arriving
- **THEN** the ridden track is gone from the map

#### Scenario: Screen off
- **WHEN** the rider navigates with the screen off and opens the app again
- **THEN** the ridden track includes the stretch ridden with the screen off, from the fixes taken then, without the location being sampled more often than before

### Requirement: Display on while navigating in the app
While navigating and the app is in the foreground, the display SHALL stay on, overriding the system's display timeout. When the app is left or navigation ends, the system's normal display timeout SHALL apply again.

#### Scenario: Navigating with the app open
- **WHEN** the rider navigates with the app open and does not touch the phone for longer than the system's display timeout
- **THEN** the display stays on

#### Scenario: Turning the display off
- **WHEN** the rider presses the power button while navigating
- **THEN** the display turns off and navigation continues with spoken notices

#### Scenario: Leaving the app
- **WHEN** the rider switches to another app while navigating
- **THEN** the display turns off after the system's timeout as usual

#### Scenario: Navigation ends
- **WHEN** navigation ends by Stop or by arriving and the app stays open
- **THEN** the display turns off after the system's timeout as usual

#### Scenario: Planning
- **WHEN** the app is open and not navigating
- **THEN** the display turns off after the system's timeout as usual

### Requirement: Ferry arrival estimate
While navigating, the app SHALL estimate when the rider gets to each ferry still ahead on the route, from the route distance to it and the rider's pace on the route so far, renewed when navigation starts and whenever a knot is passed. Until the rider has covered 2 km of the route, it SHALL assume 16 km/h cycling and 4.5 km/h hiking. The popover of a ferry ahead on the progress strip SHALL show the estimate, rounded to five minutes, in one line that starts with it and tells whether the ferry is open then, in place of its status for now, as in "At 14:30: open until 19:00": in green when it is open then, and only the estimate without a color when the operating times do not tell.

#### Scenario: Arrival in the ferry list
- **WHEN** the rider navigates a route with a ferry ahead that runs "Apr-Oct: Mo-Su 07:00-19:00" and that they get to around 14:30 on Wednesday, 16 June 2027
- **THEN** tapping the ferry on the strip shows "At 14:30: open until 19:00" in green above the ferry's times, and no status for now

#### Scenario: Arrival when the times do not tell
- **WHEN** the rider navigates a route with a ferry ahead without operating times in OpenStreetMap and gets there around 14:30
- **THEN** the ferry's popover shows "At 14:30" without a color

#### Scenario: Ferry passed in the list
- **WHEN** the rider has passed the knot after a ferry
- **THEN** the ferry's popover shows its status for now, as the ferry list while planning

#### Scenario: Ferry passed
- **WHEN** the rider has passed the knot after a ferry
- **THEN** the strip shows no distance and arrival for it, and the app gives no warning about it

#### Scenario: Phone in another time zone
- **WHEN** the phone is set to London time and the rider gets to a ferry at 18:15 London time, which is 19:15 in the Netherlands
- **THEN** the ferry's popover says "At 19:15", the strip "19:15" and any warning "around 19:15", compared with the ferry's times in Dutch time

### Requirement: Ferry warning
When a ferry ahead is closed at the rider's estimated arrival, or closes within 15 minutes after it, according to its operating times in OpenStreetMap in the ferry region's local time, the app SHALL warn the rider once per ferry with a spoken notice, and the progress strip SHALL show that ferry's badge with a red ring and its arrival in red, and its popover the warning in red. When the operating times do not tell whether the ferry runs then, the app SHALL NOT warn.

#### Scenario: Closed when the rider gets there
- **WHEN** the rider starts navigating a cycling route at 18:00 on Wednesday, 16 June 2027, and the ferry Veer Broekhuizen, which runs "Apr-Oct: Mo-Su 07:00-19:00", is 20 km ahead
- **THEN** the app says "The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:15"

#### Scenario: Closes shortly after the rider gets there
- **WHEN** the same ferry is 12 km ahead at 18:05 on that day
- **THEN** the app says "The ferry Veer Broekhuizen closes at 19:00, shortly after you get there around 18:50"

#### Scenario: Not open yet when the rider gets there
- **WHEN** the same ferry is 8 km ahead at 05:45 on that day
- **THEN** the app says "The ferry Veer Broekhuizen is closed when you get there around 06:15, it opens at 07:00"

#### Scenario: Closed for the season
- **WHEN** the same ferry is 8 km ahead at 12:00 on Wednesday, 2 December 2026
- **THEN** the app says "The ferry Veer Broekhuizen is closed when you get there around 12:30, it opens on 1 April at 07:00"

#### Scenario: Open when the rider gets there
- **WHEN** the same ferry is 8 km ahead at 14:00 on Wednesday, 16 June 2027
- **THEN** the app gives no ferry warning

#### Scenario: The rider's own pace
- **WHEN** the rider has covered 10 km of the route in 50 minutes, and at 18:00 on Wednesday, 16 June 2027 the same ferry is 10 km ahead
- **THEN** the app says "The ferry Veer Broekhuizen closes at 19:00, shortly after you get there around 18:50", where the assumed 16 km/h would have given 18:40 and no warning

#### Scenario: Hiking
- **WHEN** the rider starts navigating a hiking route at 18:30 on Wednesday, 16 June 2027, with the same ferry 3 km ahead
- **THEN** the app says "The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:10"

#### Scenario: Falling behind
- **WHEN** the ferry was reachable when navigation started, and the rider is so slow that the estimate at a later knot is after its closing
- **THEN** the app warns at that knot

#### Scenario: Warned once
- **WHEN** the rider was warned about a ferry and passes further knots before it
- **THEN** the app does not warn about that ferry again

#### Scenario: Ferry without a name
- **WHEN** a ferry without a name in OpenStreetMap is closed when the rider gets there around 19:15
- **THEN** the app says "The ferry closes at 19:00, before you get there around 19:15"

#### Scenario: Times that cannot be told
- **WHEN** a ferry ahead runs "Mo-Sa 06:00-19:00; PH,Su 09:00-19:00" and the rider gets there at 08:00 on Good Friday, 26 March 2027, or a ferry ahead has no operating times in OpenStreetMap
- **THEN** the app gives no warning about that ferry

#### Scenario: Several ferries
- **WHEN** two ferries ahead are closed when the rider gets there
- **THEN** the app warns about both, the nearer one first

#### Scenario: Warning in the ferry list
- **WHEN** the rider navigates a route with a ferry ahead that they get to around 19:15 and that closes at 19:00 on that day
- **THEN** the ferry's badge on the strip has a red ring, "19:15" below it is red, and its popover shows "At 19:15: closed until tomorrow 07:00" in red

#### Scenario: Closed for the season in the ferry list
- **WHEN** the rider navigates a route with a ferry ahead that is closed until 1 May at 10:00 and gets there around 18:10
- **THEN** the ferry's popover shows "At 18:10: closed until 1 May 10:00" in red

#### Scenario: Screen off
- **WHEN** the screen is off when a ferry warning is due
- **THEN** the warning is spoken

#### Scenario: Offline
- **WHEN** the device has no internet access
- **THEN** the ferry warnings are given as online

### Requirement: Progress strip
While navigating, the panel SHALL show, below the number to follow, the route as a strip drawn to scale by distance along the route, in place of the knot badge row: the start, the stops, the end and the ferries marked and labeled, the other knots as small marks, the rider's place on the route as a dot, and the part of the route already ridden filled. While the panel is shown, the strip SHALL follow each position.

#### Scenario: Strip at the start
- **WHEN** the rider starts navigating a route from 24 via the stop 30 to 31, 12.4 km long, at 24
- **THEN** the strip shows 24 at its start, 30 at the distance of 30 along the route, 31 at its end, the other knots as small marks, and the dot at 24 with nothing filled

#### Scenario: Moving along
- **WHEN** the rider has ridden 4 km of that route
- **THEN** the dot is a third of the way along the strip and the strip is filled up to it

#### Scenario: Ferry on the strip
- **WHEN** the route crosses a ferry between 12 and 30
- **THEN** the strip marks the ferry with ⛴ between 12 and 30

#### Scenario: Long route
- **WHEN** the route passes 80 knots and has two stops
- **THEN** the strip fits the width of the panel without scrolling, with the start, the stops and the end labeled and the other knots as marks

#### Scenario: Off route on the strip
- **WHEN** the rider is off route
- **THEN** the dot stays at the last place on the route and is shown as off route

#### Scenario: Progress with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the strip after 4.0 of 12.4 km
- **THEN** it announces "4.0 of 12.4 km ridden, next knot 12"

### Requirement: Distances and arrivals on the strip
While navigating, the progress strip SHALL show below each labeled stop, ferry and end still ahead the distance along the route to it and the arrival, at the pace of "Ferry arrival estimate", following each position while shown. Where they would overlap those of a more important mark (the end, then ferries with a warning, then stops, then other ferries), they SHALL be left out; the start and passed marks SHALL show none.

#### Scenario: Ahead at the start
- **WHEN** the rider starts navigating a cycling route at 18:20 at 24, with the stop 30 at 5.2 km and the end 31 at 12.4 km
- **THEN** the strip shows "5.2 km" and "18:40" below 30, and "12 km" and "19:05" below 31

#### Scenario: Along the way
- **WHEN** the rider has ridden 400 m of that route
- **THEN** the strip shows "4.8 km" below 30 and "12 km" below 31

#### Scenario: The rider's own pace
- **WHEN** the rider has covered 10 km in 50 minutes and 10 km are left to the end at 18:00
- **THEN** the strip shows "10 km" and "18:50" below the end

#### Scenario: Ferry ahead on the strip
- **WHEN** a ferry is 3.0 km ahead at 18:20 on a cycling route
- **THEN** the strip shows "3.0 km" and "18:30" below the ferry

#### Scenario: Stop passed
- **WHEN** the rider has passed the stop 30
- **THEN** the strip shows no distance and arrival below 30

#### Scenario: Stops close together
- **WHEN** the stops 96, 37 and 62 are so close to the end 31 on the strip that their badges are left out
- **THEN** only 31 shows a distance and arrival

#### Scenario: Distances that would overlap
- **WHEN** a stop is shown with its badge, but its distance and arrival would overlap those of the end
- **THEN** only the end shows a distance and arrival

#### Scenario: Round trip
- **WHEN** the rider navigates a round trip from 24 back to 24
- **THEN** the end 24 shows the distance back to it along the route

#### Scenario: Off route on the strip ahead
- **WHEN** the rider is off route, 300 m from the next knot 12, and 30 is 4.4 km after 12
- **THEN** 30 shows "4.7 km", counted from 12 plus those 300 m

### Requirement: Rounded times and distances
Durations under an hour SHALL be shown rounded to the minute, at least "1 min"; longer durations, as "2 h 30 min", and the arrivals SHALL be rounded to five minutes, so they do not suggest more precision than the estimate has. Distances SHALL be shown in meters under 1 km, with one decimal under 10 km and in whole kilometers from 10 km, so they stay short.

#### Scenario: Short time
- **WHEN** a duration is 19 minutes and 30 seconds
- **THEN** it is shown as "20 min"

#### Scenario: Very short time
- **WHEN** a duration is 20 seconds
- **THEN** it is shown as "1 min"

#### Scenario: Long time rounded
- **WHEN** a duration is 2 hours and 33 minutes
- **THEN** it is shown as "2 h 35 min"

#### Scenario: Arrival rounded
- **WHEN** the rider arrives at 19:06 and 30 seconds by the estimate
- **THEN** the arrival is shown as "19:05"

#### Scenario: Short distance
- **WHEN** a distance is 800 m
- **THEN** it is shown as "800 m"

#### Scenario: Middle distance
- **WHEN** a distance is 3456 m
- **THEN** it is shown as "3.5 km"

#### Scenario: Long distance
- **WHEN** a distance is 200.2 km, or 9.96 km
- **THEN** it is shown as "200 km", or "10 km"

### Requirement: Ferries on the strip
While navigating, the progress strip SHALL show each ferry of the route as a badge, giving way only to the end where a ferry ahead with a warning would overlap, and to the end, the stops and the start otherwise. The arrival below a ferry ahead SHALL be green when it is open then. The ferry list SHALL NOT be shown while navigating.

#### Scenario: Closed ferry before a stop
- **WHEN** a ferry closed at the rider's arrival is so close to a stop on the strip that their badges would overlap
- **THEN** the strip shows the ferry with its distance and arrival, and not the stop

#### Scenario: Open ferry before a stop
- **WHEN** a ferry open at the rider's arrival is that close to a stop
- **THEN** the strip shows the stop, and not the ferry

#### Scenario: Open ferry ahead
- **WHEN** a ferry ahead is open when the rider gets there around 14:30
- **THEN** "14:30" below it is green

#### Scenario: No ferry list while navigating
- **WHEN** the rider navigates a route with ferries
- **THEN** the panel shows no ferry list below the strip

### Requirement: Ferry popover
While navigating, tapping a ferry on the progress strip SHALL open a popover with where it is, its name, its arrival line or, once passed, its status for now, its operating times, fee and website, noting that they are according to OpenStreetMap. Screen readers SHALL reach each ferry on the strip as a button.

#### Scenario: Ferry popover
- **WHEN** the rider taps the ferry between 76 and 02 on the strip
- **THEN** a popover shows "76 → 02", "Renkumse Veer", e.g. "At 21:10: closed until 1 May 10:00", the tagged times, the website and "Times according to OpenStreetMap"

#### Scenario: Popover of a passed ferry
- **WHEN** the rider taps a ferry already passed
- **THEN** its popover shows its status for now in place of an arrival

#### Scenario: Ferry with a screen reader
- **WHEN** a screen reader reaches the ferry between 76 and 02 on the strip
- **THEN** it announces it as a button, "Ferry 76 → 02, at 21:10 closed until 1 May 10:00"

### Requirement: Way to the route on the map
While navigating and off route, the map SHALL show a dashed straight line from the rider's position to the knot to head for. While the map follows the rider, it SHALL keep both the rider and that knot in view.

#### Scenario: Line to the knot
- **WHEN** the rider is off route with 02 as the next knot
- **THEN** the map shows a dashed line from their position to 02

#### Scenario: Both in view
- **WHEN** the rider is off route 1.7 km from 02 and the map follows them
- **THEN** the map shows both their position and 02

#### Scenario: Back on route
- **WHEN** the rider comes back on route
- **THEN** the dashed line is gone and the map follows the rider as before

### Requirement: Direction to the route
While navigating and off route, the header SHALL show, next to the distance to the next knot, an arrow pointing towards it relative to the rider's riding direction, and the compass direction in words (north, north-east, east, south-east, south, south-west, west, north-west). When the rider has not moved recently, the arrow SHALL point relative to north, as the map.

#### Scenario: Direction in the header
- **WHEN** the rider is off route, rides north and 02 is 1.7 km to the north-east
- **THEN** the header shows "Off route, next 02 1.7 km", an arrow pointing to the front right and "north-east"

#### Scenario: Standing still
- **WHEN** the rider has not moved recently and 02 is to the east
- **THEN** the arrow points to the right, as east on the map

#### Scenario: Direction with a screen reader
- **WHEN** a screen reader reaches the header while off route
- **THEN** it announces "Off route, next 02, 1.7 km to the north-east"

### Requirement: Direction outside the app
While navigating and off route, the Android notification and the iOS Live Activity SHALL show the distance and compass direction to the next knot, e.g. "1.7 km north-east to 02", updated when the distance changed by 100 m or more, without more location fixes than navigating does.

#### Scenario: Lock screen off route
- **WHEN** the rider is off route with the screen off, 1.7 km south-west of 02
- **THEN** the lock screen shows "Off route, next 02" and "1.7 km north-east to 02"

#### Scenario: Updated on the way
- **WHEN** the rider got 100 m closer to 02
- **THEN** the lock screen shows the new distance

#### Scenario: Back on route outside the app
- **WHEN** the rider comes back on route
- **THEN** the lock screen shows the number to follow and the progress line as before

### Requirement: Panel height while navigating
In portrait, when navigation starts, the panel SHALL take the height of its navigation part: the number to follow and the progress strip with its distances and arrivals, whatever height it had while planning. While navigating, it SHALL keep that height and show no handle to drag it. When navigation ends, it SHALL go back to its smallest planning height.

#### Scenario: Starting with the smallest panel
- **WHEN** the panel is at its smallest height in portrait and the rider starts navigating a route with the stop 30 at 5.2 km and the end 31 at 12.4 km
- **THEN** the panel shows the number to follow and the whole strip, with "5.2 km" and the arrival below 30 and "12 km" and the arrival below 31, without being dragged

#### Scenario: Starting with a high panel
- **WHEN** the panel is at half the screen in portrait and the rider starts navigating
- **THEN** the panel moves down to the height of the number to follow and the strip

#### Scenario: Dragging the panel
- **WHEN** the rider drags the panel down or up while navigating in portrait
- **THEN** the panel keeps the height of the number to follow and the strip with its distances and arrivals

#### Scenario: Ending navigation
- **WHEN** navigation ends by Stop or by arriving
- **THEN** the panel goes back to its smallest planning height

#### Scenario: Navigating in landscape
- **WHEN** the rider navigates in landscape
- **THEN** the side panel shows the number to follow and the whole strip, as before
