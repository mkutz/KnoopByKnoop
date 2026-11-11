## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Start navigation
A complete route SHALL be navigable. Navigation SHALL be started by the user while the app is open, and SHALL begin at the first waypoint of the route: within 30 m of any of the nodes it is tagged on, it SHALL begin after it; elsewhere it SHALL lead there, see "Start on the route nearby" for when the app asks first. Standing within 30 m of several consecutive knots at the start of the route, it SHALL begin after the last of them. On the way to the start, the notices SHALL say "To the start" instead of "Off route", the header SHALL show the way as in "Header on the way to the route", and reaching the start SHALL be told as passing a knot. Within 200 m of the start, the first leg's course SHALL count as the way there. It SHALL need location access only while the app is in use, not always.

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

### Requirement: Skipping ahead
When the rider passes a later knot of the route than the next knot, navigation SHALL continue from that knot, as if the knots before it were passed. Before the start of the route is reached, passing a later knot SHALL NOT skip ahead.

#### Scenario: Shortcut
- **WHEN** the rider heading to 45 on the route 24, 45, 12, 31 passes 12 without passing 45
- **THEN** 31 becomes the next knot and the app says "Next 31"

#### Scenario: Passing a later knot on the way to the start
- **WHEN** the rider heading for the first knot 24 of the route 24, 45, 12, 31 passes 12
- **THEN** 24 stays the next knot and the app says nothing about 12

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

## REMOVED Requirements

### Requirement: Joining the course on the way to the start
**Reason**: It guessed from the riding whether the rider wants the whole route, and the app's own guided way to the first knot could follow a course against the route and trigger it: "Back on route, next 02", "Wrong direction" and repeated "Turn back now" on the way to 06 (mocked ride `dreesch-to-06`, 2026-11-11).

**Migration**: The rider chooses when navigation starts, see "Start on the route nearby"; on the way to the start, nothing joins the route, see "Start navigation".
