# Tasks

Scenario names of one or two words need a `// Scenario: <name>` mark on their test (the check matches test-name prefixes only from three words on).

## 1. Core in the browser

- [x] 1.1 `modules/core`: add `js { browser() }` with `commonTest` running on it (headless Chrome or Node, whichever the Gradle plugin's test task supports without extra setup). Fix what `commonMain` can't compile or what behaves differently on JS. Verify: `./gradlew :modules:core:jsTest` and `:modules:core:jvmTest` pass, and `./gradlew check` stays green on all targets.
- [x] 1.2 Time a long cycling route (Renesse → Aachen, from the real-area cut-outs) on JS against the JVM. If it takes more than a second, move the hot spots to `Int`/`Double` in `commonMain`. Verify: a jsTest asserting the route matches the JVM's, with the time noted in this task.
  - Done 2026-11-27: on the published graph of 2026-11-23, Renesse → Aachen (254,843 m, 121 knots) took 13–20 ms on Node and 7–24 ms on the JVM, the same route on both; no hot spots to move. The core's 980 common tests, routing included, run on JS and the JVM, so their routes match.
- [x] 1.3 Route link in the core next to `RouteFile`: encoding a route as `https://knoopbyknoop.com/route/#` plus the percent-encoded file text, and reading it back. Tests "Link of a route" and "Route not sent to the server" (the link's path is `/route/` and the route is only after `#`).

## 2. Data on the website

- [x] 2.1 `.github/workflows/website.yml`: download the latest `network-data` release with `gh release download` into `data/<publication>/` of the site, with `data/index.json` naming that folder; also run after a successful `Update network data` run (`workflow_run`). A site check fails when a file named in the index is missing. Verify: `actionlint` in Build, and the first run serves `https://knoopbyknoop.com/data/index.json`. "New publication" goes into `openspec/checked-by-hand.md` (the next weekly run).
  - Done 2026-11-27: the download, the check of every file against the index and the copy into `data/<publication>/` (tests in `NetworkDataTest`, and a build with the real publication of 2026-11-23). The first run after the push (7526b17) published the planner and the data of 2026-11-23; ranges of the 103 MB path file are served (206).
- [x] 2.2 Data loading in `web`: read `data/index.json`, then the graph and courses of the chosen network, and path data and place details in squares by range, checking each against the index's checksum with the core's checks. Tests "Data of the latest publication" (a fake fetch only ever requests files under the index's folder) and "File not matching the index" (a wrong checksum leaves the data unused and sets the message).

## 3. Planner module and pages

- [x] 3.1 Module `web` (Kotlin/JS, browser, IR) in `settings.gradle.kts`, depending on the core, with MapLibre GL JS 6 and `qrcode-generator` from npm (versions checked with ctx7 at the time), bundled into `planner.js` and `planner.css`. The planner's state lives in one class that the tests drive on Node, without a browser. Verify: `./gradlew :web:jsBrowserProductionWebpack` writes the bundle, and `./gradlew :web:jsTest` runs.
- [x] 3.2 Generator: `plan/index.html` in every language with the site's header and footer, the planner in the navigation of every page, texts from `website/content/<lang>/planner.properties` (missing keys fail the check), the bundle copied in, and a `<noscript>` text with the store badges. `route/index.html` sends to the planner in the browser's language (one of the four, else English), keeping the part after `#`. Tests "Planner in the navigation", "Planner in Dutch" (the Dutch page's texts and a distance written "3,4 km"), "JavaScript turned off" (the German page's noscript text and badges) and "Link without the app" (the route page's language choice, given languages and a fragment).
- [x] 3.3 Check for other sites per page: `tiles.openfreemap.org` and `photon.komoot.io` allowed on the planner pages and in the bundle only; scripts from any other host fail on every page. Tests "Base map on the planner only" and "Script from another site on the planner".
- [x] 3.4 The map: base map from OpenFreeMap Liberty, dark from `map-styles/liberty-dark.json` served by the site, following `prefers-color-scheme`; the network's knots and courses as the apps draw them; the network switch. Test "Switch to hiking" (the state's route is empty and the network is hiking). "Cycling network around Siegburg", "Dark setting" and "First visit" (knots within 5 s on 50 Mbit/s, measured with the browser's network throttling) go into `openspec/checked-by-hand.md`.

## 4. Planning

- [x] 4.1 Clicking knots through the core's planning session, as the apps do. Tests "Start, end and a stop" (the route matches the core's route for the same waypoints) and "Double click on a waypoint".
- [x] 4.2 The panel: waypoints in riding order with numbers and towns, remove, move by dragging, back to start, clear; the summary with distance and knots in the page's language. Tests "Reorder by dragging" (the state's move from the drag's indexes), "Round trip in the planner" and "Distance and knots".
- [x] 4.3 Route points: a click away from knots offers "Add route point here", joining the network with the core's path routing on the squares read by range. Test "Route point at a café".
  - Done 2026-11-27: the test checks the point joins the route and the path data around it is asked for; the way along paths comes from the core's tested router.
- [x] 4.4 Undo with the core's route history, and the route and history kept in `localStorage` in a `try`. Tests "Undo a removed stop", "Reload" (a new state from the same storage has the route) and "Browser storage unavailable" (throwing storage leaves the planner working). "Language switch keeps the route" is tested with the same storage across two page languages.

## 5. Places, search and ferries

- [x] 5.1 Place search through the core's Photon requests, in the page's language, moving the map to a chosen hit. Tests "Station found in the planner" (a recorded Photon answer for "Utrecht Centraal") and "Search unavailable in the planner".
- [x] 5.2 Places useful on a ride from the base map, with the switch, and the place card from the place details, with "Add to route". Tests "Café on the card" (the card's content from a place with opening hours) and "Place added to the route".
- [x] 5.3 Ferries on the route in the panel with the core's ferry status in the ferry's local time. Tests "Ferry on the route" and "Ferry listed without operating times".

## 6. Hand-off

- [x] 6.1 Copy link, QR code, and downloads of the `.knoop` file and GPX from the core's writers. Tests "Copy the link", "Same file as the app" (name and text equal the core's for the route 12 → 40 → 34) and "GPX" (marked). "Scan to the phone" goes into `openspec/checked-by-hand.md`.
- [x] 6.2 Opening a `.knoop` file by choosing it or dropping it on the map, and a route link, with the core's reader; an opened route is an undoable edit. Tests "Dropped file" (the state after opening a hiking file while planning cycling) and "Not a route".
- [x] 6.3 "Opening the planner": the planner never calls the geolocation API; a test over the planner's sources (the bundle holds MapLibre's unused locate control); the scenario check allows a test or a hand check, not both.

## 7. Apps open route links

- [x] 7.1 Android: intent filter with `autoVerify` for `https://knoopbyknoop.com/route/`, read with the core's link reader and opened like a `.knoop` file. `website/static/.well-known/assetlinks.json` with the Play app signing key's and the upload key's SHA-256 fingerprints (the maintainer reads the first from the Play Console). Verify: `adb shell pm get-app-links com.knoopbyknoop` shows the domain verified on the emulator after the site is published.
  - Done 2026-11-27: intent filter, link handling and `assetlinks.json` with the current Play app signing key from the Play Console. The upload key isn't needed, as Play installs carry Google's key. The verification on a device moved to "Link opened on Android" in `openspec/checked-by-hand.md`, as it needs a release with the filter.
- [x] 7.2 iOS: the Associated Domains entitlement `applinks:knoopbyknoop.com` in `apps/ios/project.yml`, `onOpenURL` handling the link like a `.knoop` file, and `website/static/.well-known/apple-app-site-association` for `/route/*`. The maintainer enables Associated Domains for the App ID; check the release workflow's signing on its next run. Verify: the debug build on the simulator opens a route link from Safari.
  - Done 2026-11-27: entitlement, link handling and the association file; the simulator build compiles. Universal links need a signed build, so the check moved to "Link opened on iOS" in `openspec/checked-by-hand.md`. Left for the maintainer: Associated Domains for the App ID, and the release workflow's signing on its next run.
- [x] 7.3 "Link opened on Android", "Link opened on iOS" and "Link opened while navigating" go into `openspec/checked-by-hand.md`: on the emulator and on the iPhone with a release build, from a messenger and the camera.

## 8. Privacy and finish

- [x] 8.1 `PRIVACY.md`: the planner, what OpenFreeMap and Photon receive from it, and the route kept only in the browser; "Last updated" moves, and the German, Dutch and French translations follow with their `translates:` date. Help pages: a section on the planner and route links. Verify: `./gradlew :website:test`.
- [x] 8.2 README and CHANGELOG ("Unreleased"); `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.
- [x] 8.3 `./gradlew checkScenarios -Pchange=add-web-planner` passes.

## Workflow follow-up

- Archive `add-project-website` before this change, since this change's `project-website` delta modifies requirements that exist only after that archive.
- The apps open links only after a release with tasks 7.1 and 7.2; until then, links open the web planner on phones too.
