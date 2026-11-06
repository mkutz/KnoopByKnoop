# Tasks

## 1. Data

- [x] 1.1 Pipeline:
  - `OsmRelation` and `PbfExtract` keep member roles.
  - `NetworkExtraction` builds directed neighbors per route, the course there and the course back with their bridge stretches, with the fallbacks of the design.
  - `Main` simplifies the course back with its stretches and keeps it only where it differs.
  - Tests on small fixtures: "A cycle path on each side", "One-way stretch", "No marks", "Marks that leave no way", "Bridges on the course back".
- [x] 1.2 Core: `NetworkGeometry` writes and reads the section after the bridge sections.
  - Tests: a written file reads back with the same courses back; a file without the section reads with none; "Released apps" with a copy of the 0.10.0 reader's rules.
- [x] 1.3 Build the whole area locally, without publishing.
  - Report: courses back per network, file sizes against "Geometry size budget", the length differences (median, largest, how many above 100 m), courses back more than twice as long, and connections with both signpost courses and a course back.
  - Spot check 28 → 02 and back at the Sieg bridge in Siegburg-Zange.
  - Result 2026-11-06: cycling 6,475 courses back on 31,928 connections, geometry 3.27 MB (2.65 MB before); hiking 54, 2.60 MB. Length difference median 16 m, 90 % 74 m, largest 1,418 m; 400 above 100 m (cycling), 4 (hiking). One course back more than twice as long: 70–12 near 50.909, 3.402 in Belgium, 353 m vs 1,523 m. 1,961 cycling connections have both signpost courses and a course back. At Zange, 28–02 and 01–02 have no course back; 02–06 has one, but the route 06 → 02 took an undirected signpost course instead, which led to section 5.

## 2. Core

- [x] 2.1 `NetworkGeometry` gives the course of a direction in `courseOf`, `courses(knots)` and the route point overlay; the bridge segment set includes the courses back.
  - Tests: "Route on its side of the road", "Out and back", "Line ahead and off route" (navigation on the course back), "Geometry without courses back".

## 3. Apps

- [x] 3.1 Android and iOS build without changes, or with the small ones needed. Check on the emulator and in the simulator with locally built data: 28 → 02 → 28 at Zange shows each direction on its own side.
  - Both apps built without changes. 28–02 has no marks, so 02 → 06 → 02 was checked instead (2026-11-06): on the emulator and in the simulator, north on the eastern side of the L 16 bridge, back on the western side to 02's second signpost.

## 5. Signpost courses in each direction (added 2026-11-06)

- [x] 5.1 Pipeline: signpost courses there and back on the directed ways, with the fallbacks; `Main` simplifies the courses back and keeps them where they differ. Test "Courses to each signpost in each direction".
- [x] 5.2 Core: a section for the signpost courses back after the courses back; the way-back options use them; their bridge stretches join the segment set. Tests for the file and for 06 → 02 taking the signpost course back.
- [x] 5.3 Build the whole area again, report the counts and the size, and check 06 → 02 at Zange in the data and on the emulator.
  - Result 2026-11-06: the first try gave only 24 signpost courses back, and 06 → 02 still took the main signpost's course reversed. At Zange the way back reaches 02's second signpost first; the main signpost has no way back that avoids it, the second no way there. A signpost course without a way in one direction now is not offered in that direction (`OneWay`), as the knot's other signposts are; stored in the same section. cycling.geometry 3.29 MB, hiking 2.60 MB. 06 → 02 now follows the course back to the second signpost; on the emulator, 02 → 06 → 02 runs north on the eastern side of the L 16 bridge and back on the western side.

## 4. Release

- [x] 4.1 Changelog line, and manual checks for the maintainer: walk or ride 28 → 02 and back at Zange on both phones.
