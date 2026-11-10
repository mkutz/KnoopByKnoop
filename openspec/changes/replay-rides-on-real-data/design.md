# Design

## Context

- The published cycling data (data date 2026-11-08): `cycling.graph` 545 KB, `cycling.geometry` 3.1 MB, `cycling.paths` 98 MB in squares of 1/16° (about 7 × 4.4 km here), each compressed on its own and listed in the file's directory.
- `NetworkGeometry` holds per connection index the course, the signpost courses, the bridge stretches and the courses back; it is bound to its graph by the graph file's SHA-256.
- Change 3's `checks/Replay.kt` and change 4's `checks/Replay2.kt` replay the scripted 06 → 08 ride near Titz (448 fixes, a 400 m detour) through `NavigationSession` with the full published files.
- `NavigationReplayTest` records whole rides as text and records a missing text on the first run.
- The repository is public and Apache-licensed; the network data is OpenStreetMap's, under the ODbL.

## Goals / Non-Goals

**Goals:**
- A ride on real data runs in the normal test run, in well under a second each, without network access or the published files.
- A difference in what a rider would get shows as the lines that changed, per fix.

**Non-Goals:**
- Asserting what is right: the recorded texts pin today's behavior; the specs and the unit tests say what is right.

## Decisions

### Cutting an area in the pipeline

`pipeline cut <published-dir> <south> <west> <north> <east> <out-dir>` keeps the knots inside the box, the connections and ferries with both knots kept, and writes the graph with `GraphFile`. The geometry keeps the courses, signpost courses, bridge stretches and courses back of the kept connections, re-indexed, and is written for the new graph's hash. The path file gets the squares the box touches, their compressed bytes copied unchanged, under a new header.

*Alternative:* cut from an OpenStreetMap extract by running the pipeline on a small `.osm.pbf`. It would test the extraction too, but the result differs from what the apps install (merged knots and courses depend on the surroundings) and needs the extract downloaded.

### Ride scripts

```
# Siegburg-Zange: walk of 2026-11-09, leaving west of the route south of the river and going on
route 81468719 59469043 1336813155
along 0 1             # along the course from the knot at 0 of the route to the one at 1
path 50.7861 7.1953   # along the real paths to there
line 50.7850 7.1930   # straight to there
still 60              # standing still for 60 s
coarse 50.7900 7.2100 # one fix of 80 m accuracy there, as with the screen off
noise 4               # from here on, fixes scattered by up to 4 m, from a fixed seed
```

A small generator in `jvmTest` turns a script into fixes 10 m apart at 18 km/h. `path` asks the cut-out's `PathRouter` for the way, so detours follow real streets. Titz keeps change 3's 448 fixes as a plain fix list, so its recorded text can be compared with that replay.

### What a ride records

Per fix, from `NavigationSession`'s step: the notices, the precision, the way search (from, to, found length and turns), the header's spoken text, the progress line, the guided way's line, the end. A way search is answered at once with the cut-out's `PathRouter`. The format follows `NavigationReplayTest`; a missing text is recorded and the test fails once.

### The rides

| Area | Ride | Shows |
|---|---|---|
| Titz | change 3's 06 → 08 with its detour | guided way back, arriving |
| Siegburg-Zange | 01 → 02 → 28 straight through | the signpost the route uses at 02, the crossing at the L16 bridge |
| Siegburg-Zange | leaving west south of the river and going on | the turn back to where the rider left (walk 2026-11-09) |
| Siegburg-Zange | turning off with the way starting behind the rider | "turn back" on the guided way (ride 2026-11-04) |
| Rhine | 83 → 12 → 21, ferry, 20 | the ferry on the strip and its notices |
| Rhine | a route visiting 83 twice | the later visit not taken for the first (ride 2026-11-08) |
| Rhine | coarse fixes 120 m off the course with the screen off | off route only after a precise fix (ride 2026-11-08) |
| Arnhem | through 43 entering at one node, leaving at another | passing a double knot, Dutch sign plates |

The exact positions are picked in the cut-outs while writing the scripts; each script's first line names its finding.

### Frozen data

The cut-outs carry their data date and are not refreshed with the weekly publication. Re-cutting is a deliberate change: run `cut` again, re-record, and review the differences like any other.

## Risks / Trade-offs

- [A recorded text pins a known bug, so its fix "breaks" the test] → Intended: the fix's change re-records the ride, and the diff shows exactly what it changed for the rider; the script's comment names the finding.
- [Path squares make the cut-outs large] → The squares a 10 km box touches are 6–12; at about 30 KB each, an area stays under 1 MB. The cut command prints the sizes, and task 1.2 checks the total stays under 4 MB.
- [A cut-out misses a connection the full network has, so a route or a way differs from the ride] → Boxes reach 2 km beyond the rides; Titz is compared with the full-data replay of change 4 (task 3.1).
- [Recorded texts change with harmless refactorings of wording] → Same as with `NavigationReplayTest`; the diff is read, not just re-recorded.
