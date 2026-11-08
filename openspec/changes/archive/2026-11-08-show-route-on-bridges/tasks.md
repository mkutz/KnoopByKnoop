# Tasks

## 1. Data

- [x] 1.1 Pipeline:
  - `NetworkExtraction` keeps the `bridge` tag of route ways and marks the bridge segments of each course and signpost course.
  - The simplification is cut at stretch boundaries.
  - Tests on small fixtures: "Course over a bridge", "Course under a bridge", "Several ways of one bridge", "Bridge tagged no", "Stretch kept through simplification", "Signpost course over a bridge".
- [x] 1.2 Core: `NetworkGeometry` writes and reads both bridge sections after the signpost courses.
  - Tests: a written file reads back with the same stretches; a file without the sections reads with none.
  - "Released apps": a file with the sections, read by a copy of the 0.9.0 reader's rules, gives the same courses and signpost courses.
- [x] 1.3 Build the cycling and hiking geometry for the whole area locally, without publishing.
  - Report the size against "Geometry size budget" and the number of stretches per network.
  - Spot check the bridge between 28 and 29, and the Rhine route under the Guido-Westerwelle-Brücke in Bonn.
  - Result 2026-11-06 (extracts of that day): cycling.geometry 2.65 MB (published 2.50 MB), 16,154 stretches on 10,489 of 31,928 connections; hiking.geometry 2.60 MB (published 2.47 MB), 17,841 stretches on 12,322 of 74,509 connections. 28–29 near Siegburg (489 m) has one stretch of 241 m. In Bonn, 79–80 has a 577 m stretch over the Kennedybrücke, and the bank routes 80–81 and 05–80 next to it have none there. The other Rhine bridges were not found by the rough coordinates used.

## 2. Lines for the map

- [x] 2.1 Core: `NetworkGeometry` finds bridge segments by their points, so reversal, joined courses, signpost courses and cuts need no extra handling (see the design).
  - `MapFeatures` emits connections, the route and the line ahead as features with `bridge` true or false, and says whether any stretch exists.
  - Tests: "Route over a bridge", "Over and under the same bridge" (out and back), "Line ahead while navigating" (slicing through a stretch), "Geometry without bridge stretches", "Same transparency everywhere" (the route's paint properties are the same on both sides of the split).

## 3. Apps

- [x] 3.1 Android:
  - Copy the `bridge_` layers of the loaded style at 0.6 opacity, and add the `-bridge` line layers and the bridge edges with the filters and the order of the design, below `ridden-track`.
  - Add the copies only when bridge stretches exist; the offline style gets none.
  - Test against the bundled dark style: its bridge layers are found and copied.
  - Check on the emulator, light and dark: "Route under a bridge", "Route over a bridge", "Connections and bridges", "Knots and position stay on top", "Ridden track", "Tunnel", "Dark map", "Offline map", "Geometry without bridge stretches".
  - Checked 2026-11-06 on the emulator with locally built data (light, dark, offline, navigating 28→29 over the Sieg bridge, under the A 560) and by the maintainer on the Pixel at the Sieg bridges near Siegburg, which led to the bridge edges. "Tunnel" was not looked at separately: nothing in the change touches tunnels. "Geometry without bridge stretches" is covered by the core tests.
- [x] 3.2 iOS: the same with `MLNLineStyleLayer` copies.
  - The project builds with `xcodebuild`.
  - Check the same scenarios as 3.1 on the simulator.
  - Checked 2026-11-06: in the simulator with locally built data, the copies and the bridge edges show at the Sieg bridge near Siegburg, light and dark; the maintainer checked it on the iPhone. The offline map and the ridden track on iOS were not looked at separately.

## 4. Release

- [x] 4.1 Changelog line, and the manual checks for the maintainer's rides in `manual-checks.md`:
  - over and under the bridge between 28 and 29, on the Pixel and the iPhone;
  - the line ahead while navigating;
  - the dark map;
  - a look at battery use while navigating.
- [x] 4.2 Reinstall the normal build on the Pixel in place of the prototype, once the maintainer is done with it.
  - The prototype was replaced on 2026-11-05; since 2026-11-06 the Pixel runs this change's build with locally built data.
