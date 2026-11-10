# Manual checks

## How it was checked

- **Tests:** `VerifyTest` in the pipeline, on small graphs, geometry and path files written by the tests, with a published copy in a temporary directory read through a `file:` URL.
- **Real data, 2026-11-10:** the published files (index, graphs, geometry and the 98 MB path data) were downloaded and checked with `pipeline verify` against the published release. All checks passed in about 2 s:
  - cycling 20,584 knots and 31,927 connections, hiking 50,460 and 74,605, path data 3,711 squares and 10,435,008 segments, all above their floors
  - no drop against the published data (the same files)
  - known routes, length against the straight line: Breda → Antwerp 65.2 / 48.8 km, Hasselt → Aachen 76.6 / 55.3 km, Nijmegen → Kleve 25.1 / 19.9 km, Veluwe 23.4 / 16.8 km, Utrecht → Amstelveen 28.5 / 21.5 km, Kempen 24.8 / 18.9 km
- **Choosing the hiking pairs:** the first guesses failed on the real data, Siegburg (no hiking knot within 1 km; the data has no hiking knots in North Rhine-Westphalia at all) and Mol → Geel (51.7 km for 9.3 km, across separate networks). They were replaced by pairs picked from the data.
- **Workflow:** `actionlint` passes. The first run with the new step is still to come (task 4.3).

## Scenarios

| Scenario | How it was checked |
|---|---|
| File the apps would reject | `VerifyTest`: a geometry of another graph, a changed byte in a path square, a graph changed after the index |
| Too few knots | `VerifyTest`: a network below its floor, a missing network, path data below its floor, missing path data |
| Sudden drop | `VerifyTest`: 8 % fewer knots, 9 % fewer connections |
| Small change | `VerifyTest`: 2 % fewer and 3 % more knots pass |
| Deliberate drop | `VerifyTest`: an allowed 8 % drop passes, an allowed drop below the floor fails |
| Known route lost | `VerifyTest`: no knot near a position, no route, a route more than twice the straight line; a fitting route passes. Real data: six routes pass |
| Nothing published yet | `VerifyTest`: nothing published, and a published graph of an unsupported format version, skip the drop with a warning |
