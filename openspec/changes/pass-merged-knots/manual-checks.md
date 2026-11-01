# Manual checks

## Scenarios

Android checked 2026-11-01 on the emulator (`Medium_Phone_API_37.0`) with the debug build of this change and a cycling graph and geometry built by the new pipeline code from the Geofabrik extract `koeln-regbez` of 2026-11-01 (812 knots, 28 of them tagged on several nodes), installed in place of the bundled ones and restored afterwards. Positions mocked with `adb emu geo fix`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Positions of a merged knot | ✅ | ✅ | The pipeline-built graph has 02 (node 59469043) with its other node 81461844 at 50.789673, 7.198766 |
| Passing a merged knot at its other node | ✅ | ⏳ | Route 02 → 28, started 465 m north of 02: "Off route, next 02, 465 m to the south" ([screenshot](screenshots/android-off-route-before-02.png)); riding south onto node 81461844, 195 m from where 02 is drawn: "Back on route, next 02", then "Next 28", the panel "28 is the end 2.6 km" with the dot past 02 ([screenshot](screenshots/android-passed-02-at-other-node.png)) |
| Passing a merged knot that is not on the route, Start at another node of a merged knot | unit test | unit test | `NavigationTest` |
| Graph files without the positions of merged knots | unit test | unit test | `GraphFileTest` |
| Apps without the positions of merged knots | core of 0.6.0 | core of 0.6.0 | 2026-11-01: the bundled cycling graph written with the section, Siegburg 02 with its node 81461844 (471,693 instead of 471,685 bytes), read by the core at `a493d81` (0.6.0) in a throwaway worktree test: 20,583 knots, 31,925 connections, 213 ferries, as written; 02 at 50.788382, 7.20065 |

### Steps for the maintainer

1. After the next weekly publication (or with a build whose bundled graphs have the section), plan a route starting at Siegburg 02 towards 28.
2. Ride to 02 by the north-west node (Siegburg, south of the Sieg, where the path from 28 arrives), on the Pixel and the iPhone: "Next 28" right there, the distance to 28 counts down, the dot moves on.
