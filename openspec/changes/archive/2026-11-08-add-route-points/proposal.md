# Proposal

## Why

The maintainer lives between 02 and 06, not at a knot. Today every waypoint has to be a knot, so the first and last stretch of a ride are not part of the plan, and neither is a detour to a place worth visiting. Fietsknoop offers this as Plus nodes: click the map, and the route runs through that point. Knoop by Knoop should let riders start, stop and end anywhere.

Since `remove-guidance-switch`, every cycling route downloads the path data around it, and the app can find ways along suitable paths on the device. So the way to a place away from the network needs no online service, and no position leaves the device.

## What Changes

- **Tap anywhere:** a tap with no knot within reach offers "Start here", "Add stop here" (once a route exists) and "End here". Knots keep priority, and nothing changes until one is chosen.
- **Route points:** the chosen place becomes a waypoint with no number, shown as a flag on the map, in the waypoint list and on the strip. It can be the start, a stop or the end, and moves and is removed like other waypoints.
- **Old start or end:** when a new start or end comes before or after it, it becomes a stop, whether it is a knot or a route point.
- **On a course:** a tap on a course of the network puts the point on it, and the route uses that course.
- **Away from the network:** on a cycling route, the point connects to a nearby place on a course by a way found on the device from the path data. Until the squares around it are installed, and when no way is found, it is a straight dashed line, which is replaced once a way is found. The squares come with the route's usual download, as the line is part of the route's course. On a hiking route, it is always a straight line, as there is only cycling path data.
- **Round trips:** the action to return to the start also works from a route point.
- **Navigation:** a route point works like a knot for passing, off route and arriving, with its own wording, on screen, outside the app and in the voice. Its way is stored with the route, so navigating never computes it.
- **Kept with the route:** route points and their ways survive a restart and appear in the GPX export.

## Out of Scope

- **Turn-by-turn directions on the way to a point.** The rider follows the line on the map, with the distance and direction. Fietsknoop works the same way.
- **Dragging a route point** to another place. To change it, choose another place and remove the old one.
- **Choosing a route point from search or from the rider's position.**
- **Routing between two points directly, off the network.** Each connects to the network on its own, unless they are on the same connection.
- **Path data for hiking.** Hiking route points connect by straight lines.
- **Rerouting while navigating.**
- **Changes to the graph, geometry and path files, and to the index.**

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `route-planning`: a waypoint can be a route point, in any position.
- `map-planning`: new requirements for choosing a route point by tapping the map, how it connects to the network, and how it is shown, kept and exported.
- `knot-navigation`: new requirements for navigating from, through and to a route point, and for riding its way.

## Impact

- **Core:** a new kind of waypoint in `Knot` and `PlanningSession`, an overlay on the network graph and geometry, finding nearby places on courses, ways from `PathRouter`, `RouteSnapshot`, `MapFeatures`, `Gpx`, `Navigation`, `RouteProgress`, and new wording in `Texts` and the speech texts in all four languages.
- **Apps:** the map tap and its choices, finding ways off the main thread, the flag on the map, in the list and on the strip, the way's line, and the lock screen, notification and island on both platforms.
- **Outside the code:** none. No new service, so the privacy policy and the store declarations stay as they are.
