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
Passing the last knot of the route SHALL be announced with "You've arrived" and SHALL end navigation. The user SHALL be able to stop navigation at any time in the app. Ending navigation SHALL stop using the location.

#### Scenario: Arriving
- **WHEN** the rider passes 31, the end of the route
- **THEN** the app says "You've arrived", navigation ends, and the notification or Live Activity goes away

#### Scenario: Stopping in the app
- **WHEN** the rider taps Stop in the app while navigating
- **THEN** navigation ends and the planned route is still there

#### Scenario: No location use after navigation
- **WHEN** navigation ended by arriving, by Stop, or because the app was closed or ended while navigating, and the app is started again or left in the background
- **THEN** the app does not use the location, and the system shows no location indicator for it

### Requirement: Battery use while navigating
Navigating with the screen off SHALL use at most 5 % of the battery per hour, measured on a real ride on the reference devices named in the design.

#### Scenario: One hour with the screen off
- **WHEN** the rider navigates a route for one hour with the screen off
- **THEN** the battery level drops by at most 5 percentage points
