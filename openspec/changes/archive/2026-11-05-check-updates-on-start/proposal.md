# Proposal

## Why

The weekly network data published on 2026-11-02 reached neither of the maintainer's phones. The apps check the published index at most once a day, so a phone that checked the evening before waits until that evening. For riders this hardly matters. For testing a data change on the next ride it blocks, and there is no way to make the app look.

## What Changes

- **Every start:** the apps check the published index each time they are started, not at most once a day. Newer data is downloaded and installed in the background as before.
- **Coming back to the app:** bringing it back from the background is not a start and does not check.
- **Nothing else changes:**
  - The check costs one small download of the index, about 1.4 KB.
  - Files are only downloaded when the index lists newer data.
  - Without a connection, Android waits for one and iOS tries again at the next start, as before.
- **No button:** the maintainer decided against a "Check for updates" button in the info sheet; no one but the maintainer would look for it.

## Out of Scope

- **A button or setting** to check by hand.
- **Checking when the app comes back to the foreground**, or periodically in the background.
- **The retry after a failed update** in the info sheet, which stays as it is.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-sync`: "Keep the networks current" checks at every start, no longer at most once a day.

## Impact

- **Core:** `UpdateCheck` in `Networks.kt` and its test in `NetworkSyncTest` are removed.
- **Android:** `NetworkSync.isDue` and the stored time of the last check are removed. `AppViewModel` enqueues the sync at every start.
- **iOS:** `AppModel.isSyncDue` and the stored time of the last check are removed. The sync runs at every start.
