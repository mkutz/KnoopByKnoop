# Design

## Context

See proposal.md for the motivation.

- **Pipeline:** `pipeline build <region> <extract> <output-dir>` reads one Geofabrik extract into a `NetworkExtraction`, in three passes: relations, then ways, then nodes. The extraction collects routes in a list. Ways and nodes go into maps keyed by OSM ID. The ways it wants are fixed lazily on the first way it sees, and the node coordinates it keeps on the first node.
- **Workflow:** `update-network-data.yml` builds each region in its own matrix job, then one job writes `index.json` and uploads everything to the `network-data` release.
- **File format:** graph files and index entries carry a `region` field, which the apps use for storage paths (`regions/<region>/`), for `RouteSnapshot.region` and for `PlanningSession.region`.
- **Apps:** both go through `RegionCatalog` and `RegionInstaller` in the core. Android downloads with `DownloadManager` and a visible notification. iOS uses a background `URLSession`. Downloads only start from the regions screen.
- **Extract borders:** Geofabrik extracts overlap at the borders, and a relation crossing a border appears in both extracts with the same ID and member list. Each extract may lack some of its ways or nodes, which is why border connections are skipped today as incomplete.

## Goals / Non-Goals

**Goals:**
- Cross-border connections come out complete because the extracts are read as one dataset, not by merging finished graphs.
- No change to the graph, geometry or index file formats, so readers, verification and tests stay as they are.
- A data model that still works if the data is split into several files per network type later.

**Non-Goals:**
- Routing over several graph files at once. Splitting the data later will need it, but nothing needs it now.
- Background update checks while the app is not running.

## Decisions

### Read all extracts in the same three passes
`build` takes several extracts. `PbfExtract` gets a companion function that runs each pass over all files before the next pass starts: relations of all files, then ways of all files, then nodes of all files. That satisfies `NetworkExtraction`'s ordering contract. `NetworkExtraction.acceptRelation` keeps routes in a map keyed by relation ID, so a relation seen in two extracts counts once. Ways and nodes are already deduplicated by their maps. With all members from all extracts available, border connections are complete, and junction merging (`mergeSameKnots`) works across the border unchanged.

*Alternative:* merge the PBF files with osmium before the build. That adds a native tool to CI and to local development for something the reader can do in a few lines. Rejected.

*Alternative:* merge the finished per-region graphs. Connections cut by an extract border would still be missing, because neither extract can build them alone. Rejected.

### Keep `region` in the formats, as the covered area
The `region` field stays in the graph header and the index, now naming the covered area: `nl-be-nrw`. The CLI becomes `pipeline build <area> <output-dir> <extract>...`, and files are named `cycling.graph`, `cycling.geometry`, `hiking.graph` and `hiking.geometry`. In Kotlin the field is renamed to `area` where it is exposed, and the serialized names stay unchanged. The data date is the oldest of the extracts' dates.

Keeping the field is what leaves room for a later split. An index with several areas per network type is valid as it is, and the apps install every file of a network they find in the index. Bumping the format version was considered. It would buy nothing while no app is released.

### One build job; delete stale release assets after publishing
The workflow downloads all three extracts in one job (about 1.5 GB, well within the runner's disk) and runs a single `build`. A missing or unreadable extract fails the job before `publish`, so the previous release stays as it is. After uploading, `publish` deletes every release asset that is not part of the new set, which removes the old per-region files after the first successful run. The size budgets apply to `cycling.graph` and `cycling.geometry`.

### Bundle the graphs at build time
A new command `pipeline bundle <base-url> <output-dir>` fetches `index.json` and the graph files of every network from the release. It verifies each graph against its index entry with the core's existing size and checksum check, and checks that the graph's format version is the one the core reads. It then writes the graphs together with a `bundled-index.json` that lists only them. The Gradle task `:pipeline:bundleNetworkGraphs` runs the command into `pipeline/build/bundled-networks/`. Its output is cached by Gradle, and `--rerun-tasks` or a clean refreshes it.

- **Android:** the app registers the task's output as a generated assets directory through the variant API.
- **iOS:** the existing pre-build script, which already calls Gradle, runs the task and copies its output into the app bundle's resources.
- **Release builds** fail when the fetch or the verification fails, so a release never ships without its data. **Debug builds** fall back to an empty bundle with a warning, so working offline on the code stays possible. The app then downloads everything as on first start.

On first start, the apps install the bundled graphs through `NetworkInstaller`, exactly like downloaded files, with `bundled-index.json` as their index. The catalog and the update logic therefore need no special case. A bundled graph is skipped when an installed graph of the same network has the same or a newer data date ("Newer data already installed").

*Alternative:* commit the graphs to the repository. That adds about 1.5 MB of binary changes every week and needs its own update automation. Rejected.

*Alternative:* bundle the geometry too. It costs another 4.2 MB per app for something that only replaces straight lines during the first seconds. Rejected.

### `NetworkCatalog` replaces `RegionCatalog` in the core
`NetworkCatalog(index, installed, downloading, lastFailure)` derives one status per network type: `MISSING`, `DOWNLOADING`, `INSTALLED` or `UPDATABLE`, plus whether the last attempt failed. It also returns the files the apps still need to fetch: files of that network whose index entry differs from the installed one. `RegionInstaller`'s verification (size, checksum, format version, geometry belonging to its graph) moves over unchanged as `NetworkInstaller`. `RouteSnapshot` and `PlanningSession` drop `region`. The snapshot's JSON already ignores unknown keys, so routes saved by today's builds still restore, and knots whose IDs changed with the merge are reported as removed waypoints, as specified.

The rule "check at most once a day" is a pure function in the core (`UpdateCheck.isDue(lastCheck, now)`), so both apps use the same rule and it is tested once.

### Android: WorkManager instead of DownloadManager
A unique `NetworkSyncWorker` (a `CoroutineWorker` with a `CONNECTED` network constraint and exponential backoff) fetches the index and then each missing file, verifies it and installs it. The app enqueues the worker on start when the catalog has something missing or when an update check is due. The network constraint gives "starts by itself when network access is back" without any code of ours. Each file is installed as soon as it is verified, so a retry only fetches what is still missing. Cycling files come first.

*Alternative:* keep `DownloadManager`. It needs a visible notification, or an extra permission to hide it, for downloads the user never started. It also can't wait for connectivity before starting, so retries would need our own scheduling. Rejected.

### iOS: keep the background URLSession, start it from the app model
`AppModel` runs the same flow on start: fetch the index, ask the catalog which files are missing, and enqueue them on the existing background `URLSession`, cycling first. Background sessions already continue when the user leaves the app and retry when the connection comes back. On first start without network access, an `NWPathMonitor` starts the flow once a path is available.

### Storage and the first install
Both apps store networks under `networks/` instead of `regions/<region>/`. After the first successful install of the new data, the old `regions/` directory is deleted. Its route snapshot stays and is restored on the new graph.

### Courses-loading note and info sheet
The planner is the only screen, and with the bundled graphs it is usable right away. While the selected network's geometry is not installed, a small note at the top of the map says "Loading the courses…", or that they need network access once. Connections and routes are drawn as straight lines, as `map-planning` already specifies for missing geometry. In the rare case that a debug build has no bundled graphs and nothing is installed yet, the same note says that the network is loading and the map shows no knots. An info button on the map opens a sheet with each network's data date, the download or update state, a retry action after a failure, and the ODbL attribution. MapLibre's attribution control stays visible on the map, as `map-planning` requires.

## Risks / Trade-offs

- **[Risk] The merged extraction needs more memory in CI** (three extracts at once). → The extraction only keeps network routes, their ways and the coordinates of their nodes. It is still well within a GitHub runner, and the build logs its sizes so growth is visible.
- **[Risk] A later split per area needs routing across graphs** or overlapping areas. → Accepted. The formats already allow several areas, and the split is out of scope until the data grows.
- **[Trade-off] About 6 MB downloads on cellular without asking.** → It's smaller than many map tile downloads. A Wi-Fi-only setting is out of scope and can be added later without changing the specs.
- **[Trade-off] The graphs are stored twice** (in the app and installed, about 1.5 MB). → Accepted. This keeps installation uniform and lets updates replace the installed copy without touching the app bundle.
- **[Risk] App builds need network access** to fetch the graphs. → Debug builds fall back to an empty bundle. CI and release builds always have network access.
- **[Risk] Knot IDs change for merged border knots**, so routes saved by older builds may lose waypoints. → Only development builds exist, and the loss is reported to the user, as specified.

## Migration Plan

1. Merge the pipeline and workflow changes, then run `update-network-data` manually. The release then holds `cycling.*`, `hiking.*` and the new `index.json`, and the old assets are deleted.
2. Ship the app changes. Development builds with per-region data download the new networks on their next start and delete `regions/`.

**Rollback:** revert the workflow and run it again. That republishes the per-region files, which the previous app builds read.
