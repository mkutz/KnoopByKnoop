# Design

## Context

- **The workflow today:** the `build` job writes `cycling.graph`, `hiking.graph`, their `.geometry` files and `cycling.paths` and uploads them as an artifact. The `publish` job checks the size budgets, writes `index.json` with `pipeline index` (which already fails when a geometry file does not belong to its graph) and uploads everything to the rolling release `network-data` with `--clobber`.
- **What the apps check:** `NetworkInstaller.checkGraph` and `checkGeometry` (core, `Networks.kt`) compare size and checksum with the index entry and read the file. `pipeline bundle` already uses them for the bundled graphs.
- **Counts of 2026-11-10** (last build log): cycling 20,584 knots, 31,927 connections; hiking 50,460 knots, 74,605 connections; path data 3,711 squares, 10,435,008 segments.

## Goals / Non-Goals

**Goals:**
- One command that fails the run before the upload, with a log line per check saying what was measured and what was expected.
- The checks tested on small graphs written by the tests, without the network.

**Non-Goals:**
- Statistics or dashboards about the data over time.

## Decisions

### `verify` runs after `index`, against the new `index.json`

Order in the publish job: size budgets → `pipeline index` → `pipeline verify` → upload. Verifying against the index means the graph and geometry files are checked exactly as an app checks a download (`NetworkInstaller` with the index entry), including size and checksum.

*Alternative:* verify in the build job. Rejected: the artifact passes through the upload and download between the jobs, and the index is written in the publish job.

### All checks are collected, then the command fails once

`verify(directory, published, allowDrop, floors, knownRoutes): List<Failure>` runs every check and returns what failed; `main` prints every result and exits with 1 when the list is not empty. One run then shows every problem, not just the first. Floors and known routes are parameters with the production values as defaults, so tests can pass tiny ones.

### Floors: about 90 % of today, as constants

| | knots | connections |
|---|---|---|
| cycling | 18,000 | 28,000 |
| hiking | 45,000 | 67,000 |

Path data: 3,300 squares and 9,000,000 segments. Both networks must be in the index; a missing network fails as zero knots.

The floors catch a missing extract or region even on the first publication. They are raised by hand when the networks grow; the drop check covers the weeks in between.

### Drop: against the published graphs, 5 % per network

`verify` fetches the published `index.json` and graph files from the base URL (the same `fetch` as `bundle`, moved to a shared helper) and reads them with `GraphFile.read`. For each network it fails when the knots or the connections drop by more than 5 %. Growth is never a failure.

If the published index or a published graph cannot be fetched or read (first publication, an older format version, GitHub unreachable), the drop check is skipped with a warning in the log, and the other checks still decide. *Why not fail:* a format version change would otherwise block every first publication of a new format.

`--allow-drop` skips the drop check only. The workflow passes it only from a `workflow_dispatch` input `allow-drop` (default false), so a scheduled run never allows a drop.

The geometry and path data are not compared with the published files: the geometry follows its graph, and the path data's directory alone is several megabytes.

### Known routes: by position, not by knot id

A known route is a pair of positions with a network type. The nearest knot within 1 km of each position is taken with `KnotLocator`; the route between them must exist (`ShortestPathRouter`) and be at most twice the straight-line distance. Positions instead of ids, as ids and refs can change when OpenStreetMap is edited; a knot vanishing within 1 km of a well-chosen position is itself worth a failure.

Pairs (chosen at implementation, each checked against the data of 2026-11-10 and kept only if it routes today):
- cycling: Netherlands → Belgium (e.g. Breda → Antwerp), Belgium → North Rhine-Westphalia (e.g. Hasselt → Aachen), Netherlands → North Rhine-Westphalia (e.g. Nijmegen → Kleve)
- hiking: one pair within each country's well-covered area (e.g. Siegburg, the Veluwe, the Kempen)

*Alternative:* compare whole routes with last week. Rejected for now: route lengths change with ordinary edits, and the threshold would be guesswork.

### Path data: every square unpacked

`PathFile.open` over a `RandomAccessFile`, then for each square of the directory: read its bytes, compare `sha256Hex` with the directory's checksum, and decode the segments. Counted squares and segments go to the floors. One square at a time keeps the memory low; decoding 100 MB takes seconds on a runner.

## Risks / Trade-offs

- [Real growth or shrinkage over months makes the floors stale] → They are 10 % below today and only meant to catch big losses; the drop check catches week-to-week losses. Raising them is a one-line change.
- [A legitimate OpenStreetMap change drops more than 5 % in one week] → The maintainer reruns the workflow by hand with `allow-drop`.
- [GitHub unreachable during the drop check] → The check is skipped with a warning instead of failing, so a published but stale week is not more likely than today; floors and known routes still apply.
- [A known pair loses its knot through an ordinary edit] → The failure names the pair and the position; the maintainer moves the position. Pairs are chosen at well-established knots to keep this rare.
