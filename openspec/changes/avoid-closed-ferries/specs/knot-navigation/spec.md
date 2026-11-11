## ADDED Requirements

### Requirement: Riding around a closed ferry
When navigation is started and a ferry on the route is closed at the estimated arrival or within 15 minutes after, as for "Ferry warning", the app SHALL ask before navigating whether to ride around it, with the warning and the detour's extra length and time. Riding around SHALL plan the same waypoints without that ferry, or any other closed by then, and keep avoiding them; without a way around, the app SHALL say so.

#### Scenario: Ferry closed on the way
- **WHEN** the rider starts navigating at 18:20 a route over the ferry Lülsdorf–Wesseling, which closes at 19:00, and gets there around 19:40 at the usual pace
- **THEN** the app asks with the warning "The ferry Lülsdorf–Wesseling closes at 19:00, before you get there around 19:40", "Ride around it · 6.3 km longer, about 25 min" and "Keep the ferry"

#### Scenario: Riding around it
- **WHEN** the rider chooses to ride around the ferry
- **THEN** the plan's waypoints stay, the route no longer takes the ferry, and navigation starts on the new route

#### Scenario: Keeping the ferry
- **WHEN** the rider chooses to keep the ferry
- **THEN** navigation starts on the planned route, and the spoken warning comes on the way as before

#### Scenario: Ferry open when the rider gets there
- **WHEN** every ferry on the route is open at the rider's estimated arrival and for 15 minutes after
- **THEN** the app does not ask

#### Scenario: Another ferry on the detour
- **WHEN** the detour around one closed ferry takes another ferry that is also closed by then
- **THEN** that one is left out too, and the question names both

#### Scenario: No way around
- **WHEN** the closed ferry is the only link between two waypoints
- **THEN** the app says there is no way around it, offers only to navigate with the ferry, and the spoken warning comes on the way

#### Scenario: Editing the plan afterwards
- **WHEN** the rider rode around a ferry, stopped, and adds a knot to the plan
- **THEN** the new route still avoids that ferry, also after the app was restarted

#### Scenario: Ferry without operating times
- **WHEN** the ferry on the route has no operating times in OpenStreetMap
- **THEN** the app does not ask about it

#### Scenario: Question in all languages
- **WHEN** the app's language is German, Dutch or French
- **THEN** the question and its choices are in that language

### Requirement: Detour on the map
While the app asks about a closed ferry, the map SHALL show the ferry and the detour together, zoomed to both, with the detour drawn as a second line distinct from the planned route. After the answer, the map SHALL show the route the rider chose.

#### Scenario: Map while asking
- **WHEN** the app asks about the ferry Lülsdorf–Wesseling with a detour over the next bridge
- **THEN** the map shows the ferry's crossing and the detour's line to the bridge and back, both in view

#### Scenario: Map after the answer
- **WHEN** the rider chooses to ride around the ferry
- **THEN** the map shows the new route as the route, and the second line is gone
