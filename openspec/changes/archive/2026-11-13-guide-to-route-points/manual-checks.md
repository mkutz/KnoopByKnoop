# Manual checks

## Scenarios

Android checked 2026-11-08 on the emulator (`Medium_Phone_API_37.0`), debug build, app on screen, the route 06 → 02 → the restaurant Alt Mülldorf as the end, planned in the app with its way along paths. From 02 the way follows the course towards 01 along the Sieg and leaves it under the A 560. Positions came from the emulator's test GPS provider along the last 400 m to 02, then along the planned way to the flag. The shell's mock location permission and the test provider were removed again afterwards.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Towards the next knot | ✅ emulator | unit test | "Next 02, then towards 01", then at 02 "Towards 01, then the end", spoken "Next 2, then towards 1" |
| Leaving the network | ✅ emulator | unit test | "In 90 m, turn right into An der Ziegelei", "Turn right now into An der Ziegelei": 90 m, as positions came every 20 m |
| Turn by turn to the flag | ✅ emulator | unit test | The header shows the light tile with the turn and its street ([screenshot](screenshots/android-after-turn-off.png)). The planned way runs 150 m straight from the network to the first path, which the mocked rider followed; the guided way goes along An der Ziegelei, so the rider left it, a new way was found (239 m, 1 turn) and "In 80 m, turn left" told |
| Missing the turn-off | unit test | unit test | Leaving the guided way on the emulator, as above, found a new way to the flag |
| A straight line | unit test | unit test | |
| Without path data | unit test | unit test | |
| From the start flag onto the network | unit test | unit test | |

Found on the emulator and fixed before these checks: the way after the turn-off, looked for from the turn-off itself, started on the path above it and came back 1000 m long with "Turn back now". It is now looked for from 40 m into the way, and a way much longer than the planned one is not used.

The route point's header badge is the plain flag where the strip shows the place's icon; that is the existing badge, not this change.

### Found and fixed

- **The header grew to five lines on a guided way** (TestFlight feedback on build 25, 9 November 06:33 and 08:06): the row of the tile, turn arrow, turn distance, street, distance left and next knot didn't fit one line, so iOS stacked every item and Android wrapped them, and the header jumped between one and five lines as the way was found. On a guided way the header is now always one row, with the street small under the turn's distance and cut short; checked on the emulator ([screenshot](screenshots/android-guided-header.png)) and in the simulator ([screenshot](screenshots/ios-guided-header.png)), starting away from the route.

- **A second line beside the route** (TestFlight feedback on build 25, 9 November 06:35): on route the guided way was drawn along the planned way too, a white line beside the orange one on the dark map. Of the two options the maintainer chose from [mockups](screenshots/mockup-guided-line.png), on route the guided way is now drawn only where it is more than 20 m from the planned way, joined to it at both ends; off route the whole way shows as before. Unit tests in `RoutePointGuidanceTest`; on the emulator the found way along An der Ziegelei shows where it differs ([screenshot](screenshots/android-guided-line-differs.png)).

### Found on the starts of 2026-11-10

The maintainer started a route of a start and an end flag in Siegburg twice, 15–40 m from the start flag: on the iPhone at about 17:59 and on the Pixel at 19:01. Both times the guided way led east along Lindenstraße and the railway (1.3 km) instead of along the planned way south (1.14 km); on the iPhone, after walking west past the flag, a new way began with a left turn right there, and no notice was spoken. The Pixel's log: navigation started 19:01:11.6, first guided way 19:01:12.5, 1,291 m with 5 turns. A local replay on a cut-out of the published data (not committed, as it is near the maintainer's home) gives that way without any riding direction from a first position about 30 m north of the flag, and the planned way from the flag itself. Added to this change as tasks 1.5 to 1.7 and 2.2: a way found from an imprecise position is searched again with the next precise one, off route as well (decided with the maintainer).

### Checked after the fix (2026-11-10)

- **Tests:** "Starting beside the start flag", "Precise from the start" (`RoutePointGuidanceTest`), "Turn at the start of the way" (`GuidedWayTest`, with the router reporting the turn in `PathRouterTest`); `./gradlew check`, the core on the iOS simulator and the iOS app build pass; `checkScenarios -Pchange=guide-to-route-points` passes.
- **Recorded ride on the real data** (`RealRideTest`, `titz-start-flag`): a start flag in Titz 579 m from 06; the first position 35 m off at 30 m accuracy gets a way of 629 m, the first precise position at the flag searches again and gets 579 m, the planned way, followed to 06 and 08.
- **Two recorded rides changed with the turn at the start of a way, read before re-recording:** `siegburg-going-on`, riding 200 m beside the route, first got a new "Turn right now" with every way search; now it says it once, and "Turn back now" later still comes. `siegburg-way-behind` gets "Turn sharp left now" at the start of one way, then "In 50 m, turn left".
- **Emulator** (debug build, the test GPS provider, removed afterwards): a start flag in Titz to 06, 577 m planned. Started with the position 35 m off at 30 m accuracy: guided way 628 m (log 20:24:03); the first precise position at the flag: 578 m (20:24:09), and the header "240 m Ophertener Straße · 580 m › 06", the planned way's first turn ([screenshot](screenshots/android-start-flag-searched-again.png)). The first way's "Switch to the street now" had been spoken before the precise position came.

### Steps for the maintainer

The maintainer's walk home → gym in Siegburg on 2026-11-09 (iPhone, dark mode, a route of a start and an end flag, path data installed): the voice guidance "works great now". Near the end flag by the station, a short white guided line was drawn beside the route although the maintainer followed it, against "The guided way on the map" (drawn only where it differs). Not investigated yet; it goes to the change on the guided way's look. The turn back north-east off route is `rejoin-route-ahead`.

1. Plan home → 28 → 02 → home with the start and end flags at home, as on the ride of 2026-11-08.
2. Start at home: each turn to the network is told, and at the network "Turn left now onto the route towards 02" (or right, as it is there).
3. After 28: "Next 02, then towards 06"; after 02: "Towards 06, then the end".
4. Before Cecilienstraße: "In 100 m, turn right into Cecilienstraße", then "Turn right now…", and the turns to the flag, with the light tile in the header.
5. Note any turn that was missing, wrong or late, and whether a way different from the planned one was guided.
