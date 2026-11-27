# Charter

Explore the Knoop by Knoop Android and iOS apps (current main, unreleased changes since 1.0.0 included) on the Android emulator and the iOS simulator. Report bugs and observations to the maintainer.

Session focus (s1): route links (`https://knoopbyknoop.com/route/#…`), the only unreleased app behaviour, and the hand-off from the web planner to the apps: links as the planner makes them and as messengers and cameras change them, the other network, while navigating, a route that differs after a data update.

Run start: 2026-11-27T19:24Z. Timebox: 1 hour for the run. Interface: Android emulator Medium_Phone_API_37.0 (debug build from main) via adb, iOS simulator iPhone 18 Pro (iOS 27.2, debug build from main) via simctl. Fidelity gaps: App Links verification and universal links need release signing, so links are handed to the apps directly (`am start` with the package, `simctl openurl`); the decision of the OS to open the app instead of the browser is not covered. Authority: local devices only, no source changes.

Source evidence before probing: both apps pass an https link to `RouteLink.textOf`, which accepts `http(s)://(www.)knoopbyknoop.com/route(/)#<text>` only. Android's intent filter takes every path starting with `/route`; the iOS association file takes `/route/*`. Hypotheses: H1 a link with a query before `#` (messengers add `?fbclid=…`, `?utm_…`) is no route link for `textOf`; H2 a link that isn't a route link gives a confusing message; H3 a long link from the planner is cut or mangled on the way.

# Session log

Links used: L1 = `99 45548428 Voorst` → `75 45417704 Zutphen` (cycling, waypoints only); L1-full = the same with the knots passed listed as the planner writes them; L1-query = L1-full with `?fbclid=IwAR123` before `#`; H = hiking `L59 1182586454 Empe` → `L67 1716753820` (waypoints only); H-missing = H with `42 123` in between. Screenshots in the scratchpad (not kept).

### Experiment E1
Android, app stopped, kept network cycling: open L1.

### Observation O1
After a System UI "isn't responding" dialog of the freshly booted emulator (environment, not the app), the app shows 99 → 75, 7.9 km, as the web planner. The end knot 75 is half under the zoom buttons. No "differs" message seen (it may have gone before the screenshot).

### Experiment E2
Android, app running: open L1-query (H1).

### Observation O2
"This file holds no route the app can open." H1 confirmed: a query before `#` makes `RouteLink.textOf` return nothing. The website's route page reads only `location.hash`, so the same link works in a browser. The message also speaks of a file for a link (H2).

### Experiment E3
Android, cycling route shown: open H-missing.

### Observation O3
Switches to hiking, "Not found, left out of the route: 42", L59 → L60 → L67, 986 m. As specified.

### Experiment E4
Android: navigate the hiking route (position fixes near Empe), open L1.

### Observation O4
"Stop navigating to open the route.", navigation unchanged. As specified. Navigate first waited for a precise fix (known).

### Experiment E5
Android, app in the background: open `https://knoopbyknoop.com/route/` (no route).

### Observation O5
"This file holds no route the app can open." Same wording issue as O2.

### Experiment E6
iOS simulator, app restarted before each link (`simctl terminate`, `openurl` launches it): L1, L1-full, L1-query.

### Observation O6
L1: 99 → 75, 7.9 km, alert "The route differs from the file's list of knots." (the hand-made link lists no knots passed: expected, not a bug). L1-full: opens cleanly. L1-query: "This file holds no route the app can open." H1 confirmed on iOS too. In both apps the opened route's end knot 75 sits under the zoom buttons, against "shown whole on the map" (route-sharing "Open a route file").

### Experiment E7
Android: settings sheet open, then open L1-full (hiking shown, so a network switch).

### Observation O7
The app closes. Crash buffer: `OutOfMemoryError` (33 MB allocation, growth limit 192 MB) in `AppViewModel.addCourses` reading the courses layer with `readText()` (AppViewModel.kt:1471): the known emulator OOM ([[hiking-oom]] in the maintainer's notes), now reached by a link switching hiking → cycling.

### Experiment E8
Android, no sheet: H and L1-full alternately, three rounds, 12 s apart.

### Observation O8
A second crash at round 2 (same stack, "Knoop by Knoop keeps stopping"). Two crashes in about five network switches by link. The settings sheet was not needed. Main thread also skipped 41–71 frames while loading.

### Experiment E9
Android, landscape (user_rotation 1, restored afterwards), app stopped, kept network hiking: open L1-full.

### Observation O9
"Loading the hiking knot network…" with an empty side panel for over 20 s, then the kept hiking route L59 → L67. The cycling link's route never appears, no message.

### Experiment E10
Same in portrait, then switch to cycling with the dialog ("The current route will be discarded").

### Observation O10
Portrait: the same, the link is dropped silently. After switching to cycling, 99 → 75 appears with Undo: the link's route, applied as an edit minutes later, although switching discards the route. Bug confirmed on Android: a link for the other network than the kept one, opened on a cold start, waits for that network instead of switching to it. Source: `AppState.network` defaults to CYCLING, `openRouteFile` compares with it before the kept network is restored, so `pendingRouteFile` waits for cycling while hiking loads. The reverse (cycling kept, hiking link, cold start) was not tried on Android.

### Experiment E11
iOS, app restarted, kept network cycling: open H (12 s), again (35 s); then send H with L60 listed to the running app.

### Observation O11
Both cold starts: the app switches to hiking but shows "Plan a ride" with an empty route and the map elsewhere; after 35 s the alert "The route differs…" is up, so the file was read and the opened route was shown (`show(prepared)` precedes that alert), then replaced by an empty route. Sent to the running app, H opens (986 m). Bug confirmed on iOS: a link for the other network on a cold start ends with an empty route. Likely a race between the opened route and the network's own load restoring the kept (empty) hiking route; not confirmed. The reverse on iOS (hiking kept, cycling link, cold start) worked.

# Findings summary

About 25 minutes (19:24Z–19:49Z by the clock), stopped with the charter answered; Android emulator and iOS simulator shut down afterwards, emulator rotation restored.

Bugs:

1. A route link for the other network than the one kept, opened while the app is not running, is lost: on Android it waits silently and lands as an edit when the visitor later switches (O9, O10); on iOS the route is opened and then replaced by an empty one (O11). This is the common case of a shared link: the receiving app is usually not running. Both unreleased.
2. A link with a query before `#` (as Facebook and others add `?fbclid=…`) is refused by both apps with "This file holds no route…" (O2, O6); the website opens it.
3. The Android out-of-memory crash in `addCourses` on the emulator, known, now reached by links that switch network: 2 crashes in about 5 switches (O7, O8).

Observations:

4. The opened route's end knot sits under the zoom buttons on both apps (O1, O6), against "shown whole on the map".
5. Messages for a link say "file" (O2, O5).

Not covered: the OS handing links to the apps (needs release signing: App Links verification, universal links), links from real messengers and cameras, very long links, iOS while navigating, the Dynamic Island change (waits on the maintainer's iPhone).

Correction to the findings summary: the clock read 19:40Z when the summary was written, so the session ran about 16 minutes (19:24Z–19:40Z), not 25.
