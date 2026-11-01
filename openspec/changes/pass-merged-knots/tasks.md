# Tasks

## 1. Graph data

- [x] 1.1 `Knot.otherNodes` and `positions`; `GraphFile` writes and reads the section after the ferries, read only when bytes are left. Tests in `GraphFileTest` for "Positions of a merged knot" (round trip of a knot with another node) and "Graph files without the positions of merged knots" (a file without the section reads with no other nodes); both pass
- [x] 1.2 "Apps without the positions of merged knots": a throwaway test against the core of 0.6.0 (`git worktree` at `a493d81`) reads a graph file with the section and finds the same knots and connections; the result noted in `manual-checks.md`
- [x] 1.3 `NetworkExtraction.mergeSameKnots` keeps the other members' positions; a test in `NetworkExtractionTest` with 02 tagged on two nodes 195 m apart finds one knot 02 with the other node's position; all pipeline tests pass

## 2. Navigation

- [x] 2.1 Tests in `NavigationTest` for "Passing a merged knot at its other node", "Passing a merged knot that is not on the route" and "Start at another node of a merged knot", failing first; then `entered`, `start`, `lastWithin` and `KnotLocator` use all positions; the tests and all existing core tests pass

## 3. Check

- [x] 3.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased says that a knot tagged in two places counts as passed at either
- [x] 3.2 On the emulator, with a graph written by the new pipeline code for the area around Siegburg, a route starting at 02 passes 02 at node 81461844 (screenshot of the panel after it), written up in `openspec/changes/pass-merged-knots/manual-checks.md`

## Workflow follow-up

- `fix-double-knots` is archived before this change, as both modify "Passing a knot" and "Start navigation".
- After the next weekly publication, the maintainer rides through Siegburg 02 at its north-west node on the Pixel and the iPhone before `/opsx:archive pass-merged-knots`.
