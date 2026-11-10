package com.knoopbyknoop.core

import kotlin.math.min
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the network sync came to, kept across starts the same way on both apps, see `network-sync`
 * "Install only verified data". Immutable: every event returns a new state.
 *
 * @property failed the networks whose last download or check failed
 * @property needsAppUpdateFor the networks with a published file this app version can't read
 * @property retries how many runs in a row discarded a corrupted file
 * @property runCorrupted whether the run going on discarded a corrupted file
 */
@Serializable
data class NetworkSyncState(
  val failed: Set<IndexedNetworkType> = setOf(),
  val needsAppUpdateFor: Set<IndexedNetworkType> = setOf(),
  val retries: Int = 0,
  val runCorrupted: Boolean = false,
) {

  /** Whether to ask the user to update the app. */
  val needsAppUpdate: Boolean
    get() = needsAppUpdateFor.isNotEmpty()

  /** Whether a corrupted file waits to be downloaded again. */
  val retry: Boolean
    get() = retries > 0

  /** How long to wait before downloading again, see "Retrying a corrupted download". */
  val retryDelayMillis: Long
    get() =
      if (retries == 0) 0
      else min(FIRST_RETRY_MILLIS shl min(retries - 1, MAX_DOUBLINGS), MAX_RETRY_MILLIS)

  /** [entry] was installed: its network works, and this app version reads it. */
  fun installed(entry: NetworkIndexEntry): NetworkSyncState =
    copy(failed = failed - entry.network, needsAppUpdateFor = needsAppUpdateFor - entry.network)

  /** [entry] was downloaded but not installed, for [reason]. */
  fun rejected(entry: NetworkIndexEntry, reason: RejectionReason): NetworkSyncState =
    when (reason) {
      RejectionReason.UNSUPPORTED_VERSION ->
        copy(failed = failed + entry.network, needsAppUpdateFor = needsAppUpdateFor + entry.network)
      RejectionReason.CORRUPTED,
      RejectionReason.MISMATCHING_GEOMETRY ->
        copy(
          failed = failed + entry.network,
          retries = if (runCorrupted) retries else retries + 1,
          runCorrupted = true,
        )
    }

  /** A file of [network] couldn't be downloaded. */
  fun downloadFailed(network: IndexedNetworkType): NetworkSyncState =
    copy(failed = failed + network)

  /** The run ended; one without a corrupted file ends the retries. */
  fun finished(): NetworkSyncState =
    if (runCorrupted) copy(runCorrupted = false) else copy(retries = 0)

  fun toJson(): String = json.encodeToString(this)

  companion object {
    /** The first wait, then twice as long each time, as WorkManager does on Android. */
    const val FIRST_RETRY_MILLIS = 30_000L

    /** The longest wait, WorkManager's limit. */
    const val MAX_RETRY_MILLIS = 5 * 60 * 60 * 1000L

    private const val MAX_DOUBLINGS = 20

    private val json = Json { ignoreUnknownKeys = true }

    /** The state stored with [toJson]; a fresh one if it can't be read. */
    fun parse(text: String): NetworkSyncState =
      try {
        json.decodeFromString(text)
      } catch (exception: IllegalArgumentException) {
        NetworkSyncState()
      }
  }
}
