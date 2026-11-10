# Tasks

## 1. Read-back and floors

- [x] 1.1 Move `fetch` from `Bundle.kt` to a shared helper in the pipeline; `BundleTest` still passes.
- [x] 1.2 `Verify.kt` with `verify(directory, published, allowDrop, floors, knownRoutes): List<Failure>` and the read-back: each graph and geometry entry of `index.json` through `NetworkInstaller`, every square of the path data checked against its checksum and decoded. Floors per network type and for the path data, both networks required. Tests in `VerifyTest` on graphs written by the test:
  - "File the apps would reject": a geometry from another graph, and a path file with one byte of a square changed, each give a failure.
  - "Too few knots": a network below its floor gives a failure naming the counts; a missing network fails as zero knots.
  `./gradlew :pipeline:test` passes.

## 2. Drop against the published data

- [x] 2.1 The drop check against the published index and graphs, fetched from the base URL (a `file:` URL in tests, so they need no network); `--allow-drop` skips it. Tests in `VerifyTest`:
  - "Sudden drop": 8 % fewer knots, and separately 8 % fewer connections, each give a failure.
  - "Small change": 2.5 % fewer and any growth pass.
  - "Deliberate drop": with `allowDrop`, the 8 % drop passes, and a floor failure still fails.
  - "Nothing published yet": a failing fetch, and a published graph of an unsupported format version, skip the check with a warning and no failure.
  `./gradlew :pipeline:test` passes.

## 3. Known routes

- [x] 3.1 Known routes by position (nearest knot within 1 km, route at most twice the straight line) with `KnotLocator` and `ShortestPathRouter`. Tests in `VerifyTest` for "Known route lost": no knot near a position, no route between the knots, and a route more than twice the straight line each give a failure naming the pair; a fitting route passes.
- [x] 3.2 Choose the production pairs of the design and the floors; check them against the published data of 2026-11-10 by running `pipeline verify` on the downloaded published files (graphs, geometry, index and path data), which passes and logs every measured count and route length.

## 4. Command and workflow

- [x] 4.1 `pipeline verify [--allow-drop] <dir> [<published-base-url>]` in `Main.kt` and its usage text: prints one line per check and exits with 1 on any failure. A test in `MainTest` checks the report passes only without failures (the checks themselves are in `VerifyTest`).
- [x] 4.2 `update-network-data.yml`: `verify` after "Write index" and before "Publish release", with the published base URL; a `workflow_dispatch` input `allow-drop` (boolean, default false) passed as `--allow-drop` only when set. `actionlint` passes in the Build workflow.
- [x] 4.3 Run the workflow by hand once after pushing and check its log: every check listed, nothing failed, the data published as before.

## 5. Wrap-up

- [x] 5.1 `manual-checks.md` lists every scenario with how it was checked (the `VerifyTest` tests, and the workflow run of 4.3). `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. No CHANGELOG entry, as nothing changes in the apps.

## Workflow follow-up

- Archive after the first scheduled Monday run has passed through the new checks.
