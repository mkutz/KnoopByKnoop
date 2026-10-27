# Tasks

## 1. Pipeline: one graph per network type

- [x] 1.1 `NetworkExtraction` keeps routes keyed by relation ID; test that a relation fed twice (as from two overlapping extracts) yields one connection
- [x] 1.2 Read several extracts in shared passes (relations of all files, then ways, then nodes); `PbfExtractTest` with two small PBF files written by `PbfWriter`, overlapping in one knot: covers "Knot on a border" and "Connection cut by an extract border" (each file holds only part of the route's ways)
- [x] 1.3 The data date is the oldest of the extracts' dates; test for "Data date of several extracts"
- [x] 1.4 CLI `pipeline build <area> <output-dir> <extract>...` writing `cycling.*` and `hiking.*` with area `nl-be-nrw`; `index` keeps working on the new names; `MainTest` updated and covers "Covered area" with two fixture extracts
- [x] 1.5 Rename the exposed `region` property to `area` in `GraphFile` and `NetworkIndexEntry`, keeping the serialized name and the byte format; `GraphFileTest` and `NetworkIndexTest` still pass unchanged otherwise

## 2. Workflow: build and publish the merged files

- [ ] 2.1 `update-network-data.yml`: one build job downloading all three extracts and running a single `build`; a failed extract download fails the job before `publish` ("Failed rebuild keeps old data")
- [ ] 2.2 Size budgets on `cycling.graph` (5 MB) and `cycling.geometry` (10 MB): "Size budget", "Geometry size budget"
- [ ] 2.3 `publish` deletes release assets that are not part of the new set, after the upload succeeded
- [ ] 2.4 Run the workflow manually; the `network-data` release lists exactly `cycling.*`, `hiking.*` and `index.json`, and `index.json` lists each graph with its geometry ("Index lists geometry")

## 3. Core: networks instead of regions

- [x] 3.1 `NetworkCatalog` with a status per network type (missing, downloading, installed, updatable, last attempt failed) and the files still to fetch, cycling first; replaces `RegionCatalog`; tests for "First start" (geometry missing, graphs only when newer than bundled), "Cycling is ready first", "Newer data available", "Newer data already installed", "Failed update"
- [x] 3.2 `NetworkInstaller` takes over `RegionInstaller`'s verification unchanged; the existing tests move along and cover "Corrupted download" and "Unsupported file version"
- [x] 3.3 `UpdateCheck.isDue(lastCheck, now)`: due without a previous check and after 24 hours, not before; tests
- [x] 3.4 `RouteSnapshot` and `PlanningSession` without `region`; test that a snapshot JSON with a `region` key still restores, and that a missing knot is reported ("Knot gone after an update", "Route across an update")
- [x] 3.5 A test graph spanning two fixture areas plans a route across them: "Route from the Netherlands into Belgium" at core level
- [x] 3.6 `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` passes

## 4. Bundled graphs

- [x] 4.1 `pipeline bundle <base-url> <output-dir>`: fetches `index.json` and the graphs, verifies them against the index and the supported format version, writes the graphs and `bundled-index.json`; tests against local fixture files for a matching graph and for a corrupted one (fails)
- [x] 4.2 Gradle task `:pipeline:bundleNetworkGraphs` with cached output; release builds fail without data, debug builds fall back to an empty bundle with a warning; verified by running it once online and once with an unreachable base URL
- [x] 4.3 Core: installing from `bundled-index.json` through `NetworkInstaller`; tests for "Planning right after installation" and "Newer data already installed"

## 5. Android

- [ ] 5.1 Add WorkManager; `NetworkSyncWorker` (network constraint, backoff, unique work) fetches the index and the missing files in catalog order, verifies and installs each file on its own
- [ ] 5.2 Bundled graphs as generated assets of the app, installed on first start; storage under `networks/`; delete `regions/` after the first successful install
- [ ] 5.3 Enqueue the worker on app start when files are missing or the update check is due; remove `RegionDownloads`, `RegionsScreen` and the navigation to it
- [ ] 5.4 Courses-loading note on the map for the selected network (loading, needs network access) and the info sheet (data dates, state, retry, attribution)
- [ ] 5.5 `./gradlew :apps:android:assembleDebug` passes and the APK contains both bundled graphs

## 6. iOS

- [ ] 6.1 `AppModel` runs the sync on start: index, missing files in catalog order on the background `URLSession`, verify and install each file; `NWPathMonitor` starts it when network access comes back
- [ ] 6.2 The pre-build script runs `:pipeline:bundleNetworkGraphs` and copies its output into the app's resources; bundled graphs installed on first start; storage under `networks/`; delete `regions/` after the first successful install
- [ ] 6.3 Remove `RegionsView` and its navigation; courses-loading note and info sheet as on Android
- [ ] 6.4 The iOS app builds (`xcodegen` and `xcodebuild` for the simulator) and contains both bundled graphs

## 7. Manual checks

- [ ] 7.1 On the Android emulator and the iOS simulator, record in `manual-checks.md` in this change: "Planning right after installation" (fresh install in airplane mode), "First start", "Leaving the app during the download", "Interrupted download", "Cycling is ready first", "No network access on first start", "Opening the info sheet", "Airplane mode", "Cycling network of the Netherlands" (no gap at the borders) and a planned route from Zeeland into Flanders ("Route from the Netherlands into Belgium")
- [ ] 7.2 Development builds that still have per-region data switch to the new networks on start, keep the saved route and remove `regions/`
- [ ] 7.3 `./gradlew check` and the iOS app build pass locally and in CI
