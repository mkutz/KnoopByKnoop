# Proposal

## Why

The maintainer lives between 02 and 06, not at a knot. Today every waypoint has to be a knot, so the first and last stretch of a ride are not part of the plan, and neither is a detour to a place worth visiting. Fietsknoop offers this as Plus nodes: click the map, and the route runs through that point. Knoop by Knoop should let riders start, stop and end anywhere.

## What Changes

- **Tap anywhere:** a tap with no knot within reach offers "Start here", "Add stop here" (once a route exists) and "End here". Knots keep priority, and nothing changes until one is chosen.
- **Route points:** the chosen place becomes a waypoint with no number, shown as a flag on the map, in the waypoint list and on the strip. It can be the start, a stop or the end, and moves and is removed like other waypoints.
- **Old start or end:** when a new start or end comes before or after it, it becomes a stop, whether it is a knot or a route point.
- **On a course:** a tap on a course of the network puts the point on it, and the route uses that course. No service is involved.
- **Away from the network:** the point connects to the nearest place on a course. The way there comes from an online routing service using OpenStreetMap data. Without an answer, it is a straight dashed line, which is replaced when the service can be reached.
- **Round trips:** the action to return to the start also works from a route point.
- **Navigation:** a route point works like a knot for passing, off route and arriving, with its own wording, on screen, outside the app and in the voice. Navigating never needs the service, as the way is stored with the route.
- **Kept with the route:** route points and their ways survive a restart and appear in the GPX export.
- **BREAKING for the privacy promise:** the position of a point away from the network is sent to the routing service, so "your routes stay on your device" no longer holds as written. The privacy policy, the store declarations and the info sheet change with this.

## Out of Scope

- **Turn-by-turn directions on the way to a point.** The rider follows the line on the map, with the distance and direction. Fietsknoop works the same way.
- **Dragging a route point** to another place. To change it, choose another place and remove the old one.
- **Choosing a route point from search or from the rider's position.**
- **Routing between two points directly, off the network.** Each connects to the network on its own, unless they are on the same connection.
- **Downloading path data** to route away from the network offline.
- **Running a routing server of our own,** as the app has no backend.
- **Rerouting while navigating.**
- **Changes to the graph and geometry files.** Only `index.json` gets an optional field.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `route-planning`: a waypoint can be a route point, in any position.
- `map-planning`: new requirements for choosing a route point by tapping the map, how it connects to the network, and how it is shown, kept and exported.
- `knot-navigation`: a new requirement for navigating from, through and to a route point.
- `network-sync`: the info sheet names the routing service, and the published index says which service to use.

## Impact

- **Core:** a new kind of waypoint in `Knot` and `PlanningSession`, an overlay on the network graph and geometry, finding the nearest course, requests to and answers from the routing service, `RouteSnapshot`, `NetworkIndex`, `MapFeatures`, `Gpx`, `Navigation`, `RouteProgress`, and new wording in `Texts` and the speech texts in all four languages.
- **Apps:** the map tap and its choices, sending the requests, the flag on the map, in the list and on the strip, the way's line, and the lock screen, notification and island on both platforms.
- **Pipeline:** `index.json` gets the service's address.
- **Outside the code:** `PRIVACY.md`, the Play data safety form and the App Store privacy labels. A third-party service with its own terms and availability.
