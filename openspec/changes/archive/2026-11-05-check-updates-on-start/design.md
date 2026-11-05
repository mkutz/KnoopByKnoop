# Design

## Context

- **Core:** `UpdateCheck.isDue(lastCheck, now)` in `Networks.kt` is true when the index was never fetched or was last fetched 24 h ago or more (`INTERVAL_MILLIS`). `NetworkSyncTest` "the update check is due once a day" covers it.
- **Android:**
  - `AppViewModel` runs at the app's start. After installing the bundled graphs and restoring the route, it enqueues `NetworkSyncWorker` when `NetworkSync.isDue()`, which is `UpdateCheck.isDue` or a network without courses.
  - The worker needs a connection, as unique work with `KEEP`. It stores the time of the check under `last-check`.
- **iOS:** `AppModel`'s start task runs `sync()` when `isSyncDue()`, the same test with `network-sync-last-check` in `UserDefaults`. `sync()` stores the time.
- **The download itself:** the sync fetches `index.json`, about 1.4 KB, and downloads only the files that `NetworkCatalog.filesToFetch` lists: missing, or with a newer data date. So a start with nothing new costs only the index.

## Goals / Non-Goals

**Goals:**
- Every start checks, on both platforms, with the least code.

**Non-Goals:**
- Checking when the app comes back to the foreground, periodically, or by hand.

## Decisions

**Remove the daily gate instead of shortening it.**
- Android enqueues the worker at every start of `AppViewModel`. iOS runs `sync()` in every start task.
- `UpdateCheck`, `NetworkSync.isDue`, `AppModel.isSyncDue` and their stored times go away, as nothing reads them anymore. So does the core test of the interval.
- The `last-check` keys left on phones are harmless and are not cleaned up.
- Alternative: a short interval, e.g. 1 h. It keeps code that only delays, and the maintainer's case, data published at night and a ride in the morning, wants a check at every start.

**"Start" is where the apps start today.**
- Android: `AppViewModel` is created once per process, and lives on through rotation.
- iOS: the start task in `AppModel.init`.
- Coming back from the background reuses both, so it does not check. Force-quitting and opening the app is the way to check by hand.

**`KEEP` stays on Android.** A sync still running or waiting for a connection is not replaced by the next start's.

## Risks / Trade-offs

- **More index requests:** one per start instead of one per day. The index is a GitHub release asset of about 1.4 KB, so negligible for battery and traffic, and far below GitHub's limits for the number of testers.
- **Offline starts:**
  - On Android, the worker waits for a connection, as before.
  - On iOS, the sync fails quietly. Since the "Failed update" scenario shows a failed attempt in the info sheet, an offline start may now show it more often. That is accepted and checked in the simulator: the next start with a connection clears it.
