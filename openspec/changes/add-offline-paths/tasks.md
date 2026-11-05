# Tasks

## 1. Path data

- [x] 1.1 Core: `PathFile` format with the directory and per-square reader, and `expect fun inflate`. Tests: reading a written file; "Unsupported path format"; a square's checksum mismatch is rejected.
- [x] 1.2 Pipeline: path extraction and writer with the cost table of the design. `PathExtractionTest` on small fixtures: "Ways across borders", "Roads that can't be ridden", shared nodes connect across squares.
- [x] 1.3 `index.json` gets the optional `paths` entry. `NetworkIndexTest`: "An earlier app reads the new index" against a copy of the current parser's rules, "An index without path data".
- [x] 1.4 Build the real file for the whole area locally or in a workflow run without publishing, and report its size, the largest square and the time and memory used. Stop and ask the maintainer if it exceeds 150 MB or the runner's limits.
- [x] 1.5 `update-network-data.yml`: build, include `*.paths` in the artifact, check the 150 MB budget, publish. Verify with a manual run: "Failed path build" keeps the publication, "Path data size budget" fails on a lowered budget, and the current release apps still read the new index. Run for real on 2026-11-05 at the maintainer's choice: 102.7 MB published, range requests and the directory's checksum verified. The failure paths weren't run: by design, publishing needs a successful build and the budget check comes before any upload. The parser check for released apps is against a copy of their rules.

## 2. Router

- [x] 2.1 Core: `PathRouter` with A* and a turning-back cost. `PathRouterTest` for each scenario of `path-routing`: "Without network access", "Cycle path beside a main road", "Main road with a cycle lane", "A much longer detour", "Poor surface", "Closed to bicycles", "Mandatory separate cycle path", "One-way street", "One-way street open to bicycles", "Ferry", "Turning back to change sides", "A stray piece of path", "Far from any path", "Squares missing in between", "Not enough memory" (on the emulator, forcing the error), "A way ahead about as short as the way back", "The way back much shorter", "Standing still", "Ways all along a long route".
- [x] 2.2 Run the eight public trips of the design on the real file, with no requests to outside services, and report lengths against the design's table, plus whether Siegstraße still turns back.
- [ ] 2.3 Measure on the maintainer's Pixel and iPhone with a temporary build: a 12 km way, with squares loaded from storage: with the screen on at most 500 ms, with the screen off at most 1.5 s for the first search and 500 ms after that, and at most 50 MB still in use after collecting (changed with the maintainer on 2026-11-05, as the first search runs off the main thread and once per ride, and most of the peak is garbage). On a release build, after compression. **Stop and go back to the maintainer if this fails.**

## 3. Download

- [x] 3.1 Core: which squares a route needs, never depending on the rider, only while the setting is on, and the store's limit. Tests: "Off by default", "Turning it on with a route", "Turning it off", "Route planned at home", "Changing the route", "Starting away from the route", "Far off route", "Starting before the download is done", "Still downloading", "Ready", "Not available", "A hiking route", "Newer path data published", "Storage limit".
- [x] 3.2 Android: range requests, store and verification, on the emulator with the real file. Tests or checks: "Without network access", "Corrupted square", "File replaced during a download".
- [x] 3.3 iOS: the same; the project builds, and a simulator run downloads the squares for a route.
- [x] 3.4 Info sheet on both: the switch and its explanation in all app languages, stored with the other settings. The status in the panel on both, in all app languages. Checks: "Reading the explanation", "Kept across starts", "Status with a screen reader", "Opening the info sheet with path data", and on the emulator that a fresh install and a cycling route cause no request for the path file while the switch is off.

## 4. Guidance in the core

- [ ] 4.1 `Navigation` holds the guided way and its target. `NavigationTest`: "Starting away from the route", "Wrong turn", "Without courses", "Path data arrives on the way", "Beyond the path data", "Reaching the path data", "No way, no path data, far away or hiking", "Guidance turned off", "Back on route with a guided way", "Skipping ahead on the guided way".
- [ ] 4.2 Leaving the way. Tests: "Missing a turn", "A single inaccurate fix", "No new way", "Riding away from the way", at most once in 10 seconds.
- [ ] 4.3 Turn notices and texts in all app languages. Tests: "A turn ahead", "Turn close to the start of the way", "Two turns together", "Straight on", "A bend without a choice", "A bend with a choice", "Crossing with an offset", "Way behind the rider", "Still riding away", "Screen off" (no change in `precision`), "No guidance notices on a guided way".
- [ ] 4.4 What the header and outside-the-app show. Tests: "In the header", "On the lock screen", "Distance to the course", "Distances ahead on a guided way", "Header with a screen reader", and the existing scenarios of "Off route", "Direction to the route" and "Direction outside the app" still pass without a guided way.

## 5. Android

- [ ] 5.1 `NetworkMap`, the planner's header and `NavigationService`, off the main thread. A mocked ride on the emulator with screenshots: "Line along the guided way", "Back to the dashed line", the header, the notification, a spoken notice in the log, and that no location precision changes.

## 6. iOS

- [ ] 6.1 `NetworkMapView`, the planner's header, `Navigator` and the Live Activity. The project builds, and a simulator ride shows the same; the lock screen goes to the maintainer's steps.

## 7. Wrap-up

- [ ] 7.1 `PRIVACY.md`: only with the switch on, GitHub also receives the rough area of planned routes, and never where the rider is; the summary keeps its promise for the app as installed. List what the Play data safety form and the App Store labels need, for the maintainer to change. Verify by reading the result against "Turning on guidance away from the route".
- [ ] 7.2 `manual-checks.md` with the scenarios and the maintainer's steps, at least: a real ride from home to the first knot, with the phone locked; a wrong turn on purpose; flight mode with the squares in; battery on the ride, against "Battery use while navigating".
- [ ] 7.3 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- `improve-off-route` must be archived before this change.
- Once this change is settled, revisit the online routing part of `add-route-points`.
- `add-junction-pictures` builds on this change.
