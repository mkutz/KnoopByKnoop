# Design

## Context

- **A waypoint is a `Knot`,** and so is everything that follows: `Route`, `Leg`, `Path`, `NetworkGeometry.courses(knots)`, `RouteProgress`, `Navigation`, `MapFeatures`, `Gpx`, `RouteSnapshot`. All look knots up by `id` and print `ref`.
- **`Knot.ref` goes straight into sentences:** "Next 45", "Nächster Knotenpunkt 45", in about a dozen `Texts` functions and the same in speech, in four languages.
- **`session.graph` is the installed network.** The apps hand it to the map (`PlannerScreen`, `AppViewModel`) and to navigation (`AppViewModel`, `NavigationService`, and the same on iOS).
- **A tap** goes through `holder.onTap` / `onTap` with a tolerance, only at the zoom where knots show. The map draws `geometry.courses`, the main course of each connection, and not `signpostCourses`.
- **A restored route** is planned from `graph` alone, before the geometry is loaded (`NetworkGeometry.straightLines` stands in).
- **Path data** is downloaded for every complete cycling route since `remove-guidance-switch`: the squares within 5 km of the route's course, chosen from the course alone. `PathRouter.way(from, to)` finds a way along suitable paths on the device, and returns `null` when the installed squares don't connect the two places. The guided way uses it off route, up to 20 km from the target.
- **`PRIVACY.md`** promises "Your location and your routes stay on your device"; GitHub sees the rough area of planned cycling routes.

## Goals / Non-Goals

**Goals:**
- One code path for routing, drawing and navigating knots and route points.
- No cost for routes without route points, and no slower map or startup.
- Planning and navigating that never wait for path data. It only makes a line better.
- No position of a route point leaves the device.

**Non-Goals:**
- Any change to the published graph and geometry files.

## Decisions

**A route point is a `Knot` with no number.** `Knot` gets an optional `routePoint` with the place on a connection (the two knot IDs and the fraction along the course) and an optional way. `ref` is empty and `isRoutePoint` tells them apart. Its `id` is negative, as OpenStreetMap IDs are positive, and derived from the tapped position, so a new place is a new ID and `Route`'s reuse of known legs cannot return a stale leg.
- Alternative: a sealed `Waypoint` of knot or point. Cleaner in types, but it changes every signature named above and `Path.knots`, for no change in behaviour.

**An overlay on the graph and geometry.** A route point adds connections to both knots of its connection, with the lengths split by the fraction, plus the way's length if it has one. It adds one to each other point on the same connection, and the original connection stays. `PlanningSession` keeps `graph` as the installed network for the map, and gets `routeGraph` for routing and for navigation. It shares the base graph's maps, so an edit does not rebuild them. `NetworkGeometry` gets the same overlay, with courses cut at the place and the way put in front.
- Splitting the connection instead would lead other routes through the point. So `ShortestPathRouter` also does not go on through a route point unless it starts there.
- A point on a course and one away from it differ only by the way, so the rest of the code does not care.
- Where the connection has `signpostCourses`, each one passing within 10 m of the place is cut too, so `courses(knots)` picks the fitting signpost. This is the maintainer's own case: 02-06 branches to both signposts of 02.

**Finding the place is in the core.** `NetworkGeometry.nearestOnCourses(position, tolerance)` returns the connection, fraction and snapped position, leaving out connections with a ferry. A tap calls it with the touch tolerance. A point away from the network asks for the nearest place on each of the 3 nearest connections, see "Ways from the path data". It runs on a choice only, with a bounding box check per course, and it only looks at the main courses, which are the ones drawn.

**The choice in the app.** A tap first checks knots and route points as today, then offers the choices. Android shows a `DropdownMenu` at the tap. iOS draws a small menu of its own on the map, below the tapped place and under the planner's sheet, as a `confirmationDialog` in iOS 26 hangs off the sheet instead of the place (maintainer, 2026-11-06). A tap on the map beside it or moving the map closes it.
- Alternative: a tap on a path and a long press elsewhere. Fewer stray offers, but harder to discover. The maintainer chose tapping anywhere.
- The label is "Add stop here" and not "Stop here", which reads like the end of the route.

**New session actions.** `startAt`, `addStopAt`, `endAt`, following `map-planning`. "End before the start" keeps the point as a pending end until a start is tapped. Moving and removing need no new rules. A role is the position in the list.

**Ways from the path data.** `RoutePointWays` in the core connects a point away from the network, on a cycling route:
- **Candidates:** the nearest place on each of the 3 nearest connections without a ferry. For each, `PathRouter.way(point, place)`; the cheapest way wins. Trying several places mostly avoids joining across a river by a long way round.
- **Limits:** a point more than 20 km from every course stays a straight line, as for the guided way. No way found means a straight line to the nearest place.
- **When:** when a point is chosen, and again when the route's squares finish downloading while a line is still straight. Never while navigating: the stored way or line is ridden, and the guided way covers riding off route. It runs off the main thread in both apps, as the router reads squares.
- **The squares:** the straight line is part of the route's course, so the usual download fetches the squares around the point too. Nothing else is requested.
- **Direction:** the way is found from the point to the course and used in both directions. A one-way street on a short way is accepted, see Risks.
- **Hiking:** there is no path data for walking, so hiking route points always connect by straight lines.
- **Alternative:** an online routing service. It needs a third party's agreement and sends the point, which can be someone's home, off the device. It was the plan until path data existed.

**Why the line comes first.** Choosing a point gives a straight line at once and the way replaces it, as a new `PlanningSession`. This is the same state as missing path data, so that state is always tested.

**Riding the way.** A way from the path data is just part of the leg's course, so `distanceToCourse` and `RouteProgress` work on it without a change. A straight line is marked, and `Navigation` skips the off route check along it and shows "way to" guidance, reusing "Way to the route". A route point also counts as reached within the radius of the way's end. `DirectionWatch` is reset at a passed stop, so turning back is not a wrong way.

**Wording.** `Navigation` asks for sentences by what comes next, not by a string: the `Texts` and speech functions that take `next` or `after` get variants for the start, stop and end. The apps get the role in `NavigationDisplay` and draw an icon where they draw a number, using each platform's own flag icon. On the island and other small places the icon replaces the number.
- Alternative: a `ref` like "End" pushed through the existing sentences. It reads wrongly: "Nächster Knotenpunkt Ziel", and it fails at "End, then …".

**The stored route.** `RouteSnapshot.Waypoint` gets optional fields: the tapped position, the place on the connection, the way's points and length, and whether it is straight. Nothing else is needed to restore it, with no geometry, no path data and no internet access. If the connection has gone, the point goes through "find the place" again once the geometry is loaded.

**GPX.** `Gpx` names a route point "Start", "Stop" or "End", in English as with the file's other words, and uses the same in the file name. The way is part of the track.

**Privacy.** A route point's position never leaves the device. GitHub sees the squares around the route, as for any cycling route, chosen from the course alone. `PRIVACY.md` and the store declarations stay as they are.

## Uses of `ref` (task 3.1)

Every direct `.ref` in the core and both apps, as of 2026-11-06, and what it does for a route point, whose `ref` is empty:

**Core, unchanged as only knots get there**
- `GraphFile` writing knots; `MapFeatures.knots` (the installed graph, never route points); `RouteSnapshot` storing `ref` (empty for a point, which is restored from its `point` field); `RouteSnapshot.restore` naming removed knots (points are never removed); `MapFeatures.describe` naming ferries (a route point is never at a ferry).

**Core, by role through `Spot`**
- `Navigation`: `display` (adds `nextRole` and `followRole`), the next, reminder, off route, guidance, back on route and wrong way notices, and the line off route (`wayTo`). Text and speech get the point sentences of `Texts`, e.g. "On to the stop, then 12" and "Weiter zum Ziel".
- `NavigationDisplay.text` and `offRouteText`: the same. `At 45 follow …` is not used next to a route point, which has no signs; it reads as `Next …, then …`.
- `RouteProgress`: the line outside the app (`toKnot`), the strip's description (`ridden`), each coming knot's description (the role alone), the ferry legs; `ProgressMark` gets `routePoint`.
- `MapFeatures.describe`: `StartOnly` gets `routePoint` and `endFirst`; unreachable legs name a point by its role.
- `MapFeatures.routeKnotsLayer` and `waypoints`: a `point` property, so the apps draw a flag.
- `Gpx`: "Start", "Stop" or "End" as the waypoint's name, in the title and the file name.

**Android (tasks 4.1 to 4.4)**
- `NetworkMap` route badges (2 uses): a flag image for `point` features.
- `PlannerScreen` badges in the list and knot sequence (2 uses) and the screen reader words for removing and moving (2 uses): the flag, and the role word in place of the number.
- `NavigationService` notification title (`end = knots.last().ref`): the role word for an end point.
- `AppViewModel` collecting `refs` for the knot search: the installed graph only, unchanged.

**iOS (tasks 5.1 to 5.4)**
- `NetworkMapView` route badges (2 uses), `PlannerView` badges in the list and knot sequence (2 uses): the flag.
- `AppModel` collecting `refs`: the installed graph only, unchanged.

## Risks / Trade-offs

- [The best place to join is not among the 3 nearest connections] → The line and length are visible, and the rider can choose another place.
- [The stored way differs from newer path data] → The way is stored with the route and kept until the point is chosen again. Paths rarely change for a short way.
- [Squares missing where the way leaves the 5 km around the course] → No way is found and the line stays straight. Points far from the network are rare in planning.
- [A way is used in both directions, but one-way streets differ] → Accepted for short ways. Check on real rides.
- [Code reading `ref` assumes a number] → Check every one of the 47 direct `.ref` uses in the core and both apps (task 3.1), and add a test for each kind of notice.
- [Empty `ref` reaching the knot search, badges or spoken numbers] → Route points are only in the overlay, never in `graph.knots`, so search and the knot layer never see them.
- [A point the rider cannot get within 30 m of] → It also counts at the end of the way. With a straight line, it can still be missed. Stops are then skipped by passing what comes next, and the rider can end navigation by hand at the end.
- [Stray offers in the middle of planning] → The offer changes nothing and can be dismissed. Check on the Pixel and iPhone that it does not get in the way.
- [A point within 30 m of a knot] → Both count as passed, as at double knots (`lastWithin`). Tested in `NavigationTest`.
- [Size of the change] → It touches most of the core's route handling and both apps' planner and navigation displays. It waits until the changes now waiting on ride checks are archived, and the core can ship first with straight lines only if the ways take longer to settle.

## Migration Plan

No migration. Snapshots without the new fields work as before. After a downgrade, the older app ignores the unknown fields and fails to find a negative ID, so it reports the waypoint as removed with an empty number.
