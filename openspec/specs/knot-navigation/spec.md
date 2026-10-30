# knot-navigation Specification

## Purpose
Guides the rider along a planned route knot by knot: it tells which number to follow at the next knot, by voice and on the lock screen, so the screen can stay off and the battery lasts.

## Requirements

### Requirement: Start navigation
A complete route SHALL be navigable. Navigation SHALL be started by the user while the app is open, and SHALL begin where the rider is: at a knot of the route, on its course, or at the route knot nearest to them. It SHALL need location access only while the app is in use, not always.

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
- **WHEN** the rider starts navigation 2 km away from the route, nearest to route knot 45
- **THEN** navigation begins with 45 as the next knot and says the rider is off route

#### Scenario: Incomplete route
- **WHEN** the route has no start, no end, or an unreachable leg
- **THEN** navigation cannot be started

#### Scenario: Location access denied
- **WHEN** the user starts navigation and denies location access
- **THEN** navigation does not start, and the app says that it needs location access to navigate

### Requirement: Number to follow
While navigating, the app SHALL show the number to follow at the next knot as its most prominent information, e.g. "At 45 follow 12". When the next knot is the end of the route, it SHALL say so instead.

#### Scenario: Next knot in the middle of the route
- **WHEN** the next knot is 45 and the route continues to 12
- **THEN** the app shows "At 45 follow 12", with 12 as the most prominent element

#### Scenario: Next knot is the end
- **WHEN** the next knot is 31, the end of the route
- **THEN** the app shows that 31 is the end of the route

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
The rider SHALL be told they are off route when they pass a knot that is not on the route, or are more than 100 m away from the course of the route. The notice SHALL name the next knot, e.g. "Off route, next 45". The app SHALL NOT plan a new route back.

#### Scenario: Passing a knot that is not on the route
- **WHEN** the rider heading to 45 passes knot 77, which is not on the route
- **THEN** the app says "Off route, next 45"

#### Scenario: Wrong turn between knots
- **WHEN** the rider heading to 45 leaves the course of the route and is more than 100 m away from it
- **THEN** the app says "Off route, next 45"

#### Scenario: Back on route
- **WHEN** the rider who is off route comes back within 100 m of the course, or passes a knot of the route
- **THEN** the app says they are back on route and names the next knot

#### Scenario: Without geometry
- **WHEN** the network's courses are not installed
- **THEN** off route is only detected by passing a knot that is not on the route

#### Scenario: Off route is announced once
- **WHEN** the rider stays off route for several minutes
- **THEN** the app says "Off route" once, not repeatedly

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
Navigation SHALL keep running with the screen off and while other apps are in use. While navigating, the app SHALL show the number to follow at the next knot outside the app, with a button to stop: on Android in an ongoing notification, on iOS in a Live Activity on the lock screen and in the Dynamic Island.

#### Scenario: Lock screen
- **WHEN** the rider is navigating and looks at the lock screen
- **THEN** it shows "At 45 follow 12", updated whenever the next knot changes

#### Scenario: Dynamic Island
- **WHEN** the rider is navigating on an iPhone with a Dynamic Island and uses another app
- **THEN** the Dynamic Island shows the next knot and the number to follow there

#### Scenario: Off route outside the app
- **WHEN** the rider is off route and looks at the lock screen
- **THEN** it shows that they are off route, and the next knot

#### Scenario: Stop from the lock screen
- **WHEN** the rider taps Stop in the notification or the Live Activity
- **THEN** navigation stops, and the notification or Live Activity goes away

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
Passing the last knot of the route SHALL be announced with "You've arrived" and SHALL end navigation. The user SHALL be able to stop navigation at any time in the app. The app SHALL use the location only while navigating, so the system's location indicator shows only then; ending navigation SHALL stop using it. On iOS, closing the app while navigating SHALL end navigation.

#### Scenario: Arriving
- **WHEN** the rider passes 31, the end of the route
- **THEN** the app says "You've arrived", navigation ends, and the notification or Live Activity goes away

#### Scenario: Stopping in the app
- **WHEN** the rider taps Stop in the app while navigating
- **THEN** navigation ends and the planned route is still there

#### Scenario: No location use after navigation
- **WHEN** navigation ended by arriving, by Stop, or because the app was closed or ended while navigating, and the app is started again or left in the background
- **THEN** the app does not use the location, and the system shows no location indicator for it

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
While navigating, the app SHALL estimate when the rider gets to each ferry still ahead on the route, from the route distance to it and the rider's pace on the route so far, renewed when navigation starts and whenever a knot is passed. Until the rider has covered 2 km of the route, it SHALL assume 16 km/h cycling and 4.5 km/h hiking. The ferry list SHALL show the estimate for each ferry ahead, rounded to five minutes.

#### Scenario: Arrival in the ferry list
- **WHEN** the rider navigates a route with a ferry ahead that they get to around 19:15
- **THEN** the ferry list shows "You get there around 19:15" with the ferry's status and times

#### Scenario: Ferry passed
- **WHEN** the rider has passed the knot after a ferry
- **THEN** the ferry list no longer shows when the rider gets there, and the app gives no warning about it

#### Scenario: Phone in another time zone
- **WHEN** the phone is set to London time and the rider gets to a ferry at 18:15 London time, which is 19:15 in the Netherlands
- **THEN** the ferry list and any warning say "around 19:15", compared with the ferry's times in Dutch time

### Requirement: Ferry warning
When a ferry ahead is closed at the rider's estimated arrival, or closes within 15 minutes after it, according to its operating times in OpenStreetMap in the ferry region's local time, the app SHALL warn the rider once per ferry with a spoken notice and show that ferry's arrival in red. When the operating times do not tell whether the ferry runs then, the app SHALL NOT warn.

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
- **WHEN** the rider navigates a route with a ferry ahead that they get to around 19:15 and that closes at 19:00
- **THEN** the ferry list shows "You get there around 19:15" in red

#### Scenario: Screen off
- **WHEN** the screen is off when a ferry warning is due
- **THEN** the warning is spoken

#### Scenario: Offline
- **WHEN** the device has no internet access
- **THEN** the ferry warnings are given as online
