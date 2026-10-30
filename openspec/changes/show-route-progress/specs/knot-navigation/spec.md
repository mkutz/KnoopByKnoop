# Spec Delta

## ADDED Requirements

### Requirement: Progress strip
While navigating, the panel SHALL show the route as a strip drawn to scale by distance along the route, in place of the knot badge row: the start, the stops, the end and the ferries marked and labeled, the other knots as small marks, the rider's place on the route as a dot, and the part of the route already ridden filled. While the panel is shown, the strip SHALL follow each position.

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

### Requirement: Distances and times ahead
While navigating, the panel SHALL show below the progress strip the distance and time to the next knot, and the distance, time and arrival to the next stop, if one remains before the end, and to the end. Distances SHALL run along the route from the rider's place on it; times SHALL use the pace of "Ferry arrival estimate". While the panel is shown, the values SHALL follow each position.

#### Scenario: Ahead at the start
- **WHEN** the rider starts navigating a cycling route at 18:20 at 24, with the next knot 12 at 800 m, the stop 30 at 5.2 km and the end 31 at 12.4 km
- **THEN** the panel shows "Next knot 12 · 800 m · 3 min", "Next stop 30 · 5.2 km · 20 min · 18:40" and "End 31 · 12.4 km · 47 min · 19:05"

#### Scenario: Along the way
- **WHEN** the rider is halfway between 24 and 12 on that route
- **THEN** the panel shows "Next knot 12 · 400 m", and the distances to 30 and 31 are 400 m shorter than at the start

#### Scenario: The rider's own pace
- **WHEN** the rider has covered 10 km in 50 minutes and 10 km are left to the end at 18:00
- **THEN** the panel shows the end with "50 min" and "18:50"

#### Scenario: Long time
- **WHEN** 40 km are left at 16 km/h at 12:00
- **THEN** the panel shows the end with "2 h 30 min" and "14:30"

#### Scenario: No stop left
- **WHEN** the rider has passed the last stop before the end
- **THEN** the panel shows the next knot and the end, without a next stop

#### Scenario: Next knot is the next stop
- **WHEN** the next knot is the stop 30
- **THEN** the panel shows the next knot and the next stop with the same distance

#### Scenario: Round trip
- **WHEN** the rider navigates a round trip from 24 back to 24
- **THEN** the end shows 24 and the distance back to it along the route

#### Scenario: Off route ahead
- **WHEN** the rider is off route, 300 m from the next knot 12
- **THEN** the panel shows "Next knot 12 · 300 m" as the straight distance, without a time, and the stop and the end counted from 12 plus those 300 m

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
