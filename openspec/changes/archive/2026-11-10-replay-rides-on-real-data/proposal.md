# Proposal

## Why

The bugs found on rides came from real OpenStreetMap shapes: the street crossing at 02 in Siegburg-Zange, the needless turn back to the river path, ways starting behind the rider, bends announced as turns, knot 83 visited twice, false "off route" from a coarse fix. The tests use straight synthetic lines, so none of these could have been caught there, and a fix for one cannot be checked against the others except on a ride. The replays of changes 3, 4 and 4b showed that whole rides compared fix by fix catch what single assertions miss, but the real-data replay runs only by hand from a scratch directory with the 98 MB path file.

## What Changes

- A pipeline command `cut` that writes the published cycling graph, courses and path data of a small area as files of the same formats: the knots in the area, the connections and ferries between them, their courses, and the path squares the area touches, copied unchanged.
- Four cut-outs from the published data, committed as test resources with their data date and OpenStreetMap's attribution:
  - **Titz**: 06 → 08, the ride of change 3's replay.
  - **Siegburg-Zange**: 01, 02 with its two signposts at the Sieg bridge, 28.
  - **Rhine at Lülsdorf**: 12, 21, the ferry Lülsdorf–Wesseling to 20, and 83.
  - **Arnhem**: 43, a Dutch knot tagged on four nodes.
- Scripted rides on them: a short text per ride naming the route's knots and the rider's way, along a course, along real paths to a point, straight to a point, standing still, with coarse fixes or with GPS noise from a fixed seed. Each ride says which ride finding it reproduces.
- `RealRideTest` runs each ride through `NavigationSession` with the real path router, fix by fix, and compares every notice, precision, way search with its length and turns, header, progress line and guided way with a committed text, as `NavigationReplayTest` does on the synthetic network.
- The rides record today's behavior, including what the ride findings call wrong; a later fix shows as a reviewed difference in the recorded text.

Out of scope:
- Fixing any of the findings the rides show; each stays its own change.
- Recorded tracks from the maintainer's rides: the repository is public, and tracks would show where rides start. Rides are scripted on the real data instead.
- Hiking cut-outs, and the apps' own code (the rides run the core's session, as the apps do).
- Refreshing the cut-outs with each weekly publication: they are frozen at one data date, and a re-cut is a deliberate change with its recorded texts reviewed.

## Capabilities

### New Capabilities

### Modified Capabilities

## Impact

- `pipeline`: the `cut` command and its test.
- `modules/core/src/jvmTest`: `RealRideTest`, the ride scripts and their recorded texts, and the cut-outs under `resources/real-areas/` (about 0.5–1 MB each, OpenStreetMap data under the ODbL, with a notice saying so).
- `CLAUDE.md`: how to re-record a ride after a deliberate behavior change.
- No change to the apps; no CHANGELOG entry.
