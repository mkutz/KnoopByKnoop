# Proposal

## Why

The code review of 2026-11-10 found the handling of downloaded network files written twice, in `NetworkSync.kt` (Android) and `AppModel.installFinished` (iOS), and drifted apart in ways riders can notice:

- **"Update the app" never shows on Android.** A file in a format the app can't read sets the flag, but the end of the same sync clears it again, because only corrupted files count as "retry" (`NetworkSync.run`). `network-sync` "Unsupported file version" asks for the notice.
- **On iOS it is forgotten at each start** (kept in memory only) and comes back only after the file was downloaded and rejected again.
- **A corrupted download is retried on Android** (WorkManager, after 30 s and then twice as long each time) **but not on iOS** until the next start or the info sheet's retry; `network-sync` "Corrupted download" says it is retried later.
- **Failed networks are kept across starts on Android, in memory on iOS.**

Each decision is small, but both apps make all of them on their own, without tests.

## What Changes

- **One record of the sync's outcome in the core:** `NetworkSyncState` with the failed networks, whether an app update is needed, and how many retries ran, changed only through `installed(entry)`, `rejected(entry, reason)` and `finished()`, and stored as JSON by each app in the same way.
- **One check of a downloaded file:** `NetworkInstaller.check(entry, bytes, installedGraphEntry, installedGraph)`, a graph on its own and a geometry against the graph installed before it, instead of the two `verify` functions.
- **One retry delay:** 30 s, doubling, at most 5 hours, as WorkManager does it on Android; iOS waits it out while the app runs and retries.
- **"Update the app" stays until it no longer applies:** set by a file in an unsupported format, kept across starts, and cleared once a sync ends with that network's files installed.
- **Spec:** `network-sync` "Install only verified data" gets the retry delay and the lasting notice as scenarios, for both apps.

## Out of Scope

- **Moving `NetworkStorage` into the core** (file names, stale data, the map layer cache key), copied line for line on both apps today: its own change, as it touches the files on every phone and needs a migration check.
- **How downloads run:** WorkManager on Android, the background `URLSession` on iOS.
- **Downloading again a file that was rejected for its format at every start:** after an app update it is needed anyway; a few megabytes once per start until then.
- **The info sheet's layout and texts.**

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-sync`: "Install only verified data", with when a corrupted download is retried and how long the request to update the app stays.

## Impact

- **Core:** `NetworkSyncState` (with JSON), `NetworkInstaller.check`, `NetworkSyncState.retryDelayMillis`; tests in `NetworkSyncTest`.
- **Android:** `NetworkSync` keeps its state as a `NetworkSyncState` in the existing preferences file, migrating the old keys once; `NetworkSyncWorker` unchanged.
- **iOS:** `AppModel` keeps a `NetworkSyncState` in `UserDefaults` instead of `rejected` and `needsAppUpdate`, and schedules the retry.
- **CHANGELOG:** the notice to update the app shows on Android, and lasts on both; iPhone retries a broken download by itself.
