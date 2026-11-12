## ADDED Requirements

### Requirement: The plan after navigation
Changes made to the route for one navigation, by starting on the route nearby or by riding around a closed ferry, SHALL count only until that navigation ends. While navigating, the planner SHALL show the route as navigated. When navigation ends, by stopping or arriving, the planner SHALL show the plan exactly as it was before Navigate was pressed. The stored plan SHALL never contain these changes, so the plan as made also comes back after the app was closed or ended while navigating.

#### Scenario: Plan restored after stopping
- **WHEN** the rider started nearby on 06 → 02 → 28 and rode around a closed ferry, and then stops navigation
- **THEN** the planner shows 06 → 02 → 28 as planned, over the ferry, without the temporary start or knots

#### Scenario: Plan restored after arriving
- **WHEN** the rider started nearby and arrives at the end
- **THEN** the planner shows the plan from its own start

#### Scenario: App ended while navigating
- **WHEN** the rider started nearby, and the app is closed or ended while navigating and opened again
- **THEN** the planner shows the plan from its own start

#### Scenario: Navigating the plan again
- **WHEN** the rider stopped after starting nearby and riding around a ferry, and presses Navigate again
- **THEN** the app asks again where to start and about the ferry, as for any plan

#### Scenario: Routes saved with avoided ferries
- **WHEN** the app reads a route saved by an earlier version that avoided a ferry for good
- **THEN** the route has its waypoints and takes the ferry again

## MODIFIED Requirements

### Requirement: Start on the route nearby
When navigation is started more than 30 m from the start and a place on the course before the first stop, more than 200 m along it from the start, is less than half as far as the start, the app SHALL ask once whether to ride to the start or to start on the route nearby, each with its straight distance. Starting nearby SHALL make that place the start of the route for this navigation only, as if the plan started there, see "The plan after navigation".

#### Scenario: Asked near the course
- **WHEN** the route runs 06 → 02 → 28 with 02 a stop, and the rider starts navigation 250 m from the course of 06 → 02 and 4.4 km from 06
- **THEN** the app asks "Ride to 06 · 4.4 km" or "Start on the route nearby · 250 m"

#### Scenario: Riding to the start
- **WHEN** the rider is asked and chooses to ride to 06
- **THEN** the plan stays as it was, and navigation leads to 06

#### Scenario: Starting nearby
- **WHEN** the rider is asked and chooses to start on the route nearby
- **THEN** the navigated route starts at a temporary start at the nearest place on the course of 06 → 02, as "Start here" on a course places it, without 06, with 02 and 28. Navigation leads to that point, and the plan still starts at 06

#### Scenario: Nearby at a knot
- **WHEN** the nearest place on the course is within 30 m of 05, a knot of the route between 06 and 02, and the rider starts nearby
- **THEN** the navigated route starts at 05, without a route point

#### Scenario: On the course past the start
- **WHEN** the rider starts navigation on the course 2 km after 06
- **THEN** the app asks. Starting nearby starts the navigated route where the rider is, and navigation begins with the next knot after that place

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
- **THEN** the app asks, and starting nearby makes 05 the start of the navigated route

#### Scenario: Position unknown
- **WHEN** the rider starts navigation and no position is known within 10 seconds
- **THEN** the app does not ask and navigation leads to the start

#### Scenario: Question dismissed
- **WHEN** the rider dismisses the question without choosing
- **THEN** navigation does not start, the plan stays as it was, and the location is no longer used

#### Scenario: The plan after the ride
- **WHEN** the rider started nearby, and then stops navigation or arrives
- **THEN** the planner shows the plan from 06 again, as it was before Navigate

### Requirement: Riding around a closed ferry
When navigation is started and a ferry on the route is closed at the estimated arrival or within 15 minutes after, as for "Ferry warning", the app SHALL ask before navigating whether to ride around it, with the warning and the detour's extra length and time. With several closed ferries, it SHALL ask about one ferry at a time, in riding order, and say which of how many it is; each answer counts for that ferry only. Riding around SHALL add temporary knots on the way around to the route of this navigation only, so that it leaves out that ferry and any other closed by then on the way around, see "The plan after navigation". A ferry with several crossings SHALL count as one ferry, and riding around it SHALL leave out all its crossings. Without a way around, the app SHALL say so.

#### Scenario: Ferry closed on the way
- **WHEN** the rider starts navigating at 18:20 a route over the ferry Lülsdorf–Wesseling, which closes at 19:00, and gets there around 19:40 at the usual pace
- **THEN** the app asks with the warning "The ferry Lülsdorf–Wesseling closes at 19:00, before you get there around 19:40", "Ride around it · 6.3 km longer, about 24 min" and "Keep the ferry"

#### Scenario: Riding around it
- **WHEN** the rider chooses to ride around the ferry
- **THEN** the navigated route has the plan's waypoints and temporary knots on the way around, it no longer takes the ferry, and navigation starts on it

#### Scenario: Several closed ferries
- **WHEN** the route takes the ferries Drielse Veer and Horsterveer, both closed by the time the rider gets there
- **THEN** the app first asks about Drielse Veer as "Closed ferry 1 of 2", with the way around it and its extra length and time, and after the answer it asks about Horsterveer as "Closed ferry 2 of 2"

#### Scenario: Keeping one ferry and riding around another
- **WHEN** the rider rides around the first of two closed ferries and keeps the second
- **THEN** navigation starts on the route around the first ferry and over the second, and the spoken warning comes for the second

#### Scenario: A ferry with two crossings
- **WHEN** the ferry Horsterveer is two connections of the network and the route takes one of them
- **THEN** the app names Horsterveer once, and the way around it takes neither crossing

#### Scenario: Question dismissed for one of several ferries
- **WHEN** the rider rode around the first closed ferry and dismisses the question about the second
- **THEN** navigation does not start and the plan stays as it was

#### Scenario: Temporary knots are not stops
- **WHEN** the rider rides around the ferry over the bridge knot 5
- **THEN** 5 is a knot of the navigated route like any other: not shown or told as a stop

#### Scenario: Keeping the ferry
- **WHEN** the rider chooses to keep the ferry
- **THEN** navigation starts on the planned route, and the spoken warning comes on the way as before

#### Scenario: Ferry open when the rider gets there
- **WHEN** every ferry on the route is open at the rider's estimated arrival and for 15 minutes after
- **THEN** the app does not ask

#### Scenario: Another ferry on the detour
- **WHEN** the detour around one closed ferry takes another ferry that is also closed by then
- **THEN** that one is left out too, and the question about the first ferry names both

#### Scenario: No way around
- **WHEN** the closed ferry is the only link between two waypoints
- **THEN** the app says there is no way around it, offers only to navigate with the ferry, and the spoken warning comes on the way

#### Scenario: Editing the plan afterwards
- **WHEN** the rider rode around a ferry, stopped, and adds a knot to the plan
- **THEN** the new route takes the ferry again where it is the shortest way, also after the app was restarted

#### Scenario: Started nearby and riding around
- **WHEN** the rider starts nearby and the route from there takes a ferry closed by then
- **THEN** the app asks about the ferry for the route from the nearby start, and riding around keeps that start

#### Scenario: Ferry without operating times
- **WHEN** the ferry on the route has no operating times in OpenStreetMap
- **THEN** the app does not ask about it

#### Scenario: Question in all languages
- **WHEN** the app's language is German, Dutch or French
- **THEN** the question and its choices are in that language

### Requirement: Detour on the map
While the app asks about a closed ferry, the map SHALL show that ferry and its detour together, zoomed to both, with the detour drawn as a second line distinct from the planned route. After the answer, the map SHALL show the route the rider chose until navigation ends, and the plan after that.

#### Scenario: Map while asking
- **WHEN** the app asks about the ferry Lülsdorf–Wesseling with a detour over the next bridge
- **THEN** the map shows the ferry's crossing and the detour's line to the bridge and back, both in view

#### Scenario: Map for each of several ferries
- **WHEN** the app asks about the second of two closed ferries
- **THEN** the map shows the second ferry and only its own detour, both in view

#### Scenario: Map after the answer
- **WHEN** the rider chooses to ride around the ferry
- **THEN** the map shows the new route as the route while navigating, and the second line is gone

#### Scenario: Map after the ride
- **WHEN** the rider rode around the ferry and navigation ends
- **THEN** the map shows the planned route over the ferry again
