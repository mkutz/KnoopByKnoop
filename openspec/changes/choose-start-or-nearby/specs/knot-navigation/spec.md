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
A complete route SHALL be navigable. Navigation SHALL be started by the user while the app is open, and SHALL begin at the first waypoint of the route: within 30 m of any of the nodes it is tagged on, it SHALL begin after it; elsewhere it SHALL lead there, see "Start on the route nearby" for when the app asks first. Standing within 30 m of several consecutive knots at the start of the route, it SHALL begin after the last of them. On the way to the start, the rider SHALL be told "To the start" wherever "Off route" would be said or shown, and reaching the start SHALL be told as passing a knot. Within 200 m of the start, the first leg's course SHALL count as the way there. It SHALL need location access only while the app is in use, not always.

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
- **WHEN** the route runs 24, 25, 45, 25 is 10 m after 24, and the rider starts navigation standing between 24 and 25
- **THEN** navigation begins with 45 as the next knot

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

### Requirement: Number to follow
While navigating and close to the next knot, from when the rider is reminded of it (see "Reminder before a knot"), the app SHALL show the number to follow there as its most prominent information, e.g. "At 45 follow 12", with the knots as badges like on the map. Farther away, it SHALL show the next knot and the distance to it as its most prominent information, e.g. "Next 45", with the number to follow after it smaller, e.g. "then 12". In the same line as the next knot, it SHALL show the distance to the next knot along the route, or straight to it while off route or while the next knot is the first knot of the route. When the next knot is the end of the route, it SHALL say so instead.

#### Scenario: Next knot in the middle of the route
- **WHEN** the next knot is 45, 150 m ahead, and the route continues to 12
- **THEN** the app shows "At 45 follow 12" with 45 and 12 as badges, 12 as the most prominent element, and the distance to 45, "150 m"

#### Scenario: Far from the next knot
- **WHEN** the next knot is 83, 1.2 km ahead, and the route continues to 30
- **THEN** the app shows "Next 83" and "1.2 km" with 83 as a badge, 83 and the distance as the most prominent elements, and "then 30" smaller below them, with 30 as a badge

#### Scenario: Coming close
- **WHEN** the rider heading to 83 comes within 200 m of it and is reminded "83 ahead, then 30"
- **THEN** the app shows "At 83 follow 30" with 30 as the most prominent element

#### Scenario: Knot right after the last one
- **WHEN** the rider passes 45 and the next knot 12 is 150 m after it
- **THEN** the app shows "At 12 follow 31" right away

#### Scenario: Next knot is the end
- **WHEN** the next knot is 31, the end of the route
- **THEN** the app shows that 31 is the end of the route

#### Scenario: Distance off route
- **WHEN** the rider is off route, 1.7 km straight from the next knot 02
- **THEN** the app shows "Off route, next 02" with 02 as a badge and "1.7 km"

#### Scenario: Distance on the way to the start
- **WHEN** the rider is on the way to the start 06 without a guided way, 4.4 km straight from it
- **THEN** the app shows "To the start, 06" with 06 as a badge and "4.4 km", not "Off route"

#### Scenario: Distance to the first knot
- **WHEN** the rider started away from the route 24, 45, 12, 31, came within 30 m of its course 80 m straight from 24, and 24 is still the next knot
- **THEN** the app shows "At 24 follow 45" and "80 m", and outside the app "80 m to 24", not "0 m"

#### Scenario: Far from the next knot with a screen reader
- **WHEN** a screen reader reaches the header while the next knot 83 is 1.2 km ahead and 30 follows
- **THEN** it announces "Next 83, 1.2 km, then 30"

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

## REMOVED Requirements

### Requirement: Joining the course on the way to the start
**Reason**: It guessed from the riding whether the rider wants the whole route, and the app's own guided way to the first knot could follow a course against the route and trigger it: "Back on route, next 02", "Wrong direction" and repeated "Turn back now" on the way to 06 (mocked ride `dreesch-to-06`, 2026-11-11).

**Migration**: The rider chooses when navigation starts, see "Start on the route nearby"; on the way to the start, nothing joins the route, see "Start navigation".
