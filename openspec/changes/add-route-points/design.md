# Design

## Context

- **A waypoint is a `Knot`,** and so is everything that follows: `Route`, `Leg`, `Path`, `NetworkGeometry.courses(knots)`, `RouteProgress`, `Navigation`, `MapFeatures`, `Gpx`, `RouteSnapshot`. All look knots up by `id` and print `ref`.
- **`Knot.ref` goes straight into sentences:** "Next 45", "Nächster Knotenpunkt 45", in about a dozen `Texts` functions and the same in speech, in four languages.
- **`session.graph` is the installed network.** The apps hand it to the map (`PlannerScreen`, `AppViewModel`) and to navigation (`AppViewModel`, `NavigationService`, and the same on iOS).
- **A tap** goes through `holder.onTap` / `onTap` with a tolerance, only at the zoom where knots show. The map draws `geometry.courses`, the main course of each connection, and not `signpostCourses`.
- **A restored route** is planned from `graph` alone, before the geometry is loaded (`NetworkGeometry.straightLines` stands in).
- **Place search** is the model for an online service: `PlaceSearch` in the core builds requests and reads answers, and the apps send them with a `User-Agent`.
- **`NetworkIndex`** is read at most once a day and ignores unknown keys, so released apps skip a new optional field.
- **`PRIVACY.md`** promises "Your location and your routes stay on your device" and lists three services.

## Goals / Non-Goals

**Goals:**
- One code path for routing, drawing and navigating knots and route points.
- No cost for routes without route points, and no slower map or startup.
- Planning and navigating that never depend on the service. It only makes a line better.

**Non-Goals:**
- Any change to the published graph and geometry files.

## Decisions

**A route point is a `Knot` with no number.** `Knot` gets an optional `routePoint` with the place on a connection (the two knot IDs and the fraction along the course) and an optional way. `ref` is empty and `isRoutePoint` tells them apart. Its `id` is negative, as OpenStreetMap IDs are positive, and derived from the tapped position, so a new place is a new ID and `Route`'s reuse of known legs cannot return a stale leg.
- Alternative: a sealed `Waypoint` of knot or point. Cleaner in types, but it changes every signature named above and `Path.knots`, for no change in behaviour.

**An overlay on the graph and geometry.** A route point adds connections to both knots of its connection, with the lengths split by the fraction, plus the way's length if it has one. It adds one to each other point on the same connection, and the original connection stays. `PlanningSession` keeps `graph` as the installed network for the map, and gets `routeGraph` for routing and for navigation. It shares the base graph's maps, so an edit does not rebuild them. `NetworkGeometry` gets the same overlay, with courses cut at the place and the way put in front.
- Splitting the connection instead would lead other routes through the point. So `ShortestPathRouter` also does not go on through a route point unless it starts there.
- A point on a course and one away from it differ only by the way, so the rest of the code does not care.
- Where the connection has `signpostCourses`, each one passing within 10 m of the place is cut too, so `courses(knots)` picks the fitting signpost. This is the maintainer's own case: 02-06 branches to both signposts of 02.

**Finding the place is in the core.** `NetworkGeometry.nearestOnCourses(position, tolerance)` returns the connection, fraction and snapped position, leaving out connections with a ferry. A tap calls it with the touch tolerance. A point away from the network calls it with no limit. It runs on a choice only, with a bounding box check per course, and it only looks at the main courses, which are the ones drawn.

**The choice in the app.** A tap first checks knots and route points as today, then offers the choices. Android shows a `DropdownMenu` at the tap, iOS a `confirmationDialog`.
- Alternative: a tap on a path and a long press elsewhere. Fewer stray offers, but harder to discover. The maintainer chose tapping anywhere.
- The label is "Add stop here" and not "Stop here", which reads like the end of the route.

**New session actions.** `startAt`, `addStopAt`, `endAt`, following `map-planning`. "End before the start" keeps the point as a pending end until a start is tapped. Moving and removing need no new rules. A role is the position in the list.

**The routing service.** `RoutingService` in the core builds the request and reads the answer into a list of positions and a length, and the apps send it, as with `PlaceSearch`.
- **Which one:** task 1.1 settles it before any code depends on it. The FOSSGIS Valhalla server is the working candidate: OpenStreetMap data, `bicycle` and `pedestrian`, no key, but volunteers run it mainly for OpenStreetMap work, at most one request per second, with no guarantee.
- **openrouteservice** and **GraphHopper** have free plans, but with keys that would ship in the app, low daily limits shared by all users, and non-commercial terms. **BRouter** suits cycling, but the terms of its public server were not found.
- **The address** is in `index.json`, not in the app, as FOSSGIS recommends. It can be replaced or turned off without a release. A missing field means no service.
- **Requests** are one per new way, from the point to the place on the course, with a timeout of a few seconds and no retry loop, at most one per second per device. A retry only happens when the planner is shown again with a straight line.
- **The answer** is stored with the waypoint, and used in both directions.

**Why the line comes first.** Choosing a point gives a straight line at once and the answer replaces it, as a new `PlanningSession`. This is the same state as no internet access, so that state is always tested.

**Riding the way.** A way from the service is just part of the leg's course, so `distanceToCourse` and `RouteProgress` work on it without a change. A straight line is marked, and `Navigation` skips the off route check along it and shows "way to" guidance, reusing "Way to the route". A route point also counts as reached within the radius of the way's end. `DirectionWatch` is reset at a passed stop, so turning back is not a wrong way.

**Wording.** `Navigation` asks for sentences by what comes next, not by a string: the `Texts` and speech functions that take `next` or `after` get variants for the start, stop and end. The apps get the role in `NavigationDisplay` and draw an icon where they draw a number, using each platform's own flag icon. On the island and other small places the icon replaces the number.
- Alternative: a `ref` like "End" pushed through the existing sentences. It reads wrongly: "Nächster Knotenpunkt Ziel", and it fails at "End, then …".

**The stored route.** `RouteSnapshot.Waypoint` gets optional fields: the tapped position, the place on the connection, the way's points and length, and whether it is straight. Nothing else is needed to restore it, with no geometry and no internet access. If the connection has gone, the point goes through "find the place" again once the geometry is loaded.

**GPX.** `Gpx` names a route point "Start", "Stop" or "End", in English as with the file's other words, and uses the same in the file name. The way is part of the track.

**Privacy.** The tapped position can be someone's home, and it leaves the device. Only when a place away from the network is chosen, with only that position, the place on the course and the network's type, and with no identifiers. `PRIVACY.md` loses the unqualified promise, and gets the service in its table. The Play data safety form and App Store labels are updated before the release, and the info sheet says it in the app.

## Risks / Trade-offs

- [The service disappears, limits us or says no] → The address can be changed or removed in the index, and straight lines keep everything working. Ask the operator first, as a released app is more than its stated main use (task 1.1).
- [The nearest course is across a river, so the way is a long way round] → The line and length are visible, and the rider can choose another place. No search for a better place to join in this change.
- [A way is used in both directions, but one-way streets differ] → Accepted for short ways. Check on real rides.
- [Code reading `ref` assumes a number] → Check every one of the 47 direct `.ref` uses in the core and both apps (task 3.1), and add a test for each kind of notice.
- [Empty `ref` reaching the knot search, badges or spoken numbers] → Route points are only in the overlay, never in `graph.knots`, so search and the knot layer never see them.
- [A point the rider cannot get within 30 m of] → It also counts at the end of the way. With a straight line, it can still be missed. Stops are then skipped by passing what comes next, and the rider can end navigation by hand at the end.
- [Stray offers in the middle of planning] → The offer changes nothing and can be dismissed. Check on the Pixel and iPhone that it does not get in the way.
- [A point within 30 m of a knot] → Both count as passed, as at double knots (`lastWithin`). Tested in `NavigationTest`.
- [Size of the change] → It touches most of the core's route handling and both apps' planner and navigation displays. It waits until the changes now waiting on ride checks are archived, and the core can ship first with straight lines only if the service takes longer to settle.

## Migration Plan

No migration. Snapshots and indexes without the new fields work as before. After a downgrade, the older app ignores the unknown fields and fails to find a negative ID, so it reports the waypoint as removed with an empty number.

The privacy policy goes live with the release that sends the first request.
