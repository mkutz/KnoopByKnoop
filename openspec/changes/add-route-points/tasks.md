# Tasks

## 1. The service

- [ ] 1.1 Settle the routing service with the maintainer: read its full terms, ask its operator about use in a released app, and record the choice and the address in `design.md`. Verified by the answer being recorded. Pause here if no service agrees; the rest works with straight lines.
- [ ] 1.2 `RoutingService` in the core: requests and answers, tested with recorded answers in `RoutingServiceTest`, like `PhotonResponses`. Test: "What is sent". `NetworkIndex` gets the optional address, and the pipeline writes it. Tests in `NetworkIndexTest`: "Another service address", "An index without a service".

## 2. Core: routing

- [ ] 2.1 `Knot.routePoint` and `isRoutePoint`, the overlay graph, and `ShortestPathRouter` not going through a route point. Tests in `ShortestPathRouterTest` and `RouteTest`: "Starting from a route point", "Ending at a route point", "Two route points on one connection", "Passing a route point that is not on this leg", "Length with a way to the network", "Moving a route point", "A stop on the route's own course", "A stop as a detour", "Several route points". The existing scenarios of "Route from waypoints" and "Waypoints are knots, not numbers" stay green.
- [ ] 2.2 `PlanningSession.startAt`, `addStopAt`, `endAt`, `routeGraph`. Tests in `PlanningTest`: "First waypoint", "End before the start", "Before an existing start", "After an existing end", "An earlier route point", "A round trip stops being one", "Removing the route point", "Out and back from home".
- [ ] 2.3 `NetworkGeometry.nearestOnCourses` and the geometry overlay, with `signpostCourses` cut at the place. Tests in `NetworkGeometryTest`: "Knots go first" (the core part), "On a course", "On a ferry connection", "Through the fitting signpost", and that the way is part of the line.
- [ ] 2.4 Straight lines and answers in the session. Tests in `PlanningTest`: "While waiting", "The service answers", "No internet access", "The service fails", "Internet access is back", "Service turned off".
- [ ] 2.5 `RouteSnapshot` and `Gpx`. Tests: "After a restart", "Connection gone", "Older route", "Start point in the file", "A stop in the file".

## 3. Core: navigation

- [ ] 3.1 List every use of `.ref` in the core and both apps, and decide each. Verified by adding the list to `design.md`, each use covered by a later task, with no route point `ref` left in a sentence.
- [ ] 3.2 `Texts` and speech for the start, stop and end in English, German, Dutch and French. Test: "All languages", in `LanguageTest` for each new sentence.
- [ ] 3.3 `Navigation`, `RouteProgress` and `NavigationDisplay`. Tests in `NavigationTest`: "Starting at the point", "Starting away from the point", "Joining further along", "A stop next", "The last knot before the end", "Reminder and arrival", "A point the way does not reach", "Off route with the end next", "On the strip", "A round trip from a route point", and a route point within 30 m of a knot.
- [ ] 3.4 Riding the way. Tests in `NavigationTest`: "Following a way from the service", "Leaving a way from the service", "A straight line", "Turning round at a detour", "Without internet access".

## 4. Android

- [ ] 4.1 The tap and the offer in `NetworkMap` and `PlannerScreen`, in all four languages. Checked on the emulator with screenshots: "A route point goes first", "Offer and dismiss", "Offer without a route", "Zoomed out".
- [ ] 4.2 Sending the requests, like `PlaceSearchClient`. The lines' styles and the planner's note about straight lines. Checked on the emulator, online and in airplane mode: "The line on the map" and the planning scenarios again in the real app.
- [ ] 4.3 The flag on the map, in the list and knot sequence, with screen reader words; navigation passes `routeGraph`. Checked on the emulator: "On the map", "In the list".
- [ ] 4.4 Strip, notification and lock screen. A mocked ride from a point away from the network, through a stop on a detour, to a point on 02-06, with no blank number on screen.

## 5. iOS

- [ ] 5.1 The tap and the offer in `NetworkMapView` and `PlannerView`, in all four languages. Checked in the simulator with the same scenarios as 4.1.
- [ ] 5.2 Sending the requests, the lines' styles and the planner's note. Checked in the simulator, online and without network, as in 4.2.
- [ ] 5.3 The flag on the map, in the list and knot sequence, with VoiceOver words; navigation passes `routeGraph`. Checked in the simulator: "On the map", "In the list".
- [ ] 5.4 Strip, Live Activity and island. The same mocked ride in the simulator, with the notices logged the same as on Android.

## 6. Privacy and wrap-up

- [ ] 6.1 `PRIVACY.md` and the info sheet on both platforms, in all four languages: the service, what it receives, and its attribution. Scenario: "In the info sheet". Verified by reading the sheet in each language, and the policy against what `RoutingService` sends.
- [ ] 6.2 `manual-checks.md` with the scenarios and the maintainer's steps; a CHANGELOG entry under Unreleased; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.

## Workflow follow-up

- Before the release: the maintainer updates the Play data safety form and the App Store privacy labels.
- The maintainer plans from home on the Pixel and iPhone, adds a stop at a place off the network, rides it, and listens to the notices, before `/opsx:archive add-route-points`.
