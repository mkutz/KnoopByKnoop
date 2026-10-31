# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

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
