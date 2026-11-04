# Spec Delta

## ADDED Requirements

### Requirement: Choose a route point on the map
Where knots are shown, a tap with no knot or route point within the tolerance SHALL offer "Start here" and "End here" for the tapped place, and "Add stop here" once a route exists. The offer SHALL NOT change the route until one is chosen, and SHALL be dismissible.

#### Scenario: Knots go first
- **WHEN** the user taps a place with a knot within the tolerance
- **THEN** the knot is treated as tapped, and nothing is offered

#### Scenario: A route point goes first
- **WHEN** the user taps a place with a route point within the tolerance
- **THEN** nothing is offered and the route does not change

#### Scenario: Offer and dismiss
- **WHEN** the user taps a place and dismisses the offer
- **THEN** the route is unchanged

#### Scenario: Offer without a route
- **WHEN** there is no route yet and the user taps a place
- **THEN** only "Start here" and "End here" are offered

#### Scenario: Zoomed out
- **WHEN** knots are not shown and the user taps the map
- **THEN** nothing is offered

### Requirement: What a chosen route point does
"Start here" SHALL make the place the new start, "End here" the new end, and "Add stop here" a stop in the leg where it adds the least distance, whatever the switch in "What new knots become" says. A waypoint that was the start or end before SHALL become a stop, whether it is a knot or a route point.

#### Scenario: First waypoint
- **WHEN** there are no waypoints and the user chooses "Start here", then taps 28
- **THEN** the route goes from the point to 28

#### Scenario: End before the start
- **WHEN** there are no waypoints and the user chooses "End here", then taps 28
- **THEN** 28 is the start and the point the end

#### Scenario: Before an existing start
- **WHEN** the route is 02 → 28 and the user chooses "Start here"
- **THEN** the waypoints are the point, 02 as a stop, and 28

#### Scenario: After an existing end
- **WHEN** the route is 28 → 02 and the user chooses "End here"
- **THEN** the waypoints are 28, 02 as a stop, and the point

#### Scenario: An earlier route point
- **WHEN** the start is a route point and the user chooses "Start here" somewhere else
- **THEN** the new place is the start and the earlier point a stop, which the user can remove

#### Scenario: A round trip stops being one
- **WHEN** the route is a round trip from 12 and the user chooses "End here"
- **THEN** the waypoints keep 12 as start and as a stop, and the point is the end

### Requirement: A route point on a course
A tap with a course of the network within the tolerance SHALL put the route point at the nearest place on that course, without using the routing service. Connections with a ferry SHALL be left out, as the data does not say where on the course the ferry is.

#### Scenario: On a course
- **WHEN** the user chooses "Start here" on the course of 02-06
- **THEN** the point is on the course, and the line begins there

#### Scenario: Through the fitting signpost
- **WHEN** the route starts at a route point on 02-06 before its branches to the two signposts of 02, and goes on to 28
- **THEN** its line goes straight to the signpost that fits 02 → 28, as in "Routes through a knot with several signposts"

#### Scenario: On a ferry connection
- **WHEN** the user chooses a place on the course of a connection with a ferry
- **THEN** the point is treated as away from the network

### Requirement: A route point away from the network
A route point with no course within the tolerance SHALL connect to the nearest place on a course without a ferry, by a way from an online routing service for the network's type, cycling or walking. Until the service answers, and when it does not, the way SHALL be a straight line. Planning SHALL never wait for the service. A route point's way SHALL be requested again only if it is still a straight line, when the planner is shown with internet access, and never while navigating.

#### Scenario: The service answers
- **WHEN** the user chooses a place away from the network and the service answers
- **THEN** the point connects along the answered way, and the route's length and time include it

#### Scenario: While waiting
- **WHEN** the user chooses a place away from the network and the service has not answered yet
- **THEN** the route is shown at once with a straight line, and is updated when the answer arrives

#### Scenario: No internet access
- **WHEN** the device has no internet access and the user chooses a place away from the network
- **THEN** the point connects by a straight line, the planner says that its way and length are only straight, and planning keeps working

#### Scenario: The service fails
- **WHEN** the service does not answer in time, answers with an error, or finds no way
- **THEN** the point connects by a straight line, as without internet access

#### Scenario: Internet access is back
- **WHEN** a route has a straight line and the planner is shown with internet access
- **THEN** the way is requested again and replaces the line if the service answers

#### Scenario: Service turned off
- **WHEN** the published index names no routing service
- **THEN** points away from the network connect by straight lines and nothing is requested

#### Scenario: What is sent
- **WHEN** the way is requested
- **THEN** the service receives the point's position, the place on the course and the network's type, and not the rider's position or the rest of the route

#### Scenario: The line on the map
- **WHEN** a route has a way to a point away from the network
- **THEN** that way is drawn differently from the route on the network, and a straight line differently from a way from the service

### Requirement: Route points on the map and in the list
A route point SHALL be shown as a flag instead of a number badge, on the map and in the waypoint list and knot sequence, with the word for start, stop or end for screen readers.

#### Scenario: On the map
- **WHEN** a waypoint is a route point
- **THEN** a flag marks its place

#### Scenario: In the list
- **WHEN** a waypoint is a route point
- **THEN** its row shows the flag, the word for its role, a handle and a remove button

#### Scenario: Removing the route point
- **WHEN** the waypoints are a route point, 02 and 28, and the user removes the route point
- **THEN** the route is 02 → 28

### Requirement: Round trips from a route point
The action to return to the start SHALL also be available when the start is a route point, and SHALL make the same place the end.

#### Scenario: Out and back from home
- **WHEN** the waypoints are a route point and 28, and the user returns to the start
- **THEN** the route goes from the point to 28 and back to the same point

### Requirement: Keeping route points
Route points and their ways SHALL be kept with the route across restarts, also before the courses are loaded and without internet access. A route point whose connection no longer exists after a data update SHALL connect anew to the nearest course. A route stored before route points existed SHALL still be restored.

#### Scenario: After a restart
- **WHEN** a waypoint is a route point with a way from the service, and the app is restarted without internet access
- **THEN** the same place and the same way are in the route, and its length is unchanged

#### Scenario: Connection gone
- **WHEN** a data update removed the connection of a route point
- **THEN** the point stays in the route and connects to the nearest course, as a newly chosen one does

#### Scenario: Older route
- **WHEN** a route stored by an earlier version is restored
- **THEN** its knots are restored as before

### Requirement: Route points in the GPX export
The GPX export SHALL contain a route point as a waypoint named for its role, and the track SHALL run along its way.

#### Scenario: Start point in the file
- **WHEN** a route starting at a route point is exported
- **THEN** the first waypoint is named "Start", has the type start, and the track begins at its position

#### Scenario: A stop in the file
- **WHEN** a route with a route point as a stop is exported
- **THEN** that waypoint is named "Stop" and has the type stop
