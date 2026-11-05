# Tasks

## 1. Data

- [ ] 1.1 Pipeline:
  - `NetworkExtraction` keeps the `bridge` tag of route ways and marks the bridge segments of each course and signpost course.
  - The simplification is cut at stretch boundaries.
  - Tests on small fixtures: "Course over a bridge", "Course under a bridge", "Several ways of one bridge", "Bridge tagged no", "Stretch kept through simplification", "Signpost course over a bridge".
- [ ] 1.2 Core: `NetworkGeometry` writes and reads both bridge sections after the signpost courses.
  - Tests: a written file reads back with the same stretches; a file without the sections reads with none.
  - "Released apps": a file with the sections, read by a copy of the 0.9.0 reader's rules, gives the same courses and signpost courses.
- [ ] 1.3 Build the cycling and hiking geometry for the whole area locally, without publishing.
  - Report the size against "Geometry size budget" and the number of stretches per network.
  - Spot check the bridge between 28 and 29, and the Rhine route under the Guido-Westerwelle-Brücke in Bonn.

## 2. Lines for the map

- [ ] 2.1 Core: `courseOf` carries the stretches through reversal, joined courses and signpost courses.
  - `MapFeatures` emits connections, the route and the line ahead as features with `bridge` true or false, and says whether any stretch exists.
  - Tests: "Route over a bridge", "Over and under the same bridge" (out and back), "Line ahead while navigating" (slicing through a stretch), "Geometry without bridge stretches", "Same transparency everywhere" (the route's paint properties are the same on both sides of the split).

## 3. Apps

- [ ] 3.1 Android:
  - Copy the `bridge_` layers of the loaded style at 0.6 opacity, and add the `-bridge` line layers with the filters and the order of the design.
  - Add the copies only when bridge stretches exist; the offline style gets none.
  - Test against the bundled dark style: its bridge layers are found and copied.
  - Check on the emulator, light and dark: "Route under a bridge", "Route over a bridge", "Connections and bridges", "Knots and position stay on top", "Ridden track", "Tunnel", "Dark map", "Offline map", "Geometry without bridge stretches".
- [ ] 3.2 iOS: the same with `MLNLineStyleLayer` copies.
  - The project builds with `xcodebuild`.
  - Check the same scenarios as 3.1 on the simulator.

## 4. Release

- [ ] 4.1 Changelog line, and the manual checks for the maintainer's rides in `manual-checks.md`:
  - over and under the bridge between 28 and 29, on the Pixel and the iPhone;
  - the line ahead while navigating;
  - the dark map;
  - a look at battery use while navigating.
- [ ] 4.2 Reinstall the normal build on the Pixel in place of the prototype, once the maintainer is done with it.
