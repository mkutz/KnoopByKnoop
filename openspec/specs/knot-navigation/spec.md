# knot-navigation Specification

## Purpose
Guides the rider along a planned route knot by knot: it tells which number to follow at the next knot, by voice and on the lock screen, so the screen can stay off and the battery lasts.

## Requirements

### Requirement: Start navigation
A complete route SHALL be navigable. Navigation SHALL be started by the user while the app is open, and SHALL begin at the first waypoint of the route: within 30 m of any of the nodes it is tagged on, plus as far as the position may be off, it SHALL begin after it; elsewhere it SHALL lead there, see "Start on the route nearby" for when the app asks first. Standing within 30 m of several consecutive knots at the start of the route, it SHALL begin after the last of them. On the way to the start, the notices SHALL say "To the start" instead of "Off route", the header SHALL show the way as in "Header on the way to the route", and reaching the start SHALL be told as passing a knot. Within 200 m of the start, the first leg's course SHALL count as the way there. It SHALL need location access only while the app is in use, not always.

#### Scenario: Start at the first knot
- **WHEN** the route runs 24, 45, 12, 31 and the rider starts navigation standing at knot 24
- **THEN** navigation begins with 45 as the next knot

#### Scenario: Start at a knot in the middle of the route
- **WHEN** the rider starts navigation standing at knot 12 of the route 24, 45, 12, 31, is asked and starts on the route nearby
- **THEN** the plan starts at 12 and navigation begins with 31 as the next knot

#### Scenario: Start between two knots
- **WHEN** the rider starts navigation on the course of the route between 45 and 12, is asked and starts on the route nearby
- **THEN** navigation begins with 12 as the next knot

#### Scenario: Start at a double knot
- **WHEN** the route runs 45, 46, 12, 46 is 10 m after 45, and the rider starts navigation standing between 45 and 46
- **THEN** navigation begins with 12 as the next knot

#### Scenario: Start with an imprecise position
- **WHEN** the rider starts navigation 50 m from knot 24 of the route 24, 45, 12, 31, with a first position accurate to 30 m
- **THEN** navigation begins with 45 as the next knot, without "To the start"

#### Scenario: Start at another node of a merged knot
- **WHEN** knot 24 of the route 24, 45, 12, 31 is tagged on two nodes 195 m apart, and the rider starts navigation standing at the node that is not the knot's position on the map
- **THEN** navigation begins with 45 as the next knot

#### Scenario: Start away from the route
- **WHEN** the rider starts navigation 2 km away from the course of the route 24, 45, 12, 31 and 3 km from 24
- **THEN** navigation begins with 24, the first knot, as the next knot and says "To the start, 24, 3 km" with its direction

#### Scenario: Incomplete route
- **WHEN** the route has no start, no end, or an unreachable leg
- **THEN** navigation cannot be started

#### Scenario: Location access denied
- **WHEN** the user starts navigation and denies location access
- **THEN** navigation does not start, and the app says that it needs location access to navigate

#### Scenario: Joining the route on the way to the start
- **WHEN** the rider rides to the first knot 06 of the route 06 → 02 → 28, and the way there follows the course of 06 → 02 against the route for 800 m
- **THEN** 06 stays the next knot, and the rider is told neither "Back on route" nor "Wrong direction" nor to turn back

#### Scenario: Following a course on the way to the start
- **WHEN** the rider heads for 24 and follows the course of the leg from 45 to 12 in the route's direction for 300 m
- **THEN** 24 stays the next knot

#### Scenario: Reaching the start
- **WHEN** the rider on the way to 24 comes within 30 m of it
- **THEN** the app says "Next 45, then 12", as when passing a knot, not "Back on route"

#### Scenario: On the course close to the first knot
- **WHEN** the rider heads for 24 and comes within 30 m of the course of the leg from 24 to 45, 80 m from 24
- **THEN** the rider is on the way to 24 along its course, with 24 still the next knot

#### Scenario: Start away from the route without courses
- **WHEN** the network's courses are not installed and the rider starts navigation nearest to route knot 45, 2 km from 24, without being asked
- **THEN** navigation begins with 24 as the next knot

### Requirement: Number to follow
While navigating, the panel's header SHALL show what to look for on the signs, as one row of badges, arrows and distances, without words.

**Close to the next knot** (from when the rider is reminded of it, see "Reminder before a knot"), the row SHALL show, in this order:
1. the next knot's badge, with the distance to it
2. an arrow
3. the number to follow there on a sign plate, as the most prominent element

**Farther away**, the row SHALL show the next knot's badge, plain, and the distance to it as the most prominent elements, then › and the badge of the number to follow there, plain and smaller.

**The sign plate** marks the number to follow at the knot the rider is close to, the one to look for on that knot's arrow signs. It SHALL be shown only when close: between knots, arrow signs on the way can point in any direction, so a plate's arrow would mislead. It SHALL be a rectangular plate around the badge with the arrow to the right of the number, like the knot's arrow signs: white with a border in the plate colour and the arrow in that colour in the Netherlands, the plate colour with a white arrow elsewhere. Every other badge in the row SHALL be a plain disc.

**When the next knot is the end**, the row SHALL show the end's badge with its corner symbol and the distance, and no number to follow.

**The distance** SHALL be along the route, or straight to the next knot while it is the first knot of the route.

**The badges** SHALL look like the knot's signs: in the Netherlands, the next knot filled and the number to follow white with a ring, as on the Dutch arrow signs; elsewhere, all filled, in the plate colour of `map-planning` "Route knots stand out". A knot the rider picked as a waypoint SHALL keep its corner symbol.

**Screen readers** SHALL get the row in words.

#### Scenario: Next knot in the middle of the route
- **WHEN** the next knot is 45, 150 m ahead, and the route continues to 12
- **THEN** the header shows the plain badge 45 with "150 m", an arrow, and 12 on a sign plate with → as the most prominent element, and no words

#### Scenario: Far from the next knot
- **WHEN** the next knot is 83, 1.2 km ahead, and the route continues to 30
- **THEN** the header shows the plain badge 83 and "1.2 km" as the most prominent elements, and › with a smaller plain badge 30, and no sign plate

#### Scenario: Coming close
- **WHEN** the rider heading to 83 comes within 200 m of it and is reminded "83 ahead, then 30"
- **THEN** the header shows the plain badge 83, an arrow, and 30 on a sign plate as the most prominent element

#### Scenario: Knot right after the last one
- **WHEN** the rider passes 45 and the next knot 12 is 150 m after it
- **THEN** the header shows 12, an arrow and 31 right away

#### Scenario: Next knot is the end
- **WHEN** the next knot is 31, the end of the route, 900 m ahead
- **THEN** the header shows the badge 31 with the end's corner symbol and "900 m", and no knot after it

#### Scenario: Distance off route
- **WHEN** the rider is off route, 1.7 km straight from where they get back onto the route
- **THEN** the header shows the off-route row of "Direction to the route" with "1.7 km", not the number to follow

#### Scenario: Distance to the first knot
- **WHEN** the rider started away from the route 24, 45, 12, 31, came back within 30 m of its course 80 m straight from 24, and 24 is still the next knot
- **THEN** the header shows 24, an arrow, 45 and "80 m", and outside the app "80 m to 24", not "0 m"

#### Scenario: Dutch knots in the header
- **WHEN** the rider is close to the Dutch knot 95 with 1 to follow
- **THEN** 95 is a filled dark green disc, and 1 is white with a dark green ring and number on a white sign plate with a dark green border and arrow

#### Scenario: German knots in the header
- **WHEN** the rider is close to 25 in North Rhine-Westphalia with 32 to follow
- **THEN** 25 is a red disc with a white ring and number, and 32 the same on a red sign plate with a white arrow

#### Scenario: Picked knot in the header
- **WHEN** the number to follow is 30, a stop the rider picked
- **THEN** its badge has the stop's corner symbol

#### Scenario: Same layout in every language
- **WHEN** the rider is 1.2 km before 83 with 30 to follow, once with the app in English and once in German
- **THEN** the header looks the same and has the same height

#### Scenario: Far from the next knot with a screen reader
- **WHEN** a screen reader reaches the header while the next knot 83 is 1.2 km ahead and 30 follows
- **THEN** it announces "Next 83, 1.2 km, then 30"

#### Scenario: Close to the next knot with a screen reader
- **WHEN** a screen reader reaches the header while the rider is 150 m before 45 with 12 to follow
- **THEN** it announces "At 45 follow 12, 150 m"

### Requirement: Passing a knot
A knot SHALL count as passed when the rider comes within 30 m of it, of any of the nodes it is tagged on. Passing the next knot SHALL make the knot after it the next knot and SHALL be announced with the next two knots, e.g. "Next 12, then 31". When the rider comes within 30 m of several consecutive knots of the route at once, all of them SHALL count as passed, and one announcement SHALL name the knot after the last of them.

#### Scenario: Passing the next knot
- **WHEN** the rider heading to 45 on the route 24, 45, 12, 31 comes within 30 m of 45
- **THEN** 12 becomes the next knot and the app says "Next 12, then 31"

#### Scenario: Riding close to a knot without passing it
- **WHEN** the rider passes 80 m away from the next knot on a parallel road
- **THEN** the knot does not count as passed

#### Scenario: Round trip
- **WHEN** the route is a round trip 24, 45, 12, 24 and the rider starts navigation at 24
- **THEN** 45 is the next knot, and reaching 24 again only counts as arriving after 45 and 12 were passed

#### Scenario: Passing a double knot
- **WHEN** the rider heading to 45 on the route 24, 45, 46, 12, with 46 10 m after 45, comes within 30 m of both 45 and 46 at once
- **THEN** both count as passed, 12 becomes the next knot, and the app says "Next 12" once, without a notice for 46

#### Scenario: Riding on after a double knot
- **WHEN** the rider passed the double knot 45, 46 and rides on towards 12
- **THEN** the distance to 12 counts down and the rider's place on the progress strip moves on

#### Scenario: Passing a merged knot at its other node
- **WHEN** knot 45 of the route 24, 45, 12, 31 is tagged on two nodes 195 m apart, and the rider heading to 45 comes within 30 m of the node that is not the knot's position on the map
- **THEN** 12 becomes the next knot and the app says "Next 12, then 31"

#### Scenario: Passing a merged knot that is not on the route
- **WHEN** knot 77, not on the route, is tagged on two nodes, and the rider heading to 45 comes within 30 m of the node that is not 77's position on the map
- **THEN** the app says "Off route, next 45" with the distance and direction to 45

### Requirement: Reminder before a knot
When the rider comes within 200 m of the next knot, the app SHALL remind them of the number to follow there, e.g. "45 ahead, then 12". The reminder SHALL be given once per knot.

#### Scenario: Approaching the next knot
- **WHEN** the rider heading to 45, with 12 after it, comes within 200 m of 45
- **THEN** the app says "45 ahead, then 12" once

#### Scenario: Knots close together
- **WHEN** the next knot is less than 200 m after the knot just passed
- **THEN** the app does not give a separate reminder for it, because the announcement on passing already named it

### Requirement: Skipping ahead
When the rider passes a later knot of the route than the next knot, navigation SHALL continue from that knot, as if the knots before it were passed. Before the start of the route is reached, passing a later knot SHALL NOT skip ahead.

#### Scenario: Shortcut
- **WHEN** the rider heading to 45 on the route 24, 45, 12, 31 passes 12 without passing 45
- **THEN** 31 becomes the next knot and the app says "Next 31"

#### Scenario: Passing a later knot on the way to the start
- **WHEN** the rider heading for the first knot 24 of the route 24, 45, 12, 31 passes 12
- **THEN** 24 stays the next knot and the app says nothing about 12

### Requirement: Off route
The rider SHALL be told they are off route when they pass a knot that is not on the route, or are more than 100 m away from the course of the route. They SHALL be back on route only when they pass a knot of the route or come within 30 m of the course, so a path running alongside the route does not count as the route. The notice SHALL name the next knot and the straight distance and compass direction to it, e.g. "Off route, next 45, 1.7 km to the north-east". While off route without a guided way, a guidance notice SHALL repeat the distance and direction when the rider got 500 m closer to the knot or its direction changed by more than 45° since the last notice. On a guided way, the turn notices SHALL replace these guidance notices. The planned route SHALL NOT change.

#### Scenario: Passing a knot that is not on the route
- **WHEN** the rider heading to 45 passes knot 77, which is not on the route
- **THEN** the app says "Off route, next 45" with the distance and direction to 45

#### Scenario: Wrong turn between knots
- **WHEN** the rider heading to 45 leaves the course of the route and is more than 100 m away from it, with 45 1.7 km to the north-east
- **THEN** the app says "Off route, next 45, 1.7 km to the north-east"

#### Scenario: Back on route
- **WHEN** the rider who is off route comes back within 30 m of the course, or passes a knot of the route
- **THEN** the app says they are back on route and names the next knot

#### Scenario: Parallel path
- **WHEN** the rider who is off route rides on a path 60 m beside the course
- **THEN** they are still off route, and the app does not say they are back on route

#### Scenario: Without geometry
- **WHEN** the network's courses are not installed
- **THEN** off route is only detected by passing a knot that is not on the route

#### Scenario: Off route is announced once
- **WHEN** the rider stays off route for several minutes
- **THEN** the app says "Off route" once, not repeatedly, and only the guidance or turn notices follow

#### Scenario: Getting closer
- **WHEN** the rider is off route without a guided way and got 500 m closer to 45 since the last notice, now 1.2 km to the north
- **THEN** the app says "Next 45, 1.2 km to the north"

#### Scenario: Direction changes
- **WHEN** the rider is off route without a guided way and the direction to 45 changed from north-east to south-east
- **THEN** the app says the distance and the new direction, e.g. "Next 45, 1.4 km to the south-east"

#### Scenario: Riding around without getting closer
- **WHEN** the rider is off route without a guided way, neither 500 m closer nor in a direction more than 45° off since the last notice
- **THEN** the app says nothing

#### Scenario: No guidance notices on a guided way
- **WHEN** the rider is on a guided way and got 500 m closer to 45
- **THEN** the app gives no distance and direction notice, only the turn notices

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
Navigation SHALL keep running with the screen off and while other apps are in use. While navigating, the app SHALL show outside the app what its panel shows: the next knot, far from it with the number to follow after it, close to it the number to follow there, see "Number to follow". With them, it SHALL show the distance to the next knot and the arrival at the end, renewed about every 100 m of the route, with a button to stop: on Android in an ongoing notification, from Android 16 as a Live Update with the route's progress; on iOS in a Live Activity on the lock screen and in the Dynamic Island, also with the route's progress. Showing them SHALL NOT need more location fixes than navigating does.

#### Scenario: Far from the next knot in German
- **WHEN** the app is in German, the rider is far from 29 with 25 to follow, and the system's text is larger than its default
- **THEN** the notification's title shows "Nächster 29, dann 25" completely

#### Scenario: Lock screen
- **WHEN** the rider is navigating, close to 45 with 12 to follow, and looks at the lock screen
- **THEN** it shows "At 45 follow 12" and, e.g., "150 m to 45 · end around 19:05"

#### Scenario: Far from the next knot outside the app
- **WHEN** the rider is 1.4 km from 29 with 25 to follow, and looks at the lock screen
- **THEN** it shows "Next 29, then 25" on Android and "Next 29", "then 25" with badges on iOS, and "1.4 km to 29 · end around 15:25"

#### Scenario: Dynamic Island
- **WHEN** the rider is navigating on an iPhone with a Dynamic Island and uses another app
- **THEN** the Dynamic Island shows the next knot and the number to follow there

#### Scenario: Dynamic Island with room for one number
- **WHEN** the Dynamic Island is shared, e.g. with a screen recording, so it has room for one number
- **THEN** it shows the knot the app shows largest: the next knot while far from it or off route, the number to follow when close

#### Scenario: Off route outside the app
- **WHEN** the rider is off route and looks at the lock screen
- **THEN** it shows that they are off route, and the next knot

#### Scenario: Stop from the lock screen
- **WHEN** the rider taps Stop in the notification or the Live Activity
- **THEN** navigation stops, and the notification or Live Activity goes away

#### Scenario: Progress outside the app
- **WHEN** the rider passes a knot with the screen off, and the next knot is 800 m ahead and the end is reached around 19:05 at their pace
- **THEN** the notification or Live Activity shows "800 m to 45 · end around 19:05"

#### Scenario: Distance on the way
- **WHEN** the rider has ridden 100 m further towards 45 since the line was last renewed
- **THEN** the notification or Live Activity shows the new distance and the arrival at their current pace, without the rider passing a knot and without more location fixes

#### Scenario: Live Update
- **WHEN** the rider is navigating on Android 16 or newer
- **THEN** the notification shows a bar along the route with their place and marks for stops and ferries, the knot to look for as a large badge, and the same knot as a chip in the status bar: the next knot while far from it, the number to follow when close

#### Scenario: Progress bar in the Live Activity
- **WHEN** the rider is navigating on an iPhone and looks at the lock screen, or at the expanded Dynamic Island
- **THEN** the Live Activity shows a bar along the route with their place and marks for stops and ferries, and the end, renewed with the distance, with the markers at both ends of the bar complete

#### Scenario: Live Activity in light and dark mode
- **WHEN** the rider navigates on an iPhone and the system is in light or in dark mode
- **THEN** the Live Activity's card on the lock screen is light or dark with it, with all its text readable, and follows a change of the mode with a later position

#### Scenario: Live Update turned off
- **WHEN** the rider has turned off Live Updates for the app, or the phone has Android 16 without its QPR2 update
- **THEN** the notification still shows the words, the distance and the bar, without the chip

#### Scenario: Older Android
- **WHEN** the rider is navigating on an Android version before 16
- **THEN** the notification shows the words, the distance and the arrival, and the knot to look for as a large badge

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
While navigating with the app open, the map SHALL follow the rider, keeping their position in view as they ride, until the user moves or zooms the map. Then the map SHALL stay where the user put it and a recenter button SHALL appear, which brings the map back to the rider at the zoom for what lies ahead (see "Zoom while following") and follows them again. The map SHALL NOT return to the rider on its own. While following, on the route and off it, the base map's labels and icons SHALL stay steady.

#### Scenario: Riding with the map following
- **WHEN** the rider rides along the route and has not touched the map since navigation started
- **THEN** the map moves with them, and their position stays in view

#### Scenario: Looking around
- **WHEN** the rider drags or zooms the map while navigating
- **THEN** the map stops following and stays where they put it, and the recenter button appears

#### Scenario: Recentering
- **WHEN** the rider taps the recenter button
- **THEN** the map moves to their position at the zoom for what lies ahead, follows them again, and the button disappears

#### Scenario: Recentering near a knot
- **WHEN** the rider moved the map away and taps the recenter button 150 m before the next knot
- **THEN** the map moves to their position zoomed in on the knot, as described in "Zoom at a knot"

#### Scenario: No automatic return
- **WHEN** the rider moved the map and does not touch it for several minutes while riding on
- **THEN** the map stays where they put it, and the recenter button stays

#### Scenario: No automatic zoom after looking around
- **WHEN** the rider zoomed the map by hand and then rides up to a knot
- **THEN** the map keeps the zoom they chose and doesn't zoom in on the knot

#### Scenario: Off route while following
- **WHEN** the rider leaves the route while the map follows them
- **THEN** the map shows their position and the end of the line to the route together, as described in "Way to the route on the map"

#### Scenario: Steady base map while following
- **WHEN** the map follows the rider past a supermarket and a parking with their icons on the base map
- **THEN** the icons and street names stay shown while they are in view, without disappearing and reappearing with every position

#### Scenario: Steady base map off route
- **WHEN** the map follows a rider who is off route or on the way to the first knot, showing them and the end of the line
- **THEN** the icons and street names stay shown while they are in view, as while following along the route

### Requirement: Riding direction on the map
While navigating, the rider's position SHALL be shown as an arrow pointing in the direction they are riding, taken from their most recent movement on the ridden track, not from the compass, while the map stays north-up. When the rider has not moved recently, the position SHALL be shown as a dot without a direction. The arrow SHALL be about 44 points long and the dot about 30 points across with its outline, so the position stands out at a glance.

#### Scenario: Riding
- **WHEN** the rider has ridden east for the last 20 m
- **THEN** their position is an arrow pointing east, and north is still up on the map

#### Scenario: Standing still
- **WHEN** the rider stops at a traffic light and has not moved 5 m for more than 10 s
- **THEN** their position is a dot without a direction

#### Scenario: Seeing the position at a glance
- **WHEN** the rider glances at the phone on the handlebar while navigating
- **THEN** their arrow is about 44 points long, larger than a knot's badge, and stands out from the route and the knots

### Requirement: Ridden track
While navigating, the map SHALL show the track the rider has ridden since navigation started, as a line distinguishable from the planned route, drawn on top of it and narrower than it, so the ridden part of the route shows and the route stays visible at its edges, made from the location fixes navigation already uses. Fixes too inaccurate to place the rider SHALL NOT be part of the track. A fix SHALL be part of the track only when it is farther from the track's last point than the inaccuracies of both together, and at least 5 m. A fix that would mean riding faster than 15 m/s on a cycling route, or 4 m/s on a hiking route, since the track's last point SHALL NOT be part of the track, and a point the next fix comes straight back from SHALL be taken out of it. The track SHALL exist only while navigating.

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

#### Scenario: Riding along the route
- **WHEN** the rider has ridden the first 2 km of the route as planned
- **THEN** those 2 km show the blue track on top of the route with the route's orange at both edges, and the rest of the route is orange with its direction arrows

#### Scenario: Standing with scattered fixes
- **WHEN** a hiker stands still for two minutes, with fixes accurate to 20 m that scatter up to 20 m around them
- **THEN** the track gets no new point

#### Scenario: Scatter while walking slowly
- **WHEN** a hiker walks 200 m straight east at 1.3 m/s, with fixes every 10 s accurate to 30 m that scatter up to 30 m to either side
- **THEN** the track's points are at least 60 m apart, and none of them lies west of the one before

#### Scenario: Impossible jump
- **WHEN** on a hiking route a fix lies 120 m from the track's last point, 10 s after it
- **THEN** it is not added to the ridden track

#### Scenario: Spike out and back
- **WHEN** on a cycling route a fix lands 80 m north of the track while riding east, and the next fix is back on the line further east
- **THEN** the track has no point 80 m north

#### Scenario: Out and back on the same path
- **WHEN** a cyclist rides 100 m off the route and back on the same path at 5 m/s, with a fix every 10 s
- **THEN** the ridden track shows the way out to its far end and back

#### Scenario: Riding fast with good fixes
- **WHEN** a cyclist rides at 8 m/s with fixes every second accurate to 5 m
- **THEN** the track has a point at least every 16 m along the way

### Requirement: Display on while navigating in the app
While navigating and the app is in the foreground, the display SHALL stay on, overriding the system's display timeout. Over the lock screen, and when the app is left or navigation ends, the system's normal display timeout SHALL apply.

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

#### Scenario: Coming back to the app
- **WHEN** the rider leaves the app while navigating and comes back, and navigation then ends or the phone is locked
- **THEN** the display turns off after the system's timeout as usual

#### Scenario: Over the lock screen
- **WHEN** the navigation shows over the lock screen and the rider doesn't touch the phone for longer than the system's lock screen timeout
- **THEN** the display turns off, and navigation continues with spoken notices

### Requirement: Ferry arrival estimate
While navigating, the app SHALL estimate when the rider gets to each ferry still ahead on the route, from the route distance to it and the rider's pace, renewed with every precise position on the route. The pace SHALL be the one over the last 5 km of the route, without the time the rider stood still or was off route. Until the rider has covered 2 km of the route, it SHALL assume 16 km/h cycling and 4.5 km/h hiking. The popover of a ferry ahead on the progress strip SHALL show the estimate, rounded to five minutes, in one line that starts with it and tells whether the ferry is open then, in place of its status for now, as in "At 14:30: open until 19:00": in green when it is open then, and only the estimate without a color when the operating times do not tell.

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

#### Scenario: Slowing down
- **WHEN** the rider rode 20 km at 20 km/h and then the last 5 km at 12 km/h, and 10 km are left to the end at 18:00
- **THEN** the strip shows "18:50" below the end, at 12 km/h, not "18:30"

#### Scenario: Between two knots
- **WHEN** the rider has covered more than 2 km, slows down, and is still before the next knot
- **THEN** the arrivals on the strip and in the coming knots follow without the rider passing a knot

#### Scenario: A break
- **WHEN** the rider rode 10 km at 20 km/h, stands still for 30 minutes, and rides on
- **THEN** the arrivals move later while they stand, and when they ride on the pace is still 20 km/h

#### Scenario: A detour
- **WHEN** the rider is off route for 10 minutes and comes back on the route
- **THEN** those 10 minutes do not count for the pace

#### Scenario: Closed at the renewed estimate
- **WHEN** the rider slows down between two knots so that a ferry ahead is closed at the estimate
- **THEN** the strip shows the ferry with its clock badge and its popover the warning at once, and the spoken warning comes at the next knot passed

### Requirement: Ferry warning
When a ferry ahead is closed at the rider's estimated arrival, or closes within 15 minutes after it, according to its operating times in OpenStreetMap in the ferry region's local time, the app SHALL warn the rider once per ferry with a spoken notice, and the progress strip SHALL show that ferry's badge with a dark clock badge at its corner and its arrival in bold, and its popover the warning in red. The strip SHALL NOT mark the warning with red, so it never reads like a German knot. When the operating times do not tell whether the ferry runs then, the app SHALL NOT warn.

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
- **THEN** the ferry's badge on the strip has a clock badge at its corner, "19:15" below it is bold, and its popover shows "At 19:15: closed until tomorrow 07:00" in red

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
While navigating, the panel SHALL show, below the number to follow, the route as a strip drawn to scale by distance along the route, in place of the knot badge row: the start, the stops, the end and the ferries marked and labeled, the start, the stops and the end with badges like on the map, the next knot and the number to follow there also with their badges, so every number in the header is on the strip too, the other knots as small marks, the rider's place on the route as a dot, and the part of the route already ridden filled. While the panel is shown, the strip SHALL follow each position.

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
- **THEN** the dot stays at the last place on the route and is shown as a ring with ✕, not in red

#### Scenario: Progress with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the strip after 4.0 of 12.4 km
- **THEN** it announces "4.0 of 12.4 km ridden, next knot 12"

#### Scenario: Knots of the header on the strip
- **WHEN** the rider is 150 m before 45, with 12 to follow, on a route from 24 to 31
- **THEN** the strip shows 45 just ahead of the dot and 12 after it as badges, not as marks

#### Scenario: Passed knot leaves the strip's labels
- **WHEN** the rider has passed 45 and the next knot is 12 with 31 to follow
- **THEN** 45 is a small mark again, and 12 and 31 are badges

#### Scenario: Next knot close to a stop
- **WHEN** the next knot's badge would overlap a stop's badge on the strip
- **THEN** the next knot's badge is shown, and the stop's is left out unless it is the next knot or the number to follow itself

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
While navigating and off route, the map SHALL show a dashed straight line from the rider's position to the nearest point of the route ahead of them: on the course of the leg to the next knot, after the last place the rider was on the route. Without the network's courses, or before the first knot, the line SHALL go to the next knot. On a guided way, the map SHALL show a solid line along that way instead of the dashed line. While the map follows the rider, it SHALL keep both the rider and the end of that line in view, except close before a tricky spot on a guided way (see "Zoom at a tricky spot").

#### Scenario: Line to the route
- **WHEN** the rider heading to 02 left the course to the west and is off route 150 m from it, with 02 1.7 km ahead
- **THEN** the map shows a dashed line from their position to the nearest point of the course ahead of where they left it, about 150 m long, not to 02

#### Scenario: Route behind the rider
- **WHEN** the rider is off route and the nearest point of the leg's course lies before the place where they left the route
- **THEN** the line goes to that place where they left the route, not back along the part they already rode

#### Scenario: Line to the knot
- **WHEN** the network's courses are not installed, or the rider is off route before the first knot 24
- **THEN** the map shows a dashed line from their position to the next knot

#### Scenario: Both in view
- **WHEN** the rider is off route 400 m from the course and the map follows them
- **THEN** the map shows both their position and the end of the line

#### Scenario: Doubtful junction on the way back
- **WHEN** the rider is off route on a guided way 1.2 km long and comes within 150 m of a doubtful junction on it
- **THEN** the map zooms in on the junction, and the end of the line may leave the view until they are past it

#### Scenario: Back on route
- **WHEN** the rider comes back on route
- **THEN** the dashed line is gone and the map follows the rider as before

#### Scenario: Line along the guided way
- **WHEN** the rider is off route on a guided way
- **THEN** the map shows a solid line along the way and no dashed line

#### Scenario: Back to the dashed line
- **WHEN** the rider is off route and the guided way can't be replaced after leaving it
- **THEN** the map shows the dashed line again

### Requirement: Direction to the route
While navigating and off route without a guided way, the header SHALL show, on its dark background (see `map-planning` "Light and dark appearance"):
- the to-the-route symbol: a small rider arrow joined by a dashed line to the route line, as on the map, with the arrow pointing away from the route
- an arrow pointing to where the rider gets back onto the route (the end of the line in "Way to the route on the map"), relative to the rider's riding direction
- the straight distance to that place, as the most prominent element
- › and the next knot's badge, small, as for the number to follow farther away, only while the rider is within 30 m of a course of the knot network

The header SHALL show no compass word. When the rider has not moved recently, the arrow SHALL point relative to north, as the map.

#### Scenario: Direction in the header
- **WHEN** the rider is off route without a guided way, rides north, and the place where they get back onto the course is 1.7 km to the north-east
- **THEN** the header is dark, with the to-the-route symbol, an arrow pointing to the front right and "1.7 km", and no compass word

#### Scenario: Back to the route, not to the knot
- **WHEN** the rider left the course of the leg to 02, 150 m from the course, and 02 is 1.4 km ahead along it
- **THEN** the header shows "150 m" and the arrow points to the course, not to 02

#### Scenario: Off the network
- **WHEN** the rider is off route on a road that is no path of the knot network
- **THEN** the header shows no knot badge

#### Scenario: Wrong turn within the network
- **WHEN** the rider passed 77, which is not on the route, and rides on along a course of the network
- **THEN** the header also shows › and the next knot 02 as a small badge

#### Scenario: Standing still
- **WHEN** the rider has not moved recently, has no guided way and the place where they get back onto the route is to the east
- **THEN** the arrow points to the right, as east on the map

#### Scenario: Without courses
- **WHEN** the network's courses are not installed and the rider is off route
- **THEN** the arrow and the distance point to the next knot

#### Scenario: Direction with a screen reader
- **WHEN** a screen reader reaches the header while off route without a guided way, 1.7 km from the route to the north-east, with 02 as the next knot
- **THEN** it announces "Off route, next 02, 1.7 km to the north-east"

### Requirement: Direction outside the app
While navigating and off route without a guided way, the Android notification and the iOS Live Activity SHALL show the distance and compass direction to the next knot, e.g. "1.7 km north-east to 02", updated when the distance changed by 100 m or more, without more location fixes than navigating does.

#### Scenario: Lock screen off route
- **WHEN** the rider is off route without a guided way and with the screen off, 1.7 km south-west of 02
- **THEN** the lock screen shows "Off route, next 02" and "1.7 km north-east to 02"

#### Scenario: Updated on the way
- **WHEN** the rider got 100 m closer to 02
- **THEN** the lock screen shows the new distance

#### Scenario: Back on route outside the app
- **WHEN** the rider comes back on route
- **THEN** the lock screen shows the number to follow and the progress line as before

### Requirement: Panel height while navigating
In portrait, when navigation starts, the panel SHALL take the height of its navigation part: the number to follow and the progress strip with its distances and arrivals, whatever height it had while planning. While navigating, it SHALL not be lower than that, and the rider SHALL be able to drag it higher to see the coming knots. When navigation ends, it SHALL go back to its smallest planning height.

#### Scenario: Starting with the smallest panel
- **WHEN** the panel is at its smallest height in portrait and the rider starts navigating a route with the stop 30 at 5.2 km and the end 31 at 12.4 km
- **THEN** the panel shows the number to follow and the whole strip, with "5.2 km" and the arrival below 30 and "12 km" and the arrival below 31, without being dragged

#### Scenario: Starting with a high panel
- **WHEN** the panel is at half the screen in portrait and the rider starts navigating
- **THEN** the panel moves down to the height of the number to follow and the strip

#### Scenario: Dragging the panel
- **WHEN** the rider drags the panel down while navigating in portrait
- **THEN** the panel stops at the height of the number to follow and the strip with its distances and arrivals

#### Scenario: Dragging the panel up
- **WHEN** the rider drags the panel up while navigating in portrait
- **THEN** the panel follows and shows the coming knots below the strip

#### Scenario: Ending navigation
- **WHEN** navigation ends by Stop or by arriving
- **THEN** the panel goes back to its smallest planning height

#### Scenario: Navigating in landscape
- **WHEN** the rider navigates in landscape
- **THEN** the side panel shows the number to follow, the whole strip and the coming knots below it

### Requirement: Strip labels at large text sizes
The distances and arrivals below the progress strip's marks SHALL grow with the system text size and SHALL never be cut off. Where larger labels would overlap, the ranking of "Distances and arrivals on the strip" SHALL decide which are left out.

#### Scenario: Largest text size
- **WHEN** the system text size is at its largest and the rider navigates to the end 94 at 4.1 km, arriving at 17:25
- **THEN** the strip shows "4.1 km" and "17:25" below 94, complete and readable

#### Scenario: Labels that would overlap at a large text size
- **WHEN** the system text size is large and a stop's distance and arrival would overlap those of the end
- **THEN** the end's distance and arrival are shown, and the stop's are left out

#### Scenario: Default text size
- **WHEN** the system text size is the default
- **THEN** the strip shows its labels as before

### Requirement: Number to follow at large text sizes
In every app language and at large system text sizes, the header SHALL show its badges, arrows, distances and Stop completely. The badges SHALL keep their size, like the badges on the map. Where a distance does not fit in the row, it SHALL be shown in a line below it. The header SHALL have the same height in every app language at the same text size.

#### Scenario: Number to follow at the largest text size
- **WHEN** the system text size is at its largest and the rider is 150 m before 94 with 41 to follow
- **THEN** the header shows 94, the arrow, 41 and Stop completely, and "150 m" completely below them

#### Scenario: Next knot at the largest text size
- **WHEN** the system text size is at its largest and the rider is 467 m before 94 with 41 to follow
- **THEN** the header shows 94, "467 m", › 41 and Stop completely

#### Scenario: Number to follow at the default text size
- **WHEN** the system text size is the default
- **THEN** the distance stays in the row with the badges

#### Scenario: Off route in German
- **WHEN** the app is in German and the rider is off route, 1.7 km from the route
- **THEN** the header shows the to-the-route symbol, the arrow, "1.7 km" and Stop completely, at the same height as in English

### Requirement: Coming knots
While navigating, the panel SHALL list the coming knots of the route below the progress strip, in riding order from the next knot to the end: each with its badge as on the map, the distance to it along the route and the arrival, at the pace of "Ferry arrival estimate", following each position while shown. Ferries SHALL be listed between the knots they connect, with their arrival state as on the strip. Passed knots SHALL NOT be listed.

#### Scenario: Coming knots at the start
- **WHEN** the rider starts navigating the route 24, 45, 30 (stop), 12, 31 at 18:20 at 24, with 45 at 1.1 km, 30 at 5.2 km, 12 at 8.0 km and 31 at 12.4 km
- **THEN** the list shows 45 "1.1 km", 30 "5.2 km 18:40", 12 "8.0 km", 31 "12 km 19:05", each with its arrival, 30 and 31 with their stop and end badges

#### Scenario: After passing knots
- **WHEN** the rider has passed 45 and 30 of that route
- **THEN** the list starts with 12 and no longer shows 45 and 30

#### Scenario: Ferry in the list
- **WHEN** the route crosses a ferry between 12 and 31 that is closed at the rider's arrival
- **THEN** the list shows the ferry between 12 and 31, with its arrival in red as on the strip

#### Scenario: Off route
- **WHEN** the rider is off route 1.7 km from the next knot 12
- **THEN** the list starts with 12 at "1.7 km", and the distances to the knots after it count from 12 plus 1.7 km

#### Scenario: Long route
- **WHEN** the route has 80 knots ahead
- **THEN** the list shows them all, and scrolls

#### Scenario: Coming knots with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches 30 in the list
- **THEN** it announces "Knot 30, stop, 5.2 km, 18:40"

### Requirement: Showing a coming knot on the map
Tapping a knot in the list of coming knots SHALL show that knot on the map, with the panel back at the height of the number to follow and the strip in portrait, so the knot is not hidden by it. The map SHALL then stop following the rider, as when the rider moves the map, and the recenter button SHALL bring it back to the rider.

#### Scenario: Tapping a coming knot
- **WHEN** the rider has dragged the panel up in portrait and taps 12 in the list while the map follows them
- **THEN** the panel goes down to the height of the number to follow and the strip, the map shows 12 at the navigation zoom, stops following the rider, and the recenter button appears

#### Scenario: Back to the rider
- **WHEN** the rider taps the recenter button after that
- **THEN** the map moves to the rider at the navigation zoom and follows them again

### Requirement: Navigation over the lock screen
On Android, while navigating with the app on top when the phone is locked, the app SHALL show the navigation over the lock screen when the phone is woken, without unlocking. Only the navigation SHALL be reachable there: the place search SHALL be hidden, and anything else SHALL need unlocking. Stopping SHALL need holding the stop button for about a second. The button SHALL be round and the size of the usual Stop button, and SHALL look different from it: outlined, with a ring around it that fills while it is held, so its behavior shows without reading it. A short tap SHALL show the hint "Hold to stop". The notification SHALL stay available. When navigation ends, the lock screen SHALL show as usual.

#### Scenario: Waking the phone on the ride
- **WHEN** the rider locks the phone with the app on top while navigating, and wakes it
- **THEN** the map and the header show without unlocking, with the current distances

#### Scenario: Audio playing
- **WHEN** the same rider has audio playing
- **THEN** the navigation shows over the lock screen in the same way, with no media player above it, and pulling the notifications down shows the player and the navigation's notification

#### Scenario: The place search
- **WHEN** the navigation shows over the lock screen
- **THEN** the place search isn't shown

#### Scenario: Before touching the stop button
- **WHEN** the navigation shows over the lock screen
- **THEN** the stop button is round and outlined, with an empty ring, the size of the solid red Stop button in the app and unlike it

#### Scenario: A touch on the stop button
- **WHEN** the rider taps the stop button over the lock screen
- **THEN** "Hold to stop" shows above the button for a few seconds, the ring fills a little and empties again, and navigation continues

#### Scenario: Letting go early
- **WHEN** the rider lets go of the stop button before it is full
- **THEN** the ring empties again, and navigation continues

#### Scenario: Without animations
- **WHEN** the phone's animations are turned off and the rider taps the stop button
- **THEN** navigation continues, and it still takes a hold of about a second to stop

#### Scenario: Holding the stop button
- **WHEN** the rider holds the stop button over the lock screen
- **THEN** the ring fills around the button over about a second, and when it is closed, navigation stops and the lock screen shows as usual

#### Scenario: Stop with a screen reader
- **WHEN** a screen reader reaches the stop button over the lock screen
- **THEN** it announces "Hold to stop", and its action stops navigation

#### Scenario: Unlocking
- **WHEN** the rider unlocks the phone while the navigation shows over the lock screen
- **THEN** within about a second the place search and the usual Stop button are back, and navigation continues

#### Scenario: The notification
- **WHEN** the rider pulls the notifications down while the navigation shows over the lock screen
- **THEN** the notification shows with its Stop button, and stopping there works as before

#### Scenario: The lock screen behind the navigation
- **WHEN** the rider flicks the screen up while the navigation shows over the lock screen
- **THEN** the lock screen behind it shows, with the notification

#### Scenario: Another app on top
- **WHEN** the rider locks the phone with another app on top while navigating
- **THEN** the lock screen shows as usual, with the notification

#### Scenario: Not navigating
- **WHEN** the rider locks the phone with the app on top while not navigating
- **THEN** the lock screen shows as usual

### Requirement: Riding the line through a signpost
While navigating, a rider following any course of a connection to a signpost of its knot SHALL NOT be told they are off route for it, and passing that signpost SHALL count as passing the knot.

#### Scenario: Following the line shown
- **WHEN** the rider on 06 → 02 → 28 follows the branch to the signpost near the L 16, which at some point is more than 100 m from the connection's main course
- **THEN** they stay on route, and passing that signpost makes 28 the next knot

#### Scenario: The other branch
- **WHEN** the rider on the same route takes the branch to the 02 by the river instead
- **THEN** they stay on route, as this is a course of the connection too

#### Scenario: Away from all courses
- **WHEN** the rider is more than 100 m from every course of the current leg and its neighbors
- **THEN** they are off route, as before

### Requirement: Straight ways to a knot with several signposts
Wherever navigation gives a straight distance or direction to the next knot, or draws a dashed line to it, and wherever it decides by the straight distance to that knot whether the rider is near it, it SHALL measure to the signpost the route's line uses: where the line first reaches the knot, or for the first knot of the route, where the line leaves it. Passing any signpost of the knot SHALL still count as passing it. Without the network's courses, it SHALL measure to the knot's main position.

#### Scenario: Approaching the first knot at the signpost the route leaves from
- **WHEN** the route 02, 28 leaves 02 at its north-west signpost, its main position is by the river, and the rider rides towards the north-west signpost, now 80 m from it
- **THEN** the app shows "At 02 follow 28" and "80 m", and the distance shrinks as they get closer

#### Scenario: Dashed line to the first knot
- **WHEN** the rider is off route before the first knot 02 of the route 02, 28, which leaves 02 at its north-west signpost
- **THEN** the dashed line ends at the north-west signpost, not at the one by the river

#### Scenario: Off route, heading for a knot on the route
- **WHEN** the rider on the route 06, 02, 28 is off route with 02 next, and the line reaches 02 at its north-west signpost, 1.2 km to the north
- **THEN** the app says "Off route, next 02, 1.2 km to the north", measured to that signpost, in the notice, the header and outside the app

#### Scenario: The route uses the other signpost
- **WHEN** the rider on the route 06, 02, 01 is off route with 02 next, and the line reaches 02 at its signpost by the river
- **THEN** the distance and direction go to the signpost by the river

#### Scenario: Line through both signposts
- **WHEN** the rider on the route 28, 02, 01 heads for 02, and the line reaches its north-west signpost first and runs on to the one by the river
- **THEN** the reminder, and the distance and direction while off route, go to the north-west signpost, and on the route 01, 02, 28 to the one by the river

#### Scenario: Reminder at the signpost used
- **WHEN** the rider on the route 06, 02, 28 comes within 200 m of the north-west signpost of 02, more than 200 m from its main position
- **THEN** the app says "02 ahead, then 28" once

#### Scenario: Passing the other signpost
- **WHEN** the rider heading for the north-west signpost of 02 passes its signpost by the river instead
- **THEN** 02 counts as passed and the app names the next knot

#### Scenario: A knot with one signpost
- **WHEN** the next knot has one position
- **THEN** the distances and directions are the same as before this change

#### Scenario: Without courses
- **WHEN** the network's courses are not installed and the rider is off route with 02 next
- **THEN** the distance, the direction and the dashed line go to the main position of 02

### Requirement: Leg ahead on the map
While navigating, the map SHALL show the course to the next knot thick and with arrows, and the rest of the route thin and without arrows. When a knot is passed, the course to the new next knot SHALL take its place. The ridden track SHALL stay on top of the route. Starting on the route nearby on a stretch the route uses in both directions, the plan SHALL start on its first use.

#### Scenario: Which way at a fork
- **WHEN** the rider nears a fork where the route also comes back on the other branch, with 28 as the next knot
- **THEN** only the course to 28 is thick and has arrows, so the map shows which way to go now

#### Scenario: Passing a knot
- **WHEN** the rider passes 28 with 27 as the new next knot
- **THEN** the course to 27 is thick and has arrows, and the course to 28 is thin

#### Scenario: Before the first knot
- **WHEN** the rider is off route before the first knot of the route
- **THEN** no course stands out, the route is thin, and the dashed line leads to the first knot

#### Scenario: Off route
- **WHEN** the rider is off route with 28 as the next knot
- **THEN** the course to 28 stays thick with its arrows

#### Scenario: Starting on a stretch used twice
- **WHEN** the rider starts on the route nearby on the round trip 02, 27, 02, which rides 02, 28, 27, 28, 02, on the course between 02 and 28, not at a knot
- **THEN** the plan starts there on the way out and 28 is the next knot, not 02 on the way back

#### Scenario: Ending navigation
- **WHEN** navigation ends
- **THEN** the route is drawn as while planning, with arrows along all of it

### Requirement: Riding the wrong way
While navigating with the network's courses, a rider within 30 m of the course of the current leg who has ridden at least 150 m back along it from the furthest place they reached on that leg, and has moved at least 100 m in a straight line from that place, SHALL be told they are riding the wrong way. The app SHALL say it once and show it until they turn around, pass the next knot or leave the course. It SHALL NOT plan a new route.

#### Scenario: Riding back along the route
- **WHEN** the rider heading to 28 joined the route 2.0 km before 28 and has ridden 150 m back along it, away from 28
- **THEN** the app says "Wrong direction, turn around, next 28" once

#### Scenario: Wrong direction in the panel
- **WHEN** the rider is riding the wrong way, 2.2 km along the route from 28
- **THEN** the header is dark and shows a U-turn symbol, the badge 28 and "2.2 km", without words, the rider's dot on the strip is a ring with ✕, and the map shows no dashed line and keeps following them

#### Scenario: Wrong direction outside the app
- **WHEN** the rider is riding the wrong way with the screen off or in another app
- **THEN** the notification or Live Activity shows "Wrong direction, next 28" with its warning look, and the Dynamic Island shows the warning sign and 28

#### Scenario: In the app's language
- **WHEN** the app is in German and the rider is riding the wrong way
- **THEN** the app says "Falsche Richtung, bitte wenden, nächster Knotenpunkt 28", and the header looks as in English

#### Scenario: Turning around
- **WHEN** the rider who was told they ride the wrong way turns around and has ridden 50 m along the route towards 28
- **THEN** the app says "Back on route, next 28" and shows the route's usual header

#### Scenario: Turning back shortly
- **WHEN** the rider rides 100 m back along the route, e.g. to pick something up, and goes on
- **THEN** the app says nothing

#### Scenario: A place that jumps
- **WHEN** the course doubles back past itself, and the nearest place on it jumps 300 m back while the rider has moved 20 m
- **THEN** the app says nothing

#### Scenario: Leaving the course while riding the wrong way
- **WHEN** the rider riding the wrong way gets more than 100 m away from the course
- **THEN** the app says they are off route, as on any other way off the course

#### Scenario: Passing the next knot
- **WHEN** the rider riding the wrong way passes the next knot by another way
- **THEN** the app gives the usual notice for the knot after it, and the warning is gone

#### Scenario: Riding back again later
- **WHEN** the rider turned around, rode on, and later rides 150 m back along the route again
- **THEN** the app says it again

#### Scenario: On the way back of an out-and-back route
- **WHEN** the route goes 02 → 01 → 02, and the rider passed 01 and rides the same way back
- **THEN** the app does not say they ride the wrong way

#### Scenario: Following the branch to another signpost
- **WHEN** the rider follows the course to a signpost of the next knot that is not on the main course
- **THEN** the app does not say they ride the wrong way

#### Scenario: Before the first knot
- **WHEN** the first knot is still the next knot
- **THEN** the app does not say they ride the wrong way

#### Scenario: Without geometry
- **WHEN** the network's courses are not installed
- **THEN** the app does not say they ride the wrong way

### Requirement: Leaving the guided way
When the rider is more than 30 m from the guided way on two location fixes in a row, the app SHALL find a new way from their position and riding direction, at most once every 10 seconds. It SHALL keep the last way until it has a new one. While the rider is more than 15 m from the way, the distances shown SHALL include how far they are from it, and the line on the map SHALL start at the rider.

#### Scenario: Missing a turn
- **WHEN** the rider rides past a turn and is more than 30 m from the way on two fixes
- **THEN** the app finds a new way, and the next turn notice is about the new way

#### Scenario: A single inaccurate fix
- **WHEN** one fix is more than 30 m from the way and the next is on it again
- **THEN** the way stays as it is

#### Scenario: No new way
- **WHEN** no new way can be found
- **THEN** off route continues without a guided way

#### Scenario: Riding away from the way
- **WHEN** the rider is 40 m from a way with 320 m left, and hasn't been more than 30 m away on two fixes yet
- **THEN** the distance left shown is "360 m", and the line on the map runs from the rider to the way

### Requirement: Turn notices on the guided way
On a guided way, the app SHALL say each turn once, about 100 m before it, e.g. "In 100 m, turn left". Turns within 40 m of each other SHALL count as one. Continuing straight on SHALL need no notice, and neither SHALL a bend where every other way at the junction turns at least 30° more than the way to take. Several turns at one junction that add up to straight on SHALL be said as straight on. When a way starts against the riding direction, the app SHALL say to turn back, and SHALL show it until the rider follows the way. The notice SHALL be spoken, also with the screen off and in the app's language.

#### Scenario: A turn ahead
- **WHEN** the rider is 100 m from a left turn on the guided way
- **THEN** the app says "In 100 m, turn left" once

#### Scenario: Turn close to the start of the way
- **WHEN** a new way has its first turn 40 m ahead
- **THEN** the app says "Turn left now", after the off-route notice

#### Scenario: Two turns together
- **WHEN** the way turns right and, 30 m later, left
- **THEN** the app gives one notice, using the overall direction of the two

#### Scenario: Straight on
- **WHEN** the way continues straight on over a junction, staying on the same street
- **THEN** the app says nothing there

#### Scenario: A bend without a choice
- **WHEN** the path bends right at a junction, and the only other way there turns left by more than 30° further
- **THEN** the app says nothing there

#### Scenario: A bend with a choice
- **WHEN** the way bends right at a junction, and another way there continues straighter
- **THEN** the app says the turn

#### Scenario: Crossing with an offset
- **WHEN** the way turns right and, 30 m later, left at a crossing, and continues in the same direction
- **THEN** the app says "In 100 m, go straight on"

#### Scenario: Way behind the rider
- **WHEN** a new way starts against the riding direction
- **THEN** the app says "Turn back now", and the header shows "Turn back" until the rider has followed the way for 15 m

#### Scenario: Still riding away
- **WHEN** the rider ignores it, and the next new way also starts against the riding direction
- **THEN** the header shows "Turn back" again, and the app doesn't say it again

#### Scenario: Screen off
- **WHEN** the rider reaches a turn notice with the screen off
- **THEN** it is spoken, without more location fixes than off route uses without a guided way

### Requirement: Switching between street and path on the guided way
Where the guided way changes at a junction from a street or road to a cycle path or another path, or back to a street, the notice SHALL name the way to switch to, also where the way otherwise goes straight on or bends and would need no notice. A switch that adds up to straight on SHALL be said without a direction, and a switch with a turn with the turn. A change between a cycle path and another path is no switch.

#### Scenario: Cycle path beside the street
- **WHEN** the rider rides on a street, and at a roundabout the way leaves it for the cycle path beside the street ahead, which adds up to straight on
- **THEN** the app says "In 100 m, switch to the cycle path", not "go straight on"

#### Scenario: Turning onto a cycle path
- **WHEN** the way turns right from a street onto a cycle path
- **THEN** the app says "In 100 m, turn right, onto the cycle path"

#### Scenario: Back to the street
- **WHEN** the way leaves a cycle path for the street beside it
- **THEN** the app says "In 100 m, switch to the street"

#### Scenario: Onto a path
- **WHEN** the way leaves a street for a footway open to bicycles
- **THEN** the app says "In 100 m, switch to the path"

#### Scenario: From one path to another
- **WHEN** the way goes from a cycle path straight on onto a footway open to bicycles
- **THEN** the app says nothing there

#### Scenario: Crossing a street on a path
- **WHEN** the way follows a cycle path, runs 20 m along a street to cross it, and continues on a cycle path
- **THEN** the app says no switch there

#### Scenario: Switch in the header
- **WHEN** a switch to the cycle path is the next notice
- **THEN** the header shows "To the cycle path in 100 m"

### Requirement: Guided way in the header and outside the app
On a guided way, the header, the Android notification and the iOS Live Activity SHALL show the next turn with its distance, and the distance left along the way, in place of the straight distance. The header SHALL show them without words, on its dark background: the to-the-route symbol, the turn as an arrow with its distance, the street name of the turn when known, and the distance left along the way.

#### Scenario: In the header
- **WHEN** the rider is off route with next knot 02, a left turn into Weierstraße 120 m ahead and 1.9 km left along the way
- **THEN** the header shows the to-the-route symbol, a left-turn arrow with "120 m", "Weierstraße" and "1.9 km", and no words of the app

#### Scenario: On the lock screen
- **WHEN** the same rider looks at the lock screen
- **THEN** it shows "Off route, next 02" and "Left in 120 m · 1.9 km"

#### Scenario: Distance to the course
- **WHEN** the guided way ends on the course of the route, 300 m ahead
- **THEN** the distance left shown is "300 m", to the route and not to the next knot

#### Scenario: Distances ahead on a guided way
- **WHEN** the rider is on a guided way with 140 m left, ending on the course 1.1 km before 82
- **THEN** the progress panel shows 1.2 km to 82, and the knots after it and the arrival times count from there

#### Scenario: Header with a screen reader
- **WHEN** a screen reader reaches the header on a guided way
- **THEN** it announces "Off route, next 02, Left in 120 m, Weierstraße, 1.9 km"

### Requirement: Guided way back to the route
While navigating a cycling route and off route, the app SHALL find a way along suitable paths from the rider to the end of the line in "Way to the route on the map", as soon as the path data allows. It SHALL NOT change the planned route and SHALL NOT need network access at that moment.

#### Scenario: Starting away from the route
- **WHEN** the rider starts navigating at home, off route, with the squares installed and 24 as the first knot
- **THEN** the app finds a way to 24 along suitable paths and guides the rider along it

#### Scenario: Wrong turn
- **WHEN** the rider left the course of the leg to 45 and is off route with the squares installed
- **THEN** the app finds a way to the course ahead of where they left it

#### Scenario: Without courses
- **WHEN** the network's courses aren't installed and the rider is off route with the squares installed
- **THEN** the app finds a way to the next knot

#### Scenario: Path data arrives on the way
- **WHEN** the rider is off route without a way, and the squares between them and the target finish downloading
- **THEN** the app finds a way at the next location fix and guides the rider along it

#### Scenario: No way, no path data, far away or hiking
- **WHEN** no way can be found, the squares aren't installed, the target is more than 20 km away, or the route is a hiking route
- **THEN** off route works without a guided way, with the straight line, distance and compass direction

#### Scenario: Beyond the path data
- **WHEN** the rider is off route where no squares are installed, e.g. after starting 6 km from the route
- **THEN** off route works without a guided way, with the straight line, distance and compass direction

#### Scenario: Reaching the path data
- **WHEN** the same rider gets to where the squares are installed
- **THEN** the app finds a way and guides the rider along it, within 10 seconds

#### Scenario: Back on route with a guided way
- **WHEN** the rider following a guided way comes within 30 m of the course
- **THEN** the guided way ends, and the app says they are back on route, as in "Off route"

#### Scenario: Skipping ahead on the guided way
- **WHEN** the rider left the route on the leg to 45 and, following a guided way back, passes 12, a later knot of the route
- **THEN** the guided way ends and navigation continues from 12, as when skipping ahead; on a guided way to the start, passing a later knot does not skip ahead, see "Skipping ahead"

### Requirement: Navigating with route points
A route point SHALL be passed, reached and aimed at like a knot, with the same radius, also around the end of its way where the way stops short of the point, and SHALL count for reminders, distances and arrival. Everywhere a number would name it, on screen, outside the app and in the voice, it SHALL be named as the start, the stop or the end in the app's language. Navigating SHALL NOT compute a route point's way anew.

#### Scenario: Starting at the point
- **WHEN** the rider starts navigating at a route point and the first knot is 02, then 28
- **THEN** the first notice is the usual "Next 02, then 28"

#### Scenario: Starting away from the point
- **WHEN** the rider starts navigating 400 m from the route point at the start
- **THEN** they are on the way to the start, told "To the start" with the way there, in words instead of a number

#### Scenario: Joining further along
- **WHEN** the rider on the way to the route point at the start reaches the route between the point and 02 without reaching the point
- **THEN** the point stays the next waypoint, as on the way to any start

#### Scenario: A stop next
- **WHEN** the rider passes 28 and a route point is the next stop, then 27
- **THEN** the notice names the stop and 27, and the same words are on the lock screen and spoken

#### Scenario: The last knot before the end
- **WHEN** the rider passes 81 and the end is a route point
- **THEN** the notice names the end instead of a number, and the same words are on the lock screen and spoken

#### Scenario: Reminder and arrival
- **WHEN** the rider nears a route point at the end and then comes within the radius
- **THEN** the reminder says that the end is coming, and arrival is announced as for a knot

#### Scenario: A point the way does not reach
- **WHEN** the way along paths ends 60 m from the route point, as the point is inside a park, and the rider reaches the end of the way
- **THEN** the point counts as reached

#### Scenario: Off route with the end next
- **WHEN** the rider is off route and the route point at the end is next
- **THEN** the notice gives the way to the end in words, and the map's dashed line goes to the route ahead, on the way to the point, as in "Way to the route on the map"

#### Scenario: On the strip
- **WHEN** a route with route points is navigated
- **THEN** the strip marks them with the flag, and its distances count only the ridden part of their connections and their ways

#### Scenario: A round trip from a route point
- **WHEN** the route starts and ends at the same route point and the rider leaves it
- **THEN** leaving does not count as arriving, and arrival is announced only after the waypoints before the end were passed

#### Scenario: All languages
- **WHEN** the app's language is German, Dutch or French
- **THEN** the wording for the start, stop and end, and "To the start", is in that language, in the texts and the voice

### Requirement: Riding the way to a route point
On a way along paths to a route point, the rider SHALL be told the distance and direction with the line on the map, with no directions at each turn, and leaving the way SHALL count as off route as on a course. On a straight line, which cannot be followed, the rider SHALL NOT be told they are off route, and SHALL be told the distance and direction to where it leads.

#### Scenario: Following a way along paths
- **WHEN** the rider rides along the way to a stop
- **THEN** they stay on route, and the distance to the stop counts down along the way

#### Scenario: Leaving a way along paths
- **WHEN** the rider moves more than the off route distance away from the way
- **THEN** they are off route, as on a course

#### Scenario: A straight line
- **WHEN** the next stretch is a straight line of 800 m to a stop, and the rider rides around a building 200 m from it
- **THEN** they are not told they are off route, and see the distance and direction to the stop

#### Scenario: Turning round at a detour
- **WHEN** the rider reaches a stop at the end of a way and turns back along it
- **THEN** the turn is not taken for riding the wrong way, and the next knot is named

#### Scenario: Without internet access
- **WHEN** the rider navigates a route with a way along paths to a route point, without internet access
- **THEN** the way and its guidance work as with internet access

### Requirement: Start on the route nearby
When navigation is started more than 30 m from the start and a place on the course before the first stop, more than 200 m along it from the start, is less than half as far as the start, the app SHALL ask once whether to ride to the start or to start on the route nearby, each with its straight distance. Starting nearby SHALL make that place the start of the plan, for good, before navigation begins.

#### Scenario: Asked near the course
- **WHEN** the route runs 06 → 02 → 28 with 02 a stop, and the rider starts navigation 250 m from the course of 06 → 02 and 4.4 km from 06
- **THEN** the app asks "Ride to 06 · 4.4 km" or "Start on the route nearby · 250 m"

#### Scenario: Riding to the start
- **WHEN** the rider is asked and chooses to ride to 06
- **THEN** the plan stays as it was, and navigation leads to 06

#### Scenario: Starting nearby
- **WHEN** the rider is asked and chooses to start on the route nearby
- **THEN** the plan starts at a route point at the nearest place on the course of 06 → 02, as "Start here" on a course places it, 06 is no longer in the plan, 02 and 28 stay, and navigation leads to that point

#### Scenario: Nearby at a knot
- **WHEN** the nearest place on the course is within 30 m of 05, a knot of the route between 06 and 02, and the rider starts nearby
- **THEN** the plan starts at 05, without a route point

#### Scenario: On the course past the start
- **WHEN** the rider starts navigation on the course 2 km after 06
- **THEN** the app asks, and starting nearby starts the plan where the rider is and navigation begins with the next knot after that place

#### Scenario: At the start
- **WHEN** the rider starts navigation within 30 m of 06
- **THEN** the app does not ask and navigation begins at 06

#### Scenario: Close to the start along the course
- **WHEN** the nearest place on the course is 150 m along it from 06
- **THEN** the app does not ask and navigation leads to 06

#### Scenario: Course not much closer
- **WHEN** the rider starts navigation 1.5 km from the course of 06 → 02 and 2 km from 06
- **THEN** the app does not ask and navigation leads to 06

#### Scenario: Only before the first stop
- **WHEN** the rider starts navigation 100 m from the course of 02 → 28 and 5 km from the course of 06 → 02 and from 06
- **THEN** the app does not ask and navigation leads to 06, so no stop is dropped

#### Scenario: Without courses
- **WHEN** the network's courses are not installed, and the rider starts navigation 300 m from 05, a knot of the route before 02, and 4 km from 06
- **THEN** the app asks, and starting nearby makes 05 the start of the plan

#### Scenario: Position unknown
- **WHEN** the rider starts navigation and no position is known within 10 seconds
- **THEN** the app does not ask and navigation leads to the start

#### Scenario: Question dismissed
- **WHEN** the rider dismisses the question without choosing
- **THEN** navigation does not start, the plan stays as it was, and the location is no longer used

#### Scenario: The plan after the ride
- **WHEN** the rider started nearby, and then stops navigation or arrives
- **THEN** the planner shows the route from the new start, as it was ridden

### Requirement: Courses loaded after the start
When navigation was started before the network's courses had loaded, it SHALL take them as soon as they are loaded, without stopping or restarting, and from then on SHALL navigate along them as if it had started with them. Knots already passed SHALL stay passed and SHALL NOT be announced again. The next knot SHALL stay the same unless the rider is already past it along the course. Taking the courses SHALL NOT be spoken. When no courses are installed, navigation SHALL go on without them.

#### Scenario: Courses arriving on the course
- **WHEN** the rider started navigation before the courses had loaded, rides on the course between 45 and 46, and the courses finish loading
- **THEN** the next knot is still 46, the map draws the course of the leg ahead instead of a straight line, and nothing is spoken for the change

#### Scenario: Knots passed before the courses
- **WHEN** the rider passed 45 and 46 before the courses had loaded, and they then finish loading
- **THEN** 45 and 46 stay passed, the next knot is 47, and neither is announced again

#### Scenario: Off route when the courses arrive
- **WHEN** the courses finish loading while the rider is 300 m from the course of the leg to the next knot
- **THEN** on the next position the rider is off route, with the usual notice and the line or guided way back

#### Scenario: Courses on a slow phone
- **WHEN** the rider presses Navigate right after starting the app, before the courses have loaded, and rides on
- **THEN** a few seconds after the courses have loaded, the map draws the course ahead and leaving the course is told as off route

### Requirement: Zoom while following
While the map follows the rider on the route, its zoom SHALL follow what lies ahead: zoomed in at a tricky spot, zoomed in at a knot, briefly zoomed out after a knot, and otherwise the navigation zoom, where the next few hundred meters of the route and their streets can be read. When more than one applies, the tricky spot SHALL win over the knot, and the knot over the zoom-out after a knot. The rider SHALL stay in the middle of the map, as before.

#### Scenario: Riding between knots
- **WHEN** the rider is on the route, 1.5 km from the next knot, with no tricky spot within 150 m ahead
- **THEN** the map follows them at the navigation zoom

#### Scenario: Tricky spot at a knot
- **WHEN** the rider comes within 200 m of knot 45, and a tricky spot begins 50 m after 45
- **THEN** the map zooms in on the tricky spot, with 45 in view on the way to it

#### Scenario: Off route
- **WHEN** the rider is off route without a tricky spot close ahead on a guided way
- **THEN** the map keeps them and the end of the way to the route in view, as described in "Way to the route on the map", and doesn't zoom in on the next knot

### Requirement: Zoom at a knot
From the reminder before the next knot (see "Reminder before a knot"), the map SHALL zoom in so that the rider, the knot and the first 100 m of the route after it are in view, to show where to expect the signpost and which way the route goes on. It SHALL NOT come closer than about 300 m across the screen's narrower side. The zoom SHALL last until the knot is passed.

#### Scenario: Approaching a knot
- **WHEN** the rider heading to 45, with 12 after it, comes within 200 m of 45
- **THEN** the map zooms in so their position, 45, and the first 100 m of the route towards 12 are in view

#### Scenario: Coming closer
- **WHEN** the rider rides on from 200 m to 50 m before 45
- **THEN** 45 and the first 100 m of the route after it stay in view, and the zoom doesn't change with every position

#### Scenario: Knot at the end of the route
- **WHEN** the rider comes within 200 m of 31, the end of the route
- **THEN** the map zooms in so their position and 31 are in view

### Requirement: Zoom out after passing a knot
After passing a knot, the map SHALL zoom out for about 10 s, just far enough that the rider and the next knot are in view, but not farther out than about 3 km across the screen's narrower side, and then go back to the navigation zoom. When the next knot is within 200 m, the map SHALL zoom in on it at once instead. A tricky spot within 150 m ahead SHALL skip or end the zoom-out.

#### Scenario: Next knot 800 m ahead
- **WHEN** the rider passes 45, and the next knot 12 is 800 m ahead
- **THEN** the map zooms out so their position and 12 are in view, and after about 10 s goes back to the navigation zoom

#### Scenario: Next knot far ahead
- **WHEN** the rider passes 45, and the next knot 12 is 6 km ahead
- **THEN** the map zooms out to about 3 km across, with 12 not yet in view, and after about 10 s goes back to the navigation zoom

#### Scenario: Next knot close
- **WHEN** the rider passes 45, and the next knot 12 is 150 m after it
- **THEN** the map doesn't zoom out and zooms in on 12 right away

#### Scenario: Tricky spot right after the knot
- **WHEN** the rider passes 45, and a tricky spot begins 60 m after it
- **THEN** the map doesn't zoom out and zooms in on the tricky spot

#### Scenario: Last knot passed
- **WHEN** the rider arrives at the end of the route
- **THEN** the map doesn't zoom out, as there is no next knot

### Requirement: Tricky spots on the route
A stretch of the route's course SHALL count as a tricky spot when it has two or more sharp turns, of 60° or more, within 100 m of each other. On a guided way, a doubtful junction (see "Doubtful junctions") SHALL count as a tricky spot too. Without the network's courses, the route has no tricky spots of its own.

#### Scenario: Zigzag
- **WHEN** the route's course turns right by 80°, then 40 m on left by 90°
- **THEN** the two turns are one tricky spot

#### Scenario: One sharp turn
- **WHEN** the route's course turns by 90° once, with no other sharp turn within 100 m
- **THEN** it isn't a tricky spot

#### Scenario: Gentle bends
- **WHEN** the route's course bends by 30° several times within 100 m
- **THEN** it isn't a tricky spot

#### Scenario: Without courses
- **WHEN** the network's courses are not installed
- **THEN** the route has no tricky spots, and the map only zooms at knots

### Requirement: Zoom at a tricky spot
When the rider comes within 150 m of a tricky spot ahead, the map SHALL zoom in so that the rider, the whole tricky spot and 30 m of the way after it are in view, no closer than about 200 m across the screen's narrower side. The zoom SHALL last until the rider is 20 m past the spot.

#### Scenario: Approaching a zigzag
- **WHEN** the rider comes within 150 m of a zigzag on the route
- **THEN** the map zooms in so their position, both turns and the way after them are in view

#### Scenario: Past the spot
- **WHEN** the rider is 20 m past the last turn of the zigzag, with the next knot 1 km ahead
- **THEN** the map goes back to the navigation zoom

#### Scenario: Doubtful junction off route
- **WHEN** the rider is off route on a guided way and comes within 150 m of a doubtful junction on it
- **THEN** the map zooms in on the junction, as on the route

### Requirement: Smooth zoom changes
A change of zoom SHALL be one even move of about 1 s. During one of the zoom rules, the zoom SHALL stay the same while the map follows the rider. The base map's labels SHALL stay steady as described in "Following the rider".

#### Scenario: Zooming in at a knot
- **WHEN** the rider comes within 200 m of the next knot
- **THEN** the map zooms in in one even move, without jumps, while it keeps following them

#### Scenario: Labels while zoomed in
- **WHEN** the map is zoomed in at a knot and follows the rider towards it
- **THEN** the street names and icons stay shown while they are in view, without disappearing and reappearing with every position

### Requirement: Header on the way to the route
After navigation started away from the route, until the rider is first on the route, the header SHALL show the way to the route like "Direction to the route", or like "Guided way in the header and outside the app" on a guided way, but on a neutral tile without the dark background, as this is no mistake, and with the to-the-route symbol's arrow pointing towards the route. The next knot's badge SHALL be shown small only while the rider is within 30 m of a course of the knot network. After the rider has been on the route, leaving it SHALL show the off-route header.

#### Scenario: Starting at home
- **WHEN** the rider starts navigating at home, 800 m from the route to the east, on a street that is no path of the knot network
- **THEN** the header shows the to-the-route symbol on a neutral background, an arrow to the right and "800 m", no knot badge, and is not dark

#### Scenario: Reaching the network on the way
- **WHEN** that rider reaches a course of the knot network on the way to the first knot 24
- **THEN** the header also shows › and 24 as a small badge

#### Scenario: On the route
- **WHEN** the rider comes within 30 m of the course
- **THEN** the header shows the number to follow as usual

#### Scenario: Leaving the route later
- **WHEN** the rider was on the route and leaves it by more than 100 m
- **THEN** the header is the dark off-route header

#### Scenario: On the way with a screen reader
- **WHEN** a screen reader reaches the header on the way to the route, 800 m to the east, with 24 as the first knot
- **THEN** it announces "Next 24, 800 m to the east"
