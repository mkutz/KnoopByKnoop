# Design

## Context

Both apps sync the same way: fetch `index.json`, ask `NetworkCatalog.filesToFetch`, download each file, check it (`NetworkInstaller.checkGraph`, or `checkGeometry` against the installed graph), install it or record the rejection. What differs is where the outcome lives (see the proposal) and how a corrupted file is retried. Android runs the sync in a `CoroutineWorker` that WorkManager retries with exponential backoff from 30 s (its maximum is 5 hours); iOS downloads with a background `URLSession` and installs in `AppModel.installFinished` when files finish.

## Goals / Non-Goals

**Goals:**
- Every decision about a downloaded file and the sync's outcome in the core, tested once.
- The same outcome kept across starts on both apps.

**Non-Goals:**
- Changing how files are downloaded or stored.

## Decisions

### `NetworkSyncState`: an immutable record with three transitions

```kotlin
@Serializable
data class NetworkSyncState(
  val failed: Set<IndexedNetworkType> = setOf(),
  val needsAppUpdate: Set<IndexedNetworkType> = setOf(),  // networks with a file the app can't read
  val retries: Int = 0,                                    // corrupted downloads since the last clean run
) {
  fun installed(entry: NetworkIndexEntry): NetworkSyncState   // network ok, its update need gone
  fun rejected(entry: NetworkIndexEntry, reason: RejectionReason): NetworkSyncState
  fun started(toFetch: Collection<NetworkIndexEntry>): NetworkSyncState  // nothing to fetch: current
  fun downloadFailed(network: IndexedNetworkType): NetworkSyncState
  fun finished(): NetworkSyncState                            // a run with no corrupted file resets retries
  val retry: Boolean                                           // a corrupted file is waiting for a retry
  val retryDelayMillis: Long                                   // 30 s × 2^(retries-1), at most 5 h
  fun toJson(): String; companion object { fun parse(text: String): NetworkSyncState }
}
```

*Why `needsAppUpdate` per network:* it is cleared when that network's file is installed, so a sync that only fetched the other network doesn't clear it. Shown as "needs app update" when not empty, as today.

*Why `started`:* a sync that finds nothing to fetch for a network leaves it with current data, e.g. when the publication went back to a file the device has; then neither a failure nor the request to update applies to it any more. Found while implementing.

*Why `retries` in the state:* iOS needs the count for the delay; Android's WorkManager counts its own attempts, so Android only uses `retry`. Keeping the count in the state also lets iOS resume the delay after a start.

*Alternative:* keep the flags in the apps and only share the check. Rejected: the bugs are in the flags.

### `NetworkInstaller.check`

```kotlin
fun check(entry, bytes, installedGraphEntry: NetworkIndexEntry?, installedGraph: NetworkGraph?): InstallCheck<*>
```

A graph goes through `checkGraph`; a geometry without an installed graph is `MISMATCHING_GEOMETRY`, otherwise `checkGeometry`. Replaces `NetworkSync.verify` and the branch in `installFinished`.

### Storage per app, same JSON

Android stores `toJson()` under one key in the `network-sync` preferences and reads the old `failed` and `needs-app-update` keys once if the new key is missing (the old `needs-app-update` is dropped: it was never kept anyway). iOS stores it in `UserDefaults` under `networkSyncState`.

### iOS retry

After `installFinished`, if `state.retry`, `AppModel` starts a `Task` that sleeps `retryDelayMillis` and runs `sync()` again; a newer sync cancels a waiting retry. Background `URLSession` downloads are not involved: the retry only runs while the app runs, which the spec scenario states.

## Risks / Trade-offs

- [A broken published file triggers retries for hours] → The delay doubles to 5 hours; the publication gate of `gate-network-data-publish` makes such files unlikely.
- [Migration on Android loses the failed networks once] → They come back with the next failed attempt; nothing is lost that the user relies on.
