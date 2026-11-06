# Tasks

## 2. Core: routing

- [x] 2.1 `Knot.routePoint` and `isRoutePoint`, the overlay graph, and `ShortestPathRouter` not going through a route point. Tests in `ShortestPathRouterTest` and `RouteTest`: "Starting from a route point", "Ending at a route point", "Two route points on one connection", "Passing a route point that is not on this leg", "Length with a way to the network", "Moving a route point", "A stop on the route's own course", "A stop as a detour", "Several route points". The existing scenarios of "Route from waypoints" and "Waypoints are knots, not numbers" stay green.
- [x] 2.2 `PlanningSession.startAt`, `addStopAt`, `endAt`, `routeGraph`. Tests in `PlanningTest`: "First waypoint", "End before the start", "Before an existing start", "After an existing end", "An earlier route point", "A round trip stops being one", "Removing the route point", "Out and back from home".
- [x] 2.3 `NetworkGeometry.nearestOnCourses` and the geometry overlay, with `signpostCourses` cut at the place. Tests in `NetworkGeometryTest`: "Knots go first" (the core part), "On a course", "On a ferry connection", "Through the fitting signpost", and that the way is part of the line.
- [x] 2.4 `RoutePointWays`: ways from the path data, the 3 nearest connections as candidates, the 20 km limit, and straight lines in the session until a way is found. Tests on a small path file, as in `PathRouterTest`, and in `PlanningTest`: "A way is found", "Joining a nearby course", "While the path data loads", "No path data", "No way found", "A hiking route", and that a straight line is not replaced while navigating.
- [x] 2.5 `RouteSnapshot` and `Gpx`. Tests: "After a restart", "Connection gone", "Older route", "Start point in the file", "A stop in the file".

## 3. Core: navigation

- [x] 3.1 List every use of `.ref` in the core and both apps, and decide each. Verified by adding the list to `design.md`, each use covered by a later task, with no route point `ref` left in a sentence.
- [x] 3.2 `Texts` and speech for the start, stop and end in English, German, Dutch and French. Test: "All languages", in `LanguageTest` for each new sentence.
- [x] 3.3 `Navigation`, `RouteProgress` and `NavigationDisplay`. Tests in `NavigationTest`: "Starting at the point", "Starting away from the point", "Joining further along", "A stop next", "The last knot before the end", "Reminder and arrival", "A point the way does not reach", "Off route with the end next", "On the strip", "A round trip from a route point", and a route point within 30 m of a knot.
- [x] 3.4 Riding the way. Tests in `NavigationTest`: "Following a way along paths", "Leaving a way along paths", "A straight line", "Turning round at a detour", "Without internet access".

## 4. Android

- [x] 4.1 The tap and the offer in `NetworkMap` and `PlannerScreen`, in all four languages. Checked on the emulator with screenshots: "A route point goes first", "Offer and dismiss", "Offer without a route", "Zoomed out".
- [x] 4.2 Finding the ways off the main thread when a point is chosen and when the route's squares arrive. The lines' styles and the planner's note about straight lines. Checked on the emulator, online and in airplane mode: "The line on the map", "Nothing leaves the device" (no request with the place in the network log), and the planning scenarios again in the real app.
- [x] 4.3 The flag on the map, in the list and knot sequence, with screen reader words; navigation passes `routeGraph`. Checked on the emulator: "On the map", "In the list".
- [x] 4.4 Strip, notification and lock screen. A mocked ride from a point away from the network, through a stop on a detour, to a point on 02-06, with no blank number on screen.

## 5. iOS

- [x] 5.1 The tap and the offer in `NetworkMapView` and `PlannerView`, in all four languages. Checked in the simulator with the same scenarios as 4.1.
- [x] 5.2 Finding the ways off the main thread, the lines' styles and the planner's note. Checked in the simulator, with and without path data, as in 4.2.
- [x] 5.3 The flag on the map, in the list and knot sequence, with VoiceOver words; navigation passes `routeGraph`. Checked in the simulator: "On the map", "In the list".
- [x] 5.4 Strip, Live Activity and island. The same mocked ride in the simulator, with the notices logged the same as on Android.

## 6. Wrap-up

- [x] 6.1 `manual-checks.md` with the scenarios and the maintainer's steps; a CHANGELOG entry under Unreleased; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.

## 7. Found on the maintainer's walk

- [ ] 7.1 No offer far from the network (hiking route from home ran 70 km out to the Eifel and back, 141 km): `tapAt` answers `MapTap.TooFar` beyond 20 km (cycling) or 5 km (hiking) from every course, and both apps say so in all four languages. Tests for "Too far from the network"; checked on the emulator and in the simulator.

## Workflow follow-up

- The maintainer plans from home on the Pixel and iPhone, adds a stop at a place off the network, rides it, and listens to the notices, before `/opsx:archive add-route-points`.
