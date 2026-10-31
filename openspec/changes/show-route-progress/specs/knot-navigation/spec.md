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
While navigating, the progress strip SHALL show below each labeled stop, ferry and end still ahead the distance along the route to it and the arrival, at the pace of "Ferry arrival estimate", following each position while shown. Where they would overlap those of a more important mark (end, then stops, then ferries), they SHALL be left out; the start and passed marks SHALL show none.

#### Scenario: Ahead at the start
- **WHEN** the rider starts navigating a cycling route at 18:20 at 24, with the stop 30 at 5.2 km and the end 31 at 12.4 km
- **THEN** the strip shows "5.2 km" and "18:40" below 30, and "12.4 km" and "19:05" below 31

#### Scenario: Along the way
- **WHEN** the rider has ridden 400 m of that route
- **THEN** the strip shows "4.8 km" below 30 and "12.0 km" below 31

#### Scenario: The rider's own pace
- **WHEN** the rider has covered 10 km in 50 minutes and 10 km are left to the end at 18:00
- **THEN** the strip shows "10.0 km" and "18:50" below the end

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

### Requirement: Rounded times
Durations under an hour SHALL be shown rounded to the minute, at least "1 min"; longer durations, as "2 h 30 min", and the arrivals SHALL be rounded to five minutes, so they do not suggest more precision than the estimate has.

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
