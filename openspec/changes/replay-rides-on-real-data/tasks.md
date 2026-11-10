# Tasks

## 1. Cut-outs

- [x] 1.1 `pipeline cut` with `CutTest` on a small graph with courses, signpost courses, a bridge stretch, a course back, a ferry and path squares written by the test: only the knots in the box and the connections and ferries between them are kept, the geometry reads back for the new graph with its sections on the right connections, and the kept squares' bytes are unchanged. `./gradlew :pipeline:test` passes.
- [x] 1.2 The four cut-outs (Titz, Siegburg-Zange, Rhine at Lülsdorf, and the Veluwe around 43, which is near Apeldoorn rather than Arnhem) from the published data of 2026-11-08 under `modules/core/src/jvmTest/resources/real-areas/`, with a `NOTICE` (OpenStreetMap contributors, ODbL, data date). Each reads in the apps' own way (`NetworkInstaller.check` or the same checks); together under 4 MB.

## 2. Rides

- [x] 2.1 The ride script generator in `jvmTest` with tests: `along` follows the course, `path` follows the way `PathRouter` finds, `line`, `still`, `coarse` and `noise` give the fixes the design describes, and the same script gives the same fixes every run.
- [x] 2.2 `RealRideTest` and the eight rides of the design, their texts recorded with the current code. Each recorded text read against the finding its script names; what each shows (or does not) written into `manual-checks.md`. `./gradlew :modules:core:jvmTest` passes, the rides together in under 5 s.

## 3. Proof and documentation

- [x] 3.1 The Titz ride gives the same notices, way searches and lengths as change 4's full-data replay (`checks/replay-after.txt`). `./gradlew check` passes. `CLAUDE.md` says how to re-record a ride after a deliberate change and how to re-cut an area. No CHANGELOG entry.
